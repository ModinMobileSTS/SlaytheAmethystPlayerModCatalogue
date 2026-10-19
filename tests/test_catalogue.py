import copy
import hashlib
import json
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest import mock
from urllib.parse import unquote
import warnings
import zipfile

from scripts import catalogue

ROOT = Path(__file__).resolve().parents[1]
EXAMPLE = ROOT / "mods/basemod/Catalogue Example-ModinMobileSTS"
EXAMPLE_JAR = EXAMPLE / json.loads((EXAMPLE / "metadata.json").read_text(encoding="utf-8"))["jar"]


class CatalogueTests(unittest.TestCase):
    def setUp(self):
        temporary = ROOT / "agent-tmp"
        temporary.mkdir(exist_ok=True)
        self.temporary = tempfile.TemporaryDirectory(prefix="catalogue-tests-", dir=temporary)
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        (self.root / "mods").mkdir()
        self.metadata = json.loads((EXAMPLE / "metadata.json").read_text(encoding="utf-8"))

    def add_patch(self, metadata=None):
        metadata = copy.deepcopy(self.metadata if metadata is None else metadata)
        destination = self.root / catalogue.patch_directory(metadata)
        destination.mkdir(parents=True, exist_ok=True)
        (destination / "metadata.json").write_bytes(catalogue.encode_json(metadata))
        shutil.copyfile(EXAMPLE_JAR, destination / metadata["jar"])
        self.rewrite_jar(destination, metadata)
        return destination

    def rewrite_jar(self, destination, metadata, changes=None, extra=None, descriptor=True):
        with zipfile.ZipFile(EXAMPLE_JAR) as jar:
            files = {name: jar.read(name) for name in jar.namelist()}
        manifest = json.loads(files["ModTheSpire.json"])
        manifest.update({field: metadata[field] for field in ("name", "description", "version")})
        manifest["dependencies"] = [metadata["target"]["modId"]]
        manifest.update(changes or {})
        files["ModTheSpire.json"] = catalogue.encode_json(manifest)
        files.pop("agent-patch.json")
        if descriptor:
            files["agent-patch.json"] = (EXAMPLE / "src/agent-patch.json").read_bytes()
            data = json.loads(files["agent-patch.json"])
            data.update({key: metadata[key] for key in ("name", "description", "version")})
            data.update(parent_mod_id=metadata["target"]["modId"], patch_mod_id=manifest["modid"])
            files["agent-patch.json"] = catalogue.encode_json(data)
        files.update(extra or {})
        with zipfile.ZipFile(destination / metadata["jar"], "w") as jar:
            for name, data in files.items():
                jar.writestr(name, data)

    def generate(self, **kwargs):
        return catalogue.generate(self.root, **kwargs)

    def index(self, name="basemod"):
        path = "index.json" if name == "index" else f"mods/{name}/index.json"
        return json.loads((self.root / path).read_bytes())

    def test_real_example_jar_and_java8_class(self):
        result = catalogue.validate_jar(EXAMPLE_JAR, self.metadata)
        self.assertEqual(result["patchModId"], "amethyst_agent_basemod_catalogue_example")
        with zipfile.ZipFile(EXAMPLE_JAR) as jar:
            classes = [name for name in jar.namelist() if name.endswith(".class")]
            self.assertEqual(len(classes), 1)
            bytecode = jar.read(classes[0])
            self.assertEqual(bytecode[:4], b"\xca\xfe\xba\xbe")
            self.assertEqual(int.from_bytes(bytecode[6:8], "big"), 52)
            self.assertIn(b"SpireInitializer", bytecode)
            self.assertIn(b"initialize", bytecode)
            self.assertFalse(any(name.startswith(("com/", "basemod/")) for name in jar.namelist()))

    def test_valid_index_contains_metadata_hash_and_encoded_urls(self):
        destination = self.add_patch()
        self.assertEqual(self.generate(), 1)
        index = self.index()
        self.assertEqual(index["count"], 1)
        self.assertEqual(index["workshopId"], "1605833019")
        entry = index["patches"][0]
        self.assertEqual(entry["author"], self.metadata["author"])
        self.assertTrue(entry["example"])
        self.assertEqual(entry["sha256"], hashlib.sha256((destination / self.metadata["jar"]).read_bytes()).hexdigest())
        self.assertEqual(entry["size"], (destination / self.metadata["jar"]).stat().st_size)
        self.assertIn("Catalogue%20Example", entry["download_url"])
        self.assertEqual(self.index("index")["schemaVersion"], 2)
        summary = self.index("index")["mods"]["basemod"]
        self.assertEqual(summary["count"], 1)
        self.assertEqual(summary["versions"], {})  # The shipped example does not light up a badge.
        self.assertEqual(summary["sha256"], hashlib.sha256((self.root / "mods/basemod/index.json").read_bytes()).hexdigest())
        self.assertFalse((self.root / "basemod.json").exists())

    def test_multiple_mods_grouped_and_sorted(self):
        self.add_patch()
        other = copy.deepcopy(self.metadata)
        other["target"].update(modId="AnotherMod", workshopId="123456789", versions=["2.0"])
        self.add_patch(other)
        other["name"] = "Another Patch"
        self.add_patch(other)
        self.assertEqual(self.generate(), 3)
        self.assertEqual(self.index("AnotherMod")["count"], 2)
        groups = self.index("index")["mods"]
        self.assertEqual(list(groups), ["AnotherMod", "basemod"])
        self.assertEqual(groups["AnotherMod"]["count"], 2)
        self.assertEqual([entry["name"] for entry in self.index("AnotherMod")["patches"]], ["Another Patch", "Catalogue Example"])
        self.assertEqual(groups["basemod"]["count"], 1)

    def test_unicode_names_preserve_case_and_encode_url(self):
        self.metadata["name"] = "中文补丁"
        self.metadata["author"]["name"] = "作者甲"
        self.metadata["target"]["modId"] = "CaseSensitiveMod"
        self.add_patch()
        self.generate()
        entry = self.index("CaseSensitiveMod")["patches"][0]
        self.assertIn("中文补丁-作者甲", unquote(entry["download_url"]))
        self.assertIn("CaseSensitiveMod", entry["download_url"])

    def test_repeated_generation_is_byte_identical(self):
        self.add_patch()
        self.generate()
        before = {p.relative_to(self.root): p.read_bytes() for p in self.root.rglob("*.json")}
        self.generate()
        self.assertEqual(before, {p.relative_to(self.root): p.read_bytes() for p in self.root.rglob("*.json")})
        self.assertEqual(self.generate(check=True), 1)

    def test_same_name_and_author_overwrite_one_entry(self):
        destination = self.add_patch()
        self.generate()
        old_hash = self.index()["patches"][0]["sha256"]
        self.metadata["version"] = "2.0.0"
        self.assertEqual(self.add_patch(), destination)
        self.generate()
        self.assertEqual(self.index()["count"], 1)
        self.assertEqual(self.index()["patches"][0]["version"], "2.0.0")
        self.assertNotEqual(self.index()["patches"][0]["sha256"], old_hash)

    def test_same_name_different_authors_are_separate(self):
        self.add_patch()
        self.metadata["author"]["name"] = "Another Author"
        self.add_patch()
        self.assertEqual(self.generate(), 2)

    def test_remove_last_patch_removes_stale_index(self):
        destination = self.add_patch()
        self.generate()
        shutil.rmtree(destination)
        self.assertEqual(self.generate(), 0)
        self.assertFalse((self.root / "mods/basemod/index.json").exists())
        self.assertEqual(self.index("index"), {"schemaVersion": 2, "count": 0, "mods": {}})

    def test_stale_check_fails_without_writing(self):
        self.add_patch()
        with self.assertRaisesRegex(catalogue.CatalogueError, "stale"):
            self.generate(check=True)
        self.assertFalse((self.root / "index.json").exists())

    def test_invalid_upload_does_not_modify_old_catalogue(self):
        destination = self.add_patch()
        self.generate()
        previous = (self.root / "mods/basemod/index.json").read_bytes()
        (destination / self.metadata["jar"]).write_bytes(b"broken upload")
        with self.assertRaises(catalogue.CatalogueError):
            self.generate()
        self.assertEqual(previous, (self.root / "mods/basemod/index.json").read_bytes())

    def test_missing_required_fields_and_unknown_fields(self):
        for field in self.metadata:
            if field == "example":
                continue
            metadata = copy.deepcopy(self.metadata)
            del metadata[field]
            with self.subTest(field=field), self.assertRaises(catalogue.CatalogueError):
                catalogue.validate_metadata(metadata)
        self.metadata["downloadUrl"] = "https://attacker.example/patch.jar"
        with self.assertRaises(catalogue.CatalogueError):
            catalogue.validate_metadata(self.metadata)

    def test_invalid_path_segments(self):
        for name in ("../escape", "a/b", "a\\b", ".", "..", " hidden", "hidden ", ".hidden", "name.",
                     "CON", "LPT1.txt", "x:y", "x?y", "x\ncommit", "x\u202ey", "e\u0301"):
            with self.subTest(name=name), self.assertRaises(catalogue.CatalogueError):
                catalogue.path_segment(name, "name")

    def test_directory_byte_length_limit(self):
        self.metadata["name"] = "中" * 100
        with self.assertRaisesRegex(catalogue.CatalogueError, "240"):
            catalogue.validate_metadata(self.metadata)

    def test_invalid_email_and_commit_injection(self):
        for email in ("invalid", "a@b", "name <a@example.com>", "a@example.com\nInjected", "a@ example.com"):
            self.metadata["author"]["email"] = email
            with self.subTest(email=email), self.assertRaises(catalogue.CatalogueError):
                catalogue.validate_metadata(self.metadata)

    def test_workshop_id_must_be_string_not_json_number(self):
        for value in (1605833019, "0", "-1", "1.2", "", "1" * 21):
            self.metadata["target"]["workshopId"] = value
            with self.subTest(value=value), self.assertRaises(catalogue.CatalogueError):
                catalogue.validate_metadata(self.metadata)

    def test_exact_versions_and_single_wildcard(self):
        for versions in (["1.0", "2.0"], ["*"]):
            self.metadata["target"]["versions"] = versions
            catalogue.validate_metadata(self.metadata)
        for versions in ([], ["*", "1.0"], ["1.0", "1.0"], "1.0", [5], [" "]):
            self.metadata["target"]["versions"] = versions
            with self.subTest(versions=versions), self.assertRaises(catalogue.CatalogueError):
                catalogue.validate_metadata(self.metadata)

    def test_boolean_schema_version_and_nonboolean_example_rejected(self):
        self.metadata["schemaVersion"] = True
        with self.assertRaises(catalogue.CatalogueError):
            catalogue.validate_metadata(self.metadata)
        self.metadata["schemaVersion"] = 1
        self.metadata["example"] = "false"
        with self.assertRaises(catalogue.CatalogueError):
            catalogue.validate_metadata(self.metadata)

    def test_directory_metadata_mismatch(self):
        destination = self.add_patch()
        destination.rename(destination.with_name("wrong-directory"))
        with self.assertRaisesRegex(catalogue.CatalogueError, "directory does not match"):
            self.generate()

    def test_workshop_mapping_conflict(self):
        self.add_patch()
        self.metadata["name"] = "Other Patch"
        self.metadata["target"]["workshopId"] = "12345"
        self.add_patch()
        with self.assertRaisesRegex(catalogue.CatalogueError, "conflicting Workshop"):
            self.generate()

    def test_casefold_collisions(self):
        self.add_patch()
        self.metadata["name"] = "CATALOGUE EXAMPLE"
        self.add_patch()
        with self.assertRaisesRegex(catalogue.CatalogueError, "collision"):
            self.generate()

    def test_modid_casefold_collision(self):
        self.add_patch()
        self.metadata["target"]["modId"] = "BaseMod"
        self.metadata["name"] = "Distinct Patch"
        self.add_patch()
        with self.assertRaisesRegex(catalogue.CatalogueError, "differ only by case"):
            self.generate()

    def test_symlinks_rejected_in_mods_and_root_indexes(self):
        destination = self.add_patch()
        link = destination / "link"
        link.symlink_to(ROOT / "README.md")
        with self.assertRaisesRegex(catalogue.CatalogueError, "symlinks"):
            self.generate()
        link.unlink()
        (self.root / "index.json").symlink_to(self.root / "mods", target_is_directory=True)
        with self.assertRaisesRegex(catalogue.CatalogueError, "real file"):
            self.generate()

    def test_missing_jar_and_orphan_directory_fail(self):
        destination = self.add_patch()
        (destination / self.metadata["jar"]).unlink()
        with self.assertRaisesRegex(catalogue.CatalogueError, "missing JAR"):
            self.generate()
        (destination / "metadata.json").unlink()
        with self.assertRaisesRegex(catalogue.CatalogueError, "missing metadata"):
            self.generate()

    def test_manifest_mismatch(self):
        destination = self.add_patch()
        for changes in ({"name": "wrong"}, {"version": "wrong"}, {"description": "wrong"},
                        {"dependencies": ["BaseMod"]}, {"modid": "basemod"}):
            self.rewrite_jar(destination, self.metadata, changes=changes)
            with self.subTest(changes=changes), self.assertRaises(catalogue.CatalogueError):
                self.generate()

    def test_optional_descriptor_is_accepted_but_mismatch_rejected(self):
        destination = self.add_patch()
        self.rewrite_jar(destination, self.metadata, descriptor=False)
        self.generate()
        self.rewrite_jar(destination, self.metadata, extra={"agent-patch.json": b'{"parent_mod_id":"wrong"}'})
        with self.assertRaisesRegex(catalogue.CatalogueError, "mismatch"):
            self.generate()

    def test_corrupt_or_manifest_only_jar_rejected(self):
        destination = self.add_patch()
        (destination / self.metadata["jar"]).write_bytes(b"not a zip")
        with self.assertRaises(catalogue.CatalogueError):
            self.generate()
        with zipfile.ZipFile(destination / self.metadata["jar"], "w") as jar:
            jar.writestr("ModTheSpire.json", (EXAMPLE / "src/ModTheSpire.json").read_bytes())
        with self.assertRaisesRegex(catalogue.CatalogueError, "compiled classes"):
            self.generate()

    def test_jar_unsafe_paths_rejected(self):
        destination = self.add_patch()
        for name in ("../escape.class", "/absolute.class", "C:/absolute.class", "a\\b.class"):
            self.rewrite_jar(destination, self.metadata, extra={name: b"data"})
            with self.subTest(name=name), self.assertRaisesRegex(catalogue.CatalogueError, "unsafe JAR"):
                self.generate()

    def test_jar_symlink_rejected(self):
        destination = self.add_patch()
        with zipfile.ZipFile(destination / self.metadata["jar"], "a") as jar:
            info = zipfile.ZipInfo("link")
            info.external_attr = 0o120777 << 16
            jar.writestr(info, "../../outside")
        with self.assertRaisesRegex(catalogue.CatalogueError, "symlinks in JAR"):
            self.generate()

    def test_duplicate_json_keys_and_oversized_json_rejected(self):
        with self.assertRaisesRegex(catalogue.CatalogueError, "duplicate"):
            catalogue.parse_json(b'{"name":"one","name":"two"}', "metadata")
        with self.assertRaisesRegex(catalogue.CatalogueError, "exceeds"):
            catalogue.parse_json(b" " * (catalogue.MAX_JSON_BYTES + 1), "metadata")

    def test_reserved_index_modid_rejected(self):
        self.metadata["target"]["modId"] = "index"
        with self.assertRaisesRegex(catalogue.CatalogueError, "reserved"):
            catalogue.validate_metadata(self.metadata)

    def test_size_and_entry_count_budgets(self):
        self.add_patch()
        for constant, value in (("MAX_JAR_BYTES", 1), ("MAX_ENTRIES", 1), ("MAX_EXPANDED_BYTES", 1)):
            with self.subTest(constant=constant), mock.patch.object(catalogue, constant, value):
                with self.assertRaises(catalogue.CatalogueError):
                    self.generate()

    def test_duplicate_jar_entries(self):
        destination = self.add_patch()
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(destination / self.metadata["jar"], "a") as jar:
                jar.writestr("ModTheSpire.json", b"{}")
        with self.assertRaisesRegex(catalogue.CatalogueError, "duplicate JAR"):
            self.generate()

    def test_corrupt_class_crc_rejected(self):
        destination = self.add_patch()
        path = destination / self.metadata["jar"]
        with zipfile.ZipFile(path) as jar:
            entry = next(item for item in jar.infolist() if item.filename.endswith(".class"))
        data = bytearray(path.read_bytes())
        # ZIP_STORED fixture: mutate a class payload, not the central directory.
        offset = entry.header_offset
        name_size = int.from_bytes(data[offset + 26:offset + 28], "little")
        extra_size = int.from_bytes(data[offset + 28:offset + 30], "little")
        data[offset + 30 + name_size + extra_size] ^= 0x01
        path.write_bytes(data)
        with self.assertRaisesRegex(catalogue.CatalogueError, "CRC"):
            self.generate()

    def test_metadata_only_or_unexpected_files_in_mods(self):
        (self.root / "mods/README.md").write_text("not a mod directory")
        with self.assertRaisesRegex(catalogue.CatalogueError, "unexpected file"):
            self.generate()

    def test_ref_with_slash_is_one_encoded_url_component(self):
        self.add_patch()
        self.generate(ref="feature/catalogue")
        self.assertIn("/feature%2Fcatalogue/", self.index()["patches"][0]["download_url"])

    def test_custom_repository_and_immutable_ref(self):
        self.add_patch()
        self.generate(repository="example/catalogue", ref="0123456789abcdef")
        self.assertIn("/example/catalogue/0123456789abcdef/", self.index()["patches"][0]["download_url"])

    def test_custom_jar_filename_stays_beside_metadata_and_source(self):
        self.metadata["jar"] = "我的补丁 2.0.JAR"
        destination = self.add_patch()
        (destination / "src").mkdir()
        (destination / "src/metadata.json").write_text('{"not":"catalogue metadata"}')
        self.assertEqual(catalogue.discover_metadata(self.root / "mods"), [destination / "metadata.json"])
        self.assertEqual(self.generate(), 1)
        entry = self.index()["patches"][0]
        self.assertEqual(entry["jar"], "我的补丁 2.0.JAR")
        self.assertTrue(unquote(entry["download_url"]).endswith("/我的补丁 2.0.JAR"))

    def test_invalid_jar_filename_rejected(self):
        for name in ("../escape.jar", "src/nested.jar", "src\\nested.jar", "/absolute.jar", "no-extension", "file.zip"):
            self.metadata["jar"] = name
            with self.subTest(name=name), self.assertRaises(catalogue.CatalogueError):
                catalogue.validate_metadata(self.metadata)

    def test_changing_jar_filename_updates_download_for_same_entry(self):
        self.add_patch()
        self.generate()
        self.metadata["jar"] = "UpdatedPatch.jar"
        self.add_patch()
        self.assertEqual(self.generate(), 1)
        self.assertTrue(self.index()["patches"][0]["download_url"].endswith("/UpdatedPatch.jar"))

    def test_detail_index_includes_description_without_legacy_path_or_url_fields(self):
        self.add_patch()
        self.generate()
        entry = self.index()["patches"][0]
        self.assertEqual(entry["description"], self.metadata["description"])
        self.assertIn("download_url", entry)
        for key in ("path", "url", "download", "metadataUrl", "id"):
            self.assertNotIn(key, entry)
        self.assertFalse((self.root / "catalogue").exists())

    def test_readme_list_is_generated_and_preserves_intro(self):
        self.add_patch()
        readme = self.root / "README.md"
        readme.write_text("# 简介\n\n自定义简介。\n\n<!-- MOD_LIST_START -->\n旧列表\n<!-- MOD_LIST_END -->\n", encoding="utf-8")
        self.generate()
        document = readme.read_text(encoding="utf-8")
        self.assertTrue(document.startswith("# 简介\n\n自定义简介。\n\n"))
        self.assertIn(self.metadata["description"], document)
        self.assertIn("Catalogue Example 1\\.0\\.0（示例）", document)
        self.assertIn(self.index()["patches"][0]["download_url"], document)
        self.assertNotIn("旧列表", document)
        self.generate(check=True)
        self.generate()
        self.assertEqual(document, readme.read_text(encoding="utf-8"))

    def test_source_metadata_is_unchanged_by_generation(self):
        destination = self.add_patch()
        path = destination / "metadata.json"
        before = path.read_bytes()
        self.generate()
        self.assertEqual(path.read_bytes(), before)

    def test_readme_failure_does_not_partially_update_indexes(self):
        self.add_patch()
        self.generate()
        previous = (self.root / "index.json").read_bytes()
        self.metadata["version"] = "2.0.0"
        self.add_patch()
        (self.root / "README.md").write_text("Missing markers")
        with self.assertRaises(catalogue.CatalogueError):
            self.generate()
        self.assertEqual((self.root / "index.json").read_bytes(), previous)

    def test_readme_table_escapes_player_input(self):
        self.metadata["description"] = '| broken\n<script>alert(1)</script> [link](https://evil.example)'
        self.add_patch()
        self.generate()
        document = (self.root / "README.md").read_text(encoding="utf-8")
        self.assertIn("\\| broken<br>&lt;script&gt;", document)
        self.assertNotIn("<script>", document)
        self.assertNotIn("[link](https://evil.example)", document)

    def test_deletion_removes_readme_entry(self):
        destination = self.add_patch()
        self.generate()
        shutil.rmtree(destination)
        self.generate()
        document = (self.root / "README.md").read_text(encoding="utf-8")
        self.assertIn("暂无模组。", document)
        self.assertNotIn("Catalogue Example", document)

    def test_readme_stale_check_and_missing_markers(self):
        self.add_patch()
        self.generate()
        readme = self.root / "README.md"
        readme.write_text(readme.read_text(encoding="utf-8").replace("Catalogue Example", "Stale"), encoding="utf-8")
        with self.assertRaisesRegex(catalogue.CatalogueError, "README mod list is stale"):
            self.generate(check=True)
        readme.write_text("custom text without markers")
        with self.assertRaisesRegex(catalogue.CatalogueError, "marker pair"):
            self.generate()

    def test_unrelated_root_json_is_preserved_when_removing_old_group(self):
        destination = self.add_patch()
        unrelated = self.root / "settings.json"
        unrelated.write_text('{"custom":"settings"}')
        self.generate()
        shutil.rmtree(destination)
        self.generate()
        self.assertEqual(unrelated.read_text(), '{"custom":"settings"}')
        self.assertFalse((self.root / "mods/basemod/index.json").exists())

    def test_unrelated_mod_index_collision_is_rejected_before_writes(self):
        self.add_patch()
        collision = self.root / "mods/basemod/index.json"
        collision.write_text('{"custom":"data"}')
        with self.assertRaisesRegex(catalogue.CatalogueError, "unrelated JSON"):
            self.generate()
        self.assertEqual(collision.read_text(), '{"custom":"data"}')
        self.assertFalse((self.root / "index.json").exists())

    def test_previous_index_cannot_escape_root_when_cleaning(self):
        self.add_patch()
        previous = {"schemaVersion": 1, "count": 1, "mods": [{"target": {"modId": "../outside"}}]}
        (self.root / "index.json").write_bytes(catalogue.encode_json(previous))
        with self.assertRaises(catalogue.CatalogueError):
            self.generate()
        self.assertFalse((self.root / "mods/basemod/index.json").exists())

    def test_migration_removes_only_legacy_root_indexes(self):
        self.add_patch()
        self.generate()
        group = self.index()
        legacy = {"schemaVersion": 1, "count": 1, "mods": group["patches"]}
        (self.root / "index.json").write_bytes(catalogue.encode_json(legacy))
        (self.root / "basemod.json").write_bytes(catalogue.encode_json(group))
        unrelated = self.root / "settings.json"
        unrelated.write_text('{"custom":"settings"}')
        self.generate()
        self.assertFalse((self.root / "basemod.json").exists())
        self.assertEqual(self.index(), group)
        self.assertEqual(self.index("index")["schemaVersion"], 2)
        self.assertEqual(unrelated.read_text(), '{"custom":"settings"}')
        self.generate(check=True)

    def test_unrelated_root_modid_json_is_preserved(self):
        self.add_patch()
        unrelated = self.root / "basemod.json"
        unrelated.write_text('{"custom":"data"}')
        self.generate()
        self.assertEqual(unrelated.read_text(), '{"custom":"data"}')
        self.generate(check=True)

    def test_mod_index_download_does_not_contain_other_mods(self):
        self.add_patch()
        other = copy.deepcopy(self.metadata)
        other["target"].update(modId="AnotherMod", workshopId="123456789")
        self.add_patch(other)
        self.generate()
        document = self.index()
        self.assertEqual(document["count"], 1)
        self.assertTrue(all(entry["target"]["modId"] == "basemod" for entry in document["patches"]))
        self.assertEqual(self.index("index")["count"], 2)
        self.assertNotIn("AnotherMod", json.dumps(document))

    def test_mod_index_failure_does_not_update_root_or_readme(self):
        self.add_patch()
        self.generate()
        index_before = (self.root / "index.json").read_bytes()
        readme_before = (self.root / "README.md").read_bytes()
        self.metadata["version"] = "2.0.0"
        self.add_patch()
        (self.root / "mods/basemod/index.json").write_bytes(b'{"custom":true}')
        with self.assertRaisesRegex(catalogue.CatalogueError, "unrelated JSON"):
            self.generate()
        self.assertEqual((self.root / "index.json").read_bytes(), index_before)
        self.assertEqual((self.root / "README.md").read_bytes(), readme_before)

    def test_v2_previous_index_cannot_escape_root_when_cleaning(self):
        self.add_patch()
        previous = {"schemaVersion": 2, "count": 0, "mods": {"../outside": {}}}
        (self.root / "index.json").write_bytes(catalogue.encode_json(previous))
        with self.assertRaises(catalogue.CatalogueError):
            self.generate()
        self.assertFalse((self.root / "mods/basemod/index.json").exists())

    def test_badge_index_counts_exact_wildcard_and_hidden_example_versions(self):
        self.metadata["example"] = False
        self.metadata["target"]["versions"] = ["1.0", "2.0"]
        self.add_patch()
        wildcard = copy.deepcopy(self.metadata)
        wildcard["name"] = "All Versions"
        wildcard["target"]["versions"] = ["*"]
        self.add_patch(wildcard)
        hidden = copy.deepcopy(self.metadata)
        hidden.update(name="Hidden Example", example=True)
        self.add_patch(hidden)
        other = copy.deepcopy(self.metadata)
        other["target"].update(modId="OtherMod", workshopId="12345", versions=["3.0"])
        self.add_patch(other)
        self.generate()
        groups = self.index("index")["mods"]
        summary = groups["basemod"]
        self.assertEqual(summary["count"], 3)  # All records, including examples.
        self.assertEqual(summary["versions"], {"*": 1, "1.0": 1, "2.0": 1})
        self.assertEqual(groups["OtherMod"]["versions"], {"3.0": 1})
        for version, expected in (("1.0", 2), ("2.0", 2), ("3.0", 1)):
            counts = summary["versions"]
            self.assertEqual(counts.get(version, 0) + counts.get("*", 0), expected)
        self.assertNotIn("BaseMod", groups)
        self.assertNotIn("NotInCatalogue", groups)

    def test_example_only_group_has_no_badge(self):
        self.metadata["target"]["versions"] = ["*"]
        self.add_patch()
        self.generate()
        self.assertEqual(self.index("index")["mods"]["basemod"]["versions"], {})
        self.assertTrue(self.index()["patches"][0]["example"])

    def test_badge_index_does_not_duplicate_patch_details(self):
        self.add_patch()
        self.generate()
        group = self.index("index")["mods"]["basemod"]
        self.assertEqual(set(group), {"workshopId", "count", "versions", "sha256"})
        self.assertNotIn(self.metadata["description"], (self.root / "index.json").read_text(encoding="utf-8"))
        self.assertEqual(self.index()["patches"][0]["description"], self.metadata["description"])
        self.assertIn("download_url", self.index()["patches"][0])

    def test_detail_hash_changes_even_when_badge_counts_are_unchanged(self):
        self.metadata["example"] = False
        self.add_patch()
        self.generate()
        before = self.index("index")["mods"]["basemod"]
        self.metadata["description"] = "更新后的补丁简介。"
        self.add_patch()
        self.generate()
        after = self.index("index")["mods"]["basemod"]
        self.assertEqual(before["count"], after["count"])
        self.assertEqual(before["versions"], after["versions"])
        self.assertNotEqual(before["sha256"], after["sha256"])
        self.assertEqual(after["sha256"], hashlib.sha256((self.root / "mods/basemod/index.json").read_bytes()).hexdigest())
        self.generate(check=True)


if __name__ == "__main__":
    unittest.main()
