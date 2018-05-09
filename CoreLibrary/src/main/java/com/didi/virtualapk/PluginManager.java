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

package com.didi.virtualapk;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.Application;
import android.app.IActivityManager;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.IContentProvider;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.databinding.DataBinderMapperProxy;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.Singleton;

import com.didi.virtualapk.delegate.IContentProviderProxy;
import com.didi.virtualapk.internal.PluginContentResolver;
import com.didi.virtualapk.delegate.ActivityManagerProxy;
import com.didi.virtualapk.internal.ComponentsHandler;
import com.didi.virtualapk.internal.LoadedPlugin;
import com.didi.virtualapk.internal.VAInstrumentation;
import com.didi.virtualapk.utils.PluginUtil;
import com.didi.virtualapk.utils.ReflectUtil;
import com.didi.virtualapk.utils.RunUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by renyugang on 16/8/9.
 */
public class PluginManager {

    public static final String TAG = "PluginManager";

    private static volatile PluginManager sInstance = null;

    // Context of host app
    private Context mContext;
    private ComponentsHandler mComponentsHandler;
    private Map<String, LoadedPlugin> mPlugins = new ConcurrentHashMap<>();
    private final List<Callback> mCallbacks = new ArrayList<>();

    private Instrumentation mInstrumentation; // Hooked instrumentation
    private IActivityManager mActivityManager; // Hooked IActivityManager binder
    private IContentProvider mIContentProvider; // Hooked IContentProvider binder

    public static PluginManager getInstance(Context base) {
        if (sInstance == null) {
            synchronized (PluginManager.class) {
                if (sInstance == null)
                    sInstance = new PluginManager(base);
            }
        }

        return sInstance;
    }

    private PluginManager(Context context) {
        Context app = context.getApplicationContext();
        if (app == null) {
            this.mContext = context;
        } else {
            this.mContext = ((Application)app).getBaseContext();
        }
        prepare();
    }

    private void prepare() {
        Systems.sHostContext = getHostContext();
        this.hookInstrumentationAndHandler();
        this.hookSystemServices();
        hookDataBindingUtil();
    }

    public void init() {
        mComponentsHandler = new ComponentsHandler(this);
        RunUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                doInWorkThread();
            }
        });
    }

    private void doInWorkThread() {
    }
    
    private void hookDataBindingUtil() {
        try {
            Class cls = Class.forName("android.databinding.DataBindingUtil");
            Object old = ReflectUtil.getField(cls, null, "sMapper");
            Callback callback = new DataBinderMapperProxy(old);
            ReflectUtil.setField(cls, null, "sMapper", callback);
            
            addCallback(callback);
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void addCallback(Callback callback) {
        if (callback == null) {
            return;
        }
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    /**
     * hookSystemServices, but need to compatible with Android O in future.
     */
    private void hookSystemServices() {
        try {
            Singleton<IActivityManager> defaultSingleton;
    
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                defaultSingleton = (Singleton<IActivityManager>) ReflectUtil.getField(ActivityManager.class, null, "IActivityManagerSingleton");
            } else {
                defaultSingleton = (Singleton<IActivityManager>) ReflectUtil.getField(ActivityManagerNative.class, null, "gDefault");
            }
            IActivityManager activityManagerProxy = ActivityManagerProxy.newInstance(this, defaultSingleton.get());

            // Hook IActivityManager from ActivityManagerNative
            ReflectUtil.setField(defaultSingleton.getClass().getSuperclass(), defaultSingleton, "mInstance", activityManagerProxy);

            if (defaultSingleton.get() == activityManagerProxy) {
                this.mActivityManager = activityManagerProxy;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hookInstrumentationAndHandler() {
        try {
            Instrumentation baseInstrumentation = ReflectUtil.getInstrumentation(this.mContext);
            if (baseInstrumentation.getClass().getName().contains("lbe")) {
                // reject executing in paralell space, for example, lbe.
                System.exit(0);
            }

            final VAInstrumentation instrumentation = new VAInstrumentation(this, baseInstrumentation);
            Object activityThread = ReflectUtil.getActivityThread(this.mContext);
            ReflectUtil.setInstrumentation(activityThread, instrumentation);
            ReflectUtil.setHandlerCallback(this.mContext, instrumentation);
            this.mInstrumentation = instrumentation;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hookIContentProviderAsNeeded() {
        Uri uri = Uri.parse(PluginContentResolver.getUri(mContext));
        mContext.getContentResolver().call(uri, "wakeup", null, null);
        try {
            Field authority = null;
            Field mProvider = null;
            ActivityThread activityThread = (ActivityThread) ReflectUtil.getActivityThread(mContext);
            Map mProviderMap = (Map) ReflectUtil.getField(activityThread.getClass(), activityThread, "mProviderMap");
            Iterator iter = mProviderMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Object key = entry.getKey();
                Object val = entry.getValue();
                String auth;
                if (key instanceof String) {
                    auth = (String) key;
                } else {
                    if (authority == null) {
                        authority = key.getClass().getDeclaredField("authority");
                        authority.setAccessible(true);
                    }
                    auth = (String) authority.get(key);
                }
                if (auth.equals(PluginContentResolver.getAuthority(mContext))) {
                    if (mProvider == null) {
                        mProvider = val.getClass().getDeclaredField("mProvider");
                        mProvider.setAccessible(true);
                    }
                    IContentProvider rawProvider = (IContentProvider) mProvider.get(val);
                    IContentProvider proxy = IContentProviderProxy.newInstance(mContext, rawProvider);
                    mIContentProvider = proxy;
                    Log.d(TAG, "hookIContentProvider succeed : " + mIContentProvider);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * load a plugin into memory, then invoke it's Application.
     * @param apk the file of plugin, should end with .apk
     * @throws Exception
     */
    public void loadPlugin(File apk) throws Exception {
        if (null == apk) {
            throw new IllegalArgumentException("error : apk is null.");
        }

        if (!apk.exists()) {
            throw new FileNotFoundException(apk.getAbsolutePath());
        }

        LoadedPlugin plugin = LoadedPlugin.create(this, this.mContext, apk);
        if (null != plugin) {
            this.mPlugins.put(plugin.getPackageName(), plugin);
            synchronized (mCallbacks) {
                for (int i = 0; i < mCallbacks.size(); i++) {
                    mCallbacks.get(i).onAddedLoadedPlugin(plugin);
                }
            }
            // try to invoke plugin's application
            plugin.invokeApplication();
        } else {
            throw  new RuntimeException("Can't load plugin which is invalid: " + apk.getAbsolutePath());
        }
    }

    public LoadedPlugin getLoadedPlugin(Intent intent) {
        ComponentName component = PluginUtil.getComponent(intent);
        return getLoadedPlugin(component.getPackageName());
    }

    public LoadedPlugin getLoadedPlugin(ComponentName component) {
        return this.getLoadedPlugin(component.getPackageName());
    }

    public LoadedPlugin getLoadedPlugin(String packageName) {
        return this.mPlugins.get(packageName);
    }

    public List<LoadedPlugin> getAllLoadedPlugins() {
        List<LoadedPlugin> list = new ArrayList<>();
        list.addAll(mPlugins.values());
        return list;
    }

    public Context getHostContext() {
        return this.mContext;
    }

    public Instrumentation getInstrumentation() {
        return this.mInstrumentation;
    }

    public IActivityManager getActivityManager() {
        return this.mActivityManager;
    }

    public synchronized IContentProvider getIContentProvider() {
        if (mIContentProvider == null) {
            hookIContentProviderAsNeeded();
        }

        return mIContentProvider;
    }

    public ComponentsHandler getComponentsHandler() {
        return mComponentsHandler;
    }

    public ResolveInfo resolveActivity(Intent intent) {
        return this.resolveActivity(intent, 0);
    }

    public ResolveInfo resolveActivity(Intent intent, int flags) {
        for (LoadedPlugin plugin : this.mPlugins.values()) {
            ResolveInfo resolveInfo = plugin.resolveActivity(intent, flags);
            if (null != resolveInfo) {
                return resolveInfo;
            }
        }

        return null;
    }

    public ResolveInfo resolveService(Intent intent, int flags) {
        for (LoadedPlugin plugin : this.mPlugins.values()) {
            ResolveInfo resolveInfo = plugin.resolveService(intent, flags);
            if (null != resolveInfo) {
                return resolveInfo;
            }
        }

        return null;
    }

    public ProviderInfo resolveContentProvider(String name, int flags) {
        for (LoadedPlugin plugin : this.mPlugins.values()) {
            ProviderInfo providerInfo = plugin.resolveContentProvider(name, flags);
            if (null != providerInfo) {
                return providerInfo;
            }
        }

        return null;
    }

    /**
     * used in PluginPackageManager, do not invoke it from outside.
     */
    @Deprecated
    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();

        for (LoadedPlugin plugin : this.mPlugins.values()) {
            List<ResolveInfo> result = plugin.queryIntentActivities(intent, flags);
            if (null != result && result.size() > 0) {
                resolveInfos.addAll(result);
            }
        }

        return resolveInfos;
    }

    /**
     * used in PluginPackageManager, do not invoke it from outside.
     */
    @Deprecated
    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();

        for (LoadedPlugin plugin : this.mPlugins.values()) {
            List<ResolveInfo> result = plugin.queryIntentServices(intent, flags);
            if (null != result && result.size() > 0) {
                resolveInfos.addAll(result);
            }
        }

        return resolveInfos;
    }

    /**
     * used in PluginPackageManager, do not invoke it from outside.
     */
    @Deprecated
    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();

        for (LoadedPlugin plugin : this.mPlugins.values()) {
            List<ResolveInfo> result = plugin.queryBroadcastReceivers(intent, flags);
            if (null != result && result.size() > 0) {
                resolveInfos.addAll(result);
            }
        }

        return resolveInfos;
    }

    public interface Callback {
        void onAddedLoadedPlugin(LoadedPlugin plugin);
    }
}