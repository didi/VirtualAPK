package com.didi.virtualapk.hooker

import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.pipeline.TransformTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.internal.reflect.Instantiator

/**
 * Manager of hookers, responsible for registration and scheduling execution
 *
 * @author zhengtao
 */
public abstract class TaskHookerManager {

    protected Map<String, GradleTaskHooker> taskHookerMap = new HashMap<>()

    protected Project project
    protected AppExtension android
    protected Instantiator instantiator

    public TaskHookerManager(Project project, Instantiator instantiator) {
        this.project = project
        this.instantiator = instantiator
        android = project.extensions.findByType(AppExtension)
        project.gradle.addListener(new VirtualApkTaskListener())
    }

    public abstract void registerTaskHookers()

    protected void registerTaskHooker(GradleTaskHooker taskHooker) {
        taskHooker.setTaskHookerManager(this)
        taskHookerMap.put(taskHooker.taskName, taskHooker)
    }


    public <T> T findHookerByName(String taskName) {
        return taskHookerMap[taskName] as T
    }

    private class VirtualApkTaskListener implements TaskExecutionListener {

        @Override
        void beforeExecute(Task task) {
//            println "beforeExecute ${task.name} tid: ${Thread.currentThread().id} t: ${Thread.currentThread().name}"
            if (task.project == project) {
                if (task in TransformTask) {
                    taskHookerMap[task.transform.name]?.beforeTaskExecute(task)
                } else {
                    taskHookerMap[task.name]?.beforeTaskExecute(task)
                }
            }
        }

        @Override
        void afterExecute(Task task, TaskState taskState) {
//            println "afterExecute ${task.name} tid: ${Thread.currentThread().id} t: ${Thread.currentThread().name}"
            if (task.project == project) {
                if (task in TransformTask) {
                    taskHookerMap[task.transform.name]?.afterTaskExecute(task)
                } else {
                    taskHookerMap[task.name]?.afterTaskExecute(task)
                }
            }
        }
    }

}