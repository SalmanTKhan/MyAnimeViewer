package com.taskdesignsinc.android.myanimeviewer.util;

import android.content.Context;
import android.text.TextUtils;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.DownloadRecord;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteRecord;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteTag;
import com.taskdesignsinc.android.myanimeviewer.model.HistoryRecord;
import com.taskdesignsinc.android.myanimeviewer.model.OfflineHistoryRecord;
import com.taskdesignsinc.android.myanimeviewer.model.parse.AnimeRecord;
import com.taskdesignsinc.android.myanimeviewer.model.parse.EpisodeRecord;
import com.taskdesignsinc.android.myanimeviewer.repository.DataRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by salma on 8/5/2017.
 */
public class ParseManager implements DataRepository {

    private static String TAG = ParseManager.class.getSimpleName();
    private static ParseManager mInstance;

    public static ParseManager getInstance(Context context) {
        if (TextUtils.isEmpty(context.getString(R.string.parse_server)) || TextUtils.isEmpty(context.getString(R.string.parse_app_id))) {

        } else {
            if (mInstance == null)
                mInstance = new ParseManager(context);
        }
        return mInstance;
    }

    private ParseManager(Context context) {
        loadParseConfig(context);
    }

    private void loadParseConfig(Context context) {
        ParseObject.registerSubclass(AnimeRecord.class);
        ParseObject.registerSubclass(EpisodeRecord.class);
        Parse.initialize(new Parse.Configuration.Builder(context)
                .applicationId(context.getString(R.string.parse_app_id))
                .clientKey("")
                .server(context.getString(R.string.parse_server))
                .enableLocalDataStore()
                .build()
        );
    }

    public List<Anime> getAnimeList(int page) {
        ArrayList<Anime> animeList = new ArrayList<>();
        try {
            ParseQuery<AnimeRecord> query = AnimeRecord.getQuery();
            query.setSkip((page-1) * 100);
            for (AnimeRecord record : query.find()) {
                Anime anime = AnimeRecord.toAnime(record);
                if (anime != null)
                    animeList.add(anime);
            }
        } catch (ParseException e) {
            WriteLog.appendLogException(TAG, "getAnimeList failed", e);
        }
        return animeList;
    }

    @Override
    public List<Anime> getAnimeList() {
        ArrayList<Anime> animeList = new ArrayList<>();
        int limit = 10000;
        int page = 0;
        try {
            boolean endReached = false;
            do {
                ParseQuery<AnimeRecord> query = AnimeRecord.getQuery();
                query.setLimit(limit);
                query.setSkip(page * limit);
                List<AnimeRecord> list = query.find();
                if (list != null) {
                    for (AnimeRecord animeRecord : list) {
                        Anime anime = AnimeRecord.toAnime(animeRecord);
                        if (anime != null)
                            animeList.add(anime);
                    }
                    if (list.size() < limit)
                        endReached = true;
                    else
                        page++;
                } else
                    endReached = true;
            } while (!endReached);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        WriteLog.appendLog(TAG,"Anime loaded from server: " + animeList.size());
        return animeList;
    }

    @Override
    public HashMap<String, Anime> getAnimeMapByUrl(String url) {
        return null;
    }

    @Override
    public List<Anime> getAnimeListByUrl(String url) {
        ArrayList<Anime> animeList = new ArrayList<>();
        ParseQuery<AnimeRecord> query = AnimeRecord.getQuery();
        try {
            List<AnimeRecord> animeRecords = query.whereContains("url", url).find();
            for (AnimeRecord record : animeRecords) {
                Anime anime = AnimeRecord.toAnime(record);
                if (anime != null)
                    animeList.add(anime);
            }
        } catch (ParseException e) {
            WriteLog.appendLogException(TAG, "getAnimeListByUrl(" + url + ") failed", e);
        }
        return animeList;
    }

    @Override
    public List<Anime> getAnimeListByTitle(String title) {
        ArrayList<Anime> animeList = new ArrayList<>();
        ParseQuery<AnimeRecord> query = AnimeRecord.getQuery();
        try {
            List<AnimeRecord> animeRecords = query.whereContains("title", title).find();
            for (AnimeRecord record : animeRecords) {
                Anime anime = AnimeRecord.toAnime(record);
                if (anime != null)
                    animeList.add(anime);
            }
        } catch (ParseException e) {
            WriteLog.appendLogException(TAG, "getAnimeListByTitle(" + title + ") failed", e);
        }
        return animeList;
    }

    @Override
    public List<Anime> getAnimeListByTitle(String title, String url) {
        return null;
    }

    @Override
    public void insertAnimeList(List<Anime> animeList) {

    }

    @Override
    public void updateAnimeList(List<Anime> animeList) {

    }

    @Override
    public Anime getAnimeByUrl(String animeUrl) {
        return null;
    }

    @Override
    public Anime getAnimeByUrl(String animeUrl, boolean loadEpisodes) {
        return null;
    }

    @Override
    public String getCoverByUrl(String animeUrl) {
        return null;
    }

    @Override
    public void insertAnime(Anime anime) {

    }

    @Override
    public void updateAnime(Anime anime) {

    }

    @Override
    public void deleteAnime(String animeUrl) {

    }

    @Override
    public void insertEpisodeList(List<Episode> episodes) {

    }

    @Override
    public void deleteEpisodeList(Anime anime) {

    }

    @Override
    public void deleteEpisodes(String animeUrl) {

    }

    @Override
    public void updateEpisode(Episode episode) {

    }

    @Override
    public List<Episode> getEpisodes(String animeUrl) {
        return null;
    }

    @Override
    public Episode getEpisodeByUrl(String episodeUrl) {
        return null;
    }

    @Override
    public int getEpisodeCount(String animeUrl) {
        return 0;
    }

    @Override
    public String getEpisodeTitle(String url) {
        return null;
    }

    @Override
    public String getLastViewedEpisode(String animeUrl) {
        return null;
    }

    @Override
    public List<FavoriteRecord> getFavorites() {
        return null;
    }

    @Override
    public void insertFavorite(String animeUrl, int tagId) {

    }

    @Override
    public void deleteFavorite(String animeUrl) {

    }

    @Override
    public FavoriteTag getFavoriteTag(int id) {
        return null;
    }

    @Override
    public List<FavoriteTag> getFavoriteTags() {
        return null;
    }

    @Override
    public void insertFavoriteTag(int tagId, String title, int ribbonId) {

    }

    @Override
    public boolean deleteFavoriteTag(int tagId) {
        return false;
    }

    @Override
    public FavoriteRecord getFavoriteByAnimeUrl(String url) {
        return null;
    }

    @Override
    public boolean isFavorite(String animeUrl) {
        return false;
    }

    @Override
    public void deleteFavoriteRecordsByTagID(int tagId) {

    }

    @Override
    public boolean deleteFavoriteRecords() {
        return false;
    }

    @Override
    public void insertHistoryRecord(String animeUrl, String episodeUrl, boolean isViewed) {

    }

    @Override
    public HistoryRecord getHistoryRecord(String episodeUrl) {
        return null;
    }

    @Override
    public void deleteHistoryRecord(String episodeUrl) {

    }

    @Override
    public void deleteHistoryRecords() {

    }

    @Override
    public List<HistoryRecord> getHistoryRecords() {
        return null;
    }

    @Override
    public List<HistoryRecord> getHistoryRecords(String animeUrl) {
        return null;
    }

    @Override
    public List<DownloadRecord> getDownloadTasks(boolean isCompleted) {
        return null;
    }

    @Override
    public DownloadRecord getDownloadTask(String episodeUrl, boolean isCompleted) {
        return null;
    }

    @Override
    public void deleteDownloadTask(String episodeUrl) {

    }

    @Override
    public void deleteIncompleteDownloadTasks() {

    }

    @Override
    public DownloadRecord getDownloadTask(String episodeUrl) {
        return null;
    }

    @Override
    public void insertDownloadTask(DownloadRecord record) {

    }

    @Override
    public void updateDownloadTask(DownloadRecord record) {

    }

    @Override
    public void deleteOfflineHistoryRecord(String path) {

    }

    @Override
    public void deleteOfflineHistoryRecords() {

    }

    @Override
    public OfflineHistoryRecord getOfflineHistoryRecord(String path) {
        return null;
    }

    @Override
    public List<OfflineHistoryRecord> getOfflineHistoryRecords() {
        return null;
    }

    @Override
    public void insertOfflineHistoryRecord(String path, boolean isViewed) {

    }

    @Override
    public Boolean isPathViewed(String path) {
        return null;
    }

    @Override
    public String getLastViewedByPath(String parent) {
        return null;
    }

    public long getAnimeCount() {
        if (mInstance == null)
            return 0;
        ParseQuery<AnimeRecord> query = AnimeRecord.getQuery();
        try {
            return query.count();
        } catch (ParseException e) {

        }
        return 0;
    }

    @Override
    public HashMap<String, Anime> getAnimeMap() {
        return null;
    }
}
