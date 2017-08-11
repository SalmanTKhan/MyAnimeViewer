package com.taskdesignsinc.android.myanimeviewer.loader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.helper.AnimeHelper;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.NetworkUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

/**
 * A custom Loader that loads all of the installed applications.
 */
public class AnimeLoader extends AsyncTaskLoader<Anime> {

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
        final AnimeLoader mLoader;

        public AnimeIntentReceiver(AnimeLoader loader) {
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

    Anime mAnime;
    AnimeIntentReceiver mPackageObserver;
    Parser mParser;
    String url;

    public AnimeLoader(Context context, Parser parser, String url) {
        super(context);
        mParser = parser;
        this.url = url;
    }

    /**
     * This is where the bulk of our work is done.  This function is
     * called in a background thread and should generate a new set of
     * data to be published by the loader.
     */
    @Override
    public Anime loadInBackground() {
        WriteLog.appendLog("loadInBackground() called on " + url);
        final Context context = getContext();

        // Create corresponding array of entries and load their labels.
        if (NetworkUtils.isNetworkAvailable(context)) {
            if (!TextUtils.isEmpty(url)) {
                Anime cachedAnime = MAVApplication.getInstance().getRepository().getAnimeByUrl(url, false);
                Anime parsedAnime = mParser.getAnimeDetails(url);
                parsedAnime.setEpisodeCount(parsedAnime.getEpisodes().size());
                int previousEpisodeCount = cachedAnime != null ? cachedAnime.getEpisodeCount() : 0;
                if (previousEpisodeCount != 0)
                    parsedAnime.setNewEpisodes(parsedAnime.getEpisodeCount() - previousEpisodeCount);
                for (int i = 0; i < parsedAnime.getEpisodes().size(); i++) {
                    parsedAnime.getEpisodes().get(i).setIndex(i);
                }
                if (cachedAnime == null) {
                    for (Episode episode : parsedAnime.getEpisodes())
                        episode.setAnime(parsedAnime);
                    MAVApplication.getInstance().getRepository().insertAnime(parsedAnime);
                    MAVApplication.getInstance().getRepository().insertEpisodeList(parsedAnime.getEpisodes());
                } else {
                    parsedAnime.setId(cachedAnime.getId());
                    for (Episode episode : parsedAnime.getEpisodes())
                        episode.setAnime(cachedAnime);
                    AnimeHelper.update(parsedAnime, cachedAnime);
                    MAVApplication.getInstance().getRepository().updateAnime(cachedAnime);
                    MAVApplication.getInstance().getRepository().insertEpisodeList(parsedAnime.getEpisodes());
                    //MAVApplication.getInstance().getRepository().deleteEpisodeList(cachedAnime);
                }
                return parsedAnime;
            }
        }

        // Done!
        return null;
    }

    /**
     * Called when there is new data to deliver to the client.  The
     * super class will take care of delivering it; the implementation
     * here just adds a little more logic.
     */
    @Override
    public void deliverResult(Anime apps) {
        if (isReset()) {
            // An async query came in while the loader is stopped.  We
            // don't need the result.
            if (apps != null) {
                onReleaseResources(apps);
            }
        }
        Anime oldApps = apps;
        mAnime = apps;

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
        if (mAnime != null) {
            // If we currently have a result available, deliver it
            // immediately.
            deliverResult(mAnime);
        }

        // Start watching for changes in the app data.
        if (mPackageObserver == null) {
            mPackageObserver = new AnimeIntentReceiver(this);
        }

        // Has something interesting in the configuration changed since we
        // last built the app list?
        boolean configChange = mLastConfig.applyNewConfig(getContext().getResources());

        if (takeContentChanged() || mAnime == null || configChange) {
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
    public void onCanceled(Anime apps) {
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
        if (mAnime != null) {
            onReleaseResources(mAnime);
            mAnime = null;
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
    protected void onReleaseResources(Anime apps) {
        // For a simple List<> there is nothing to do.  For something
        // like a Cursor, we would close it here.
    }
}