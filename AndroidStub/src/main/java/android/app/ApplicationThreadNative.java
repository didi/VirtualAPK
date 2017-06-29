package android.app;

import android.os.Binder;
import android.os.IBinder;

/**
 * @author johnsonlee
 */
public abstract class ApplicationThreadNative extends Binder implements IApplicationThread {

    @Override
    public IBinder asBinder() {
        throw new RuntimeException("Stub!");
    }

}
