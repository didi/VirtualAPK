package com.didi.virtualapk.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.ide.ArtifactDependencyGraph
import com.android.build.gradle.internal.tasks.AppPreBuildTask
import com.android.builder.model.Dependencies
import com.android.builder.model.SyncIssue
import com.didi.virtualapk.collector.dependence.AarDependenceInfo
import com.didi.virtualapk.collector.dependence.DependenceInfo
import com.didi.virtualapk.collector.dependence.JarDependenceInfo
import com.didi.virtualapk.utils.FileUtil
import com.didi.virtualapk.utils.Log
import org.gradle.api.Project

import java.util.function.Consumer

/**
 * Gather list of dependencies(aar&jar) need to be stripped&retained after the PrepareDependenciesTask finished.
 * The entire stripped operation throughout the build lifecycle is based on the result of this hooker。
 *
 * @author zhengtao
 */
class PrepareDependenciesHooker extends GradleTaskHooker<AppPreBuildTask> {

    //group:artifact:version
    def hostDependencies = [] as Set

    def retainedAarLibs = [] as Set<AarDependenceInfo>
    def retainedJarLib = [] as Set<JarDependenceInfo>
    def stripDependencies = [] as Collection<DependenceInfo>

    public PrepareDependenciesHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }

    @Override
    String getTaskName() {
        return scope.getTaskName('pre', 'Build')
    }

    /**
     * Collect host dependencies via hostDependenceFile or exclude configuration before PrepareDependenciesTask execute,
     * @param task Gradle Task fo PrepareDependenciesTask
     */
    @Override
    void beforeTaskExecute(AppPreBuildTask task) {

        hostDependencies.addAll(virtualApk.hostDependencies.keySet())

        virtualApk.excludes.each { String artifact ->
            final def module = artifact.split(':')
            hostDependencies.add("${module[0]}:${module[1]}")
        }
    }

    /**
     * Classify all dependencies into retainedAarLibs & retainedJarLib & stripDependencies
     *
     * @param task Gradle Task fo PrepareDependenciesTask
     */
    @Override
    void afterTaskExecute(AppPreBuildTask task) {
        Dependencies dependencies = new ArtifactDependencyGraph().createDependencies(scope, false, new Consumer<SyncIssue>() {
            @Override
            void accept(SyncIssue syncIssue) {
                Log.i 'PrepareDependenciesHooker', "Error: ${syncIssue}"
            }
        })

        dependencies.libraries.each {
            def mavenCoordinates = it.resolvedCoordinates
            if (hostDependencies.contains("${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}")) {
                Log.i 'PrepareDependenciesHooker', "Need strip aar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                stripDependencies.add(
                        new AarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))

            } else {
                Log.i 'PrepareDependenciesHooker', "Need retain aar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                retainedAarLibs.add(
                        new AarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))
            }

        }
        dependencies.javaLibraries.each {
            def mavenCoordinates = it.resolvedCoordinates
            if (hostDependencies.contains("${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}")) {
                Log.i 'PrepareDependenciesHooker', "Need strip jar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                stripDependencies.add(
                        new JarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))
            } else {
                Log.i 'PrepareDependenciesHooker', "Need retain jar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                retainedJarLib.add(
                        new JarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))
            }

        }

        File hostDir = vaContext.getBuildDir(scope)
        FileUtil.saveFile(hostDir, "${taskName}-stripDependencies", stripDependencies)
        FileUtil.saveFile(hostDir, "${taskName}-retainedAarLibs", retainedAarLibs)
        FileUtil.saveFile(hostDir, "${taskName}-retainedJarLib", retainedJarLib)

        checkDependencies()

        Log.i 'PrepareDependenciesHooker', "Analyzed all dependencis. Get more infomation in dir: ${hostDir.absoluteFile}"

        vaContext.stripDependencies = stripDependencies
        vaContext.retainedAarLibs = retainedAarLibs
        mark()
    }

    void checkDependencies() {
        ArrayList<DependenceInfo> allRetainedDependencies = new ArrayList<>()
        allRetainedDependencies.addAll(retainedAarLibs)
        allRetainedDependencies.addAll(retainedJarLib)

        ArrayList<String> checked = new ArrayList<>()

        allRetainedDependencies.each {
            String group = it.group
            String artifact = it.artifact
            String version = it.version

            // com.didi.virtualapk:core
            if (group == 'com.didi.virtualapk' && artifact == 'core') {
                checked.add("${group}:${artifact}:${version}")
            }

            // com.android.support:all
            if (group == 'com.android.support' || group.startsWith('com.android.support.')) {
                checked.add("${group}:${artifact}:${version}")
            }

            // com.android.databinding:all
            if (group == 'com.android.databinding' || group.startsWith('com.android.databinding.')) {
                checked.add("${group}:${artifact}:${version}")
            }
        }

        if (!checked.empty) {
            throw new Exception("The dependencies [${String.join(', ', checked)}] that will be used in the current plugin must be included in the host app first. Please add it in the host app as well.")
        }
    }
}