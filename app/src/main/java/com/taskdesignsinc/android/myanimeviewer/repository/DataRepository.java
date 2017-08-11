package com.taskdesignsinc.android.myanimeviewer.repository;

import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.DownloadRecord;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteRecord;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteTag;
import com.taskdesignsinc.android.myanimeviewer.model.HistoryRecord;
import com.taskdesignsinc.android.myanimeviewer.model.OfflineHistoryRecord;

import java.util.HashMap;
import java.util.List;

/**
 * Created by salma on 7/25/2017.
 */

public interface DataRepository {

    //Anime
    long getAnimeCount();
    HashMap<String,Anime> getAnimeMap();
    List<Anime> getAnimeList();
    HashMap<String,Anime> getAnimeMapByUrl(String url);
    List<Anime> getAnimeListByUrl(String url);
    List<Anime> getAnimeListByTitle(String title);
    List<Anime> getAnimeListByTitle(String title, String url);
    void insertAnimeList(List<Anime> animeList);
    void updateAnimeList(List<Anime> animeList);
    Anime getAnimeByUrl(String animeUrl);
    Anime getAnimeByUrl(String animeUrl, boolean loadEpisodes);
    String getCoverByUrl(String animeUrl);
    void insertAnime(Anime anime);
    void updateAnime(Anime anime);
    void deleteAnime(String animeUrl);

    //Episode
    void insertEpisodeList(List<Episode> episodes);
    void deleteEpisodeList(Anime anime);
    void deleteEpisodes(String animeUrl);
    void updateEpisode(Episode episode);
    List<Episode> getEpisodes(String animeUrl);
    Episode getEpisodeByUrl(String episodeUrl);
    int getEpisodeCount(String animeUrl);
    String getEpisodeTitle(String url);
    String getLastViewedEpisode(String animeUrl);

    //FavoriteRecord
    List<FavoriteRecord> getFavorites();
    void insertFavorite(String animeUrl, int tagId);
    void deleteFavorite(String animeUrl);

    //FavoriteTag
    FavoriteTag getFavoriteTag(int id);
    List<FavoriteTag> getFavoriteTags();
    void insertFavoriteTag(int tagId, String title, int ribbonId);
    boolean deleteFavoriteTag(int tagId);

    //FavoriteRecord
    FavoriteRecord getFavoriteByAnimeUrl(String url);
    boolean isFavorite(String animeUrl);
    void deleteFavoriteRecordsByTagID(int tagId);
    boolean deleteFavoriteRecords();

    void insertHistoryRecord(String animeUrl, String episodeUrl, boolean isViewed);
    HistoryRecord getHistoryRecord(String episodeUrl);
    void deleteHistoryRecord(String episodeUrl);
    void deleteHistoryRecords();
    List<HistoryRecord> getHistoryRecords();
    List<HistoryRecord> getHistoryRecords(String animeUrl);

    List<DownloadRecord> getDownloadTasks(boolean isCompleted);
    DownloadRecord getDownloadTask(String episodeUrl, boolean isCompleted);
    void deleteDownloadTask(String episodeUrl);
    void deleteIncompleteDownloadTasks();
    DownloadRecord getDownloadTask(String episodeUrl);
    void insertDownloadTask(DownloadRecord record);
    void updateDownloadTask(DownloadRecord record);

    void deleteOfflineHistoryRecord(String path);
    void deleteOfflineHistoryRecords();
    OfflineHistoryRecord getOfflineHistoryRecord(String path);
    List<OfflineHistoryRecord> getOfflineHistoryRecords();
    void insertOfflineHistoryRecord(String path, boolean isViewed);
    Boolean isPathViewed(String path);
    String getLastViewedByPath(String parent);
}
