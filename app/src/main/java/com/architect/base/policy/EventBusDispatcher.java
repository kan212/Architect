package com.architect.base.policy;


import com.architect.base.api.ApiBase;
import com.architect.base.api.ApiBase.ApiStatusCode;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by kan212 on 17/12/18.
 */

public class EventBusDispatcher implements ApiBase.IApiResultDispatcher {

    @Override
    public void onResponseOK(Object o, ApiBase api) {
        if (api.getStatusCode() == ApiStatusCode.OK) {
            api.setData(o);
            EventBus.getDefault().post(api);
        }
    }

    @Override
    public void onResponseError(Object o, ApiBase api) {
        if (api.getStatusCode() != ApiStatusCode.OK) {
            api.setError(o);
            EventBus.getDefault().post(api);
        }
    }
}
