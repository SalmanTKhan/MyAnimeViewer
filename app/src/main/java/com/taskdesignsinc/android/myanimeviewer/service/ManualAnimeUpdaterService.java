package com.taskdesignsinc.android.myanimeviewer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.taskdesignsinc.android.myanimeviewer.AnimeDetailsActivity;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.MainActivity;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.NetworkUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ManualAnimeUpdaterService extends Service {
    private static final String mTag = ManualAnimeUpdaterService.class.getSimpleName();
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private static NotificationManager mNotificationManager;
    private static SparseIntArray mNotificationMap;
    private SharedPreferences mPrefs;

    private static int mNotificationID = 1296780;

    //Handler that receives messages from the thread
    static class ServiceHandler extends Handler {
        private final WeakReference<ManualAnimeUpdaterService> mService;
        private final Context mContext;
        private String mError;

        ServiceHandler(ManualAnimeUpdaterService service) {
            mService = new WeakReference<>(service);
            mContext = service;
        }

        public ServiceHandler(Looper looper, ManualAnimeUpdaterService service) {
            super(looper);
            mContext = service;
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {

            Bundle extras = msg.getData();

            if (extras != null) {
                final String animeUrl = extras.getString(Constants.ANIME_URL);
                final int updateNotification = Integer.parseInt(mService.get().getPrefs().getString(Constants.KEY_FAVORITES_UPDATE_NOTIFICATION, "2"));
                if (!NetworkUtils.isNetworkAvailable(mContext)) {
                    WriteLog.appendLog(mTag, "Skipping checking updates for " + animeUrl + " because no internet connection");
                    return;
                }
                WriteLog.appendLog(mTag, "Checking updates for " + animeUrl);

                final Anime cachedAnime = MAVApplication.getInstance().getRepository().getAnimeByUrl(animeUrl);
                if (cachedAnime != null) {
                    if (updateNotification != Constants.UPDATE_NOTIFICATION_NONE) {
                        // Build notification
                        Builder nb = new Builder(mContext);
                        nb.setWhen(System.currentTimeMillis());
                        nb.setContentTitle(cachedAnime.getTitle());

                        nb.setSmallIcon(android.R.drawable.stat_sys_download);
                        nb.setOngoing(false);

                        RemoteViews contentView = new RemoteViews(mContext.getApplicationContext().getPackageName(), R.layout.download_notif);
                        contentView.setImageViewResource(R.id.status_icon, R.mipmap.ic_launcher);
                        contentView.setTextViewText(R.id.status_text, cachedAnime.getTitle() + " " + mContext.getResources().getString(R.string.updating));
                        contentView.setTextColor(R.id.status_text, mContext.getResources().getColor(android.R.color.black));
                        contentView.setProgressBar(R.id.status_progress, 50, 0, true);
                        contentView.setViewVisibility(R.id.status_progress_wrapper, View.VISIBLE);
                        nb.setContent(contentView);
                        Intent i = new Intent(mContext, AnimeDetailsActivity.class);
                        i.putExtra(Constants.ANIME_ID, cachedAnime.getId());
                        i.putExtra(Constants.ANIME_URL, animeUrl);
                        nb.setContentIntent(PendingIntent.getActivity(mContext, cachedAnime.getUrl().hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT));

                        final Notification notification = nb.getNotification();
                        notification.contentView = contentView;

                        if (notification != null) {
                            int notifyID = cachedAnime.getUrl().hashCode();
                            if (updateNotification == Constants.UPDATE_NOTIFICATION_ONCE)
                                notifyID = ManualAnimeUpdaterService.mNotificationID;
                            mNotificationMap.put(cachedAnime.getUrl().hashCode(), notifyID);
                            mNotificationManager.notify(notifyID, notification);
                        }
                    }
                    Episode episode = null;
                    int lOldEpisodeCount = MAVApplication.getInstance().getRepository().getEpisodeCount(cachedAnime.getUrl());
                    int lTempNewEpisodeCount = 0;
                    Parser lParser = Parser.getExistingInstance(Parser.getTypeByURL(animeUrl));
                    
                    final Anime parsedAnime = lParser.getAnimeDetails(animeUrl);
                    if (!TextUtils.isEmpty(parsedAnime.getUrl()) && !cachedAnime.getUrl().equals(parsedAnime.getUrl())) {
                        MAVApplication.getInstance().getRepository().deleteAnime(cachedAnime.getUrl());
                        MAVApplication.getInstance().getRepository().deleteEpisodes(cachedAnime.getUrl());
                        if (updateNotification != Constants.UPDATE_NOTIFICATION_NONE) {
                            int notifyID = cachedAnime.getUrl().hashCode();
                            if (updateNotification == Constants.UPDATE_NOTIFICATION_ONCE)
                                notifyID = ManualAnimeUpdaterService.mNotificationID;
                            mNotificationManager.cancel(notifyID);
                            // Build notification
                            Builder nb = new Builder(mContext);
                            nb.setWhen(System.currentTimeMillis());
                            nb.setContentTitle(parsedAnime.getTitle());

                            nb.setSmallIcon(android.R.drawable.stat_sys_download);
                            nb.setOngoing(false);

                            RemoteViews contentView = new RemoteViews(mContext.getApplicationContext().getPackageName(), R.layout.download_notif);
                            contentView.setImageViewResource(R.id.status_icon, R.mipmap.ic_launcher);
                            contentView.setTextViewText(R.id.status_text, parsedAnime.getTitle() + " " + mContext.getResources().getString(R.string.updating));
                            contentView.setProgressBar(R.id.status_progress, 50, 0, true);
                            contentView.setViewVisibility(R.id.status_progress_wrapper, View.VISIBLE);
                            nb.setContent(contentView);
                            Intent i = new Intent(mContext, AnimeDetailsActivity.class);
                            i.putExtra(Constants.ANIME_ID, parsedAnime.getId());
                            i.putExtra(Constants.ANIME_URL, animeUrl);
                            nb.setContentIntent(PendingIntent.getActivity(mContext, parsedAnime.getUrl().hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT));

                            final Notification notification = nb.getNotification();
                            notification.contentView = contentView;

                            if (notification != null) {
                                notifyID = parsedAnime.getUrl().hashCode();
                                if (updateNotification == Constants.UPDATE_NOTIFICATION_ONCE)
                                    notifyID = ManualAnimeUpdaterService.mNotificationID;
                                mNotificationMap.put(parsedAnime.getUrl().hashCode(), notifyID);
                                mNotificationManager.notify(notifyID, notification);
                            }
                        }
                    } else if (TextUtils.isEmpty(parsedAnime.getUrl())) {
                        if (!NetworkUtils.isNetworkAvailable(mContext)) {
                            WriteLog.appendLog(mTag, "Skipping checking updates for " + animeUrl + " because no internet connection");
                        } else {
                            WriteLog.appendLog(mTag, "Skipping because parsed manga is returning no url");
                        }
                        mNotificationManager.cancel(cachedAnime.getUrl().hashCode());
                        return;
                    }
                    if (parsedAnime != null) {
                        parsedAnime.setEpisodeCount(parsedAnime.getEpisodes().size());
                    }
                    if (lOldEpisodeCount != parsedAnime.getEpisodes().size()) {
                        if (parsedAnime != null && parsedAnime.getEpisodes() != null) {
                            List<Episode> newEpisodes = new ArrayList<>();
                            for (int j = 0; j < parsedAnime.getEpisodes().size(); j++) {
                                episode = MAVApplication.getInstance().getRepository().getEpisodeByUrl(parsedAnime.getEpisodes().get(j).getUrl());
                                if (episode == null) {
                                    episode = parsedAnime.getEpisodes().get(j);
                                    episode.setIndex(j);
                                    episode.setAnime(parsedAnime);
                                    newEpisodes.add(episode);
                                    lTempNewEpisodeCount++;
                                }
                            }
                            parsedAnime.setNewEpisodes(lTempNewEpisodeCount);
                            MAVApplication.getInstance().getRepository().insertEpisodeList(newEpisodes);
                        }
                    } else {
                        WriteLog.appendLog(mTag, "no episode updates for " + parsedAnime.getTitle());
                        int notifyID = parsedAnime.getUrl().hashCode();
                        if (updateNotification == Constants.UPDATE_NOTIFICATION_ONCE)
                            notifyID = ManualAnimeUpdaterService.mNotificationID;
                        mNotificationManager.cancel(
                                notifyID);
                        return;
                    }
                    //parsedAnime.setLastUpdate(System.currentTimeMillis());
                    if (MAVApplication.getInstance().getRepository().getAnimeByUrl(parsedAnime.getUrl()) == null)
                        MAVApplication.getInstance().getRepository().insertAnime(parsedAnime);
                    else {
                        parsedAnime.setId(cachedAnime.getId());
                        MAVApplication.getInstance().getRepository().updateAnime(parsedAnime);
                    }
                    Intent lIntent = new Intent(Constants.Intents.ANIME_UPDATED);
                    lIntent.putExtra(Constants.ANIME_ID, parsedAnime.getId());
                    lIntent.putExtra(Constants.ANIME_URL, animeUrl);
                    mContext.sendBroadcast(lIntent);

                    if (lTempNewEpisodeCount != 0) {
                        WriteLog.appendLog(mTag, "new " + lTempNewEpisodeCount + " episodes added for " + parsedAnime.getTitle());
                        if (updateNotification != Constants.UPDATE_NOTIFICATION_NONE) {
                            if (mNotificationManager == null)
                                mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.cancel(cachedAnime.getUrl().hashCode());
                            // Sets an ID for the notification, so it can be updated
                            Builder mNotifyBuilder = new Builder(mContext)
                                    .setContentTitle(parsedAnime.getTitle() + " updated")
                                    .setContentText(lTempNewEpisodeCount + " added episode(s)")
                                    .setSmallIcon(R.mipmap.ic_launcher);

                            mNotifyBuilder.setAutoCancel(true);
                            Intent lIntent2 = new Intent(mContext, MainActivity.class);
                            lIntent2.putExtra(Constants.ANIME_ID, parsedAnime.getId());
                            lIntent2.putExtra(Constants.ANIME_URL, parsedAnime.getUrl());
                            mNotifyBuilder.setContentIntent(PendingIntent.getActivity(mContext, parsedAnime.getUrl().hashCode(), lIntent2, PendingIntent.FLAG_UPDATE_CURRENT));
                            // Because the ID remains unchanged, the existing notification is updated.
                            int notifyID = parsedAnime.getUrl().hashCode();
                            if (updateNotification == Constants.UPDATE_NOTIFICATION_ONCE)
                                notifyID = ManualAnimeUpdaterService.mNotificationID;
                            mNotificationManager.notify(
                                    notifyID,
                                    mNotifyBuilder.getNotification());
                            mNotificationMap.delete(parsedAnime.getUrl().hashCode());
                        }
                    }
                } else {
                    WriteLog.appendLog(mTag, "Anime not found in database" + animeUrl);
                    mError = "Anime not found in database";
                    Toast.makeText(mContext, mError, Toast.LENGTH_SHORT).show();
                }
            }
            WriteLog.appendLog(mTag, "finished");
            if (!hasMessages(0))
                mService.get().stopSelf();
        }
    }

    /**
     * @return the prefs
     */
    public SharedPreferences getPrefs() {
        return mPrefs;
    }

    /**
     * @param prefs the prefs to set
     */
    public void setPrefs(SharedPreferences prefs) {
        mPrefs = prefs;
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationMap = new SparseIntArray();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        //mServiceHandler = new ServiceHandler(mServiceLooper);
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        setPrefs(PreferenceManager.getDefaultSharedPreferences(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mServiceHandler != null) {
            if (intent != null && intent.getExtras() != null) {
                // For each start request, send a message to start a job and deliver the
                // start ID so we know which request we're stopping when we finish the job
                Message msg = mServiceHandler.obtainMessage();
                msg.arg1 = startId;
                msg.setData(intent.getExtras());
                mServiceHandler.sendMessage(msg);
            } else
                stopSelf();
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        for (int i = 0; i < mNotificationMap.size(); i++)
            mNotificationManager.cancel(mNotificationMap.valueAt(i));
    }
}