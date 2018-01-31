package com.didi.virtualapk.transform

import com.android.build.api.transform.*

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
                println "input dir: ${directoryInput.getFile()}"
            }
            for (JarInput jarInput : input.getJarInputs()) {
                println "input jar: ${jarInput.getFile()}"
            }
        }

        Collection<TransformInput> referencedInputs = invocation.getReferencedInputs();
        for (TransformInput transformInput : referencedInputs) {
            for (DirectoryInput directoryInput : transformInput.getDirectoryInputs()) {
                println "referenced input dir: ${directoryInput.getFile()}"
            }
            for (JarInput jarInput : transformInput.getJarInputs()) {
                println "referenced input jar: ${jarInput.getFile()}"
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