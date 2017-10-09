package com.didi.virtualapk.utils

/**
 * Extend << operator to copy file
 * @author zhengtao
 */
class FileBinaryCategory {


    //Write content from a url to a file
    def static leftShift(File file, URL url) {
        def conn = url.openConnection()
        conn.with {
            def is = conn.getInputStream()
            file.withOutputStream { os->
                def bs = new BufferedOutputStream(os)
                bs << is
            }
        }
    }


    //Write content from a src file to a dst file
    def static leftShift(File dst, File src) {
        src.withInputStream { is->
            dst.withOutputStream { os->
                def bs = new BufferedOutputStream(os)
                bs << is
            }
        }
    }
}

