package com.didi.virtualapk

import com.android.build.gradle.internal.scope.VariantScope
import com.didi.virtualapk.collector.dependence.AarDependenceInfo
import com.didi.virtualapk.collector.dependence.DependenceInfo
import com.didi.virtualapk.utils.CheckList
import com.didi.virtualapk.utils.Log

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
    private boolean forceUseHostDependences
    private ArrayList<String> warningList = new ArrayList<>()
    /**  host dependence file - version.txt*/
    public File hostDependenceFile
    //group:artifact -> group:artifact:version
    private Map hostDependencies

    private HashSet<String> flagTable = new HashSet<>()

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

    public boolean getForceUseHostDependences() {
        return forceUseHostDependences
    }

    public void setForceUseHostDependences(boolean forceUseHostDependences) {
        this.forceUseHostDependences = forceUseHostDependences
    }

    public Map getHostDependencies() {
        if (hostDependencies == null) {
            hostDependencies = [] as LinkedHashMap
            hostDependenceFile.splitEachLine('\\s+', { columns ->
                String id = columns[0]
                int index1 = id.indexOf(':')
                int index2 = id.lastIndexOf(':')
                def module = [group: 'unspecified', name: 'unspecified', version: 'unspecified']

                if (index1 < 0 || index2 < 0 || index1 == index2) {
                    Log.e('Dependencies', "Parsed error: [${id}] -> ${module}")
                    return
                }

                if (index1 > 0) {
                    module.group = id.substring(0, index1)
                }
                if (index2 - index1 > 0) {
                    module.name = id.substring(index1 + 1, index2)
                }
                if (id.length() - index2 > 1) {
                    module.version = id.substring(index2 + 1)
                }

                hostDependencies.put("${module.group}:${module.name}", module)
            })
        }
        return hostDependencies
    }

    public void addWarning(String detail) {
        warningList.add(detail)
    }

    public void printWarning(String tag) {
        warningList.each {
            Log.i(tag, it)
        }
    }

    public void setFlag(String key, boolean value) {
        if (value) {
            flagTable.add(key)
        } else {
            flagTable.remove(key)
        }
    }

    public boolean getFlag(String key) {
        return flagTable.contains(key)
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