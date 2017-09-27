package com.didi.virtualapk

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.TaskContainerAdaptor
import com.android.build.gradle.internal.TaskFactory
import com.didi.virtualapk.tasks.AssemblePlugin
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.reflect.Instantiator
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry

import javax.inject.Inject;

/**
 * Base class of VirtualApk plugin, we create assemblePlugin task here
 * @author zhengtao
 */
public class BasePlugin implements Plugin<Project> {

    protected Project project
    protected Instantiator instantiator
    protected TaskFactory taskFactory
    protected boolean isBuildingPlugin = false


    @Inject
    public BasePlugin(Instantiator instantiator, ToolingModelBuilderRegistry registry) {
        this.instantiator = instantiator
    }

    @Override
    public void apply(Project project) {
        this.project = project

        def startParameter = project.gradle.startParameter
        def targetTasks = startParameter.taskNames

        targetTasks.each {
            if (it.contains("assemblePlugin") || it.contains("aP")) {
                isBuildingPlugin = true
            }
        }

        project.extensions.create('virtualApk', VAExtention)

        taskFactory = new TaskContainerAdaptor(project.tasks)
        project.afterEvaluate {
            project.android.applicationVariants.each { ApkVariant variant ->
                if (variant.buildType.name.equalsIgnoreCase("release")) {
                    final def variantPluginTaskName = "assemblePlugin${variant.name.capitalize()}"
                    final def configAction = new AssemblePlugin.ConfigAction(project, variant)

                    taskFactory.create(variantPluginTaskName, AssemblePlugin, configAction)

                    taskFactory.named("assemblePlugin", new Action<Task>() {
                        @Override
                        void execute(Task task) {
                            task.dependsOn(variantPluginTaskName)
                        }
                    })
                }
            }
        }

        project.task('assemblePlugin', dependsOn: "assembleRelease", group: 'build', description: 'Build plugin apk')
    }


    protected final VAExtention getVirtualApk() {
        return this.project.virtualApk
    }
}
