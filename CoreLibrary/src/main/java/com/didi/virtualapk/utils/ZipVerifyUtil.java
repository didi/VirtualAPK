/*
 * Copyright (C) 2017 Beijing Didi Infinity Technology and Development Co.,Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.didi.virtualapk.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.didi.virtualapk.internal.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by renyugang on 16/5/12.
 */

/**
 * verify signature of zip file<br>
 * usage: boolean valid = ZipVerifyUtil.verifyZip(context, zipPath)
 */
public class ZipVerifyUtil {

    public static boolean verifyZip(Context context, String zipPath) {
        return verifyZip(context, zipPath, "test.cer");
    }
    
    public static boolean verifyZip(Context context, String zipPath, String cerName) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream in = context.getAssets().open(cerName);
            Certificate certificate = certificateFactory.generateCertificate(in);
            in.close();
            return verifyZip(zipPath, certificate);
        } catch (IOException | CertificateException e) {
            Log.w(Constants.TAG, e);
            return false;
        }
    }

    public static boolean verifyZip(String zipPath, Certificate remoteCertificate) {
        try {
            String certPath = checkZipFileForCertificate(zipPath);
            Certificate certificate = getCertificateFromZip(zipPath, certPath);
            remoteCertificate.verify(certificate.getPublicKey());
            return true;
        } catch (Exception e) {
            Log.w(Constants.TAG, e);
            return false;
        }
    }

    public static Certificate getCertificateFromZip(String zipPath, String certPath) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        ZipFile zip = new ZipFile(new File(zipPath));
        InputStream in = zip.getInputStream(zip.getEntry(certPath));
        Certificate certificate = certificateFactory.generateCertificates(in).iterator().next();
        in.close();
        zip.close();
        return certificate;
    }

    public static String checkZipFileForCertificate(String zipPath) throws IOException {
        String certPath = "";
        ZipFile zip = new ZipFile(new File(zipPath));

        // This call will throw a java.lang.SecurityException if someone has tampered
        // with the signature of _any_ element of the JAR file.
        // Alas, it will proceed without a problem if the JAR file is not signed at all
        InputStream is = zip.getInputStream(zip.getEntry("META-INF/MANIFEST.MF"));
        Manifest man = new Manifest(is);
        is.close();

        Set<String> signed = new HashSet();
        for (Map.Entry<String, Attributes> entry : man.getEntries().entrySet()) {
            for (Object attrkey : entry.getValue().keySet()) {
                if (attrkey instanceof Attributes.Name
                        && ((Attributes.Name) attrkey).toString().indexOf("-Digest") != -1)
                    signed.add(entry.getKey());
            }
        }

        Set<String> entries = new HashSet<String>();
        for (Enumeration<ZipEntry> entry = (Enumeration<ZipEntry>) zip.entries(); entry.hasMoreElements();) {
            ZipEntry ze = entry.nextElement();
            if (!ze.isDirectory()) {
                String name = ze.getName();
                if (!name.startsWith("META-INF/")) {
                    entries.add(name);
                } else if (name.endsWith(".RSA") || name.endsWith(".DSA")) {
                    certPath = name;
                }
            }
        }

        // contains all entries in the Manifest that are not signed.
        // Ususally, this contains:
        // * MANIFEST.MF itself
        // * *.SF files containing the signature of MANIFEST.MF
        // * *.DSA files containing public keys of the signer

        Set<String> unsigned = new HashSet<String>(entries);
        unsigned.removeAll(signed);

        // contains all the entries with a signature that are not present in the JAR
        Set<String> missing = new HashSet<String>(signed);
        missing.removeAll(entries);
        zip.close();
        if (unsigned.isEmpty() && missing.isEmpty()) {
            return certPath;
        }
        return null;
    }

    public static Certificate getCertificate(String certificatePath) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        FileInputStream in = new FileInputStream(certificatePath);
        Certificate certificate = certificateFactory.generateCertificate(in);
        in.close();
        return certificate;
    }

    private static byte[] decode(String base64) throws Exception {
        return Base64.decode(base64.getBytes(), Base64.DEFAULT);
    }

}
