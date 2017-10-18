package com.didi.virtualapk.hooker

import com.android.build.gradle.AndroidConfig
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.packaging.ParsedPackagingOptions
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.MergeJavaResourcesTransform
import com.didi.virtualapk.collector.HostJniLibsCollector
import org.gradle.api.Project

import java.lang.reflect.Field

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
    String getTaskName() {
        return "mergeJniLibs"
    }

    /**
     * Prevent .so files from packaging into apk via the PackagingOptions exclude configuration
     * @param task Gradle task of mergeJniLibs
     */
    @Override
    void beforeTaskExecute(TransformTask task) {

        def excludeJniFiles = jniLibsCollector.collect(virtualApk.stripDependencies)

        excludeJniFiles.each {
            androidConfig.packagingOptions.exclude("/${it}")
        }

        Field field = MergeJavaResourcesTransform.class.getDeclaredField('packagingOptions')
        field.setAccessible(true)
        field.set(task.transform, new ParsedPackagingOptions(androidConfig.packagingOptions))
    }

    @Override
    void afterTaskExecute(TransformTask task) {}
}