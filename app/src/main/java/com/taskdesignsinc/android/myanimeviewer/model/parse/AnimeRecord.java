package com.taskdesignsinc.android.myanimeviewer.model.parse;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;

@ParseClassName("Anime")
public class AnimeRecord extends ParseObject {

    public static AnimeRecord fromAnime(Anime anime) {
        AnimeRecord record = new AnimeRecord();
        record.put("title", anime.getTitle());
        record.put("url", anime.getUrl());
        record.put("cover", anime.getCover());
        record.put("type", anime.getType());
        record.put("genres", anime.getGenres());
        record.put("summary", anime.getSummary());
        record.put("creator", anime.getCreator());
        if (anime.getEpisodeCount() == -1)
            anime.setEpisodeCount(anime.getEpisodes().size());
        record.put("episodeCount", anime.getEpisodeCount());
        return record;
    }

    public static Anime toAnime(AnimeRecord record) {
        Anime anime = new Anime();
        anime.setTitle(record.getString("title"));
        anime.setUrl(record.getString("url"));
        anime.setCover(record.getString("cover"));
        anime.setType(record.getString("type"));
        anime.setGenres(record.getString("genres"));
        anime.setSummary(record.getString("summary"));
        anime.setCreator(record.getString("creator"));
        anime.setEpisodeCount(record.getInt("episodeCount"));
        return anime;
    }

    public static ParseQuery<AnimeRecord> getQuery() {
        return ParseQuery.getQuery(AnimeRecord.class);
    }
}
