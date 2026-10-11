#!/usr/bin/env python3
"""Combine compatible Android/Bionic ARM64EC Proton Wine .wcp.xz and FEXCore DLLs.

Does not execute any third-party binary, build Wine or replace any installed Wine.
A successfully assembled archive is NOT proof that its ABI/runtime will work.
Usage: python3 scripts/assemble_fex_proton.py --wine proton-wine-arm64ec.wcp.xz
         --fex FEXCore-2609-ARM64EC-modules.tar.xz
         --output proton-fex-winlator.tar.xz
"""
from __future__ import annotations

import argparse
import hashlib
import posixpath
import struct
import tarfile
from pathlib import Path, PurePosixPath

FEX_FILES = {
    "lib/wine/aarch64-windows/libarm64ecfex.dll",
    "lib/wine/aarch64-windows/libwow64fex.dll",
}
MAX_TOTAL_SIZE = 6 * 1024 * 1024 * 1024
MAX_FILES = 200_000


def safe_name(name: str) -> str:
    if not name or "\\" in name or name.startswith("/"):
        raise ValueError(f"Unsafe member name: {name!r}")
    if any(p == ".." for p in PurePosixPath(name).parts):
        raise ValueError(f"Parent traversal member: {name!r}")
    normalized = posixpath.normpath(name)
    if normalized in (".", "..") or normalized.startswith("../"):
        raise ValueError(f"Unsafe normalized member: {name!r}")
    return normalized


def verify_entry(member: tarfile.TarInfo) -> str:
    name = safe_name(member.name)
    if member.issym() or member.islnk():
        if not member.linkname or member.linkname.startswith("/"):
            raise ValueError(f"Absolute/dangling link: {name}")
        base = posixpath.dirname(name) if member.issym() else ""
        final = posixpath.normpath(posixpath.join(base, member.linkname))
        if final == ".." or final.startswith("../"):
            raise ValueError(f"Escaping symlink: {name}")
    elif not member.isfile() and not member.isdir():
        raise ValueError(f"Unsupported tar device/node: {name}")
    return name


def check_bionic_wine(content: bytes) -> None:
    if len(content) < 64 or content[:4] != b"\x7fELF":
        raise ValueError("Wine is not an ELF binary")
    if content[4:6] != b"\x02\x01" or struct.unpack_from("<H", content, 18)[0] != 183:
        raise ValueError("Wine must be little-endian ELF64 ARM64 (ARM64EC bridge)")
    phoff = struct.unpack_from("<Q", content, 32)[0]
    phentsize, count = struct.unpack_from("<HH", content, 54)
    if phoff < 64 or phentsize < 56 or count < 1 or count > 1024:
        raise ValueError("Invalid ELF program headers")
    if phoff + phentsize * count > len(content):
        raise ValueError("Truncated ELF program headers")
    for i in range(count):
        pos = phoff + i * phentsize
        if struct.unpack_from("<I", content, pos)[0] != 3:
            continue
        p_off = struct.unpack_from("<Q", content, pos + 8)[0]
        size = struct.unpack_from("<Q", content, pos + 32)[0]
        if size > 512 or p_off + size > len(content):
            raise ValueError("Invalid ELF interpreter")
        interp = content[p_off:p_off + size].rstrip(b"\0")
        if interp in (b"/system/bin/linker64",
                      b"/apex/com.android.runtime/bin/linker64"):
            return
    raise ValueError("Wine is not an Android/Bionic dynamic executable")


def check_pe(data: bytes, label: str) -> None:
    if len(data) < 256 or data[:2] != b"MZ":
        raise ValueError(f"{label} lacks DOS PE header")
    pe = struct.unpack_from("<I", data, 0x3c)[0]
    if pe < 64 or pe + 24 > len(data) or data[pe:pe+4] != b"PE\0\0":
        raise ValueError(f"{label} lacks correct PE signature")


def archive_members(archive: tarfile.TarFile) -> dict[str, tarfile.TarInfo]:
    index: dict[str, tarfile.TarInfo] = {}
    total = 0
    for m in archive:
        name = verify_entry(m)
        if name in index:
            raise ValueError(f"Duplicate tar member: {name}")
        total += m.size
        if len(index) >= MAX_FILES or total > MAX_TOTAL_SIZE:
            raise ValueError("Archive is too large")
        index[name] = m
    return index


def assemble(wine: Path, fex: Path, output: Path) -> str:
    if not output.name.endswith(".tar.xz"):
        raise ValueError("Output must use .tar.xz extension")
    for source in (wine, fex):
        if output.resolve() == source.resolve():
            raise ValueError("Output cannot overwrite input archives")
    output.parent.mkdir(parents=True, exist_ok=True)
    try:
        with tarfile.open(wine, "r:xz") as w, tarfile.open(fex, "r:xz") as f:
            wines = archive_members(w)
            dlls = archive_members(f)
            if "bin/wine" not in wines:
                raise ValueError("Proton Wine archive has no bin/wine")
            binary = w.extractfile(wines["bin/wine"])
            if binary is None:
                raise ValueError("Wine is not a regular or resolved executable")
            check_bionic_wine(binary.read())
            for name in FEX_FILES:
                if name not in dlls or not dlls[name].isfile():
                    raise ValueError(f"Missing FEX translator: {name}")
                source = f.extractfile(dlls[name])
                if source is None:
                    raise ValueError(f"Unreadable FEX DLL: {name}")
                check_pe(source.read(), name)

            with tarfile.open(output, mode="w:xz", format=tarfile.PAX_FORMAT) as dest:
                for name, member in wines.items():
                    if name in FEX_FILES:
                        continue  # replace matching FEXCore modules as a pair
                    member.name = name
                    fileobj = w.extractfile(member) if member.isfile() else None
                    dest.addfile(member, fileobj)
                    if fileobj is not None:
                        fileobj.close()
                for name in sorted(FEX_FILES):
                    member = dlls[name]
                    member.name = name
                    stream = f.extractfile(member)
                    dest.addfile(member, stream)
                    if stream is not None:
                        stream.close()
    except Exception:
        output.unlink(missing_ok=True)
        raise
    checksum = hashlib.sha256()
    with output.open("rb") as stream:
        while block := stream.read(1024 * 1024):
            checksum.update(block)
    digest = checksum.hexdigest()
    output.with_name(output.name + ".sha256").write_text(
        f"{digest}  {output.name}\n", encoding="utf-8")
    return digest


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--wine", type=Path, required=True,
                        help="ARM64EC Wine Type .wcp.xz archive")
    parser.add_argument("--fex", type=Path, required=True,
                        help="Compiled FEXCore ARM64EC .tar.xz DLL modules")
    parser.add_argument("--output", type=Path, required=True,
                        help="Winlator importable .tar.xz output file")
    args = parser.parse_args()
    print("Created ARM64EC Wine + FEXCore tar.xz; SHA256:", assemble(args.wine, args.fex, args.output))
    print("WARNING: compatible Wine unixlibs, rootfs and Android execution remain required.")


if __name__ == "__main__":
    main()
