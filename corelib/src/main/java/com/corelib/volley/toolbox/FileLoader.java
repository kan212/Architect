package com.corelib.volley.toolbox;

import android.os.Handler;
import android.os.Looper;

import com.corelib.volley.Request;
import com.corelib.volley.RequestQueue;
import com.corelib.volley.Response;
import com.corelib.volley.VolleyError;
import com.corelib.volley.VolleyLog;
import com.corelib.volley.toolbox.VolleyConfig.CacheType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 模仿自ImageLoader，用于管理图片文件下载
 *
 */
public class FileLoader {
    private final String IMAGE_NEWS_ID = "id";
    private final String IMAGE_FROM = "from";

    /** 请求队列 */
    private final RequestQueue mRequestQueue;

    /** 派发响应的等待时间 */
    private int mBatchResponseDelayMs = 100;

    /** 缓存器 */
    private final Map<VolleyConfig.CacheType, FileCache> mCacheMap;

    /** 进行中的请求映射表 */
    private final HashMap<String, BatchedFileRequest> mInFlightRequests = new HashMap<String, BatchedFileRequest>();

    /** 等待派发的响应映射表 */
    private final HashMap<String, BatchedFileRequest> mBatchedResponses = new HashMap<String, BatchedFileRequest>();

    /** 派发响应Handler */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /** 派发响应的Runnable */
    private Runnable mRunnable;

    public interface FileCache {
        /** 缓存初始化 */
        public void init();

        /** 清空缓存 */
        public void clear();

        /** 查找缓存，返回文件名，或null */
        public String getFile(String key);

        /** 写缓存， 成功返回文件名，失败返回null */
        public String putFile(String key, byte[] data);

        /** 删除缓存 */
        public void remove(String key);
    }

    private class BatchedFileRequest {
        /** 真正的网络请求 */
        private final Request<?> mRequest;
        /** 请求成功的文件名 */
        private String mResponseFileName;
        /** 请求失败 */
        private VolleyError mError;
        /** 同一请求的等待列表 */
        private final LinkedList<FileContainer> mContainers = new LinkedList<FileContainer>();

        public BatchedFileRequest(Request<?> request, FileContainer container) {
            mRequest = request;
            mContainers.add(container);
        }

        public void setHeaders(HashMap headers) {
            if (mRequest != null) {
                mRequest.setHeaders(headers);
            }

        }

        public void setError(VolleyError error) {
            mError = error;
        }

        public VolleyError getError() {
            return mError;
        }

        public void addContainer(FileContainer container) {
            mContainers.add(container);
        }

        public boolean removeContainerAndCancelIfNecessary(FileContainer container) {
            mContainers.remove(container);
            if (mContainers.size() == 0) {
                mRequest.cancel();
                return true;
            }
            return false;
        }
    }

    public class FileContainer {
        /** 文件名 */
        private String mFileName;
        /** 监听 */
        private final FileListener mListener;
        /** 缓存索引键 */
        private final String mCacheKey;
        /** 请求URL */
        private final String mRequestUrl;
        private Map<String, String> mHeaders;
        private int mResponseCode;
        private long mReqStartTime;
        private long mReqEndTime;
        public FileContainer(String fileName, String requestUrl, String cacheKey,
                             FileListener listener) {
            mFileName = fileName;
            mRequestUrl = requestUrl;
            mCacheKey = cacheKey;
            mListener = listener;
        }
        public Map<String,String> getResponseHeaders(){
            return  mHeaders;
        }

        public int getResponseCode(){
            return  mResponseCode;
        }

        public long getReqStartTime() {
            return mReqStartTime;
        }

        public void seReqStartTime(long mReqStartTime) {
            this.mReqStartTime = mReqStartTime;
        }

        public long getReqEndTime() {
            return mReqEndTime;
        }

        public void setReqEndTime(long mReqEndTime) {
            this.mReqEndTime = mReqEndTime;
        }
        public void cancelRequest() {
            if (mListener == null) {
                return;
            }

            BatchedFileRequest request = mInFlightRequests.get(mCacheKey);
            if (request != null) {
                boolean canceled = request.removeContainerAndCancelIfNecessary(this);
                if (canceled) {
                    mInFlightRequests.remove(mCacheKey);
                }
            } else {
                request = mBatchedResponses.get(mCacheKey);
                if (request != null) {
                    request.removeContainerAndCancelIfNecessary(this);
                    if (request.mContainers.size() == 0) {
                        mBatchedResponses.remove(mCacheKey);
                    }
                }
            }
        }

        public String getFileName() {
            return mFileName;
        }

        public String getRequestUrl() {
            return mRequestUrl;
        }
    }

    public interface FileListener{
        public void onResponse(FileContainer response, boolean isImmediate);
        public void onErrorResponse(VolleyError error, FileContainer response);
    }

    public FileLoader(RequestQueue queue) {
        mRequestQueue = queue;
        mCacheMap = VolleyConfig.getFileCacheMap();

        Iterator<Entry<VolleyConfig.CacheType, FileCache>> iterator = mCacheMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<VolleyConfig.CacheType, FileCache> entry = iterator.next();
            entry.getValue().init();
        }
    }

    public boolean isCached(String requestUrl) {
        return isCached(VolleyConfig.CacheType.NORMAL_CACHE, requestUrl);
    }

    public boolean isCached(VolleyConfig.CacheType cacheType, String requestUrl) {
        throwIfNotOnMainThread();

        String cacheKey = VolleyUtil.uri2CacheKey(requestUrl);
        return mCacheMap.get(cacheType).getFile(cacheKey) != null;
    }

    /**
     * Get local file name
     *
     * @param requestUrl
     * @return
     *
     * @author xuegang
     * @version Created: 2014年10月1日 上午9:44:29
     */
    public String getFileFromCache(String requestUrl) {
        return getFileFromCache(VolleyConfig.CacheType.NORMAL_CACHE, requestUrl);
    }

    public String getFileFromCache(VolleyConfig.CacheType cacheType, String requestUrl) {
        String cacheKey = VolleyUtil.uri2CacheKey(requestUrl);
        return mCacheMap.get(cacheType).getFile(cacheKey);
    }

    /**
     * Get local file name, prefix with file scheme("file://")
     *
     * @param requestUrl
     * @return
     *
     * @author xuegang
     * @version Created: 2014年10月1日 上午9:44:06
     */
    public String getFileFromCachePrefixFileScheme(String requestUrl) {
        return getFileFromCachePrefixFileScheme(VolleyConfig.CacheType.NORMAL_CACHE, requestUrl);
    }

    public String getFileFromCachePrefixFileScheme(VolleyConfig.CacheType cacheType, String requestUrl) {
        String cacheKey = VolleyUtil.uri2CacheKey(requestUrl);
        String localFileName = mCacheMap.get(cacheType).getFile(cacheKey);
        if (null == localFileName) {
            return null;
        }

        return "file://" + localFileName;
    }

    protected FileContainer get(String requestUrl, Object requestTag, FileListener fileListener) {
        return get(requestUrl, requestTag, fileListener, false);
    }

    protected FileContainer get(String requestUrl, Object requestTag, FileListener fileListener,
                                boolean onlyCache) {
        return get(requestUrl, requestTag, fileListener, onlyCache, CacheType.NORMAL_CACHE, false);
    }

    protected FileContainer getForParameters(String requestUrl, Object requestTag, FileListener fileListener,
                                             boolean onlyCache, String newsId, String from) {
        return getForParameters(requestUrl, requestTag, fileListener, onlyCache, CacheType.NORMAL_CACHE, false,newsId,from);
    }


    protected FileContainer get(String requestUrl, Object requestTag, FileListener fileListener,
                                CacheType cacheType) {
        return get(requestUrl, requestTag, fileListener, false, cacheType, false);
    }

    protected FileContainer getInThread(String requestUrl, Object requestTag, FileListener fileListener, boolean onMainThread) {
        return get(requestUrl, requestTag, fileListener, false, CacheType.NORMAL_CACHE, onMainThread);
    }

    /**
     * Issues a file request with the given URL if that file is not available
     * in the cache, and returns a file container that contains all of the data
     * relating to the request.
     *
     * @param requestUrl The url of the remote file
     * @param requestTag The tag that can be used to cancel this request
     * @param fileListener The listener to call when the remote file is loaded
     * @param onlyCache True if only get file from cache
     * @param cacheType cache type: normal or uncleanable
     * @return A container object that contains all of the properties of the request, as well as
     *         the currently available file.
     *
     * @author xuegang
     * @version Created: 2014年10月22日 上午10:53:39
     */
    protected FileContainer get(String requestUrl, Object requestTag, FileListener fileListener,
                                boolean onlyCache, CacheType cacheType) {
        return get(requestUrl, requestTag, fileListener, false, CacheType.NORMAL_CACHE, false);
    }

    protected FileContainer getForParameters(String requestUrl, Object requestTag, FileListener fileListener,
                                             boolean onlyCache, CacheType cacheType, boolean checkMainThread, String newsId, String from) {
        if (!checkMainThread) {
            throwIfNotOnMainThread();
        }

        final String cacheKey = VolleyUtil.uri2CacheKey(requestUrl);

        String cachedFile = mCacheMap.get(cacheType).getFile(cacheKey);
        if (null != cachedFile) {
            FileContainer container = new FileContainer(cachedFile, requestUrl, null, null);
            fileListener.onResponse(container, true);
            return container;
        } else {
            if (onlyCache) {
                fileListener.onErrorResponse(new VolleyError("no cache while onlyCache is set"),null);
                return null;
            }
        }

        final FileContainer fileContainer = new FileContainer(cachedFile, requestUrl, cacheKey,
                fileListener);

        BatchedFileRequest request = mInFlightRequests.get(cacheKey);
        if (null != request) {
            if (request.mRequest.isCanceled()) {
                VolleyLog.e("cancel: %s", requestUrl);
                mInFlightRequests.remove(cacheKey);
            } else {

                HashMap<String, String> headers = new HashMap<>();
                headers.put(IMAGE_NEWS_ID, newsId);
                headers.put(IMAGE_FROM, from);
                request.setHeaders(headers);
                request.addContainer(fileContainer);
                return fileContainer;
            }
        }

        FileRequest newRequest = new FileRequest(requestUrl, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                onGetFileSuccess(cacheKey, response);
            }

            @Override
            public void onResponseHeadersAndData(Map<String, String> headers, String originalData, int responseCode, boolean isFromNetWork) {
                if (fileContainer != null&&isFromNetWork) {
                    fileContainer.mHeaders = headers;
                    fileContainer.mResponseCode=responseCode;
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                onGetFileError(cacheKey, error);
            }
        }, cacheType);
        if(fileContainer!=null){
            fileContainer.seReqStartTime(System.currentTimeMillis());
        }
        HashMap<String, String> headers = new HashMap<>();
        headers.put(IMAGE_NEWS_ID, newsId);
        headers.put(IMAGE_FROM, from);
        newRequest.setHeaders(headers);
        newRequest.setFileLoader(this);
        newRequest.setOnlyCache(onlyCache);
        newRequest.setTag(requestTag);
        mRequestQueue.add(newRequest);
        mInFlightRequests.put(cacheKey, new BatchedFileRequest(newRequest, fileContainer));
        return fileContainer;
    }


    protected FileContainer get(String requestUrl, Object requestTag, FileListener fileListener,
                                boolean onlyCache, CacheType cacheType, boolean checkMainThread) {
        if (!checkMainThread) {
            throwIfNotOnMainThread();
        }

        final String cacheKey = VolleyUtil.uri2CacheKey(requestUrl);

        String cachedFile = mCacheMap.get(cacheType).getFile(cacheKey);
        if (null != cachedFile) {
            FileContainer container = new FileContainer(cachedFile, requestUrl, null, null);
            fileListener.onResponse(container, true);
            return container;
        } else {
            if (onlyCache) {
                fileListener.onErrorResponse(new VolleyError("no cache while onlyCache is set"),null);
                return null;
            }
        }

        final FileContainer fileContainer = new FileContainer(cachedFile, requestUrl, cacheKey,
                fileListener);

        BatchedFileRequest request = mInFlightRequests.get(cacheKey);
        if (null != request) {
            if (request.mRequest.isCanceled()) {
                VolleyLog.e("cancel: %s", requestUrl);
                mInFlightRequests.remove(cacheKey);
            } else {
                request.addContainer(fileContainer);
                return fileContainer;
            }
        }

        FileRequest newRequest = new FileRequest(requestUrl, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                onGetFileSuccess(cacheKey, response);
            }

            @Override
            public void onResponseHeadersAndData(Map<String, String> headers, String originalData, int responseCode, boolean isFromNetWork) {
                if (fileContainer != null&&isFromNetWork) {
                    fileContainer.mHeaders = headers;
                    fileContainer.mResponseCode=responseCode;
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                onGetFileError(cacheKey, error);
            }
        }, cacheType);
        if(fileContainer!=null){
            fileContainer.seReqStartTime(System.currentTimeMillis());
        }
        newRequest.setFileLoader(this);
        newRequest.setOnlyCache(onlyCache);
        newRequest.setTag(requestTag);
        mRequestQueue.add(newRequest);
        mInFlightRequests.put(cacheKey, new BatchedFileRequest(newRequest, fileContainer));
        return fileContainer;
    }

    public void setBatchedResponseDelay(int newBatchedResponseDelayMs) {
        mBatchResponseDelayMs = newBatchedResponseDelayMs;
    }

    private void onGetFileSuccess(String cacheKey, String response) {
        BatchedFileRequest request = mInFlightRequests.remove(cacheKey);
        if (null != request) {
            request.mResponseFileName = response;
            batchResponse(cacheKey, request);
        } else {
            VolleyLog.e("no batched file request found: %s", cacheKey);
        }
    }

    private void onGetFileError(String cacheKey, VolleyError error) {
        BatchedFileRequest request = mInFlightRequests.remove(cacheKey);
        if (null != request) {
            request.setError(error);
            batchResponse(cacheKey, request);
        } else {
            VolleyLog.e("no batched file request found: %s", cacheKey);
        }
    }

    private void batchResponse(String cacheKey, BatchedFileRequest request) {
        mBatchedResponses.put(cacheKey, request);

        if (null == mRunnable) {
            mRunnable = new Runnable() {

                @Override
                public void run() {
                    for (BatchedFileRequest bfr : mBatchedResponses.values()) {
                        for (FileContainer container : bfr.mContainers) {
                            if (null == container.mListener) {
                                continue;
                            }

                            if (null == bfr.getError()) {
                                container.mFileName = bfr.mResponseFileName;
                                container.mListener.onResponse(container, false);
                            } else {
                                container.mListener.onErrorResponse(bfr.getError(),container);
                            }
                        }
                    }

                    mBatchedResponses.clear();
                    mRunnable = null;
                }
            };

            mHandler.postDelayed(mRunnable, mBatchResponseDelayMs);
        }
    }

    /* package */String saveFile(String requestUrl, byte[] data, VolleyConfig.CacheType cacheType) {
        final String cachedKey = VolleyUtil.uri2CacheKey(requestUrl);
        return mCacheMap.get(cacheType).putFile(cachedKey, data);
    }

    private void throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("FileLoader must be invoked from the main thread.");
        }
    }
}
