package com.taskdesignsinc.android.myanimeviewer.model.helper;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.model.DownloadRecord;
import com.taskdesignsinc.android.myanimeviewer.service.download.AnimeDownloadTask;

/**
 * Created by salma on 8/3/2017.
 */

public class DownloadRecordMapper {

    public static DownloadRecord map(int status, AnimeDownloadTask task) {
        //UserRecordHelper.getInstance(mContext).updateDownload(task.getId(), task.getAnimeUrl(),
        // task.getUrl(), task.getDownloadPath(), task.getDownloadSize(), task.getTotalSize(),
        // DownloadIntents.SubTypes.PAUSED);
        DownloadRecord record = MAVApplication.getInstance().getRepository().getDownloadTask(task.getUrl());
        if (record == null)
            record = new DownloadRecord();
        record.setStatus(status);
        record.setAnimeUrl(task.getAnimeUrl());
        record.setPosition(task.getId());
        record.setEpisodeUrl(task.getUrl());
        record.setPath(task.getDownloadPath());
        record.setProgress(task.getDownloadSize());
        record.setSize(task.getTotalSize());
        return record;
    }
}
