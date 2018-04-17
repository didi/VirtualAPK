package com.didi.virtualapk.tasks

import com.android.annotations.NonNull
import com.android.build.gradle.api.ApkVariant
import com.didi.virtualapk.utils.TaskUtil
import com.sun.istack.internal.NotNull
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Callable

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

    /**
     * Copy the plugin apk to out/plugin directory and rename to
     * the format required for the backend system
     */
    @TaskAction
    public void outputPluginApk() {
        project.virtualApk.checkList.check(variantName)

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
        ApkVariant variant

        ConfigAction(@NotNull Project project, @NotNull ApkVariant variant) {
            this.project = project
            this.variant = variant
        }

        @Override
        void execute(AssemblePlugin assemblePluginTask) {

            map(assemblePluginTask, "appPackageName") {
                variant.applicationId
            }

            map(assemblePluginTask, "apkTimestamp", {
                new Date().format("yyyyMMddHHmmss")
            })

            map(assemblePluginTask, "originApkFile") {
                variant.outputs[0].outputFile
            }

            map(assemblePluginTask, "pluginApkDir") {
                new File(project.buildDir, "/outputs/plugin/${variant.name}")
            }

            map(assemblePluginTask, "variantName") {
                variant.name
            }

            assemblePluginTask.setGroup("build")
            assemblePluginTask.setDescription("Build ${variant.name.capitalize()} plugin apk")
            assemblePluginTask.dependsOn(variant.assemble.name)
        }

        static void map(@NonNull Task task, @NonNull String key, @NonNull Callable<?> value) {
            TaskUtil.map(task, key, value)
        }
    }

}
