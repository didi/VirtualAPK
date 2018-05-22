package com.didi.virtualapk.utils

public final class Log {

    private Log() {

    }

    public static int i(String tag, String msg) {
        println "[INFO][${tag}] ${msg}"
        return 0
    }

}
