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

package com.didi.virtualapk.delegate;

import android.app.ActivityThread;
import android.app.Application;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.internal.Constants;
import com.didi.virtualapk.internal.LoadedPlugin;
import com.didi.virtualapk.internal.utils.PluginUtil;
import com.didi.virtualapk.utils.Reflector;

import java.lang.reflect.Method;

/**
 * @author johnsonlee
 */
public class LocalService extends Service {
    private static final String TAG = Constants.TAG_PREFIX + "LocalService";

    /**
     * The target service, usually it's a plugin service intent
     */
    public static final String EXTRA_TARGET = "target";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_PLUGIN_LOCATION = "plugin_location";

    public static final int EXTRA_COMMAND_START_SERVICE = 1;
    public static final int EXTRA_COMMAND_STOP_SERVICE = 2;
    public static final int EXTRA_COMMAND_BIND_SERVICE = 3;
    public static final int EXTRA_COMMAND_UNBIND_SERVICE = 4;


    private PluginManager mPluginManager;

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPluginManager = PluginManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent || !intent.hasExtra(EXTRA_TARGET) || !intent.hasExtra(EXTRA_COMMAND)) {
            return START_STICKY;
        }

        Intent target = intent.getParcelableExtra(EXTRA_TARGET);
        int command = intent.getIntExtra(EXTRA_COMMAND, 0);
        if (null == target || command <= 0) {
            return START_STICKY;
        }

        ComponentName component = target.getComponent();
        LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
        
        if (plugin == null) {
            Log.w(TAG, "Error target: " + target.toURI());
            return START_STICKY;
        }
        // ClassNotFoundException when unmarshalling in Android 5.1
        target.setExtrasClassLoader(plugin.getClassLoader());
        switch (command) {
            case EXTRA_COMMAND_START_SERVICE: {
                ActivityThread mainThread = ActivityThread.currentActivityThread();
                IApplicationThread appThread = mainThread.getApplicationThread();
                Service service;

                if (this.mPluginManager.getComponentsHandler().isServiceAvailable(component)) {
                    service = this.mPluginManager.getComponentsHandler().getService(component);
                } else {
                    try {
                        service = (Service) plugin.getClassLoader().loadClass(component.getClassName()).newInstance();

                        Application app = plugin.getApplication();
                        IBinder token = appThread.asBinder();
                        Method attach = service.getClass().getMethod("attach", Context.class, ActivityThread.class, String.class, IBinder.class, Application.class, Object.class);
                        IActivityManager am = mPluginManager.getActivityManager();

                        attach.invoke(service, plugin.getPluginContext(), mainThread, component.getClassName(), token, app, am);
                        service.onCreate();
                        this.mPluginManager.getComponentsHandler().rememberService(component, service);
                    } catch (Throwable t) {
                        return START_STICKY;
                    }
                }

                service.onStartCommand(target, 0, this.mPluginManager.getComponentsHandler().getServiceCounter(service).getAndIncrement());
                break;
            }
            case EXTRA_COMMAND_BIND_SERVICE: {
                ActivityThread mainThread = ActivityThread.currentActivityThread();
                IApplicationThread appThread = mainThread.getApplicationThread();
                Service service = null;

                if (this.mPluginManager.getComponentsHandler().isServiceAvailable(component)) {
                    service = this.mPluginManager.getComponentsHandler().getService(component);
                } else {
                    try {
                        service = (Service) plugin.getClassLoader().loadClass(component.getClassName()).newInstance();

                        Application app = plugin.getApplication();
                        IBinder token = appThread.asBinder();
                        Method attach = service.getClass().getMethod("attach", Context.class, ActivityThread.class, String.class, IBinder.class, Application.class, Object.class);
                        IActivityManager am = mPluginManager.getActivityManager();

                        attach.invoke(service, plugin.getPluginContext(), mainThread, component.getClassName(), token, app, am);
                        service.onCreate();
                        this.mPluginManager.getComponentsHandler().rememberService(component, service);
                    } catch (Throwable t) {
                        Log.w(TAG, t);
                    }
                }
                try {
                    IBinder binder = service.onBind(target);
                    IBinder serviceConnection = PluginUtil.getBinder(intent.getExtras(), "sc");
                    IServiceConnection iServiceConnection = IServiceConnection.Stub.asInterface(serviceConnection);
                    if (Build.VERSION.SDK_INT >= 26) {
                        iServiceConnection.connected(component, binder, false);
                    } else {
                        Reflector.QuietReflector.with(iServiceConnection).method("connected", ComponentName.class, IBinder.class).call(component, binder);
                    }
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
                break;
            }
            case EXTRA_COMMAND_STOP_SERVICE: {
                Service service = this.mPluginManager.getComponentsHandler().forgetService(component);
                if (null != service) {
                    try {
                        service.onDestroy();
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to stop service " + service + ": " + e.toString());
                    }
                } else {
                    Log.i(TAG, component + " not found");
                }
                break;
            }
            case EXTRA_COMMAND_UNBIND_SERVICE: {
                Service service = this.mPluginManager.getComponentsHandler().forgetService(component);
                if (null != service) {
                    try {
                        service.onUnbind(target);
                        service.onDestroy();
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to unbind service " + service + ": " + e.toString());
                    }
                } else {
                    Log.i(TAG, component + " not found");
                }
                break;
            }
        }

        return START_STICKY;
    }

}
