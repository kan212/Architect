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

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import com.corelib.R;
import com.corelib.volley.VolleyError;
import com.corelib.volley.toolbox.ImageLoader.*;



/**
 * Handles fetching an image from a URL as well as the life-cycle of the
 * associated request.
 */
public class NetworkImageView extends ImageView {
    /** The URL of the network image to load */
    private String mUrl;
    /** flag to indicate the image should only get from cache, not network */
    private boolean mOnlyCache;

    /**
     * Whether enable animation to display this ImageView
     */
    private boolean mEnableAnimation = true;

    /**
     * Resource ID of the image to be used as a placeholder until the network
     * image is loaded.
     */
    private int mDefaultImageId;

    /**
     * Resource ID of the image to be used if the network response fails.
     */
    private int mErrorImageId;

    /** Local copy of the ImageLoader. */
    private ImageLoader mImageLoader;

    /** Current ImageContainer. (either in-flight or finished) */
    private ImageContainer mImageContainer;
    /** Listener for image load event */
    private OnLoadListener mOnLoadListener;
    /** Whether view is animated */
    private boolean mHasAnimated = false;
    private int mWidth;
    private int mHeight;
    private boolean mIsUsedInRecyclerView = false;

    /**
     * 除去url参数，其它均为V540添加的性能监控需要的参数
     */
    public interface OnLoadListener {
        void onLoadSuccess(String url);

        void onLoadFailed(String url);
    }


    public NetworkImageView(Context context) {
        this(context, null);
    }

    public NetworkImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NetworkImageView);
        mEnableAnimation = a.getBoolean(R.styleable.NetworkImageView_enableAnimation, true);
        mDefaultImageId = a.getResourceId(R.styleable.NetworkImageView_defaultImage, 0);
        mErrorImageId = a.getResourceId(R.styleable.NetworkImageView_errorImage, 0);
        a.recycle();
    }

    /**
     * Sets URL of the image that should be loaded into this view. Note that
     * calling this will immediately either set the cached image (if available)
     * or the default image specified by {@link NetworkImageView#setDefaultImageResId(int)} on the
     * view.
     *
     * NOTE: If applicable, {@link NetworkImageView#setDefaultImageResId(int)} and
     * {@link NetworkImageView#setErrorImageResId(int)} should be called
     * prior to calling this function.
     *
     * @param url
     *            The URL that should be loaded into this ImageView.
     * @param imageLoader
     *            ImageLoader that will be used to make the request.
     * @param onlyCache
     *            Flag to indicate the image should only fetched from cache, not network
     */
    protected void setImageUrl(String url, ImageLoader imageLoader, boolean onlyCache, String newsId, String from) {
        setImageUrl(url, imageLoader, onlyCache, getWidth(), getHeight(),newsId,from);
    }

    /**
     * add parameter: ImageView's width and height
     *
     * @param url
     *            The URL that should be loaded into this ImageView.
     * @param imageLoader
     *            ImageLoader that will be used to make the request.
     * @param onlyCache
     *            Flag to indicate the image should only fetched from cache, not network
     * @param width
     *            ImageView's width
     * @param height
     *            ImageView's height
     */
    protected void setImageUrl(String url, ImageLoader imageLoader, boolean onlyCache, int width,
                               int height, String newsId, String from) {
        mUrl = url;
        mImageLoader = imageLoader;
        // The URL has potentially changed. See if we need to load it.
        mOnlyCache = onlyCache;
        mWidth = width;
        mHeight = height;
        loadImageIfNecessary(false,newsId,from);
    }

    /**
     *
     * 660开始图片请求header添加来源from跟newsid,开发使用的时候构建parameters
     * 为必传参数，这部分不在单独封装
     * @param url
     * @param imageLoader
     * @param parameters
     */
    protected void setImageUrl(String url, ImageLoader imageLoader, String newsId, String from) {
        setImageUrl(url, imageLoader, false,newsId,from);
    }

    /**
     * Sets the default image resource ID to be used for this view until the
     * attempt to load it completes.
     */
    public void setDefaultImageResId(int defaultImage) {
        mDefaultImageId = defaultImage;
    }

    /**
     * Sets the error image resource ID to be used for this view in the event
     * that the image requested fails to load.
     */
    public void setErrorImageResId(int errorImage) {
        mErrorImageId = errorImage;
    }

    /**
     * set OnLoadListener
     *
     * @param l
     *
     * @author xuegang
     * @version Created: 2014年9月18日 下午4:14:58
     */
    public void setOnLoadListener(OnLoadListener l) {
        mOnLoadListener = l;
    }



    /**
     * Loads the image for the view if it isn't already loaded.
     *
     * @param isInLayoutPass
     *            True if this was invoked from a layout pass, false otherwise.
     */
    void loadImageIfNecessary(final boolean isInLayoutPass, String newsId, String from) {
        int width = mWidth;
        int height = mHeight;

        boolean wrapWidth = false, wrapHeight = false;
        if (getLayoutParams() != null) {
            wrapWidth = getLayoutParams().width == LayoutParams.WRAP_CONTENT;
            wrapHeight = getLayoutParams().height == LayoutParams.WRAP_CONTENT;
        }

        // if the view's bounds aren't known yet, and this is not a
        // wrap-content/wrap-content
        // view, hold off on loading the image.
        // boolean isFullyWrapContent = wrapWidth && wrapHeight;
        // if (width == 0 && height == 0 && !isFullyWrapContent) {
        // if (null != mOnLoadListener) {
        // mOnLoadListener.onLoadFailed(mUrl);
        // }
        // return;
        // }

        // if the URL to be loaded in this view is empty, cancel any old
        // requests and clear the
        // currently loaded image.
        if (TextUtils.isEmpty(mUrl)) {
            if (mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            setDefaultImageOrNull();
            return;
        }

        // if there was an old request in this view, check if it needs to be
        // canceled.
        if (mImageContainer != null && mImageContainer.getRequestUrl() != null) {
            if (mImageContainer.getRequestUrl().equals(mUrl)) {
                // if the request is from the same URL, return.
                // return;
            } else {
                // if there is a pre-existing request, cancel it if it's
                // fetching a different URL.
                mImageContainer.cancelRequest();
                setDefaultImageOrNull();
            }
        }

        // Calculate the max image width / height to use while ignoring
        // WRAP_CONTENT dimens.
        int maxWidth = wrapWidth ? 0 : width;
        int maxHeight = wrapHeight ? 0 : height;

        // The pre-existing content of this view didn't match the current URL.
        // Load the new image
        // from the network.
        ImageContainer newContainer = mImageLoader.get(mUrl, new ImageListener() {
            @Override
            public void onResponse(final ImageContainer response, boolean isImmediate) {
                if(response!=null){
                    response.setReqEndTime(System.currentTimeMillis());
                }
                // If this was an immediate response that was delivered
                // inside of a layout
                // pass do not set the image immediately as it will
                // trigger a requestLayout
                // inside of a layout. Instead, defer setting the image
                // by posting back to
                // the main thread.
                if (isImmediate && isInLayoutPass) {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            onResponse(response, false);
                        }
                    });
                    return;
                }
                if (response.getBitmap() != null) {
                    setImageBitmap(response.getBitmap());
                    if (needAnimation()) {
                        fadeAnimate();
                    }
                    if (mOnLoadListener != null) {
                        mOnLoadListener.onLoadSuccess(mUrl);
                    }
                } else {
                    if (mErrorImageId != 0) {
                        setImageResource(mErrorImageId);
                    }
                    if (mOnLoadListener != null) {
                        mOnLoadListener.onLoadFailed(mUrl);
                    }
                }
            }

            @Override
            public void onErrorResponse(VolleyError error, ImageContainer response, boolean isImmediate) {
                if (mErrorImageId != 0) {
                    setImageResource(mErrorImageId);
                }
                if (mOnLoadListener != null) {
                    if(response!=null){
                        mOnLoadListener.onLoadFailed(mUrl);
                    }else {
                        mOnLoadListener.onLoadFailed(mUrl);
                    }
                }
            }
        }, maxWidth, maxHeight, mOnlyCache,newsId,from);

        // update the ImageContainer to be the new bitmap container.
        mImageContainer = newContainer;
    }

    private void setDefaultImageOrNull() {
        if (mDefaultImageId != 0) {
            setImageResource(mDefaultImageId);
        } else {
            setImageBitmap(null);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // loadImageIfNecessary(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!mIsUsedInRecyclerView) {
            if (mImageContainer != null) {
                // If the view was bound to an image request, cancel it and clear
                // out the image from the view.
                mImageContainer.cancelRequest();
                setImageBitmap(null);

                // also clear out the container so we can reload the image if
                // necessary.
                mImageContainer = null;
            }
            mHasAnimated = false;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    /**
     * Judge whether we need animation. There are now three conditions:
     * 1. We must enable animation.
     * 2. This view has never been animated.
     * 3. SDK version is equal or larger than honeycomb. Note, this is just for purpose of
     * efficiency
     *
     * @return
     *
     * @author xuegang
     * @version Created: 2014年10月21日 上午10:54:37
     */
    private boolean needAnimation() {
        if (mEnableAnimation && !mHasAnimated
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return true;
        }

        return false;
    }

    private void fadeAnimate() {
        mHasAnimated = true;
        AlphaAnimation fadeImage = new AlphaAnimation(0, 1);
        fadeImage.setDuration(1000);
        fadeImage.setInterpolator(new DecelerateInterpolator());
        startAnimation(fadeImage);
    }

    /**
     * 该View是否用在RecyclerView中。RecyclerView和某些网络图片加载框架存在兼容问题，会导致滑动时图片加载有问题
     * @author fuhong
     * @param isUsedInRecyclerView
     */
    public void setIsUsedInRecyclerView(boolean isUsedInRecyclerView) {
        mIsUsedInRecyclerView = isUsedInRecyclerView;
    }
}
