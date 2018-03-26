package com.didi.virtualapk.utils

import com.android.annotations.NonNull
import org.gradle.api.Task
import org.gradle.api.internal.ConventionMapping

import java.util.concurrent.Callable

/**
 * Created by qiaopu on 2018/3/19.
 */
public class TaskUtil {
    
    public static void map(@NonNull Task task, @NonNull String key, @NonNull Callable<?> value) {
        if (task instanceof GroovyObject) {
            ConventionMapping conventionMapping =
                    (ConventionMapping) ((GroovyObject) task).getProperty("conventionMapping");
            conventionMapping.map(key, value);
        } else {
            throw new IllegalArgumentException(
                    "Don't know how to apply convention mapping to task of type " + task.getClass().getName());
        }
    }

}
