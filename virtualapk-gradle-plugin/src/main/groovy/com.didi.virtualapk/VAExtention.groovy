package com.didi.virtualapk

import com.android.build.gradle.internal.variant.BaseVariantData
import com.didi.virtualapk.collector.dependence.AarDependenceInfo
import com.didi.virtualapk.collector.dependence.DependenceInfo

/**
 * VirtualApk extension for plugin projects.
 *
 * @author zhengtao
 */
public class VAExtention {

    /** Custom defined resource package Id **/
    int packageId
    /** Local host application directory or Jenkins build number, fetch config files from here **/
    String targetHost
    /** Apply Host Proguard Mapping or not**/
    boolean applyHostMapping = true
    /** Exclude dependent aar or jar **/
    Collection<String> excludes = new HashSet<>()

    /**  host Symbol file - Host_R.txt */
    File hostSymbolFile
    /**  host dependence file - version.txt*/
    File hostDependenceFile


    Collection<DependenceInfo> stripDependencies = []
    Collection<AarDependenceInfo> retainedAarLibs = []

    /** Variant application id */
    String packageName

    /** Package path for java classes */
    String packagePath

    /** File of split R.java */
    File splitRJavaFile

    BaseVariantData variantData

    public void exclude(final String...filters) {
        if (null != filters) {
            for (final String filter :filters) {
                this.excludes.add(filter);
            }
        }
    }
}