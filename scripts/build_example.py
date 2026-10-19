#!/usr/bin/env python3
"""Rebuild the harmless Java 8 example; do not redistribute ModTheSpire itself."""

import argparse
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
EXAMPLE = ROOT / "mods/basemod/Catalogue Example-ModinMobileSTS"


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--modthespire", required=True, type=Path)
    args = parser.parse_args()
    jar_name = json.loads((EXAMPLE / "metadata.json").read_text(encoding="utf-8"))["jar"]
    if not args.modthespire.is_file():
        parser.error("ModTheSpire JAR does not exist")
    javac = shutil.which("javac")
    if not javac:
        parser.error("javac from JDK 11+ is required")
    temporary = ROOT / "agent-tmp"
    temporary.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="build-example-", dir=temporary) as directory:
        classes = Path(directory) / "classes"
        classes.mkdir()
        subprocess.run([
            javac, "--release", "8", "-g:none", "-encoding", "UTF-8", "-classpath", str(args.modthespire.resolve()),
            "-d", str(classes), *map(str, sorted((EXAMPLE / "src").rglob("*.java"))),
        ], check=True)
        files = {p.relative_to(classes).as_posix(): p for p in classes.rglob("*.class")}
        files.update({name: EXAMPLE / "src" / name for name in ("ModTheSpire.json", "agent-patch.json")})
        output = Path(directory) / jar_name
        with zipfile.ZipFile(output, "w", compression=zipfile.ZIP_STORED) as jar:
            for name, path in sorted(files.items()):
                info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
                info.create_system = 3
                info.external_attr = 0o100644 << 16
                jar.writestr(info, path.read_bytes())
        shutil.copyfile(output, EXAMPLE / jar_name)
    print(f"Built {EXAMPLE / jar_name}")


if __name__ == "__main__":
    main()
