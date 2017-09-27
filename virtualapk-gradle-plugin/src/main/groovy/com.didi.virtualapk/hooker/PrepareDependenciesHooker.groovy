package com.didi.virtualapk.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.tasks.PrepareDependenciesTask
import com.android.builder.dependency.JarDependency
import com.didi.virtualapk.collector.dependence.AarDependenceInfo
import com.didi.virtualapk.collector.dependence.DependenceInfo
import com.didi.virtualapk.collector.dependence.JarDependenceInfo
import org.gradle.api.Project

/**
 * Gather list of dependencies(aar&jar) need to be stripped&retained after the PrepareDependenciesTask finished.
 * The entire stripped operation throughout the build lifecycle is based on the result of this hooker。
 *
 * @author zhengtao
 */
class PrepareDependenciesHooker extends GradleTaskHooker<PrepareDependenciesTask> {

    //group:artifact:version
    def hostDependencies = [] as Set

    def retainedAarLibs = [] as Set<AarDependenceInfo>
    def retainedJarLib = [] as Set<JarDependency>
    def stripDependencies = [] as Collection<DependenceInfo>

    public PrepareDependenciesHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }

    @Override
    String getTaskName() {
        return "prepare${apkVariant.name.capitalize()}Dependencies"
    }

    /**
     * Collect host dependencies via hostDependenceFile or exclude configuration before PrepareDependenciesTask execute,
     * @param task Gradle Task fo PrepareDependenciesTask
     */
    @Override
    void beforeTaskExecute(PrepareDependenciesTask task) {

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
    void afterTaskExecute(PrepareDependenciesTask task) {

        virtualApk.variantData = task.variant

        virtualApk.variantData.variantConfiguration.allLibraries.each {
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

        virtualApk.variantData.variantConfiguration.jarDependencies.each {
            def mavenCoordinates = it.resolvedCoordinates
            if (hostDependencies.contains("${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}")) {
                stripDependencies.add(
                        new JarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))
            } else {
                retainedJarLib.add(it)
            }
        }

        virtualApk.stripDependencies = stripDependencies
        virtualApk.retainedAarLibs = retainedAarLibs
    }

}