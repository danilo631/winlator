#!/usr/bin/env python3
"""Bundle upstream-built FEXCore Wine PE modules for an Android ARM64EC Wine tree.

The archive is NOT a self-contained Wine runtime. Combine these DLLs with a
matching Bionic ARM64EC Wine build, then package Wine using package_proton_wine.py.
"""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import tarfile


def find_module(stage: Path, filename: str) -> Path:
    choices = list(stage.rglob(filename))
    if len(choices) != 1:
        raise ValueError(f"Expected one {filename} inside {stage}; found {len(choices)}")
    file = choices[0]
    if not file.is_file() or file.stat().st_size < 1024:
        raise ValueError(f"Invalid or incomplete PE DLL: {file}")
    with file.open("rb") as stream:
        dos_header = stream.read(128)
        if dos_header[:2] != b"MZ":
            raise ValueError(f"Not a PE DLL (MZ missing): {file}")
        offset = int.from_bytes(dos_header[0x3c:0x40], "little")
        if offset < 64 or offset > file.stat().st_size - 24:
            raise ValueError(f"Malformed PE pointer: {file}")
        stream.seek(offset)
        if stream.read(4) != b"PE\x00\x00":
            raise ValueError(f"Not a PE DLL (PE signature missing): {file}")
    return file


def package(arm64ec_stage: Path, wow64_stage: Path, output: Path) -> str:
    arm64ec = find_module(arm64ec_stage, "libarm64ecfex.dll")
    wow64 = find_module(wow64_stage, "libwow64fex.dll")
    output.parent.mkdir(parents=True, exist_ok=True)
    with tarfile.open(output, mode="w:xz") as tar:
        tar.add(arm64ec, arcname="lib/wine/aarch64-windows/libarm64ecfex.dll", recursive=False)
        tar.add(wow64, arcname="lib/wine/aarch64-windows/libwow64fex.dll", recursive=False)
    sha = hashlib.sha256(output.read_bytes()).hexdigest()
    output.with_name(output.name+".sha256").write_text(
        sha + "  " + output.name + "\n", encoding="utf-8")
    return sha


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--arm64ec", type=Path, required=True)
    parser.add_argument("--wow64", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if not args.output.name.endswith(".tar.xz"):
        parser.error("Output must end in .tar.xz")
    print("FEXCore PE components SHA256:", package(args.arm64ec, args.wow64, args.output))


if __name__ == "__main__":
    main()
