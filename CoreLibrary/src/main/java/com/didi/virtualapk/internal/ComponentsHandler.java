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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.IBinder;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.didi.virtualapk.PluginManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by renyugang on 17/6/7.
 */

public class ComponentsHandler {

    public static final String TAG = "PluginManager";

    private Context mContext;
    private PluginManager mPluginManager;
    private StubActivityInfo mStubActivityInfo = new StubActivityInfo();


    private ArrayMap<ComponentName, Service> mServices = new ArrayMap<ComponentName, Service>();
    private ArrayMap<IBinder, Intent> mBoundServices = new ArrayMap<IBinder, Intent>();
    private ArrayMap<Service, AtomicInteger> mServiceCounters = new ArrayMap<Service, AtomicInteger>();

    public ComponentsHandler(PluginManager pluginManager) {
        mPluginManager = pluginManager;
        mContext = pluginManager.getHostContext();
    }

    /**
     * transform intent from implicit to explicit
     */
    public Intent transformIntentToExplicitAsNeeded(Intent intent) {
        ComponentName component = intent.getComponent();
        if (component == null
            || component.getPackageName().equals(mContext.getPackageName())) {
            ResolveInfo info = mPluginManager.resolveActivity(intent);
            if (info != null && info.activityInfo != null) {
                component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                intent.setComponent(component);
            }
        }

        return intent;
    }

    public void markIntentIfNeeded(Intent intent) {
        if (intent.getComponent() == null) {
            return;
        }

        String targetPackageName = intent.getComponent().getPackageName();
        String targetClassName = intent.getComponent().getClassName();
        // search map and return specific launchmode stub activity
        if (!targetPackageName.equals(mContext.getPackageName()) && mPluginManager.getLoadedPlugin(targetPackageName) != null) {
            intent.putExtra(Constants.KEY_IS_PLUGIN, true);
            intent.putExtra(Constants.KEY_TARGET_PACKAGE, targetPackageName);
            intent.putExtra(Constants.KEY_TARGET_ACTIVITY, targetClassName);
            dispatchStubActivity(intent);
        }
    }

    private void dispatchStubActivity(Intent intent) {
        ComponentName component = intent.getComponent();
        String targetClassName = intent.getComponent().getClassName();
        LoadedPlugin loadedPlugin = mPluginManager.getLoadedPlugin(intent);
        ActivityInfo info = loadedPlugin.getActivityInfo(component);
        if (info == null) {
            throw new RuntimeException("can not find " + component);
        }
        int launchMode = info.launchMode;
        Resources.Theme themeObj = loadedPlugin.getResources().newTheme();
        themeObj.applyStyle(info.theme, true);
        String stubActivity = mStubActivityInfo.getStubActivity(targetClassName, launchMode, themeObj);
        Log.i(TAG, String.format("dispatchStubActivity,[%s -> %s]", targetClassName, stubActivity));
        intent.setClassName(mContext, stubActivity);
    }


    public AtomicInteger getServiceCounter(Service service) {
        return this.mServiceCounters.get(service);
    }

    /**
     * Retrieve the started service by component name
     *
     * @param component
     * @return
     */
    public Service getService(ComponentName component) {
        return this.mServices.get(component);
    }

    /**
     * Put the started service into service registry, and then increase the counter associate with
     * the service
     *
     * @param component
     * @param service
     */
    public void rememberService(ComponentName component, Service service) {
        synchronized (this.mServices) {
            this.mServices.put(component, service);
            this.mServiceCounters.put(service, new AtomicInteger(0));
        }
    }

    /**
     * Remove the service from service registry
     *
     * @param component
     * @return
     */
    public Service forgetService(ComponentName component) {
        synchronized (this.mServices) {
            Service service = this.mServices.remove(component);
            this.mServiceCounters.remove(service);
            return service;
        }
    }

    /**
     * Remove the bound service from service registry
     *
     * @param iServiceConnection IServiceConnection binder when unbindService
     * @return
     */
    public Intent forgetIServiceConnection(IBinder iServiceConnection) {
        synchronized (this.mBoundServices) {
            Intent intent = this.mBoundServices.remove(iServiceConnection);
            return intent;
        }
    }

    /**
     * save the bound service
     *
     * @param iServiceConnection IServiceConnection binder when bindService
     * @return
     */
    public void remberIServiceConnection(IBinder iServiceConnection, Intent intent) {
        synchronized (this.mBoundServices) {
            mBoundServices.put(iServiceConnection, intent);
        }
    }

    /**
     * Check if a started service with the specified component exists in the registry
     *
     * @param component
     * @return
     */
    public boolean isServiceAvailable(ComponentName component) {
        return this.mServices.containsKey(component);
    }

}
