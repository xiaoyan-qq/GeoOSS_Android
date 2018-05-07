package com.example.xiaoxiao.geooss_android;

import android.app.Application;

import com.vondear.rxtools.RxTool;

public class GeoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //初始化RxTools
        RxTool.init(this);
    }
}
