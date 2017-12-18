package com.corelib.volley.toolbox;


import com.corelib.volley.FileError;
import com.corelib.volley.NetworkResponse;
import com.corelib.volley.Request;
import com.corelib.volley.Response;

/**
 * 文件下载
 *
 * 不建议直接使用本请求。应该使用FileLoader
 * 同样，本请求只适合下载较小的文件，如图片。
 *
 *
 */
public class FileRequest extends Request<String> {

    private final String mRequestUrl;
    private final Response.Listener<String> mListener;
    private FileLoader mFileLoader;
    private final VolleyConfig.CacheType mCacheType;

    public FileRequest(int method, String url, Response.Listener<String> listener,
                       Response.ErrorListener errorListener, VolleyConfig.CacheType cacheType) {
        super(method, url, errorListener);
        mRequestUrl = url;
        mListener = listener;
        mCacheType = cacheType;
    }

    public FileRequest(String url, Response.Listener<String> listener, Response.ErrorListener errorListener, VolleyConfig.CacheType cacheType) {
        this(Method.GET, url, listener, errorListener, cacheType);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        if(mListener!=null&&response!=null){
            mListener.onResponseHeadersAndData(response.headers,null,response.statusCode,response.isFroNetwork());
        }
        if (null == mFileLoader) {
            return Response.error(new FileError("FileLoader not set"));
        }

        String fileName = mFileLoader.saveFile(mRequestUrl, response.data, mCacheType);
        if (null != fileName) {
            return Response.success(fileName, HttpHeaderParser.parseCacheHeaders(response));
        } else {
            return Response.error(new FileError("Cache file error: " + mRequestUrl));
        }
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

    /* package */void setFileLoader(FileLoader fileLoader) {
        mFileLoader = fileLoader;
    }
}
