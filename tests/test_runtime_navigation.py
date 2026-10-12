"""Regression guards for visible Wine / Proton entry points and Android resource IDs."""
import re
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

BASE = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"
JAVA = BASE / "overrides/app/src/main/java/com/winlator/optimization/RuntimeCenterFragment.java"
LAYOUT = BASE / "overrides/app/src/main/res/layout/runtime_center_fragment.xml"
MENU = BASE / "overrides/app/src/main/res/menu/main_menu.xml"
PATCHER = BASE / "scripts/apply_app_overrides.py"


class WineNavigationTest(unittest.TestCase):
    def test_runtime_ui_has_action_targets(self):
        source = JAVA.read_text(encoding="utf-8")
        ids = set(re.findall(r"R\.id\.(\w+)", source))
        xml = ET.parse(LAYOUT).getroot()
        layout_ids = {
            node.attrib.get(ANDROID + "id", "").split("/")[-1]
            for node in xml.iter()
        }
        self.assertFalse(ids - layout_ids, f"Buttons missing in XML: {ids - layout_ids}")
        self.assertIn("BTImportWine", ids)
        self.assertIn("BTDownloadProtonBox", ids)
        self.assertIn("BTDownloadProtonFex", ids)
        self.assertIn("BTCreateWineContainer", ids)
        self.assertIn("TVRuntimeInstalledWine", ids)

    def test_automatic_installer_is_visible_and_reaches_wine_pipeline(self):
        root = ET.parse(LAYOUT).getroot()
        ids = {node.attrib.get(ANDROID + "id", "").split("/")[-1] for node in root.iter()}
        self.assertIn("BTAutoInstallProton", ids)
        launcher = (BASE / "overrides/app/src/main/java/com/winlator/optimization/ProtonInstallUI.java").read_text(encoding="utf-8")
        client = (BASE / "overrides/app/src/main/java/com/winlator/optimization/ProtonReleaseClient.java").read_text(encoding="utf-8")
        patcher = PATCHER.read_text(encoding="utf-8")
        self.assertIn("ProtonInstallUI.attach(this, root)", JAVA.read_text(encoding="utf-8"))
        self.assertIn("winlator_verified_wine_file", launcher)
        self.assertIn("winlator_verified_wine_file", patcher)
        self.assertIn("installWine(wineInfo)", patcher)
        self.assertIn("SHA-256", client)
        self.assertIn("commons-compress:1.28.0", patcher)

    def test_menu_navigation_is_present(self):
        items = ET.parse(MENU).getroot()
        ids = {
            x.attrib.get(ANDROID + "id", "").split("/")[-1]
            for x in items.iter()
        }
        self.assertIn("menu_item_runtime_center", ids)
        patch = PATCHER.read_text(encoding="utf-8")
        self.assertIn("MainActivity Wine runtime menu navigation", patch)
        self.assertIn("SettingsFragment always reveal Wine installation controls", patch)
        self.assertIn("winlator_open_wine_import", patch)
        self.assertIn("winlator_focus_wine", patch)

    def test_wine_download_is_explicit_and_official(self):
        source = JAVA.read_text(encoding="utf-8")
        self.assertIn("https://github.com/GameNative/proton-wine/releases", source)
        self.assertIn('proton-wine-11.0-2-x86_64.wcp.xz', source)
        self.assertIn('proton-wine-11.0-2-arm64ec.wcp.xz', source)
        self.assertIn("confirmDownload", source)
        self.assertIn("Importar pacote Wine", LAYOUT.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
