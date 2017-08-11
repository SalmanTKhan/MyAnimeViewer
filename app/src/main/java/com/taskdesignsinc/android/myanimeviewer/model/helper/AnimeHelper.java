package com.taskdesignsinc.android.myanimeviewer.model.helper;

import android.text.TextUtils;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;

/**
 * Created by salma on 7/26/2017.
 */

public class AnimeHelper {

    public static void update(Anime animeSource, Anime animeDestination) {
        if (!TextUtils.isEmpty(animeSource.getUrl()))
            animeDestination.setUrl(animeSource.getUrl());
        if (animeSource.getStatus() != -1)
            animeDestination.setStatus(animeSource.getStatus());
        if (!TextUtils.isEmpty(animeSource.getTitle()))
            animeDestination.setTitle(animeSource.getTitle());
        if (!TextUtils.isEmpty(animeSource.getGenres()))
            animeDestination.setGenres(animeSource.getGenres());
        if (animeSource.getEpisodes() != null && !animeSource.getEpisodes().isEmpty()) {
            for (Episode episode : animeSource.getEpisodes()) {
                Episode cachedEpisode = MAVApplication.getInstance().getRepository().getEpisodeByUrl(episode.getUrl());
                if (cachedEpisode != null)
                    episode.setId(cachedEpisode.getId());
                episode.setAnime(animeDestination);
                animeDestination.getEpisodes().add(episode);
            }
        }
    }
}
