#!/usr/bin/env python3
"""Validate untrusted patch metadata/JARs and build deterministic JSON indexes.

This module never imports or executes uploaded Java bytecode. Cloud functions
can reuse validate_metadata() and patch_directory() before committing uploads.
"""

from __future__ import annotations

import argparse
import hashlib
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
    require(target["modId"].casefold() != "index", "modId 'index' is reserved for catalogue/index.json")
    require(isinstance(target["workshopId"], str) and WORKSHOP_ID.fullmatch(target["workshopId"]) is not None,
            "workshopId must be a positive decimal string")
    versions = target["versions"]
    require(isinstance(versions, list) and 1 <= len(versions) <= 100, "target.versions must be a nonempty array")
    for version in versions:
        text(version, "target version")
    require(len(versions) == len(set(versions)), "duplicate target versions")
    require("*" not in versions or versions == ["*"], "wildcard must be the only target version")
    require(metadata["jar"] == "patch.jar", "jar must be patch.jar")
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


def collect(root: Path, repository: str, ref: str) -> tuple[dict[str, bytes], int]:
    mods_root = root / "mods"
    require(mods_root.is_dir() and not mods_root.is_symlink(), "mods/ must be a real directory")
    # Reject symlinks anywhere, including extra source/doc files; never read outside the checkout.
    for directory, directories, files in os.walk(mods_root, followlinks=False):
        for name in directories + files:
            require(not (Path(directory) / name).is_symlink(), "symlinks under mods/ are unsupported")
    grouped: dict[str, list[dict]] = {}
    workshop_by_mod: dict[str, str] = {}
    casefold_paths: set[str] = set()
    for mod_dir in sorted(mods_root.iterdir()):
        require(mod_dir.is_dir(), f"unexpected file under mods/: {mod_dir.name}")
        for patch_dir in sorted(mod_dir.iterdir()):
            require(patch_dir.is_dir(), f"unexpected file under {mod_dir}")
            metadata_path = patch_dir / "metadata.json"
            require(metadata_path.is_file(), f"missing metadata: {metadata_path}")
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
            jar_info = validate_jar(patch_dir / "patch.jar", metadata)
            jar_path = (relative / "patch.jar").as_posix()
            entry = {
                **metadata,
                "id": relative.as_posix(),
                "patchModId": jar_info.pop("patchModId"),
                "metadataUrl": raw_url(repository, ref, metadata_path.relative_to(root).as_posix()),
                "download": {"path": jar_path, "url": raw_url(repository, ref, jar_path), **jar_info},
            }
            grouped.setdefault(mod_id, []).append(entry)
    require(len({mod_id.casefold() for mod_id in grouped}) == len(grouped), "modIds differ only by case")
    output = {}
    summaries = []
    for mod_id, entries in sorted(grouped.items()):
        entries.sort(key=lambda entry: entry["id"])
        index_path = f"catalogue/{mod_id}.json"
        output[f"{mod_id}.json"] = encode_json({
            "schemaVersion": 1, "modId": mod_id, "workshopId": workshop_by_mod[mod_id],
            "count": len(entries), "patches": entries,
        })
        summaries.append({"modId": mod_id, "workshopId": workshop_by_mod[mod_id], "count": len(entries),
                          "path": index_path, "url": raw_url(repository, ref, index_path)})
    output["index.json"] = encode_json({"schemaVersion": 1, "count": sum(map(len, grouped.values())), "mods": summaries})
    # Avoid a mod named 'index' replacing the global index.
    return output, sum(map(len, grouped.values()))


def encode_json(value: object) -> bytes:
    return (json.dumps(value, ensure_ascii=False, indent=2, allow_nan=False) + "\n").encode("utf-8")


def generate(root: Path, repository: str = REPOSITORY, ref: str = "main", check: bool = False) -> int:
    require(re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository) is not None, "invalid repository")
    text(ref, "ref", 255)
    output, count = collect(root, repository, ref)  # Validate everything before touching existing indexes.
    catalogue = root / "catalogue"
    require(not catalogue.is_symlink(), "catalogue/ cannot be a symlink")
    if catalogue.exists():
        require(catalogue.is_dir(), "catalogue/ must be a directory")
        require(all(p.is_file() and not p.is_symlink() and p.suffix == ".json" for p in catalogue.iterdir()),
                "catalogue/ may contain only generated JSON files")
    existing = {p.name: p.read_bytes() for p in catalogue.glob("*.json")}
    if check:
        require(existing == output, "catalogue is stale; run python3 scripts/catalogue.py")
    else:
        catalogue.mkdir(exist_ok=True)
        for name in existing.keys() - output.keys():
            (catalogue / name).unlink()
        for name, data in output.items():
            (catalogue / name).write_bytes(data)
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
    except (CatalogueError, OSError) as error:
        print(f"catalogue: {error}", file=sys.stderr)
        return 1
    print(f"catalogue: {count} patch(es) validated; indexes {'checked' if args.check else 'generated'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
