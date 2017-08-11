package com.taskdesignsinc.android.myanimeviewer.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.FlexibleAdapter;
import com.taskdesignsinc.android.myanimeviewer.model.OfflineHistoryRecord;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.FileUtils;
import com.taskdesignsinc.android.myanimeviewer.util.ImageLoaderManager;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class OfflineHistoryRecyclerAdapter extends FlexibleAdapter<OfflineHistoryRecyclerAdapter.ViewHolder, OfflineHistoryRecord> implements Filterable {

    private static final String TAG = OfflineHistoryRecyclerAdapter.class.getSimpleName();

    public void sort(Comparator<OfflineHistoryRecord> comparator) {
        Collections.sort(mResults, comparator);
        notifyDataSetChanged();
    }

    public void setFilterType(int filterType) {
        mFilterType = filterType;
    }

    @Override
    public Filter getFilter() {
        switch (mFilterType) {
            case 0:
                mFilter = new RecordFilter();
                break;
            case 1:
                mFilter = new RecordCustomFilter();
                break;
        }
        return mFilter;
    }

    public interface OnItemClickListener {
        /**
         * Delegate the click event to the listener and check if selection MULTI enabled.<br/>
         * If yes, call toggleActivation.
         *
         * @param position
         * @return true if MULTI selection is enabled, false for SINGLE selection
         */
        boolean onListItemClick(int position);

        /**
         * This always calls toggleActivation after listener event is consumed.
         *
         * @param position
         */
        void onListItemLongClick(int position);
    }

    private Context mContext;
    List<OfflineHistoryRecord> mResults;
    private LayoutInflater mInflater;
    private OnItemClickListener mClickListener;
    //Selection fields
    private boolean
            mLastItemInActionMode = false,
            mSelectAll = false;

    SharedPreferences mPrefs;

    private Filter mFilter;
    private int mFilterType = 0;

    public OfflineHistoryRecyclerAdapter(List<OfflineHistoryRecord> mangaList, Context context, OnItemClickListener listener) {
        this.mContext = context;
        this.mClickListener = listener;
        mResults = new ArrayList<OfflineHistoryRecord>();
        //checkLibraryRecord(mangaList);
        this.mItems = mangaList;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mReadColor = mPrefs.getInt(Constants.KEY_EPISODE_VIEWED_COLOR, Color.GRAY);
        mUnreadColor = mPrefs.getInt(Constants.KEY_EPISODE_UNVIEWED_COLOR, ThemeManager.getInstance(context).getTextColor());
        if (!ThemeManager.getInstance().isValidTextColor(mUnreadColor))
            mUnreadColor = ThemeManager.getInstance(context).getTextColor();
        mLastReadColor = mPrefs.getInt(Constants.KEY_EPISODE_LAST_VIEWED_COLOR, Color.RED);
    }

    public void updateDataSet(String param) {
        //Fill mItems with your custom list
        //this.mItems = data;
    }

    public void clear() {
        if (mResults != null) {
            int size = this.mResults.size();
            if (mItems != null)
                mItems.clear();
            if (mResults != null)
                mResults.clear();
            notifyItemRangeRemoved(0, size);
        } else {
            notifyItemRangeRemoved(0, 0);
        }
    }

    public void clearData() {
        if (mResults != null) {
            int size = mResults.size();
            if (size > 0) {
                if (mItems != null)
                    mItems.clear();
                if (mResults != null)
                    mResults.clear();
                this.notifyDataSetChanged();
            }
        } else {
            this.notifyDataSetChanged();
        }
    }

    public void addItem(OfflineHistoryRecord manga) {
        if (manga == null)
            return;
        if (mItems != null) {
            this.mItems.add(manga);
        }
        if (mResults != null)
            mResults.add(manga);
        notifyItemInserted(mResults.size() - 1);
    }

    public void addItem(int position, OfflineHistoryRecord manga) {
        if (manga == null)
            return;
        if (mItems != null) {
            this.mItems.add(position, manga);
        }
        if (mResults != null)
            mResults.add(position, manga);
        notifyItemInserted(position);
    }

    public void addLibraryRecordList(List<OfflineHistoryRecord> offlineRecords) {
        if (offlineRecords == null || offlineRecords == Collections.EMPTY_LIST)
            return;
        //checkLibraryRecord(offlineRecords);
        int startPos = 0;
        if (mItems != null) {
            startPos = this.mItems.size();
            this.mItems.addAll(offlineRecords);
        } else {
            this.mItems = offlineRecords;
        }
        mResults = (List<OfflineHistoryRecord>) ((ArrayList<OfflineHistoryRecord>) mItems).clone();
        if (offlineRecords.size() > 1)
            this.notifyItemRangeInserted(startPos, offlineRecords.size() - 1);
        else
            this.notifyItemRangeInserted(startPos, 1);
    }

    @Override
    public int getItemCount() {
        return mResults.size();
    }

    public OfflineHistoryRecord getItem(int position) {
        return mResults.get(position);
    }

    public OfflineHistoryRecord getItem(String url) {
        for (OfflineHistoryRecord lOfflineHistoryRecord : mResults) {
            if (lOfflineHistoryRecord.getPath().equals(url))
                return lOfflineHistoryRecord;
        }
        return null;
    }

    public int getItemPosition(String url) {
        for (int i = 0; i < mResults.size(); i++) {
            if (mResults.get(i).getPath().equals(url))
                return i;
        }
        return -1;
    }

    @Override
    public void setMode(int mode) {
        super.setMode(mode);
        notifyDataSetChanged();
        if (mode == MODE_SINGLE) mLastItemInActionMode = true;
    }

    @Override
    public void selectAll() {
        mSelectAll = true;
        super.selectAll();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder for viewType " + viewType);
        if (mInflater == null) {
            mInflater = LayoutInflater.from(parent.getContext());
        }
        View v = mInflater.inflate(R.layout.history_material_row, parent, false);
        ViewHolder viewHolder = new ViewHolder(v, this);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        Log.d(TAG, "onBindViewHolder for position " + position);
        final OfflineHistoryRecord lOfflineHistoryRecord = mResults.get(position);

        //When user scrolls this bind the correct selection status
        viewHolder.bindLibraryRecord(lOfflineHistoryRecord);
        viewHolder.itemView.setActivated(isSelected(position));

        if (mSelectAll || mLastItemInActionMode) {
            //Reset the flags with delay
            if (viewHolder.mCheckView != null)
                viewHolder.mCheckView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSelectAll = mLastItemInActionMode = false;
                    }
                }, 200L);
            //Consume the Animation
            //flip(holder.mImageView, isSelected(position), 200L);
        } else {
            //Display the current flip status
            //setFlipped(holder.mImageView, isSelected(position));
        }

        //This "if-else" is just an example
        if (viewHolder.mCheckView != null) {
            if (getMode() == MODE_MULTI) {
                if (isSelected(position)) {
                    viewHolder.mCheckView.setChecked(true);
                } else {
                    viewHolder.mCheckView.setChecked(false);
                }
            } else {
                viewHolder.mCheckView.setChecked(false);
            }
        }
    }

    static int mReadColor = Color.GRAY;
    static int mUnreadColor = Color.WHITE;
    static int mLastReadColor = Color.RED;

    /**
     * Provide a reference to the views for each data item.
     * Complex data labels may need more than one view per item, and
     * you provide access to all the views for a data item in a view holder.
     */
    static final class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {

        OfflineHistoryRecyclerAdapter mAdapter;

        private TextView mAnimeView = null;
        private TextView mChapterView = null;
        private ImageView mCoverImageView = null;
        private TextView mLastReadView = null;
        private CheckBox mCheckView;

        public OfflineHistoryRecord mOfflineHistoryRecord;

        ViewHolder(View itemView, final OfflineHistoryRecyclerAdapter adapter) {
            super(itemView);

            this.mAdapter = adapter;
            this.itemView.setOnClickListener(this);
            this.itemView.setOnLongClickListener(this);
            mAnimeView = (TextView) itemView.findViewById(R.id.hrTitleText);
            mChapterView = (TextView) itemView.findViewById(R.id.hrEpisodeTitleText);
            mCoverImageView = (ImageView) itemView.findViewById(R.id.hrCoverImage);
            mLastReadView = (TextView) itemView.findViewById(R.id.hrHistoryText);
            mCheckView = (CheckBox) itemView.findViewById(R.id.hrCheckBox);
        }

        public void bindLibraryRecord(OfflineHistoryRecord pHistory) {
            if (pHistory == null)
                return;
            mOfflineHistoryRecord = pHistory;
            if (mCheckView != null)
                if (mAdapter.getMode() == MODE_MULTI)
                    mCheckView.setVisibility(View.VISIBLE);
                else {
                    mCheckView.setChecked(false);
                    mCheckView.setVisibility(View.GONE);
                }
            File lCoverFile = FileUtils.getDisplayImage(pHistory.getPath());
            if (lCoverFile != null) {
                String lCoverURL = "file:///" + lCoverFile.getAbsolutePath();
                ImageLoaderManager.getInstance().loadImage(lCoverURL, mCoverImageView);
            }
            if (pHistory.getFile().getParentFile() != null) {
                if (!TextUtils.isEmpty(pHistory.getFile().getParentFile().getName())) {
                    mAnimeView.setText(pHistory.getFile().getParentFile().getName());
                } else {
                    WriteLog.appendLog(TAG + ": Anime not found, hiding respective view");
                    mAnimeView.setVisibility(View.GONE);
                }
            }
            if (!TextUtils.isEmpty(pHistory.getName())) {
                if (!TextUtils.isEmpty(pHistory.getName())) {
                    mChapterView.setText(pHistory.getName());
                    mChapterView.setVisibility(View.VISIBLE);
                }
            } else {
                WriteLog.appendLog(TAG + ": Chapter not found, hiding respective view");
                mChapterView.setVisibility(View.GONE);
            }
            mLastReadView.setText(DateUtils.getRelativeTimeSpanString(pHistory.getTimeStamp()));
        }

        /**
         * Perform animation and selection on the current ItemView.
         * <br/><br/>
         * <b>IMPORTANT NOTE!</b> <i>setActivated</i> changes the selection color of the item
         * background if you added<i>android:background="?attr/selectableItemBackground"</i>
         * on the row layout AND in the style.xml.
         * <br/><br/>
         * This must be called after the listener consumed the event in order to add the
         * item number in the selection list.<br/>
         * Adapter must have a reference to its instance to check selection state.
         * <br/><br/>
         * If you do this, it's not necessary to invalidate the row (with notifyItemChanged): In this way
         * <i>onBindViewHolder</i> is NOT called on selection and custom animations on objects are NOT interrupted,
         * so you can SEE the animation in the Item and have the selection smooth with ripple.
         */
        private void toggleActivation() {
            itemView.setActivated(mAdapter.isSelected(getAdapterPosition()));
            if (itemView.isActivated()) {
                if (mCheckView != null)
                    mCheckView.setChecked(true);
            } else {
                if (mCheckView != null)
                    mCheckView.setChecked(false);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onClick(View view) {
            if (mAdapter.mClickListener.onListItemClick(getAdapterPosition()))
                toggleActivation();
            else {
                final OfflineHistoryRecord lRecord = mAdapter.getItem(getAdapterPosition());
                //Anime manga = AnimeUtils.getAnimeFromJson(lRecord.getFile());
                File mangaFolder = FileUtils.findParentWithName(lRecord.getFile(), "My Anime Reader");
                //AnimeUtils.viewAnime(view.getContext(), mCoverImageView, mangaFolder.getAbsolutePath());
                //"/storage/emulated/0/My Anime Reader/Hataraku Maou-sama!/Chapter 29"
                /**
                 new MaterialDialog.Builder(view.getContext())
                 .content("What would you like to do?")
                 .positiveText("View Chapter")
                 .negativeText("View Anime")
                 .callback(new MaterialDialog.ButtonCallback() {
                @Override public void onPositive(MaterialDialog dialog) {
                //ChapterUtils.viewChapter(view.getContext(), lRecord.getAnime(), lRecord.getChapter());
                }

                @Override public void onNegative(MaterialDialog dialog) {

                }
                })
                 .show();
                 **/
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean onLongClick(View view) {
            mAdapter.mClickListener.onListItemLongClick(getAdapterPosition());
            toggleActivation();
            return true;
        }
    }

    private class RecordFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (mItems == Collections.EMPTY_LIST)
                return results;
            // We implement here the filter logic
            if (constraint == null || constraint.length() == 0) {
                // No filter implemented we return all the list
                results.values = mItems;
                results.count = mItems.size();
            } else {
                // We perform filtering operation
                List<OfflineHistoryRecord> filteredList = new ArrayList<OfflineHistoryRecord>();

                for (OfflineHistoryRecord p : mItems) {
                    filteredList.add(p);
                }

                results.values = filteredList;
                results.count = filteredList.size();

            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {

            // Now we have to inform the adapter about the new list filtered
            if (results.count == 0)
                notifyDataSetChanged();
            else {
                mResults = (List<OfflineHistoryRecord>) results.values;
                notifyDataSetChanged();
            }

        }
    }

    private class RecordCustomFilter extends Filter {
        boolean mSourceCheck = true;

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            // NOTE: this function is *always* called from a background thread, and
            // not the UI thread.
            constraint = constraint.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if (mItems == Collections.EMPTY_LIST)
                return result;
            if (constraint != null && constraint.toString().length() > 0) {
                mSourceCheck = !constraint.toString().contains("all");
                ArrayList<OfflineHistoryRecord> filteredItems = new ArrayList<>();
                ArrayList<OfflineHistoryRecord> tempItems = new ArrayList<>();
                synchronized (this) {
                    tempItems.addAll(mItems);
                }
                for (int i = 0, l = tempItems.size(); i < l; i++) {
                    OfflineHistoryRecord m = tempItems.get(i);
                    filteredItems.add(m);
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            } else {
                synchronized (this) {
                    result.values = mItems;
                    result.count = mItems.size();
                }
            }
            return result;
        }

        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {

            // Now we have to inform the adapter about the new list filtered
            if (results.count == 0) {
                mResults.clear();
                notifyDataSetChanged();
            } else {
                mResults = (List<OfflineHistoryRecord>) results.values;
                notifyDataSetChanged();
            }
        }
    }
}