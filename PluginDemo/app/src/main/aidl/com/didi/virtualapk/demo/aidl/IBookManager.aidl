package com.didi.virtualapk.demo.aidl;

import com.didi.virtualapk.demo.aidl.Book;
import com.didi.virtualapk.demo.aidl.IOnNewBookArrivedListener;

interface IBookManager {
     List<Book> getBookList();
     void addBook(in Book book);
     void registerListener(IOnNewBookArrivedListener listener);
     void unregisterListener(IOnNewBookArrivedListener listener);
}