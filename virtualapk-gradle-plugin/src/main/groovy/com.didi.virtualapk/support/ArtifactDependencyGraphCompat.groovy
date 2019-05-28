package com.didi.virtualapk.support

import com.android.build.gradle.internal.scope.VariantScope
import com.android.builder.model.Dependencies
import com.android.builder.model.SyncIssue
import com.didi.virtualapk.os.Build
import com.didi.virtualapk.utils.Log
import com.google.common.collect.ImmutableMap
import org.gradle.api.Project

import java.util.function.Consumer

import static com.android.build.gradle.internal.publishing.AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH
import static CompatUtil.loadClass

/**
 * @author 潘志会 @ Zhihu Inc.
 * @since 2019/05/27
 */
class ArtifactDependencyGraphCompat {
    public static final String GRAPH_CLASS_NAME_3_3_X = "com.android.build.gradle.internal.ide.dependencies.ArtifactDependencyGraph"
    public static final String GRAPH_NAME_PRE_3_3_X = "com.android.build.gradle.internal.ide.ArtifactDependencyGraph"

    public static final String BUILD_MAPPING_UTILS = "com.android.build.gradle.internal.ide.dependencies.BuildMappingUtils"
    public static final String ARTIFACT_UTILS = "com.android.build.gradle.internal.ide.dependencies.ArtifactUtils"
    public static final String CLASS_MODULE_BUILDER_3_1_X = "com.android.build.gradle.internal.ide.ModelBuilder"

    static Class<?> getCompatClass() {
        if (Build.V3_3_OR_LATER) {
            return loadClass(GRAPH_CLASS_NAME_3_3_X)
        } else {
            return loadClass(GRAPH_NAME_PRE_3_3_X)
        }
    }

    static Set getAllArtifacts(Project project, applicationVariant) {
        def scope = applicationVariant.variantData.scope
        if (Build.V3_3_OR_LATER) {
            // Set<ResolvedArtifact>
            // com.android.build.gradle.internal.ide.dependencies.ResolvedArtifact，属于 AGP 的类
            loadClass(ARTIFACT_UTILS).getAllArtifacts(scope, COMPILE_CLASSPATH, null, getBuildMapping(project))
        } else if (Build.V3_1_OR_LATER) {
            // Set<ArtifactDependencyGraph.HashableResolvedArtifactResult>
            // HashableResolvedArtifactResult 为 ResolvedArtifactResult 的子类，后者属于 gradle 的类
            return getCompatClass().getAllArtifacts(scope, COMPILE_CLASSPATH, null, getBuildMapping(project))
        } else {
            // 同 V3_1_X
            return getCompatClass().getAllArtifacts(scope, COMPILE_CLASSPATH, null)
        }
    }

    static Dependencies createDependencies(Project project, VariantScope scope) {
        Consumer consumer = new Consumer<SyncIssue>() {
            @Override
            void accept(SyncIssue syncIssue) {
                Log.i 'PrepareDependenciesHooker', "Error: ${syncIssue}"
            }
        }
        Dependencies dependencies
        if (Build.V3_3_OR_LATER) {
            dependencies = getCompatClass().newInstance()
                    .createDependencies(scope, false, getBuildMapping(project), consumer)
        } else if (Build.V3_1_OR_LATER) {
            dependencies = getCompatClass().newInstance()
                    .createDependencies(scope, false, getBuildMapping(project), consumer)
        } else {
            dependencies = getCompatClass().newInstance()
                    .createDependencies(scope, false, consumer)
        }
        return dependencies
    }

    static ImmutableMap<String, String> getBuildMapping(Project project) {
        ImmutableMap<String, String> buildMapping = null
        if (Build.V3_3_OR_LATER) {
            buildMapping = loadClass(BUILD_MAPPING_UTILS).computeBuildMapping(project.gradle)
        } else if (Build.V3_1_OR_LATER) {
            buildMapping = loadClass(CLASS_MODULE_BUILDER_3_1_X).computeBuildMapping(project.gradle)
        }
        return buildMapping;
    }

}
