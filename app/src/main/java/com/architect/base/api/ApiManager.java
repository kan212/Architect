package com.architect.base.api;

import android.text.TextUtils;

import com.architect.base.policy.EventBusDispatcher;
import com.architect.base.policy.VolleyApiExecutor;


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

    public String getReplaceUri() {
        return replaceUri;
    }

    public void setReplaceUri(String replaceUri) {
        this.replaceUri = replaceUri;
    }

    public void doApi(ApiBase api){
        doApi(api,false);
    }

    public void doApi(ApiBase api, boolean isMinPriority) {
        if(!TextUtils.isEmpty(getReplaceUri())){
            this.executor.executeForReplaceUri(api, this.dispatcher, isMinPriority, getReplaceUri());
        }else{
            this.executor.execute(api, this.dispatcher,isMinPriority);
        }
    }

}
