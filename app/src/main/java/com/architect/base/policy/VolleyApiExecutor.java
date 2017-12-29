package com.architect.base.policy;

import android.text.TextUtils;

import com.architect.base.api.ApiBase;
import com.architect.base.api.ApiBase.*;
import com.architect.base.volley.VolleyHelper;
import com.corelib.volley.NetworkError;
import com.corelib.volley.NetworkResponse;
import com.corelib.volley.NoConnectionError;
import com.corelib.volley.ParseError;
import com.corelib.volley.Request;
import com.corelib.volley.Request.Method;
import com.corelib.volley.Response;
import com.corelib.volley.TimeoutError;
import com.corelib.volley.VolleyError;
import com.corelib.volley.toolbox.GsonRequest;
import com.corelib.volley.toolbox.HttpHeaderParser;
import com.corelib.volley.toolbox.StringRequest;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by kan212 on 17/12/18.
 */

public class VolleyApiExecutor implements ApiBase.IApiExecutor {

    private Request<?> req;
    //遇到DNS错误最多允许切换2次服务，1次cdn cache,1次ip
    private static final int DNS_CHANGE_SERVER_MAX_TIMES = 2;
    private static final String DNS_REQUEST_HOST = "newsapi.sina.cn";
    private static final String DNS_REQUEST_CONFIG_OPEN = "on";


    @Override
    public void execute(ApiBase api, ApiBase.IApiResultDispatcher dispatcher, boolean isMinPriority) {
        switch (api.getRequestMethod()) {
            case Method.GET:
                if (api.getFlag() == ApiType.REQ_TYPE_CHECK_UPDATE) {
                    req = createStringRequest(api, dispatcher);
                } else {
                    req = createGetRequest(api, dispatcher);
                }
                break;
            case Method.POST:
                req = createPostRequest(api, dispatcher);
                break;
            default:
                break;
        }
        if (req != null) {
            VolleyHelper.getInstance().add(api, req, isMinPriority);
            api.setReqStartTime(System.currentTimeMillis());
        }
    }

    @Override
    public void executeForReplaceUri(ApiBase api, ApiBase.IApiResultDispatcher dispatcher, boolean isMinPriority, String replaceUri) {
        if (TextUtils.isEmpty(replaceUri)) {
            execute(api, dispatcher, isMinPriority);
            return;
        }
        api.setReplaceRequestUri(replaceUri);
        Request<?> request = null;
        switch (api.getRequestMethod()) {
            case Method.GET:
                if (api.getFlag() == ApiType.REQ_TYPE_CHECK_UPDATE) {
                    request = createStringRequest(api, dispatcher);
                } else {
                    request = createGetRequest(api, dispatcher);
                }
                break;
            case Method.POST:
                request = createPostRequest(api, dispatcher);
                break;
            default:
                break;
        }
        if (request != null) {
            request.setDnsRequestHost(DNS_REQUEST_HOST);
            api.setReqStartTime(System.currentTimeMillis());
            api.addChangeServerTimes();
            VolleyHelper.getInstance().add(api, request, isMinPriority);
        }
    }

    private Request<?> createStringRequest(final ApiBase api,
                                           final ApiBase.IApiResultDispatcher dispatcher) {
        Request<?> req = new StringRequest(api.getUri(), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                api.setReqEndTime(System.currentTimeMillis());
                api.setStatusCode(ApiStatusCode.OK);
                dispatcher.onResponseOK(response, api);
            }

            @Override
            public void onResponseHeadersAndData(Map<String, String> headers, String originalData, int responseCode, boolean isFromNetwork) {
                // 将HttpCode 设置给API回掉
                api.setHttpCode(responseCode);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (isNeedChangeServerAddress(error, api)) {
//                    createDnsRequest(api, dispatcher, error);
                } else {
                    responseError(api, dispatcher, error);
                }
            }
        }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                try {
                    String jsonString = new String(response.data, "UTF-8");
                    return Response.success(jsonString,
                            HttpHeaderParser.parseCacheHeaders(response));
                } catch (UnsupportedEncodingException e) {
                    return Response.error(new ParseError(e));
                } catch (Exception je) {
                    return Response.error(new ParseError(je));
                }
            }
        };
        req.setHeaders(api.getRequestHeader());
        return req;
    }

    private Request<?> createGetRequest(final ApiBase api, final ApiBase.IApiResultDispatcher dispatcher) {
        @SuppressWarnings("unchecked")
        Request<?> req = new GsonRequest(api.getUri(), api.getResponseClass(), new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                api.setReqEndTime(System.currentTimeMillis());
                api.setStatusCode(ApiStatusCode.OK);
                dispatcher.onResponseOK(response, api);
            }

            @Override
            public void onResponseHeadersAndData(Map headers, String originalData, int responseCode, boolean isFromNetwork) {
                // 将HttpCode 设置给API回掉
                api.setHttpCode(responseCode);
                api.setOriginalData(originalData);
                if (headers != null && headers.size() > 0) {
                    @SuppressWarnings("unused")
                    Map<String, String> responseHeader = (Map<String, String>) (headers);
                    if (responseHeader != null && responseHeader.size() > 0) {
                        api.setResponseHeaders(responseHeader);
                    }
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                if (isNeedChangeServerAddress(error, api)) {
//                    createDnsRequest(api, dispatcher, error);
//                } else {
                    responseError(api, dispatcher, error);
//                }
            }
        });
        req.setHeaders(api.getRequestHeader());
        return req;
    }

    private Request<?> createPostRequest(final ApiBase api, final ApiBase.IApiResultDispatcher dispatcher) {
        @SuppressWarnings("unchecked")
        Request<?> req = new GsonRequest(api.getUri(), api.getPostParams(), api.getResponseClass(), new Response.Listener() {
            @Override
            public void onResponse(Object response) {
                api.setReqEndTime(System.currentTimeMillis());
                api.setStatusCode(ApiStatusCode.OK);
                dispatcher.onResponseOK(response, api);
            }

            @Override
            public void onResponseHeadersAndData(Map headers, String originalData, int responseCode, boolean isFromNetwork) {
                api.setHttpCode(responseCode);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                if (isNeedChangeServerAddress(error, api)) {
//                    createDnsRequest(api, dispatcher, error);
//                } else {
                    responseError(api, dispatcher, error);
//                }
            }
        });
        req.setHeaders(api.getRequestHeader());
        return req;
    }

    //发起切换服务后的新请求
//    private void createDnsRequest(ApiBase api, ApiBase.IApiResultDispatcher dispatcher, VolleyError error) {
//        String newBaseUrl = getNewServerUrl(api.getDnsConfig(), api.getBaseUrl());
//        if (SNTextUtils.isEmpty(newBaseUrl)) {
//            responseError(api, dispatcher, error);
//            return;
//        }
//        boolean isDnsConfigIP = isIPAddress(api, newBaseUrl);
//        api.setDnsErrorIpRequestUri((getDnsErrorIpRequestUri(api, newBaseUrl, isDnsConfigIP)));
//        api.setBaseUrl(newBaseUrl);
//        Request<?> request = null;
//        switch (api.getRequestMethod()) {
//            case Method.GET:
//                if (api.getFlag() == ApiType.REQ_TYPE_CHECK_UPDATE) {
//                    request = createStringRequest(api, dispatcher);
//                } else {
//                    request = createGetRequest(api, dispatcher);
//                }
//                break;
//            case Method.POST:
//                request = createPostRequest(api, dispatcher);
//                break;
//            default:
//                break;
//        }
//
//        if (request == null) {
//            responseError(api, dispatcher, error);
//            return;
//        }
//        String requestHost = isDnsConfigIP ? DNS_REQUEST_HOST : null;
//        request.setDnsRequestHost(requestHost);
//        api.setReqStartTime(System.currentTimeMillis());
//        api.addChangeServerTimes();
//        VolleyHelper.getInstance().add(api, request, false);
//    }


    /**
     * 只针对A0001，A0002用户
     * 根据已经切换server的次数，和判断是否是符合切换服务的dns异常
     *
     * @param error
     * @return
     */
    private boolean isNeedChangeServerAddress(VolleyError error, ApiBase api) {
        //请求是否超次
        if (error == null || api == null || api.getChangeServerTimes() >= DNS_CHANGE_SERVER_MAX_TIMES) {
            return false;
        }
//        //切换服务的开关是否打开
//        if (!DNS_REQUEST_CONFIG_OPEN.equals(api.getDnsConfig().getDnsSwitch())) {
//            return false;
//        }
//        //是否是sina服务请求
//        if (api.getChangeServerTimes() == 0 && !ConstantData.SINA_NEWS_HOST_V5.equals(api.getBaseUrl())) {
//            return false;
//        }
        //是否是A0001，A0002错误
        if (error instanceof TimeoutError || error instanceof NoConnectionError) {
            return true;
        }
        //是否是A0002错误
        if (error instanceof NetworkError && error.networkResponse != null) {
            if (error.networkResponse.statusCode < 400 && error.networkResponse.statusCode != 302) {
                return true;
            }
        }
        return false;
    }

        //返回 server error
    private void responseError(ApiBase api, ApiBase.IApiResultDispatcher dispatcher, VolleyError error) {
        api.setReqEndTime(System.currentTimeMillis());
        api.setStatusCode(ApiStatusCode.Timeout);
        dispatcher.onResponseError(error, api);
    }
}
