package com.taskdesignsinc.android.myanimeviewer.util;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

public class AsyncTaskUtils {

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
		}
		else {
			task.execute(params);
		}
	}

}
