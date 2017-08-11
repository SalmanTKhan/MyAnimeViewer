package com.taskdesignsinc.android.myanimeviewer.model.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.taskdesignsinc.android.myanimeviewer.AnimeDetailsActivity;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.util.AsyncTaskUtils;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.FileUtils;
import com.taskdesignsinc.android.myanimeviewer.util.StorageUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Salman T. Khan on 11/30/2015.
 */
public class AnimeUtils {

    public final static String mTag = AnimeUtils.class.getSimpleName();

    public synchronized static void saveAsync(Anime... pAnime) {
        if (pAnime == null || pAnime.length == 0)
            return;
        AsyncTask<Anime, Void, Void> task = new AsyncTask<Anime, Void, Void>() {

            @Override
            protected Void doInBackground(Anime... params) {
                Anime lAnime = null;
                for (int i = 0; i < params.length; i++) {
                    lAnime = params[i];
                    if (lAnime == null)
                        continue;
                    File pFileName = new File(
                            StorageUtils.getDataDirectory(),
                            FileUtils.getValidFileName(lAnime.getTitle())
                                    + "/"
                                    + "anime.json");
                    if (!pFileName.getParentFile().exists())
                        continue;
                    try {
                        Gson gson = new Gson();
                        FileWriter writer = new FileWriter(pFileName.getAbsolutePath());
                        // convert java object to JSON format,
                        // and returned as JSON formatted string
                        String json = gson.toJson(lAnime);
                        WriteLog.appendLog(mTag, "saveAsync called" + pFileName.getAbsolutePath() + "\n" + json);
                        writer.write(json);
                        writer.close();
                    } catch (IOException e) {
                        WriteLog.appendLog(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }
                }
                return null;
            }

        };
        AsyncTaskUtils.executeAsyncTask(task, pAnime);
    }

    public static void saveAsync(final Context context, Anime... pAnime) {
        if (pAnime == null || pAnime.length == 0)
            return;
        AsyncTask<Anime, Void, Void> task = new AsyncTask<Anime, Void, Void>() {

            @Override
            protected Void doInBackground(Anime... params) {
                Anime lAnime = null;
                for (int i = 0; i < params.length; i++) {
                    lAnime = params[i];
                    if (lAnime == null)
                        continue;
                    File lFileName = new File(
                            StorageUtils.getDataDirectory(),
                            FileUtils.getValidFileName(lAnime.getTitle())
                                    + "/"
                                    + "anime.json");
                    if (!lFileName.getParentFile().exists())
                        continue;
                    try {
                        Gson gson = new Gson();
                        FileWriter writer = new FileWriter(lFileName.getAbsolutePath());
                        // convert java object to JSON format,
                        // and returned as JSON formatted string
                        String json = gson.toJson(lAnime);
                        writer.write(json);
                        writer.close();
                        /**
                         if (DropboxManager.getInstance(context).isSyncLibraryEnabled())
                         {
                         String lAnimeFile = FileUtils.getValidFileName(lAnime.getTitle())
                         + "/"
                         + "manga.json";
                         DropboxManager.getInstance(context).storeFile(lAnimeFile, lFileName);
                         }
                         **/
                    } catch (IOException e) {
                        WriteLog.appendLog(Log.getStackTraceString(e));
                        e.printStackTrace();
                    }
                }
                return null;
            }

        };
        AsyncTaskUtils.executeAsyncTask(task, pAnime);
    }

    public static void viewAnime(Activity activity, String animeUrl) {
        viewAnime(activity, animeUrl, null, null);
    }

    public static void viewAnime(@NonNull Activity activity, @NonNull Anime anime, @Nullable View transitionView, @Nullable String transitionName) {
        viewAnime(activity, anime.getUrl(), transitionView, transitionName);
    }

    public static void viewAnime(Context context, AppCompatImageView imageView, Anime anime) {
        if (context == null) {
            WriteLog.appendLog("viewAnime called with null context");
            return;
        }
        if (anime == null) {
            WriteLog.appendLog("viewAnime called with null data");
            return;
        }
        String transitionName = context.getString(R.string.transition_anime_cover);
        Intent lIntent = new Intent();
        lIntent.setClass(context, AnimeDetailsActivity.class);
        lIntent.putExtra(Constants.ANIME_ID, anime.getId());
        lIntent.putExtra(Constants.ANIME_URL, anime.getUrl());
        //TransitionManager.setTransitionName(imageView, transitionName);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation((AppCompatActivity) context, Pair.create((View) imageView, transitionName));
        ActivityCompat.startActivity(context, lIntent, options.toBundle());
        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).overridePendingTransition(0, 0);
        }
    }

    public static void viewAnime(@NonNull Activity activity, @NonNull String animeUrl, @Nullable View transitionView, @Nullable String transitionName) {
        Intent lIntent = new Intent(activity, AnimeDetailsActivity.class);
        lIntent.putExtra(Constants.ANIME_ID, animeUrl.hashCode());
        lIntent.putExtra(Constants.ANIME_URL, animeUrl);
        if (transitionView != null && !TextUtils.isEmpty(transitionName)) {
            ActivityOptionsCompat options =
                    ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                            transitionView,   // The view which starts the transition
                            transitionName    // The transitionName of the view we’re transitioning to
                    );
            ActivityCompat.startActivity(activity, lIntent, options.toBundle());
        } else
            ActivityCompat.startActivity(activity, lIntent, null);
    }
}
