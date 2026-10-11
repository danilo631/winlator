import io
from pathlib import Path
import struct
import sys
import tarfile
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from assemble_fex_proton import assemble, check_bionic_wine, check_pe, safe_name, FEX_FILES


def elf(interpreter=b"/system/bin/linker64"):
    data = bytearray(256)
    data[:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<H", data, 18, 183)
    struct.pack_into("<Q", data, 32, 64)
    struct.pack_into("<HH", data, 54, 56, 1)
    struct.pack_into("<I", data, 64, 3)
    struct.pack_into("<Q", data, 72, 128)
    raw = interpreter + b"\0"
    struct.pack_into("<Q", data, 96, len(raw))
    data[128:128 + len(raw)] = raw
    return bytes(data)


def pe():
    data = bytearray(512)
    data[:2] = b"MZ"
    struct.pack_into("<I", data, 0x3c, 128)
    data[128:132] = b"PE\0\0"
    return bytes(data)


def tarfile_with_entries(path, files):
    with tarfile.open(path, "w:xz") as tar:
        for name, raw in files.items():
            info = tarfile.TarInfo(name)
            info.mode = 0o755 if name == "bin/wine" else 0o644
            info.size = len(raw)
            tar.addfile(info, io.BytesIO(raw))


class ProtonFexAssemblyTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.p = Path(self.temp.name)
        self.wine = self.p / "wine.wcp.xz"
        self.fex = self.p / "fex.tar.xz"
        self.output = self.p / "final.tar.xz"
        self.fexfiles = {name: pe() for name in FEX_FILES}
        tarfile_with_entries(self.wine, {
            "bin/wine": elf(), "lib/wine/aarch64-windows/libarm64ecfex.dll": b"old",
            "lib/wine/something.so": b"data"})
        tarfile_with_entries(self.fex, self.fexfiles)

    def test_verified_combination_and_replacement(self):
        sha = assemble(self.wine, self.fex, self.output)
        self.assertEqual(len(sha), 64)
        with tarfile.open(self.output, "r:xz") as combined:
            self.assertEqual(set(combined.getnames()),
                             {"bin/wine", "lib/wine/something.so"} | FEX_FILES)
            for dll in FEX_FILES:
                self.assertEqual(combined.extractfile(dll).read(), self.fexfiles[dll])
        self.assertTrue((self.p / "final.tar.xz.sha256").exists())

    def test_reject_linux_abi(self):
        tarfile_with_entries(self.wine, {"bin/wine": elf(b"/lib/ld-linux-aarch64.so.1")})
        with self.assertRaisesRegex(ValueError, "Android/Bionic"):
            assemble(self.wine, self.fex, self.output)
        self.assertFalse(self.output.exists())

    def test_reject_unsafe_tar_members(self):
        tarfile_with_entries(self.wine, {"../escape": b"bad", "bin/wine": elf()})
        with self.assertRaises(ValueError):
            assemble(self.wine, self.fex, self.output)

    def test_reject_incomplete_fex(self):
        tarfile_with_entries(self.fex, {"lib/wine/aarch64-windows/libarm64ecfex.dll": pe()})
        with self.assertRaisesRegex(ValueError, "Missing FEX"):
            assemble(self.wine, self.fex, self.output)

    def test_signatures(self):
        check_bionic_wine(elf())
        check_pe(pe(), "dll")
        with self.assertRaises(ValueError):
            check_pe(b"MZ"+bytes(320), "dll")
        with self.assertRaises(ValueError):
            safe_name("../../payload")


if __name__ == "__main__":
    unittest.main()
