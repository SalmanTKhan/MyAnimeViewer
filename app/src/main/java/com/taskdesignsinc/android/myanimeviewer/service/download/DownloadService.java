
package com.taskdesignsinc.android.myanimeviewer.service.download;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.Constants.DownloadIntents;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

public class DownloadService extends Service {

    private AnimeDownloadManager mDownloadManager;

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {

        super.onCreate();
        mDownloadManager = new AnimeDownloadManager(this.getApplicationContext());
    }
    
    @Override
    public void onDestroy()
    {
    	mDownloadManager.close();
        super.onDestroy();
    }

    @Override
	public int onStartCommand(Intent pIntent, int pFlags, int pStartId) {
    	
    	WriteLog.appendLog("onStartCommand called");
    	
    	if (pIntent != null) {
            int type = pIntent.getIntExtra(DownloadIntents.TYPE, -1);
            String url;

            switch (type) {
                case DownloadIntents.Types.START:
                	WriteLog.appendLog("Intent Type: Start");
                    if (!mDownloadManager.isRunning()) {
                        mDownloadManager.startManage();
                    } else {
                        mDownloadManager.reBroadcastAddAllTask();
                    }
                    break;
                case DownloadIntents.Types.ADD:
                    url = pIntent.getStringExtra(DownloadIntents.URL);
                    WriteLog.appendLog("Intent Type: Add " + url);
                    if (!TextUtils.isEmpty(url) && !mDownloadManager.hasTask(url)) {
                        mDownloadManager.addTask(url);
                    }
                    break;
                case DownloadIntents.Types.CONTINUE:
                    url = pIntent.getStringExtra(DownloadIntents.URL);
                    WriteLog.appendLog("Intent Type: Continue " + url);
                    if (!TextUtils.isEmpty(url)) {
                        mDownloadManager.continueTask(url);
                    }
                    break;
                case DownloadIntents.Types.DELETE:
                    url = pIntent.getStringExtra(DownloadIntents.URL);
                    WriteLog.appendLog("Intent Type: Delete " + url);
                    if (!TextUtils.isEmpty(url)) {
                        mDownloadManager.deleteTask(url);
                    }
                    break;
                case DownloadIntents.Types.PAUSE:
                    url = pIntent.getStringExtra(DownloadIntents.URL);
                    WriteLog.appendLog("Intent Type: Pause " + url);
                    if (!TextUtils.isEmpty(url)) {
                        mDownloadManager.pauseTask(url);
                    }
                    break;
                case DownloadIntents.Types.STOP:
                	WriteLog.appendLog("Intent Type: Stop");
                	if (mDownloadManager.getDownloadableTaskCount() == 0) {
                		mDownloadManager.close();
                		// mDownloadManager = null;
                		stopSelf();
                	}
                    return START_NOT_STICKY;
                case DownloadIntents.Types.CONTINUE_ALL:
                	WriteLog.appendLog("Intent Type: Continue All");
                	mDownloadManager.continueAllTasks();
                	break;
                case DownloadIntents.Types.PAUSE_ALL:
                	WriteLog.appendLog("Intent Type: Pause All");
                	mDownloadManager.pauseAllTask();
                	break;
                	
                case DownloadIntents.Types.DELETE_ALL:
                	WriteLog.appendLog("Intent Type: Delete All");
                	mDownloadManager.deleteAllTasks();
                	break;

                default:
                    break;
            }
        }
		return START_STICKY;
    }

}
