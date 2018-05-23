package com.didi.virtualapk

import com.android.build.gradle.internal.scope.VariantScope
import com.didi.virtualapk.collector.dependence.AarDependenceInfo
import com.didi.virtualapk.collector.dependence.DependenceInfo
import com.didi.virtualapk.utils.CheckList

/**
 * VirtualApk extension for plugin projects.
 *
 * @author zhengtao
 */
public class VAExtention {

    /** Custom defined resource package Id **/
    private int packageId
    /** Local host application directory or Jenkins build number, fetch config files from here **/
    private String targetHost
    /** Apply Host Proguard Mapping or not**/
    private boolean applyHostMapping = true
    /** Exclude dependent aar or jar **/
    private Collection<String> excludes = new HashSet<>()
    /**  host dependence file - version.txt*/
    public File hostDependenceFile


    private final Map<String, VAContext> vaContextMap = [] as HashMap

    public VAContext getVaContext(String variantName) {
        synchronized (vaContextMap) {
            VAContext vaContext = vaContextMap.get(variantName)
            if (vaContext == null) {
                vaContext = new VAContext(variantName)
                vaContextMap.put(variantName, vaContext)
            }
            return vaContext
        }
    }

    public int getPackageId() {
        return packageId
    }

    public void setPackageId(int packageId) {
        this.packageId = packageId
    }

    public String getTargetHost() {
        return targetHost
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost
    }

    public boolean getApplyHostMapping() {
        return applyHostMapping
    }

    public void setApplyHostMapping(boolean applyHostMapping) {
        this.applyHostMapping = applyHostMapping
    }

    Collection<String> getExcludes() {
        return excludes
    }

    public void setExcludes(final String...filters) {
        if (null != filters) {
            for (final String filter :filters) {
                this.excludes.add(filter)
            }
        }
    }

    public static class VAContext {

        /**  host Symbol file - Host_R.txt */
        public File hostSymbolFile

        public Collection<DependenceInfo> stripDependencies = []
        public Collection<AarDependenceInfo> retainedAarLibs = []

        /** Variant application id */
        public String packageName

        /** Package path for java classes */
        public String packagePath

        /** File of split R.java */
        public File splitRJavaFile

        public final CheckList checkList

        VAContext(String variantName) {
            checkList = new CheckList(variantName)
        }

        public File getBuildDir(VariantScope scope) {
            return new File(scope.getGlobalScope().getIntermediatesDir(),
                    "virtualapk/" + scope.getVariantConfiguration().getDirName())
        }

    }
}