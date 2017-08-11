package com.taskdesignsinc.android.myanimeviewer.adapter;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.service.download.AnimeDownloadTask;
import com.taskdesignsinc.android.myanimeviewer.service.download.DownloadService;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.Constants.DownloadIntents;

import java.util.ArrayList;
import java.util.HashMap;

public class DownloadListAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<HashMap<Integer, String>> dataList;
    private HashMap<String, HashMap<Integer, String>> dataListCheck;

    public DownloadListAdapter(Context context) {
        mContext = context;
        dataList = new ArrayList<HashMap<Integer, String>>();
        dataListCheck = new HashMap<String, HashMap<Integer, String>>();
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void addItem(String url, String title) {
        addItem(DownloadIntents.SubTypes.QUEUED, url, title, false);
    }

    public void addItem(int subtype, String url, String title, boolean isPaused) {
        if (!dataListCheck.containsKey(url)) {
            HashMap<Integer, String> item = ViewHolder.getItemDataMap(subtype, url, title, null,
                    null, isPaused + "");
            dataListCheck.put(url, item);
            dataList.add(item);
            this.notifyDataSetChanged();
        } else {
            String tmp;
            for (int i = 0; i < dataList.size(); i++) {
                tmp = dataList.get(i).get(ViewHolder.KEY_URL);
                if (tmp.equals(url)) {
                    dataList.get(i).put(ViewHolder.KEY_SUBTYPE, String.valueOf(subtype));
                    dataList.get(i).put(ViewHolder.KEY_URL, url);
                    dataList.get(i).put(ViewHolder.KEY_TITLE, title);
                    dataList.get(i).put(ViewHolder.KEY_IS_PAUSED, Boolean.toString(isPaused));
                    this.notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    public void pauseItem(String url) {
        String tmp;
        for (int i = 0; i < dataList.size(); i++) {
            tmp = dataList.get(i).get(ViewHolder.KEY_URL);
            if (tmp.equals(url)) {
                dataList.get(i).put(ViewHolder.KEY_IS_PAUSED, "true");
                this.notifyDataSetChanged();
                break;
            }
        }
    }

    public void removeItem(String url) {
        String tmp;
        for (int i = 0; i < dataList.size(); i++) {
            tmp = dataList.get(i).get(ViewHolder.KEY_URL);
            if (tmp.equals(url)) {
                dataList.remove(i);
                this.notifyDataSetChanged();
                break;
            }
        }
    }

    public void removeAllItems() {
        dataList.clear();
        this.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(
                    R.layout.download_list_item, null);
        }

        HashMap<Integer, String> itemData = dataList.get(position);
        String url = itemData.get(ViewHolder.KEY_URL);
        convertView.setTag(url);

        ViewHolder viewHolder = new ViewHolder(convertView);
        viewHolder.setData(itemData);

        viewHolder.continueButton.setOnClickListener(new DownloadBtnListener(
                url, viewHolder));
        viewHolder.pauseButton.setOnClickListener(new DownloadBtnListener(url,
                viewHolder));
        viewHolder.deleteButton.setOnClickListener(new DownloadBtnListener(url,
                viewHolder));

        return convertView;
    }

    private class DownloadBtnListener implements View.OnClickListener {
        private String url;
        private ViewHolder mViewHolder;

        public DownloadBtnListener(String url, ViewHolder viewHolder) {
            this.url = url;
            this.mViewHolder = viewHolder;
        }

        @Override
        public void onClick(View v) {
            Intent downloadIntent = new Intent(mContext, DownloadService.class);

            switch (v.getId()) {
                case R.id.btn_continue:
                    // mDownloadManager.continueTask(mPosition);
                    downloadIntent.putExtra(DownloadIntents.TYPE,
                            DownloadIntents.Types.CONTINUE);
                    downloadIntent.putExtra(DownloadIntents.URL, url);
                    mContext.startService(downloadIntent);

                    mViewHolder.continueButton.setVisibility(View.GONE);
                    mViewHolder.pauseButton.setVisibility(View.VISIBLE);
                    break;
                case R.id.btn_pause:
                    // mDownloadManager.pauseTask(mPosition);
                    downloadIntent.putExtra(DownloadIntents.TYPE, DownloadIntents.Types.PAUSE);
                    downloadIntent.putExtra(DownloadIntents.URL, url);
                    mContext.startService(downloadIntent);

                    mViewHolder.continueButton.setVisibility(View.VISIBLE);
                    mViewHolder.pauseButton.setVisibility(View.GONE);
                    break;
                case R.id.btn_delete:
                    // mDownloadManager.deleteTask(mPosition);
                    downloadIntent.putExtra(DownloadIntents.TYPE, DownloadIntents.Types.DELETE);
                    downloadIntent.putExtra(DownloadIntents.URL, url);
                    mContext.startService(downloadIntent);

                    removeItem(url);
                    break;
            }
        }
    }

    public static class ViewHolder {

        public static final int KEY_URL = 0;
        public static final int KEY_TITLE = 1;
        public static final int KEY_SPEED = 2;
        public static final int KEY_PROGRESS = 3;
        public static final int KEY_IS_PAUSED = 4;
        public static final int KEY_SUBTYPE = 5;

        public TextView titleText;
        public ProgressBar progressBar;
        public TextView speedText;
        public ImageButton pauseButton;
        public ImageButton deleteButton;
        public ImageButton continueButton;

        private boolean hasInited = false;

        public ViewHolder(View parentView) {
            if (parentView != null) {
                titleText = (TextView) parentView.findViewById(R.id.title);
                speedText = (TextView) parentView.findViewById(R.id.speed);
                progressBar = (ProgressBar) parentView
                        .findViewById(R.id.progress_bar);
                pauseButton = (ImageButton) parentView.findViewById(R.id.btn_pause);
                deleteButton = (ImageButton) parentView.findViewById(R.id.btn_delete);
                continueButton = (ImageButton) parentView
                        .findViewById(R.id.btn_continue);
                hasInited = true;
            }
        }

        public static HashMap<Integer, String> getItemDataMap(int subType, String url, String title,
                                                              String speed, String progress, String isPaused) {
            HashMap<Integer, String> item = new HashMap<Integer, String>();
            item.put(KEY_SUBTYPE, String.valueOf(subType));
            item.put(KEY_URL, url);
            item.put(KEY_TITLE, title);
            item.put(KEY_SPEED, speed);
            item.put(KEY_PROGRESS, progress);
            item.put(KEY_IS_PAUSED, isPaused);
            return item;
        }

        public void setData(HashMap<Integer, String> item) {
            if (hasInited) {
                titleText
                        .setText(item.get(KEY_TITLE));
                if (Integer.parseInt(item.get(KEY_SUBTYPE)) != DownloadIntents.SubTypes.QUEUED)
                    speedText.setText(item.get(KEY_SPEED));
                else
                    speedText.setText("Queued");
                String progress = item.get(KEY_PROGRESS);
                if (TextUtils.isEmpty(progress)) {
                    progressBar.setProgress(0);
                } else {
                    progressBar.setProgress(Integer.parseInt(progress));
                }
                if (Boolean.parseBoolean(item.get(KEY_IS_PAUSED))) {
                    onPause();
                }
            }
        }

        public void onPause() {
            if (hasInited) {
                pauseButton.setVisibility(View.GONE);
                continueButton.setVisibility(View.VISIBLE);
            }
        }

        public void setData(int subtype, String url, String title, String speed, String progress) {
            setData(subtype, url, title, speed, progress, false + "");
        }

        public void setData(int subtype, String url, String title, String speed, String progress,
                            String isPaused) {
            if (hasInited) {
                HashMap<Integer, String> item = getItemDataMap(subtype, url, title, speed,
                        progress, isPaused);

                titleText
                        .setText(title);
                speedText.setText(speed);
                if (TextUtils.isEmpty(progress)) {
                    progressBar.setProgress(0);
                } else {
                    progressBar
                            .setProgress(Integer.parseInt(item.get(KEY_PROGRESS)));
                }

            }
        }

        public void bindTask(AnimeDownloadTask task) {
            if (hasInited) {
                titleText.setText(task.getTitle());
                speedText.setText(task.getDownloadSpeed() + "kbps | "
                        + task.getDownloadSize() + " / " + task.getTotalSize());
                progressBar.setProgress((int) task.getDownloadPercent());
                if (task.isInterrupt()) {
                    onPause();
                }
            }
        }

    }
}