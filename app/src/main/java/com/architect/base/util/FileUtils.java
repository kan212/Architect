package com.architect.base.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import com.architect.ArchitectApp;
import com.architect.base.volley.VolleyHelper;
import com.corelib.base.net.DateUtils;
import com.corelib.base.util.MD5;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by kan212 on 17/12/28.
 */

public class FileUtils {


    /**
     * Default I/O buffer size
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    /**
     * Name of download directory
     */
    public static final String DOWNLOAD_DIRECTORY = "download";
    /**
     * Name of uncleanable directory
     */
    private static final String UNCLEANABLE_DIRECTORY = "uncleanable";
    /**
     * Download directory
     */
    private static File sDownloadDirectory = null;
    /**
     * Uncleanable directory
     */
    private static File sUncleanableDirectory = null;
    /**
     * Cache directory
     */
    private static File sCacheDirectory = null;

    /**
     * 获取下载目录
     *
     * @return
     */
    public static File getDownloadDirectory() {
        if (null == sDownloadDirectory) {
            synchronized (FileUtils.class) {
                if (null == sDownloadDirectory) {
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        sDownloadDirectory = ArchitectApp.getAppContext()
                                .getExternalFilesDir(DOWNLOAD_DIRECTORY);
                    } else {
                        sDownloadDirectory = ArchitectApp.getAppContext().getDir(
                                DOWNLOAD_DIRECTORY, Context.MODE_PRIVATE);
                    }
                }
            }
        }

        return sDownloadDirectory;
    }

    /**
     * 获取不可清除缓存的目录
     *
     * @return
     */
    public static File getUncleanableDirectory() {
        if (null == sUncleanableDirectory) {
            synchronized (FileUtils.class) {
                if (null == sUncleanableDirectory) {
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        sUncleanableDirectory = ArchitectApp.getAppContext()
                                .getExternalFilesDir(UNCLEANABLE_DIRECTORY);
                    } else {
                        sUncleanableDirectory = ArchitectApp.getAppContext().getDir(
                                UNCLEANABLE_DIRECTORY, Context.MODE_PRIVATE);
                    }
                }
            }
        }

        return sUncleanableDirectory;
    }

    /**
     * 获取缓存目录
     *
     * @return
     */
    public static File getCacheDirectory() {
        if (null == sCacheDirectory) {
            synchronized (FileUtils.class) {
                if (null == sCacheDirectory) {
                    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                        try {
                            sCacheDirectory = ArchitectApp.getAppContext().getExternalCacheDir();
                        } catch (Exception exception) {
                        }
                    }
                    // default to use internal cache directory
                    if (null == sCacheDirectory) {
                        sCacheDirectory = ArchitectApp.getAppContext().getCacheDir();
                    }
                }
            }
        }

        return sCacheDirectory;
    }

    public static String getUncleanableDirectoryPath() {
        File directory = getUncleanableDirectory();
        String path = null;
        if (null != directory) {
            path = directory.getAbsolutePath();
        }

        return path;
    }

    public static String getCacheDirectoryPath() {
        File cacheDir = getCacheDirectory();
        String cachePath = null;
        if (null != cacheDir) {
            cachePath = cacheDir.getAbsolutePath();
        }
        return cachePath;
    }

    public static boolean isCacheSpaceLess() {
        try {
            long leftSize = getAvailableCacheSpace();
            if (leftSize < ConstantData.MIN_DOWNLOAD_IMAGE_SOTROAGRE_SIZE) {
                return true;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private static long getAvailableCacheSpace() {
        StatFs stat = new StatFs(getCacheDirectoryPath());
        long blockSize = 0L;
        long availBlocks = 0L;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.getBlockSizeLong();
            availBlocks = stat.getAvailableBlocksLong();
        } else {
            blockSize = stat.getBlockSize();
            availBlocks = stat.getAvailableBlocks();
        }
        return availBlocks * blockSize;
    }

    /**
     * Read data from input stream and return as a String
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static String readStream(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int cnt = 0;
        while ((cnt = in.read(buffer)) > -1) {
            baos.write(buffer, 0, cnt);
        }
        baos.flush();

        in.close();
        baos.close();

        return baos.toString();
    }

    /**
     * @param file: file object
     * @return file contents as string
     */
    public static String readFileToString(File file) {
        try {
            InputStream fileInputStream = new FileInputStream(file);
            return readStream(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] readFileToBytes(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            throw new FileNotFoundException(filePath);
        }
        FileInputStream fileInputStream = new FileInputStream(file);
        FileChannel fileChannel = fileInputStream.getChannel();
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());
            while (fileChannel.read(byteBuffer) > 0) {
                //do nothing
            }
            return byteBuffer.array();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            try {
                fileChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Copy data from input stream to output stream
     *
     * @param input
     * @param out
     * @throws IOException
     */
    public static void copyStream(InputStream input, OutputStream out) throws IOException {
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int temp = -1;
        while ((temp = input.read(buffer)) != -1) {
            out.write(buffer, 0, temp);
        }
        out.flush();
    }

    /**
     * Copy stream data without closing stream
     *
     * @param input
     * @param out
     * @throws IOException
     * @author xuegang
     * @version Created: 2014年9月23日 下午5:45:06
     */
    public static void copyWithoutOutputClosing(InputStream input, OutputStream out)
            throws IOException {
        try {
            final byte[] buffer = new byte[512];
            int temp = -1;
            while ((temp = input.read(buffer)) != -1) {
                out.write(buffer, 0, temp);
                out.flush();
            }
        } catch (final IOException e) {
            throw e;
        } finally {
            input.close();
        }
    }

    public static final int sSaveBmpSuccess = 0;
    public static final int sSaveBmpAlreadyExist = 1;
    public static final int sSaveBmpFailed = 2;
    public static final int sSaveBmpNull = 3;
    private static final String PATH = String.format("%s%s",
            Environment.getExternalStorageDirectory(), "/sina/news/save/");

    /**
     * savePicAlbum: save input bitmap to album.
     *
     * @param - ctx
     *          - bmp, bitmap which will be saved into album.
     *          - urlOrName, picture url or file name, which will be used to generate saved name.
     *          - outPathBuild, returned saved file path in string builder, can be null.
     *          - reCreate: recreate file when same file exists if true.
     * @return - sSaveBmpSuccess, sSaveBmpAlreadyExist, sSaveBmpFailed or sSaveBmpNull
     */
    public static int savePicAlbum(Context ctx, Bitmap bmp, String urlOrName,
                                   StringBuilder outPathStrBuilder, boolean reCreate) {
        if (bmp == null) {
            return sSaveBmpNull;
        }

        File file = new File(PATH);
        if (!file.exists()) {
            file.mkdirs();
        }

        if (urlOrName.toLowerCase(Locale.getDefault()).contains(".png")) {
            urlOrName = MD5.hexdigest(urlOrName).substring(0, 10) + ".png";
        } else {
            urlOrName = MD5.hexdigest(urlOrName).substring(0, 10) + ".jpg";
        }

        String path = PATH + urlOrName;
        if (outPathStrBuilder != null) {
            outPathStrBuilder.append(path);
        }
        File tempFile = new File(path);
        if (tempFile.exists()) {
            if (!reCreate) {
                return sSaveBmpAlreadyExist;
            }

            tempFile.delete();
        }

        OutputStream outputStream;
        try {
            outputStream = new FileOutputStream(path);
            if (urlOrName.toLowerCase(Locale.getDefault()).contains(".png")) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            } else {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            }
            outputStream.flush();
            outputStream.close();
            addFile2MediaDb(ctx, path);
        } catch (IOException e) {
            e.printStackTrace();
            return sSaveBmpFailed;
        }
        return sSaveBmpSuccess;
    }

    /**
     * savePicAlbum: save input bitmap to album.
     *
     * @param - ctx
     *          - url, gif url, which will be used to generate saved name.
     * @return - sSaveBmpSuccess, sSaveBmpAlreadyExist, sSaveBmpFailed or sSaveBmpNull
     */
    public static int saveGifToAlbum(Context ctx, String url) {
        if (TextUtils.isEmpty(url)) {
            return sSaveBmpNull;
        }

        String srcFileName = VolleyHelper.getInstance().getFileLoader().getFileFromCache(url);
        if (TextUtils.isEmpty(srcFileName)) {
            return sSaveBmpNull;
        }

        File file = new File(PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        String dstFileName = MD5.hexdigest(url).substring(0, 10) + ".gif";
        String path = PATH + dstFileName;
        try {
            copy(new File(srcFileName), new File(String.format(Locale.getDefault(), "%s/%s", PATH, dstFileName)));
            addFile2MediaDb(ctx, path);
            return sSaveBmpSuccess;
        } catch (IOException e) {
            e.printStackTrace();
            return sSaveBmpFailed;
        }
    }

    /**
     *
     * @param ctx
     * @param gifPath
     * @return
     */
    public static int saveVideoGifToAlbum(Context ctx, String gifPath) {
        if (TextUtils.isEmpty(gifPath)) {
            return sSaveBmpNull;
        }

        File file = new File(PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        String dstFileName = DateUtils.formatDate(new Date()) + ".gif";
        String path = PATH + dstFileName;
        try {
            copy(new File(gifPath), new File(String.format(Locale.getDefault(), "%s/%s", PATH, dstFileName)));
            addFile2MediaDb(ctx, path);
            return sSaveBmpSuccess;
        } catch (IOException e) {
            e.printStackTrace();
            return sSaveBmpFailed;
        }
    }

    /**
     * copy: copy file from src to dst.
     *
     * @param - src: source path.
     *          - dst: destination path.
     */
    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            in.close();
            out.close();
        }
    }

    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] lsFiles = file.listFiles();
                for (File f : lsFiles) {
                    deleteFile(f.getAbsolutePath());
                }
            } else {
                file.delete();
            }
        }
    }

    /**
     * addFile2MediaDb: send {@value Intent#ACTION_MEDIA_SCANNER_SCAN_FILE} broadcast to notify
     * system update media database.
     * <p/>
     * Call this method to show items in Gallary immediately after save file to
     * sdcard.
     */
    public static void addFile2MediaDb(Context ctx, String path) {
        Uri newFileUri = Uri.fromFile(new File(path));
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(newFileUri);
        ctx.sendBroadcast(intent);
    }

    /**
     * Close a Closeable object safely
     *
     * @param object
     * @author xuegang
     * @version Created: 2014年11月25日 上午11:48:29
     */
    public static void safeClose(Closeable object) {
        if (null != object) {
            try {
                object.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取SDCard总大小
     *
     * @return
     */
    public static long getSDCardTotalSize() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(sdcardDir.getPath());
            return sf.getBlockSize() * (sf.getBlockCount() / 1024);
        }
        return 0;
    }

    /**
     * 获取SDCard剩余大小
     *
     * @return(单位：k)
     */
    public static long getSDCardRemainSize() {
        try {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File sdcardDir = Environment.getExternalStorageDirectory();
                StatFs sf = new StatFs(sdcardDir.getPath());
                return sf.getAvailableBlocks() * (sf.getBlockSize() / 1024);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }

        return 0;
    }

    /**
     * SD卡是否可用
     *
     * @return
     */
    public static boolean isSDCardUsable() {
        try {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return false;
    }

    public static long getFileSize(File file) {
        long size = 0;
        if (file != null && file.isFile() && file.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                try {
                    size = fis.available();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                safeClose(fis);
            }
        } else {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e("获取文件大小", "文件不存在!");
        }
        return size;
    }

    /**
     * 压缩整个文件夹中的所有文件，生成指定名称的zip压缩包
     *
     * @param filePath 文件所在目录
     * @param zipPath  压缩后zip文件名称
     * @param dirFlag  zip文件中第一层是否包含一级目录，true包含；false没有
     */
    public static void ZipFolder(String filePath, String zipPath, boolean dirFlag) {
        ZipOutputStream zipOut = null;
        try {
            File file = new File(filePath);// 要被压缩的文件夹
            File zipFile = new File(zipPath);
            zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File fileSec : files) {
                    if (dirFlag) {
                        zipFile(zipOut, fileSec, file.getName() + File.separator);
                    } else {
                        zipFile(zipOut, fileSec, "");
                    }
                }
            }
            zipOut.flush();
            zipOut.finish();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            safeClose(zipOut);
        }
    }

    private static void zipFile(ZipOutputStream zipOut, File file, String baseDir) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File fileSec : files) {
                zipFile(zipOut, fileSec, baseDir + file.getName() + File.separator);
            }
        } else {
            byte[] buf = new byte[1024];
            InputStream input = null;
            try {
                input = new FileInputStream(file);
                zipOut.putNextEntry(new ZipEntry(baseDir + file.getName()));
                int len;
                while ((len = input.read(buf)) != -1) {
                    zipOut.write(buf, 0, len);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                safeClose(input);
            }
        }
    }

    /**
     * 把bitmap,png格式的图片 转换成jpg图片
     * 因jpg不支持透明，如png透明图片，则转成白底！
     *
     * @param bitmap     源图
     * @param newImgpath 新图片的路径
     */
    public static void saveJPG_After(Bitmap bitmap, String newImgpath) {
        //复制Bitmap  因为png可以为透明，jpg不支持透明，把透明底明变成白色
        //主要是先创建一张白色图片，然后把原来的绘制至上去
//        Bitmap outB=bitmap.copy(Bitmap.Config.ARGB_8888,true);
//        Canvas canvas=new Canvas(outB);
//        canvas.drawColor(Color.WHITE);
//        canvas.drawBitmap(bitmap, 0, 0, null);
        File file = new File(newImgpath);
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
