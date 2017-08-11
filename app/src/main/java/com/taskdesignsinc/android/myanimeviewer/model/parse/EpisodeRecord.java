package com.taskdesignsinc.android.myanimeviewer.model.parse;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;

import java.util.Date;

@ParseClassName("Episode")
public class EpisodeRecord extends ParseObject {

    long id;
    AnimeRecord anime;
    int index;
    String title;
    String url;
    Date date;
    String videoUrl;
    boolean isViewed = false;
    String localPath;
    int playPosition = 0;
    String animeUrl;

    public void setAnime(AnimeRecord anime) {
        this.anime = anime;
    }

    public static EpisodeRecord fromEpisode(Episode episode) {
        EpisodeRecord record = new EpisodeRecord();
        record.put("title", episode.getTitle());
        record.put("url", episode.getUrl());
        record.put("index", episode.getIndex());
        record.put("animeUrl", episode.getAnimeUrl());
        return record;
    }

    public static Episode fromEpisode(EpisodeRecord record) {
        Episode episode = new Episode();
        episode.setTitle(record.getString("title"));
        episode.setUrl(record.getString("url"));
        episode.setIndex(record.getInt("index"));
        episode.setAnimeUrl(record.getString("animeUrl"));
        return episode;
    }

    public static ParseQuery<EpisodeRecord> getQuery() {
        return ParseQuery.getQuery(EpisodeRecord.class);
    }

}
