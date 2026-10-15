package com.electrostore.app;

import android.app.Application;


public class ImadApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        StockAlerts.createChannel(this);
    }
}
