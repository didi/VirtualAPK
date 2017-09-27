package com.didi.virtualapk.collector.dependence

import com.android.builder.dependency.JarDependency
import com.didi.virtualapk.collector.dependence.DependenceInfo

/**
 * Represents a Jar dependency. This could be the output of a Java project.
 *
 * @author zhengtao
 */
class JarDependenceInfo extends DependenceInfo {

    @Delegate JarDependency jarDependency

    JarDependenceInfo(String group, String artifact, String version, JarDependency jarDependency) {
        super(group, artifact, version)
        this.jarDependency = jarDependency
    }

    @Override
    File getJarFile() {
        return jarDependency.jarFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.JAR
    }
}