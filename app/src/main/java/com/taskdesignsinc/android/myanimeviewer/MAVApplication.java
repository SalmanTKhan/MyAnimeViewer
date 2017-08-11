package com.taskdesignsinc.android.myanimeviewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.taskdesignsinc.android.myanimeviewer.model.MyObjectBox;
import com.taskdesignsinc.android.myanimeviewer.repository.DataRepository;
import com.taskdesignsinc.android.myanimeviewer.repository.ObjectBoxRepository;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.ImageLoaderManager;
import com.taskdesignsinc.android.myanimeviewer.util.ParseManager;
import com.taskdesignsinc.android.myanimeviewer.util.StorageUtils;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import java.io.File;

import io.objectbox.BoxStore;
import timber.log.Timber;

/**
 * Created by salma on 7/20/2017.
 */
public class MAVApplication extends MultiDexApplication {

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static MAVApplication application = null;

    private RefWatcher refWatcher;
    private BoxStore boxStore;
    private DataRepository mDataRepository;
    private SharedPreferences mPrefs;

    public static MAVApplication getInstance() {
        return application;
    }

    public static File getCacheDirectory() {
        return application.getCacheDir();
    }

    public static RefWatcher getRefWatcher(Context context) {
        MAVApplication application = (MAVApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }

    public DataRepository getRepository() {
        return mDataRepository;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        application = this;

        //ACRA.init(this);
        initializeLeakCanary();
        initializeTimber();
        initializeObjectBox();
        ImageLoaderManager.getInstance(application);
        mDataRepository = new ObjectBoxRepository();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        setupDataPaths();
        ParseManager.getInstance(getApplicationContext());
    }

    private void initializeObjectBox() {
        boxStore = MyObjectBox.builder().androidContext(this).build();
    }

    private void initializeLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        refWatcher = LeakCanary.install(this);
    }

    private void initializeTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree() {
                // Add the line number to the tag
                @Override
                protected String createStackElementTag(StackTraceElement element) {
                    return String.format("[Line - %s] [Method - %s] [Class - %s]",
                            element.getLineNumber(),
                            element.getMethodName(),
                            super.createStackElementTag(element));
                }
            });
        } else {
            Timber.plant(new ReleaseTree());
        }
    }

    private void setupDataPaths() {
        String dataDirPath = mPrefs.getString(Constants.KEY_LIBRARY_PATH, "");
        File file = null;
        if (TextUtils.isEmpty(dataDirPath)) {
            file = new File(mPrefs.getBoolean(Constants.KEY_LIBRARY_FORCE2SD, false) ?
                    StorageUtils.getExtSDCard(this) : Environment.getExternalStorageDirectory(),
                    StorageUtils.DEFAULT_LIBRARY_FOLDER);
            file.mkdirs();
            dataDirPath = file.getAbsolutePath();
            mPrefs.edit().putString(Constants.KEY_LIBRARY_PATH, dataDirPath).apply();
        }
        if (!TextUtils.isEmpty(dataDirPath))
            StorageUtils.setDataDirectory(new File(dataDirPath));
        if (StorageUtils.getDataDirectory() == null)
            StorageUtils.setDataDirectory(new File(mPrefs.getBoolean(Constants.KEY_LIBRARY_FORCE2SD, false) ?
                    StorageUtils.getExtSDCard(this) : Environment.getExternalStorageDirectory(), StorageUtils.DEFAULT_LIBRARY_FOLDER));

        if (StorageUtils.getCacheDir() == null) {
            StorageUtils.setCacheDir(new File(StorageUtils.getExtSDCard(this), StorageUtils.DEFAULT_LIBRARY_FOLDER + "Cache/"));
        }
    }

    public SharedPreferences getPreferences() {
        return mPrefs;
    }

    /**
     * A tree which logs important information for crash reporting.
     */
    private static class ReleaseTree extends Timber.Tree {

        private static final int MAX_LOG_LENGTH = 4000;

        @Override
        protected boolean isLoggable(int priority) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return false;
            }

            // Only log WARN, INFO, ERROR, WTF
            return true;
        }

        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (isLoggable(priority)) {
                // Message is short enough, does not need to be broken into chunks
                if (message.length() < MAX_LOG_LENGTH) {
                    if (priority == Log.ASSERT) {
                        Log.wtf(tag, message);
                    } else {
                        Log.println(priority, tag, message);
                    }
                    return;
                }

                // Split by line, then ensure each line can fit into Log's maximum length
                for (int i = 0, length = message.length(); i < length; i++) {
                    int newline = message.indexOf('\n', i);
                    newline = newline != -1 ? newline : length;
                    do {
                        int end = Math.min(newline, i + MAX_LOG_LENGTH);
                        String part = message.substring(i, end);
                        if (priority == Log.ASSERT) {
                            Log.wtf(tag, part);
                        } else {
                            Log.println(priority, tag, part);
                        }
                        i = end;
                    } while (i < newline);
                }
            }
        }
    }
}
