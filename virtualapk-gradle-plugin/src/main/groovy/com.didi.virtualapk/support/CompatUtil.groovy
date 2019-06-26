package com.didi.virtualapk.support;

/**
 * @author 潘志会 @ Zhihu Inc.
 * @since 2019/05/27
 */
class CompatUtil {
    static Class loadClass(String className) {
        return CompatUtil.class.classLoader.loadClass(className)
    }
}
