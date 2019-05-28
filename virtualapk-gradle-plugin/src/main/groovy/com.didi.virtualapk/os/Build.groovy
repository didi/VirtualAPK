package com.didi.virtualapk.os

import static com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION

final class Build {

    /**
     * Android Gradle Plugin 版本，3 位数，最后一位永远是 0，比如 3.0.0 对应 300，3.2.1 对应 320
     */
    public static final int AGP_VERSION = getAgpVersion()

    public static final Boolean V3_1_OR_LATER = AGP_VERSION >= VERSION_CODE.V3_1_X
    public static final Boolean V3_2_OR_LATER = AGP_VERSION >= VERSION_CODE.V3_2_X
    public static final Boolean V3_3_OR_LATER = AGP_VERSION >= VERSION_CODE.V3_3_X

    /**
     * 以 int 形式返回 android gradle plugin 版本，比如 300,310,320
     * @return
     */
    private static int getAgpVersion() {
        String[] versionArr = ANDROID_GRADLE_PLUGIN_VERSION.split("\\.")
        return Integer.valueOf(versionArr[0]) * 100 + Integer.valueOf(versionArr[1]) * 10
    }

    static interface VERSION_CODE {
        int NONE = 0
        int V3_1_X = 310
        int V3_2_X = 320
        int V3_3_X = 330
        int V3_4_X = 340
        int V3_5_X = 350
    }

    static boolean isSupportVersion(int minVersion) {
        return AGP_VERSION >= minVersion
    }

}
