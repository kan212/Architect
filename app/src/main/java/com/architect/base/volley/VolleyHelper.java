package com.architect.base.volley;

import android.os.Process;

import com.architect.ArchitectApp;
import com.architect.base.api.ApiBase;
import com.architect.base.util.FileUtils;
import com.architect.base.util.HttpUtils;
import com.corelib.volley.Network;
import com.corelib.volley.Request;
import com.corelib.volley.RequestQueue;
import com.corelib.volley.RequestQueue.RequestFilter;
import com.corelib.volley.toolbox.BitmapCache;
import com.corelib.volley.toolbox.UnlimitedDiskFileCache;
import com.corelib.volley.toolbox.UnlimitedUnCleanableDiskFileCache;
import com.corelib.volley.toolbox.Volley;
import com.corelib.volley.toolbox.VolleyConfig;

import java.util.Collection;


/**
 * Created by kan212 on 17/12/27.
 */

public class VolleyHelper {

    private RequestQueue mRequestQueue;
    private RequestQueue mMinPriorityQueue;

    private static VolleyHelper _instance = null;
    private CoreImageLoader mImageLoader = null;
    private CoreFileLoad mFileLoader = null;


    public static VolleyHelper getInstance() {
        if (null == _instance) {
            synchronized (VolleyHelper.class) {
                if (null == _instance) {
                    _instance = new VolleyHelper();
                }
            }
        }
        return _instance;
    }

    private VolleyHelper() {
        VolleyConfig.setLocalImageDirectory(FileUtils.getCacheDirectoryPath());
        VolleyConfig.setLocalUncleanableDirectory(FileUtils.getUncleanableDirectoryPath());
        VolleyConfig.setImageCache(new BitmapCache(ArchitectApp.getMemoryCacheSize()));
        VolleyConfig.putFileCache(VolleyConfig.CacheType.NORMAL_CACHE,
                new UnlimitedDiskFileCache(FileUtils.getCacheDirectoryPath()));
        VolleyConfig.putFileCache(VolleyConfig.CacheType.UNCLEANABLE_CACHE,
                new UnlimitedUnCleanableDiskFileCache(FileUtils.getUncleanableDirectoryPath()));
        mRequestQueue = Volley.newRequestQueue(ArchitectApp.getAppContext(),
                HttpUtils.getUserAgent());
        mMinPriorityQueue = Volley.newRequestQueue(ArchitectApp.getAppContext(),
                HttpUtils.getUserAgent());
        mMinPriorityQueue.setPriority(Process.THREAD_PRIORITY_LOWEST);

        mImageLoader = new CoreImageLoader(mRequestQueue);
        mFileLoader = new CoreFileLoad(mRequestQueue);
    }

    /**
     * 获取默认的图片下载器，只能在UI线程调用
     *
     * @return
     * @author xuegang
     * @version Created: 2014年9月16日 上午11:55:04
     */
    public CoreImageLoader getImageLoader() {
        return mImageLoader;
    }

    /**
     * 获取文件下载器，只能在UI线程调用
     *
     * @return
     * @author xuegang
     * @version Created: 2014年9月16日 上午11:54:30
     */
    public CoreFileLoad getFileLoader() {
        return mFileLoader;
    }

    /**
     * 添加一个网络请求到Volley框架，只能在UI线程调用
     *
     * @param request
     * @author xuegang
     * @version Created: 2014年9月16日 上午11:55:22
     * @deprecated use add(String tag, Request<?> request) instead.
     */
    @Deprecated
    public void add(Request<?> request) {
        mRequestQueue.add(request);
    }

    public void add(Object tag, Request<?> request, boolean isMinPriority) {
        if (tag == null) {
            throw new IllegalArgumentException("tag can't be null");
        }
        request.setTag(tag);

        // debug mode下timeout，retrytimes 加倍
//        if (DebugConfig.getInstance().isDebugSwitchOn()) {
//            request.setRetryPolicy(new DefaultRetryPolicy(
//                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 2,
//                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES * 2,
//                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
//        }
        if(isMinPriority){
            mMinPriorityQueue.add(request);
        } else {
            mRequestQueue.add(request);
        }
    }

    /**
     * 取消tag的网络请求
     *
     * @param tag
     * @author xuegang
     * @version Created: 2014年9月16日 上午11:55:44
     */
    public void cancelRequests(final Object tag) {
        if (null == tag) {
            return;
        }

        mRequestQueue.cancelAll(tag);
        mMinPriorityQueue.cancelAll(tag);
    }

    /**
     * 取消特定ApiBase对象的网络请求
     *
     * @param api
     * @author baocheng
     * @version Created: 2015年11月17日
     */
    public void cancelRequestsByApi(final ApiBase api) {
        cancelRequests(api);
    }

    /**
     * 批量取消一组ApiBase对象的网络请求
     *
     * @param apis
     * @author baocheng
     * @version Created: 2015年11月17日
     */
    public void cancelRequestsByApi(final Collection<ApiBase> apis) {
        RequestFilter filter = new RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                for (ApiBase api : apis) {
                    if (request.getTag() == api) {
                        return true;
                    }
                }
                return false;
            }
        };
        mRequestQueue.cancelAll(filter);
        mMinPriorityQueue.cancelAll(filter);
    }

    /**
     * 批量取消所有带有特定flag的ApiBase对象的网络请求
     *
     * @param flag
     * @author baocheng
     * @version Created: 2015年11月17日
     */
    public void cancelRequestsByApiFlag(final int flag) {
        RequestFilter filter = new RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                if (!(request.getTag() instanceof ApiBase)) {
                    return false;
                }

                return ((ApiBase) request.getTag()).getFlag() == flag;
            }
        };
        mRequestQueue.cancelAll(filter);
        mMinPriorityQueue.cancelAll(filter);
    }

    /**
     * 获取默认的网络执行器
     *
     * @return
     * @author xuegang
     * @version Created: 2014年9月16日 上午11:56:12
     */
    public Network getDefaultNetwork() {
        return Volley.getDefaultNetwork(HttpUtils.getUserAgent());
    }

}

