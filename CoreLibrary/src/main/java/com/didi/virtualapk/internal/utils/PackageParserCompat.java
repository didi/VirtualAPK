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

package com.didi.virtualapk.internal.utils;

import android.content.Context;
import android.content.pm.PackageParser;
import android.os.Build;

import com.didi.virtualapk.utils.Reflector;

import java.io.File;

/**
 * @author johnsonlee
 */
public final class PackageParserCompat {

    public static final PackageParser.Package parsePackage(final Context context, final File apk, final int flags) {
        try {
            if (Build.VERSION.SDK_INT >= 28
                || (Build.VERSION.SDK_INT == 27 && Build.VERSION.PREVIEW_SDK_INT != 0)) { // Android P Preview
                return PackageParserPPreview.parsePackage(context, apk, flags);
            } else if (Build.VERSION.SDK_INT >= 24) {
                return PackageParserV24.parsePackage(context, apk, flags);
            } else if (Build.VERSION.SDK_INT >= 21) {
                return PackageParserLollipop.parsePackage(context, apk, flags);
            } else {
                return PackageParserLegacy.parsePackage(context, apk, flags);
            }
    
        } catch (Throwable e) {
            throw new RuntimeException("error", e);
        }
    }

    private static final class PackageParserPPreview {

        static final PackageParser.Package parsePackage(Context context, File apk, int flags) throws Throwable {
            PackageParser parser = new PackageParser();
            PackageParser.Package pkg = parser.parsePackage(apk, flags);
            Reflector.with(parser)
                .method("collectCertificates", PackageParser.Package.class, boolean.class)
                .call(pkg, false);
            return pkg;
        }
    }

    private static final class PackageParserV24 {

        static final PackageParser.Package parsePackage(Context context, File apk, int flags) throws Throwable {
            PackageParser parser = new PackageParser();
            PackageParser.Package pkg = parser.parsePackage(apk, flags);
            Reflector.with(parser)
                .method("collectCertificates", PackageParser.Package.class, int.class)
                .call(pkg, flags);
            return pkg;
        }
    }

    private static final class PackageParserLollipop {

        static final PackageParser.Package parsePackage(final Context context, final File apk, final int flags) throws Throwable {
            PackageParser parser = new PackageParser();
            PackageParser.Package pkg = parser.parsePackage(apk, flags);
            parser.collectCertificates(pkg, flags);
            return pkg;
        }

    }

    private static final class PackageParserLegacy {

        static final PackageParser.Package parsePackage(Context context, File apk, int flags) throws Throwable {
            PackageParser parser = new PackageParser(apk.getAbsolutePath());
            PackageParser.Package pkg = parser.parsePackage(apk, apk.getAbsolutePath(), context.getResources().getDisplayMetrics(), flags);
            Reflector.with(parser)
                .method("collectCertificates", PackageParser.Package.class, int.class)
                .call(pkg, flags);
            return pkg;
        }

    }

}