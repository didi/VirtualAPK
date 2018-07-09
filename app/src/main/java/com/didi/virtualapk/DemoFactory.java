package com.didi.virtualapk;

import android.content.Context;
import android.support.annotation.Keep;
import android.util.Log;

/**
 * Created by qiaopu on 2018/7/5.
 */
@Keep
public class DemoFactory {
    public static PluginManager create(Context context) {
        return new PluginManager(context) {
            @Override
            public void init() {
                super.init();
                Log.e(TAG, "example");
            }
        };
    }
}
