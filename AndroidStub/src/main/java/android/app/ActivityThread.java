package android.app;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * @author johnsonlee
 */
public final class ActivityThread {

    public static ActivityThread currentActivityThread() {
        throw new RuntimeException("Stub!");
    }

    public static boolean isSystem() {
        throw new RuntimeException("Stub!");
    }

    public static String currentOpPackageName() {
        throw new RuntimeException("Stub!");
    }

    public static String currentPackageName() {
        throw new RuntimeException("Stub!");
    }

    public static String currentProcessName() {
        throw new RuntimeException("Stub!");
    }

    public static Application currentApplication() {
        throw new RuntimeException("Stub!");
    }

    public ApplicationThread getApplicationThread() {
        throw new RuntimeException("Stub!");
    }

    public Instrumentation getInstrumentation() {
        throw new RuntimeException("Stub!");
    }

    public Looper getLooper() {
        throw new RuntimeException("Stub!");
    }

    public Application getApplication() {
        throw new RuntimeException("Stub!");
    }

    public String getProcessName() {
        throw new RuntimeException("Stub!");
    }

    public final ActivityInfo resolveActivityInfo(final Intent intent) {
        throw new RuntimeException("Stub!");
    }

    public final Activity getActivity(final IBinder token) {
        throw new RuntimeException("Stub!");
    }

    final Handler getHandler() {
        throw new RuntimeException("Stub!");
    }

    private class ApplicationThread extends ApplicationThreadNative {


    }
}
