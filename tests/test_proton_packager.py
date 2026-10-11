import os
from pathlib import Path
import sys
import tarfile
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from package_proton_wine import package, validate


def fake_elf(machine):
    data = bytearray(256)
    data[:6] = b"\x7fELF\x02\x01"
    data[18:20] = machine.to_bytes(2, "little")
    return data


class ProtonPackagerTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.source = self.root / "source"
        (self.source / "bin").mkdir(parents=True)
        (self.source / "lib" / "wine" / "aarch64-windows").mkdir(parents=True)

    def test_box64_and_archive_layout(self):
        (self.source / "bin" / "wine").write_bytes(fake_elf(62))
        archive = self.root / "proton-wine.tar.xz"
        checksum = package(self.source, archive, "box64")
        self.assertEqual(len(checksum), 64)
        with tarfile.open(archive, "r:xz") as file:
            paths = file.getnames()
        self.assertIn("wine/bin/wine", paths)
        self.assertTrue(all(p == "wine" or p.startswith("wine/") for p in paths))
        with self.assertRaises(ValueError):
            validate(self.source, "fexcore")

    def test_arm64ec_requires_fex_dlls(self):
        (self.source / "bin" / "wine").write_bytes(fake_elf(183))
        with self.assertRaises(ValueError):
            validate(self.source, "fexcore")
        for dll in ("libarm64ecfex.dll", "libwow64fex.dll"):
            (self.source / "lib" / "wine" / "aarch64-windows" / dll).write_bytes(b"MZ"+b"z"*300)
        self.assertGreater(len(validate(self.source, "fexcore")), 2)
        with self.assertRaises(ValueError):
            validate(self.source, "box64")

    def test_reject_output_in_tree_and_escaping_link(self):
        (self.source / "bin" / "wine").write_bytes(fake_elf(62))
        with self.assertRaises(ValueError):
            package(self.source, self.source / "bad.tar.xz", "box64")
        outside = self.root / "untrusted"
        outside.write_text("outside")
        os.symlink(outside, self.source / "leak")
        with self.assertRaises(ValueError):
            validate(self.source, "box64")


if __name__ == "__main__":
    unittest.main()
