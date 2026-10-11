#!/usr/bin/env python3
"""Apply self-contained Winlator 11.2 app overrides without forking its submodule.

Usage:
  python3 scripts/apply_app_overrides.py --check
  python3 scripts/apply_app_overrides.py

The exact upstream source anchors are verified before ANY file is written.
Run after 'git submodule update --init --recursive'.
"""
from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app"
JAVA = APP / "app" / "src" / "main" / "java"
OVERLAY = ROOT / "overrides" / "app" / "src" / "main" / "java"

GPU_FILE = Path("com/winlator/core/GPUHelper.java")
CONTAINER_FILE = Path("com/winlator/ContainerDetailFragment.java")
SHORTCUT_FILE = Path("com/winlator/contentdialog/ShortcutSettingsDialog.java")
FRAME_RATING_FILE = Path("com/winlator/widget/FrameRating.java")
DXVK_CONFIG_FILE = Path("com/winlator/contentdialog/DXVKConfigDialog.java")
NEW_FILES = (
    Path("com/winlator/core/GPUProbe.java"),
    Path("com/winlator/optimization/GameProfileUI.java"),
    Path("com/winlator/optimization/ProfileRules.java"),
    Path("com/winlator/optimization/FrameTimeTracker.java"),
    Path("com/winlator/optimization/ThermalAdvisor.java"),
    Path("com/winlator/optimization/ConfigFilePolicy.java"),
)

def replace_once(text: str, old: str, new: str, label: str) -> str:
    if new in text:
        return text  # already applied
    if text.count(old) != 1:
        raise ValueError(f"{label}: expected one upstream anchor, found {text.count(old)}")
    return text.replace(old, new, 1)

def transform_gpu(src: str) -> str:
    replacement = (
        "    private static ArrayMap<String, String> loadGPUInformation(Context context) {\n"
        "        return GPUProbe.query(context);\n"
        "    }\n\n"
    )
    if replacement in src:
        return src
    start_token = "    private static ArrayMap<String, String> loadGPUInformation(Context context) {"
    end_token = "    public static String glGetRenderer(Context context) {"
    if src.count(start_token) != 1 or src.count(end_token) != 1:
        raise ValueError("GPUHelper: upstream method signature changed")
    a = src.index(start_token)
    b = src.index(end_token, a)
    return src[:a] + replacement + src[b:]

def transform_container(src: str) -> str:
    src = replace_once(
        src, "import com.winlator.core.EnvVars;\n",
        "import com.winlator.core.EnvVars;\n"
        "import com.winlator.core.GPUHelper;\n"
        "import com.winlator.optimization.ProfileRules;\n"
        "import com.winlator.optimization.GameProfileUI;\n",
        "ContainerDetailFragment imports",
    )
    src = replace_once(
        src,
        "        final EnvVarsView envVarsView = createEnvVarsTab(view);\n",
        "        final EnvVarsView envVarsView = createEnvVarsTab(view);\n"
        "        GameProfileUI.attach(context, view.findViewById(R.id.LLTabEnvVars),\n"
        "            envVarsView, sBox64Preset);\n",
        "ContainerDetailFragment profile control",
    )
    src = replace_once(
        src,
        "        envVarsView.setEnvVars(new EnvVars(isEditMode() ? container.getEnvVars() : Container.DEFAULT_ENV_VARS));",
        "        String initialVars = isEditMode() ? container.getEnvVars()\n"
        "            : ProfileRules.newContainerEnv(Container.DEFAULT_ENV_VARS,\n"
        "                GPUHelper.getAdrenoModelId(context) > 0);\n"
        "        envVarsView.setEnvVars(new EnvVars(initialVars));",
        "ContainerDetailFragment new GPU defaults",
    )
    return src

def transform_shortcut(src: str) -> str:
    src = replace_once(
        src, "import com.winlator.core.EnvVars;\n",
        "import com.winlator.core.EnvVars;\n"
        "import com.winlator.optimization.GameProfileUI;\n",
        "ShortcutSettingsDialog import",
    )
    src = replace_once(
        src,
        "        final EnvVarsView envVarsView = createEnvVarsTab();\n",
        "        final EnvVarsView envVarsView = createEnvVarsTab();\n"
        "        GameProfileUI.attach(context, findViewById(R.id.LLTabEnvVars),\n"
        "            envVarsView, sBox64Preset);\n",
        "ShortcutSettingsDialog profile control",
    )
    return src


def transform_frame_rating(src: str) -> str:
    src = replace_once(
        src, "import com.winlator.R;\n",
        "import com.winlator.R;\n"
        "import com.winlator.optimization.FrameTimeTracker;\n",
        "FrameRating frametime import",
    )
    src = replace_once(
        src, "    private float lastFPS = 0;\n",
        "    private float lastFPS = 0;\n"
        "    private final FrameTimeTracker frameTimes = new FrameTimeTracker(240);\n",
        "FrameRating tracker member",
    )
    src = replace_once(
        src, "    public void reset() {\n        frameCount = 0;\n",
        "    public void reset() {\n        frameTimes.reset();\n        frameCount = 0;\n",
        "FrameRating tracker reset",
    )
    src = replace_once(
        src, "    public void update() {\n        long time = SystemClock.elapsedRealtime();\n",
        "    public void update() {\n        if (mode == Mode.FULL) frameTimes.record(SystemClock.elapsedRealtimeNanos());\n"
        "        long time = SystemClock.elapsedRealtime();\n",
        "FrameRating track only full HUD",
    )
    src = replace_once(
        src,
        '        ((TextView)fpsPanel.getChildAt(1)).setText(String.format(Locale.ENGLISH, "%.1f", lastFPS));',
        '        FrameTimeTracker.Stats stats = mode == Mode.FULL ? frameTimes.snapshot() : null;\n'
        '        String fpsText = (stats != null && stats.samples >= 20)\n'
        '            ? String.format(Locale.ENGLISH, "%.1f | update p95 %.1fms", lastFPS, stats.p95Ms)\n'
        '            : String.format(Locale.ENGLISH, "%.1f", lastFPS);\n'
        '        ((TextView)fpsPanel.getChildAt(1)).setText(fpsText);',
        "FrameRating show update interval in FULL HUD",
    )
    return src

def transform_dxvk_config(src: str) -> str:
    src = replace_once(
        src, "import com.winlator.R;\n",
        "import com.winlator.R;\n"
        "import com.winlator.optimization.ConfigFilePolicy;\n",
        "DXVKConfigDialog import",
    )
    src = replace_once(
        src,
        '        FileUtils.delete(dxvkConfigFile);\n'
        '        String content = getDXVKConfigContent(config);\n'
        '        if (FileUtils.writeString(dxvkConfigFile, content)) {\n'
        '            envVars.put("DXVK_CONFIG_FILE", RootFS.getDosUserConfigPath()+"\\\\dxvk.conf");\n'
        '        }',
        '        String content = getDXVKConfigContent(config);\n'
        '        boolean exists = dxvkConfigFile.isFile();\n'
        '        String current = exists ? FileUtils.readString(dxvkConfigFile) : "";\n'
        '        boolean ready = !ConfigFilePolicy.needsWrite(exists, current, content)\n'
        '            || FileUtils.writeString(dxvkConfigFile, content);\n'
        '        if (ready) {\n'
        '            envVars.put("DXVK_CONFIG_FILE", RootFS.getDosUserConfigPath()+"\\\\dxvk.conf");\n'
        '        }',
        "DXVKConfigDialog skip redundant file delete/write",
    )
    return src

def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="validate without modifying files")
    args = parser.parse_args()

    if not (APP / "gradlew").is_file():
        parser.error("Winlator app submodule missing: git submodule update --init --recursive")

    edits = {
        GPU_FILE: transform_gpu,
        CONTAINER_FILE: transform_container,
        SHORTCUT_FILE: transform_shortcut,
        FRAME_RATING_FILE: transform_frame_rating,
        DXVK_CONFIG_FILE: transform_dxvk_config,
    }
    outputs = {}

    # Validate all source files and overlay assets before writing anything.
    for path, transform in edits.items():
        dest = JAVA / path
        if not dest.is_file():
            raise RuntimeError(f"missing app source: {dest}")
        original = dest.read_text(encoding="utf-8")
        outputs[dest] = transform(original)

    for path in NEW_FILES:
        source = OVERLAY / path
        if not source.is_file():
            raise RuntimeError(f"missing overlay source: {source}")
        dest = JAVA / path
        contents = source.read_text(encoding="utf-8")
        if dest.is_file() and dest.read_text(encoding="utf-8") != contents:
            raise RuntimeError(f"will not overwrite a different upstream file: {dest}")
        outputs[dest] = contents

    if args.check:
        print(f"OK: validated {len(outputs)} Winlator app overrides")
        return 0

    for dest, contents in outputs.items():
        dest.parent.mkdir(parents=True, exist_ok=True)
        if not dest.is_file() or dest.read_text(encoding="utf-8") != contents:
            dest.write_text(contents, encoding="utf-8")
            print(f"updated: {dest.relative_to(ROOT)}")
    print("Winlator Enhanced overrides applied (idempotent).")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
