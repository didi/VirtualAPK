package com.didi.virtualapk.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.didi.virtualapk.VAExtention
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project

/**
 * Minify R class file under the applicationId namespace before dx task
 *
 * @author zhengtao
 */
class DxTaskHooker extends GradleTaskHooker<TransformTask> {


    public DxTaskHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }

    @Override
    String getTaskName() {
        return "dex"
    }

    /**
     * Replace the R class files record all resources with the stripped R only record plugin resources.
     * Input file may be a directory or jar file
     *
     * @param task Gradle transform task for dex
     */
    @Override
    void beforeTaskExecute(TransformTask task) {
        task.inputs.files.each { input ->
            if(input.directory) {
                input.eachFileRecurse { file ->
                    if (file.directory && file.path.endsWith(virtualApk.packagePath)) {

                        recompileSplitR(file)

                    } else if (file.file && file.name.endsWith('.jar')) {
                        // Decompress jar file
                        File unzipJarDir = new File(file.parentFile, FilenameUtils.getBaseName(file.name))
                        project.copy {
                            from project.zipTree(file)
                            into unzipJarDir
                        }

                        // VirtualApk Package Dir
                        File pkgDir = new File(unzipJarDir, virtualApk.packagePath)
                        if (pkgDir.exists()) {
                            boolean compileResult = recompileSplitR(pkgDir)
                            if (compileResult) {
                                project.ant.zip(baseDir: unzipJarDir, destFile: file)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete the large R class file under the applicationId namespace, then
     * compile the splitRJavaFile to generate the R class file only records
     * plugin resources
     *
     * @param pkgDir The path to storing the R class file
     * @return true if the search&delete&compile actions succeed
     */
    boolean recompileSplitR(File pkgDir) {

        File[] RClassFiles = pkgDir.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                return name.startsWith('R$') && name.endsWith('.class')
            }
        })

        if(RClassFiles?.length) {
            RClassFiles.each {
                it.delete()
            }

            String baseDir = pkgDir.path - "/${virtualApk.packagePath}"

            project.ant.javac(
                srcdir: virtualApk.splitRJavaFile.parentFile,
                source: apkVariant.javaCompiler.sourceCompatibility,
                target: apkVariant.javaCompiler.targetCompatibility,
                destdir: new File(baseDir))

            return true
        }

        return false
    }


    @Override
    void afterTaskExecute(TransformTask task) { }
}