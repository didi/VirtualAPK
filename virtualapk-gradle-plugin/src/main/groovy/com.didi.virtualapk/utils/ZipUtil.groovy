package com.didi.virtualapk.utils

import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


/**
 * operate on jar/zip file
 */
public class ZipUtil {

    private byte[] buffer = new byte[1024]

    private final File file

    public static ZipUtil with(final File file) {
        return new ZipUtil(file)
    }

    /**
     * unzip jar to class names
     * @param jarFile
     * @return class name set
     */
    public static Set<String> jarFileToClasses(final File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.name.endsWith("jar")) {
            return Collections.EMPTY_SET;
        }

        Set<String> entryNames = new LinkedHashSet<>();
        JarFile jar = new JarFile(jarFile)
        jar.entries().each { entry ->
            entryNames.add(entry.name)
        }

        return entryNames
    }

    private ZipUtil(final File file) {
        this.file = file
    }

    public ZipUtil deleteAll(final Set<String> deletes) {
//        deletes.each {
//            println "Deleting [${it}]..."
//        }
        ZipFile zf = new ZipFile(this.file)
        File temp = new File(this.file.parentFile, "${this.file.name}~")
        ZipOutputStream os = new ZipOutputStream(new FileOutputStream(temp))

        def entries = zf.entries()
        while (entries.hasMoreElements()) {
            ZipEntry ze = entries.nextElement()
            //print "#### [${ze.name}] "
            if (!deletes.find { it == ze.name }) {
                writeEntry(zf, os, ze)
                //println " +"
            } else {
                //println " -"
            }
        }

        zf.close()
        os.flush()
        os.close()

        this.file.delete() // delete first to avoid `renameTo' failed on Windows
        temp.renameTo(file)
        return this
    }

    private void writeEntry(ZipFile zf, ZipOutputStream os, ZipEntry ze) throws IOException {
        ZipEntry ze2 = new ZipEntry(ze.getName());
        ze2.setMethod(ze.getMethod());
        ze2.setTime(ze.getTime());
        ze2.setComment(ze.getComment());
        ze2.setExtra(ze.getExtra());
        if (ze.getMethod() == ZipEntry.STORED) {
            ze2.setSize(ze.getSize());
            ze2.setCrc(ze.getCrc());
        }
        os.putNextEntry(ze2);
        writeBytes(zf, ze, os);
    }

    private synchronized void writeBytes(ZipFile zf, ZipEntry ze, ZipOutputStream os) throws IOException {
        int n;

        InputStream is = null;
        try {
            is = zf.getInputStream(ze);
            long left = ze.getSize();

            while((left > 0) && (n = is.read(buffer, 0, buffer.length)) != -1) {
                os.write(buffer, 0, n);
                left -= n;
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
}