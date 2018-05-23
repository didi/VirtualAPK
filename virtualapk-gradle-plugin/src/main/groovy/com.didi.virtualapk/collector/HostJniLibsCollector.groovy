package com.didi.virtualapk.collector

import com.android.SdkConstants
import com.android.annotations.NonNull
import com.android.build.api.transform.QualifiedContent
import com.didi.virtualapk.collector.dependence.AarDependenceInfo
import com.didi.virtualapk.collector.dependence.DependenceInfo
import com.didi.virtualapk.collector.dependence.JarDependenceInfo
import com.didi.virtualapk.utils.PackagingUtils
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap

import java.util.jar.JarFile
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static com.android.SdkConstants.FD_APK_NATIVE_LIBS

/**
 * Native lib file(.so) collector
 *
 * @author zhengtao
 */
class HostJniLibsCollector {

    private final Pattern jarAbiPattern = Pattern.compile("lib/([^/]+)/[^/]+")
    private final Pattern folderAbiPattern = Pattern.compile("([^/]+)/[^/]+")
    private final Pattern filenamePattern = Pattern.compile(".*\\.so")

    /**
     * Map of .so files need to be stripped, any .so file may exist in multiple folders(aar) or jars
     */
    ListMultimap<String, DependenceInfo> jniFileList = ArrayListMultimap.create();

    /**
     * Collect all the .so files in the excluded dependencies
     * @param stripDependencies  DependencyInfos that exists in the host apk, including AAR and JAR
     * @return .so files need to be stripped
     */
    def collect(Collection<DependenceInfo> stripDependencies) {
        stripDependencies.each {
            if (it instanceof AarDependenceInfo) {
                gatherListFromFolder(it as AarDependenceInfo, jniFileList)
            } else {
                gatherListFromJar(it as JarDependenceInfo, jniFileList)
            }
        }
        jniFileList.keySet()
    }


    private void gatherListFromJar(
            @NonNull JarDependenceInfo jarDependence,
            @NonNull ListMultimap<String, DependenceInfo> content) throws IOException {

        ZipFile zipFile = new ZipFile(jarDependence.jarFile);
        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();

                String path = entry.getName();
                if (skipEntry(entry, path)) {
                    continue;
                }

                content.put(path, jarDependence);
            }

        } finally {
            zipFile.close();
        }
    }


    private void gatherListFromFolder(
            @NonNull AarDependenceInfo aarDependence,
            @NonNull ListMultimap<String, DependenceInfo> content) {
        gatherListFromFolder(aarDependence.jniFolder, "", aarDependence, content);
    }



    private boolean skipEntry(
            @NonNull ZipEntry entry,
            @NonNull String path) {
        if (entry.directory || JarFile.MANIFEST_NAME == path ||
                !validateJarPath(path)) {
            return true;
        }

        // split the path into segments.
        String[] segments = path.split("/");

        // empty path? skip to next entry.
        if (segments.length == 0) {
            return true;
        }

        // Check each folders to make sure they should be included.
        // Folders like CVS, .svn, etc.. should already have been excluded from the
        // jar file, but we need to exclude some other folder (like /META-INF) so
        // we check anyway.
        for (int i = 0 ; i < segments.length - 1; i++) {
            if (!PackagingUtils.checkFolderForPackaging(segments[i])) {
                return true;
            }
        }

        return !PackagingUtils.checkFileForPackaging(segments[segments.length-1],
                false /*allowClassFiles*/);
    }




    private void gatherListFromFolder(
            @NonNull File file,
            @NonNull String path,
            @NonNull AarDependenceInfo aarDependenceInfo,
            @NonNull ListMultimap<String, DependenceInfo> content) {
        File[] children = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File f, String name) {

                return f.isDirectory() || !name.endsWith(SdkConstants.DOT_CLASS);
            }
        });

        if (children != null) {
            for (File child : children) {
                String newPath = path.isEmpty() ? child.getName() : path + '/' + child.getName();
                if (child.isDirectory()) {
                    gatherListFromFolder(
                            child,
                            newPath,
                            aarDependenceInfo,
                            content);
                } else if (child.isFile() && validateFolderPath(newPath)) {
                    content.put(folderPathToKey(newPath), aarDependenceInfo);
                }
            }
        }
    }


    public boolean validateJarPath(@NonNull String path) {
        // extract abi from path, checking the general path structure (lib/<abi>/<filename>)
        Matcher m = jarAbiPattern.matcher(path);

        // if the ABI is accepted, check the 3rd segment
        if (m.matches()) {
            // remove the beginning of the path (lib/<abi>/)
            String filename = path.substring(5 + m.group(1).length());
            // and check the filename
            return filenamePattern.matcher(filename).matches() ||
                    SdkConstants.FN_GDBSERVER == filename ||
                    SdkConstants.FN_GDB_SETUP == filename;
        }

        return false;
    }


    public boolean validateFolderPath(@NonNull String path) {
        // extract abi from path, checking the general path structure (<abi>/<filename>)
        Matcher m = folderAbiPattern.matcher(path);

        // if the ABI is accepted, check the 3rd segment
        if (m.matches()) {
            // remove the beginning of the path (<abi>/)
            String filename = path.substring(1 + m.group(1).length());
            // and check the filename
            return filenamePattern.matcher(filename).matches() ||
                    SdkConstants.FN_GDBSERVER == filename ||
                    SdkConstants.FN_GDB_SETUP == filename;
        }

        return false;
    }

    @NonNull
    public String folderPathToKey(@NonNull String path) {
        return FD_APK_NATIVE_LIBS + "/" + path;
    }

    @NonNull
    public String keyToFolderPath(@NonNull String path) {
        return path.substring(FD_APK_NATIVE_LIBS.length() + 1);
    }
}