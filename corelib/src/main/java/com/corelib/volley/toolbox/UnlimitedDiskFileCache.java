package com.corelib.volley.toolbox;

import android.text.TextUtils;

import com.corelib.volley.VolleyLog;
import com.corelib.volley.toolbox.FileLoader.FileCache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 
 * @Description
 * @author xuegang
 * @version Created: 2014年11月13日 下午2:57:10
 */
public class UnlimitedDiskFileCache implements FileCache {
    protected String mRootDirectoryPath;
    protected File mRootDirectory;

    public UnlimitedDiskFileCache(String rootDirectoryPath) {
        mRootDirectoryPath = rootDirectoryPath;
    }

    @Override
    public void init() {

    }

    @Override
    public void clear() {

    }

    protected void createRootDirectory() {
        if (null == mRootDirectory) {
            if (null == mRootDirectoryPath) {
                mRootDirectoryPath = VolleyConfig.getLocalImageDirectoryPath();
            }

            mRootDirectory = new File(mRootDirectoryPath);
            if (!mRootDirectory.exists()) {
                mRootDirectory.mkdirs();
            }
        } else if (!mRootDirectory.exists()) {
            mRootDirectory.mkdirs();
        }
    }

    @Override
    public String getFile(String fileName) {
        if (null == mRootDirectory) {
            createRootDirectory();
            if (null == mRootDirectory || !mRootDirectory.exists()) {
                VolleyLog.e("Root directory does not exits");
                return null;
            }
        }

        if (TextUtils.isEmpty(fileName)) {
            VolleyLog.e("invalid parameter");
            return null;
        }

        File file = new File(mRootDirectory, fileName);
        if (!file.exists()) {
            return null;
        }

        return file.getAbsolutePath();
    }

    @Override
    public String putFile(String fileName, byte[] data) {
        if (null == mRootDirectory || !mRootDirectory.exists()) {
            createRootDirectory();
            if (null == mRootDirectory || !mRootDirectory.exists()) {
                VolleyLog.e("DiskFileCache's root directory is null");
                return null;
            }
        }


        if (TextUtils.isEmpty(fileName) || null == data || data.length == 0) {
            VolleyLog.e("invalid parameter");
            return null;
        }

        File file = new File(mRootDirectory, fileName);
        if (file.exists()) {
            return file.getAbsolutePath();
        }

        try {
            file.createNewFile();
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void remove(String key) {

    }

}
