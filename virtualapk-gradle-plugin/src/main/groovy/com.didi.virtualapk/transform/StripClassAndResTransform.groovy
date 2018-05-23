package com.didi.virtualapk.transform

import com.android.build.api.transform.*
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformManager
import com.didi.virtualapk.VAExtention
import com.didi.virtualapk.collector.HostClassAndResCollector
import com.didi.virtualapk.utils.Log
import groovy.io.FileType
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
/**
 * Strip Host classes and java resources from project, it's an equivalent of provided compile
 * @author zhengtao
 */
class StripClassAndResTransform extends Transform {

    private Project project
    private VAExtention virtualApk
    private HostClassAndResCollector classAndResCollector

    StripClassAndResTransform(Project project) {
        this.project = project
        this.virtualApk = project.virtualApk
        classAndResCollector = new HostClassAndResCollector()
    }

    void onProjectAfterEvaluate() {
        project.android.applicationVariants.each { ApplicationVariant variant ->
            virtualApk.getVaContext(variant.name).checkList.addCheckPoint(name)
        }
    }

    @Override
    String getName() {
        return 'stripClassAndRes'
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_JARS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * Only copy the jars or classes and java resources of retained aar into output directory
     */
    @Override
    void transform(final TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        VAExtention.VAContext vaContext = virtualApk.getVaContext(transformInvocation.context.variantName)
        def stripEntries = classAndResCollector.collect(vaContext.stripDependencies)

        if (!isIncremental()) {
            transformInvocation.outputProvider.deleteAll()
        }

        transformInvocation.inputs.each {
            it.directoryInputs.each { directoryInput ->
                Log.i 'StripClassAndResTransform', "input dir: ${directoryInput.file.absoluteFile}"
                def destDir = transformInvocation.outputProvider.getContentLocation(
                        directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                Log.i 'StripClassAndResTransform', "output dir: ${destDir.absoluteFile}"
                directoryInput.file.traverse(type: FileType.FILES) {
                    def entryName = it.path.substring(directoryInput.file.path.length() + 1)
//                    Log.i 'StripClassAndResTransform', "found file: ${it.absoluteFile}"
//                    Log.i 'StripClassAndResTransform', "entryName: ${entryName}"
                    if (!stripEntries.contains(entryName)) {
                        def dest = new File(destDir, entryName)
                        FileUtils.copyFile(it, dest)
//                        Log.i 'StripClassAndResTransform', "Copied to file: ${dest.absoluteFile}"
                    } else {
                        Log.i 'StripClassAndResTransform', "Stripped file: ${it.absoluteFile}"
                    }
                }
            }

            it.jarInputs.each { jarInput ->
                Log.i 'StripClassAndResTransform', "input jar: ${jarInput.file.absoluteFile}"
                Set<String> jarEntries = HostClassAndResCollector.unzipJar(jarInput.file)
                if (!stripEntries.containsAll(jarEntries)){
                    def dest = transformInvocation.outputProvider.getContentLocation(jarInput.name,
                            jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    Log.i 'StripClassAndResTransform', "output jar: ${dest.absoluteFile}"
                    FileUtils.copyFile(jarInput.file, dest)
//                    Log.i 'StripClassAndResTransform', "Copied to jar: ${dest.absoluteFile}"
                } else {
                    Log.i 'StripClassAndResTransform', "Stripped jar: ${jarInput.file.absoluteFile}"
                }
            }
        }

        vaContext.checkList.mark(name)
    }
}