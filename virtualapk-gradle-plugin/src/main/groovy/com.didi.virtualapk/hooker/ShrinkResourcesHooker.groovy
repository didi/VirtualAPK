package com.didi.virtualapk.hooker

import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ShrinkResourcesTransform
import com.didi.virtualapk.transform.TransformWrapper
import com.didi.virtualapk.utils.Reflect
import org.gradle.api.Project

class ShrinkResourcesHooker extends GradleTaskHooker<TransformTask> {
    
    ShrinkResourcesHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }
    
    @Override
    String getTaskName() {
        return "shrinkRes"
    }
    
    @Override
    void beforeTaskExecute(TransformTask task) {
        def shrinkResourcesTransform = task.transform as ShrinkResourcesTransform
        Reflect.on(task).set('transform', new TransformWrapper(shrinkResourcesTransform) {
            @Override
            void transform(TransformInvocation invocation) throws TransformException, InterruptedException, IOException {
                println "sourceDir: ${Reflect.on(origin).get('sourceDir')}"
                super.transform(invocation)
            }
        })
    }

    @Override
    void afterTaskExecute(TransformTask task) {

    }
}