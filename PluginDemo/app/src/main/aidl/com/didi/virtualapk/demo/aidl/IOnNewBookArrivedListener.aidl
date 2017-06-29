package com.didi.virtualapk.demo.aidl;

import com.didi.virtualapk.demo.aidl.Book;

interface IOnNewBookArrivedListener {
    void onNewBookArrived(in Book newBook);
}
