package com.didi.virtualapk.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.dependency.ManifestDependencyImpl
import com.android.build.gradle.tasks.MergeManifests
import com.android.builder.dependency.ManifestDependency
import groovy.xml.QName
import groovy.xml.XmlUtil
import org.gradle.api.Project
import com.didi.virtualapk.collector.dependence.DependenceInfo

import java.util.function.Predicate

/**
 * Filter the stripped ManifestDependency in the ManifestDependency list of MergeManifests task
 *
 * @author zhengtao
 */
class MergeManifestsHooker extends GradleTaskHooker<MergeManifests> {

    public static final String ANDROID_NAMESPACE = 'http://schemas.android.com/apk/res/android'

    public MergeManifestsHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }

    @Override
    String getTaskName() {
        return "process${apkVariant.name.capitalize()}Manifest"
    }

    @Override
    void beforeTaskExecute(MergeManifests task) {

        def stripAarNames = virtualApk.stripDependencies.
                findAll {
                    it.dependenceType == DependenceInfo.DependenceType.AAR
                }.
                collect { DependenceInfo dep ->
                    "${dep.group}:${dep.artifact}:${dep.version}"
                } as Set<String>

        def manifestDependencies = task.libraries
        manifestDependencies.removeIf(new Predicate<ManifestDependencyImpl>() {
            @Override
            boolean test(ManifestDependencyImpl manifestDependency) {
                return stripAarNames.contains("${manifestDependency.name}")
            }
        })

        manifestDependencies.each {
            stripManifestDep(it, stripAarNames)
        }

        task.libraries = manifestDependencies
    }

    /**
     * Recursively removes the manifests need be stripped(already exist in host apk) in transitive manifest dependencies
     * @param manifest The target ManifestDependency who's transitive dependencies will be filtered
     * @param stripAarNames Set of aar names need stripped
     */
    void stripManifestDep(ManifestDependency manifest, Set stripAarNames) {
        if (manifest == null) return

        manifest.manifestDependencies.removeIf(new Predicate<ManifestDependency>() {
            @Override
            boolean test(ManifestDependency manifestDependency) {
                stripAarNames.contains("${manifestDependency.name}")
            }
        })
        manifest.manifestDependencies.each {
            stripManifestDep(it, stripAarNames)
        }
    }

    /**
     * Filter specific attributes from <application /> element after MergeManifests task executed
     */
    @Override
    void afterTaskExecute(MergeManifests task) {
        final File xml = task.manifestOutputFile
        if (xml?.exists()) {
            final Node manifest = new XmlParser().parse(xml)


            manifest.application.each { application ->
                [ 'icon', 'label', 'allowBackup', 'supportsRtl' ].each {
                    application.attributes().remove(new QName(MergeManifestsHooker.ANDROID_NAMESPACE, it))
                }
            }

            xml.withPrintWriter('utf-8', { pw ->
                XmlUtil.serialize(manifest, pw)
            })
        }
    }
}