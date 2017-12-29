package com.corelib.volley.toolbox;

import java.io.File;

/**
 * 
 * @Description
 * @version Created: 2015年6月18日16:50:27
 */
public class UnlimitedUnCleanableDiskFileCache extends UnlimitedDiskFileCache {

    public UnlimitedUnCleanableDiskFileCache(String rootDirectoryPath) {
        super(rootDirectoryPath);
    }

    @Override
    protected void createRootDirectory() {
        if (null == mRootDirectory) {
            if (null == mRootDirectoryPath) {
                mRootDirectoryPath = VolleyConfig.getLocalUncleanableDirectoryPath();
            }

            mRootDirectory = new File(mRootDirectoryPath);
            if (!mRootDirectory.exists()) {
                mRootDirectory.mkdirs();
            }
        }
    }

}
