package com.taskdesignsinc.android.myanimeviewer.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.taskdesignsinc.android.myanimeviewer.R;

import java.io.File;
import java.io.IOException;

public class StorageUtils {

    public static String DEFAULT_LIBRARY_FOLDER = "My Anime Viewer/";

    private static File cacheDir = null;
    private static File dataDir = null;
    private static File backupDir = null;
    private static File searchDataFile = null;
    private static String lastPath = "";

    public static final String ERROR = "No Space";
    private static boolean storageAllowed = false;

    static {
        if (!BuildUtils.isMarshmallowOrLater())
            StorageUtils.storageAllowed = true;
    }

    /**
     * @return the cacheDir
     */
    public static File getCacheDir() {
        return cacheDir;
    }

    /**
     * @param pCacheDir The cache directory
     */
    public static void setCacheDir(File pCacheDir) {
        StorageUtils.cacheDir = pCacheDir;
    }

    public static File getExtSDCard(Context c) {
        File externalFolder = null;
        File[] test = ContextCompat.getExternalCacheDirs(c.getApplicationContext());
        if (test != null && test.length > 0) {
            externalFolder = test[test.length - 1];
            if (externalFolder != null) {
                if (externalFolder.getAbsolutePath().contains("cache"))
                    externalFolder = externalFolder.getParentFile();
            }
        }
        return externalFolder;
    }

    /**
     * @param pDirectory The data directory
     */
    public static void setDataDirectory(File pDirectory) {
        dataDir = pDirectory;
        if (dataDir != null) {
            if (!dataDir.exists())
                dataDir.mkdirs();
            File nomediaFile = new File(dataDir, ".nomedia");
            if (!nomediaFile.exists())
                try {
                    if (nomediaFile.createNewFile())
                        WriteLog.appendLog(nomediaFile.getAbsolutePath() + " file created.");
                } catch (IOException e) {
                    WriteLog.appendLog(nomediaFile.getAbsolutePath() + " failed to be created.");
                    WriteLog.appendLog(Log.getStackTraceString(e));
                }
            else {
                WriteLog.appendLog(nomediaFile.getAbsolutePath() + " file exists.");
            }
            backupDir = new File(
                    StorageUtils.getDataDirectory(),
                    "Backup/");
            if (!backupDir.exists())
                backupDir.mkdirs();
        }
    }

    public static File getDataDirectory() {
        return dataDir;
    }

    public static File getBackupDirectory() {
        return backupDir;
    }

    /**
     * @return the lastPath
     */
    public static String getLastPath() {
        return lastPath;
    }

    /**
     * @param mLastPath the lastPath to set
     */
    public static void setLastPath(String mLastPath) {
        StorageUtils.lastPath = mLastPath;
    }

    /**
     * @return the searchDataFile
     */
    public static File getSearchDataFile() {
        return searchDataFile;
    }

    /**
     * @param mSearchDataFile the searchDataFile to set
     */
    public static void setSearchDataFile(File mSearchDataFile) {
        StorageUtils.searchDataFile = mSearchDataFile;
    }

    public static long getAvailableStorage() {

        String storageDirectory = null;
        storageDirectory = Environment.getExternalStorageDirectory().toString();

        try {
            StatFs stat = new StatFs(storageDirectory);
            long avaliableSize = ((long) stat.getAvailableBlocks() * (long) stat.getBlockSize());
            return avaliableSize;
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    public static void requestStoragePermissions(Context context) {
        PermissionListener dialogPermissionListener =
                DialogOnDeniedPermissionListener.Builder
                        .withContext(context)
                        .withTitle("Write to Storage Permission")
                        .withMessage("Storage permission is required to save any manga to your phone.")
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.mipmap.ic_launcher)
                        .build();
        Dexter.withActivity((AppCompatActivity) context).withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(dialogPermissionListener).check();
    }

    public static void requestStoragePermissions(Context context, PermissionListener listener) {
        Dexter.withActivity((Activity) context).withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(listener).check();
    }

    public static void requestStoragePermissions(PermissionListener listener) {
        if (BuildUtils.isMarshmallowOrLater()) {
            //if (!Dexter.isRequestOngoing()) Dexter.checkPermission(listener, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            storageAllowed = true;
        }
    }

    public static boolean isStorageAllowed() {
        return storageAllowed;
    }

    public static void setStorageAllowed(boolean storageAllowed) {
        StorageUtils.storageAllowed = storageAllowed;
    }

    public static boolean isSDCardPresent() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static boolean isSDCardWritable() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }
}