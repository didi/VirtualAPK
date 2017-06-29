package android.app;

import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IIntentSender;
import android.os.IBinder;
import android.os.IInterface;

/**
 * @author johnsonlee
 */
interface IActivityManager {

    ComponentName startService(in IApplicationThread caller, in Intent service, in String resolvedType, in String callingPackage, in int userId);

    int stopService(in IApplicationThread caller, in Intent service, in String resolvedType, in int userId);

    boolean stopServiceToken(in ComponentName className, in IBinder token, in int startId);

    void setServiceForeground(in ComponentName className, in IBinder token, in int id, in Notification notification, in boolean keepNotification);

    int bindService(in IApplicationThread caller, in IBinder token, in Intent service, in String resolvedType, in IServiceConnection connection, in int flags, in String callingPackage, in int userId);

    boolean unbindService(in IServiceConnection connection);

    void publishService(in IBinder token, in Intent intent, in IBinder service);

    void unbindFinished(in IBinder token, in Intent service, in boolean doRebind);

    IIntentSender getIntentSender(in int type, in String packageName, in IBinder token, in String resultWho, int requestCode, in Intent[] intents, in String[] resolvedTypes, in int flags, in Bundle options, in int userId);

    void cancelIntentSender(in IIntentSender sender);

    String getPackageForIntentSender(in IIntentSender sender);

    int getUidForIntentSender(in IIntentSender sender);

}
