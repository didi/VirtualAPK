package com.didi.virtualapk.hooker

import com.android.build.api.transform.*
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.transforms.ShrinkResourcesTransform
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
        Reflect.on(task).set('transform', new TransformWrapper(shrinkResourcesTransform))
    }

    @Override
    void afterTaskExecute(TransformTask task) {

    }

    static class TransformWrapper extends Transform {

        Transform origin

        TransformWrapper(Transform transform) {
            origin = transform
        }

        @Override
        String getName() {
            return origin.getName()
        }

        @Override
        Set<QualifiedContent.ContentType> getInputTypes() {
            return origin.getInputTypes()
        }

        @Override
        Set<QualifiedContent.ContentType> getOutputTypes() {
            return origin.getOutputTypes()
        }

        @Override
        Set<? super QualifiedContent.Scope> getScopes() {
            return origin.getScopes()
        }

        @Override
        Set<? super QualifiedContent.Scope> getReferencedScopes() {
            return origin.getReferencedScopes()
        }

        @Override
        Collection<File> getSecondaryFileInputs() {
            return origin.getSecondaryFileInputs()
        }

        @Override
        Collection<SecondaryFile> getSecondaryFiles() {
            return origin.getSecondaryFiles()
        }

        @Override
        Collection<File> getSecondaryFileOutputs() {
            return origin.getSecondaryFileOutputs()
        }

        @Override
        Collection<File> getSecondaryDirectoryOutputs() {
            return origin.getSecondaryDirectoryOutputs()
        }

        @Override
        Map<String, Object> getParameterInputs() {
            return origin.getParameterInputs()
        }

        @Override
        boolean isIncremental() {
            return origin.isIncremental()
        }

        @Override
        void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
            origin.transform(context, inputs, referencedInputs, outputProvider, isIncremental)
        }

        @Override
        void transform(TransformInvocation invocation) throws TransformException, InterruptedException, IOException {
            println "sourceDir: ${Reflect.on(origin).get('sourceDir')}"

            Collection<TransformInput> referencedInputs = invocation.getReferencedInputs();
            for (TransformInput transformInput : referencedInputs) {
                for (DirectoryInput directoryInput : transformInput.getDirectoryInputs()) {
                    println "classes dir: ${directoryInput.getFile()}"
                }
                for (JarInput jarInput : transformInput.getJarInputs()) {
                    println "classes jar: ${jarInput.getFile()}"
                }
            }

            origin.transform(invocation)
        }

        @Override
        boolean isCacheable() {
            return origin.isCacheable()
        }

        @Override
        String toString() {
            return origin.toString()
        }
    }
}