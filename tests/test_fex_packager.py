import sys
from pathlib import Path
import tarfile
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from package_fexcore_dlls import package, find_module


def fake_pe() -> bytes:
    data = bytearray(1536)
    data[:2] = b"MZ"
    data[0x3c:0x40] = (128).to_bytes(4, "little")
    data[128:132] = b"PE\x00\x00"
    return bytes(data)


class FexPackageTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.ec = self.root / "ec"
        self.wow = self.root / "wow"
        self.ec.mkdir()
        self.wow.mkdir()

    def test_pe_module_discovery_and_archive(self):
        (self.ec / "libarm64ecfex.dll").write_bytes(fake_pe())
        (self.wow / "libwow64fex.dll").write_bytes(fake_pe())
        output = self.root / "fexcore.tar.xz"
        self.assertEqual(len(package(self.ec, self.wow, output)), 64)
        self.assertTrue(output.with_name(output.name+".sha256").is_file())
        with tarfile.open(output, "r:xz") as archive:
            self.assertEqual(set(archive.getnames()), {
                "lib/wine/aarch64-windows/libarm64ecfex.dll",
                "lib/wine/aarch64-windows/libwow64fex.dll"})

    def test_reject_corrupt_pe(self):
        (self.ec / "libarm64ecfex.dll").write_bytes(b"MZ" + bytes(1600))
        with self.assertRaises(ValueError):
            find_module(self.ec, "libarm64ecfex.dll")

    def test_reject_missing_dll(self):
        with self.assertRaises(ValueError):
            package(self.ec, self.wow, self.root / "missing.tar.xz")


if __name__ == "__main__":
    unittest.main()
