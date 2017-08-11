package com.taskdesignsinc.android.myanimeviewer.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.HistoryRecord;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.FileUtils;
import com.taskdesignsinc.android.myanimeviewer.util.StorageUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.myanimeviewer.view.HeaderGridView;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EpisodeListAdapter extends ArrayAdapter<Episode> {
    private final LayoutInflater mInflater;
    List<Episode> mList;
    List<Episode> mResults;
    private EpisodeFilter mFilter;
    private final String mPath;
    private boolean mIsLibraryEpisodes;

    Anime mAnime = null;

    private SharedPreferences mPrefs;
    static int mViewedColor = Color.GRAY;
    static int mUnviewedColor = Color.WHITE;
    static int mLastViewedColor = Color.RED;

    @Override
    public Episode getItem(int pPosition) {
        if (mResults == null || mResults.isEmpty() || pPosition >= mResults.size() || pPosition < 0)
            return null;
        return mResults.get(pPosition);
    }

    private Comparator<? super Episode> mComparator;
    private int mDisplayType = 1;
    private int mSecondaryType = -1;
    public static final int NONE = -1;

    private String mLastEpisodeViewed;
    private boolean mHideViewedEpisodes;

    public boolean isHideViewed() {
        return mHideViewedEpisodes;
    }

    public void setHideViewed(boolean hideRead) {
        mHideViewedEpisodes = hideRead;
        if (mAnime != null)
            setData(mAnime);
        else
            checkEpisodeStatus();
    }

    public EpisodeListAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mList = new ArrayList<Episode>();
        mResults = new ArrayList<Episode>();
        mPath = "";

        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (mPrefs != null) {
            mViewedColor = mPrefs.getInt(Constants.KEY_EPISODE_VIEWED_COLOR, Color.GRAY);
            mUnviewedColor = mPrefs.getInt(Constants.KEY_EPISODE_UNVIEWED_COLOR, ThemeManager.getInstance(context).getTextColor());
            if (!ThemeManager.getInstance().isValidTextColor(mUnviewedColor))
                mUnviewedColor = ThemeManager.getInstance(context).getTextColor();
            mLastViewedColor = mPrefs.getInt(Constants.KEY_EPISODE_LAST_VIEWED_COLOR, Color.RED);
        }
    }

    public EpisodeListAdapter(Context context, boolean hideViewed) {
        super(context, R.layout.episode_item);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mList = new ArrayList<Episode>();
        mResults = new ArrayList<Episode>();
        mPath = "";

        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (mPrefs != null) {
            mViewedColor = mPrefs.getInt(Constants.KEY_EPISODE_VIEWED_COLOR, Color.GRAY);
            mUnviewedColor = mPrefs.getInt(Constants.KEY_EPISODE_UNVIEWED_COLOR, ThemeManager.getInstance(context).getTextColor());
            if (!ThemeManager.getInstance().isValidTextColor(mUnviewedColor))
                mUnviewedColor = ThemeManager.getInstance(context).getTextColor();
            mLastViewedColor = mPrefs.getInt(Constants.KEY_EPISODE_LAST_VIEWED_COLOR, Color.RED);
        }
    }

    public EpisodeListAdapter(Context context, String path, boolean hideViewedEpisodes, boolean isLibraryEpisodes) {
        super(context, R.layout.episode_item);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mList = new ArrayList<Episode>();
        mResults = new ArrayList<Episode>();
        mPath = path;
        mHideViewedEpisodes = hideViewedEpisodes;
        mIsLibraryEpisodes = isLibraryEpisodes;

        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (mPrefs != null) {
            mViewedColor = mPrefs.getInt(Constants.KEY_EPISODE_VIEWED_COLOR, Color.GRAY);
            mUnviewedColor = mPrefs.getInt(Constants.KEY_EPISODE_UNVIEWED_COLOR, ThemeManager.getInstance(context).getTextColor());
            if (!ThemeManager.getInstance().isValidTextColor(mUnviewedColor))
                mUnviewedColor = ThemeManager.getInstance(context).getTextColor();
            mLastViewedColor = mPrefs.getInt(Constants.KEY_EPISODE_LAST_VIEWED_COLOR, Color.RED);
        }
    }

    public int getItemPosition(String url) {
        Episode lEpisode = null;
        for (int i = 0; i < mResults.size(); i++) {
            lEpisode = mResults.get(i);
            if (lEpisode == null)
                continue;
            if (!mIsLibraryEpisodes) {
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
        mLastEpisodeViewed = MAVApplication.getInstance().getRepository().getLastViewedEpisode(mAnime.getUrl());
        List<HistoryRecord> historyRecords = MAVApplication.getInstance().getRepository().getHistoryRecords(mAnime.getUrl());
        for (HistoryRecord historyRecord : historyRecords) {
            int pos = getItemPosition(historyRecord.getEpisodeUrl());
            if (pos != -1) {
                mResults.get(pos).setViewed(historyRecord.isViewed());
            }
        }
    }

    public void setDisplayType(int type) {
        mDisplayType = type;
    }

    /* (non-Javadoc)
     * @see android.widget.ArrayAdapter#getCount()
     */
    @Override
    public int getCount() {
        return mResults.size();
    }

    @Override
    public void sort(Comparator<? super Episode> pComparator) {
        mComparator = pComparator;
        Collections.sort(mResults, pComparator);
        super.sort(pComparator);
    }

    @Override
    public void notifyDataSetChanged() {
        if (mComparator != null)
            Collections.sort(mResults, mComparator);
        super.notifyDataSetChanged();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setData(Anime data) {
        clear();
        mList.clear();
        mResults.clear();
        if (data != null) {
            mAnime = data;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                addAll(data.getEpisodes());
                mList.addAll(data.getEpisodes());
            } else {
                for (Episode obj : data.getEpisodes()) {
                    add(obj);
                    mList.add(obj);
                }
            }
            if (mComparator != null)
                Collections.sort(mList, mComparator);
            mResults = new ArrayList<>(mList);
            checkEpisodeStatus();
        } else {
            clear();
            mList.clear();
            mResults.clear();
        }
    }

    /**
     * Populate new items in the list.
     */
    @NonNull
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        EpisodeHolder holder = null;

        if (convertView == null) {
            row = mInflater.inflate(R.layout.episode_item, null);
            holder = new EpisodeHolder(row);
            row.setTag(holder);
        } else {
            holder = (EpisodeHolder) row.getTag();
        }

        if (position < mResults.size()) {
            final Episode lEpisode = mResults.get(position);
            holder.populateFrom(lEpisode);
            holder.mMenu.setVisibility(View.INVISIBLE);
            holder.mMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //CastUtils.showQueuePopup(getContext(), view, MediaInfoUtils.buildMediaInfo(mAnime, lEpisode));
                }
            });
            if (!TextUtils.isEmpty(mLastEpisodeViewed)) {
                if (!mIsLibraryEpisodes) {
                    if (mResults.get(position).getUrl().equals(mLastEpisodeViewed)) {
                        holder.mTitleView.setTextColor(mLastViewedColor);
                    }
                } else {
                    if (mResults.get(position).getLocalPath().equals(mLastEpisodeViewed))
                        holder.mTitleView.setTextColor(mLastViewedColor);
                }
            }

            AbsListView lv = (AbsListView) parent;
            if (lv.getChoiceMode() == AbsListView.CHOICE_MODE_MULTIPLE) {
                position += 2;
                SparseBooleanArray checkArray;
                checkArray = lv.getCheckedItemPositions();

                holder.mCheckView.setVisibility(View.VISIBLE);
                holder.mCheckView.setChecked(false);
                if (checkArray != null) {
                    if (checkArray.get(position)) {
                        holder.mCheckView.setChecked(true);

                    }
                }
            }
        }

        return row;
    }

    public void setLastEpisodeViewed(Episode lastEpisodeViewed) {
        if (lastEpisodeViewed != null)
            mLastEpisodeViewed = lastEpisodeViewed.getUrl();
    }

    static class EpisodeHolder {
        private TextView mTitleView = null;
        private CheckBox mCheckView = null;
        private TextView mSubTextView = null;
        private ImageView mDownloadView = null;
        private final View mMenu;
        ColorStateList oldColors = null;

        public static Context mContext;

        EpisodeHolder(View row) {
            mTitleView = row.findViewById(R.id.ei_titleText);
            mCheckView = row.findViewById(R.id.ei_chk_box);
            mSubTextView = row.findViewById(R.id.ei_subText);
            mDownloadView = row.findViewById(R.id.ei_downloadedImage);
            mMenu = row.findViewById(R.id.ei_menu);
            if (oldColors == null)
                oldColors = mTitleView.getTextColors();
        }

        void populateFrom(final Episode episode) {
            mCheckView.setVisibility(View.GONE);
            mSubTextView.setVisibility(View.INVISIBLE);
            mTitleView.setText(episode.getTitle());
            mTitleView.setTextColor(oldColors);
            if (episode.isViewed())
                mTitleView.setTextColor(mViewedColor);
            else
                mTitleView.setTextColor(mUnviewedColor);
            /**
            if (!TextUtils.isEmpty(episode.getDate())) {
                mSubTextView.setVisibility(View.VISIBLE);
                mSubTextView.setText(episode.getDate());
            }
             **/
            if (!TextUtils.isEmpty(episode.getLocalPath())) {
                mDownloadView.setVisibility(View.VISIBLE);
            } else {
                mDownloadView.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null)
            mFilter = new EpisodeFilter();

        return mFilter;
    }

    private class EpisodeFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            // We implement here the filter logic
            if (constraint == null || constraint.length() == 0) {
                // No filter implemented we return all the list
                results.values = mList;
                results.count = mList.size();
            } else {
                // We perform filtering operation
                List<Episode> nEpisodeList = new ArrayList<Episode>();

                for (Episode p : mList) {
                    if (p.getTitle().toUpperCase().startsWith(constraint.toString().toUpperCase()))
                        nEpisodeList.add(p);
                }

                results.values = nEpisodeList;
                results.count = nEpisodeList.size();

            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {

            // Now we have to inform the adapter about the new list filtered
            if (results.count == 0)
                notifyDataSetInvalidated();
            else {
                mResults = (List<Episode>) results.values;
                notifyDataSetChanged();
            }

        }
    }
}
