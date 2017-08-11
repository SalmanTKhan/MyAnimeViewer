package com.taskdesignsinc.android.myanimeviewer.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.OnItemClickListener;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.HistoryRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by salma on 8/1/2017.
 */
public class EpisodeRecyclerAdapter extends RecyclerView.Adapter<EpisodeRecyclerAdapter.EpisodeHolder> {

    private final Context context;
    private LayoutInflater inflater;
    private OnItemClickListener itemClickListener;
    private List<Episode> list;
    private List<Episode> result;

    static int mViewedColor = Color.GRAY;
    static int mUnviewedColor = Color.WHITE;
    static int mLastViewedColor = Color.RED;
    private String lastEpisodeViewed;
    private boolean isLibraryEpisodes = false;

    public EpisodeRecyclerAdapter(Context context, OnItemClickListener itemClickListener) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        list = new ArrayList<>();
        result = new ArrayList<>();
    }

    public EpisodeRecyclerAdapter(Context context, List<Episode> episodes, OnItemClickListener itemClickListener) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        list = episodes;
        Collections.copy(list, result);
    }

    public int getItemPosition(String url) {
        Episode lEpisode = null;
        for (int i = 0; i < result.size(); i++) {
            lEpisode = result.get(i);
            if (lEpisode == null)
                continue;
            if (!isLibraryEpisodes) {
                if (lEpisode.getUrl().equals(url))
                    return i;
            } else {
                if (lEpisode.getLocalPath().equals(url))
                    return i;
            }
        }
        return -1;
    }

    public synchronized void checkEpisodeStatus() {
        if (result != null && !result.isEmpty())
            lastEpisodeViewed = MAVApplication.getInstance().getRepository().getLastViewedEpisode(result.get(0).getAnimeUrl());
        for (Episode episode : result) {
            HistoryRecord historyRecord = MAVApplication.getInstance().getRepository().getHistoryRecord(episode.getUrl());
            if (historyRecord != null) {
                episode.setViewed(historyRecord.isViewed());
            }
        }
    }

    @Override
    public EpisodeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.getContext());
        }
        View view = null;
        EpisodeHolder viewHolder = null;

        view = inflater.inflate(R.layout.episode_item, parent, false);
        viewHolder = new EpisodeHolder(view, this);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(EpisodeHolder holder, int position) {
        Episode episode = result.get(position);

        holder.bind(episode);
        if (!TextUtils.isEmpty(lastEpisodeViewed)) {
            if (!isLibraryEpisodes) {
                if (episode.getUrl().equals(lastEpisodeViewed)) {
                    holder.mTitleView.setTextColor(mLastViewedColor);
                }
            } else {
                if (episode.getLocalPath().equals(lastEpisodeViewed))
                    holder.mTitleView.setTextColor(mLastViewedColor);
            }
        }
    }

    @Override
    public int getItemCount() {
        return result.size();
    }

    public void setData(List<Episode> episodes) {
        result = episodes;
        checkEpisodeStatus();
        notifyDataSetChanged();
    }

    public Episode getItem(int position) {
        if (position < result.size())
            return result.get(position);
        return null;
    }

    public void setLastEpisodeViewed(String lastViewedEpisode) {
        this.lastEpisodeViewed = lastViewedEpisode;
    }

    public static class EpisodeHolder extends RecyclerView.ViewHolder implements
            View.OnTouchListener,
            View.OnClickListener,
            View.OnLongClickListener {

        private final EpisodeRecyclerAdapter adapter;
        private TextView mTitleView = null;
        private CheckBox mCheckView = null;
        private TextView mSubTextView = null;
        private ImageView mDownloadView = null;
        private final View mMenu;
        ColorStateList oldColors = null;

        public EpisodeHolder(View itemView, EpisodeRecyclerAdapter adapter) {
            super(itemView);
            this.itemView.setOnTouchListener(this);
            this.itemView.setOnClickListener(this);
            this.itemView.setOnLongClickListener(this);
            this.adapter = adapter;

            mTitleView = itemView.findViewById(R.id.ei_titleText);
            mCheckView = itemView.findViewById(R.id.ei_chk_box);
            mSubTextView = itemView.findViewById(R.id.ei_subText);
            mDownloadView = itemView.findViewById(R.id.ei_downloadedImage);
            mMenu = itemView.findViewById(R.id.ei_menu);
            if (oldColors == null)
                oldColors = mTitleView.getTextColors();
        }

        public void bind(Episode episode) {
            mCheckView.setVisibility(View.GONE);
            mSubTextView.setVisibility(View.INVISIBLE);
            mTitleView.setText(episode.getTitle());
            if (episode.isViewed())
                mTitleView.setTextColor(mViewedColor);
            else
                mTitleView.setTextColor(mUnviewedColor);
            /**
             if (!TextUtils.isEmpty(pObject.getDate())) {
             mSubTextView.setVisibility(View.VISIBLE);
             mSubTextView.setText(pObject.getDate());
             }
             **/
            if (!TextUtils.isEmpty(episode.getLocalPath())) {
                mDownloadView.setVisibility(View.VISIBLE);
            } else {
                mDownloadView.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            return false;
        }

        @Override
        public void onClick(View view) {
            adapter.itemClickListener.onListItemClick(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            return false;
        }
    }
}
