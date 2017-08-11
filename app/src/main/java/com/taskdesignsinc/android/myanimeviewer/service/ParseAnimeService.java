package com.taskdesignsinc.android.myanimeviewer.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.squareup.picasso.Picasso;
import com.taskdesignsinc.android.myanimeviewer.AnimeDetailsActivity;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.MainActivity;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteRecord;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.NetworkUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ParseAnimeService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private static NotificationManager mNotificationManager;
    private static SparseIntArray mNotificationMap;

    static final String mTag = ParseAnimeService.class.getSimpleName();
    private static volatile PowerManager.WakeLock lockStatic = null;
    private static int mNotificationID = 1296770;

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, mTag);
            lockStatic.setReferenceCounted(true);
        }

        return (lockStatic);
    }

    //Handler that receives messages from the thread
    private static class ServiceHandler extends Handler {
        private final WeakReference<ParseAnimeService> mService;
        private final Context mContext;
        private WebView webview;
        private Parser mParser;

        public ServiceHandler(Looper looper, ParseAnimeService service) {
            super(looper);
            mContext = service;
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                Bundle extras = msg.getData();

                if (extras != null) {
                    final String animeURL = extras.getString(Constants.ANIME_URL);
                    int mType = Parser.getTypeByURL(animeURL);
                    WriteLog.appendLog(mTag, "parsing " + animeURL);
                    if (mType == -1)
                        return;

                    showNotification(animeURL);
                    mParser = Parser.getExistingInstance(mType);
                    if (Parser.isDeadSource(mParser)) {
                        if (MAVApplication.getInstance().getRepository().isFavorite(animeURL)) {
                            FavoriteRecord lRecord = MAVApplication.getInstance().getRepository().getFavoriteByAnimeUrl(animeURL);
                            MAVApplication.getInstance().getRepository().deleteFavorite(animeURL);
                        }
                        return;
                    }
                    if (mParser.isCloudFlareDDOSEnabled) {
                        webview = new WebView(mService.get());
                        WebSettings websettings = webview.getSettings();
                        websettings.setJavaScriptEnabled(true);
                        webview.setWebViewClient(new ParserWebClient(animeURL));
                        webview.loadUrl(mParser.getServerUrl());
                    } else {
                        parseData(animeURL);
                    }
                }
            } finally {
                PowerManager.WakeLock lock = getLock(mContext);

                if (lock.isHeld()) {
                    lock.release();
                }
            }
        }

        private void buildAnimeNotification(int updateNotification, Context context, Anime lTempAnime) {
            if (updateNotification != Constants.UPDATE_NOTIFICATION_NONE) {
                if (mNotificationManager == null)
                    mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                // Sets an ID for the notification, so it can be updated
                Builder mNotifyBuilder = new Builder(context)
                        .setContentTitle(lTempAnime.getTitle())
                        .setContentText("Parsed data from server")
                        .setSmallIcon(R.mipmap.ic_launcher);

                mNotifyBuilder.setWhen(System.currentTimeMillis());
                mNotifyBuilder.setContentTitle(lTempAnime.getTitle());

                mNotifyBuilder.setSmallIcon(R.mipmap.ic_launcher);
                mNotifyBuilder.setOngoing(false);

                // BigPictureStyle
                NotificationCompat.BigPictureStyle s = new NotificationCompat.BigPictureStyle();
                if (!TextUtils.isEmpty(lTempAnime.getCover())) {
                    try {
                        mNotifyBuilder.setLargeIcon(Picasso.with(context).load(lTempAnime.getCover()).get());
                        s.bigLargeIcon(Picasso.with(context).load(lTempAnime.getCover()).get());
                        s.bigPicture(Picasso.with(context).load(lTempAnime.getCover()).get());
                        s.setSummaryText("Parsed data from server");
                    } catch (IOException e) {
                        WriteLog.appendLogException(mTag, "Unable to show image " + lTempAnime.getCover(), e);
                    }
                    mNotifyBuilder.setStyle(s);
                }
                mNotifyBuilder.setAutoCancel(true);
                Intent lIntent2 = new Intent(context, AnimeDetailsActivity.class);
                lIntent2.putExtra(Constants.ANIME_ID, lTempAnime.getId());
                lIntent2.putExtra(Constants.ANIME_URL, lTempAnime.getUrl());
                mNotifyBuilder.setContentIntent(PendingIntent.getActivity(context, lTempAnime.getUrl().hashCode(), lIntent2, PendingIntent.FLAG_UPDATE_CURRENT));
                // Because the ID remains unchanged, the existing notification is updated.
                int notifyID = lTempAnime.getUrl().hashCode();
                if (updateNotification == Constants.UPDATE_NOTIFICATION_ONCE)
                    notifyID = mNotificationID;
                mNotificationManager.notify(
                        notifyID,
                        mNotifyBuilder.getNotification());
                mNotificationMap.delete(lTempAnime.getUrl().hashCode());
            }
        }

        private void showNotification(String animeURL) {
            // Build notification
            Builder notificationBuilder = new Builder(mContext);
            notificationBuilder.setWhen(System.currentTimeMillis());
            notificationBuilder.setContentTitle(animeURL);
            notificationBuilder.setContentText("Parsing required anime data");

            notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_download);
            notificationBuilder.setOngoing(false);
            notificationBuilder.setProgress(0, 0, true);
            Intent i = new Intent(mContext, MainActivity.class);
            notificationBuilder.setContentIntent(PendingIntent.getActivity(mContext, animeURL.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT));

            /**
             RemoteViews contentView = new RemoteViews(mContext.getApplicationContext().getPackageName(), R.layout.download_notif_dark);
             contentView.setImageViewResource(R.id.status_icon, R.mipmap.ic_launcher);
             contentView.setTextViewText(R.id.status_text, "Parsing missing anime " + animeURL);
             contentView.setProgressBar(R.id.status_progress, 50, 0, true);
             contentView.setViewVisibility(R.id.status_progress_wrapper, View.VISIBLE);
             notificationBuilder.setContent(contentView);
             Intent i = new Intent(mContext, MainActivity.class);
             notificationBuilder.setContentIntent(PendingIntent.getActivity(mContext, animeURL.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT));

             final Notification notification = notificationBuilder.getNotification();
             notification.contentView = contentView;
             **/

            if (notificationBuilder != null) {
                mNotificationMap.put(animeURL.hashCode(), animeURL.hashCode());
                mNotificationManager.notify(animeURL.hashCode(), notificationBuilder.build());
            }
        }

        public class ParseAnimeTask extends AsyncTask<String, Void, Anime> {

            @Override
            protected Anime doInBackground(String... values) {
                if (!NetworkUtils.isNetworkAvailable(mContext)) {
                    return null;
                }
                String animeUrl = values[0];
                Episode episode = null;
                int lTempNewEpisodeCount = 0;
                final Anime parseAnime = mParser.getAnimeDetails(animeUrl);
                if (!TextUtils.isEmpty(parseAnime.getUrl()) && !parseAnime.getUrl().equals(animeUrl)) {
                    if (MAVApplication.getInstance().getRepository().isFavorite(animeUrl)) {
                        FavoriteRecord favoriteRecord = MAVApplication.getInstance().getRepository().getFavoriteByAnimeUrl(animeUrl);
                        MAVApplication.getInstance().getRepository().deleteFavorite(animeUrl);
                        if (!TextUtils.isEmpty(parseAnime.getTitle()))
                            MAVApplication.getInstance().getRepository().insertFavorite(parseAnime.getUrl(), (favoriteRecord != null) ? favoriteRecord.getTagId() : -1);
                    }
                    mNotificationManager.cancel(animeUrl.hashCode());
                    showNotification(parseAnime.getUrl());
                    animeUrl = parseAnime.getUrl();
                }

                if (TextUtils.isEmpty(parseAnime.getUrl()) || TextUtils.isEmpty(parseAnime.getTitle())) {
                    WriteLog.appendLog(mTag, "No anime data loaded from url " + animeUrl);
                    //MAVApplication.getInstance().getRepository().deleteFavoriteRecord(animeUrl);
                    //MAVApplication.getInstance().getRepository().deleteAnime(animeUrl);
                    mNotificationManager.cancel(animeUrl.hashCode());
                    mNotificationMap.delete(animeUrl.hashCode());
                    return null;
                } else {
                    WriteLog.appendLog(mTag, "Anime data loaded from " + animeUrl);
                    //if (parseAnime.getEpisodes() != null && !parseAnime.getEpisodes().isEmpty())
                    //parseAnime.setLastEpisodeID(parseAnime.getEpisodes().get(parseAnime.getEpisodes().size()-1).getId());
                    Anime cachedAnime =MAVApplication.getInstance().getRepository().getAnimeByUrl(parseAnime.getUrl());
                    if (cachedAnime == null)
                        MAVApplication.getInstance().getRepository().insertAnime(parseAnime);
                    else {
                        parseAnime.setId(cachedAnime.getId());
                        MAVApplication.getInstance().getRepository().updateAnime(parseAnime);
                    }
                    if (MAVApplication.getInstance().getRepository().getEpisodeCount(parseAnime.getUrl()) != parseAnime.getEpisodes().size()) {
                        if (parseAnime != null && parseAnime.getEpisodes() != null) {
                            List<Episode> newEpisodeList = new ArrayList<>();
                            for (int j = 0; j < parseAnime.getEpisodes().size(); j++) {
                                episode = MAVApplication.getInstance().getRepository().getEpisodeByUrl(parseAnime.getEpisodes().get(j).getUrl());
                                if (episode == null) {
                                    episode = parseAnime.getEpisodes().get(j);
                                    episode.setIndex(j);
                                    episode.setAnime(parseAnime);
                                    newEpisodeList.add(episode);
                                    lTempNewEpisodeCount++;
                                }
                            }
                            MAVApplication.getInstance().getRepository().insertEpisodeList(newEpisodeList);
                        }
                    } else {
                        mNotificationManager.cancel(
                                parseAnime.getUrl().hashCode());
                        return parseAnime;
                    }
                    Intent lIntent = new Intent(Constants.Intents.ANIME_UPDATED);
                    lIntent.putExtra(Constants.ANIME_ID, parseAnime.getId());
                    lIntent.putExtra(Constants.ANIME_URL, parseAnime.getUrl());
                    mContext.sendBroadcast(lIntent);

                    if (mNotificationManager == null)
                        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                    // Sets an ID for the notification, so it can be updated
                    Builder mNotifyBuilder = new Builder(mContext)
                            .setContentTitle(parseAnime.getTitle() + " added")
                            .setContentText(lTempNewEpisodeCount + " added episode(s)")
                            .setSmallIcon(R.mipmap.ic_launcher);

                    mNotifyBuilder.setAutoCancel(true);
                    Intent lIntent2 = new Intent(mContext, MainActivity.class);
                    lIntent2.putExtra(Constants.ANIME_ID, parseAnime.getId());
                    lIntent2.putExtra(Constants.ANIME_URL, parseAnime.getUrl());
                    mNotifyBuilder.setContentIntent(PendingIntent.getActivity(mContext, parseAnime.getUrl().hashCode(), lIntent2, PendingIntent.FLAG_UPDATE_CURRENT));
                    // Because the ID remains unchanged, the existing notification is updated.
                    mNotificationManager.notify(
                            animeUrl.hashCode(),
                            mNotifyBuilder.getNotification());
                    mNotificationMap.delete(animeUrl.hashCode());
                }
                return parseAnime;
            }
        }

        private void parseData(String animeURL) {
            new ParseAnimeTask().execute(animeURL);
        }

        class ParserWebClient extends WebViewClient {

            private final String mAnimeUrl;

            public ParserWebClient(String animeURL) {
                this.mAnimeUrl = animeURL;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String cookies = CookieManager.getInstance().getCookie(url);
                Log.d(mTag, "All the cookies in a string:" + cookies);
                if (!TextUtils.isEmpty(cookies) && cookies.contains("clearance")) {
                    if (mParser.isCloudFlareDDOSEnabled) {
                        mParser.isCloudFlareDDOSPassed = true;
                        mParser.mCustomUserAgent = view.getSettings().getUserAgentString();
                        mParser.mCookies = cookies;
                        parseData(mAnimeUrl);
                    }
                } else {
                    parseData(mAnimeUrl);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d(mTag, "Error code " + errorCode + " Description " + description);
            }
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        //HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        //thread.start();

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationMap = new SparseIntArray();

        // Get the HandlerThread's Looper and use it for our Handler
        //mServiceLooper = thread.getLooper();
        mServiceLooper = Looper.getMainLooper();
        //mServiceHandler = new ServiceHandler(mServiceLooper);
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        PowerManager.WakeLock lock = getLock(this.getApplicationContext());

        if (!lock.isHeld() || (flags & START_FLAG_REDELIVERY) != 0) {
            lock.acquire();
        }

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
        if (mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        for (int i = 0; i < mNotificationMap.size(); i++)
            mNotificationManager.cancel(mNotificationMap.valueAt(i));
    }
}