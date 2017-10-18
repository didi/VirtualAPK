package com.didi.virtualapk.utils
/**
 * Created by qiaopu on 2017/9/4.
 */
public class FileUtil {
    
    public static void saveFile(File dir, String fileName, Closure<List<?>> action) {
        List<?> list = action.call();
        saveFile(dir, fileName, list)
    }
    
    public static void saveFile(File dir, String fileName, Collection<?> collection) {
        saveFile(dir, fileName, false, collection)
    }

    public static void saveFile(File dir, String fileName, boolean sort, Collection<?> collection) {
        dir.mkdirs()
        def file = new File(dir, "${fileName}.txt")
        ArrayList<?> list = new ArrayList<>(collection)
        if (sort) {
            Collections.sort(list)
        }
        list.add('')
        file.write(list.join('\r\n'))
    }
    
}
