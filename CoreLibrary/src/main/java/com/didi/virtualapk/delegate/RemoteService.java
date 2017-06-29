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

import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;

import com.didi.virtualapk.PluginManager;
import com.didi.virtualapk.internal.LoadedPlugin;

import java.io.File;

/**
 * @author johnsonlee
 */
public class RemoteService extends LocalService {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        Intent target = intent.getParcelableExtra(EXTRA_TARGET);
        if (target != null) {
            String pluginLocation = intent.getStringExtra(EXTRA_PLUGIN_LOCATION);
            ComponentName component = target.getComponent();
            LoadedPlugin plugin = PluginManager.getInstance(this).getLoadedPlugin(component);
            if (plugin == null && pluginLocation != null) {
                try {
                    PluginManager.getInstance(this).loadPlugin(new File(pluginLocation));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

}
