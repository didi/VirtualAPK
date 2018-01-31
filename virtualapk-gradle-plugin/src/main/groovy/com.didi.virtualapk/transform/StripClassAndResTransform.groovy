package com.didi.virtualapk.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.didi.virtualapk.VAExtention
import com.didi.virtualapk.collector.HostClassAndResCollector
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

        def stripEntries = classAndResCollector.collect(virtualApk.stripDependencies)

        if (!isIncremental()) {
            transformInvocation.outputProvider.deleteAll()
        }

        transformInvocation.inputs.each {
            it.directoryInputs.each { directoryInput ->
//                println "input dir: ${directoryInput.file.absoluteFile}"
                def destDir = transformInvocation.outputProvider.getContentLocation(
                        directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
//                println "output dir: ${destDir.absoluteFile}"
                directoryInput.file.traverse(type: FileType.FILES) {
                    def entryName = it.path.substring(directoryInput.file.path.length() + 1)
//                    println "found file: ${it.absoluteFile}"
//                    println "entryName: ${entryName}"
                    if (!stripEntries.contains(entryName)) {
                        def dest = new File(destDir, entryName)
                        FileUtils.copyFile(it, dest)
                        println "Copied to file: ${dest.absoluteFile}"
                    } else {
                        println "Stripped file: ${it.absoluteFile}"
                    }
                }
            }

            it.jarInputs.each { jarInput ->
//                println "${name} jar: ${jarInput.file.absoluteFile}"
                Set<String> jarEntries = HostClassAndResCollector.unzipJar(jarInput.file)
                if (!stripEntries.containsAll(jarEntries)){
                    def dest = transformInvocation.outputProvider.getContentLocation(jarInput.name,
                            jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    FileUtils.copyFile(jarInput.file, dest)
                    println "Copied to jar: ${dest.absoluteFile}"
                } else {
                    println "Stripped jar: ${jarInput.file.absoluteFile}"
                }
            }
        }
    }
}