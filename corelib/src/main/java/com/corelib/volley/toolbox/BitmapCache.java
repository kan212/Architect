package com.corelib.volley.toolbox;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;

public class BitmapCache implements ImageLoader.ImageCache {
    /** Size of memory cache */
    private static final int BITMAP_CACHE_MIN_SIZE = 4 * 1024 * 1024;
    /** Size of soft reference cache */
    private static final int SOFT_CACHE_SIZE = 16;
    /** LRU based memory cache */
    private LruCache<String, Bitmap> mMemoryCache = null;
    /** Soft-referenced memory cache */
    private static LinkedHashMap<String, WeakReference<Bitmap>> sSoftMemoryCache;

    public BitmapCache() {
        this(BITMAP_CACHE_MIN_SIZE);
    }

    public BitmapCache(int maxSize) {
        if (maxSize < BITMAP_CACHE_MIN_SIZE) {
            maxSize = BITMAP_CACHE_MIN_SIZE;
        }
        mMemoryCache = new LruCache<String, Bitmap>(maxSize) {

            @Override
            protected int sizeOf(String key, Bitmap value) {
                int size = value.getRowBytes() * value.getHeight();
                return size;
            }

            @Override
            protected void entryRemoved(boolean evicted, String key,
                                        Bitmap oldValue, Bitmap newValue) {
                if (oldValue != null) {
                    sSoftMemoryCache.put(key, new WeakReference<Bitmap>(
                            oldValue));
                }
            }
        };

        sSoftMemoryCache = new LinkedHashMap<String, WeakReference<Bitmap>>(
                SOFT_CACHE_SIZE, 0.75f, true) {
            private static final long serialVersionUID = 6040103833179403725L;

            @Override
            protected boolean removeEldestEntry(
                    Entry<String, WeakReference<Bitmap>> eldest) {
                if (size() > SOFT_CACHE_SIZE) {
                    return true;
                }
                return false;
            }
        };
    }

    @Override
    public synchronized Bitmap getBitmap(String url) {
        /* Firstly, search from memory cache */
        Bitmap bitmap = mMemoryCache.get(url);
        if (null != bitmap) {
            /* Found in memory cache, return directly */
            return bitmap;
        }

        /* Secondly, search from soft reference cache */
        WeakReference<Bitmap> softReverenceBitmap = sSoftMemoryCache.get(url);
        if (null != softReverenceBitmap) {
            bitmap = softReverenceBitmap.get();
            if (null != bitmap) {
                /*
                 * Found in soft reference cache, so put result back to memory
                 * cache, and remove it from soft reference
                 */
                mMemoryCache.put(url, bitmap);
                sSoftMemoryCache.remove(url);
                return bitmap;
            }
        }

        return null;
    }

    @Override
    public synchronized void putBitmap(String url, Bitmap bitmap) {
        mMemoryCache.put(url, bitmap);
    }
}
