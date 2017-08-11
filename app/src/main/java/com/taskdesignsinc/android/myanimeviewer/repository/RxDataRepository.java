package com.taskdesignsinc.android.myanimeviewer.repository;

import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;

import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;
import io.objectbox.reactive.DataSubscription;

/**
 * Created by salma on 7/25/2017.
 */

public interface RxDataRepository {

    //Anime
    Query<Anime> queryAnimeList(String serverUrl);

    //Episode
    Query<Episode> queryEpisodes(String animeUrl);

}
