
package com.taskdesignsinc.android.myanimeviewer.service.download;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.DownloadRecord;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.helper.DownloadRecordMapper;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.FileUtils;
import com.taskdesignsinc.android.myanimeviewer.util.ImageLoaderManager;
import com.taskdesignsinc.android.myanimeviewer.util.StorageUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * AnimeDownloadManager
 *
 * @author Salman T. Khan
 */
public class AnimeDownloadManager extends Thread {

    private static String TAG = AnimeDownloadManager.class.getSimpleName();

    private static final int MAX_TASK_COUNT = Integer.MAX_VALUE;
    private static final int MAX_DOWNLOAD_THREAD_COUNT = 1;

    private Context mContext;

    private TaskQueue mTaskQueue;
    private ArrayList<AnimeDownloadTask> mDownloadingTasks;
    private ArrayList<AnimeDownloadTask> mPausedTasks;

    private Boolean isRunning = false;

    static final String NAME = AnimeDownloadManager.class.getSimpleName();
    private static volatile PowerManager.WakeLock lockStatic = null;

    private int mIndex = 0;
    private boolean isPaused = false;
    private SharedPreferences mPrefs;

    private int generateID() {
        mIndex++;
        return mIndex;
    }

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr =
                    (PowerManager) context.getSystemService(Context.POWER_SERVICE);

            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME);
            lockStatic.setReferenceCounted(true);
        }

        return (lockStatic);
    }

    public AnimeDownloadManager(Context context) {

        mContext = context;
        mTaskQueue = new TaskQueue();
        mDownloadingTasks = new ArrayList<AnimeDownloadTask>();
        mPausedTasks = new ArrayList<AnimeDownloadTask>();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        ImageLoaderManager.getInstance(mContext);
    }


    /**
     * Start download manager thread and call check for incomplete tasks
     */
    public void startManage() {
        if (isRunning())
            return;
        WriteLog.appendLog(TAG,"startManage called");
        isRunning = true;
        this.start();
        checkIncompleteTasks();
    }

    public void close() {
        WriteLog.appendLog("DownloadManager closed");
        isRunning = false;
        pauseAllTask();
        PowerManager.WakeLock lock = getLock(mContext);

        if (lock.isHeld()) {
            lock.release();
        }
        try {
            this.interrupt();
        } catch (Exception e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void run() {

        super.run();
        PowerManager.WakeLock lock = getLock(mContext);
        while (isRunning) {
            AnimeDownloadTask task = mTaskQueue.poll();
            if (task != null) {
                if (!lock.isHeld()) {
                    lock.acquire();
                }
                mDownloadingTasks.add(task);
                task.execute();
            }
        }
        if (lock.isHeld()) {
            lock.release();
        }
    }

    public void addTask(String url) {

        if (!StorageUtils.isSDCardPresent()) {
            Toast.makeText(mContext, "addTask: SD Card isn't mounted", Toast.LENGTH_LONG).show();
            return;
        }

        if (!StorageUtils.isSDCardWritable()) {
            Toast.makeText(mContext, "addTask: SD isn't writable", Toast.LENGTH_LONG).show();
            return;
        }

        if (getTotalTaskCount() >= MAX_TASK_COUNT) {
            Toast.makeText(mContext, "addTask: Too many tasks at once", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            addTask(newDownloadTask(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void addTask(DownloadRecord record) {

        if (!StorageUtils.isSDCardPresent()) {
            Toast.makeText(mContext, "addTask: SD Card isn't mounted", Toast.LENGTH_LONG).show();
            return;
        }

        if (!StorageUtils.isSDCardWritable()) {
            Toast.makeText(mContext, "addTask: SD isn't writable", Toast.LENGTH_LONG).show();
            return;
        }

        if (getTotalTaskCount() >= MAX_TASK_COUNT) {
            Toast.makeText(mContext, "addTask: Too many tasks at once", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            addTask(newDownloadTask(record.getEpisodeUrl()), record.isPaused());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private void addTask(AnimeDownloadTask task) {

        broadcastAddTask(Constants.DownloadIntents.SubTypes.QUEUED, task.getUrl());

        mTaskQueue.offer(task);

        if (!this.isAlive()) {
            this.startManage();
        }
    }

    private void addTask(AnimeDownloadTask task, boolean isPaused) {

        broadcastAddTask((isPaused) ? Constants.DownloadIntents.SubTypes.PAUSED : Constants.DownloadIntents.SubTypes.QUEUED, task.getUrl());

        mTaskQueue.offer(task);

        if (!this.isAlive()) {
            this.startManage();
        }
    }

    private void broadcastAddTask(int subType, String url) {

        broadcastAddTask(subType, url, (subType == Constants.DownloadIntents.SubTypes.PAUSED) ? true : false);
    }

    private void broadcastAddTask(int subType, String url, boolean isInterrupt) {
        WriteLog.appendLog("Broadcasting Task: " + url + "\n Interrupted: " + isInterrupt);
        Intent nofityIntent = new Intent(Constants.Intents.EPISODE_DOWNLOAD);
        nofityIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.ADD);
        nofityIntent.putExtra(Constants.DownloadIntents.SUBTYPE, subType);
        nofityIntent.putExtra(Constants.DownloadIntents.URL, url);
        nofityIntent.putExtra(Constants.DownloadIntents.IS_PAUSED, isInterrupt);
        mContext.sendBroadcast(nofityIntent);
    }

    public void reBroadcastAddAllTask() {
        WriteLog.appendLog(TAG,"Rebroadcasting Tasks");
        AnimeDownloadTask task;
        WriteLog.appendLog(TAG,"Downloading Tasks " + mDownloadingTasks.size());
        for (int i = 0; i < mDownloadingTasks.size(); i++) {
            task = mDownloadingTasks.get(i);
            broadcastAddTask(Constants.DownloadIntents.SubTypes.DOWNLOADING, task.getUrl(), task.isInterrupt());
        }
        WriteLog.appendLog("Queued Tasks " + mTaskQueue.size());
        for (int i = 0; i < mTaskQueue.size(); i++) {
            task = mTaskQueue.get(i);
            broadcastAddTask(Constants.DownloadIntents.SubTypes.QUEUED, task.getUrl());
        }
        WriteLog.appendLog("Paused Tasks " + mPausedTasks.size());
        for (int i = 0; i < mPausedTasks.size(); i++) {
            task = mPausedTasks.get(i);
            broadcastAddTask(Constants.DownloadIntents.SubTypes.PAUSED, task.getUrl(), true);
        }
    }

    public boolean hasTask(String url) {

        AnimeDownloadTask task;
        for (int i = 0; i < mDownloadingTasks.size(); i++) {
            task = mDownloadingTasks.get(i);
            if (task.getUrl().equals(url)) {
                return true;
            }
        }
        for (int i = 0; i < mTaskQueue.size(); i++) {
            task = mTaskQueue.get(i);
        }
        return false;
    }

    public AnimeDownloadTask getTask(int position) {

        if (position >= mDownloadingTasks.size()) {
            return mTaskQueue.get(position - mDownloadingTasks.size());
        } else {
            return mDownloadingTasks.get(position);
        }
    }

    public int getQueueTaskCount() {

        return mTaskQueue.size();
    }

    public int getDownloadingTaskCount() {

        return mDownloadingTasks.size();
    }

    public int getPausingTaskCount() {

        return mPausedTasks.size();
    }

    public int getTotalTaskCount() {

        return getQueueTaskCount() + getDownloadingTaskCount() + getPausingTaskCount();
    }

    public int getDownloadableTaskCount() {

        return getQueueTaskCount() + getDownloadingTaskCount();
    }


    public void checkIncompleteTasks() {
        List<DownloadRecord> incompleteTasks = MAVApplication.getInstance().getRepository().getDownloadTasks(false);
        Collections.sort(incompleteTasks, DownloadRecord.Order.ByPosition);
        if (incompleteTasks.size() > 0) {
            mIndex = incompleteTasks.get(incompleteTasks.size() - 1).getPosition();
            for (int i = 0; i < incompleteTasks.size(); i++) {
                addTask(incompleteTasks.get(i));
            }
        }
    }

    public synchronized void pauseTask(String url) {

        AnimeDownloadTask task;
        for (int i = 0; i < mDownloadingTasks.size(); i++) {
            task = mDownloadingTasks.get(i);
            if (task != null && task.getUrl().equals(url)) {
                pauseTask(task);
                break;
            }
        }
    }

    public synchronized void pauseAllTask() {
        AnimeDownloadTask task;
        for (int i = 0; i < mDownloadingTasks.size(); i++) {
            task = mDownloadingTasks.get(i);
            if (task != null) {
                pauseTask(task);
            }
        }
        for (int i = 0; i < mTaskQueue.size(); i++) {
            task = mTaskQueue.get(i);
            mPausedTasks.add(task);
        }
        mTaskQueue.clear();
        for (int i = 0; i < mPausedTasks.size(); i++) {
            task = mPausedTasks.get(i);
            MAVApplication.getInstance().getRepository().updateDownloadTask(DownloadRecordMapper.map(Constants.DownloadIntents.SubTypes.PAUSED, task));
        }
    }

    public synchronized void deleteTask(String url) {
        WriteLog.appendLog("deleteTask(" + url + ") called");
        AnimeDownloadTask task = null;
        WriteLog.appendLog("deleteTask: checking Downloading Tasks");
        for (int i = 0; i < mDownloadingTasks.size(); i++) {
            task = mDownloadingTasks.get(i);
            if (task != null && task.getUrl().equals(url)) {
                task.onCancelled();
                completeTask(task);
                Episode lEpisode = MAVApplication.getInstance().getRepository().getEpisodeByUrl(url);
                Anime lAnime = (lEpisode != null) ? MAVApplication.getInstance().getRepository().getAnimeByUrl(lEpisode.getAnimeUrl()) : null;
                MAVApplication.getInstance().getRepository().deleteDownloadTask(url);
                if (lAnime != null && lEpisode != null) {
                    File file = new File(StorageUtils.getDataDirectory(),
                            FileUtils.getValidFileName(lAnime.getTitle()) + "/" + FileUtils.getValidFileName(lEpisode.getTitle()) + "/");
                    if (file.exists())
                        FileUtils.deleteDirectory(file);
                } else if (lAnime == null)
                    WriteLog.appendLog("deleteTask: Anime is invalid");
                else if (lEpisode == null)
                    WriteLog.appendLog("deleteTask: Episode is invalid");
                break;
            }
        }
        WriteLog.appendLog("deleteTask: checking Queued Tasks");
        for (int i = 0; i < mTaskQueue.size(); i++) {
            task = mTaskQueue.get(i);
            if (task != null && task.getUrl().equals(url)) {
                if (mTaskQueue.remove(task)) {
                    WriteLog.appendLog("deleteTask: task removed " + task.getUrl());
                    MAVApplication.getInstance().getRepository().deleteDownloadTask(url);
                } else {
                    WriteLog.appendLog("deleteTask: unable to remove task " + task.getUrl());
                }
            }
        }
        WriteLog.appendLog("deleteTask: checking Paused Tasks");
        for (int i = 0; i < mPausedTasks.size(); i++) {
            task = mPausedTasks.get(i);
            if (task != null && task.getUrl().equals(url)) {
                mPausedTasks.remove(task);
                MAVApplication.getInstance().getRepository().deleteDownloadTask(url);
            }
        }
        completeTask(task);
    }

    public synchronized void deleteAllTasks() {
        WriteLog.appendLog("deleteAllTasks() called");
        AnimeDownloadTask task;
        WriteLog.appendLog("deleteAllTasks() Queued Tasks: " + mTaskQueue.size());
        mTaskQueue.clear();
        WriteLog.appendLog("deleteAllTasks() Paused Tasks: " + mPausedTasks.size());
        mPausedTasks.clear();
        WriteLog.appendLog("deleteAllTasks() Downloading Tasks" + mDownloadingTasks.size());
        for (int i = 0; i < mDownloadingTasks.size(); i++) {
            task = mDownloadingTasks.get(i);
            if (task != null) {
                File file = null;
                task.cancel(true);
                Episode lEpisode = MAVApplication.getInstance().getRepository().getEpisodeByUrl(task.getUrl());
                if (lEpisode != null) {
                    Anime lAnime = MAVApplication.getInstance().getRepository().getAnimeByUrl(lEpisode.getAnimeUrl());
                    if (lAnime != null)
                        file = new File(StorageUtils.getDataDirectory(),
                                FileUtils.getValidFileName(lAnime.getTitle()) + "/" + FileUtils.getValidFileName(lEpisode.getTitle()) + "/");
                } else {
                    if (!TextUtils.isEmpty(task.getDownloadPath()))
                        file = new File(task.getDownloadPath());
                }
                if (file != null && file.exists())
                    FileUtils.deleteDirectory(file);
            }
        }
        mDownloadingTasks.clear();
        MAVApplication.getInstance().getRepository().deleteIncompleteDownloadTasks();
        completeAllTask();
    }

    public synchronized void continueTask(String url) {

        AnimeDownloadTask task;
        for (int i = 0; i < mPausedTasks.size(); i++) {
            task = mPausedTasks.get(i);
            if (task != null && task.getUrl().equals(url)) {
                continueTask(task);
            }

        }
    }

    public synchronized void continueAllTasks() {

        AnimeDownloadTask task;
        Collections.sort(mPausedTasks, AnimeDownloadTask.Order.ByIndex);
        for (int i = 0; i < mPausedTasks.size(); i++) {
            task = mPausedTasks.get(i);
            if (task != null) {
                continueTaskOnly(task);
            }
        }
        mPausedTasks.clear();
    }

    public synchronized void pauseTask(AnimeDownloadTask task) {

        if (task != null) {
            int taskId = task.getId();
            task.cancel(true);

            // move to pausing list
            String url = task.getUrl();
            try {
                mDownloadingTasks.remove(task);
                task = newDownloadTask(url);
                task.setId(taskId);
                mPausedTasks.add(task);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            NotificationManager mNotifyManager = null;
            if (mContext != null)
                mNotifyManager =
                        (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            if (mNotifyManager != null && task != null)
                mNotifyManager.cancel(task.getAnimeUrl().hashCode());
            // notify list changed
            Intent notifyIntent = new Intent(Constants.Intents.EPISODE_DOWNLOAD);
            notifyIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.PAUSE);
            notifyIntent.putExtra(Constants.DownloadIntents.URL, task.getUrl());
            mContext.sendBroadcast(notifyIntent);
            MAVApplication.getInstance().getRepository().updateDownloadTask(DownloadRecordMapper.map(Constants.DownloadIntents.SubTypes.PAUSED, task));
            //MAVApplication.getInstance().getRepository().updateDownload(task.getId(), task.getAnimeUrl(), task.getUrl(), task.getDownloadPath(), task.getDownloadSize(), task.getTotalSize(), Constants.DownloadIntents.SubTypes.PAUSED);
        }
    }

    public synchronized void continueTask(AnimeDownloadTask task) {
        if (task != null) {
            mPausedTasks.remove(task);
            mTaskQueue.offer(task);
        }
    }

    public synchronized void continueTaskOnly(AnimeDownloadTask task) {
        if (task != null) {
            mTaskQueue.offer(task);
        }
    }

    public synchronized void completeTask(AnimeDownloadTask task) {
        if (mDownloadingTasks.contains(task)) {
            mDownloadingTasks.remove(task);

            // notify list changed
            Intent notifyIntent = new Intent(Constants.Intents.EPISODE_DOWNLOAD);
            notifyIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.COMPLETE);
            notifyIntent.putExtra(Constants.DownloadIntents.URL, task.getUrl());
            if (task != null) {
                notifyIntent.putExtra(Constants.ANIME_URL, task.getAnimeUrl());
            }
            mContext.sendBroadcast(notifyIntent);
        }
    }

    public synchronized void completeAllTask() {
        mDownloadingTasks.clear();
        // notify list changed
        Intent notifyIntent = new Intent(Constants.Intents.EPISODE_DOWNLOAD);
        notifyIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.COMPLETE_ALL);
        mContext.sendBroadcast(notifyIntent);
    }

    /**
     * Create a new download task with default config
     *
     * @param url
     * @return
     * @throws MalformedURLException
     */
    private AnimeDownloadTask newDownloadTask(String url) throws MalformedURLException {

        AnimeDownloadTaskListener taskListener = new AnimeDownloadTaskListener() {

            @Override
            public void updateProcess(AnimeDownloadTask task) {
                updateProcess(task, null);
            }

            @Override
            public void updateProcess(AnimeDownloadTask task, String msg) {
                Intent updateIntent = new Intent(
                        Constants.Intents.EPISODE_DOWNLOAD);
                updateIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.PROCESS);
                if (TextUtils.isEmpty(msg))
                    updateIntent.putExtra(
                            Constants.DownloadIntents.PROCESS_SPEED,
                            FileUtils.getFileSize(task.getDownloadSpeed()) + " | "
                                    + FileUtils.getFileSize(task.getDownloadSize()) + " / "
                                    + FileUtils.getFileSize(task.getTotalSize()));
                else
                    updateIntent.putExtra(Constants.DownloadIntents.PROCESS_SPEED, msg);
                updateIntent.putExtra(Constants.DownloadIntents.PROCESS_PROGRESS, task.getDownloadPercent());
                updateIntent.putExtra(Constants.DownloadIntents.URL, task.getUrl());
                updateIntent.putExtra(Constants.DownloadIntents.TITLE, task.getTitle());
                MAVApplication.getInstance().getRepository().updateDownloadTask(DownloadRecordMapper.map(Constants.DownloadIntents.SubTypes.DOWNLOADING, task));
                mContext.sendBroadcast(updateIntent);
            }

            @Override
            public void preDownload(AnimeDownloadTask task) {
                MAVApplication.getInstance().getRepository().insertDownloadTask(DownloadRecordMapper.map(Constants.DownloadIntents.SubTypes.QUEUED, task));
            }

            @Override
            public void finishDownload(AnimeDownloadTask task) {
                MAVApplication.getInstance().getRepository().updateDownloadTask(DownloadRecordMapper.map(Constants.DownloadIntents.SubTypes.COMPLETED, task));
                completeTask(task);
            }

            @Override
            public void errorDownload(AnimeDownloadTask task, Throwable error) {

                if (error != null) {
                    WriteLog.appendLog("Error: " + error.getMessage());
                    Toast.makeText(mContext, "Error: " + error.getMessage(), Toast.LENGTH_LONG)
                            .show();
                }

                // Intent errorIntent = new
                // Intent(Constants.Intents.EPISODE_DOWNLOAD);
                // errorIntent.putExtra(DownloadIntents.TYPE, DownloadIntents.Types.ERROR);
                // errorIntent.putExtra(DownloadIntents.ERROR_CODE, error);
                // errorIntent.putExtra(DownloadIntents.ERROR_INFO,
                // DownloadTask.getErrorInfo(error));
                // errorIntent.putExtra(DownloadIntents.URL, task.getUrl());
                // mContext.sendBroadcast(errorIntent);
                //
                // if (error != DownloadTask.ERROR_UNKOWN_HOST
                // && error != DownloadTask.ERROR_BLOCK_INTERNET
                // && error != DownloadTask.ERROR_TIME_OUT) {
                // completeTask(task);
                // }
            }
        };
        return new AnimeDownloadTask(generateID(), mContext, url, taskListener);
    }

    /**
     * A obstructed task queue
     *
     * @author Yingyi Xu
     */
    private class TaskQueue {
        private Queue<AnimeDownloadTask> taskQueue;

        public TaskQueue() {

            taskQueue = new LinkedList<AnimeDownloadTask>();
        }

        public void offer(AnimeDownloadTask task) {

            taskQueue.offer(task);
        }

        public AnimeDownloadTask poll() {

            AnimeDownloadTask task = null;
            while (mDownloadingTasks.size() >= MAX_DOWNLOAD_THREAD_COUNT
                    || (task = taskQueue.poll()) == null) {
                try {
                    Thread.sleep(1000); // sleep
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return task;
        }

        public AnimeDownloadTask get(int position) {

            if (position >= size()) {
                return null;
            }
            return ((LinkedList<AnimeDownloadTask>) taskQueue).get(position);
        }

        public int size() {

            return taskQueue.size();
        }

        @SuppressWarnings("unused")
        public boolean remove(int position) {

            return taskQueue.remove(get(position));
        }

        public boolean remove(AnimeDownloadTask task) {

            return taskQueue.remove(task);
        }

        public void clear() {
            taskQueue.clear();
        }
    }
}
