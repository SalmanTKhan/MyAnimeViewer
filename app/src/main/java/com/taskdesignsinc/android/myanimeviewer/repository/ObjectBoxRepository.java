package com.taskdesignsinc.android.myanimeviewer.repository;

import android.text.TextUtils;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Anime_;
import com.taskdesignsinc.android.myanimeviewer.model.DownloadRecord;
import com.taskdesignsinc.android.myanimeviewer.model.DownloadRecord_;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.Episode_;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteRecord;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteRecord_;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteTag;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteTag_;
import com.taskdesignsinc.android.myanimeviewer.model.HistoryRecord;
import com.taskdesignsinc.android.myanimeviewer.model.HistoryRecord_;
import com.taskdesignsinc.android.myanimeviewer.model.OfflineHistoryRecord;
import com.taskdesignsinc.android.myanimeviewer.model.OfflineHistoryRecord_;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;

/**
 * Created by salma on 7/25/2017.
 */

public class ObjectBoxRepository implements DataRepository, RxDataRepository {

    private Box<Anime> getAnimeBox() {
        return MAVApplication.getInstance().getBoxStore().boxFor(Anime.class);
    }

    private Box<Episode> getEpisodeBox() {
        return MAVApplication.getInstance().getBoxStore().boxFor(Episode.class);
    }

    private Box<FavoriteRecord> getFavoritesBox() {
        return MAVApplication.getInstance().getBoxStore().boxFor(FavoriteRecord.class);
    }

    private Box<FavoriteTag> getTagBox() {
        return MAVApplication.getInstance().getBoxStore().boxFor(FavoriteTag.class);
    }

    private Box<DownloadRecord> getDownloadBox() {
        return MAVApplication.getInstance().getBoxStore().boxFor(DownloadRecord.class);
    }

    private Box<HistoryRecord> getHistoryBox() {
        return MAVApplication.getInstance().getBoxStore().boxFor(HistoryRecord.class);
    }

    private Box<OfflineHistoryRecord> getOfflineHistoryBox() {
        return MAVApplication.getInstance().getBoxStore().boxFor(OfflineHistoryRecord.class);
    }

    @Override
    public long getAnimeCount() {
        return getAnimeBox().count();
    }

    @Override
    public HashMap<String, Anime> getAnimeMap() {
        HashMap<String, Anime> map = new HashMap<>();
        QueryBuilder<Anime> query = getAnimeBox().query();
        List<Anime> list = query.build().find();
        for (Anime anime : list) {
            map.put(anime.getUrl(), anime);
        }
        return map;
    }

    @Override
    public List<Anime> getAnimeList() {
        QueryBuilder<Anime> query = getAnimeBox().query();
        return query.build().find();
    }

    @Override
    public HashMap<String, Anime> getAnimeMapByUrl(String url) {
        HashMap<String, Anime> map = new HashMap<>();
        QueryBuilder<Anime> query = getAnimeBox().query();
        query.contains(Anime_.url, url);
        List<Anime> list = query.build().find();
        for (Anime anime : list) {
            map.put(anime.getUrl(), anime);
        }
        return map;
    }

    @Override
    public List<Anime> getAnimeListByUrl(String url) {
        QueryBuilder<Anime> query = getAnimeBox().query();
        query.contains(Anime_.url, url);
        return query.build().find();
    }

    @Override
    public List<Anime> getAnimeListByTitle(String title) {
        QueryBuilder<Anime> query = getAnimeBox().query();
        query.contains(Anime_.title, title);
        return query.build().find();
    }

    @Override
    public List<Anime> getAnimeListByTitle(String title, String url) {
        QueryBuilder<Anime> query = getAnimeBox().query();
        query.contains(Anime_.title, title);
        query.contains(Anime_.url, url);
        return query.build().find();
    }

    @Override
    public void insertAnimeList(List<Anime> animeList) {
        getAnimeBox().put(animeList);
    }

    @Override
    public void updateAnimeList(List<Anime> animeList) {
        getAnimeBox().put(animeList);
    }

    @Override
    public Anime getAnimeByUrl(String animeUrl) {
        return getAnimeByUrl(animeUrl, false);
    }

    @Override
    public Anime getAnimeByUrl(String animeUrl, boolean loadEpisodes) {
        QueryBuilder<Anime> query = getAnimeBox().query();
        query.equal(Anime_.url, animeUrl);
        if (loadEpisodes) {

        }
        return query.build().findFirst();
    }

    @Override
    public String getCoverByUrl(String animeUrl) {
        Anime anime = getAnimeByUrl(animeUrl);
        if (anime != null)
            return anime.getCover();
        return "";
    }

    @Override
    public void insertAnime(Anime anime) {
        if (anime != null)
            getAnimeBox().put(anime);
    }

    @Override
    public void updateAnime(Anime anime) {
        if (anime != null)
            getAnimeBox().put(anime);
    }

    @Override
    public void deleteAnime(String animeUrl) {
        Anime anime = getAnimeByUrl(animeUrl);
        if (anime != null)
            getAnimeBox().remove(anime);
    }

    @Override
    public void insertEpisodeList(List<Episode> episodes) {
        if (episodes != null)
            getEpisodeBox().put(episodes);
    }

    @Override
    public void deleteEpisodeList(Anime anime) {
        List<Episode> episodes = getEpisodeBox().find(Episode_.animeId, anime.getId());
        if (episodes != null)
            getEpisodeBox().remove(episodes);
    }

    @Override
    public void deleteEpisodes(String animeUrl) {
        Anime anime = getAnimeByUrl(animeUrl);
        if (anime != null)
            deleteEpisodeList(anime);
    }

    @Override
    public void updateEpisode(Episode episode) {
        if (episode != null)
            getEpisodeBox().put(episode);
    }

    @Override
    public List<Episode> getEpisodes(String animeUrl) {
        Anime anime = getAnimeByUrl(animeUrl);
        if (anime != null) {
            List<Episode> episodes = getEpisodeList(anime.getId());
            return episodes;
        }
        return Collections.emptyList();
    }

    @Override
    public Episode getEpisodeByUrl(String episodeUrl) {
        QueryBuilder<Episode> queryBuilder = getEpisodeBox().query();
        queryBuilder.equal(Episode_.url, episodeUrl);
        return queryBuilder.build().findFirst();
    }

    @Override
    public int getEpisodeCount(String animeUrl) {
        Anime anime = getAnimeByUrl(animeUrl);
        if (anime != null) {
            List<Episode> episodes = getEpisodeList(anime.getId());
            return episodes.size();
        }
        return 0;
    }

    public List<Episode> getEpisodeList(long animeId) {
        QueryBuilder<Episode> query = getEpisodeBox().query();
        query.equal(Episode_.animeId, animeId);
        return query.build().find();
    }

    @Override
    public List<FavoriteRecord> getFavorites() {
        QueryBuilder<FavoriteRecord> query = getFavoritesBox().query();
        return query.build().find();
    }

    @Override
    public void insertFavorite(String animeUrl, int tagId) {
        FavoriteRecord record = new FavoriteRecord();
        record.setAnimeUrl(animeUrl);
        record.setTagId(tagId);
        Anime anime = getAnimeByUrl(animeUrl);
        if (anime != null)
            record.setAnime(anime);
        getFavoritesBox().put(record);
    }

    @Override
    public void deleteFavorite(String animeUrl) {
        FavoriteRecord record = getFavoriteByAnimeUrl(animeUrl);
        if (record != null)
            getFavoritesBox().remove(record);
    }

    @Override
    public FavoriteTag getFavoriteTag(int id) {
        QueryBuilder<FavoriteTag> query = getTagBox().query();
        query.equal(FavoriteTag_.id, id);
        return query.build().findFirst();
    }

    @Override
    public FavoriteRecord getFavoriteByAnimeUrl(String animeUrl) {
        QueryBuilder<FavoriteRecord> query = getFavoritesBox().query();
        query.equal(FavoriteRecord_.animeUrl, animeUrl);
        return query.build().findFirst();
    }

    @Override
    public boolean isFavorite(String animeUrl) {
        if (TextUtils.isEmpty(animeUrl))
            return false;
        QueryBuilder<FavoriteRecord> query = getFavoritesBox().query();
        query.equal(FavoriteRecord_.animeUrl, animeUrl);
        return query.build().count() > 0;
    }

    @Override
    public void deleteFavoriteRecordsByTagID(int tagId) {
        QueryBuilder<FavoriteRecord> query = getFavoritesBox().query();
        query.equal(FavoriteRecord_.tagId, tagId);
        List<FavoriteRecord> records = query.build().find();
        getFavoritesBox().remove(records);
    }

    @Override
    public boolean deleteFavoriteRecords() {
        getFavoritesBox().removeAll();
        return true;
    }

    @Override
    public void insertHistoryRecord(String animeUrl, String episodeUrl, boolean isViewed) {
        HistoryRecord record = getHistoryRecord(episodeUrl);
        if (record == null)
            record = new HistoryRecord();
        record.setAnimeUrl(animeUrl);
        record.setEpisodeUrl(episodeUrl);
        record.setIsViewed(isViewed);
        record.setTimeStamp(System.currentTimeMillis());
        getHistoryBox().put(record);
    }

    @Override
    public HistoryRecord getHistoryRecord(String episodeUrl) {
        QueryBuilder<HistoryRecord> query = getHistoryBox().query();
        query.equal(HistoryRecord_.episodeUrl, episodeUrl);
        return query.build().findFirst();
    }

    @Override
    public void deleteHistoryRecord(String episodeUrl) {
        HistoryRecord record = getHistoryRecord(episodeUrl);
        if (record != null)
            getHistoryBox().remove(record);
    }

    @Override
    public void deleteHistoryRecords() {

    }

    @Override
    public List<HistoryRecord> getHistoryRecords() {
        return getHistoryBox().getAll();
    }

    @Override
    public List<HistoryRecord> getHistoryRecords(String animeUrl) {
        QueryBuilder<HistoryRecord> query = getHistoryBox().query();
        query.equal(HistoryRecord_.animeUrl, animeUrl);
        return query.build().find();
    }

    @Override
    public List<DownloadRecord> getDownloadTasks(boolean isCompleted) {
        QueryBuilder<DownloadRecord> query = getDownloadBox().query();
        if (isCompleted)
            query.equal(DownloadRecord_.status, Constants.DownloadIntents.SubTypes.COMPLETED);
        else
            query.notEqual(DownloadRecord_.status, Constants.DownloadIntents.SubTypes.COMPLETED);
        return query.build().find();
    }

    @Override
    public DownloadRecord getDownloadTask(String episodeUrl, boolean isCompleted) {
        QueryBuilder<DownloadRecord> query = getDownloadBox().query();
        query.equal(DownloadRecord_.episodeUrl, episodeUrl);
        if (isCompleted)
            query.equal(DownloadRecord_.status, Constants.DownloadIntents.SubTypes.COMPLETED);
        else
            query.notEqual(DownloadRecord_.status, Constants.DownloadIntents.SubTypes.COMPLETED);
        return query.build().findFirst();
    }

    @Override
    public void deleteDownloadTask(String episodeUrl) {
        DownloadRecord record = getDownloadTask(episodeUrl);
        if (record != null)
            getDownloadBox().remove(record);
    }

    @Override
    public void deleteIncompleteDownloadTasks() {
        List<DownloadRecord> list = getDownloadTasks(false);
        getDownloadBox().remove(list);
    }

    @Override
    public DownloadRecord getDownloadTask(String episodeUrl) {
        QueryBuilder<DownloadRecord> query = getDownloadBox().query();
        query.equal(DownloadRecord_.episodeUrl, episodeUrl);
        return query.build().findFirst();
    }

    @Override
    public void insertDownloadTask(DownloadRecord record) {
        getDownloadBox().put(record);
    }

    @Override
    public void updateDownloadTask(DownloadRecord record) {
        getDownloadBox().put(record);
    }

    @Override
    public String getEpisodeTitle(String episodeUrl) {
        Episode episode = getEpisodeByUrl(episodeUrl);
        if (episode != null) {
            return episode.getTitle();
        }
        return "";
    }

    @Override
    public String getLastViewedEpisode(String animeUrl) {
        QueryBuilder<HistoryRecord> query = getHistoryBox().query();
        query.equal(HistoryRecord_.isViewed, true)
                .contains(HistoryRecord_.animeUrl, animeUrl)
                .orderDesc(HistoryRecord_.timeStamp);
        HistoryRecord record = query.build().findFirst();
        if (record != null)
            return record.getEpisodeUrl();
        return "";
    }

    @Override
    public OfflineHistoryRecord getOfflineHistoryRecord(String path) {
        QueryBuilder<OfflineHistoryRecord> query = getOfflineHistoryBox().query();
        query.equal(OfflineHistoryRecord_.path, path);
        return query.build().findFirst();
    }

    public void deleteOfflineHistoryRecord(String path) {
        OfflineHistoryRecord record = getOfflineHistoryRecord(path);
        if (record != null)
            getOfflineHistoryBox().remove(record);
    }

    @Override
    public void deleteOfflineHistoryRecords() {
        getOfflineHistoryBox().removeAll();
    }

    @Override
    public List<OfflineHistoryRecord> getOfflineHistoryRecords() {
        return getOfflineHistoryBox().getAll();
    }

    @Override
    public void insertOfflineHistoryRecord(String path, boolean isViewed) {
        OfflineHistoryRecord record = getOfflineHistoryRecord(path);
        if (record == null)
            record = new OfflineHistoryRecord();
        record.setIsViewed(isViewed);
        record.setTimeStamp(System.currentTimeMillis());
        getOfflineHistoryBox().put(record);
    }

    @Override
    public Boolean isPathViewed(String path) {
        OfflineHistoryRecord record = getOfflineHistoryRecord(path);
        if (record != null)
            return record.getIsViewed();
        return false;
    }

    @Override
    public String getLastViewedByPath(String parent) {
        QueryBuilder<OfflineHistoryRecord> query = getOfflineHistoryBox().query();
        query.contains(OfflineHistoryRecord_.path, parent);
        query.orderDesc(OfflineHistoryRecord_.timeStamp);
        OfflineHistoryRecord record = query.build().findFirst();
        if (record != null)
            return record.getPath();
        return "";
    }

    @Override
    public List<FavoriteTag> getFavoriteTags() {
        QueryBuilder<FavoriteTag> query = getTagBox().query();
        return query.build().find();
    }

    @Override
    public void insertFavoriteTag(int tagId, String title, int ribbonId) {
        FavoriteTag tag = new FavoriteTag(tagId, title, ribbonId);
        getTagBox().put(tag);
    }

    @Override
    public boolean deleteFavoriteTag(int tagId) {
        QueryBuilder<FavoriteTag> query = getTagBox().query();
        query.equal(FavoriteTag_.tagId, tagId);
        FavoriteTag tag = query.build().findFirst();
        if (tag != null) {
            getTagBox().remove(tag);
            return true;
        }
        return false;
    }

    @Override
    public Query<Anime> queryAnimeList(String serverUrl) {
        QueryBuilder<Anime> query = getAnimeBox().query();
        query.contains(Anime_.url, serverUrl);
        query.order(Anime_.title);
        return query.build();
    }

    @Override
    public Query<Episode> queryEpisodes(String animeUrl) {
        QueryBuilder<Episode> query = getEpisodeBox().query();
        query.contains(Episode_.animeUrl, animeUrl);
        query.order(Episode_.index);
        return query.build();
    }
}
