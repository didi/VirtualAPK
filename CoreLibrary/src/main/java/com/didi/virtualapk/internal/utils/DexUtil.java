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

import android.app.ActivityThread;
import android.content.Context;
import android.os.Build;

import com.didi.virtualapk.internal.Constants;
import com.didi.virtualapk.utils.Reflector;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;

import dalvik.system.DexClassLoader;

public class DexUtil {
    private static boolean sHasInsertedNativeLibrary = false;

    public static void insertDex(DexClassLoader dexClassLoader, ClassLoader baseClassLoader, File nativeLibsDir) throws Exception {
        Object baseDexElements = getDexElements(getPathList(baseClassLoader));
        Object newDexElements = getDexElements(getPathList(dexClassLoader));
        Object allDexElements = combineArray(baseDexElements, newDexElements);
        Object pathList = getPathList(baseClassLoader);
        Reflector.with(pathList).field("dexElements").set(allDexElements);

        insertNativeLibrary(dexClassLoader, baseClassLoader, nativeLibsDir);
    }

    private static Object getDexElements(Object pathList) throws Exception {
        return Reflector.with(pathList).field("dexElements").get();
    }

    private static Object getPathList(ClassLoader baseDexClassLoader) throws Exception {
        return Reflector.with(baseDexClassLoader).field("pathList").get();
    }

    private static Object combineArray(Object firstArray, Object secondArray) {
        Class<?> localClass = firstArray.getClass().getComponentType();
        int firstArrayLength = Array.getLength(firstArray);
        int secondArrayLength = Array.getLength(secondArray);
        Object result = Array.newInstance(localClass, firstArrayLength + secondArrayLength);
        System.arraycopy(firstArray, 0, result, 0, firstArrayLength);
        System.arraycopy(secondArray, 0, result, firstArrayLength, secondArrayLength);
        return result;
    }

    private static synchronized void insertNativeLibrary(DexClassLoader dexClassLoader, ClassLoader baseClassLoader, File nativeLibsDir) throws Exception {
        if (sHasInsertedNativeLibrary) {
            return;
        }
        sHasInsertedNativeLibrary = true;

        Context context = ActivityThread.currentApplication();
        Object basePathList = getPathList(baseClassLoader);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            Reflector reflector = Reflector.with(basePathList);
            List<File> nativeLibraryDirectories = reflector.field("nativeLibraryDirectories").get();
            nativeLibraryDirectories.add(nativeLibsDir);

            Object baseNativeLibraryPathElements = reflector.field("nativeLibraryPathElements").get();
            final int baseArrayLength = Array.getLength(baseNativeLibraryPathElements);

            Object newPathList = getPathList(dexClassLoader);
            Object newNativeLibraryPathElements = reflector.get(newPathList);
            Class<?> elementClass = newNativeLibraryPathElements.getClass().getComponentType();
            Object allNativeLibraryPathElements = Array.newInstance(elementClass, baseArrayLength + 1);
            System.arraycopy(baseNativeLibraryPathElements, 0, allNativeLibraryPathElements, 0, baseArrayLength);

            Field soPathField;
            if (Build.VERSION.SDK_INT >= 26) {
                soPathField = elementClass.getDeclaredField("path");
            } else {
                soPathField = elementClass.getDeclaredField("dir");
            }
            soPathField.setAccessible(true);
            final int newArrayLength = Array.getLength(newNativeLibraryPathElements);
            for (int i = 0; i < newArrayLength; i++) {
                Object element = Array.get(newNativeLibraryPathElements, i);
                String dir = ((File)soPathField.get(element)).getAbsolutePath();
                if (dir.contains(Constants.NATIVE_DIR)) {
                    Array.set(allNativeLibraryPathElements, baseArrayLength, element);
                    break;
                }
            }

            reflector.set(allNativeLibraryPathElements);
        } else {
            Reflector reflector = Reflector.with(basePathList).field("nativeLibraryDirectories");
            File[] nativeLibraryDirectories = reflector.get();
            final int N = nativeLibraryDirectories.length;
            File[] newNativeLibraryDirectories = new File[N + 1];
            System.arraycopy(nativeLibraryDirectories, 0, newNativeLibraryDirectories, 0, N);
            newNativeLibraryDirectories[N] = nativeLibsDir;
            reflector.set(newNativeLibraryDirectories);
        }
    }

}