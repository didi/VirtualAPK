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
import android.content.ContentResolverWrapper;
import android.content.Context;
import android.content.IContentProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Keep;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.delegate.RemoteContentProvider;

/**
 * Created by renyugang on 16/12/7.
 */

public class PluginContentResolver extends ContentResolverWrapper {
    private PluginManager mPluginManager;

    public PluginContentResolver(Context context) {
        super(context);
        mPluginManager = PluginManager.getInstance(context);
    }
    
    @Override
    protected IContentProvider acquireProvider(Context context, String auth) {
        if (mPluginManager.resolveContentProvider(auth, 0) != null) {
            return mPluginManager.getIContentProvider();
        }
        return super.acquireProvider(context, auth);
    }

    @Override
    protected IContentProvider acquireExistingProvider(Context context, String auth) {
        if (mPluginManager.resolveContentProvider(auth, 0) != null) {
            return mPluginManager.getIContentProvider();
        }
        return super.acquireExistingProvider(context, auth);
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected IContentProvider acquireUnstableProvider(Context context, String auth) {
        if (mPluginManager.resolveContentProvider(auth, 0) != null) {
            return mPluginManager.getIContentProvider();
        }
        return super.acquireUnstableProvider(context, auth);
    }

    @Override
    public boolean releaseProvider(IContentProvider provider) {
        return true;
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean releaseUnstableProvider(IContentProvider icp) {
        return true;
    }
    
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void unstableProviderDied(IContentProvider icp) {
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public void appNotRespondingViaProvider(IContentProvider icp) {
    }

    protected int resolveUserIdFromAuthority(String auth) {
        return 0;
    }

    @Keep
    public static Uri wrapperUri(LoadedPlugin loadedPlugin, Uri pluginUri) {
        String pkg = loadedPlugin.getPackageName();
        String pluginUriString = Uri.encode(pluginUri.toString());
        StringBuilder builder = new StringBuilder(RemoteContentProvider.getUri(loadedPlugin.getHostContext()));
        builder.append("/?plugin=" + loadedPlugin.getLocation());
        builder.append("&pkg=" + pkg);
        builder.append("&uri=" + pluginUriString);
        Uri wrapperUri = Uri.parse(builder.toString());
        return wrapperUri;
    }

    @Deprecated
    public static String getAuthority(Context context) {
        return RemoteContentProvider.getAuthority(context);
    }

    @Deprecated
    public static String getUri(Context context) {
        return RemoteContentProvider.getUri(context);
    }

    @Keep
    public static Bundle getBundleForCall(Uri uri) {
        Bundle bundle = new Bundle();
        bundle.putString(RemoteContentProvider.KEY_WRAPPER_URI, uri.toString());
        return bundle;
    }

}
