package com.taskdesignsinc.android.myanimeviewer.model.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.VideoDetailsActivity;
import com.taskdesignsinc.android.myanimeviewer.VideoPlayerActivity;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.service.download.DownloadService;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

import java.io.File;
import java.util.List;

/**
 * Created by Salman T. Khan on 11/30/2015.
 */
public class EpisodeUtils {

    public static void viewEpisode(String tag, Context context, Anime anime, Episode episode, CoordinatorLayout coordinatorLayout) {
        if (context == null) {
            WriteLog.appendLog(tag, "viewEpisode context is null");
            return;
        }
        if (anime == null) {
            WriteLog.appendLog(tag, "viewEpisode anime is null");
            return;
        }
        if (episode == null) {
            WriteLog.appendLog(tag, "viewEpisode episode is null");
            return;
        }
        WriteLog.appendLog(tag, "viewEpisode() anime: " + anime.toString() + " episode: " + episode.toString());
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(Constants.ANIME_ID, anime.getId());
        intent.putExtra(Constants.ANIME_URL, anime.getUrl());
        intent.putExtra(Constants.EPISODE_TITLE, episode.getTitle());
        intent.putExtra(Constants.EPISODE_URL, episode.getUrl());
        intent.putExtra(Constants.EPISODE_POSITION, episode.getIndex());
        intent.putExtra(Constants.EPISODE_START_POSITION, episode.getPlayPosition());
        context.startActivity(intent);
    }

    public static void viewEpisodeOffline(String tag, Context context, Anime anime, Episode episode, CoordinatorLayout coordinatorLayout) {
        if (!TextUtils.isEmpty(episode.getLocalPath())) {
            File temp = new File(episode.getLocalPath());
            Intent intent = new Intent(context, VideoDetailsActivity.class);
            intent.putExtra("shouldStart", false);
            if (anime != null) {
                intent.putExtra(Constants.ANIME_ID, anime.getId());
                intent.putExtra(Constants.ANIME_URL, anime.getUrl());
                intent.putExtra(Constants.ANIME_TITLE, anime.getTitle());
            }
            intent.putExtra(Constants.EPISODE_TITLE, episode.getTitle());
            intent.putExtra(Constants.EPISODE_URL, episode.getUrl());
            intent.putExtra(Constants.EPISODE_PATH, episode.getLocalPath());
            intent.putExtra(Constants.EPISODE_VIDEO_URL, episode.getLocalPath());
            intent.putExtra(Constants.EPISODE_START_POSITION, episode.getPlayPosition());
            context.startActivity(intent);
        } else {
            if (coordinatorLayout != null)
                Snackbar.make(coordinatorLayout, "Anime not loaded yet.", Snackbar.LENGTH_SHORT).show();
        }

    }

    public static void downloadEpisode(Context context, final Episode episode) {
        Intent downloadIntent = new Intent(context, DownloadService.class);
        downloadIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.ADD);
        downloadIntent.putExtra(Constants.DownloadIntents.URL, episode.getUrl());
        context.startService(downloadIntent);
    }

    public static void downloadEpisodes(@NonNull Activity activity, @NonNull Anime anime) {
        if (activity != null) {
            if (anime != null) {
                List<Episode> episodes = MAVApplication.getInstance().getRepository().getEpisodes(anime.getUrl());

                for (Episode episode : episodes) {
                    downloadEpisode(activity, episode);
                }
            }
        }
    }

    public static void viewEpisode(String tag, Context context, Anime anime, Episode episode) {
        viewEpisode(tag, context, anime, episode, null);
    }
}
