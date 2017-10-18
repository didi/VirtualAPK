package com.didi.virtualapk.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.tasks.MergeManifests
import com.android.manifmerger.ManifestProvider
import com.didi.virtualapk.collector.dependence.DependenceInfo
import groovy.xml.QName
import groovy.xml.XmlUtil
import org.gradle.api.Project

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

        def manifestDependencies = task.providers
        manifestDependencies.removeIf(new Predicate<ManifestProvider>() {
            @Override
            boolean test(ManifestProvider manifestDependency) {
                return stripAarNames.contains("${manifestDependency.name}")
            }
        })

        task.providers = manifestDependencies
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