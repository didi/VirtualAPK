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

import android.app.ActivityThread;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.utils.Reflector;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

/**
 * Created by renyugang on 16/8/9.
 */
class ResourcesManager {

    public static synchronized Resources createResources(Context hostContext, String apk) {
        Resources hostResources = hostContext.getResources();
        Resources newResources = null;
        AssetManager assetManager;
        try {
            Reflector reflector = Reflector.on(AssetManager.class).method("addAssetPath", String.class);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                assetManager = AssetManager.class.newInstance();
                reflector.bind(assetManager);
                reflector.call(hostContext.getApplicationInfo().sourceDir);
            } else {
                assetManager = hostResources.getAssets();
                reflector.bind(assetManager);
            }
            reflector.call(apk);
            List<LoadedPlugin> pluginList = PluginManager.getInstance(hostContext).getAllLoadedPlugins();
            for (LoadedPlugin plugin : pluginList) {
                reflector.call(plugin.getLocation());
            }
            if (isMiUi(hostResources)) {
                newResources = MiUiResourcesCompat.createResources(hostResources, assetManager);
            } else if (isVivo(hostResources)) {
                newResources = VivoResourcesCompat.createResources(hostContext, hostResources, assetManager);
            } else if (isNubia(hostResources)) {
                newResources = NubiaResourcesCompat.createResources(hostResources, assetManager);
            } else if (isNotRawResources(hostResources)) {
                newResources = AdaptationResourcesCompat.createResources(hostResources, assetManager);
            } else {
                // is raw android resources
                newResources = new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
            }
            // lastly, sync all LoadedPlugin to newResources
            for (LoadedPlugin plugin : pluginList) {
                plugin.updateResources(newResources);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newResources;

    }

    public static void hookResources(Context base, Resources resources) {
        try {
            Reflector reflector = Reflector.with(base);
            reflector.field("mResources").set(resources);
            Object loadedApk = reflector.field("mPackageInfo").get();
            Reflector.with(loadedApk).field("mResources").set(resources);

            Object activityThread = ActivityThread.currentActivityThread();
            Object resManager;
            if (Build.VERSION.SDK_INT >= 19) {
                resManager = android.app.ResourcesManager.getInstance();
            } else {
                resManager = Reflector.with(activityThread).field("mResourcesManager").get();
            }
            if (Build.VERSION.SDK_INT < 24) {
                Map<Object, WeakReference<Resources>> map = Reflector.with(resManager).field("mActiveResources").get();
                Object key = map.keySet().iterator().next();
                map.put(key, new WeakReference<>(resources));
            } else {
                // still hook Android N Resources, even though it's unnecessary, then nobody will be strange.
                Map map = Reflector.QuietReflector.with(resManager).field("mResourceImpls").get();
                Object key = map.keySet().iterator().next();
                Object resourcesImpl = Reflector.QuietReflector.with(resources).field("mResourcesImpl").get();
                map.put(key, new WeakReference<>(resourcesImpl));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isMiUi(Resources resources) {
        return resources.getClass().getName().equals("android.content.res.MiuiResources");
    }

    private static boolean isVivo(Resources resources) {
        return resources.getClass().getName().equals("android.content.res.VivoResources");
    }

    private static boolean isNubia(Resources resources) {
        return resources.getClass().getName().equals("android.content.res.NubiaResources");
    }

    private static boolean isNotRawResources(Resources resources) {
        return !resources.getClass().getName().equals("android.content.res.Resources");
    }

    private static final class MiUiResourcesCompat {
        private static Resources createResources(Resources hostResources, AssetManager assetManager) throws Exception {
            Reflector reflector = Reflector.on("android.content.res.MiuiResources");
            Resources newResources = reflector.constructor(AssetManager.class, DisplayMetrics.class, Configuration.class)
                .newInstance(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
            return newResources;
        }
    }

    private static final class VivoResourcesCompat {
        private static Resources createResources(Context hostContext, Resources hostResources, AssetManager assetManager) throws Exception {
            Reflector reflector = Reflector.on("android.content.res.VivoResources");
            Resources newResources = reflector.constructor(AssetManager.class, DisplayMetrics.class, Configuration.class)
                .newInstance(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
            reflector.method("init", String.class).callByCaller(newResources, hostContext.getPackageName());
            reflector.field("mThemeValues");
            reflector.set(newResources, reflector.get(hostResources));
            return newResources;
        }
    }

    private static final class NubiaResourcesCompat {
        private static Resources createResources(Resources hostResources, AssetManager assetManager) throws Exception {
            Reflector reflector = Reflector.on("android.content.res.NubiaResources");
            Resources newResources = reflector.constructor(AssetManager.class, DisplayMetrics.class, Configuration.class)
                .newInstance(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
            return newResources;
        }
    }

    private static final class AdaptationResourcesCompat {
        private static Resources createResources(Resources hostResources, AssetManager assetManager) throws Exception {
            Resources newResources;
            try {
                Reflector reflector = Reflector.with(hostResources);
                newResources = reflector.constructor(AssetManager.class, DisplayMetrics.class, Configuration.class)
                    .newInstance(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
            } catch (Exception e) {
                newResources = new Resources(assetManager, hostResources.getDisplayMetrics(), hostResources.getConfiguration());
            }

            return newResources;
        }
    }

}
