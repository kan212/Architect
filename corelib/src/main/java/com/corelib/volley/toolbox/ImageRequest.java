/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.corelib.volley.DefaultRetryPolicy;
import com.corelib.volley.NetworkResponse;
import com.corelib.volley.ParseError;
import com.corelib.volley.Request;
import com.corelib.volley.Response;
import com.corelib.volley.VolleyError;
import com.corelib.volley.VolleyLog;

import java.io.File;

/**
 * A canned request for getting an image at a given URL and calling back with a
 * decoded Bitmap.
 */
public class ImageRequest extends Request<Bitmap> {
    /** Socket timeout in milliseconds for image requests */
    private static final int IMAGE_TIMEOUT_MS = 1000;

    /** Default number of retries for image requests */
    private static final int IMAGE_MAX_RETRIES = 2;

    /** Default backoff multiplier for image requests */
    private static final float IMAGE_BACKOFF_MULT = 2f;

    private final Response.Listener<Bitmap> mListener;
    private final Config mDecodeConfig;
    private final int mMaxWidth;
    private final int mMaxHeight;

    /**
     * Decoding lock so that we don't decode more than one image at a time (to
     * avoid OOM's)
     */
    private static final Object sDecodeLock = new Object();
    private final CacheType mCacheType;

    /**
     * Creates a new image request, decoding to a maximum specified width and
     * height. If both width and height are zero, the image will be decoded to
     * its natural size. If one of the two is nonzero, that dimension will be
     * clamped and the other one will be set to preserve the image's aspect
     * ratio. If both width and height are nonzero, the image will be decoded to
     * be fit in the rectangle of dimensions width x height while keeping its
     * aspect ratio.
     *
     * @param url
     *            URL of the image
     * @param listener
     *            Listener to receive the decoded bitmap
     * @param maxWidth
     *            Maximum width to decode this bitmap to, or zero for none
     * @param maxHeight
     *            Maximum height to decode this bitmap to, or zero for none
     * @param decodeConfig
     *            Format to decode the bitmap to
     * @param errorListener
     *            Error listener, or null to ignore errors
     * @param cacheType cache type
     */
    public ImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth,
                        int maxHeight, Config decodeConfig, Response.ErrorListener errorListener, CacheType cacheType) {
        super(Method.GET, url, errorListener);
        setRetryPolicy(new DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES,
                IMAGE_BACKOFF_MULT));
        mListener = listener;
        mDecodeConfig = decodeConfig;
        mMaxWidth = maxWidth;
        mMaxHeight = maxHeight;
        mCacheType = cacheType;
    }

    public ImageRequest(String url, Response.Listener<Bitmap> listener, int maxWidth,
                        int maxHeight, Config decodeConfig, Response.ErrorListener errorListener) {
        this(url, listener, maxWidth, maxHeight, decodeConfig, errorListener, CacheType.NORMAL_CACHE);
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    /**
     * Scales one side of a rectangle to fit aspect ratio.
     *
     * @param maxPrimary
     *            Maximum size of the primary dimension (i.e. width for max
     *            width), or zero to maintain aspect ratio with secondary
     *            dimension
     * @param maxSecondary
     *            Maximum size of the secondary dimension, or zero to maintain
     *            aspect ratio with primary dimension
     * @param actualPrimary
     *            Actual size of the primary dimension
     * @param actualSecondary
     *            Actual size of the secondary dimension
     */
    private static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
            int actualSecondary) {
        // If no dominant value at all, just return the actual.
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling
        // ratio.
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;
        if (resized * ratio > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }

    @Override
    protected Response<Bitmap> parseCache() {
        synchronized (sDecodeLock) {
            File dir;
            if (mCacheType == CacheType.UNCLEANABLE_CACHE) {
                dir = VolleyConfig.getLocalUncleanableDirectory();
            } else {
                dir = VolleyConfig.getLocalImageDirectory();
            }
            File file = new File(dir, VolleyUtil.uri2CacheKey(getUrl()));
            if (!file.exists()) {
                if (VolleyLog.DEBUG) {
                    VolleyLog.d("No Cache: %s", getUrl());
                }
                return Response.error(new VolleyError("No Cache"));
            }

            byte[] data = VolleyUtil.readFile(file);
            if (null == data) {
                VolleyLog.e("Failed to read file: %s", file.getAbsoluteFile());
                return Response.error(new VolleyError("Read Cache Failed"));
            }

            try {
                Bitmap bitmap = doParse(data);
                if (null == bitmap) {
                    VolleyLog.e("Failed to decode Bitmap");
                    return Response.error(new VolleyError("Decode Cache Failed"));
                }
                /* 从文件Cache来的response，需要更新到内存Cache */
                updateMemoryCache(bitmap);
                return Response.success(bitmap,
                        HttpHeaderParser.parseCacheHeaders(new NetworkResponse(data)));
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                VolleyLog.e("Failed to decode Bitmap from Cache");
                return Response.error(new VolleyError("Decode Cache Bitmap Failed"));
            }
        }
    }

    @Override
    protected Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        // Serialize all decode on a global lock to reduce concurrent heap
        // usage.
        synchronized (sDecodeLock) {
            try {
                if(mListener!=null&&response!=null){
                    mListener.onResponseHeadersAndData(response.headers,null,response.statusCode,response.isFroNetwork());
                }
                Bitmap bitmap = doParse(response.data);
                if (bitmap == null) {
                    VolleyLog.e("ParseError");
                    return Response.error(new ParseError(response));
                }
                /* 从网络来的response，需要更新到文件Cache和内存Cache */
                updateFileCache(response.data);
                updateMemoryCache(bitmap);
                return Response.success(bitmap, HttpHeaderParser.parseCacheHeaders(response));
            } catch (OutOfMemoryError e) {
                VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
                return Response.error(new ParseError(e));
            }
        }
    }

    /**
     * The real guts of parseNetworkResponse. Broken out for readability.
     */
    private Bitmap doParse(byte[] data) {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;
        if (mMaxWidth == 0 && mMaxHeight == 0) {
            decodeOptions.inPreferredConfig = mDecodeConfig;
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        } else {
            // If we have to resize this image, first get the natural bounds.
            decodeOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            int actualWidth = decodeOptions.outWidth;
            int actualHeight = decodeOptions.outHeight;

            // Then compute the dimensions we would ideally like to decode to.
            int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight, actualWidth, actualHeight);
            int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth, actualHeight,
                    actualWidth);

            // Decode to the nearest power of two scaling factor.
            decodeOptions.inJustDecodeBounds = false;
            // TODO(ficus): Do we need this or is it okay since API 8 doesn't
            // support it?
            // decodeOptions.inPreferQualityOverSpeed =
            // PREFER_QUALITY_OVER_SPEED;
            decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight,
                    desiredWidth, desiredHeight);
            Bitmap tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            // TODO(xuegang): 缩放操作对内存和时间消耗太大，没必要进行此操作，对UI没什么影响。
            // If necessary, scale down to the maximal acceptable size.
            // if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth
            // ||
            // tempBitmap.getHeight() > desiredHeight)) {
            // bitmap = Bitmap.createScaledBitmap(tempBitmap,
            // desiredWidth, desiredHeight, true);
            // tempBitmap.recycle();
            // } else {
            bitmap = tempBitmap;
            // }
        }

        return bitmap;
    }

    @Override
    protected void deliverResponse(Bitmap response) {
        mListener.onResponse(response);
    }

    /**
     * Returns the largest power-of-two divisor for use in downscaling a bitmap
     * that will not result in the scaling past the desired dimensions.
     *
     * @param actualWidth
     *            Actual width of the bitmap
     * @param actualHeight
     *            Actual height of the bitmap
     * @param desiredWidth
     *            Desired width of the bitmap
     * @param desiredHeight
     *            Desired height of the bitmap
     */
    // Visible for testing.
    static int findBestSampleSize(int actualWidth, int actualHeight, int desiredWidth,
            int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }

        return (int) n;
    }

    /**
     * Save Bitmap to file cache
     *
     * @param data
     *
     * @author xuegang
     * @version Created: 2014年10月2日 下午3:30:32
     */
    private void updateFileCache(byte[] data) {
        String url = getUrl();
        String cacheKey = VolleyUtil.uri2CacheKey(url);
        FileCache fileCache = VolleyConfig.getFileCache(CacheType.NORMAL_CACHE);
        if (null == fileCache) {
            VolleyLog.e("file cache not set");
            return;
        }

        fileCache.putFile(cacheKey, data);
    }

    /**
     * Save Bitmap to image cache
     *
     * @param bitmap
     *
     * @author xuegang
     * @version Created: 2014年10月2日 下午3:30:43
     */
    private void updateMemoryCache(Bitmap bitmap) {
        String url = getUrl();
        if (TextUtils.isEmpty(url) || null == bitmap) {
            VolleyLog.e("parameter invalid");
            return;
        }

        ImageCache imageCache = VolleyConfig.getImageCache();
        if (null == imageCache) {
            VolleyLog.e("image cache not set");
            return;
        }

        imageCache.putBitmap(url, bitmap);
    }
}
