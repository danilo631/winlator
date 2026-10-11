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

/** A bounded, thread-safe GPU identity probe with pbuffer fallback. */
public final class GPUProbe {
    private GPUProbe() {}

    public static ArrayMap<String, String> query(Context context) {
        final ArrayMap<String, String> info = new ArrayMap<>();
        info.put("renderer", "");
        info.put("vendor", "");
        info.put("version", "");
        final Context appContext = context.getApplicationContext();
        final String[] values = new String[3];
        final CountDownLatch done = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            EGL10 egl = null;
            EGLDisplay display = EGL10.EGL_NO_DISPLAY;
            EGLContext glContext = EGL10.EGL_NO_CONTEXT;
            EGLSurface surface = EGL10.EGL_NO_SURFACE;
            try {
                egl = (EGL10) EGLContext.getEGL();
                display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
                if (display == EGL10.EGL_NO_DISPLAY || !egl.eglInitialize(display, new int[2])) return;

                int[] attributes = {
                    EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                    EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_NONE
                };
                EGLConfig[] configs = new EGLConfig[1];
                int[] count = new int[1];
                if (!egl.eglChooseConfig(display, attributes, configs, 1, count)
                    || count[0] < 1 || configs[0] == null) return;

                glContext = egl.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT,
                    new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE});
                if (glContext == EGL10.EGL_NO_CONTEXT) return;
                surface = egl.eglCreatePbufferSurface(display, configs[0],
                    new int[]{EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE});
                if (surface == EGL10.EGL_NO_SURFACE
                    || !egl.eglMakeCurrent(display, surface, surface, glContext)) return;

                GL10 gl = (GL10) glContext.getGL();
                values[0] = Objects.toString(gl.glGetString(GL10.GL_RENDERER), "");
                values[1] = Objects.toString(gl.glGetString(GL10.GL_VENDOR), "");
                values[2] = Objects.toString(gl.glGetString(GL10.GL_VERSION), "");
                if (!values[0].isEmpty()) {
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(appContext);
                    pref.edit().putString("gpu_renderer", values[0])
                        .putString("gpu_vendor", values[1])
                        .putString("gpu_version", values[2]).apply();
                }
            }
            catch (RuntimeException | LinkageError ignored) {
                // Unknown GPU: leave current user settings untouched.
            }
            finally {
                try {
                    if (egl != null && display != EGL10.EGL_NO_DISPLAY) {
                        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE,
                            EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                        if (surface != EGL10.EGL_NO_SURFACE) egl.eglDestroySurface(display, surface);
                        if (glContext != EGL10.EGL_NO_CONTEXT) egl.eglDestroyContext(display, glContext);
                        egl.eglTerminate(display);
                    }
                }
                catch (RuntimeException ignored) {}
                done.countDown();
            }
        }, "Winlator-GPU-Probe");
        worker.setDaemon(true);
        worker.start();

        try {
            if (done.await(2, TimeUnit.SECONDS)) {
                info.put("renderer", Objects.toString(values[0], ""));
                info.put("vendor", Objects.toString(values[1], ""));
                info.put("version", Objects.toString(values[2], ""));
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return info;
    }

    public static boolean isMali(Context context) {
        String name = GPUHelper.glGetRenderer(context).toLowerCase(Locale.ROOT);
        return name.contains("mali") || name.contains("immortalis");
    }
}
