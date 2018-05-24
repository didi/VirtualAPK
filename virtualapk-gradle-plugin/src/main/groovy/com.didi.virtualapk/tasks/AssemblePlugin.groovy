package com.didi.virtualapk.tasks

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.didi.virtualapk.VAExtention
import com.didi.virtualapk.utils.Log
import com.sun.istack.internal.NotNull
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task for assemble plugin apk
 * @author zhengtao
 */
public class AssemblePlugin extends DefaultTask {

    @OutputDirectory
    File pluginApkDir

    @Input
    String appPackageName

    @Input
    String apkTimestamp

    @Input
    File originApkFile

    String variantName

    String buildDir

    /**
     * Copy the plugin apk to out/plugin directory and rename to
     * the format required for the backend system
     */
    @TaskAction
    public void outputPluginApk() {
        VAExtention virtualApk = project.virtualApk
        virtualApk.getVaContext(variantName).checkList.check()
        virtualApk.printWarning(name)

        if (virtualApk.getFlag('tip.forceUseHostDependences')) {
            def tip = new StringBuilder('To avoid configuration WARNINGs, you could set the forceUseHostDependences to be true in build.gradle,\n ')
            tip.append('please declare it in application project build.gradle:\n')
            tip.append('    virtualApk {\n')
            tip.append('        forceUseHostDependences = true \n')
            tip.append('    }\n')
            Log.i name, tip.toString()
        }

        Log.i name, "More building infomation could be found in the dir: ${buildDir}."

        getProject().copy {
            from originApkFile
            into pluginApkDir
            rename { "${appPackageName}_${apkTimestamp}.apk" }
        }
    }


    public static class ConfigAction implements Action<AssemblePlugin> {

        @NotNull
        Project project
        @NotNull
        ApplicationVariantImpl variant

        ConfigAction(@NotNull Project project, @NotNull ApkVariant variant) {
            this.project = project
            this.variant = variant
        }

        @Override
        void execute(AssemblePlugin assemblePluginTask) {
            VAExtention virtualApk = project.virtualApk

            assemblePluginTask.appPackageName = variant.applicationId
            assemblePluginTask.apkTimestamp = new Date().format("yyyyMMddHHmmss")
            assemblePluginTask.originApkFile = variant.outputs[0].outputFile
            assemblePluginTask.pluginApkDir = new File(project.buildDir, "/outputs/plugin/${variant.name}")
            assemblePluginTask.variantName = variant.name
            assemblePluginTask.buildDir = virtualApk.getVaContext(variant.name).getBuildDir(variant.variantData.scope).canonicalPath

            assemblePluginTask.setGroup("build")
            assemblePluginTask.setDescription("Build ${variant.name.capitalize()} plugin apk")
            assemblePluginTask.dependsOn(variant.assemble.name)
        }
    }

}
