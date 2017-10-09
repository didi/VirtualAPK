package com.didi.virtualapk.collector.dependence

import com.android.builder.dependency.LibraryDependency
import com.didi.virtualapk.collector.dependence.DependenceInfo
import com.didi.virtualapk.collector.res.ResourceEntry
import com.didi.virtualapk.collector.res.StyleableEntry
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists

/**
 * Represents a AAR dependence from Maven repository or Android library module
 *
 * @author zhengtao
 */
class AarDependenceInfo extends DependenceInfo {

    /**
     * Android library dependence in android build system, delegate of AarDependenceInfo
     */
    @Delegate LibraryDependency libraryDependency

    /**
     * All resources(e.g. drawable, layout...) this library can access
     * include resources of self-project and dependence(direct&transitive) project
     */
    ListMultimap<String, ResourceEntry> aarResources = ArrayListMultimap.create()
    /**
     * All styleables this library can access, like "aarResources"
     */
    List<StyleableEntry> aarStyleables = Lists.newArrayList()

    AarDependenceInfo(String group, String artifact, String version, LibraryDependency libraryDependency) {
        super(group, artifact, version)
        this.libraryDependency = libraryDependency
    }

    @Override
    File getJarFile() {
        return libraryDependency.jarFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.AAR
    }

    /**
     * Return collection of "resourceType:resourceName", parse from R symbol file
     * @return set of a combination of resource type and name
     */
    public Set<String> getResourceKeys() {

        def resKeys = [] as Set<String>

        def rSymbol = new File(folder, 'R.txt')
        if (rSymbol.exists()) {
            rSymbol.eachLine { line ->
                if (!line.empty) {
                    def tokenizer = new StringTokenizer(line)
                    def valueType = tokenizer.nextToken()
                    def resType = tokenizer.nextToken()       // resource type (attr/string/color etc.)
                    def resName = tokenizer.nextToken()       // resource name

                    resKeys.add("${resType}:${resName}")
                }
            }
        }

        return resKeys
    }

    /**
     * Return the package name of this library, parse from manifest file
     * manifest file are obtained by delegating to "libraryDependency"
     * @return package name of this library
     */
    public String getPackage() {
        def xmlManifest = new XmlParser().parse(manifest)
        return xmlManifest.@package
    }
}