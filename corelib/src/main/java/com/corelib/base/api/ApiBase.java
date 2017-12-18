package com.corelib.base.api;

/**
 * Created by kan212 on 17/12/18.
 */

public class ApiBase {


    public interface IApiResultDispatcher {
        void onResponseOK(Object o, ApiBase api);

        void onResponseError(Object o, ApiBase api);
    }

    public interface IApiExecutor {
        void execute(ApiBase api, IApiResultDispatcher dispatcher, boolean isMinPriority);
        void executeForReplaceUri(ApiBase api, IApiResultDispatcher dispatcher, boolean isMinPriority, String replaceUri);
    }

}
