package com.architect.base.util;

import android.os.Build;

import java.util.Locale;

/**
 * Created by kan212 on 17/12/28.
 */

public final class HttpUtils {

    public static final String sDeviceId = "Architect";
    public static final String sPosttKey = "Postt";

    private static String USER_AGENT = null;

    public static String getUserAgent() {
        if (null == USER_AGENT) {
            synchronized (HttpUtils.class) {
                if (null == USER_AGENT) {
                    USER_AGENT = String.format(Locale.getDefault(),
                            "%s-%s__%s",
                            Build.MANUFACTURER,
                            Build.MODEL,
                            Build.VERSION.RELEASE);
                }
            }
        }
        return USER_AGENT;
    }
}
