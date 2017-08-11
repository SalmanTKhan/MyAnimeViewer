
package com.taskdesignsinc.android.myanimeviewer.service.download;

public interface AnimeDownloadTaskListener {

    public void updateProcess(AnimeDownloadTask task);

    public void updateProcess(AnimeDownloadTask task, String msg);

    public void finishDownload(AnimeDownloadTask task);

    public void preDownload(AnimeDownloadTask task);

    public void errorDownload(AnimeDownloadTask task, Throwable error);
}
