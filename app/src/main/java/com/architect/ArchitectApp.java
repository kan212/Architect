package com.architect;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusException;

/**
 * Created by kan212 on 17/12/28.
 */

public class ArchitectApp extends Application{

    private static Context sAppContext = null;


    @Override
    public void onCreate() {
        initGlobalConstant();
        super.onCreate();

        try {
            //初始化默认eventbus使用index
            EventBus.builder().addIndex(new MyEventBusIndex())
                    .throwSubscriberException(BuildConfig.THROW_SUBSCRIBEREXCEPTION)
                    .installDefaultEventBus();
        } catch (EventBusException e) {
        }
    }

    private void initGlobalConstant() {
        sAppContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return sAppContext;
    }

    public static int getMemoryCacheSize() {
        // Get memory class of this device, exceeding this amount will throw an OutOfMemory exception.
        final int memLimit = ActivityManager.class.cast(ArchitectApp.getAppContext()
                .getSystemService(Context.ACTIVITY_SERVICE)).getLargeMemoryClass();

        // Use 1/8th of the available memory for this memory cache.
        return 1024 * 1024 * memLimit / 8;
    }
}
