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

        virtualApk.hostDependenceFile.splitEachLine('\\s+', { columns ->
            final def module = columns[0].split(':')
            hostDependencies.add("${module[0]}:${module[1]}")
        })

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
                stripDependencies.add(
                        new AarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))

            } else {
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
                stripDependencies.add(
                        new JarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))
            } else {
                retainedJarLib.add(
                        new JarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))
            }

        }

        File hostDir = virtualApk.getBuildDir(scope)
        FileUtil.saveFile(hostDir, "${taskName}-stripDependencies", stripDependencies)
        FileUtil.saveFile(hostDir, "${taskName}-retainedAarLibs", retainedAarLibs)
        FileUtil.saveFile(hostDir, "${taskName}-retainedJarLib", retainedJarLib)
        Log.i 'PrepareDependenciesHooker', "Analyzed all dependencis. Get more infomation in dir: ${hostDir.absoluteFile}"

        virtualApk.stripDependencies = stripDependencies
        virtualApk.retainedAarLibs = retainedAarLibs
        mark()
    }

}