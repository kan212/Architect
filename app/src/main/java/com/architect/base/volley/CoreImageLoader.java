package com.architect.base.volley;


import com.corelib.volley.RequestQueue;
import com.corelib.volley.toolbox.ImageLoader;

/**
 * Created by kan212 on 17/12/28.
 */

public class CoreImageLoader extends ImageLoader {
    /**
     * Constructs a new ImageLoader.
     *
     * @param queue The RequestQueue to use for making image requests.
     */
    public CoreImageLoader(RequestQueue queue) {
        super(queue);
    }

//    @Override
//    public ImageContainer get(String requestUrl, ImageListener imageListener, int maxWidth, int maxHeight, boolean onlyCache, VolleyConfig.CacheType cacheType, String newsId, String from) {
//        return super.get(UnicomFreeTrafficHelper.replaceUnicomFreeImageUrl(requestUrl), imageListener, maxWidth, maxHeight, onlyCache, cacheType,newsId,from);
//    }
//
//    @Override
//    public ImageContainer get(String requestUrl, ImageListener imageListener, int maxWidth, int maxHeight, boolean onlyCache,String newsId,String from) {
//        return super.get(UnicomFreeTrafficHelper.replaceUnicomFreeImageUrl(requestUrl), imageListener, maxWidth, maxHeight, onlyCache,newsId,from);
//    }
//
//    @Override
//    public ImageContainer get(String requestUrl, ImageListener listener, String newsId, String from) {
//        return super.get(UnicomFreeTrafficHelper.replaceUnicomFreeImageUrl(requestUrl), listener, newsId, from);
//    }
//
//    //如果有newid和from，请调用get(String requestUrl, ImageListener listener, String newsId, String from)
//    public ImageContainer get(String requestUrl, ImageListener listener) {
//        return get(UnicomFreeTrafficHelper.replaceUnicomFreeImageUrl(requestUrl), listener, null, null);
//    }
}
