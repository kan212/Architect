/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corelib.volley.toolbox;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;

import com.corelib.volley.Request;
import com.corelib.volley.RequestQueue;
import com.corelib.volley.Response.ErrorListener;
import com.corelib.volley.Response.Listener;
import com.corelib.volley.VolleyError;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Helper that handles loading and caching images from remote URLs.
 *
 * The simple way to use this class is to call {@link ImageLoader#get(String, ImageListener, String, String)} )} and to
 * pass in the default image listener provided by
 * {@link ImageLoader#getImageListener(ImageView, int, int)}. Note that all function calls to
 * this class must be made from the main thead, and all responses will be delivered to the main
 * thread as well.
 */
public class ImageLoader {
    private final String IMAGE_NEWS_ID = "id";
    private final String IMAGE_FROM = "from";
    /** RequestQueue for dispatching ImageRequests onto. */
    private final RequestQueue mRequestQueue;

    /** Amount of time to wait after first response arrives before delivering all responses. */
    private int mBatchResponseDelayMs = 100;

    /** The cache implementation to be used as an L1 cache before calling into volley. */
    private final ImageCache mCache;

    /**
     * HashMap of Cache keys -> BatchedImageRequest used to track in-flight requests so
     * that we can coalesce multiple requests to the same URL into a single network request.
     */
    private final HashMap<String, BatchedImageRequest> mInFlightRequests = new HashMap<String, BatchedImageRequest>();

    /** HashMap of the currently pending responses (waiting to be delivered). */
    private final HashMap<String, BatchedImageRequest> mBatchedResponses = new HashMap<String, BatchedImageRequest>();

    /** Handler to the main thread. */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /** Runnable for in-flight response delivery. */
    private Runnable mRunnable;

    /**
     * Simple cache adapter interface. If provided to the ImageLoader, it
     * will be used as an L1 cache before dispatch to Volley. Implementations
     * must not block. Implementation with an LruCache is recommended.
     */
    public interface ImageCache {
        public Bitmap getBitmap(String url);

        public void putBitmap(String url, Bitmap bitmap);
    }

    /**
     * Constructs a new ImageLoader.
     *
     * @param queue The RequestQueue to use for making image requests.
     */
    public ImageLoader(RequestQueue queue) {
        mRequestQueue = queue;
        mCache = VolleyConfig.getImageCache();
    }

    /**
     * The default implementation of ImageListener which handles basic functionality
     * of showing a default image until the network response is received, at which point
     * it will switch to either the actual image or the error image.
     *
     * @param view The imageView that the listener is associated with.
     * @param defaultImageResId Default image resource ID to use, or 0 if it doesn't exist.
     * @param errorImageResId Error image resource ID to use, or 0 if it doesn't exist.
     */
    public static ImageListener getImageListener(final ImageView view, final int defaultImageResId,
                                                 final int errorImageResId) {
        return new ImageListener() {
            @Override
            public void onResponse(ImageContainer response, boolean isImmediate) {
                if (response.getBitmap() != null) {
                    view.setImageBitmap(response.getBitmap());
                } else if (defaultImageResId != 0) {
                    view.setImageResource(defaultImageResId);
                }
            }

            @Override
            public void onErrorResponse(VolleyError error, ImageContainer response, boolean isImmediate) {
                if (errorImageResId != 0) {
                    view.setImageResource(errorImageResId);
                }
            }
        };
    }

    /**
     * Interface for the response handlers on image requests.
     *
     * The call flow is this:
     * 1. Upon being attached to a request, onResponse(response, true) will
     * be invoked to reflect any cached data that was already available. If the
     * data was available, response.getBitmap() will be non-null.
     *
     * 2. After a network response returns, only one of the following cases will happen:
     * - onResponse(response, false) will be called if the image was loaded.
     * or
     * - onErrorResponse will be called if there was an error loading the image.
     */
    public interface ImageListener {
//        extends ErrorListener
        /**
         * Listens for non-error changes to the loading of the image request.
         *
         * @param response Holds all information pertaining to the request, as well
         *            as the bitmap (if it is loaded).
         * @param isImmediate True if this was called during ImageLoader.get() variants.
         *            This can be used to differentiate between a cached image loading and a network
         *            image loading in order to, for example, run an animation to fade in network
         *            loaded
         *            images.
         */
        public void onResponse(ImageContainer response, boolean isImmediate);
        public void onErrorResponse(VolleyError error, ImageContainer response, boolean isImmediate);
    }

    /**
     * Checks if the item is available in the cache.
     *
     * @param requestUrl The url of the remote image
     * @return True if the item exists in cache, false otherwise.
     */
    public boolean isCached(String requestUrl) {
        throwIfNotOnMainThread();

        String cacheKey = VolleyUtil.uri2CacheKey(requestUrl);
        return mCache.getBitmap(cacheKey) != null;
    }

    /**
     * Returns an ImageContainer for the requested URL.
     *
     * The ImageContainer will contain either the specified default bitmap or the loaded bitmap.
     * If the default was returned, the {@link ImageLoader} will be invoked when the
     * request is fulfilled.
     *
     * @param requestUrl The URL of the image to be loaded.
     * @param defaultImage Optional default image to return until the actual image is loaded.
     */
    protected ImageContainer get(String requestUrl, final ImageListener listener, String newsId, String from) {
        return get(requestUrl, listener, 0, 0, false,newsId,from);
    }

    protected ImageContainer get(String requestUrl, ImageListener imageListener, int maxWidth,
                                 int maxHeight, boolean onlyCache, String newsId, String from) {
        return get(requestUrl, imageListener, maxWidth, maxHeight, onlyCache,
                VolleyConfig.CacheType.NORMAL_CACHE,newsId,from);
    }


    /**
     * Issues a bitmap request with the given URL if that image is not available
     * in the cache, and returns a bitmap container that contains all of the data
     * relating to the request (as well as the default image if the requested
     * image is not available).
     *
     * @param requestUrl The url of the remote image
     * @param imageListener The listener to call when the remote image is loaded
     * @param maxWidth The maximum width of the returned image.
     * @param maxHeight The maximum height of the returned image.
     * @param onlyCache Only get image from cache
     * @param cacheType cache type
     * @return A container object that contains all of the properties of the request, as well as
     *         the currently available image (default if remote is not loaded).
     */
    protected ImageContainer get(final String requestUrl, final ImageListener imageListener, final int maxWidth,
                                 final  int maxHeight, final boolean onlyCache, final VolleyConfig.CacheType cacheType, final String newsId, final String from) {
        // only fulfill requests that were initiated from the main thread.
        throwIfNotOnMainThread();

        final String cacheKey = requestUrl;

        // Try to look up the request in the cache of remote images.
        Bitmap cachedBitmap = mCache.getBitmap(cacheKey);
        if (cachedBitmap != null) {
            // Return the cached bitmap.
            ImageContainer container = new ImageContainer(cachedBitmap, requestUrl, null, null);
            imageListener.onResponse(container, true);
            return container;
        }

        // The bitmap did not exist in the cache, fetch it!
        final ImageContainer imageContainer = new ImageContainer(null, requestUrl, cacheKey,
                imageListener);

        // Update the caller to let them know that they should use the default bitmap.
        // imageListener.onResponse(imageContainer, true);

        // Check to see if a request is already in-flight.
        BatchedImageRequest request = mInFlightRequests.get(cacheKey);
        if (request != null) {
            HashMap<String, String> headers = new HashMap<>();
            headers.put(IMAGE_NEWS_ID, newsId);
            headers.put(IMAGE_FROM, from);
            request.setHeaders(headers);
            // If it is, add this request to the list of listeners.
            request.addContainer(imageContainer);
            return imageContainer;
        }

        // The request is not already in flight. Send the new request to the network and
        // track it.
        final Request<?> newRequest = new ImageRequest(requestUrl, new Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                onGetImageSuccess(cacheKey, response);
            }

            @Override
            public void onResponseHeadersAndData(Map<String, String> headers, String originalData, int responseCode, boolean isFromNetwork) {
                if (imageContainer != null&&isFromNetwork) {
                    imageContainer.setIsLoadNetRes(isFromNetwork);
                    imageContainer.mResponseCode=responseCode;
                }
            }
        }, maxWidth, maxHeight, Config.ARGB_8888, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (isServerError(error)) {
                    addNewImageRequest2Queue(requestUrl, imageListener, maxWidth, maxHeight, onlyCache, cacheType, imageContainer, error,newsId,from);
                } else {
                    onGetImageError(cacheKey, error);
                }

            }
        }, cacheType);
        newRequest.setOnlyCache(onlyCache);
        HashMap<String, String> headers = new HashMap<>();
        headers.put(IMAGE_NEWS_ID, newsId);
        headers.put(IMAGE_FROM, from);
        newRequest.setHeaders(headers);
        mRequestQueue.add(newRequest);
        if(imageContainer!=null){
            imageContainer.seReqStartTime(System.currentTimeMillis());
        }
        mInFlightRequests.put(cacheKey, new BatchedImageRequest(newRequest, imageContainer));
        return imageContainer;
    }


    private boolean isServerError(VolleyError error) {

        if (error == null || error.networkResponse == null) {
            return false;

        }
        return error.networkResponse.statusCode > 400;

    }


    private void addNewImageRequest2Queue(String requestUrl, final ImageListener imageListener, int maxWidth,
                                          int maxHeight, boolean onlyCache, final VolleyConfig.CacheType cacheType, final ImageContainer
                                                  imageContainer, VolleyError error, String newsId, String from) {
        requestUrl = restoreOriginalPicUrl(requestUrl);
        Request<?> newRequest = createImageRequest(requestUrl, imageListener, maxWidth,
                maxHeight, onlyCache, cacheType, imageContainer);
        HashMap<String, String> headers = new HashMap<>();
        headers.put(IMAGE_NEWS_ID, newsId);
        headers.put(IMAGE_FROM, from);
        newRequest.setHeaders(headers);
        final String cacheKey = requestUrl;
        mInFlightRequests.remove(cacheKey);
        if (newRequest != null && mRequestQueue != null && mInFlightRequests != null) {
            newRequest.setOnlyCache(onlyCache);
            mRequestQueue.add(newRequest);
            if (imageContainer != null) {
                imageContainer.seReqStartTime(System.currentTimeMillis());
            }
            mInFlightRequests.put(cacheKey, new BatchedImageRequest(newRequest, imageContainer));
        } else {
            onGetImageError(cacheKey, error);
        }
    }

    private Request<?> createImageRequest(final String requestUrl, final ImageListener imageListener, int maxWidth,
                                          int maxHeight, boolean onlyCache, final VolleyConfig.CacheType cacheType, final ImageContainer
                                                  imageContainer) {
        final String cacheKey = requestUrl;
        final Request<?> newRequest = new ImageRequest(requestUrl, new Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                onGetImageSuccess(cacheKey, response);
            }

            @Override
            public void onResponseHeadersAndData(Map<String, String> headers, String originalData, int responseCode, boolean isFromNetwork) {
                if (imageContainer != null && isFromNetwork) {
                    imageContainer.setIsLoadNetRes(isFromNetwork);
                    imageContainer.mResponseCode = responseCode;
                }
            }
        }, maxWidth, maxHeight, Config.ARGB_8888, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onGetImageError(cacheKey, error);
            }
        }, cacheType);
        return newRequest;
    }

    /**
     * 还原免流量图片url(重试)
     * @param unicomFreeUrl
     * @return
     */
    private String restoreOriginalPicUrl(String unicomFreeUrl){
        if(TextUtils.isEmpty(unicomFreeUrl)){
            return unicomFreeUrl;
        }
        String originalUrl = "";
        String unicomFreeHost = "http://free.grid.sinaedge.com/";
        if(unicomFreeUrl.startsWith(unicomFreeHost)){
            originalUrl = unicomFreeUrl.substring(unicomFreeHost.length());
            if(TextUtils.isEmpty(originalUrl)){
                return unicomFreeUrl;
            }
//            Log.d("restoreOriginalPicUrl(restore): ", "http://" + originalUrl);
            return "http://" + originalUrl;
        }
        return unicomFreeUrl;
    }

    /**
     * Sets the amount of time to wait after the first response arrives before delivering all
     * responses. Batching can be disabled entirely by passing in 0.
     *
     * @param newBatchedResponseDelayMs The time in milliseconds to wait.
     */
    public void setBatchedResponseDelay(int newBatchedResponseDelayMs) {
        mBatchResponseDelayMs = newBatchedResponseDelayMs;
    }

    /**
     * Handler for when an image was successfully loaded.
     *
     * @param cacheKey The cache key that is associated with the image request.
     * @param response The bitmap that was returned from the network.
     */
    private void onGetImageSuccess(String cacheKey, Bitmap response) {
        // cache the image that was fetched.
        mCache.putBitmap(cacheKey, response);

        // remove the request from the list of in-flight requests.
        BatchedImageRequest request = mInFlightRequests.remove(cacheKey);

        if (request != null) {
            // Update the response bitmap.
            request.mResponseBitmap = response;

            // Send the batched response
            batchResponse(cacheKey, request);
        }
    }

    /**
     * Handler for when an image failed to load.
     *
     * @param cacheKey The cache key that is associated with the image request.
     */
    private void onGetImageError(String cacheKey, VolleyError error) {
        // Notify the requesters that something failed via a null result.
        // Remove this request from the list of in-flight requests.
        BatchedImageRequest request = mInFlightRequests.remove(cacheKey);

        if (request != null) {
            // Set the error for this request
            request.setError(error);

            // Send the batched response
            batchResponse(cacheKey, request);
        }
    }

    /**
     * Container object for all of the data surrounding an image request.
     */
    public class ImageContainer {
        /**
         * The most relevant bitmap for the container. If the image was in cache, the
         * Holder to use for the final bitmap (the one that pairs to the requested URL).
         */
        private Bitmap mBitmap;

        private final ImageListener mListener;

        /** The cache key that was associated with the request */
        private final String mCacheKey;

        /** The request URL that was specified */
        private final String mRequestUrl;

        private int mResponseCode;
        private long mReqStartTime;
        private long mReqEndTime;
        private boolean isLoadNetRes;

        public boolean isLoadNetRes() {
            return isLoadNetRes;
        }

        public void setIsLoadNetRes(boolean isLoadNetRes) {
            this.isLoadNetRes = isLoadNetRes;
        }

        /**
         * Constructs a BitmapContainer object.
         *
         * @param bitmap The final bitmap (if it exists).
         * @param requestUrl The requested URL for this container.
         * @param cacheKey The cache key that identifies the requested URL for this container.
         */
        public ImageContainer(Bitmap bitmap, String requestUrl, String cacheKey,
                              ImageListener listener) {
            mBitmap = bitmap;
            mRequestUrl = requestUrl;
            mCacheKey = cacheKey;
            mListener = listener;
        }

        /**
         * Releases interest in the in-flight request (and cancels it if no one else is listening).
         */
        public void cancelRequest() {
            if (mListener == null) {
                return;
            }

            BatchedImageRequest request = mInFlightRequests.get(mCacheKey);
            if (request != null) {
                boolean canceled = request.removeContainerAndCancelIfNecessary(this);
                if (canceled) {
                    mInFlightRequests.remove(mCacheKey);
                }
            } else {
                // check to see if it is already batched for delivery.
                request = mBatchedResponses.get(mCacheKey);
                if (request != null) {
                    request.removeContainerAndCancelIfNecessary(this);
                    if (request.mContainers.size() == 0) {
                        mBatchedResponses.remove(mCacheKey);
                    }
                }
            }
        }

        /**
         * Returns the bitmap associated with the request URL if it has been loaded, null otherwise.
         */
        public Bitmap getBitmap() {
            return mBitmap;
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

        /**
         * Returns the requested URL for this container.
         */
        public String getRequestUrl() {
            return mRequestUrl;
        }
    }

    /**
     * Wrapper class used to map a Request to the set of active ImageContainer objects that are
     * interested in its results.
     */
    private class BatchedImageRequest {
        /** The request being tracked */
        private final Request<?> mRequest;

        /** The result of the request being tracked by this item */
        private Bitmap mResponseBitmap;

        /** Error if one occurred for this response */
        private VolleyError mError;

        /** List of all of the active ImageContainers that are interested in the request */
        private final LinkedList<ImageContainer> mContainers = new LinkedList<ImageContainer>();

        private void setHeaders(HashMap parameters) {
            if (mRequest != null) {
                mRequest.setHeaders(parameters);
            }
        }


        /**
         * Constructs a new BatchedImageRequest object
         *
         * @param request The request being tracked
         * @param container The ImageContainer of the person who initiated the request.
         */
        public BatchedImageRequest(Request<?> request, ImageContainer container) {
            mRequest = request;
            mContainers.add(container);
        }

        /**
         * Set the error for this response
         */
        public void setError(VolleyError error) {
            mError = error;
        }

        /**
         * Get the error for this response
         */
        public VolleyError getError() {
            return mError;
        }

        /**
         * Adds another ImageContainer to the list of those interested in the results of
         * the request.
         */
        public void addContainer(ImageContainer container) {
            mContainers.add(container);
        }

        /**
         * Detatches the bitmap container from the request and cancels the request if no one is
         * left listening.
         *
         * @param container The container to remove from the list
         * @return True if the request was canceled, false otherwise.
         */
        public boolean removeContainerAndCancelIfNecessary(ImageContainer container) {
            mContainers.remove(container);
            if (mContainers.size() == 0) {
                mRequest.cancel();
                return true;
            }
            return false;
        }
    }

    /**
     * Starts the runnable for batched delivery of responses if it is not already started.
     *
     * @param cacheKey The cacheKey of the response being delivered.
     * @param request The BatchedImageRequest to be delivered.
     * @param error The volley error associated with the request (if applicable).
     */
    private void batchResponse(String cacheKey, BatchedImageRequest request) {
        mBatchedResponses.put(cacheKey, request);
        // If we don't already have a batch delivery runnable in flight, make a new one.
        // Note that this will be used to deliver responses to all callers in mBatchedResponses.
        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    for (BatchedImageRequest bir : mBatchedResponses.values()) {
                        for (ImageContainer container : bir.mContainers) {
                            // If one of the callers in the batched request canceled the request
                            // after the response was received but before it was delivered,
                            // skip them.
                            if (container.mListener == null) {
                                continue;
                            }
                            if (bir.getError() == null) {
                                container.mBitmap = bir.mResponseBitmap;
                                container.mListener.onResponse(container, false);
                            } else {
                                container.mListener.onErrorResponse(bir.getError(),container, false);
                            }
                        }
                    }
                    mBatchedResponses.clear();
                    mRunnable = null;
                }

            };
            // Post the runnable.
            mHandler.postDelayed(mRunnable, mBatchResponseDelayMs);
        }
    }

    private void throwIfNotOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("ImageLoader must be invoked from the main thread.");
        }
    }

    /**
     * Get Bitmap directly from cache
     *
     * @param requestUrl The URL of the image
     * @return the Bitmap requested, or null if non-exist
     */
    public Bitmap getBitmapFromCache(String requestUrl) {
        String cacheKey = VolleyUtil.uri2CacheKey(requestUrl);
        return mCache.getBitmap(cacheKey);
    }

    /**
     * Put Bitmap directly into cache
     *
     * @param requestUrl
     * @param bitmap
     *
     * @author xuegang
     * @version Created: 2014年9月11日 下午4:34:15
     */
    public void putBitmapToCache(String requestUrl, Bitmap bitmap) {
        String cacheKey = VolleyUtil.uri2CacheKey(requestUrl);
        mCache.putBitmap(cacheKey, bitmap);
    }
}
