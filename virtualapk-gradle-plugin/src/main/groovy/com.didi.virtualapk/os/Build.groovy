package com.didi.virtualapk.os

import org.gradle.api.Project

final class Build {

    public static final String GRADLE_VERSION_SDK_INT = 'va.gradle.version.sdk_int'

    static interface VERSION_CODE {
        int NONE = 0
        int V3_1_X = 310
        int V3_2_X = 320
    }

    static void initGradleVersion(Project project) {
        if (findClass("com.android.build.gradle.internal.scope.InternalArtifactType")) {
            project.ext.set(GRADLE_VERSION_SDK_INT, VERSION_CODE.V3_2_X)
            return
        }

        if (!findClass("com.android.builder.core.VariantConfiguration")) {
            project.ext.set(GRADLE_VERSION_SDK_INT, VERSION_CODE.V3_1_X)
            return
        }

        project.ext.set(GRADLE_VERSION_SDK_INT, VERSION_CODE.NONE)
    }


    private static findClass(String className) {
        try {
            Class.forName(className)
            return true
        } catch (Throwable ignored) {
            return false
        }
    }


    static boolean isSupportVersion(Project project, int minVersion) {
        return project.extensions.extraProperties.get(GRADLE_VERSION_SDK_INT) >= minVersion
    }

}
