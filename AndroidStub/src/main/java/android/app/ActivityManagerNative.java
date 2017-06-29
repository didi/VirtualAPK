package android.app;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * @author johnsonlee
 */
public abstract class ActivityManagerNative extends Binder implements IActivityManager {

    public static IActivityManager getDefault() {
        throw new RuntimeException("Stub!");
    }

    public static boolean isSystemReady() {
        throw new RuntimeException("Stub!");
    }

    public static void broadcastStickyIntent(final Intent intent, final String permission, final int userId) {
        throw new RuntimeException("Stub!");
    }

    static public IActivityManager asInterface(IBinder obj) {
        throw new RuntimeException("Stub!");
    }

    public ActivityManagerNative() {
        throw new RuntimeException("Stub!");
    }
}
