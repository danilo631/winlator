package com.winlator.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.EGL14;

import androidx.collection.ArrayMap;
import androidx.preference.PreferenceManager;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public abstract class GPUInformation {
    // Probe GLES on a worker because some vendor EGL implementations can stall.
    // Always use a pbuffer: surfaceless contexts are not universally supported.
    private static ArrayMap<String, String> loadGPUInformation(Context context) {
        final ArrayMap<String, String> result = new ArrayMap<>();
        result.put("renderer", "");
        result.put("vendor", "");
        result.put("version", "");

        final Context appContext = context.getApplicationContext();
        final CountDownLatch complete = new CountDownLatch(1);
        final String[] values = new String[3];

        Thread probe = new Thread(() -> {
            EGL10 egl = null;
            EGLDisplay display = EGL10.EGL_NO_DISPLAY;
            EGLContext eglContext = EGL10.EGL_NO_CONTEXT;
            EGLSurface surface = EGL10.EGL_NO_SURFACE;
            try {
                egl = (EGL10)EGLContext.getEGL();
                display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
                if (display == EGL10.EGL_NO_DISPLAY || !egl.eglInitialize(display, new int[2])) return;

                int[] attributes = {
                    EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                    EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_NONE
                };
                EGLConfig[] configs = new EGLConfig[1];
                int[] count = new int[1];
                if (!egl.eglChooseConfig(display, attributes, configs, 1, count)
                    || count[0] == 0 || configs[0] == null) return;

                int[] contextAttributes = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
                eglContext = egl.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT, contextAttributes);
                if (eglContext == EGL10.EGL_NO_CONTEXT) return;

                int[] surfaceAttributes = {
                    EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE
                };
                surface = egl.eglCreatePbufferSurface(display, configs[0], surfaceAttributes);
                if (surface == EGL10.EGL_NO_SURFACE
                    || !egl.eglMakeCurrent(display, surface, surface, eglContext)) return;

                GL10 gl = (GL10)eglContext.getGL();
                values[0] = Objects.toString(gl.glGetString(GL10.GL_RENDERER), "");
                values[1] = Objects.toString(gl.glGetString(GL10.GL_VENDOR), "");
                values[2] = Objects.toString(gl.glGetString(GL10.GL_VERSION), "");

                if (!values[0].isEmpty()) {
                    PreferenceManager.getDefaultSharedPreferences(appContext).edit()
                        .putString("gpu_renderer", values[0])
                        .putString("gpu_vendor", values[1])
                        .putString("gpu_version", values[2])
                        .apply();
                }
            }
            catch (RuntimeException ignored) {
                // A failed probe must not prevent creating a container.
            }
            finally {
                if (egl != null && display != EGL10.EGL_NO_DISPLAY) {
                    egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                    if (surface != EGL10.EGL_NO_SURFACE) egl.eglDestroySurface(display, surface);
                    if (eglContext != EGL10.EGL_NO_CONTEXT) egl.eglDestroyContext(display, eglContext);
                    egl.eglTerminate(display);
                }
                complete.countDown();
            }
        }, "Winlator-GPU-Probe");
        probe.setDaemon(true);
        probe.start();

        try {
            // Previous implementation could block forever on a missed notify().
            if (complete.await(2, TimeUnit.SECONDS)) {
                result.put("renderer", Objects.toString(values[0], ""));
                result.put("vendor", Objects.toString(values[1], ""));
                result.put("version", Objects.toString(values[2], ""));
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result;
    }

    public static String getRenderer(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = preferences.getString("gpu_renderer", "");
        if (!value.isEmpty()) return value;

        ArrayMap<String, String> gpuInfo = loadGPUInformation(context);
        return gpuInfo.get("renderer");
    }

    public static String getVendor(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = preferences.getString("gpu_vendor", "");
        if (!value.isEmpty()) return value;

        ArrayMap<String, String> gpuInfo = loadGPUInformation(context);
        return gpuInfo.get("vendor");
    }

    public static String getVersion(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = preferences.getString("gpu_version", "");
        if (!value.isEmpty()) return value;

        ArrayMap<String, String> gpuInfo = loadGPUInformation(context);
        return gpuInfo.get("version");
    }

    public static boolean isMali(Context context) {
        String renderer = getRenderer(context).toLowerCase(Locale.ROOT);
        String vendor = getVendor(context).toLowerCase(Locale.ROOT);
        return renderer.contains("mali") || renderer.contains("immortalis")
            || (vendor.contains("arm") && !renderer.contains("adreno"));
    }

    public static boolean isAdreno(Context context) {
        return getRenderer(context).toLowerCase(Locale.ROOT).contains("adreno");
    }

    public static boolean isAdreno6xx(Context context) {
        return getRenderer(context).toLowerCase(Locale.ENGLISH).matches(".*adreno[^6]+6[0-9]{2}.*");
    }
}
