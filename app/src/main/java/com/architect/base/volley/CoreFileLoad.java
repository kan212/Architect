package com.architect.base.volley;


import com.corelib.volley.RequestQueue;
import com.corelib.volley.toolbox.FileLoader;

/**
 * Created by kan212 on 17/12/28.
 */

public class CoreFileLoad extends FileLoader {

    public CoreFileLoad(RequestQueue queue) {
        super(queue);
    }

//    @Override
//    public FileContainer get(String requestUrl, Object requestTag, FileListener fileListener, boolean onlyCache, VolleyConfig.CacheType cacheType) {
//        return super.get(UnicomFreeTrafficHelper.replaceUnicomFreeImageUrl(requestUrl), requestTag, fileListener, onlyCache, cacheType);
//    }
//
//    @Override
//    public FileContainer get(String requestUrl, Object requestTag, FileListener fileListener) {
//        return super.get(UnicomFreeTrafficHelper.replaceUnicomFreeImageUrl(requestUrl), requestTag, fileListener);
//    }
//
//    @Override
//    public FileContainer get(String requestUrl, Object requestTag, FileListener fileListener, VolleyConfig.CacheType cacheType) {
//        return super.get(UnicomFreeTrafficHelper.replaceUnicomFreeImageUrl(requestUrl), requestTag, fileListener, cacheType);
//    }
//
//    @Override
//    public FileContainer getForParameters(String requestUrl, Object requestTag, FileListener fileListener, boolean onlyCache,String newsId,String from) {
//        return super.getForParameters(requestUrl, requestTag, fileListener, onlyCache,newsId,from);
//    }
//
//    @Override
//    public FileContainer get(String requestUrl, Object requestTag, FileListener fileListener, boolean onlyCache) {
//        return super.get(UnicomFreeTrafficHelper.replaceUnicomFreeImageUrl(requestUrl), requestTag, fileListener, onlyCache);
//    }
//
//    /**
//     * 增加参数 不在主线程操作
//     * @param requestUrl
//     * @param requestTag
//     * @param fileListener
//     * @param onMainThread
//     * @return
//     */
//    @Override
//    public FileContainer getInThread(String requestUrl, Object requestTag, FileListener fileListener, boolean onMainThread) {
//        return super.getInThread(UnicomFreeTrafficHelper.replaceUnicomFreeImageUrl(requestUrl), requestTag, fileListener, onMainThread);
//    }

}

