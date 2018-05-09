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

package com.didi.virtualapk.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.internal.Constants;
import com.didi.virtualapk.internal.LoadedPlugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by renyugang on 16/8/15.
 */
public class PluginUtil {

    public static String getTargetActivity(Intent intent) {
        return intent.getStringExtra(Constants.KEY_TARGET_ACTIVITY);
    }

    public static ComponentName getComponent(Intent intent) {
        return new ComponentName(intent.getStringExtra(Constants.KEY_TARGET_PACKAGE),
                intent.getStringExtra(Constants.KEY_TARGET_ACTIVITY));
    }

    public static boolean isIntentFromPlugin(Intent intent) {
        return intent.getBooleanExtra(Constants.KEY_IS_PLUGIN, false);
    }

    public static int getTheme(Context context, Intent intent) {
        return PluginUtil.getTheme(context, PluginUtil.getComponent(intent));
    }

    public static int getTheme(Context context, ComponentName component) {
        LoadedPlugin loadedPlugin = PluginManager.getInstance(context).getLoadedPlugin(component);

        if (null == loadedPlugin) {
            return 0;
        }

        ActivityInfo info = loadedPlugin.getActivityInfo(component);
        if (null == info) {
            return 0;
        }

        if (0 != info.theme) {
            return info.theme;
        }

        ApplicationInfo appInfo = info.applicationInfo;
        if (null != appInfo && appInfo.theme != 0) {
            return appInfo.theme;
        }

        return PluginUtil.selectDefaultTheme(0, Build.VERSION.SDK_INT);
    }

    public static int selectDefaultTheme(final int curTheme, final int targetSdkVersion) {
        return selectSystemTheme(curTheme, targetSdkVersion,
                android.R.style.Theme,
                android.R.style.Theme_Holo,
                android.R.style.Theme_DeviceDefault,
                android.R.style.Theme_DeviceDefault_Light_DarkActionBar);
    }

    private static int selectSystemTheme(final int curTheme, final int targetSdkVersion, final int orig, final int holo, final int dark, final int deviceDefault) {
        if (curTheme != 0) {
            return curTheme;
        }

        if (targetSdkVersion < 11 /* Build.VERSION_CODES.HONEYCOMB */) {
            return orig;
        }

        if (targetSdkVersion < 14 /* Build.VERSION_CODES.ICE_CREAM_SANDWICH */) {
            return holo;
        }

        if (targetSdkVersion < 24 /* Build.VERSION_CODES.N */) {
            return dark;
        }

        return deviceDefault;
    }

    public static void hookActivityResources(Activity activity, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isVivo(activity.getResources())) {
            // for 5.0+ vivo
            return;
        }

        // designed for 5.0 - only, but some bad phones not work, eg:letv
        try {
            Context base = activity.getBaseContext();
            final LoadedPlugin plugin = PluginManager.getInstance(activity).getLoadedPlugin(packageName);
            final Resources resources = plugin.getResources();
            if (resources != null) {
                ReflectUtil.setField(base.getClass(), base, "mResources", resources);

                // copy theme
                Resources.Theme theme = resources.newTheme();
                theme.setTo(activity.getTheme());
                int themeResource = (int)ReflectUtil.getField(ContextThemeWrapper.class, activity, "mThemeResource");
                theme.applyStyle(themeResource, true);
                ReflectUtil.setField(ContextThemeWrapper.class, activity, "mTheme", theme);

                ReflectUtil.setField(ContextThemeWrapper.class, activity, "mResources", resources);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static final boolean isLocalService(final ServiceInfo serviceInfo) {
        return TextUtils.isEmpty(serviceInfo.processName) || serviceInfo.applicationInfo.packageName.equals(serviceInfo.processName);
    }

    public static boolean isVivo(Resources resources) {
        return resources.getClass().getName().equals("android.content.res.VivoResources");
    }

    public static void putBinder(Bundle bundle, String key, IBinder value) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bundle.putBinder(key, value);
        } else {
            try {
                ReflectUtil.invoke(Bundle.class, bundle, "putIBinder", new Class[]{String.class, IBinder.class}, key, value);
            } catch (Exception e) {
            }
        }
    }

    public static IBinder getBinder(Bundle bundle, String key) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return bundle.getBinder(key);
        } else {
            try {
                return (IBinder) ReflectUtil.invoke(Bundle.class, bundle, "getIBinder", key);
            } catch (Exception e) {
            }

            return null;
        }
    }
    
    public static void copyNativeLib(File apk, Context context, PackageInfo packageInfo, File nativeLibDir) throws Exception {
        long startTime = System.currentTimeMillis();
        ZipFile zipfile = new ZipFile(apk.getAbsolutePath());
    
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (String cpuArch : Build.SUPPORTED_ABIS) {
                    if (findAndCopyNativeLib(zipfile, context, cpuArch, packageInfo, nativeLibDir)) {
                        return;
                    }
                }
                
            } else {
                if (findAndCopyNativeLib(zipfile, context, Build.CPU_ABI, packageInfo, nativeLibDir)) {
                    return;
                }
            }
            
            findAndCopyNativeLib(zipfile, context, "armeabi", packageInfo, nativeLibDir);
    
        } finally {
            zipfile.close();
            Log.d("NativeLib", "Done! +" + (System.currentTimeMillis() - startTime) + "ms");
        }
    }
    
    private static boolean findAndCopyNativeLib(ZipFile zipfile, Context context, String cpuArch, PackageInfo packageInfo, File nativeLibDir) throws Exception {
        Log.d("NativeLib", "Try to copy plugin's cup arch: " + cpuArch);
        boolean findLib = false;
        boolean findSo = false;
        byte buffer[] = null;
        String libPrefix = "lib/" + cpuArch + "/";
        ZipEntry entry;
        Enumeration e = zipfile.entries();
        
        while (e.hasMoreElements()) {
            entry = (ZipEntry) e.nextElement();
            String entryName = entry.getName();
            
            if (entryName.charAt(0) < 'l') {
                continue;
            }
            if (entryName.charAt(0) > 'l') {
                break;
            }
            if (!findLib && !entryName.startsWith("lib/")) {
                continue;
            }
            findLib = true;
            if (!entryName.endsWith(".so") || !entryName.startsWith(libPrefix)) {
                continue;
            }
    
            if (buffer == null) {
                findSo = true;
                Log.d("NativeLib", "Found plugin's cup arch dir: " + cpuArch);
                buffer = new byte[8192];
            }
            
            String libName = entryName.substring(entryName.lastIndexOf('/') + 1);
            Log.d("NativeLib", "verify so " + libName);
            File libFile = new File(nativeLibDir, libName);
            String key = packageInfo.packageName + "_" + libName;
            if (libFile.exists()) {
                int VersionCode = Settings.getSoVersion(context, key);
                if (VersionCode == packageInfo.versionCode) {
                    Log.d("NativeLib", "skip existing so : " + entry.getName());
                    continue;
                }
            }
            FileOutputStream fos = new FileOutputStream(libFile);
            Log.d("NativeLib", "copy so " + entry.getName() + " of " + cpuArch);
            copySo(buffer, zipfile.getInputStream(entry), fos);
            Settings.setSoVersion(context, key, packageInfo.versionCode);
        }
        
        if (!findLib) {
            Log.d("NativeLib", "Fast skip all!");
            return true;
        }
        
        return findSo;
    }
    
    private static void copySo(byte[] buffer, InputStream input, OutputStream output) throws IOException {
        BufferedInputStream bufferedInput = new BufferedInputStream(input);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(output);
        int count;
        
        while ((count = bufferedInput.read(buffer)) > 0) {
            bufferedOutput.write(buffer, 0, count);
        }
        bufferedOutput.flush();
        bufferedOutput.close();
        output.close();
        bufferedInput.close();
        input.close();
    }

}
