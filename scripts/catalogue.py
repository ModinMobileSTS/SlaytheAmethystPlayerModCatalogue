#!/usr/bin/env python3
"""Validate untrusted patch metadata/JARs and build deterministic JSON indexes.

This module never imports or executes uploaded Java bytecode. Cloud functions
can reuse validate_metadata() and patch_directory() before committing uploads.
"""

from __future__ import annotations

import argparse
import hashlib
import html
import json
import os
from pathlib import Path, PurePosixPath
import re
import sys
import unicodedata
from urllib.parse import quote
import zipfile

REPOSITORY = "ModinMobileSTS/SlaytheAmethystPlayerModCatalogue"
MAX_JAR_BYTES = 25 * 1024 * 1024
MAX_JSON_BYTES = 64 * 1024
MAX_ENTRIES = 4096
MAX_EXPANDED_BYTES = 512 * 1024 * 1024
EMAIL = re.compile(r"[^\s<>@]+@[^\s<>@]+\.[^\s<>@]+\Z")
WORKSHOP_ID = re.compile(r"[1-9][0-9]{0,19}\Z")
MOD_LIST_START = "<!-- MOD_LIST_START -->"
MOD_LIST_END = "<!-- MOD_LIST_END -->"
README_INTRO = "# Slay the Amethyst Player Mod Catalogue\n\n玩家分享的 AI 补丁模组目录，供 Slay the Amethyst 浏览、下载和分享适用的模组补丁。\n"


class CatalogueError(ValueError):
    """Invalid upload or repository layout."""


def require(condition: bool, message: str) -> None:
    if not condition:
        raise CatalogueError(message)


def object_fields(value: object, required: set[str], optional: set[str] | None = None) -> dict:
    require(isinstance(value, dict), "expected a JSON object")
    require(required <= value.keys(), f"missing fields: {sorted(required - value.keys())}")
    require(value.keys() <= required | (optional or set()), "unknown metadata fields")
    return value


def text(value: object, label: str, maximum: int = 120, multiline: bool = False) -> str:
    require(isinstance(value, str), f"{label} must be a string")
    require(0 < len(value) <= maximum and value == value.strip(), f"invalid {label} length/whitespace")
    require(unicodedata.normalize("NFC", value) == value, f"{label} must use Unicode NFC")
    require(
        all(not unicodedata.category(c).startswith("C") or (multiline and c in "\n\t") for c in value),
        f"{label} contains control/format characters",
    )
    return value


def path_segment(value: object, label: str) -> str:
    value = text(value, label)
    require(not any(c in '/\\<>:"|?*' for c in value), f"unsafe {label} path segment")
    require(value not in {".", ".."} and not value.endswith("."), f"unsafe {label} path segment")
    require(not value.startswith("."), f"hidden {label} path segment")
    reserved = {"CON", "PRN", "AUX", "NUL"} | {f"{p}{i}" for p in ("COM", "LPT") for i in range(1, 10)}
    require(value.split(".")[0].upper() not in reserved, f"reserved {label} path segment")
    return value


def validate_metadata(metadata: object) -> dict:
    metadata = object_fields(
        metadata, {"schemaVersion", "name", "description", "version", "author", "target", "jar"}, {"example"}
    )
    require(type(metadata["schemaVersion"]) is int and metadata["schemaVersion"] == 1, "unsupported schemaVersion")
    path_segment(metadata["name"], "name")
    text(metadata["description"], "description", 4000, multiline=True)
    text(metadata["version"], "version")
    author = object_fields(metadata["author"], {"name", "email"})
    path_segment(author["name"], "author.name")
    email = text(author["email"], "author.email", 254)
    require(EMAIL.fullmatch(email) is not None, "invalid author.email")
    target = object_fields(metadata["target"], {"modId", "workshopId", "versions"})
    path_segment(target["modId"], "target.modId")
    require(target["modId"].casefold() != "index", "modId 'index' is reserved for root index.json")
    require(isinstance(target["workshopId"], str) and WORKSHOP_ID.fullmatch(target["workshopId"]) is not None,
            "workshopId must be a positive decimal string")
    versions = target["versions"]
    require(isinstance(versions, list) and 1 <= len(versions) <= 100, "target.versions must be a nonempty array")
    for version in versions:
        text(version, "target version")
    require(len(versions) == len(set(versions)), "duplicate target versions")
    require("*" not in versions or versions == ["*"], "wildcard must be the only target version")
    jar_name = path_segment(metadata["jar"], "jar")
    require(jar_name.lower().endswith(".jar"), "jar must be a .jar filename")
    require(len(jar_name.encode("utf-8")) <= 240, "jar filename exceeds 240 UTF-8 bytes")
    require("example" not in metadata or type(metadata["example"]) is bool, "example must be a boolean")
    patch_directory(metadata)
    return metadata


def patch_directory(metadata: dict) -> Path:
    """Names are used verbatim after validation; never silently slug/lowercase them."""
    mod_id = path_segment(metadata["target"]["modId"], "target.modId")
    name = path_segment(metadata["name"], "name")
    author = path_segment(metadata["author"]["name"], "author.name")
    segment = f"{name}-{author}"
    require(len(segment.encode("utf-8")) <= 240, "name-author directory exceeds 240 UTF-8 bytes")
    require(len(mod_id.encode("utf-8")) <= 240, "modId directory exceeds 240 UTF-8 bytes")
    return Path("mods", mod_id, segment)


def unique_object(pairs: list[tuple[str, object]]) -> dict:
    value = {}
    for key, item in pairs:
        require(key not in value, f"duplicate JSON key: {key}")
        value[key] = item
    return value


def parse_json(data: bytes, label: str) -> object:
    require(len(data) <= MAX_JSON_BYTES, f"{label} exceeds {MAX_JSON_BYTES} bytes")
    try:
        return json.loads(data.decode("utf-8"), object_pairs_hook=unique_object)
    except (UnicodeError, ValueError) as error:
        raise CatalogueError(f"invalid {label}: {error}") from error


def validate_jar(path: Path, metadata: dict) -> dict:
    require(path.is_file(), f"missing JAR: {path}")
    size = path.stat().st_size
    require(0 < size <= MAX_JAR_BYTES, f"JAR must be 1..{MAX_JAR_BYTES} bytes")
    try:
        with zipfile.ZipFile(path) as jar:
            entries = jar.infolist()
            require(0 < len(entries) <= MAX_ENTRIES, "too many JAR entries")
            require(sum(entry.file_size for entry in entries) <= MAX_EXPANDED_BYTES, "JAR expands beyond limit")
            names = [entry.filename for entry in entries]
            require(len(names) == len(set(names)), "duplicate JAR entries")
            for entry in entries:
                name = entry.filename
                require(not name.startswith("/") and "\\" not in name and ":" not in name
                        and ".." not in PurePosixPath(name).parts, "unsafe JAR entry path")
                require(not (entry.flag_bits & 1), "encrypted JAR entries are unsupported")
                require((entry.external_attr >> 16) & 0o170000 != 0o120000, "symlinks in JAR are unsupported")
            # Stream every member once to verify decompression and CRC without
            # retaining an expanded JAR (or executing any Java) in memory.
            expanded = 0
            for entry in entries:
                with jar.open(entry) as member:
                    while chunk := member.read(64 * 1024):
                        expanded += len(chunk)
                        require(expanded <= MAX_EXPANDED_BYTES, "JAR expands beyond limit")
            require("ModTheSpire.json" in names, "JAR lacks ModTheSpire.json")
            info = jar.getinfo("ModTheSpire.json")
            require(info.file_size <= MAX_JSON_BYTES, "ModTheSpire.json exceeds limit")
            manifest = parse_json(jar.read(info), "ModTheSpire.json")
            require(isinstance(manifest, dict), "ModTheSpire.json must be an object")
            patch_mod_id = text(manifest.get("modid"), "JAR modid")
            require(patch_mod_id != metadata["target"]["modId"], "patch modid must differ from target modId")
            for field in ("name", "version", "description"):
                require(manifest.get(field) == metadata[field], f"JAR {field} does not match metadata")
            dependencies = manifest.get("dependencies")
            require(isinstance(dependencies, list) and metadata["target"]["modId"] in dependencies,
                    "JAR dependencies must include exact case-sensitive target modId")
            if "agent-patch.json" in names:
                info = jar.getinfo("agent-patch.json")
                require(info.file_size <= MAX_JSON_BYTES, "agent-patch.json exceeds limit")
                descriptor = parse_json(jar.read(info), "agent-patch.json")
                require(isinstance(descriptor, dict), "agent-patch.json must be an object")
                expected = {"parent_mod_id": metadata["target"]["modId"], "patch_mod_id": patch_mod_id,
                            **{key: metadata[key] for key in ("name", "version", "description")}}
                require(all(descriptor.get(k) == v for k, v in expected.items()), "agent-patch.json mismatch")
                text(descriptor.get("patch_id"), "patch_id")
            require(any(name.endswith(".class") for name in names), "JAR contains no compiled classes")
    except (zipfile.BadZipFile, RuntimeError, NotImplementedError, OSError) as error:
        raise CatalogueError(f"invalid JAR {path}: {error}") from error
    return {"size": size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "patchModId": patch_mod_id}


def raw_url(repository: str, ref: str, path: str) -> str:
    return f"https://raw.githubusercontent.com/{repository}/{quote(ref, safe='')}/{quote(path, safe='/')}"


def discover_metadata(mods_root: Path) -> list[Path]:
    """Discover uploads from their colocated metadata, not a separate registry.

    Only mods/<modid>/<name>-<author>/metadata.json defines an entry. JSON files
    inside src/ are source resources, never additional catalogue entries.
    """
    require(mods_root.is_dir() and not mods_root.is_symlink(), "mods/ must be a real directory")
    # Reject symlinks anywhere, including extra source/doc files; never read outside the checkout.
    for directory, directories, files in os.walk(mods_root, followlinks=False):
        for name in directories + files:
            require(not (Path(directory) / name).is_symlink(), "symlinks under mods/ are unsupported")
    for mod_dir in sorted(mods_root.iterdir()):
        require(mod_dir.is_dir(), f"unexpected file under mods/: {mod_dir.name}")
        for patch_dir in sorted(mod_dir.iterdir()):
            if patch_dir.name == "index.json" and patch_dir.is_file():
                continue  # Generated per-mod index, validated before overwriting below.
            require(patch_dir.is_dir(), f"unexpected file under {mod_dir}")
            metadata_path = patch_dir / "metadata.json"
            require(metadata_path.is_file(), f"missing metadata: {metadata_path}")
    return sorted(mods_root.glob("*/*/metadata.json"))


def collect(root: Path, repository: str, ref: str) -> tuple[dict[str, bytes], int]:
    grouped: dict[str, list[dict]] = {}
    workshop_by_mod: dict[str, str] = {}
    casefold_paths: set[str] = set()
    for metadata_path in discover_metadata(root / "mods"):
        patch_dir = metadata_path.parent
        require(metadata_path.stat().st_size <= MAX_JSON_BYTES, f"metadata exceeds {MAX_JSON_BYTES} bytes")
        metadata = validate_metadata(parse_json(metadata_path.read_bytes(), str(metadata_path)))
        relative = patch_dir.relative_to(root)
        require(relative == patch_directory(metadata), f"directory does not match metadata: {relative}")
        identity = unicodedata.normalize("NFC", relative.as_posix()).casefold()
        require(identity not in casefold_paths, f"case-insensitive path collision: {relative}")
        casefold_paths.add(identity)
        mod_id = metadata["target"]["modId"]
        workshop_id = metadata["target"]["workshopId"]
        require(workshop_by_mod.get(mod_id, workshop_id) == workshop_id, f"conflicting Workshop IDs for {mod_id}")
        workshop_by_mod[mod_id] = workshop_id
        jar_info = validate_jar(patch_dir / metadata["jar"], metadata)
        jar_path = (relative / metadata["jar"]).as_posix()
        entry = {
            **metadata,
            "patchModId": jar_info.pop("patchModId"),
            "download_url": raw_url(repository, ref, jar_path),
            **jar_info,
        }
        grouped.setdefault(mod_id, []).append(entry)
    require(len({mod_id.casefold() for mod_id in grouped}) == len(grouped), "modIds differ only by case")
    output = {}
    mod_groups = {}
    for mod_id, entries in sorted(grouped.items()):
        entries.sort(key=lambda entry: (entry["name"], entry["author"]["name"]))
        detail = encode_json({
            "schemaVersion": 1, "modId": mod_id, "workshopId": workshop_by_mod[mod_id],
            "count": len(entries), "patches": entries,
        })
        output[f"mods/{mod_id}/index.json"] = detail
        versions: dict[str, int] = {}
        for entry in entries:
            if entry.get("example", False):
                continue
            for version in entry["target"]["versions"]:
                versions[version] = versions.get(version, 0) + 1
        mod_groups[mod_id] = {
            "workshopId": workshop_by_mod[mod_id], "count": len(entries),
            "versions": dict(sorted(versions.items())), "sha256": hashlib.sha256(detail).hexdigest(),
        }
    count = sum(map(len, grouped.values()))
    output["index.json"] = encode_json({"schemaVersion": 2, "count": count, "mods": mod_groups})
    return output, count


def encode_json(value: object) -> bytes:
    return (json.dumps(value, ensure_ascii=False, indent=2, allow_nan=False) + "\n").encode("utf-8")


def read_generated_indexes(root: Path, output: dict[str, bytes]) -> dict[str, bytes]:
    """Own only root index.json and the per-mod indexes derived from it.

    Validate both old and new destinations before deleting/writing anything.
    A malformed manifest must not permit path traversal or erase unrelated data.
    Read v1 once to migrate legacy root <modid>.json files to mods/<modid>/index.json.
    """
    previous_paths = {"index.json"}
    index_path = root / "index.json"
    if index_path.exists() or index_path.is_symlink():
        require(index_path.is_file() and not index_path.is_symlink(), "index.json must be a real file")
        index = json.loads(index_path.read_bytes(), object_pairs_hook=unique_object)
        require(isinstance(index, dict) and set(index) == {"schemaVersion", "count", "mods"}
                and type(index["schemaVersion"]) is int, "invalid generated index.json")
        if index["schemaVersion"] == 1:
            require(isinstance(index["mods"], list), "invalid legacy mods array in index.json")
            mod_ids = []
            for entry in index["mods"]:
                require(isinstance(entry, dict) and isinstance(entry.get("target"), dict), "invalid mod entry in index.json")
                mod_ids.append(entry["target"].get("modId"))
        else:
            require(index["schemaVersion"] == 2 and isinstance(index["mods"], dict), "invalid mods map in index.json")
            mod_ids = index["mods"]
        for value in mod_ids:
            mod_id = path_segment(value, "indexed modId")
            require(mod_id.casefold() != "index", "unsafe generated index path")
            previous_paths.add(f"{mod_id}.json" if index["schemaVersion"] == 1 else f"mods/{mod_id}/index.json")
    existing = {}
    for name in sorted(previous_paths | output.keys()):
        path = root / name
        if not path.exists() and not path.is_symlink():
            continue
        require(path.is_file() and not path.is_symlink(), f"generated index must be a real file: {name}")
        data = path.read_bytes()
        if name != "index.json":
            document = json.loads(data, object_pairs_hook=unique_object)
            mod_id = name.split("/")[1] if name.startswith("mods/") else name[:-5]
            require(isinstance(document, dict)
                    and set(document) == {"schemaVersion", "modId", "workshopId", "count", "patches"}
                    and type(document["schemaVersion"]) is int and document["schemaVersion"] == 1
                    and document["modId"] == mod_id
                    and isinstance(document["patches"], list), f"refusing to overwrite unrelated JSON: {name}")
        existing[name] = data
    return existing


def markdown_cell(value: str) -> str:
    """Treat player-provided table text as plain text, not Markdown or HTML."""
    value = html.escape(value, quote=True)
    value = re.sub(r"([\\`*_{}\[\]()#+.!|~])", r"\\\1", value)
    return value.replace("\n", "<br>").replace("\t", " ")


def render_readme(root: Path, entries: list[dict]) -> bytes:
    readme = root / "README.md"
    require(not readme.is_symlink(), "README.md cannot be a symlink")
    if readme.exists():
        require(readme.is_file(), "README.md must be a real file")
        document = readme.read_text(encoding="utf-8")
    else:
        document = f"{README_INTRO}\n{MOD_LIST_START}\n{MOD_LIST_END}\n"
    require(document.count(MOD_LIST_START) == 1 and document.count(MOD_LIST_END) == 1,
            "README.md must contain one MOD_LIST_START/MOD_LIST_END marker pair")
    before, rest = document.split(MOD_LIST_START)
    _, after = rest.split(MOD_LIST_END)
    lines = ["", "## 模组列表", ""]
    if entries:
        lines.extend(["| 补丁 | 简介 | 作者 | 适用 modid | 适用版本 | 下载 |",
                      "| --- | --- | --- | --- | --- | --- |"])
        for entry in entries:
            name = f"{entry['name']} {entry['version']}" + ("（示例）" if entry.get("example") else "")
            cells = [name, entry["description"], entry["author"]["name"], entry["target"]["modId"],
                     ", ".join(entry["target"]["versions"])]
            lines.append("| " + " | ".join(map(markdown_cell, cells)) + f" | [下载]({entry['download_url']}) |")
    else:
        lines.append("暂无模组。")
    lines.append("")
    return (before + MOD_LIST_START + "\n".join(lines) + MOD_LIST_END + after).encode("utf-8")


def generate(root: Path, repository: str = REPOSITORY, ref: str = "main", check: bool = False) -> int:
    require(re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository) is not None, "invalid repository")
    text(ref, "ref", 255)
    output, count = collect(root, repository, ref)  # Validate everything before touching existing indexes.
    existing = read_generated_indexes(root, output)
    readme_path = root / "README.md"
    entries = [entry for name, data in sorted(output.items()) if name != "index.json"
               for entry in json.loads(data)["patches"]]
    readme = render_readme(root, entries)
    if check:
        require(existing == output, "catalogue is stale; run python3 scripts/catalogue.py")
        require(readme_path.is_file() and readme_path.read_bytes() == readme, "README mod list is stale; run python3 scripts/catalogue.py")
    else:
        for name in existing.keys() - output.keys():
            (root / name).unlink()
        for name, data in output.items():
            (root / name).write_bytes(data)
        readme_path.write_bytes(readme)
    return count


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--repository", default=REPOSITORY)
    parser.add_argument("--ref", default="main", help="branch or immutable Git commit for raw URLs")
    parser.add_argument("--check", action="store_true", help="fail instead of rewriting stale indexes")
    args = parser.parse_args()
    try:
        count = generate(args.root.resolve(), args.repository, args.ref, args.check)
    except (ValueError, OSError) as error:
        print(f"catalogue: {error}", file=sys.stderr)
        return 1
    print(f"catalogue: {count} patch(es) validated; indexes {'checked' if args.check else 'generated'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
