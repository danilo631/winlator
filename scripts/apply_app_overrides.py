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
import hashlib
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
GUEST_LAUNCHER_FILE = Path("com/winlator/xenvironment/components/GuestProgramLauncherComponent.java")
WINE_INSTALLER_FILE = Path("com/winlator/core/WineInstaller.java")
CONTAINERS_FILE = Path("com/winlator/ContainersFragment.java")
SETTINGS_FILE = Path("com/winlator/SettingsFragment.java")
MAIN_ACTIVITY_FILE = Path("com/winlator/MainActivity.java")
NEW_FILES = (
    Path("com/winlator/core/GPUProbe.java"),
    Path("com/winlator/optimization/GameProfileUI.java"),
    Path("com/winlator/optimization/ProfileRules.java"),
    Path("com/winlator/optimization/FrameTimeTracker.java"),
    Path("com/winlator/optimization/ThermalAdvisor.java"),
    Path("com/winlator/optimization/ConfigFilePolicy.java"),
    Path("com/winlator/optimization/FexWineRuntime.java"),
    Path("com/winlator/optimization/RuntimeBackendUI.java"),
    Path("com/winlator/optimization/EnhancedLibraryUI.java"),
    Path("com/winlator/optimization/LibraryFilter.java"),
    Path("com/winlator/optimization/ProtonSyncRules.java"),
    Path("com/winlator/optimization/ProtonTuningUI.java"),
    Path("com/winlator/optimization/ProtonSourcesUI.java"),
    Path("com/winlator/optimization/RuntimeCenterFragment.java"),
    Path("com/winlator/optimization/SettingsOverviewUI.java"),
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
        "import com.winlator.optimization.GameProfileUI;\n"
        "import com.winlator.optimization.RuntimeBackendUI;\n",
        "ContainerDetailFragment imports",
    )
    src = replace_once(
        src,
        "        final EnvVarsView envVarsView = createEnvVarsTab(view);\n",
        "        final EnvVarsView envVarsView = createEnvVarsTab(view);\n"
        "        GameProfileUI.attach(context, view.findViewById(R.id.LLTabEnvVars),\n"
        "            envVarsView, sBox64Preset);\n"
        "        RuntimeBackendUI.attach(context, view.findViewById(R.id.LLTabEnvVars), envVarsView);\n",
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
        "import com.winlator.optimization.GameProfileUI;\n"
        "import com.winlator.optimization.RuntimeBackendUI;\n",
        "ShortcutSettingsDialog import",
    )
    src = replace_once(
        src,
        "        final EnvVarsView envVarsView = createEnvVarsTab();\n",
        "        final EnvVarsView envVarsView = createEnvVarsTab();\n"
        "        GameProfileUI.attach(context, findViewById(R.id.LLTabEnvVars),\n"
        "            envVarsView, sBox64Preset);\n"
        "        RuntimeBackendUI.attach(context, findViewById(R.id.LLTabEnvVars), envVarsView);\n",
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


def transform_guest_launcher(src: str) -> str:
    src = replace_once(
        src, "import com.winlator.box64.Box64Preset;\n",
        "import com.winlator.box64.Box64Preset;\n"
        "import com.winlator.optimization.FexWineRuntime;\n"
        "import android.os.Handler;\n"
        "import android.os.Looper;\n"
        "import android.widget.Toast;\n",
        "GuestProgramLauncher import",
    )
    src = replace_once(
        src, "    private String box64Preset = Box64Preset.CONSERVATIVE;\n",
        "    private String box64Preset = Box64Preset.CONSERVATIVE;\n"
        "    private String executionBackend = FexWineRuntime.BACKEND_BOX64;\n",
        "GuestProgramLauncher backend member",
    )
    src = replace_once(
        src, "            extractBox64File();\n            copyDefaultBox64RCFile();\n",
        "            if (!isFexCoreSelected()) {\n"
        "                extractBox64File();\n"
        "                copyDefaultBox64RCFile();\n"
        "            }\n",
        "GuestProgramLauncher Box64 only extraction",
    )
    src = replace_once(
        src, "    private int execGuestProgram() {\n",
        "    public void setExecutionBackend(String backend) {\n"
        "        this.executionBackend = backend;\n"
        "    }\n\n"
        "    private boolean isFexCoreSelected() {\n"
        "        return FexWineRuntime.BACKEND_FEXCORE.equals(executionBackend)\n"
        "            || (this.envVars != null && FexWineRuntime.BACKEND_FEXCORE.equals(\n"
        "                this.envVars.get(FexWineRuntime.ENV_BACKEND)));\n"
        "    }\n\n"
        "    private int execGuestProgram() {\n",
        "GuestProgramLauncher backend selector",
    )
    src = replace_once(
        src, '        addBox64EnvVars(envVars);\n',
        '        final boolean nativeFex = isFexCoreSelected();\n'
        '        if (!nativeFex) addBox64EnvVars(envVars);\n',
        "GuestProgramLauncher config setup",
    )
    src = replace_once(
        src,
        '        envVars.put("BOX64_LD_LIBRARY_PATH", rootDir+"/lib/x86_64-linux-gnu");\n',
        '        if (nativeFex) {\n'
        '            File wineDir = new File(rootDir.toString()+rootFS.getWinePath());\n'
        '            envVars.put("LD_LIBRARY_PATH",\n'
        '                new File(wineDir, "lib/wine/aarch64-unix").getPath()+":"\n'
        '                +new File(wineDir, "lib").getPath()+":"\n'
        '                +rootFS.getLibDir().getPath());\n'
        '        }\n'
        '        else envVars.put("BOX64_LD_LIBRARY_PATH", rootDir+"/lib/x86_64-linux-gnu");\n',
        "GuestProgramLauncher native Wine Unix library path",
    )
    src = replace_once(
        src, '        if (this.envVars != null) envVars.putAll(this.envVars);\n',
        '        if (this.envVars != null) envVars.putAll(this.envVars);\n'
        '        envVars.remove(FexWineRuntime.ENV_BACKEND);\n',
        "GuestProgramLauncher hide internal selector from guest",
    )
    src = replace_once(
        src,
        '        String command = rootDir+"/usr/local/bin/box64 "+guestExecutable;\n',
        '        String command;\n'
        '        if (nativeFex) {\n'
        '            File wineDir = new File(rootDir.toString()+rootFS.getWinePath());\n'
        '            String error = FexWineRuntime.check(wineDir);\n'
        '            if (error == null) {\n'
        '                try { command = FexWineRuntime.nativeWineCommand(guestExecutable, wineDir); }\n'
        '                catch (IllegalArgumentException failure) { error = failure.getMessage(); command = ""; }\n'
        '            }\n'
        '            else command = "";\n'
        '            if (error != null) {\n'
        '                final String reason = error;\n'
        '                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(\n'
        '                    environment.getContext(), "FEXCore unavailable: "+reason,\n'
        '                    Toast.LENGTH_LONG).show());\n'
        '                if (terminationCallback != null) terminationCallback.call(127);\n'
        '                return -1;\n'
        '            }\n'
        '        }\n'
        '        else command = rootDir+"/usr/local/bin/box64 "+guestExecutable;\n',
        "GuestProgramLauncher real native ARM64EC branch",
    )
    return src

def transform_wine_installer(src: str) -> str:
    src = replace_once(
        src,
        'import com.winlator.MainActivity;\n',
        'import com.winlator.MainActivity;\n'
        'import com.winlator.optimization.FexWineRuntime;\n',
        "WineInstaller preflight import",
    )
    src = replace_once(
        src,
        '        guestProgramLauncherComponent.setBox64Preset(Box64Preset.STABILITY);\n',
        '        guestProgramLauncherComponent.setBox64Preset(Box64Preset.STABILITY);\n'
        '        if (FexWineRuntime.ready(new File(rootDir.toString()+rootFS.getWinePath())))\n'
        '            guestProgramLauncherComponent.setExecutionBackend(FexWineRuntime.BACKEND_FEXCORE);\n',
        "WineInstaller native prefix initialization",
    )
    src = replace_once(
        src,
        '        String wineBinPath = wineBin64.isFile() ? wineBin64.getPath() : wineBin.getPath();\n',
        '        String wineBinPath = FexWineRuntime.isNativeAndroidArm64(wineBin)\n'
        '            ? wineBin.getPath()\n'
        '            : (wineBin64.isFile() ? wineBin64.getPath() : wineBin.getPath());\n',
        "WineInstaller native arm64 binary preference",
    )
    src = replace_once(
        src,
        '        guestProgramLauncherComponent.setGuestExecutable(wineBinPath+" --version");\n',
        '        guestProgramLauncherComponent.setGuestExecutable(wineBinPath+" --version");\n'
        '        if (FexWineRuntime.isNativeAndroidArm64(wineBin)) {\n'
        '            if (!FexWineRuntime.ready(new File(winePath))) {\n'
        '                ProcessHelper.removeDebugCallback(debugCallback);\n'
        '                callback.call(null);\n'
        '                return;\n'
        '            }\n'
        '            rootFS.setWinePath(winePath);\n'
        '            guestProgramLauncherComponent.setExecutionBackend(FexWineRuntime.BACKEND_FEXCORE);\n'
        '        }\n',
        "WineInstaller probe native version",
    )
    return src


# Upstream git-blob SHA-1s from pinned Winlator 11.2 submodule.
# Entire existing layout files are replaced only if still identical to upstream.
RES_OVERLAY = ROOT / "overrides" / "app" / "src" / "main" / "res"
RESOURCE_REPLACEMENTS = {
    Path("layout/containers_fragment.xml"): "a954448ded0799501fb9a6f0de1aa1dad5fb9525",
    Path("layout/container_list_item.xml"): "6c94be0b2f5b1ddb5d9eca029ecd9da63588e42e",
    Path("menu/main_menu.xml"): "40a7b19766d480e435db50acf52197ef8d6a2b09",
}
RESOURCE_ADDITIONS = (
    Path("drawable/enhanced_hero.xml"),
    Path("drawable/enhanced_library_card.xml"),
    Path("drawable/enhanced_play_button.xml"),
    Path("drawable/enhanced_icon_tile.xml"),
    Path("drawable/enhanced_search_field.xml"),
    Path("layout/runtime_center_fragment.xml"),
)

def git_blob_sha(content: bytes) -> str:
    header = b"blob " + str(len(content)).encode("ascii") + b"\0"
    return hashlib.sha1(header + content).hexdigest()

def transform_containers_list(src: str) -> str:
    src = replace_once(
        src,
        "import android.view.LayoutInflater;\n",
        "import android.view.LayoutInflater;\n"
        "import android.text.Editable;\n"
        "import android.text.TextWatcher;\n"
        "import android.widget.EditText;\n"
        "import com.winlator.optimization.LibraryFilter;\n"
        "import com.winlator.optimization.EnhancedLibraryUI;\n",
        "ContainersFragment new UI imports",
    )
    src = replace_once(
        src,
        "        DividerItemDecoration itemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);\n"
        "        itemDecoration.setDrawable(ContextCompat.getDrawable(context, R.drawable.list_item_divider));\n"
        "        recyclerView.addItemDecoration(itemDecoration);\n",
        "        frameLayout.findViewById(R.id.BTRuntimeCenter).setOnClickListener(v ->\n"
        "            ((MainActivity)requireActivity()).showFragment(new com.winlator.optimization.RuntimeCenterFragment()));\n"
        "        EditText search = frameLayout.findViewById(R.id.ETSearchContainers);\n"
        "        search.addTextChangedListener(new TextWatcher() {\n"
        "            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}\n"
        "            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {\n"
        "                if (manager != null) loadContainersList();\n"
        "            }\n"
        "            @Override public void afterTextChanged(Editable e) {}\n"
        "        });\n",
        "ContainersFragment library filter",
    )
    src = replace_once(
        src,
        "        ArrayList<Container> containers = manager.getContainers();\n"
        "        recyclerView.setAdapter(new ContainersAdapter(containers));\n"
        "        if (containers.isEmpty()) emptyTextView.setVisibility(View.VISIBLE);\n",
        "        ArrayList<Container> all = manager.getContainers();\n"
        "        ArrayList<Container> filtered = new ArrayList<>();\n"
        "        String query = ((EditText)requireView().findViewById(R.id.ETSearchContainers)).getText().toString();\n"
        "        for (Container container : all) {\n"
        "            if (LibraryFilter.matches(query, container.getName(), container.getScreenSize(),\n"
        "                    container.getGraphicsDriver(), container.getWineVersion(),\n"
        "                    container.getDXWrapper(), container.getEnvVars())) filtered.add(container);\n"
        "        }\n"
        "        recyclerView.setAdapter(new ContainersAdapter(filtered));\n"
        "        emptyTextView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);\n"
        "        EnhancedLibraryUI.summary(requireView(), filtered.size(), all.size());\n",
        "ContainersFragment state / search",
    )
    src = replace_once(
        src,
        "            holder.title.setText(item.getName());\n",
        "            holder.title.setText(item.getName());\n"
        "            EnhancedLibraryUI.bindContainer(holder.itemView, item);\n",
        "ContainersFragment card metadata",
    )
    return src


def transform_settings_fragment(src: str) -> str:
    src = replace_once(
        src,
        "import com.winlator.core.WineInstaller;\n",
        "import com.winlator.core.WineInstaller;\n"
        "import com.winlator.optimization.ProtonSourcesUI;\n"
        "import com.winlator.optimization.SettingsOverviewUI;\n",
        "SettingsFragment Proton Wine imports",
    )
    src = replace_once(
        src,
        "        loadWineVersionSpinner(view, sWineVersion);\n",
        "        loadWineVersionSpinner(view, sWineVersion);\n"
        "        ProtonSourcesUI.attach(context, view);\n"
        "        SettingsOverviewUI.attach(context, view);\n",
        "SettingsFragment Proton source / import button",
    )
    # The stock release leaves Wine installation hidden unless DEBUG_MODE.
    # This is the reason the Proton install controls were not visible.
    src = replace_once(
        src,
        '        if (MainActivity.DEBUG_MODE) {\n'
        '            view.findViewById(R.id.LLWineInstallation).setVisibility(View.VISIBLE);\n'
        '        }\n',
        '        view.findViewById(R.id.LLWineInstallation).setVisibility(View.VISIBLE);\n',
        "SettingsFragment always reveal Wine installation controls",
    )
    src = replace_once(
        src,
        '        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.settings);\n',
        '        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.settings);\n'
        '        Bundle args = getArguments();\n'
        '        if (args != null) {\n'
        '            boolean importWine = args.getBoolean("winlator_open_wine_import", false);\n'
        '            boolean focusWine = args.getBoolean("winlator_focus_wine", false);\n'
        '            if (importWine && savedInstanceState == null)\n'
        '                view.post(() -> { if (isAdded()) selectWineFileForInstall(); });\n'
        '            else if (focusWine) {\n'
        '                View wine = view.findViewById(R.id.LLWineInstallation);\n'
        '                view.post(() -> { if (isAdded() && wine != null) {\n'
        '                    wine.requestFocus();\n'
        '                    wine.requestRectangleOnScreen(\n'
        '                        new android.graphics.Rect(0, 0, wine.getWidth(), wine.getHeight()), true);\n'
        '                }});\n'
        '            }\n'
        '        }\n',
        "SettingsFragment deep link: import Wine or focus Wine settings",
    )
    return src


def transform_main_activity(src: str) -> str:
    src = replace_once(
        src,
        "import com.winlator.contentdialog.AboutDialog;\n",
        "import com.winlator.contentdialog.AboutDialog;\n"
        "import com.winlator.optimization.RuntimeCenterFragment;\n",
        "MainActivity Wine runtime menu import",
    )
    src = replace_once(
        src,
        '            case R.id.menu_item_settings:\n'
        '                showFragment(new SettingsFragment());\n'
        '                break;\n',
        '            case R.id.menu_item_runtime_center:\n'
        '                showFragment(new RuntimeCenterFragment());\n'
        '                break;\n'
        '            case R.id.menu_item_settings:\n'
        '                showFragment(new SettingsFragment());\n'
        '                break;\n',
        "MainActivity Wine runtime menu navigation",
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
        GUEST_LAUNCHER_FILE: transform_guest_launcher,
        WINE_INSTALLER_FILE: transform_wine_installer,
        CONTAINERS_FILE: transform_containers_list,
        SETTINGS_FILE: transform_settings_fragment,
        MAIN_ACTIVITY_FILE: transform_main_activity,
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

    # Resource overlays: exact pinned upstream for existing layouts, no clobber of other files.
    res_dest = APP / "app" / "src" / "main" / "res"
    for relative, pinned_sha in RESOURCE_REPLACEMENTS.items():
        source = RES_OVERLAY / relative
        dest = res_dest / relative
        if not source.is_file() or not dest.is_file():
            raise RuntimeError(f"Missing resource overlay or upstream resource: {relative}")
        data = dest.read_bytes()
        new_data = source.read_bytes()
        if data != new_data and git_blob_sha(data) != pinned_sha:
            raise RuntimeError(f"Upstream XML changed: {relative} (refusing to overwrite)")
        outputs[dest] = new_data.decode("utf-8")
    for relative in RESOURCE_ADDITIONS:
        source = RES_OVERLAY / relative
        dest = res_dest / relative
        if not source.is_file():
            raise RuntimeError(f"Missing new drawable overlay: {relative}")
        content = source.read_text(encoding="utf-8")
        if dest.exists() and dest.read_text(encoding="utf-8") != content:
            raise RuntimeError(f"Conflicting new drawable: {relative}")
        outputs[dest] = content

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
