package com.corelib.volley.toolbox;


import com.corelib.volley.AuthFailureError;
import com.corelib.volley.NetworkResponse;
import com.corelib.volley.ParseError;
import com.corelib.volley.Request;
import com.corelib.volley.Response;
import com.corelib.volley.VolleyError;
import com.corelib.volley.VolleyLog;
import com.corelib.volley.Response.*;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class GsonRequest<T> extends Request<T> {
    private Response.Listener<T> mListener;
    private Class<T> mClazz;
    private Gson mGson;
    private Map<String, String> mPostParams;
    private Map<String, String> mResponseHeaders = null;
    private String mOriginalData = null;

    public GsonRequest(int method, String url, Class<T> clazz, Listener<T> listener,
                       ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
        mClazz = clazz;
        mGson = new Gson();
    }

    /**
     * get方式请求数据
     *
     * @param url
     *            带有请求参数的拼装好的url
     * @param clazz
     * @param listener
     * @param errorListener
     */
    public GsonRequest(String url, Class<T> clazz, Listener<T> listener, ErrorListener errorListener) {
        this(Method.GET, url, clazz, listener, errorListener);
    }

    /**
     * post方式请求数据
     *
     * @param url
     *            请求的url
     * @param params
     *            post需要的相关参数
     * @param clazz
     * @param listener
     * @param errorListener
     */
    public GsonRequest(String url, Map<String, String> params, Class<T> clazz,
                       Listener<T> listener, ErrorListener errorListener) {
        this(Method.POST, url, clazz, listener, errorListener);
        mPostParams = params;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        if (ignoreResponse()) {
            return Response.error(new ParseError(response));
        }

        String data;
        try {
            data = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            data = new String(response.data);
        }

        try {
            mResponseHeaders = response.headers;
            mOriginalData = new String(response.data);
            if (mResponseHeaders != null && response.headers.size() > 0 && mListener != null) {
                //只有图片监控需要改volley，所以其他请求默认在volley监控
                mListener.onResponseHeadersAndData(mResponseHeaders, mOriginalData,response.statusCode,false);
            }
            return Response.success(mGson.fromJson(data, mClazz),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (Exception e) {
            e.printStackTrace();
            VolleyLog.e("ParseError url: %s", getUrl());
            VolleyLog.e("ParseError json: %s", data);
            if (mClazz != null) {
                VolleyLog.e("ParseError: %s", mClazz.getSimpleName());
            }
            return Response.error(new ParseError(response));
        }
    }

    private boolean ignoreResponse() {
        return mClazz != null && mClazz.getSimpleName().equals("IgnoreResponse");
    }

    @Override
    protected void deliverResponse(T response) {
        if (ignoreResponse()) {
            return;
        }
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        if (ignoreResponse()) {
            return;
        }
        super.deliverError(error);
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        if (mPostParams != null && mPostParams.size() > 0) {
            return encodeParameters(mPostParams, getParamsEncoding());
        }
        return null;
    }

    @Override
    public byte[] getPostBody() throws AuthFailureError {
        return getBody();
    }
}
