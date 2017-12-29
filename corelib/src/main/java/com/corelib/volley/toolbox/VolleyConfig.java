package com.corelib.volley.toolbox;

import android.os.Environment;

import com.corelib.volley.toolbox.FileLoader.FileCache;
import com.corelib.volley.toolbox.ImageLoader.ImageCache;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class VolleyConfig {

    public enum CacheType {
        NORMAL_CACHE, UNCLEANABLE_CACHE;
    }

    private static final String DEFAULT_IMAGE_PATH = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/Android/data/com.architect/cache";
    private static final String DEFAULT_UNCLEANABLE_PATH = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/Android/data/com.architect/uncleanable";

    private static String mLocalImageDirectoryPath = null;
    private static String mLocalUncleanableDirectoryPath = null;
    /**
     * 本地图片目录。如果目录中有所需图片，则直接从该目录中获取，而不需要经过网络
     */
    private static File mLocalImageDirectory = null;
    private static File mLocalUncleanableDirectory = null;
    /**
     * memory cache for Bitmap
     */
    private static ImageCache mImageCache = null;
    /**
     * file cache
     */
    private static Map<CacheType, FileCache> mFileCacheMap = new HashMap<>();

    public static String getLocalImageDirectoryPath() {
        if (null == mLocalImageDirectoryPath) {
            mLocalImageDirectoryPath = DEFAULT_IMAGE_PATH;
        }

        return mLocalImageDirectoryPath;
    }

    public static String getLocalUncleanableDirectoryPath() {
        if (null == mLocalUncleanableDirectoryPath) {
            mLocalUncleanableDirectoryPath = DEFAULT_UNCLEANABLE_PATH;
        }

        return mLocalUncleanableDirectoryPath;
    }

    public static void setLocalImageDirectory(String localImageDirectory) {
        mLocalImageDirectoryPath = localImageDirectory;
    }

    public static void setLocalUncleanableDirectory(String localUncleanableDirectory) {
        mLocalUncleanableDirectoryPath = localUncleanableDirectory;
    }

    public static File getLocalImageDirectory() {
        if (null == mLocalImageDirectory) {
            if (mLocalImageDirectoryPath == null) {
                mLocalImageDirectoryPath = DEFAULT_IMAGE_PATH;
            }
            mLocalImageDirectory = new File(mLocalImageDirectoryPath);
            if (!mLocalImageDirectory.exists()) {
                mLocalImageDirectory.mkdirs();
            }
        }
        return mLocalImageDirectory;
    }

    public static File getLocalUncleanableDirectory() {
        if (null == mLocalUncleanableDirectory) {
            mLocalUncleanableDirectory = new File(getLocalUncleanableDirectoryPath());
            if (!mLocalUncleanableDirectory.exists()) {
                mLocalUncleanableDirectory.mkdirs();
            }
        }
        return mLocalUncleanableDirectory;
    }

    public static ImageCache getImageCache() {
        if (mImageCache == null) {
            mImageCache = new BitmapCache();
        }
        return mImageCache;
    }

    public static void setImageCache(ImageCache imageCache) {
        mImageCache = imageCache;
    }

    public static Map<CacheType, FileCache> getFileCacheMap() {
        return mFileCacheMap;
    }

    public static FileCache getFileCache(CacheType key) {
        return mFileCacheMap.get(key);
    }

    public static void setFileCacheMap(Map<CacheType, FileCache> fileCacheMap) {
        mFileCacheMap = fileCacheMap;
    }

    public static void putFileCache(CacheType key, FileCache cache) {
        mFileCacheMap.put(key, cache);
    }
}
