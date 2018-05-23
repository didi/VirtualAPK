package com.didi.virtualapk.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.didi.virtualapk.utils.Log
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
    String getTransformName() {
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
//            Log.i 'DxTaskHooker', "${task.name}: ${input.absoluteFile}"
            if(input.directory) {
                input.eachFileRecurse { file ->
                    handleFile(file)
                }
            } else {
                handleFile(input)
            }
        }
    }

    void handleFile(File file) {
        if (file.directory && file.path.endsWith(vaContext.packagePath)) {

            if (recompileSplitR(file)) {
                Log.i 'DxTaskHooker', "Recompiled R.java in dir: ${file.absoluteFile}"
            }

        } else if (file.file && file.name.endsWith('.jar')) {
            // Decompress jar file
            File unzipJarDir = new File(file.parentFile, FilenameUtils.getBaseName(file.name))
            project.copy {
                from project.zipTree(file)
                into unzipJarDir
            }

            // VirtualApk Package Dir
            File pkgDir = new File(unzipJarDir, vaContext.packagePath)
            if (pkgDir.exists()) {
                if (recompileSplitR(pkgDir)) {
                    Log.i 'DxTaskHooker', "Recompiled R.java in jar: ${file.absoluteFile}"
                    File backupDir = new File(vaContext.getBuildDir(scope), 'origin/classes')
                    backupDir.deleteDir()
                    project.copy {
                        from file
                        into backupDir
                    }

                    project.ant.zip(baseDir: unzipJarDir, destFile: file)
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

            String baseDir = pkgDir.path - "${File.separator}${vaContext.packagePath}"

            project.ant.javac(
                srcdir: vaContext.splitRJavaFile.parentFile,
                source: apkVariant.javaCompiler.sourceCompatibility,
                target: apkVariant.javaCompiler.targetCompatibility,
                destdir: new File(baseDir))

            mark()
            return true
        }

        return false
    }


    @Override
    void afterTaskExecute(TransformTask task) { }
}