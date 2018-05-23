package com.didi.virtualapk.utils

class CheckList {
    Map<String, Boolean> mMap = new LinkedHashMap<>()
    String variantName

    CheckList(String variantName) {
        this.variantName = variantName
    }

    void addCheckPoint(String key) {
        if (mMap.containsKey(key)) {
            throw new RuntimeException("[${key}] has already exists.")
        }
        mMap.put(key, false)
//        Log.i('test', "addCheckPoint: ${key}")
    }

    void mark(String key) {
        mMap.put(key, true)
//        Log.i('test', "mark: ${key}")
    }

    void check() {
        boolean check = true
        Map matched = mMap

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