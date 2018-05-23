package com.didi.virtualapk.hooker

import com.android.build.gradle.AndroidConfig
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.scope.TaskOutputHolder
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.sdklib.BuildToolInfo
import com.didi.virtualapk.aapt.Aapt
import com.didi.virtualapk.collector.ResourceCollector
import com.didi.virtualapk.collector.res.ResourceEntry
import com.didi.virtualapk.collector.res.StyleableEntry
import com.didi.virtualapk.utils.FileUtil
import com.didi.virtualapk.utils.Log
import com.google.common.collect.ListMultimap
import com.google.common.io.Files
import org.gradle.api.Project

/**
 * Filter the host resources out of the plugin apk.
 * Modify the .arsc file to delete host element,
 * rearrange plugin element, hold the new resource IDs
 *
 * @author zhengtao
 */
class ProcessResourcesHooker extends GradleTaskHooker<ProcessAndroidResources> {

    /**
     * Collector to gather the sources and styleables
     */
    ResourceCollector resourceCollector
    /**
     * Android config information specified in build.gradle
     */
    AndroidConfig androidConfig

    ProcessResourcesHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
        androidConfig = project.extensions.findByType(AppExtension)
    }

    @Override
    String getTaskName() {
        return scope.getTaskName('process', 'Resources')
    }

    @Override
    void beforeTaskExecute(ProcessAndroidResources aaptTask) {

    }

    /**
     * Since we need to remove the host resources and modify the resource ID,
     * we will reedit the AP_ file and repackage it after the task execute
     *
     * @param par Gradle task of process android resources
     */
    @Override
    void afterTaskExecute(ProcessAndroidResources par) {
        variantData.outputScope.getOutputs(TaskOutputHolder.TaskOutputType.PROCESSED_RES).each {
            repackage(par, it.outputFile)
        }
    }

    void repackage(ProcessAndroidResources par, File apFile) {
        def resourcesDir = new File(apFile.parentFile, Files.getNameWithoutExtension(apFile.name))

        /*
         * Clean up resources merge directory
         */
        resourcesDir.deleteDir()

        File backupFile = new File(apFile.getParentFile(), "${Files.getNameWithoutExtension(apFile.name)}-original.${Files.getFileExtension(apFile.name)}")
        backupFile.delete()
        project.copy {
            from apFile
            into apFile.getParentFile()
            rename { backupFile.name }
        }

        /*
         * Unzip resources-${variant.name}.ap_
         */
        project.copy {
            from project.zipTree(apFile)
            into resourcesDir

            include 'AndroidManifest.xml'
            include 'resources.arsc'
            include 'res/**/*'
        }

//        File backupDir = new File(resourcesDir.parentFile, resourcesDir.name + '-original')
//        backupDir.deleteDir()
//        project.copy {
//            from project.fileTree(resourcesDir)
//            into backupDir
//        }

        resourceCollector = new ResourceCollector(project, par)
        resourceCollector.collect()

        def retainedTypes = convertResourcesForAapt(resourceCollector.pluginResources)
        def retainedStylealbes = convertStyleablesForAapt(resourceCollector.pluginStyleables)
        def resIdMap = resourceCollector.resIdMap

        def rSymbolFile = par.textSymbolOutputFile
        def libRefTable = ["${virtualApk.packageId}": par.packageForR]
        def filteredResources = [] as HashSet<String>
        def updatedResources = [] as HashSet<String>

        def aapt = new Aapt(resourcesDir, rSymbolFile, androidConfig.buildToolsRevision)

        //Delete host resources, must do it before filterPackage
        aapt.filterResources(retainedTypes, filteredResources)
        //Modify the arsc file, and replace ids of related xml files
        aapt.filterPackage(retainedTypes, retainedStylealbes, virtualApk.packageId, resIdMap, libRefTable, updatedResources)

        File hostDir = vaContext.getBuildDir(scope)
        FileUtil.saveFile(hostDir, "${taskName}-retainedTypes", retainedTypes)
        FileUtil.saveFile(hostDir, "${taskName}-retainedStylealbes", retainedStylealbes)
        FileUtil.saveFile(hostDir, "${taskName}-filteredResources", filteredResources)
        FileUtil.saveFile(hostDir, "${taskName}-updatedResources", updatedResources)

        /*
         * Delete filtered entries and then add updated resources into resources-${variant.name}.ap_
         */
        com.didi.virtualapk.utils.ZipUtil.with(apFile).deleteAll(filteredResources + updatedResources)

        project.exec {
            executable par.buildTools.getPath(BuildToolInfo.PathId.AAPT)
            workingDir resourcesDir
            args 'add', apFile.path
            args updatedResources
            standardOutput = System.out
            errorOutput = System.err
        }

        updateRJava(aapt, par.sourceOutputDir)
        mark()
    }

    /**
     * Because the resource ID has changed, we need to regenerate the R.java file,
     * include the all resources R, plugin resources R, and R files of retained aars
     *
     * @param aapt Class to expand aapt function
     * @param sourceOutputDir Directory of R.java files generated by aapt
     *
     */
    def updateRJava(Aapt aapt, File sourceOutputDir) {
        File vaBuildDir = vaContext.getBuildDir(scope)
        File backupDir = new File(vaBuildDir, "origin/r")

        project.ant.move(todir: backupDir) {
            fileset(dir: sourceOutputDir) {
                include(name: '**/R.java')
            }
        }

        FileUtil.deleteEmptySubfolders(sourceOutputDir)

        def rSourceFile = new File(sourceOutputDir, "${vaContext.packagePath}${File.separator}R.java")
        aapt.generateRJava(rSourceFile, apkVariant.applicationId, resourceCollector.allResources, resourceCollector.allStyleables)
        Log.i 'ProcessResourcesHooker', "Updated R.java: ${rSourceFile.absoluteFile}"

        def splitRSourceFile = new File(vaBuildDir, "source${File.separator}r${File.separator}${vaContext.packagePath}${File.separator}R.java")
        aapt.generateRJava(splitRSourceFile, apkVariant.applicationId, resourceCollector.pluginResources, resourceCollector.pluginStyleables)
        Log.i 'ProcessResourcesHooker', "Updated R.java: ${splitRSourceFile.absoluteFile}"
        vaContext.splitRJavaFile = splitRSourceFile

        vaContext.retainedAarLibs.each {
            def aarPackage = it.package
            def rJavaFile = new File(sourceOutputDir, "${aarPackage.replace('.'.charAt(0), File.separatorChar)}${File.separator}R.java")
            aapt.generateRJava(rJavaFile, aarPackage, it.aarResources, it.aarStyleables)
            Log.i 'ProcessResourcesHooker', "Updated R.java: ${rJavaFile.absoluteFile}"
        }
    }

    /**
     * We use the third party library to modify the ASRC file,
     * this method used to transform resource data into the structure of the library
     * @param pluginResources Map of plugin resources
     */
    def convertResourcesForAapt(ListMultimap<String, ResourceEntry> pluginResources) {
        def retainedTypes = []
        retainedTypes.add(0,  [name : 'placeholder', id : Aapt.ID_NO_ATTR, entries: []])//attr 占位

        pluginResources.keySet().each { resType ->
            def firstEntry = pluginResources.get(resType).get(0)
            def typeEntry = [ type: "int", name: resType,
                              id: parseTypeIdFromResId(firstEntry.resourceId),
                              _id: parseTypeIdFromResId(firstEntry.newResourceId),
                              entries: []]

            pluginResources.get(resType).each { resEntry ->
                typeEntry.entries.add([
                        name : resEntry.resourceName,
                        id : parseEntryIdFromResId(resEntry.resourceId),
                        _id: parseEntryIdFromResId(resEntry.newResourceId),
                        v : resEntry.resourceId, _v : resEntry.newResourceId,
                        vs: resEntry.hexResourceId, _vs : resEntry.hexNewResourceId])
            }

            if (resType == 'attr') {
                retainedTypes.set(0, typeEntry)
            } else {
                retainedTypes.add(typeEntry)
            }
        }

        return retainedTypes
    }

    /**
     * Transform styleable data into the structure of the aapt library
     * @param pluginStyleables Map of plugin styleables
     */
    def convertStyleablesForAapt(List<StyleableEntry> pluginStyleables) {
        def retainedStyleables = []
        pluginStyleables.each { styleableEntry ->
            retainedStyleables.add([vtype : styleableEntry.valueType,
                                    type : 'styleable',
                                    key : styleableEntry.name,
                                    idStr : styleableEntry.value])
        }
        return retainedStyleables
    }

    /**
     * Parse the type part of a android resource id
     */
    def parseTypeIdFromResId(int resourceId) {
        resourceId >> 16 & 0xFF
    }

    /**
     * Parse the entry part of a android resource id
     */
    def parseEntryIdFromResId(int resourceId) {
        resourceId & 0xFFFF
    }

}