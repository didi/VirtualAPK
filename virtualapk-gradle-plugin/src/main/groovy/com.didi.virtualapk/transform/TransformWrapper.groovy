package com.didi.virtualapk.transform

import com.android.build.api.transform.*
import com.didi.virtualapk.utils.Log

public class TransformWrapper extends Transform {

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
        Collection<TransformInput> inputs = invocation.getInputs()
        for (TransformInput input : inputs) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                Log.i "${name}", "input dir: ${directoryInput.getFile()}"
            }
            for (JarInput jarInput : input.getJarInputs()) {
                Log.i "${name}", "input jar: ${jarInput.getFile()}"
            }
        }

        Collection<TransformInput> referencedInputs = invocation.getReferencedInputs();
        for (TransformInput transformInput : referencedInputs) {
            for (DirectoryInput directoryInput : transformInput.getDirectoryInputs()) {
                Log.i "${name}", "referenced input dir: ${directoryInput.getFile()}"
            }
            for (JarInput jarInput : transformInput.getJarInputs()) {
                Log.i "${name}", "referenced input jar: ${jarInput.getFile()}"
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