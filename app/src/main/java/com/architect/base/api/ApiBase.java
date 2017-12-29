package com.architect.base.api;

import com.corelib.volley.Request;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public interface ApiStatusCode {
        int Uknown = -1;
        int OK = 200;
        int Timeout = 100;
    }

    public interface ApiType {
        int REQ_TYPE_CHECK_UPDATE = 26;
        int REQ_TYPE_CHECK_UPDATE_AUTO = 27;
        int REQ_TYPE_CHECK_UPDATE_MANUAL = 28;
        int REQ_TYPE_GET_APPLIST = 24;
        int REQ_TYPE_ADD_APPLIST = 25;
        int REQ_TYPE_PRELOAD_NEWS = 29;
    }

    public ApiBase(Class<?> responseClass, String url) {
        headers = new HashMap<>();
        params = new LinkedHashMap<>();
        mPostParams = new HashMap<>();
        this.responseClass = responseClass;
        this.statusCode = ApiStatusCode.Uknown;
        this.baseUrl = url;
    }

    private int mMethod = Request.Method.GET;
    private int flag;


    public int getRequestMethod() {
        return mMethod;
    }

    public void setRequestMethod(int mMethod) {
        this.mMethod = mMethod;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }


    // API头信息
    private HashMap<String, String> headers;
    private Map<String, String> responseHeaders;// used for http log upload
    private String originalData = null;// used for http log upload
    private Class<?> responseClass;
    private LinkedHashMap<String, String> params;
    private int statusCode;
    private HashMap<String, String> mPostParams;
    private int httpCode;
    private int changeServerTimes;
    private String replaceRequestUri;
    private String baseUrl;
    private Object data;
    private Object error;

    private long mReqEndTime;
    private long mReqStartTime;


    /**
     * Add api header
     */
    public void addRequestHeader(String header, String value) {
        this.headers.put(header, value);
    }

    public Map<String, String> getRequestHeader() {
        return headers;
    }

    public String getUri() {
        return baseUrl;
    }

    public Class<?> getResponseClass() {
        return responseClass;
    }

    public void setResponseClass(Class<?> responseClass) {
        this.responseClass = responseClass;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setReqEndTime(long reqEndTime) {
        this.mReqEndTime = reqEndTime;
    }

    public void setReqStartTime(long reqStartTime) {
        this.mReqStartTime = reqStartTime;
    }

    public HashMap<String, String> getPostParams() {
        return mPostParams;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }

    public int getChangeServerTimes() {
        return changeServerTimes;
    }

    public void addChangeServerTimes(){
        this.changeServerTimes=changeServerTimes++;
    }

    public void setOriginalData(String originalData) {
        this.originalData = originalData;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    private String getReplaceRequestUri() {
        return replaceRequestUri;
    }

    public void setReplaceRequestUri(String uri) {
        this.replaceRequestUri = uri;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }

    public LinkedHashMap<String, String> getParams() {
        return params;
    }

    public void setParams(LinkedHashMap<String, String> params) {
        this.params = params;
    }
}
