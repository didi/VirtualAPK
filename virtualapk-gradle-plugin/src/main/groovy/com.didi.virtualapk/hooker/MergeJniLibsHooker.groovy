package com.didi.virtualapk.hooker

import com.android.build.gradle.AndroidConfig
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.didi.virtualapk.collector.HostJniLibsCollector
import com.didi.virtualapk.utils.Log
import org.gradle.api.Project

/**
 * Remove the Native libs(.so) in stripped dependencies before mergeJniLibs task
 *
 * @author zhengtao
 */
class MergeJniLibsHooker extends GradleTaskHooker<TransformTask> {

    HostJniLibsCollector jniLibsCollector
    AndroidConfig androidConfig

    public MergeJniLibsHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
        jniLibsCollector = new HostJniLibsCollector()
        androidConfig = project.extensions.findByType(AppExtension)
    }

    @Override
    String getTransformName() {
        return "mergeJniLibs"
    }

    /**
     * Prevent .so files from packaging into apk via the PackagingOptions exclude configuration
     * @param task Gradle task of mergeJniLibs
     */
    @Override
    void beforeTaskExecute(TransformTask task) {

        def excludeJniFiles = jniLibsCollector.collect(vaContext.stripDependencies)

        excludeJniFiles.each {
            androidConfig.packagingOptions.exclude("/${it}")
            Log.i 'MergeJniLibsHooker', "Stripped jni file: ${it}"
        }

        mark()
//        Reflect.on(task.transform)
//                .set('packagingOptions', new ParsedPackagingOptions(androidConfig.packagingOptions))
    }

    @Override
    void afterTaskExecute(TransformTask task) {}
}