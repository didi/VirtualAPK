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
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Singleton;

import com.didi.virtualapk.delegate.ActivityManagerProxy;
import com.didi.virtualapk.delegate.IContentProviderProxy;
import com.didi.virtualapk.delegate.RemoteContentProvider;
import com.didi.virtualapk.internal.ComponentsHandler;
import com.didi.virtualapk.internal.Constants;
import com.didi.virtualapk.internal.LoadedPlugin;
import com.didi.virtualapk.internal.VAInstrumentation;
import com.didi.virtualapk.internal.utils.PluginUtil;
import com.didi.virtualapk.utils.Reflector;
import com.didi.virtualapk.utils.RunUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by renyugang on 16/8/9.
 */
public class PluginManager {

    public static final String TAG = Constants.TAG_PREFIX + "PluginManager";

    private static volatile PluginManager sInstance = null;

    // Context of host app
    protected final Context mContext;
    protected final Application mApplication;
    protected ComponentsHandler mComponentsHandler;
    protected final Map<String, LoadedPlugin> mPlugins = new ConcurrentHashMap<>();
    protected final List<Callback> mCallbacks = new ArrayList<>();

    protected VAInstrumentation mInstrumentation; // Hooked instrumentation
    protected IActivityManager mActivityManager; // Hooked IActivityManager binder
    protected IContentProvider mIContentProvider; // Hooked IContentProvider binder

    public static PluginManager getInstance(Context base) {
        if (sInstance == null) {
            synchronized (PluginManager.class) {
                if (sInstance == null) {
                    sInstance = createInstance(base);
                }
            }
        }

        return sInstance;
    }
    
    private static PluginManager createInstance(Context context) {
        try {
            Bundle metaData = context.getPackageManager()
                                     .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)
                                     .metaData;
            
            if (metaData == null) {
                return new PluginManager(context);
            }
            
            String factoryClass = metaData.getString("VA_FACTORY");
            
            if (factoryClass == null) {
                return new PluginManager(context);
            }
            
            PluginManager pluginManager = Reflector.on(factoryClass).method("create", Context.class).call(context);
            
            if (pluginManager != null) {
                Log.d(TAG, "Created a instance of " + pluginManager.getClass());
                return pluginManager;
            }
    
        } catch (Exception e) {
            Log.w(TAG, "Created the instance error!", e);
        }
    
        return new PluginManager(context);
    }

    protected PluginManager(Context context) {
        if (context instanceof Application) {
            this.mApplication = (Application) context;
            this.mContext = mApplication.getBaseContext();
        } else {
            final Context app = context.getApplicationContext();
            if (app == null) {
                this.mContext = context;
                this.mApplication = ActivityThread.currentApplication();
            } else {
                this.mApplication = (Application) app;
                this.mContext = mApplication.getBaseContext();
            }
        }
        
        mComponentsHandler = createComponentsHandler();
        hookCurrentProcess();
    }

    protected void hookCurrentProcess() {
        hookInstrumentationAndHandler();
        hookSystemServices();
        hookDataBindingUtil();
    }

    public void init() {
        RunUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                doInWorkThread();
            }
        });
    }

    protected void doInWorkThread() {
    }
    
    public Application getHostApplication() {
        return this.mApplication;
    }
    
    protected ComponentsHandler createComponentsHandler() {
        return new ComponentsHandler(this);
    }
    
    protected VAInstrumentation createInstrumentation(Instrumentation origin) throws Exception {
        return new VAInstrumentation(this, origin);
    }
    
    protected ActivityManagerProxy createActivityManagerProxy(IActivityManager origin) throws Exception {
        return new ActivityManagerProxy(this, origin);
    }
    
    protected LoadedPlugin createLoadedPlugin(File apk) throws Exception {
        return new LoadedPlugin(this, this.mContext, apk);
    }
    
    protected void hookDataBindingUtil() {
        Reflector.QuietReflector reflector = Reflector.QuietReflector.on("android.databinding.DataBindingUtil").field("sMapper");
        Object old = reflector.get();
        if (old != null) {
            try {
                Callback callback = Reflector.on("android.databinding.DataBinderMapperProxy").constructor().newInstance();
                reflector.set(callback);
                addCallback(callback);
                Log.d(TAG, "hookDataBindingUtil succeed : " + callback);
            } catch (Reflector.ReflectedException e) {
                Log.w(TAG, e);
            }
        }
    }
    
    public void addCallback(Callback callback) {
        if (callback == null) {
            return;
        }
        synchronized (mCallbacks) {
            if (mCallbacks.contains(callback)) {
                throw new RuntimeException("Already added " + callback + "!");
            }
            mCallbacks.add(callback);
        }
    }
    
    public void removeCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    /**
     * hookSystemServices, but need to compatible with Android O in future.
     */
    protected void hookSystemServices() {
        try {
            Singleton<IActivityManager> defaultSingleton;
    
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                defaultSingleton = Reflector.on(ActivityManager.class).field("IActivityManagerSingleton").get();
            } else {
                defaultSingleton = Reflector.on(ActivityManagerNative.class).field("gDefault").get();
            }
            IActivityManager origin = defaultSingleton.get();
            IActivityManager activityManagerProxy = (IActivityManager) Proxy.newProxyInstance(mContext.getClassLoader(), new Class[] { IActivityManager.class },
                createActivityManagerProxy(origin));

            // Hook IActivityManager from ActivityManagerNative
            Reflector.with(defaultSingleton).field("mInstance").set(activityManagerProxy);

            if (defaultSingleton.get() == activityManagerProxy) {
                this.mActivityManager = activityManagerProxy;
                Log.d(TAG, "hookSystemServices succeed : " + mActivityManager);
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }
    
    protected void hookInstrumentationAndHandler() {
        try {
            ActivityThread activityThread = ActivityThread.currentActivityThread();
            Instrumentation baseInstrumentation = activityThread.getInstrumentation();
//            if (baseInstrumentation.getClass().getName().contains("lbe")) {
//                // reject executing in paralell space, for example, lbe.
//                System.exit(0);
//            }
    
            final VAInstrumentation instrumentation = createInstrumentation(baseInstrumentation);
            
            Reflector.with(activityThread).field("mInstrumentation").set(instrumentation);
            Handler mainHandler = Reflector.with(activityThread).method("getHandler").call();
            Reflector.with(mainHandler).field("mCallback").set(instrumentation);
            this.mInstrumentation = instrumentation;
            Log.d(TAG, "hookInstrumentationAndHandler succeed : " + mInstrumentation);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    protected void hookIContentProviderAsNeeded() {
        Uri uri = Uri.parse(RemoteContentProvider.getUri(mContext));
        mContext.getContentResolver().call(uri, "wakeup", null, null);
        try {
            Field authority = null;
            Field provider = null;
            ActivityThread activityThread = ActivityThread.currentActivityThread();
            Map providerMap = Reflector.with(activityThread).field("mProviderMap").get();
            Iterator iter = providerMap.entrySet().iterator();
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
                if (auth.equals(RemoteContentProvider.getAuthority(mContext))) {
                    if (provider == null) {
                        provider = val.getClass().getDeclaredField("mProvider");
                        provider.setAccessible(true);
                    }
                    IContentProvider rawProvider = (IContentProvider) provider.get(val);
                    IContentProvider proxy = IContentProviderProxy.newInstance(mContext, rawProvider);
                    mIContentProvider = proxy;
                    Log.d(TAG, "hookIContentProvider succeed : " + mIContentProvider);
                    break;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
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
            // throw the FileNotFoundException by opening a stream.
            InputStream in = new FileInputStream(apk);
            in.close();
        }

        LoadedPlugin plugin = createLoadedPlugin(apk);
        
        if (null == plugin) {
            throw new RuntimeException("Can't load plugin which is invalid: " + apk.getAbsolutePath());
        }
        
        this.mPlugins.put(plugin.getPackageName(), plugin);
        synchronized (mCallbacks) {
            for (int i = 0; i < mCallbacks.size(); i++) {
                mCallbacks.get(i).onAddedLoadedPlugin(plugin);
            }
        }
    }

    public LoadedPlugin getLoadedPlugin(Intent intent) {
        return getLoadedPlugin(PluginUtil.getComponent(intent));
    }

    public LoadedPlugin getLoadedPlugin(ComponentName component) {
        if (component == null) {
            return null;
        }
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

    public VAInstrumentation getInstrumentation() {
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