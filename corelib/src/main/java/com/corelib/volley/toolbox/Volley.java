/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.net.http.AndroidHttpClient;
import android.os.Build;

import com.corelib.volley.Network;
import com.corelib.volley.RequestQueue;

public class Volley {

    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "volley";
    /** Default Network */
    private static Network mNetwork = null;

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, String userAgent) {
        // File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        Network network = getDefaultNetwork(userAgent);
        // RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
        RequestQueue queue = new RequestQueue(new NoCache(), network);
        queue.start();

        return queue;
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @return A started {@link RequestQueue} instance.
     */
    
    /**
     * Return the default Network
     * 
     * @param userAgent
     * @return
     */
    public static Network getDefaultNetwork(final String userAgent) {
        if (null != mNetwork) {
            return mNetwork;
        }
        
        HttpStack stack = null;
        if (Build.VERSION.SDK_INT >= 9) {
            stack = new HurlStack(userAgent);
        } else {
            // Prior to Gingerbread, HttpUrlConnection was unreliable.
            // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
            stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
        }
        
        mNetwork = new BasicNetwork(stack);
        return mNetwork;
    }
}
