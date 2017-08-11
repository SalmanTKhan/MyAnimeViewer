
package com.taskdesignsinc.android.myanimeviewer;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.taskdesignsinc.android.myanimeviewer.adapter.DownloadListAdapter;
import com.taskdesignsinc.android.myanimeviewer.base.SupportListActivity;
import com.taskdesignsinc.android.myanimeviewer.service.download.DownloadService;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.NetworkUtils;
import com.taskdesignsinc.android.myanimeviewer.util.StorageUtils;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.io.IOException;

public class DownloadListActivity extends SupportListActivity {

	private DownloadListAdapter mAdapter;
	private DownloadResultReceiver mReceiver;
	private SharedPreferences mPrefs;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		ThemeManager.getInstance().setCurrentTheme(this);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.download_list_activity);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		if (!StorageUtils.isSDCardPresent()) {
			Toast.makeText(this, "SD card not found.", Toast.LENGTH_LONG).show();
			return;
		}

		if (!StorageUtils.isSDCardWritable()) {
			Toast.makeText(this, "SD card not writable", Toast.LENGTH_LONG).show();
			return;
		}

		//downloadList = (ListView) findViewById(R.id.download_list);
		getSupportActionBar().setTitle(R.string.downloads);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		getListView().setDivider(new ColorDrawable(ThemeManager.getInstance().getBackgroundColor(this)));
		mAdapter = new DownloadListAdapter(this);
		setListAdapter(mAdapter);

		if (mAdapter.isEmpty()) {
			TextView empty = (TextView) getListView().getEmptyView();
			empty.setText("Nothing is downloading right now");
		}

		startDownloadManager();

		// // handle intent
		// Intent intent = getIntent();
		// handleIntent(intent);
		mReceiver = new DownloadResultReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.Intents.EPISODE_DOWNLOAD);
		registerReceiver(mReceiver, filter);
	}
	
	private void startDownloadManager() {
		boolean lWifiOnly = mPrefs.getBoolean(Constants.KEY_DOWNLOAD_WIFI_ONLY, false);
		if ((!lWifiOnly
				&& NetworkUtils.isNetworkAvailable(this))
				|| (lWifiOnly
						&& NetworkUtils.isWiFiConnected(this))) {
			Intent downloadIntent = new Intent(this,
					DownloadService.class);
			downloadIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.START);
			startService(downloadIntent);
		} else {
			Intent downloadIntent = new Intent(this,
					DownloadService.class);
			downloadIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.STOP);
			startService(downloadIntent);
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu pMenu) {
		pMenu.add("Continue All");
		pMenu.add("Pause All");
		pMenu.add("Delete All");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem pItem) {
		if(pItem.getItemId() == android.R.id.home)
		{
			finish();
			return true;
		} else if (pItem.getTitle().equals("Continue All")) {
			Intent downloadIntent = new Intent(this,
					DownloadService.class);
			downloadIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.CONTINUE_ALL);
			startService(downloadIntent);
			return true;
		} else if (pItem.getTitle().equals("Pause All")) {
			Intent downloadIntent = new Intent(this,
					DownloadService.class);
			downloadIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.PAUSE_ALL);
			startService(downloadIntent);
			return true;
		} else if (pItem.getTitle().equals("Delete All")) {
			Intent downloadIntent = new Intent(this,
					DownloadService.class);
			downloadIntent.putExtra(Constants.DownloadIntents.TYPE, Constants.DownloadIntents.Types.DELETE_ALL);
			startService(downloadIntent);
			return true;
		}
		return super.onOptionsItemSelected(pItem);
	}

	public class DownloadResultReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			handleIntent(intent);

		}

		private void handleIntent(Intent intent) {

			if (intent != null
					&& intent.getAction().equals(
							Constants.Intents.EPISODE_DOWNLOAD)) {
				int type = intent.getIntExtra(Constants.DownloadIntents.TYPE, -1);
				int subtype = intent.getIntExtra(Constants.DownloadIntents.SUBTYPE, -1);
				String url;
				String title;

				switch (type) {
				case Constants.DownloadIntents.Types.ADD:
					url = intent.getStringExtra(Constants.DownloadIntents.URL);
					title = intent.getStringExtra(Constants.DownloadIntents.TITLE);
					if (title == null)
						title = MAVApplication.getInstance().getRepository().getEpisodeTitle(url);
					boolean isPaused = intent.getBooleanExtra(Constants.DownloadIntents.IS_PAUSED, false);
					if (!TextUtils.isEmpty(url)) {
						mAdapter.addItem(subtype, url, title, isPaused);
					}
					break;
				case Constants.DownloadIntents.Types.COMPLETE:
					url = intent.getStringExtra(Constants.DownloadIntents.URL);
					if (!TextUtils.isEmpty(url)) {
						mAdapter.removeItem(url);
					}
					break;
				case Constants.DownloadIntents.Types.PROCESS:
					url = intent.getStringExtra(Constants.DownloadIntents.URL);
					title = intent.getStringExtra(Constants.DownloadIntents.TITLE);
					if (title == null)
						title = MAVApplication.getInstance().getRepository().getEpisodeTitle(url);
					View taskListItem = getListView().findViewWithTag(url);
					DownloadListAdapter.ViewHolder viewHolder = new DownloadListAdapter.ViewHolder(taskListItem);
					viewHolder.setData(Constants.DownloadIntents.SubTypes.DOWNLOADING, url, title, intent.getStringExtra(Constants.DownloadIntents.PROCESS_SPEED),
							String.valueOf(intent.getLongExtra(Constants.DownloadIntents.PROCESS_PROGRESS, 0)));
					break;
				case Constants.DownloadIntents.Types.PAUSE:
					url = intent.getStringExtra(Constants.DownloadIntents.URL);
					if (!TextUtils.isEmpty(url)) {
						mAdapter.pauseItem(url);
					}
					break;
				case Constants.DownloadIntents.Types.ERROR:
					url = intent.getStringExtra(Constants.DownloadIntents.URL);
					// int errorCode =
					// intent.getIntExtra(DownloadIntents.ERROR_CODE,
					// DownloadTask.ERROR_UNKONW);
					// handleError(url, errorCode);
					break;
				case Constants.DownloadIntents.Types.COMPLETE_ALL:
					mAdapter.removeAllItems();
					break;
				default:
					break;
				}
			}
		}

		@SuppressWarnings("unused")
		private void showAlert(String title, String msg) {

			new AlertDialog.Builder(DownloadListActivity.this).setTitle(title).setMessage(msg)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

					dialog.dismiss();
				}
			}).create().show();
		}
	}
}
