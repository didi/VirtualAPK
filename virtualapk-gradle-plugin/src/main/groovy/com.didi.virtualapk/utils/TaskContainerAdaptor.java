package com.didi.virtualapk.utils;

import com.android.annotations.Nullable;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.internal.project.taskfactory.TaskFactory;
import org.gradle.api.tasks.TaskContainer;

///**
// * Created by muyonggang on 2023/7/13
// *
// * @author muyonggang@bytedance.com
// */
//
//public class TaskContainerAdaptor implements TaskFactory {
//
//    private final TaskContainer tasks;
//
//    public TaskContainerAdaptor(TaskContainer tasks) {
//        this.tasks = tasks;
//    }
//
//    @Override
//    public boolean containsKey(String name) {
//        return tasks.findByName(name) != null;
//    }
//
//    @Override
//    public void create(String name) {
//        tasks.create(name);
//    }
//
//    @Override
//    public void create(String name, Action<? super Task> configAction) {
//        tasks.create(name, configAction);
//    }
//
//    @Override
//    public <S extends Task> void create(String name, Class<S> type) {
//        tasks.create(name, type);
//    }
//
//    @Override
//    public <S extends Task> void create(String name, Class<S> type,
//                                        Action<? super S> configAction) {
//        tasks.create(name, type, configAction);
//    }
//
//    @Override
//    public void named(String name, Action<? super Task> configAction) {
//        configAction.execute(tasks.findByName(name));
//    }
//
//    @Nullable
//    @Override
//    public Task named(String name) {
//        return tasks.getByName(name);
//    }
//}
