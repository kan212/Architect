package com.corelib.base.api;

import com.corelib.base.policy.EventBusDispatcher;
import com.corelib.base.policy.VolleyApiExecutor;

/**
 * Created by kan212 on 17/12/18.
 */

public class ApiManager {

    private static ApiManager sManager = null;
    private String replaceUri = "";

    public static ApiManager getInstance() {
        if (sManager == null) {
            synchronized (ApiManager.class) {
                sManager = new ApiManager();
            }
        }
        return sManager;
    }

    private ApiBase.IApiExecutor executor;
    private ApiBase.IApiResultDispatcher dispatcher;

    private ApiManager() {
        this.executor = new VolleyApiExecutor();
        this.dispatcher = new EventBusDispatcher();
    }


}
