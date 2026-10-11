#!/usr/bin/env python3
"""Package an ALREADY-BUILT, compatible Wine tree (including Proton-derived Wine).

This deliberately does not download Proton, bypass Steam, build FEX or pretend
a Linux Steam Proton distribution will run unmodified under Android.
Winlator Settings > Install Wine accepts a tar.xz containing one Wine prefix.
"""
from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import tarfile

ELF_AARCH64 = 183
ELF_X86_64 = 62


def machine(path: Path) -> int:
    try:
        with path.open("rb") as handle:
            data = handle.read(20)
        if len(data) >= 20 and data[:4] == b"\x7fELF" and data[4:6] == b"\x02\x01":
            return int.from_bytes(data[18:20], "little")
    except OSError:
        pass
    return 0


def validate(source: Path, backend: str) -> list[str]:
    source = source.resolve(strict=True)
    if not source.is_dir():
        raise ValueError("Source must be an extracted Wine installation directory")
    wine = source / "bin" / "wine"
    if not wine.is_file():
        raise ValueError("Expected bin/wine. Do not package the Proton launcher script.")
    expected = ELF_X86_64 if backend == "box64" else ELF_AARCH64
    if machine(wine) != expected:
        raise ValueError(f"Wrong Wine ELF architecture: need {expected} (not Linux Proton's Python entrypoint)")
    if not (source / "lib" / "wine").is_dir():
        raise ValueError("Missing lib/wine; this is not a complete Wine installation")

    if backend == "fexcore":
        pe = source / "lib" / "wine" / "aarch64-windows"
        for dll in ("libarm64ecfex.dll", "libwow64fex.dll"):
            file = pe / dll
            if not file.is_file() or file.read_bytes()[:2] != b"MZ":
                raise ValueError(f"Missing FEXCore PE module: {dll}")
        # Native ELF interpreter is checked at launch by FexWineRuntime.
    paths = []
    for item in source.rglob("*"):
        relative = item.relative_to(source)
        if relative == Path():
            continue
        if item.is_symlink():
            try:
                target = item.resolve(strict=True)
                target.relative_to(source)
            except (ValueError, OSError):
                raise ValueError(f"Unsafe/outside/dangling symlink: {relative}")
            if os.path.isabs(os.readlink(item)):
                raise ValueError(f"Absolute symlink not relocatable: {relative}")
        if not item.is_file() and not item.is_dir() and not item.is_symlink():
            raise ValueError(f"Unsupported filesystem node: {relative}")
        paths.append(str(relative))
    return paths


def package(source: Path, output: Path, backend: str) -> str:
    source = source.resolve(strict=True)
    validate(source, backend)
    output = output.resolve()
    if output.is_relative_to(source):
        raise ValueError("Output must not be inside the source Wine tree")
    output.parent.mkdir(parents=True, exist_ok=True)
    # Single top-level directory matches WineInstaller.findWineVersionAsync.
    with tarfile.open(output, mode="w:xz", format=tarfile.PAX_FORMAT) as archive:
        archive.add(source, arcname="wine", recursive=True)
    digest = hashlib.sha256()
    with output.open("rb") as stream:
        while chunk := stream.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True, type=Path, help="Extracted Wine distribution, must contain bin/wine")
    parser.add_argument("--output", required=True, type=Path, help="Output .tar.xz to import in Winlator")
    parser.add_argument("--backend", choices=("box64", "fexcore"), default="box64",
                        help="Box64=x86_64 Wine or FEXCore=Android AArch64 Wine + PE DLLs")
    args = parser.parse_args()
    if not str(args.output).endswith(".tar.xz"):
        parser.error("Output filename must end in .tar.xz")
    sha256 = package(args.source, args.output, args.backend)
    print(f"Created: {args.output}")
    print(f"SHA256:  {sha256}")
    print("Import it through Settings > Install Wine; verify the build's Android ABI separately.")


if __name__ == "__main__":
    main()
