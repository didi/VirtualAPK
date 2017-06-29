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

import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.internal.LoadedPlugin;
import com.didi.virtualapk.internal.PluginContentResolver;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import static com.didi.virtualapk.delegate.RemoteContentProvider.KEY_WRAPPER_URI;

/**
 * Created by renyugang on 16/12/8.
 */

public class IContentProviderProxy implements InvocationHandler {
    private static final String TAG = "IContentProviderProxy";

    private IContentProvider mBase;
    private Context mContext;

    private IContentProviderProxy(Context context, IContentProvider iContentProvider) {
        mBase = iContentProvider;
        mContext = context;
    }

    public static IContentProvider newInstance(Context context, IContentProvider iContentProvider) {
        return (IContentProvider) Proxy.newProxyInstance(iContentProvider.getClass().getClassLoader(),
                new Class[] { IContentProvider.class }, new IContentProviderProxy(context, iContentProvider));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.v(TAG, method.toGenericString() + " : " + Arrays.toString(args));
        wrapperUri(method, args);

        try {
            return method.invoke(mBase, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private void wrapperUri(Method method, Object[] args) {
        Uri uri = null;
        int index = 0;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Uri) {
                    uri = (Uri) args[i];
                    index = i;
                    break;
                }
            }
        }

        Bundle bundleInCallMethod = null;
        if (method.getName().equals("call")) {
            bundleInCallMethod = getBundleParameter(args);
            if (bundleInCallMethod != null) {
                String uriString = bundleInCallMethod.getString(KEY_WRAPPER_URI);
                if (uriString != null) {
                    uri = Uri.parse(uriString);
                }
            }
        }

        if (uri == null) {
            return;
        }

        PluginManager pluginManager = PluginManager.getInstance(mContext);
        ProviderInfo info = pluginManager.resolveContentProvider(uri.getAuthority(), 0);
        if (info != null) {
            String pkg = info.packageName;
            LoadedPlugin plugin = pluginManager.getLoadedPlugin(pkg);
            String pluginUri = Uri.encode(uri.toString());
            StringBuilder builder = new StringBuilder(PluginContentResolver.getUri(mContext));
            builder.append("/?plugin=" + plugin.getLocation());
            builder.append("&pkg=" + pkg);
            builder.append("&uri=" + pluginUri);
            Uri wrapperUri = Uri.parse(builder.toString());
            if (method.getName().equals("call")) {
                bundleInCallMethod.putString(KEY_WRAPPER_URI, wrapperUri.toString());
            } else {
                args[index] = wrapperUri;
            }
        }
    }

    private Bundle getBundleParameter(Object[] args) {
        Bundle bundle = null;
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Bundle) {
                    bundle = (Bundle) args[i];
                    break;
                }
            }
        }

        return bundle;
    }

}
