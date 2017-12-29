package com.corelib.base.net;

import java.util.Collections;
import java.util.Map;

/**
 * Created by liuqun1 on 2017/10/16.
 */

public class CacheEntry {
    /** The data returned from cache. */
//    public byte[] data;

    /** ETag for cache coherency. */
    public String eTag;

    /** Date of this response as reported by the server. */
    public long serverDate;

    /** TTL for this record. */
    public long ttl;

    /** Soft TTL for this record. */
    public long softTtl;

    /** Immutable response headers as received from server; must be non-null. */
    public Map<String, String> responseHeaders = Collections.emptyMap();

    /** True if the entry is expired. */
    public boolean isExpired() {
        return this.ttl < System.currentTimeMillis();
    }

    /** True if a refresh is needed from the original data source. */
    public boolean refreshNeeded() {
        return this.softTtl < System.currentTimeMillis();
    }
}
