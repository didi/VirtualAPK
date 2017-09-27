package com.didi.virtualapk.transform

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.didi.virtualapk.VAExtention
import com.didi.virtualapk.collector.HostClassAndResCollector
import com.didi.virtualapk.hooker.PrepareDependenciesHooker
import com.didi.virtualapk.utils.ZipUtil
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
                directoryInput.file.traverse (type: FileType.FILES){
                    def entryName = it.path.substring(directoryInput.file.path.length() + 1)
                    def destName = directoryInput.name + '/' + entryName
                    def dest = transformInvocation.outputProvider.getContentLocation(
                            destName, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                    if (!stripEntries.contains(entryName)) {
                        FileUtils.copyFile(it, dest)
                    }
                }
            }

            it.jarInputs.each { jarInput ->
                Set<String> jarEntries = HostClassAndResCollector.unzipJar(jarInput.file)
                if (!stripEntries.containsAll(jarEntries)){
                    def dest = transformInvocation.outputProvider.getContentLocation(jarInput.name,
                            jarInput.contentTypes, jarInput.scopes, Format.JAR)
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
        }
    }
}