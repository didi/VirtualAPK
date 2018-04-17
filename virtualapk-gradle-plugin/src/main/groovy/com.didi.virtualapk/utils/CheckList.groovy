package com.didi.virtualapk.utils

class CheckList {
    Map<String, Boolean> mMap = new LinkedHashMap<>()

    void addCheckPoint(String variantName, String name) {
        String key = "${variantName}:${name}"
        if (mMap.containsKey(key)) {
            throw new RuntimeException("[${key}] has already exists.")
        }
        mMap.put(key, false)
//        Log.i('test', "addCheckPoint: ${key}")
    }

    void mark(String variantName, String name) {
        String key = "${variantName}:${name}"
        mMap.put(key, true)
//        Log.i('test', "mark: ${key}")
    }

    void check(String variantName) {
        boolean check = true
        Map matched = mMap.findAll {
//            Log.i 'CheckList', "all: ${it.key}: ${it.value}"
            it.key.startsWith("${variantName}:")
        }

        matched.each {
            check &= it.value
        }

        if (check) {
            Log.i 'CheckList', "All checked ok."
            return
        }

        Log.i 'CheckList', "Checked WARNING:"
        matched.each {
            Log.i 'CheckList', "${it.key}: ${it.value}"
        }
    }
}