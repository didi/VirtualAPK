package com.didi.virtualapk.collector

import com.didi.virtualapk.collector.dependence.AarDependenceInfo
import com.didi.virtualapk.collector.dependence.DependenceInfo

import java.util.zip.ZipFile

/**
 * Collector of Class and Java Resource(no-class files in jar) in host apk
 *
 * @author zhengtao
 */

class HostClassAndResCollector {

    private def hostJarFiles = [] as LinkedList<File>
    private def hostClassesAndResources = [] as LinkedHashSet<String>

    /**
     * Collect jar entries that already exist in the host apk
     *
     * @param stripDependencies DependencyInfos that exists in the host apk, including AAR and JAR
     * @return set of classes and java resources
     */
    public Set<String> collect(Collection<DependenceInfo> stripDependencies) {
        flatToJarFiles(stripDependencies, hostJarFiles)
        hostJarFiles.each {
            hostClassesAndResources.addAll(unzipJar(it))
        }
        hostClassesAndResources
    }

    /**
     * Collect the jar files that are held by the DependenceInfo， including local jars of the DependenceInfo
     * @param stripDependencies Collection of DependenceInfo
     * @param jarFiles Collection used to store jar files
     */
    def flatToJarFiles(Collection<DependenceInfo> stripDependencies, Collection<File> jarFiles) {
        stripDependencies.each {
            jarFiles.add(it.jarFile)
            if (it instanceof AarDependenceInfo) {
                it.localJars.each {
                    jarFiles.add(it)
                }
            }
        }
    }

    /**
     * Unzip the entries of Jar
     *
     * @return Set of entries in the JarFile
     */
    public static Set<String> unzipJar(File jarFile) {

        def jarEntries = [] as Set<String>

        ZipFile zipFile = new ZipFile(jarFile)
        try {
            zipFile.entries().each {
                jarEntries.add(it.name)
            }
        } finally {
            zipFile.close();
        }

        return jarEntries
    }

}