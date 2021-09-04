package com.game.javatestapp;

import android.app.Application;
import com.onesignal.OneSignal;
import com.appsflyer.AppsFlyerLib;

public class ApplicationClass extends Application {
    private static final String ONESIGNAL_APP_ID = "a9ce7f48-5a3e-48eb-9569-c3d37b2cfc86";

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        // OneSignal Initialization
        OneSignal.initWithContext(this);
        OneSignal.setAppId(ONESIGNAL_APP_ID);

        AppsFlyerLib.getInstance().init("ozVunGXBhPfuW4279veML", null, this);
        AppsFlyerLib.getInstance().start(this);
    }
}
