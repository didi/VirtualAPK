package com.didi.virtualapk.demo.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.didi.base.model.Dog;
import com.didi.virtualapk.demo.aidl.Book;

/**
 * Created by pngfi on 2017/9/26.
 */

public class MyService extends Service {


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Dog dog=intent.getParcelableExtra("dog");
        return super.onStartCommand(intent, flags, startId);
    }


}
