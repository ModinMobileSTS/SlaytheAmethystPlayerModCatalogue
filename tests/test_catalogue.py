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
        shutil.copyfile(EXAMPLE / "patch.jar", destination / "patch.jar")
        self.rewrite_jar(destination, metadata)
        return destination

    def rewrite_jar(self, destination, metadata, changes=None, extra=None, descriptor=True):
        with zipfile.ZipFile(EXAMPLE / "patch.jar") as jar:
            files = {name: jar.read(name) for name in jar.namelist()}
        manifest = json.loads(files["ModTheSpire.json"])
        manifest.update({field: metadata[field] for field in ("name", "description", "version")})
        manifest["dependencies"] = [metadata["target"]["modId"]]
        manifest.update(changes or {})
        files["ModTheSpire.json"] = catalogue.encode_json(manifest)
        files.pop("agent-patch.json")
        if descriptor:
            files["agent-patch.json"] = (EXAMPLE / "agent-patch.json").read_bytes()
            data = json.loads(files["agent-patch.json"])
            data.update({key: metadata[key] for key in ("name", "description", "version")})
            data.update(parent_mod_id=metadata["target"]["modId"], patch_mod_id=manifest["modid"])
            files["agent-patch.json"] = catalogue.encode_json(data)
        files.update(extra or {})
        with zipfile.ZipFile(destination / "patch.jar", "w") as jar:
            for name, data in files.items():
                jar.writestr(name, data)

    def generate(self, **kwargs):
        return catalogue.generate(self.root, **kwargs)

    def index(self, name="basemod"):
        return json.loads((self.root / "catalogue" / f"{name}.json").read_bytes())

    def test_real_example_jar_and_java8_class(self):
        result = catalogue.validate_jar(EXAMPLE / "patch.jar", self.metadata)
        self.assertEqual(result["patchModId"], "amethyst_agent_basemod_catalogue_example")
        with zipfile.ZipFile(EXAMPLE / "patch.jar") as jar:
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
        self.assertEqual(entry["download"]["sha256"], hashlib.sha256((destination / "patch.jar").read_bytes()).hexdigest())
        self.assertEqual(entry["download"]["size"], (destination / "patch.jar").stat().st_size)
        self.assertIn("Catalogue%20Example", entry["download"]["url"])
        self.assertEqual(self.index("index")["mods"][0]["modId"], "basemod")

    def test_multiple_mods_grouped_and_sorted(self):
        self.add_patch()
        other = copy.deepcopy(self.metadata)
        other["target"].update(modId="AnotherMod", workshopId="123456789", versions=["2.0"])
        self.add_patch(other)
        other["name"] = "Another Patch"
        self.add_patch(other)
        self.assertEqual(self.generate(), 3)
        self.assertEqual(self.index("AnotherMod")["count"], 2)
        self.assertEqual([m["modId"] for m in self.index("index")["mods"]], ["AnotherMod", "basemod"])

    def test_unicode_names_preserve_case_and_encode_url(self):
        self.metadata["name"] = "中文补丁"
        self.metadata["author"]["name"] = "作者甲"
        self.metadata["target"]["modId"] = "CaseSensitiveMod"
        self.add_patch()
        self.generate()
        entry = self.index("CaseSensitiveMod")["patches"][0]
        self.assertIn("中文补丁-作者甲", unquote(entry["download"]["url"]))
        self.assertIn("CaseSensitiveMod", entry["download"]["url"])

    def test_repeated_generation_is_byte_identical(self):
        self.add_patch()
        self.generate()
        before = {p.name: p.read_bytes() for p in (self.root / "catalogue").iterdir()}
        self.generate()
        self.assertEqual(before, {p.name: p.read_bytes() for p in (self.root / "catalogue").iterdir()})
        self.assertEqual(self.generate(check=True), 1)

    def test_same_name_and_author_overwrite_one_entry(self):
        destination = self.add_patch()
        self.generate()
        old_hash = self.index()["patches"][0]["download"]["sha256"]
        self.metadata["version"] = "2.0.0"
        self.assertEqual(self.add_patch(), destination)
        self.generate()
        self.assertEqual(self.index()["count"], 1)
        self.assertEqual(self.index()["patches"][0]["version"], "2.0.0")
        self.assertNotEqual(self.index()["patches"][0]["download"]["sha256"], old_hash)

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
        self.assertFalse((self.root / "catalogue/basemod.json").exists())
        self.assertEqual(self.index("index"), {"schemaVersion": 1, "count": 0, "mods": []})

    def test_stale_check_fails_without_writing(self):
        self.add_patch()
        with self.assertRaisesRegex(catalogue.CatalogueError, "stale"):
            self.generate(check=True)
        self.assertFalse((self.root / "catalogue").exists())

    def test_invalid_upload_does_not_modify_old_catalogue(self):
        destination = self.add_patch()
        self.generate()
        previous = (self.root / "catalogue/basemod.json").read_bytes()
        (destination / "patch.jar").write_bytes(b"broken upload")
        with self.assertRaises(catalogue.CatalogueError):
            self.generate()
        self.assertEqual(previous, (self.root / "catalogue/basemod.json").read_bytes())

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

    def test_symlinks_rejected_in_mods_and_catalogue(self):
        destination = self.add_patch()
        link = destination / "link"
        link.symlink_to(ROOT / "README.md")
        with self.assertRaisesRegex(catalogue.CatalogueError, "symlinks"):
            self.generate()
        link.unlink()
        (self.root / "catalogue").symlink_to(self.root / "mods", target_is_directory=True)
        with self.assertRaisesRegex(catalogue.CatalogueError, "symlink"):
            self.generate()

    def test_missing_jar_and_orphan_directory_fail(self):
        destination = self.add_patch()
        (destination / "patch.jar").unlink()
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
        (destination / "patch.jar").write_bytes(b"not a zip")
        with self.assertRaises(catalogue.CatalogueError):
            self.generate()
        with zipfile.ZipFile(destination / "patch.jar", "w") as jar:
            jar.writestr("ModTheSpire.json", (EXAMPLE / "ModTheSpire.json").read_bytes())
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
        with zipfile.ZipFile(destination / "patch.jar", "a") as jar:
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
            with zipfile.ZipFile(destination / "patch.jar", "a") as jar:
                jar.writestr("ModTheSpire.json", b"{}")
        with self.assertRaisesRegex(catalogue.CatalogueError, "duplicate JAR"):
            self.generate()

    def test_corrupt_class_crc_rejected(self):
        destination = self.add_patch()
        path = destination / "patch.jar"
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
        self.assertIn("/feature%2Fcatalogue/", self.index()["patches"][0]["download"]["url"])

    def test_custom_repository_and_immutable_ref(self):
        self.add_patch()
        self.generate(repository="example/catalogue", ref="0123456789abcdef")
        self.assertIn("/example/catalogue/0123456789abcdef/", self.index()["patches"][0]["download"]["url"])


if __name__ == "__main__":
    unittest.main()
