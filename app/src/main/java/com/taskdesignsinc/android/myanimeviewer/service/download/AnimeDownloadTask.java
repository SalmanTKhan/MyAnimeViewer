package com.taskdesignsinc.android.myanimeviewer.service.download;

import android.accounts.NetworkErrorException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;

import com.taskdesignsinc.android.myanimeviewer.DownloadListActivity;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.service.download.error.FileAlreadyExistException;
import com.taskdesignsinc.android.myanimeviewer.service.download.error.NoMemoryException;
import com.taskdesignsinc.android.myanimeviewer.model.helper.AnimeUtils;
import com.taskdesignsinc.android.myanimeviewer.util.FileUtils;
import com.taskdesignsinc.android.myanimeviewer.util.NetworkUtils;
import com.taskdesignsinc.android.myanimeviewer.util.StorageUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.taskdesignsinc.android.myanimeviewer.parser.Parser.mConnectTimeOut;
import static com.taskdesignsinc.android.myanimeviewer.parser.Parser.mParseTimeOut;

public class AnimeDownloadTask extends AsyncTask<Void, Integer, Long> {

    public enum Order implements Comparator<AnimeDownloadTask> {
        ByIndex() {
            @Override
            public int compare(AnimeDownloadTask task1, AnimeDownloadTask task2) {

                int diff = (int) (task1.getId() - task2.getId());

                // ascending order
                return diff;
            }
        }
    }

    private int mId;

    public final static int TIME_OUT = 60000 * 60;
    private final static int BUFFER_SIZE = 1024 * 8;

    private static final String TAG = AnimeDownloadTask.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final String TEMP_SUFFIX = ".download";

    private File file;
    private File tempFile;
    private String videoUrl;
    private RandomAccessFile outputStream;
    private AnimeDownloadTaskListener listener;
    private Context context;

    private long downloadSize;
    private long previousFileSize;
    private long totalSize;
    private long downloadPercent;
    private long networkSpeed;
    private long previousTime;
    private long totalTime;
    private Throwable error = null;
    private boolean interrupt = false;

    private Anime anime;
    private Episode episode;
    private File downloadDir;

    private NotificationManager mNotifyManager;
    private Builder mBuilder;
    private File mAnimeCover;

    private String episodeUrl;

    private SharedPreferences mPrefs;
    private Parser mParser;

    public int getId() {
        return mId;
    }

    public void setId(int mId) {
        this.mId = mId;
    }

    private final class ProgressReportingRandomAccessFile extends RandomAccessFile {
        private int progress = 0;
        private int latestPercentDone;
        private long totalSize;
        private int percentDownloaded;

        public ProgressReportingRandomAccessFile(File file, String mode, long totalSize2)
                throws FileNotFoundException {
            super(file, mode);
            this.totalSize = totalSize2;
        }

        @Override
        public void write(byte[] buffer, int offset, int count) throws IOException {

            super.write(buffer, offset, count);
            progress += count;
            latestPercentDone = (int) ((progress / (float) this.totalSize) * 100.0);
            if (percentDownloaded != latestPercentDone) {
                percentDownloaded = latestPercentDone;
                publishProgress(progress);
            }
        }
    }

    public AnimeDownloadTask(int id, Context context, String pEpisodeURL) throws MalformedURLException {
        this(id, context, pEpisodeURL, null);
    }

    public AnimeDownloadTask(int id, Context context, String pEpisodeURL, AnimeDownloadTaskListener listener)
            throws MalformedURLException {
        super();
        this.mId = id;
        this.listener = listener;
        this.context = context;
        episodeUrl = pEpisodeURL;
        episode = MAVApplication.getInstance().getRepository().getEpisodeByUrl(pEpisodeURL);
        if (episode != null) {
            anime = MAVApplication.getInstance().getRepository().getAnimeByUrl(episode.getAnimeUrl(), true);
            if (anime != null) {
                downloadDir = new File(StorageUtils.getDataDirectory(),
                        FileUtils.getValidFileName(anime.getTitle()) + "/");
                downloadDir.mkdirs();
            }
        }
    }

    public AnimeDownloadTask(int id, Context context, String pEpisodeURL, String pAnimeURL, AnimeDownloadTaskListener listener)
            throws MalformedURLException {
        super();
        this.mId = id;
        this.listener = listener;
        this.context = context;
        episodeUrl = pEpisodeURL;
        episode = MAVApplication.getInstance().getRepository().getEpisodeByUrl(pEpisodeURL);
        anime = MAVApplication.getInstance().getRepository().getAnimeByUrl(pAnimeURL);
        mParser = Parser.getExistingInstance(Parser.getTypeByURL(pAnimeURL));
    }

    public boolean isInterrupt() {

        return interrupt;
    }

    public long getDownloadPercent() {

        return downloadPercent;
    }

    public long getDownloadSize() {

        return downloadSize + previousFileSize;
    }

    public long getTotalSize() {

        return totalSize;
    }

    public long getDownloadSpeed() {

        return this.networkSpeed;
    }

    public long getTotalTime() {

        return this.totalTime;
    }

    public AnimeDownloadTaskListener getListener() {

        return this.listener;
    }

    @Override
    protected void onPreExecute() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        previousTime = System.currentTimeMillis();
        mBuilder = new NotificationCompat.Builder(context);
        if (listener != null)
            listener.preDownload(this);
        episode = MAVApplication.getInstance().getRepository().getEpisodeByUrl(episodeUrl);
        if (anime != null && episode != null) {
            mParser = Parser.getExistingInstance(Parser.getTypeByURL(anime.getUrl()));
            downloadDir = new File(StorageUtils.getDataDirectory(),
                    FileUtils.getValidFileName(anime.getTitle()) + "/");
            downloadDir.mkdirs();
            mAnimeCover = new File(StorageUtils.getDataDirectory(),
                    FileUtils.getValidFileName(anime.getTitle()));
            mAnimeCover.mkdirs();
            mAnimeCover = new File(mAnimeCover, "cover.png");
            AnimeUtils.saveAsync(anime);
            videoUrl = episode.getVideoUrl();
        } else {
            if (episode == null) {
                WriteLog.appendLog(TAG,"Download will fail due to Episode not being found by URL " + episodeUrl);
            }
            if (anime == null) {
                if (episode != null)
                    WriteLog.appendLog(TAG,"Download will fail due to Anime not being found by ID " + episode.getAnimeUrl());
                else
                    WriteLog.appendLog(TAG,"Download will fail due to Anime not being found by ID " + episodeUrl);
            }
        }
    }

    @Override
    protected Long doInBackground(Void... params) {
        long result = -1;
        try {
            if (episode != null && anime != null) {
                if (!mAnimeCover.exists())
                    if (!TextUtils.isEmpty(anime.getCover()))
                        Ion.with(context).load(anime.getCover()).write(mAnimeCover);
                if (TextUtils.isEmpty(videoUrl))
                    videoUrl = mParser.getEpisodeVideo(episodeUrl);
                else
                    WriteLog.appendLog(videoUrl + " loaded from database");
                if (!TextUtils.isEmpty(videoUrl)) {
                    mNotifyManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    mBuilder = new NotificationCompat.Builder(context);
                    mBuilder.setContentTitle(anime.getTitle())
                            .setContentText(episode.getTitle() + " is downloading")
                            .setSmallIcon(R.mipmap.ic_launcher);
                    Intent i = new Intent(context, DownloadListActivity.class);
                    mBuilder.setContentIntent(PendingIntent.getActivity(context, episode.getUrl().hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT));
                    Notification lNotification = mBuilder.build();
                    mNotifyManager.notify(anime.getUrl().hashCode(), lNotification);
                    result = download(videoUrl, episode.getTitle());
                }
            }
        } catch (NetworkErrorException e) {
            error = e;
        } catch (FileAlreadyExistException e) {
            error = e;
        } catch (NoMemoryException e) {
            error = e;
        } catch (IOException e) {
            error = e;
        } finally {
            //if (client != null) {
            //	client.close();
            //}
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {

        if (progress.length > 1) {
            totalSize = progress[1];
            if (totalSize == -1) {
                if (listener != null)
                    listener.errorDownload(this, error);
            } else {

            }
        } else {
            totalTime = System.currentTimeMillis() - previousTime;
            downloadSize = progress[0];
            downloadPercent = (downloadSize + previousFileSize) * 100 / totalSize;
            networkSpeed = downloadSize / totalTime;
            if (mBuilder != null && anime != null && episode != null) {
                mBuilder.setProgress(100, (int) downloadPercent, false);
                // Issues the notification
                mBuilder.setContentText(episode.getTitle() + " is downloading");
                if (anime != null && mNotifyManager != null)
                    mNotifyManager.notify(anime.getUrl().hashCode(), mBuilder.build());
            }
            if (listener != null)
                listener.updateProcess(this);
        }
    }

    @Override
    protected void onPostExecute(Long result) {

        if (result == -1 || interrupt || error != null) {
            if (DEBUG && error != null) {
                Log.v(TAG, "Download failed. " + error.getMessage());
            }
            if (listener != null) {
                listener.errorDownload(this, error);
            }
            return;
        }
        if (listener != null) {
            // finish download
            tempFile.renameTo(file);
            mBuilder.setContentText("Download complete")
                    // Removes the progress bar
                    .setProgress(0, 0, false);
            if (mNotifyManager == null && context != null)
                mNotifyManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotifyManager != null && anime != null)
                mNotifyManager.notify(anime.getUrl().hashCode(), mBuilder.build());
            // When the loop is finished, updates the notification
            listener.finishDownload(this);
        }
    }

    @Override
    public void onCancelled() {

        super.onCancelled();
        interrupt = true;
    }

    //private AndroidHttpClient client;
    //private HttpGet httpGet;
    //private HttpResponse response;
    public static OkHttpClient client = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .readTimeout(mParseTimeOut, TimeUnit.MILLISECONDS)
            .writeTimeout(mParseTimeOut, TimeUnit.MILLISECONDS)
            .connectTimeout(mConnectTimeOut, TimeUnit.MILLISECONDS).build();

    private long download(String url, String fileName) throws NetworkErrorException, IOException, FileAlreadyExistException,
            NoMemoryException {
        file = new File(downloadDir, fileName + ".mp4");
        tempFile = new File(downloadDir, fileName + TEMP_SUFFIX);

        if (DEBUG) {
            Log.v(TAG, "totalSize: " + totalSize);
        }

		/*
         * check net work
		 */
        if (!NetworkUtils.isNetworkAvailable(context)) {
            throw new NetworkErrorException("Network blocked.");
        }

        if (url.contains("["))
            url = url.substring(0, url.indexOf("[")) + URLEncoder.encode(url.substring(url.indexOf("[")), "UTF-8");
        else if (url.contains("]"))
            url = url.substring(0, url.indexOf("]")) + URLEncoder.encode(url.substring(url.indexOf("]")), "UTF-8");
        if (url.contains(" "))
            url = url.replace(" ", "%20");

		/*
         * check file length
		 */
        Request request = new Request.Builder().url(url)
                .build();
        Response response = client.newCall(request).execute();
        totalSize = response.body().contentLength();

        if (file.exists() && totalSize == file.length()) {
            if (DEBUG) {
                Log.v(null, "Output file already exists. Skipping download.");
            }
            return totalSize;
            //throw new FileAlreadyExistException("Output file already exists. Skipping download.");
        } else if (tempFile.exists()) {
            Request continueRequest = new Request.Builder().url(url)
                    .addHeader("Range", "bytes=" + tempFile.length() + "-")
                    .build();
            previousFileSize = tempFile.length();

            response.close();
            response = client.newCall(continueRequest).execute();

            if (DEBUG) {
                Log.v(TAG, "File is incomplete, downloading now.");
                Log.v(TAG, "File length:" + tempFile.length() + " totalSize:" + totalSize);
            }
        }

		/*
         * check memory
		 */
        long storage = StorageUtils.getAvailableStorage();
        if (DEBUG) {
            Log.i(null, "storage:" + storage + " totalSize:" + totalSize);
        }

        if (totalSize - tempFile.length() > storage) {
            throw new NoMemoryException("SD card not enough space available.");
        }

		/*
         * start download
		 */
        outputStream = new ProgressReportingRandomAccessFile(tempFile, "rw", totalSize);

        publishProgress(0, (int) totalSize);

        InputStream input = response.body().byteStream();
        int bytesCopied = copy(input, outputStream);

        if ((previousFileSize + bytesCopied) != totalSize && totalSize != -1 && !interrupt) {
            throw new IOException("Download incomplete: " + bytesCopied + " != " + totalSize);
        }

        if (DEBUG) {
            Log.v(TAG, "Download completed successfully.");
        }

        return bytesCopied;
    }

    private long downloadNewAlt(String url, String fileName) {
        WriteLog.appendLog("AnimeDownloadTask: downloadNewAlt(" + url + "," + fileName + ")");
        if (TextUtils.isEmpty(url))
            return -1;
        file = new File(downloadDir, fileName + ".mp4");
        tempFile = new File(downloadDir, fileName + TEMP_SUFFIX);

        try {
            if (url.contains("["))
                url = url.substring(0, url.indexOf("[")) + URLEncoder.encode(url.substring(url.indexOf("[")), "UTF-8");
            else if (url.contains("]"))
                url = url.substring(0, url.indexOf("]")) + URLEncoder.encode(url.substring(url.indexOf("]")), "UTF-8");
            if (url.contains(" "))
                url = url.replace(" ", "%20");
            if (TextUtils.isEmpty(url)) {
                return -1;
            }
            if (TextUtils.isEmpty(url)) {
                WriteLog.appendLog("AnimeDownloadTask: Invalid URL");
                return -1;
            }
            Ion.with(context)
                    .load(url)
                    .progress(new ProgressCallback() {
                        @Override
                        public void onProgress(long downloaded, long total) {
                            downloadSize = total;
                            downloadPercent = downloaded * 100 / totalSize;
                            mBuilder.setContentText("Downloading")
                                    // Removes the progress bar
                                    .setProgress(100, (int) downloadPercent, false);
                            mNotifyManager.notify(anime.getUrl().hashCode(), mBuilder.build());
                        }
                    })
                    .write(file)
                    .setCallback(new FutureCallback<File>() {
                        @Override
                        public void onCompleted(Exception e, File file) {
                            // download done...
                            // do stuff with the File or error
                            mBuilder.setContentText("Download complete")
                                    // Removes the progress bar
                                    .setProgress(0, 0, false);
                            mNotifyManager.notify(anime.getUrl().hashCode(), mBuilder.build());
                        }
                    });
            ;
            WriteLog.appendLog("AnimeDownloadTask: Saving anime locally " + file.getAbsolutePath());
        } catch (IOException e) {
            WriteLog.appendLog("Downloader Serice: input output error");
            WriteLog.appendLog(Log.getStackTraceString(e));
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return 1;

    }

    public int copy(InputStream input, RandomAccessFile out) throws IOException,
            NetworkErrorException {

        if (input == null || out == null) {
            return -1;
        }

        byte[] buffer = new byte[BUFFER_SIZE];

        BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
        if (DEBUG) {
            Log.v(TAG, "length" + out.length());
        }

        int count = 0, n = 0;
        long errorBlockTimePreviousTime = -1, expireTime = 0;

        try {

            out.seek(out.length());

            while (!interrupt) {
                n = in.read(buffer, 0, BUFFER_SIZE);
                if (n == -1) {
                    break;
                }
                out.write(buffer, 0, n);
                count += n;

				/*
                 * check network
				 */
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    throw new NetworkErrorException("Network blocked.");
                }

                if (networkSpeed == 0) {
                    if (errorBlockTimePreviousTime > 0) {
                        expireTime = System.currentTimeMillis() - errorBlockTimePreviousTime;
                        if (expireTime > TIME_OUT) {
                            throw new ConnectTimeoutException("connection time out.");
                        }
                    } else {
                        errorBlockTimePreviousTime = System.currentTimeMillis();
                    }
                } else {
                    expireTime = 0;
                    errorBlockTimePreviousTime = -1;
                }
            }
        } finally {
            //client.close(); // must close client first
            //client = null;
            out.close();
            in.close();
            input.close();
        }
        return count;

    }

    public String getTitle() {
        if (episode != null)
            return episode.getTitle();
        else
            return "";
    }

    public String getAnimeUrl() {
        if (anime != null) {
            return anime.getUrl();
        }
        return "";
    }

    public String getUrl() {
        if (episode != null)
            return episode.getUrl();
        return episodeUrl;
    }

    public String getDownloadPath() {
        if (downloadDir != null)
            return downloadDir.getAbsolutePath();
        return "";
    }

}
