package com.didi.virtualapk.collector.dependence

import com.android.builder.model.JavaLibrary

/**
 * Represents a Jar library. This could be the output of a Java project.
 *
 * @author zhengtao
 */
class JarDependenceInfo extends DependenceInfo {

    JavaLibrary library

    JarDependenceInfo(String group, String artifact, String version, JavaLibrary library) {
        super(group, artifact, version)
        this.library = library
    }

    @Override
    File getJarFile() {
        return library.jarFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.JAR
    }
}