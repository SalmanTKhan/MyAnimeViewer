package com.taskdesignsinc.android.myanimeviewer.loader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v4.content.AsyncTaskLoader;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.helper.AnimeHelper;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.NetworkUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class AnimeListLoader extends AsyncTaskLoader<ArrayList<Anime>> {

    public static final String mTag = AnimeListLoader.class.getSimpleName();

    /**
     * Helper for determining if the configuration has changed in an interesting
     * way so we need to rebuild the app list.
     */
    public static class InterestingConfigChanges {
        final Configuration mLastConfiguration = new Configuration();
        int mLastDensity;

        boolean applyNewConfig(Resources res) {
            int configChanges = mLastConfiguration.updateFrom(res.getConfiguration());
            boolean densityChanged = mLastDensity != res.getDisplayMetrics().densityDpi;
            if (densityChanged || (configChanges & (ActivityInfo.CONFIG_LOCALE
                    | ActivityInfo.CONFIG_UI_MODE | ActivityInfo.CONFIG_SCREEN_LAYOUT)) != 0) {
                mLastDensity = res.getDisplayMetrics().densityDpi;
                return true;
            }
            return false;
        }
    }

    /**
     * Helper class to look for interesting changes to the installed apps
     * so that the loader can be updated.
     */
    public static class AnimeIntentReceiver extends BroadcastReceiver {
        final AnimeListLoader mLoader;

        public AnimeIntentReceiver(AnimeListLoader loader) {
            mLoader = loader;
            IntentFilter filter = new IntentFilter(Constants.Intents.ANIME_UPDATED);
            mLoader.getContext().registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Tell the loader about the change.
            mLoader.onContentChanged();
        }
    }

    final InterestingConfigChanges mLastConfig = new InterestingConfigChanges();

    private ArrayList<Anime> mApps;
    private AnimeIntentReceiver mPackageObserver;
    private Parser mParser;
    private String mURL;
    private boolean mReturnAll;

    public AnimeListLoader(Context context, Parser parser, String url, boolean returnAll) {
        super(context);
        mParser = parser;
        mURL = url;
        mReturnAll = returnAll;
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override
    public ArrayList<Anime> loadInBackground() {
        WriteLog.appendLog(mTag, "loadInBackground called");
        final Context context = getContext();

        // Create corresponding array of entries and load their labels.
        List<Anime> entries = NetworkUtils.isNetworkAvailable(context) ? mParser.getAnimeList(mURL) : null;
        List<Anime> lNonCachedTitles = new ArrayList<Anime>();
        List<Anime> lCachedTitles = new ArrayList<Anime>();

        if (entries != null && !entries.isEmpty()) {
            HashMap<String, Anime> animeMap = MAVApplication.getInstance().getRepository().getAnimeMapByUrl(mParser.getServerUrl());
            for (Anime entry : entries) {
                if (animeMap.containsKey(entry.getUrl())) {
                    Anime cachedAnime = animeMap.get(entry.getUrl());
                    AnimeHelper.update(entry, cachedAnime);
                    lCachedTitles.add(cachedAnime);
                } else {
                    lNonCachedTitles.add(entry);
                }
            }
            if (!lNonCachedTitles.isEmpty())
                MAVApplication.getInstance().getRepository().insertAnimeList(lNonCachedTitles);
            if (!lCachedTitles.isEmpty()) {
                MAVApplication.getInstance().getRepository().updateAnimeList(lCachedTitles);
            }
        }

        if (entries != null)
            WriteLog.appendLog(mTag, entries.size() + " anime loaded from " + mURL);
        else
            WriteLog.appendLog(mTag, "No internet connection");
        WriteLog.appendLog(mTag, lNonCachedTitles.size() + " new anime loaded from " + mURL);
        WriteLog.appendLog(mTag, lCachedTitles.size() + " cached anime loaded from " + mURL);

        // Done!
        if (mReturnAll)
            return (ArrayList<Anime>) entries;
        else
            return (ArrayList<Anime>) lNonCachedTitles;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(ArrayList<Anime> apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        ArrayList<Anime> oldApps = apps;
        mApps = apps;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(apps);
        }

        // At this point we can release the resources associated with
        // 'oldApps' if needed; now that the new result is delivered we
        // know that it is no longer in use.
        if (oldApps != null) {
            onReleaseResources(oldApps);
        }
    }

    /**
     * Handles a request to start the Loader.
     */
    @Override
    protected void onStartLoading() {
        if (mApps != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mApps);
        }

        // Start watching for changes in the app data.
        if (mPackageObserver == null) {
            mPackageObserver = new AnimeIntentReceiver(this);
        }

        // Has something interesting in the configuration changed since we
        // last built the app list?
        boolean configChange = mLastConfig.applyNewConfig(getContext().getResources());

        if (takeContentChanged() || mApps == null || configChange) {
            // If the data has changed since the last time it was loaded
            // or is not currently available, start a load.
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(ArrayList<Anime> apps) {
        super.onCanceled(apps);

        // At this point we can release the resources associated with 'apps'
        // if needed.
        onReleaseResources(apps);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release the resources associated with 'apps'
        // if needed.
        if (mApps != null) {
            onReleaseResources(mApps);
            mApps = null;
        }

        // Stop monitoring for changes.
        if (mPackageObserver != null) {
            getContext().unregisterReceiver(mPackageObserver);
            mPackageObserver = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated
     * with an actively loaded data set.
     */
    protected void onReleaseResources(ArrayList<Anime> apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }
}