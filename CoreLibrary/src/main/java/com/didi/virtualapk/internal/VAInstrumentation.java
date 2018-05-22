/*
 * Copyright (C) 2017 Beijing Didi Infinity Technology and Development Co.,Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.didi.virtualapk.internal;

import android.app.Activity;
import android.app.Fragment;
import android.app.Instrumentation;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.utils.PluginUtil;
import com.didi.virtualapk.utils.ReflectUtil;


/**
 * Created by renyugang on 16/8/10.
 */
public class VAInstrumentation extends Instrumentation implements Handler.Callback {
    public static final String TAG = "VAInstrumentation";
    public static final int LAUNCH_ACTIVITY         = 100;

    private Instrumentation mBase;

    PluginManager mPluginManager;

    public VAInstrumentation(PluginManager pluginManager, Instrumentation base) {
        this.mPluginManager = pluginManager;
        this.mBase = base;
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        mPluginManager.getComponentsHandler().transformIntentToExplicitAsNeeded(intent);
        // null component is an implicitly intent
        if (intent.getComponent() != null) {
            Log.i(TAG, String.format("execStartActivity[%s : %s]", intent.getComponent().getPackageName(),
                    intent.getComponent().getClassName()));
            // resolve intent with Stub Activity if needed
            this.mPluginManager.getComponentsHandler().markIntentIfNeeded(intent);
        }

        ActivityResult result = realExecStartActivity(who, contextThread, token, target,
                    intent, requestCode, options);

        return result;

    }

    private ActivityResult realExecStartActivity(
            Context who, IBinder contextThread, IBinder token, Activity target,
            Intent intent, int requestCode, Bundle options) {
        ActivityResult result = null;
        try {
            Class[] parameterTypes = {Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class,
            int.class, Bundle.class};
            result = (ActivityResult)ReflectUtil.invoke(Instrumentation.class, mBase,
                    "execStartActivity", parameterTypes,
                    who, contextThread, token, target, intent, requestCode, options);
        } catch (Exception e) {
            if (e.getCause() instanceof ActivityNotFoundException) {
                throw (ActivityNotFoundException) e.getCause();
            }
            e.printStackTrace();
        }

        return result;
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, Fragment target,
            Intent intent, int requestCode, Bundle options) {
        mPluginManager.getComponentsHandler().transformIntentToExplicitAsNeeded(intent);
        // null component is an implicitly intent
        if (intent.getComponent() != null) {
            Log.i(TAG, String.format("execStartActivity[%s : %s]", intent.getComponent().getPackageName(),
                    intent.getComponent().getClassName()));
            // resolve intent with Stub Activity if needed
            this.mPluginManager.getComponentsHandler().markIntentIfNeeded(intent);
        }

        ActivityResult result = realExecStartActivity(who, contextThread, token, target,
                intent, requestCode, options);

        return result;

    }

    private ActivityResult realExecStartActivity(
            Context who, IBinder contextThread, IBinder token, Fragment target,
            Intent intent, int requestCode, Bundle options) {
        ActivityResult result = null;
        try {
            Class[] parameterTypes = {Context.class, IBinder.class, IBinder.class, Fragment.class, Intent.class,
                    int.class, Bundle.class};
            result = (ActivityResult)ReflectUtil.invoke(Instrumentation.class, mBase,
                    "execStartActivity", parameterTypes,
                    who, contextThread, token, target, intent, requestCode, options);
        } catch (Exception e) {
            if (e.getCause() instanceof ActivityNotFoundException) {
                throw (ActivityNotFoundException) e.getCause();
            }
            e.printStackTrace();
        }

        return result;
    }

    private ActivityResult realExecStartActivity(
            Context who, IBinder contextThread, IBinder token, String target,
            Intent intent, int requestCode, Bundle options) {
        ActivityResult result = null;
        try {
            Class[] parameterTypes = {Context.class, IBinder.class, IBinder.class, String.class, Intent.class,
                    int.class, Bundle.class};
            result = (ActivityResult)ReflectUtil.invoke(Instrumentation.class, mBase,
                    "execStartActivity", parameterTypes,
                    who, contextThread, token, target, intent, requestCode, options);
        } catch (Exception e) {
            if (e.getCause() instanceof ActivityNotFoundException) {
                throw (ActivityNotFoundException) e.getCause();
            }
            e.printStackTrace();
        }

        return result;
    }

    public ActivityResult execStartActivity(
            Context who, IBinder contextThread, IBinder token, String target,
            Intent intent, int requestCode, Bundle options) {
        mPluginManager.getComponentsHandler().transformIntentToExplicitAsNeeded(intent);
        // null component is an implicitly intent
        if (intent.getComponent() != null) {
            Log.i(TAG, String.format("execStartActivity[%s : %s]", intent.getComponent().getPackageName(),
                    intent.getComponent().getClassName()));
            // resolve intent with Stub Activity if needed
            this.mPluginManager.getComponentsHandler().markIntentIfNeeded(intent);
        }

        ActivityResult result = realExecStartActivity(who, contextThread, token, target,
                intent, requestCode, options);
        return null;
    }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            ComponentName component = PluginUtil.getComponent(intent);
            LoadedPlugin plugin = this.mPluginManager.getLoadedPlugin(component);
            String targetClassName = component.getClassName();

            Log.i(TAG, String.format("newActivity[%s : %s/%s]", className, component.getPackageName(), targetClassName));

            if (plugin != null) {
                Activity activity = mBase.newActivity(plugin.getClassLoader(), targetClassName, intent);
                activity.setIntent(intent);

                try {
                    // for 4.1+
                    ReflectUtil.setField(ContextThemeWrapper.class, activity, "mResources", plugin.getResources());
                } catch (Exception ignored) {
                    // ignored.
                }

                return activity;
            }
        }

        return mBase.newActivity(cl, className, intent);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        final Intent intent = activity.getIntent();
        if (PluginUtil.isIntentFromPlugin(intent)) {
            Context base = activity.getBaseContext();
            try {
                LoadedPlugin plugin = this.mPluginManager.getLoadedPlugin(intent);
                ReflectUtil.setField(base.getClass(), base, "mResources", plugin.getResources());
                ReflectUtil.setField(ContextWrapper.class, activity, "mBase", plugin.getPluginContext());
                ReflectUtil.setField(Activity.class, activity, "mApplication", plugin.getApplication());
                ReflectUtil.setFieldNoException(ContextThemeWrapper.class, activity, "mBase", plugin.getPluginContext());

                // set screenOrientation
                ActivityInfo activityInfo = plugin.getActivityInfo(PluginUtil.getComponent(intent));
                if (activityInfo.screenOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                    activity.setRequestedOrientation(activityInfo.screenOrientation);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        mBase.callActivityOnCreate(activity, icicle);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == LAUNCH_ACTIVITY) {
            // ActivityClientRecord r
            Object r = msg.obj;
            try {
                Intent intent = (Intent) ReflectUtil.getField(r.getClass(), r, "intent");
                intent.setExtrasClassLoader(VAInstrumentation.class.getClassLoader());
                ActivityInfo activityInfo = (ActivityInfo) ReflectUtil.getField(r.getClass(), r, "activityInfo");

                if (PluginUtil.isIntentFromPlugin(intent)) {
                    int theme = PluginUtil.getTheme(mPluginManager.getHostContext(), intent);
                    if (theme != 0) {
                        Log.i(TAG, "resolve theme, current theme:" + activityInfo.theme + "  after :0x" + Integer.toHexString(theme));
                        activityInfo.theme = theme;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    @Override
    public Context getContext() {
        return mBase.getContext();
    }

    @Override
    public Context getTargetContext() {
        return mBase.getTargetContext();
    }

    @Override
    public ComponentName getComponentName() {
        return mBase.getComponentName();
    }

}
