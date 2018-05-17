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

import android.annotation.TargetApi;
import android.app.Application;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.VersionedPackage;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.utils.DexUtil;
import com.didi.virtualapk.utils.PackageParserCompat;
import com.didi.virtualapk.utils.PluginUtil;
import com.didi.virtualapk.utils.ReflectUtil;
import com.didi.virtualapk.utils.RunUtil;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;

/**
 * Created by renyugang on 16/8/9.
 */
public final class LoadedPlugin {

    public static final String TAG = "LoadedPlugin";

    public static LoadedPlugin create(PluginManager pluginManager, Context host, File apk) throws Exception {
        return new LoadedPlugin(pluginManager, host, apk);
    }

    private static ClassLoader createClassLoader(Context context, File apk, File libsDir, ClassLoader parent) {
        File dexOutputDir = context.getDir(Constants.OPTIMIZE_DIR, Context.MODE_PRIVATE);
        String dexOutputPath = dexOutputDir.getAbsolutePath();
        DexClassLoader loader = new DexClassLoader(apk.getAbsolutePath(), dexOutputPath, libsDir.getAbsolutePath(), parent);

        if (Constants.COMBINE_CLASSLOADER) {
            try {
                DexUtil.insertDex(loader);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return loader;
    }

    private static AssetManager createAssetManager(Context context, File apk) {
        try {
            AssetManager am = AssetManager.class.newInstance();
            ReflectUtil.invoke(AssetManager.class, am, "addAssetPath", apk.getAbsolutePath());
            return am;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @WorkerThread
    private static Resources createResources(Context context, File apk) {
        if (Constants.COMBINE_RESOURCES) {
            Resources resources = ResourcesManager.createResources(context, apk.getAbsolutePath());
            ResourcesManager.hookResources(context, resources);
            return resources;
        } else {
            Resources hostResources = context.getResources();
            AssetManager assetManager = createAssetManager(context, apk);
            return new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
        }
    }

    private static ResolveInfo chooseBestActivity(Intent intent, String s, int flags, List<ResolveInfo> query) {
        return query.get(0);
    }

    private final String mLocation;
    private PluginManager mPluginManager;
    private Context mHostContext;
    private Context mPluginContext;
    private final File mNativeLibDir;
    private final PackageParser.Package mPackage;
    private final PackageInfo mPackageInfo;
    private Resources mResources;
    private ClassLoader mClassLoader;
    private PluginPackageManager mPackageManager;

    private Map<ComponentName, ActivityInfo> mActivityInfos;
    private Map<ComponentName, ServiceInfo> mServiceInfos;
    private Map<ComponentName, ActivityInfo> mReceiverInfos;
    private Map<ComponentName, ProviderInfo> mProviderInfos;
    private Map<String, ProviderInfo> mProviders; // key is authorities of provider
    private Map<ComponentName, InstrumentationInfo> mInstrumentationInfos;

    private Application mApplication;

    LoadedPlugin(PluginManager pluginManager, Context context, File apk) throws Exception {
        this.mPluginManager = pluginManager;
        this.mHostContext = context;
        this.mLocation = apk.getAbsolutePath();
        this.mPackage = PackageParserCompat.parsePackage(context, apk, PackageParser.PARSE_MUST_BE_APK);
        this.mPackage.applicationInfo.metaData = this.mPackage.mAppMetaData;
        this.mPackageInfo = new PackageInfo();
        this.mPackageInfo.applicationInfo = this.mPackage.applicationInfo;
        this.mPackageInfo.applicationInfo.sourceDir = apk.getAbsolutePath();
    
        if (Build.VERSION.SDK_INT >= 28
            || (Build.VERSION.SDK_INT == 27 && Build.VERSION.PREVIEW_SDK_INT != 0)) { // Android P Preview
            try {
                this.mPackageInfo.signatures = this.mPackage.mSigningDetails.signatures;
            } catch (Throwable e) {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
                this.mPackageInfo.signatures = info.signatures;
            }
        } else {
            this.mPackageInfo.signatures = this.mPackage.mSignatures;
        }
        this.mPackageInfo.packageName = this.mPackage.packageName;
        if (pluginManager.getLoadedPlugin(mPackageInfo.packageName) != null) {
            throw new RuntimeException("plugin has already been loaded : " + mPackageInfo.packageName);
        }
        this.mPackageInfo.versionCode = this.mPackage.mVersionCode;
        this.mPackageInfo.versionName = this.mPackage.mVersionName;
        this.mPackageInfo.permissions = new PermissionInfo[0];
        this.mPackageManager = new PluginPackageManager();
        this.mPluginContext = new PluginContext(this);
        this.mNativeLibDir = context.getDir(Constants.NATIVE_DIR, Context.MODE_PRIVATE);
        this.mResources = createResources(context, apk);
        this.mClassLoader = createClassLoader(context, apk, this.mNativeLibDir, context.getClassLoader());

        tryToCopyNativeLib(apk);

        // Cache instrumentations
        Map<ComponentName, InstrumentationInfo> instrumentations = new HashMap<ComponentName, InstrumentationInfo>();
        for (PackageParser.Instrumentation instrumentation : this.mPackage.instrumentation) {
            instrumentations.put(instrumentation.getComponentName(), instrumentation.info);
        }
        this.mInstrumentationInfos = Collections.unmodifiableMap(instrumentations);
        this.mPackageInfo.instrumentation = instrumentations.values().toArray(new InstrumentationInfo[instrumentations.size()]);

        // Cache activities
        Map<ComponentName, ActivityInfo> activityInfos = new HashMap<ComponentName, ActivityInfo>();
        for (PackageParser.Activity activity : this.mPackage.activities) {
            activityInfos.put(activity.getComponentName(), activity.info);
        }
        this.mActivityInfos = Collections.unmodifiableMap(activityInfos);
        this.mPackageInfo.activities = activityInfos.values().toArray(new ActivityInfo[activityInfos.size()]);

        // Cache services
        Map<ComponentName, ServiceInfo> serviceInfos = new HashMap<ComponentName, ServiceInfo>();
        for (PackageParser.Service service : this.mPackage.services) {
            serviceInfos.put(service.getComponentName(), service.info);
        }
        this.mServiceInfos = Collections.unmodifiableMap(serviceInfos);
        this.mPackageInfo.services = serviceInfos.values().toArray(new ServiceInfo[serviceInfos.size()]);

        // Cache providers
        Map<String, ProviderInfo> providers = new HashMap<String, ProviderInfo>();
        Map<ComponentName, ProviderInfo> providerInfos = new HashMap<ComponentName, ProviderInfo>();
        for (PackageParser.Provider provider : this.mPackage.providers) {
            providers.put(provider.info.authority, provider.info);
            providerInfos.put(provider.getComponentName(), provider.info);
        }
        this.mProviders = Collections.unmodifiableMap(providers);
        this.mProviderInfos = Collections.unmodifiableMap(providerInfos);
        this.mPackageInfo.providers = providerInfos.values().toArray(new ProviderInfo[providerInfos.size()]);

        // Register broadcast receivers dynamically
        Map<ComponentName, ActivityInfo> receivers = new HashMap<ComponentName, ActivityInfo>();
        for (PackageParser.Activity receiver : this.mPackage.receivers) {
            receivers.put(receiver.getComponentName(), receiver.info);
    
            BroadcastReceiver br = BroadcastReceiver.class.cast(getClassLoader().loadClass(receiver.getComponentName().getClassName()).newInstance());
            for (PackageParser.ActivityIntentInfo aii : receiver.intents) {
                this.mHostContext.registerReceiver(br, aii);
            }
        }
        this.mReceiverInfos = Collections.unmodifiableMap(receivers);
        this.mPackageInfo.receivers = receivers.values().toArray(new ActivityInfo[receivers.size()]);
    }

    private void tryToCopyNativeLib(File apk) throws Exception {
        PluginUtil.copyNativeLib(apk, mHostContext, mPackageInfo, mNativeLibDir);
    }

    public String getLocation() {
        return this.mLocation;
    }

    public String getPackageName() {
        return this.mPackage.packageName;
    }

    public PackageManager getPackageManager() {
        return this.mPackageManager;
    }

    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    public Resources getResources() {
        return this.mResources;
    }

    public void updateResources(Resources newResources) {
        this.mResources = newResources;
    }

    public ClassLoader getClassLoader() {
        return this.mClassLoader;
    }

    public PluginManager getPluginManager() {
        return this.mPluginManager;
    }

    public Context getHostContext() {
        return this.mHostContext;
    }

    public Context getPluginContext() {
        return this.mPluginContext;
    }

    public Application getApplication() {
        return mApplication;
    }

    public void invokeApplication() {
        if (mApplication != null) {
            return;
        }

        // make sure application's callback is run on ui thread.
        RunUtil.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mApplication = makeApplication(false, mPluginManager.getInstrumentation());
            }
        }, true);
    }

    public String getPackageResourcePath() {
        int myUid = Process.myUid();
        ApplicationInfo appInfo = this.mPackage.applicationInfo;
        return appInfo.uid == myUid ? appInfo.sourceDir : appInfo.publicSourceDir;
    }

    public String getCodePath() {
        return this.mPackage.applicationInfo.sourceDir;
    }

    public Intent getLaunchIntent() {
        ContentResolver resolver = this.mPluginContext.getContentResolver();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        for (PackageParser.Activity activity : this.mPackage.activities) {
            for (PackageParser.ActivityIntentInfo intentInfo : activity.intents) {
                if (intentInfo.match(resolver, launcher, false, TAG) > 0) {
                    return Intent.makeMainActivity(activity.getComponentName());
                }
            }
        }

        return null;
    }

    public Intent getLeanbackLaunchIntent() {
        ContentResolver resolver = this.mPluginContext.getContentResolver();
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);

        for (PackageParser.Activity activity : this.mPackage.activities) {
            for (PackageParser.ActivityIntentInfo intentInfo : activity.intents) {
                if (intentInfo.match(resolver, launcher, false, TAG) > 0) {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(activity.getComponentName());
                    intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
                    return intent;
                }
            }
        }

        return null;
    }

    public ApplicationInfo getApplicationInfo() {
        return this.mPackage.applicationInfo;
    }

    public PackageInfo getPackageInfo() {
        return this.mPackageInfo;
    }

    public ActivityInfo getActivityInfo(ComponentName componentName) {
        return this.mActivityInfos.get(componentName);
    }

    public ServiceInfo getServiceInfo(ComponentName componentName) {
        return this.mServiceInfos.get(componentName);
    }

    public ActivityInfo getReceiverInfo(ComponentName componentName) {
        return this.mReceiverInfos.get(componentName);
    }

    public ProviderInfo getProviderInfo(ComponentName componentName) {
        return this.mProviderInfos.get(componentName);
    }

    public Resources.Theme getTheme() {
        Resources.Theme theme = this.mResources.newTheme();
        theme.applyStyle(PluginUtil.selectDefaultTheme(this.mPackage.applicationInfo.theme, Build.VERSION.SDK_INT), false);
        return theme;
    }

    public void setTheme(int resid) {
        try {
            ReflectUtil.setField(Resources.class, this.mResources, "mThemeResId", resid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Application makeApplication(boolean forceDefaultAppClass, Instrumentation instrumentation) {
        if (null != this.mApplication) {
            return this.mApplication;
        }

        String appClass = this.mPackage.applicationInfo.className;
        if (forceDefaultAppClass || null == appClass) {
            appClass = "android.app.Application";
        }

        try {
            this.mApplication = instrumentation.newApplication(this.mClassLoader, appClass, this.getPluginContext());
            instrumentation.callApplicationOnCreate(this.mApplication);
            return this.mApplication;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ResolveInfo resolveActivity(Intent intent, int flags) {
        List<ResolveInfo> query = this.queryIntentActivities(intent, flags);
        if (null == query || query.isEmpty()) {
            return null;
        }

        ContentResolver resolver = this.mPluginContext.getContentResolver();
        return chooseBestActivity(intent, intent.resolveTypeIfNeeded(resolver), flags, query);
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        ComponentName component = intent.getComponent();
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        ContentResolver resolver = this.mPluginContext.getContentResolver();

        for (PackageParser.Activity activity : this.mPackage.activities) {
            if (match(activity, component)) {
                ResolveInfo resolveInfo = new ResolveInfo();
                resolveInfo.activityInfo = activity.info;
                resolveInfos.add(resolveInfo);
            } else if (component == null) {
                // only match implicit intent
                for (PackageParser.ActivityIntentInfo intentInfo : activity.intents) {
                    if (intentInfo.match(resolver, intent, true, TAG) >= 0) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.activityInfo = activity.info;
                        resolveInfos.add(resolveInfo);
                        break;
                    }
                }
            }
        }

        return resolveInfos;
    }

    public ResolveInfo resolveService(Intent intent, int flags) {
        List<ResolveInfo> query = this.queryIntentServices(intent, flags);
        if (null == query || query.isEmpty()) {
            return null;
        }

        ContentResolver resolver = this.mPluginContext.getContentResolver();
        return chooseBestActivity(intent, intent.resolveTypeIfNeeded(resolver), flags, query);
    }

    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        ComponentName component = intent.getComponent();
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        ContentResolver resolver = this.mPluginContext.getContentResolver();

        for (PackageParser.Service service : this.mPackage.services) {
            if (match(service, component)) {
                ResolveInfo resolveInfo = new ResolveInfo();
                resolveInfo.serviceInfo = service.info;
                resolveInfos.add(resolveInfo);
            } else if (component == null) {
                // only match implicit intent
                for (PackageParser.ServiceIntentInfo intentInfo : service.intents) {
                    if (intentInfo.match(resolver, intent, true, TAG) >= 0) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.serviceInfo = service.info;
                        resolveInfos.add(resolveInfo);
                        break;
                    }
                }
            }
        }

        return resolveInfos;
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        ComponentName component = intent.getComponent();
        List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();
        ContentResolver resolver = this.mPluginContext.getContentResolver();

        for (PackageParser.Activity receiver : this.mPackage.receivers) {
            if (receiver.getComponentName().equals(component)) {
                ResolveInfo resolveInfo = new ResolveInfo();
                resolveInfo.activityInfo = receiver.info;
                resolveInfos.add(resolveInfo);
            } else if (component == null) {
                // only match implicit intent
                for (PackageParser.ActivityIntentInfo intentInfo : receiver.intents) {
                    if (intentInfo.match(resolver, intent, true, TAG) >= 0) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.activityInfo = receiver.info;
                        resolveInfos.add(resolveInfo);
                        break;
                    }
                }
            }
        }

        return resolveInfos;
    }

    public ProviderInfo resolveContentProvider(String name, int flags) {
        return this.mProviders.get(name);
    }

    private boolean match(PackageParser.Component component, ComponentName target) {
        ComponentName source = component.getComponentName();
        if (source == target) return true;
        if (source != null && target != null
                && source.getClassName().equals(target.getClassName())
                && (source.getPackageName().equals(target.getPackageName())
                || mHostContext.getPackageName().equals(target.getPackageName()))) {
            return true;
        }
        return false;
    }

    /**
     * @author johnsonlee
     */
    private class PluginPackageManager extends PackageManager {

        private PackageManager mHostPackageManager = mHostContext.getPackageManager();

        @Override
        public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {

            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return plugin.mPackageInfo;
            }

            return this.mHostPackageManager.getPackageInfo(packageName, flags);
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public PackageInfo getPackageInfo(VersionedPackage versionedPackage, int i) throws NameNotFoundException {

            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(versionedPackage.getPackageName());
            if (null != plugin) {
                return plugin.mPackageInfo;
            }

            return this.mHostPackageManager.getPackageInfo(versionedPackage, i);
        }
    
        @Override
        public String[] currentToCanonicalPackageNames(String[] names) {
            return this.mHostPackageManager.currentToCanonicalPackageNames(names);
        }

        @Override
        public String[] canonicalToCurrentPackageNames(String[] names) {
            return this.mHostPackageManager.canonicalToCurrentPackageNames(names);
        }

        @Override
        public Intent getLaunchIntentForPackage(String packageName) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return plugin.getLaunchIntent();
            }

            return this.mHostPackageManager.getLaunchIntentForPackage(packageName);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public Intent getLeanbackLaunchIntentForPackage(String packageName) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return plugin.getLeanbackLaunchIntent();
            }

            return this.mHostPackageManager.getLeanbackLaunchIntentForPackage(packageName);
        }

        @Override
        public int[] getPackageGids(String packageName) throws NameNotFoundException {
            return this.mHostPackageManager.getPackageGids(packageName);
        }

        @Override
        public PermissionInfo getPermissionInfo(String name, int flags) throws NameNotFoundException {
            return this.mHostPackageManager.getPermissionInfo(name, flags);
        }

        @Override
        public List<PermissionInfo> queryPermissionsByGroup(String group, int flags) throws NameNotFoundException {
            return this.mHostPackageManager.queryPermissionsByGroup(group, flags);
        }

        @Override
        public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws NameNotFoundException {
            return this.mHostPackageManager.getPermissionGroupInfo(name, flags);
        }

        @Override
        public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
            return this.mHostPackageManager.getAllPermissionGroups(flags);
        }

        @Override
        public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return plugin.getApplicationInfo();
            }

            return this.mHostPackageManager.getApplicationInfo(packageName, flags);
        }

        @Override
        public ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
            if (null != plugin) {
                return plugin.mActivityInfos.get(component);
            }

            return this.mHostPackageManager.getActivityInfo(component, flags);
        }

        @Override
        public ActivityInfo getReceiverInfo(ComponentName component, int flags) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
            if (null != plugin) {
                return plugin.mReceiverInfos.get(component);
            }

            return this.mHostPackageManager.getReceiverInfo(component, flags);
        }

        @Override
        public ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
            if (null != plugin) {
                return plugin.mServiceInfos.get(component);
            }

            return this.mHostPackageManager.getServiceInfo(component, flags);
        }

        @Override
        public ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
            if (null != plugin) {
                return plugin.mProviderInfos.get(component);
            }

            return this.mHostPackageManager.getProviderInfo(component, flags);
        }

        @Override
        public List<PackageInfo> getInstalledPackages(int flags) {
            return this.mHostPackageManager.getInstalledPackages(flags);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
            return this.mHostPackageManager.getPackagesHoldingPermissions(permissions, flags);
        }

        @Override
        public int checkPermission(String permName, String pkgName) {
            return this.mHostPackageManager.checkPermission(permName, pkgName);
        }

        @Override
        public boolean addPermission(PermissionInfo info) {
            return this.mHostPackageManager.addPermission(info);
        }

        @Override
        public boolean addPermissionAsync(PermissionInfo info) {
            return this.mHostPackageManager.addPermissionAsync(info);
        }

        @Override
        public void removePermission(String name) {
            this.mHostPackageManager.removePermission(name);
        }

        @Override
        public int checkSignatures(String pkg1, String pkg2) {
            return this.mHostPackageManager.checkSignatures(pkg1, pkg2);
        }

        @Override
        public int checkSignatures(int uid1, int uid2) {
            return this.mHostPackageManager.checkSignatures(uid1, uid2);
        }

        @Override
        public String[] getPackagesForUid(int uid) {
            return this.mHostPackageManager.getPackagesForUid(uid);
        }

        @Override
        public String getNameForUid(int uid) {
            return this.mHostPackageManager.getNameForUid(uid);
        }

        @Override
        public List<ApplicationInfo> getInstalledApplications(int flags) {
            return this.mHostPackageManager.getInstalledApplications(flags);
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public boolean isInstantApp() {
            return this.mHostPackageManager.isInstantApp();
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public boolean isInstantApp(String s) {
            return this.mHostPackageManager.isInstantApp(s);
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public int getInstantAppCookieMaxBytes() {
            return this.mHostPackageManager.getInstantAppCookieMaxBytes();
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @NonNull
        @Override
        public byte[] getInstantAppCookie() {
            return this.mHostPackageManager.getInstantAppCookie();
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public void clearInstantAppCookie() {
            this.mHostPackageManager.clearInstantAppCookie();
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public void updateInstantAppCookie(@Nullable byte[] bytes) {
            this.mHostPackageManager.updateInstantAppCookie(bytes);
        }
    
        @Override
        public String[] getSystemSharedLibraryNames() {
            return this.mHostPackageManager.getSystemSharedLibraryNames();
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @NonNull
        @Override
        public List<SharedLibraryInfo> getSharedLibraries(int i) {
            return this.mHostPackageManager.getSharedLibraries(i);
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @Nullable
        @Override
        public ChangedPackages getChangedPackages(int i) {
            return this.mHostPackageManager.getChangedPackages(i);
        }
    
        @Override
        public FeatureInfo[] getSystemAvailableFeatures() {
            return this.mHostPackageManager.getSystemAvailableFeatures();
        }

        @Override
        public boolean hasSystemFeature(String name) {
            return this.mHostPackageManager.hasSystemFeature(name);
        }

        @Override
        public ResolveInfo resolveActivity(Intent intent, int flags) {
            ResolveInfo resolveInfo = mPluginManager.resolveActivity(intent, flags);
            if (null != resolveInfo) {
                return resolveInfo;
            }

            return this.mHostPackageManager.resolveActivity(intent, flags);
        }

        @Override
        public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
            ComponentName component = intent.getComponent();
            if (null == component) {
                if (intent.getSelector() != null) {
                    intent = intent.getSelector();
                    component = intent.getComponent();
                }
            }

            if (null != component) {
                LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
                if (null != plugin) {
                    ActivityInfo activityInfo = plugin.getActivityInfo(component);
                    if (activityInfo != null) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.activityInfo = activityInfo;
                        return Arrays.asList(resolveInfo);
                    }
                }
            }

            List<ResolveInfo> all = new ArrayList<ResolveInfo>();

            List<ResolveInfo> pluginResolveInfos = mPluginManager.queryIntentActivities(intent, flags);
            if (null != pluginResolveInfos && pluginResolveInfos.size() > 0) {
                all.addAll(pluginResolveInfos);
            }

            List<ResolveInfo> hostResolveInfos = this.mHostPackageManager.queryIntentActivities(intent, flags);
            if (null != hostResolveInfos && hostResolveInfos.size() > 0) {
                all.addAll(hostResolveInfos);
            }

            return all;
        }

        @Override
        public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags) {
            return this.mHostPackageManager.queryIntentActivityOptions(caller, specifics, intent, flags);
        }

        @Override
        public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
            ComponentName component = intent.getComponent();
            if (null == component) {
                if (intent.getSelector() != null) {
                    intent = intent.getSelector();
                    component = intent.getComponent();
                }
            }

            if (null != component) {
                LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
                if (null != plugin) {
                    ActivityInfo activityInfo = plugin.getReceiverInfo(component);
                    if (activityInfo != null) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.activityInfo = activityInfo;
                        return Arrays.asList(resolveInfo);
                    }
                }
            }

            List<ResolveInfo> all = new ArrayList<ResolveInfo>();

            List<ResolveInfo> pluginResolveInfos = mPluginManager.queryBroadcastReceivers(intent, flags);
            if (null != pluginResolveInfos && pluginResolveInfos.size() > 0) {
                all.addAll(pluginResolveInfos);
            }

            List<ResolveInfo> hostResolveInfos = this.mHostPackageManager.queryBroadcastReceivers(intent, flags);
            if (null != hostResolveInfos && hostResolveInfos.size() > 0) {
                all.addAll(hostResolveInfos);
            }

            return all;
        }

        @Override
        public ResolveInfo resolveService(Intent intent, int flags) {
            ResolveInfo resolveInfo = mPluginManager.resolveService(intent, flags);
            if (null != resolveInfo) {
                return resolveInfo;
            }

            return this.mHostPackageManager.resolveService(intent, flags);
        }

        @Override
        public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
            ComponentName component = intent.getComponent();
            if (null == component) {
                if (intent.getSelector() != null) {
                    intent = intent.getSelector();
                    component = intent.getComponent();
                }
            }

            if (null != component) {
                LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
                if (null != plugin) {
                    ServiceInfo serviceInfo = plugin.getServiceInfo(component);
                    if (serviceInfo != null) {
                        ResolveInfo resolveInfo = new ResolveInfo();
                        resolveInfo.serviceInfo = serviceInfo;
                        return Arrays.asList(resolveInfo);
                    }
                }
            }

            List<ResolveInfo> all = new ArrayList<ResolveInfo>();

            List<ResolveInfo> pluginResolveInfos = mPluginManager.queryIntentServices(intent, flags);
            if (null != pluginResolveInfos && pluginResolveInfos.size() > 0) {
                all.addAll(pluginResolveInfos);
            }

            List<ResolveInfo> hostResolveInfos = this.mHostPackageManager.queryIntentServices(intent, flags);
            if (null != hostResolveInfos && hostResolveInfos.size() > 0) {
                all.addAll(hostResolveInfos);
            }

            return all;
        }

        @Override
        @TargetApi(Build.VERSION_CODES.KITKAT)
        public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
            return this.mHostPackageManager.queryIntentContentProviders(intent, flags);
        }

        @Override
        public ProviderInfo resolveContentProvider(String name, int flags) {
            ProviderInfo providerInfo = mPluginManager.resolveContentProvider(name, flags);
            if (null != providerInfo) {
                return providerInfo;
            }

            return this.mHostPackageManager.resolveContentProvider(name, flags);
        }

        @Override
        public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
            return this.mHostPackageManager.queryContentProviders(processName, uid, flags);
        }

        @Override
        public InstrumentationInfo getInstrumentationInfo(ComponentName component, int flags) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
            if (null != plugin) {
                return plugin.mInstrumentationInfos.get(component);
            }

            return this.mHostPackageManager.getInstrumentationInfo(component, flags);
        }

        @Override
        public List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
            return this.mHostPackageManager.queryInstrumentation(targetPackage, flags);
        }

        @Override
        public Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return plugin.mResources.getDrawable(resid);
            }

            return this.mHostPackageManager.getDrawable(packageName, resid, appInfo);
        }

        @Override
        public Drawable getActivityIcon(ComponentName component) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
            if (null != plugin) {
                return plugin.mResources.getDrawable(plugin.mActivityInfos.get(component).icon);
            }

            return this.mHostPackageManager.getActivityIcon(component);
        }

        @Override
        public Drawable getActivityIcon(Intent intent) throws NameNotFoundException {
            ResolveInfo ri = mPluginManager.resolveActivity(intent);
            if (null != ri) {
                LoadedPlugin plugin = mPluginManager.getLoadedPlugin(ri.resolvePackageName);
                return plugin.mResources.getDrawable(ri.activityInfo.icon);
            }

            return this.mHostPackageManager.getActivityIcon(intent);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
        public Drawable getActivityBanner(ComponentName component) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
            if (null != plugin) {
                return plugin.mResources.getDrawable(plugin.mActivityInfos.get(component).banner);
            }

            return this.mHostPackageManager.getActivityBanner(component);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
        public Drawable getActivityBanner(Intent intent) throws NameNotFoundException {
            ResolveInfo ri = mPluginManager.resolveActivity(intent);
            if (null != ri) {
                LoadedPlugin plugin = mPluginManager.getLoadedPlugin(ri.resolvePackageName);
                return plugin.mResources.getDrawable(ri.activityInfo.banner);
            }

            return this.mHostPackageManager.getActivityBanner(intent);
        }

        @Override
        public Drawable getDefaultActivityIcon() {
            return this.mHostPackageManager.getDefaultActivityIcon();
        }

        @Override
        public Drawable getApplicationIcon(ApplicationInfo info) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(info.packageName);
            if (null != plugin) {
                return plugin.mResources.getDrawable(info.icon);
            }

            return this.mHostPackageManager.getApplicationIcon(info);
        }

        @Override
        public Drawable getApplicationIcon(String packageName) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return plugin.mResources.getDrawable(plugin.mPackage.applicationInfo.icon);
            }

            return this.mHostPackageManager.getApplicationIcon(packageName);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
        public Drawable getApplicationBanner(ApplicationInfo info) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(info.packageName);
            if (null != plugin) {
                return plugin.mResources.getDrawable(info.banner);
            }

            return this.mHostPackageManager.getApplicationBanner(info);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
        public Drawable getApplicationBanner(String packageName) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return plugin.mResources.getDrawable(plugin.mPackage.applicationInfo.banner);
            }

            return this.mHostPackageManager.getApplicationBanner(packageName);
        }

        @Override
        public Drawable getActivityLogo(ComponentName component) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
            if (null != plugin) {
                return plugin.mResources.getDrawable(plugin.mActivityInfos.get(component).logo);
            }

            return this.mHostPackageManager.getActivityLogo(component);
        }

        @Override
        public Drawable getActivityLogo(Intent intent) throws NameNotFoundException {
            ResolveInfo ri = mPluginManager.resolveActivity(intent);
            if (null != ri) {
                LoadedPlugin plugin = mPluginManager.getLoadedPlugin(ri.resolvePackageName);
                return plugin.mResources.getDrawable(ri.activityInfo.logo);
            }

            return this.mHostPackageManager.getActivityLogo(intent);
        }

        @Override
        public Drawable getApplicationLogo(ApplicationInfo info) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(info.packageName);
            if (null != plugin) {
                return plugin.mResources.getDrawable(0 != info.logo ? info.logo : android.R.drawable.sym_def_app_icon);
            }

            return this.mHostPackageManager.getApplicationLogo(info);
        }

        @Override
        public Drawable getApplicationLogo(String packageName) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return plugin.mResources.getDrawable(0 != plugin.mPackage.applicationInfo.logo ? plugin.mPackage.applicationInfo.logo : android.R.drawable.sym_def_app_icon);
            }

            return this.mHostPackageManager.getApplicationLogo(packageName);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
            return this.mHostPackageManager.getUserBadgedIcon(icon, user);
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public Drawable getUserBadgeForDensity(UserHandle user, int density) {
            try {
                Method method = PackageManager.class.getMethod("getUserBadgeForDensity", UserHandle.class, int.class);
                return (Drawable) method.invoke(this.mHostPackageManager, user, density);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity) {
            return this.mHostPackageManager.getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
            return this.mHostPackageManager.getUserBadgedLabel(label, user);
        }

        @Override
        public CharSequence getText(String packageName, int resid, ApplicationInfo appInfo) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return plugin.mResources.getText(resid);
            }

            return this.mHostPackageManager.getText(packageName, resid, appInfo);
        }

        @Override
        public XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return plugin.mResources.getXml(resid);
            }

            return this.mHostPackageManager.getXml(packageName, resid, appInfo);
        }

        @Override
        public CharSequence getApplicationLabel(ApplicationInfo info) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(info.packageName);
            if (null != plugin) {
                try {
                    return plugin.mResources.getText(info.labelRes);
                } catch (Resources.NotFoundException e) {
                    // ignored.
                }
            }

            return this.mHostPackageManager.getApplicationLabel(info);
        }

        @Override
        public Resources getResourcesForActivity(ComponentName component) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(component);
            if (null != plugin) {
                return plugin.mResources;
            }

            return this.mHostPackageManager.getResourcesForActivity(component);
        }

        @Override
        public Resources getResourcesForApplication(ApplicationInfo app) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(app.packageName);
            if (null != plugin) {
                return plugin.mResources;
            }

            return this.mHostPackageManager.getResourcesForApplication(app);
        }

        @Override
        public Resources getResourcesForApplication(String appPackageName) throws NameNotFoundException {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(appPackageName);
            if (null != plugin) {
                return plugin.mResources;
            }

            return this.mHostPackageManager.getResourcesForApplication(appPackageName);
        }

        @Override
        public void verifyPendingInstall(int id, int verificationCode) {
            this.mHostPackageManager.verifyPendingInstall(id, verificationCode);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
            this.mHostPackageManager.extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay);
        }

        @Override
        public void setInstallerPackageName(String targetPackage, String installerPackageName) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(targetPackage);
            if (null != plugin) {
                return;
            }

            this.mHostPackageManager.setInstallerPackageName(targetPackage, installerPackageName);
        }

        @Override
        public String getInstallerPackageName(String packageName) {
            LoadedPlugin plugin = mPluginManager.getLoadedPlugin(packageName);
            if (null != plugin) {
                return mHostContext.getPackageName();
            }

            return this.mHostPackageManager.getInstallerPackageName(packageName);
        }

        @Override
        public void addPackageToPreferred(String packageName) {
            this.mHostPackageManager.addPackageToPreferred(packageName);
        }

        @Override
        public void removePackageFromPreferred(String packageName) {
            this.mHostPackageManager.removePackageFromPreferred(packageName);
        }

        @Override
        public List<PackageInfo> getPreferredPackages(int flags) {
            return this.mHostPackageManager.getPreferredPackages(flags);
        }

        @Override
        public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity) {
            this.mHostPackageManager.addPreferredActivity(filter, match, set, activity);
        }

        @Override
        public void clearPackagePreferredActivities(String packageName) {
            this.mHostPackageManager.clearPackagePreferredActivities(packageName);
        }

        @Override
        public int getPreferredActivities(List<IntentFilter> outFilters, List<ComponentName> outActivities, String packageName) {
            return this.mHostPackageManager.getPreferredActivities(outFilters, outActivities, packageName);
        }

        @Override
        public void setComponentEnabledSetting(ComponentName component, int newState, int flags) {
            this.mHostPackageManager.setComponentEnabledSetting(component, newState, flags);
        }

        @Override
        public int getComponentEnabledSetting(ComponentName component) {
            return this.mHostPackageManager.getComponentEnabledSetting(component);
        }

        @Override
        public void setApplicationEnabledSetting(String packageName, int newState, int flags) {
            this.mHostPackageManager.setApplicationEnabledSetting(packageName, newState, flags);
        }

        @Override
        public int getApplicationEnabledSetting(String packageName) {
            return this.mHostPackageManager.getApplicationEnabledSetting(packageName);
        }

        @Override
        public boolean isSafeMode() {
            return this.mHostPackageManager.isSafeMode();
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public void setApplicationCategoryHint(@NonNull String s, int i) {
            this.mHostPackageManager.setApplicationCategoryHint(s, i);
        }
    
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public PackageInstaller getPackageInstaller() {
            return this.mHostPackageManager.getPackageInstaller();
        }
    
        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public boolean canRequestPackageInstalls() {
            return this.mHostPackageManager.canRequestPackageInstalls();
        }
    
        @TargetApi(24)
        public int[] getPackageGids(String s, int i) throws NameNotFoundException {
            return mHostPackageManager.getPackageGids(s);
        }

        public int getPackageUid(String s, int i) throws NameNotFoundException {
            Object uid = ReflectUtil.invokeNoException(PackageManager.class, mHostPackageManager, "getPackageUid",
                    new Class[]{String.class, int.class}, s, i);
            if (uid != null) {
                return (int) uid;
            } else {
                throw new NameNotFoundException(s);
            }
        }

        @TargetApi(23)
        public boolean isPermissionRevokedByPolicy(String s, String s1) {
            return false;
        }

        @TargetApi(24)
        public boolean hasSystemFeature(String s, int i) {
            return mHostPackageManager.hasSystemFeature(s);
        }
    }

}
