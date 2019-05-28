package com.didi.virtualapk.support


import com.didi.virtualapk.os.Build

/**
 * @author 潘志会 @ Zhihu Inc.
 * @since 2019/05/27
 */
class ResolvedArtifactCompat {
    static getComponentIdentifier(artifact) {
        def componentIdentifier = null
        if (Build.isSupportVersion(Build.VERSION_CODE.V3_3_X)) {
            componentIdentifier = artifact.componentIdentifier
        } else {
            componentIdentifier = artifact.id.componentIdentifier
        }
        return componentIdentifier
    }

    static getFile(artifact) {
        File file = null
        if (Build.isSupportVersion(Build.VERSION_CODE.V3_3_X)) {
            file = artifact.artifactFile
        } else {
            file = artifact.file
        }
        return file
    }

    static getVariantName(artifact) {
        // artifact 在 3.3 以上是 com.android.build.gradle.internal.ide.dependencies.ResolvedArtifact
        // artifact 在 3.3 以下是 org.gradle.api.artifacts.result.ResolvedArtifactResult
        String variantName = null
        if (Build.isSupportVersion(Build.VERSION_CODE.V3_3_X)) {
            variantName =  artifact.variantName
        } else {
            variantName = CompatUtil.loadClass(ArtifactDependencyGraphCompat.GRAPH_NAME_PRE_3_3_X).getVariant(artifact)

        }
        return variantName
    }
}
