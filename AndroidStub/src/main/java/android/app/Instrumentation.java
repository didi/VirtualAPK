package android.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;

/**
 * Created by qiaopu on 2018/5/7.
 */
public class Instrumentation {
    
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        throw new RuntimeException("Stub!");
    }
    
    public void callApplicationOnCreate(Application app) {
        throw new RuntimeException("Stub!");
    }
    
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        throw new RuntimeException("Stub!");
    }
    
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        throw new RuntimeException("Stub!");
    }
    
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        throw new RuntimeException("Stub!");
    }
    
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode) {
        throw new RuntimeException("Stub!");
    }
    
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode, Bundle options) {
        throw new RuntimeException("Stub!");
    }
    
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Fragment target, Intent intent, int requestCode, Bundle options) {
        throw new RuntimeException("Stub!");
    }
    
    public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, String target, Intent intent, int requestCode, Bundle options) {
        throw new RuntimeException("Stub!");
    }
    
    public Context getContext() {
        throw new RuntimeException("Stub!");
    }
    
    public Context getTargetContext() {
        throw new RuntimeException("Stub!");
    }
    
    public ComponentName getComponentName() {
        throw new RuntimeException("Stub!");
    }
    
    public static final class ActivityResult {
        public ActivityResult(int resultCode, Intent resultData) {
            throw new RuntimeException("Stub!");
        }
    }
}
