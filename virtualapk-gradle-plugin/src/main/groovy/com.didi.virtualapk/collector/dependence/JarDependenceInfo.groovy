package com.didi.virtualapk.collector.dependence

import com.android.builder.dependency.level2.JavaDependency

/**
 * Represents a Jar dependency. This could be the output of a Java project.
 *
 * @author zhengtao
 */
class JarDependenceInfo extends DependenceInfo {

    @Delegate JavaDependency dependency

    JarDependenceInfo(String group, String artifact, String version, JavaDependency jarDependency) {
        super(group, artifact, version)
        this.dependency = jarDependency
    }

    @Override
    File getJarFile() {
        return dependency.artifactFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.JAR
    }
}