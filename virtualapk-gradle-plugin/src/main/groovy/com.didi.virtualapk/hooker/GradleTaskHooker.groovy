package com.didi.virtualapk.hooker

import com.android.build.gradle.api.ApkVariant
import com.didi.virtualapk.VAExtention
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Base class of gradle task hooker， provides some common field used by hookers
 * @param <T> Type of hooked task
 *
 * @author zhengtao
 */
public abstract class GradleTaskHooker<T extends Task> {

    private Project project

    /**
     * A Build variant when build a apk and all its public data.
     */
    private ApkVariant apkVariant

    private VAExtention virtualApk

    private TaskHookerManager taskHookerManager

    public GradleTaskHooker(Project project, ApkVariant apkVariant) {
        this.project = project
        this.apkVariant = apkVariant
        this.virtualApk = project.virtualApk
    }

    public Project getProject() {
        return this.project
    }


    public ApkVariant getApkVariant() {
        return this.apkVariant
    }

    public VAExtention getVirtualApk() {
        return this.virtualApk
    }

    public void setTaskHookerManager(TaskHookerManager taskHookerManager) {
        this.taskHookerManager = taskHookerManager
    }

    public TaskHookerManager getTaskHookerManager() {
        return this.taskHookerManager
    }

    public T getTask() {

    }

    /**
     * Return the task name or transform name of the hooked task(transform task)
     */
    public abstract String getTaskName()

    /**
     * Callback function before the hooked task executes
     * @param task Hooked task
     */
    public abstract void beforeTaskExecute(T task)
    /**
     * Callback function after the hooked task executes
     * @param task Hooked task
     */
    public abstract void afterTaskExecute(T task)
}