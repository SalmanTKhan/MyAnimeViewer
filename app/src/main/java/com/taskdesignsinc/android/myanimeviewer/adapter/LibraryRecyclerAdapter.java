package com.taskdesignsinc.android.myanimeviewer.adapter;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.readystatesoftware.viewbadger.BadgeView;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.FlexibleAdapter;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.OnItemClickListener;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.picasso.PaletteTransformation;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.FileUtils;
import com.taskdesignsinc.android.myanimeviewer.util.ImageLoaderManager;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.myanimeviewer.view.SquareImageView;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


public class LibraryRecyclerAdapter extends FlexibleAdapter<LibraryRecyclerAdapter.ViewHolder, File> implements Filterable {

    private static final String TAG = LibraryRecyclerAdapter.class.getSimpleName();

    public void sort(Comparator<File> comparator) {
        Collections.sort(mItems, comparator);
        notifyDataSetChanged();
    }

    public void changeViewedStatus(String path, boolean isViewed) {
        if (mViewedMap != null)
            mViewedMap.put(path, isViewed);
        //mLastEpisodeViewed = MAVApplication.getInstance().getRepository().getLastViewedByParent(path);
    }

    public void setAnime(Anime anime, HashMap<String, Episode> episodes) {
        mAnime = anime;
        mEpisodes = episodes;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null)
            mFilter = new FileFilter();

        return mFilter;
    }

    @Override
    public void removeItem(int position) {
        mResults.remove(position);
        notifyItemRemoved(position);
    }

    private Context mContext;
    List<File> mResults;
    private LayoutInflater mInflater;
    private OnItemClickListener mClickListener;
    //Selection fields
    private boolean
            mLastItemInActionMode = false,
            mSelectAll = false;

    SharedPreferences mPrefs;

    private FileFilter mFilter;
    private Comparator<? super File> mComparator;
    private String mLastEpisodeViewed;
    private HashMap<String, Boolean> mViewedMap;
    private int mBackStackLevel = 0;

    Anime mAnime = null;
    HashMap<String, Episode> mEpisodes = null;
    private SparseIntArray mRibbonIDs = null;
    private HashMap<String, Anime> mAnimeMap = null;

    private boolean mShowBadge = false;
    int mDisplayType = 0;
    private int mSecondaryType = 0;

    public static final int NONE = -1;
    public static final int GENRE = 0;
    public static final int AUTHOR = 1;
    public static final int LATEST_EPISODE = 2;
    public static final int SOURCE = 3;
    public static final int SOURCE_AND_LANG = 4;

    public LibraryRecyclerAdapter(Context context, int displayType, OnItemClickListener listener) {
        this.mContext = context;
        this.mDisplayType = displayType;
        this.mClickListener = listener;
        this.mItems = new ArrayList<File>();
        this.mResults = new ArrayList<File>();
        this.mViewedMap = new HashMap<String, Boolean>();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mViewedColor = mPrefs.getInt(Constants.KEY_EPISODE_VIEWED_COLOR, Color.GRAY);
        mUnreadColor = mPrefs.getInt(Constants.KEY_EPISODE_UNVIEWED_COLOR, ThemeManager.getInstance(context).getTextColor());
        if (!ThemeManager.getInstance().isValidTextColor(mUnreadColor))
            mUnreadColor = ThemeManager.getInstance(context).getTextColor();
        mLastViewedColor = mPrefs.getInt(Constants.KEY_EPISODE_LAST_VIEWED_COLOR, Color.RED);
    }

    public LibraryRecyclerAdapter(List<File> mangaList, Context context, OnItemClickListener listener) {
        this.mContext = context;
        this.mClickListener = listener;
        mResults = new ArrayList<File>();
        mViewedMap = new HashMap<String, Boolean>();
        this.mItems = mangaList;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mViewedColor = mPrefs.getInt(Constants.KEY_EPISODE_VIEWED_COLOR, Color.GRAY);
        mUnreadColor = mPrefs.getInt(Constants.KEY_EPISODE_UNVIEWED_COLOR, ThemeManager.getInstance(context).getTextColor());
        if (!ThemeManager.getInstance().isValidTextColor(mUnreadColor))
            mUnreadColor = ThemeManager.getInstance(context).getTextColor();
        mLastViewedColor = mPrefs.getInt(Constants.KEY_EPISODE_LAST_VIEWED_COLOR, Color.RED);
    }

    public void updateDataSet(String param) {
        //Fill mItems with your custom list
        //this.mItems = data;
    }

    public void clearData() {
        int size = mResults.size();
        if (size > 0) {
            mItems.clear();
            mResults.clear();
            this.notifyDataSetChanged();
        }
    }

    public void addFileList(List<File> data, int backStackLevel) {
        mBackStackLevel = backStackLevel;
        mItems.clear();
        mResults.clear();
        mViewedMap.clear();
        if (data != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mItems.addAll(data);
            } else {
                for (File obj : data) {
                    mItems.add(obj);
                }
            }
            if (mResults == null || mResults.isEmpty())
                mResults = (List<File>) ((ArrayList<File>) mItems).clone();
            checkViewedStatus();
        } else {
            mItems.clear();
            mResults.clear();
        }
        notifyDataSetChanged();
    }

    public void checkViewedStatus() {
        if (mResults == null || mResults.isEmpty())
            return;
        WriteLog.appendLog("Checking episode status");
        mLastEpisodeViewed = MAVApplication.getInstance().getRepository().getLastViewedByPath(mResults.get(0).getParent());
        for (File lEpisode : mResults) {
            mViewedMap.put(lEpisode.getAbsolutePath(),
                    MAVApplication.getInstance().getRepository().isPathViewed(lEpisode.getAbsolutePath()));
        }
    }

    @Override
    public int getItemCount() {
        return mResults.size();
    }

    public File getItem(int position) {
        if (position >= mResults.size() || position < 0)
            return null;
        return mResults.get(position);
    }

    public int getItemPosition(String path) {
        for (int i = 0; i < mResults.size(); i++) {
            if (mResults.get(i).getAbsolutePath().equals(path))
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
        View v = null;
        ViewHolder viewHolder = null;
        switch (mDisplayType) {
            case 1:
            case 2:
                v = mInflater.inflate(R.layout.anime_list_card, parent, false);
                viewHolder = new ViewHolder(v, this);
                if (mShowBadge) {
                    viewHolder.mTopLeftBadgeView = new BadgeView(mContext, (View) v.findViewById(R.id.alc_badge_view_left));
                    viewHolder.mTopLeftBadgeView.setBadgePosition(BadgeView.POSITION_TOP_LEFT);
                    viewHolder.mTopRightBadgeView = new BadgeView(mContext, (View) v.findViewById(R.id.alc_badge_view_right));
                    viewHolder.mTopRightBadgeView.setBadgePosition(BadgeView.POSITION_TOP_RIGHT);
                }
                break;
            case 3:
                v = mInflater.inflate(R.layout.anime_list_card2, parent, false);
                viewHolder = new ViewHolder(v, this);
                if (mShowBadge) {
                    viewHolder.mTopLeftBadgeView = new BadgeView(mContext, (View) v.findViewById(R.id.alc_badge_view_left));
                    viewHolder.mTopLeftBadgeView.setBadgePosition(BadgeView.POSITION_TOP_LEFT);
                    viewHolder.mTopRightBadgeView = new BadgeView(mContext, (View) v.findViewById(R.id.alc_badge_view_right));
                    viewHolder.mTopRightBadgeView.setBadgePosition(BadgeView.POSITION_TOP_RIGHT);
                }
                break;
            default:
                v = mInflater.inflate(R.layout.library_grid_item, parent, false);
                viewHolder = new ViewHolder(v, this);
                if (mShowBadge) {
                    viewHolder.mTopRightBadgeView = new BadgeView(mContext, (View) v.findViewById(R.id.lgi_badge_view_right));
                    viewHolder.mTopRightBadgeView.setBadgePosition(BadgeView.POSITION_TOP_LEFT);
                }
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        Log.d(TAG, "onBindViewHolder for position " + position);
        final File lFile = mResults.get(position);
        Anime lAnime = null;

        if (viewHolder != null) {
            //When user scrolls this bind the correct selection status
            //if (!ThemeManager.getInstance().isLightBackground() && viewHolder.mTitleView != null)
            //viewHolder.mTitleView.setTextColor(ThemeManager.getInstance().getInvertedTextColor());
            viewHolder.bindFile(lFile, mViewedMap.get(lFile.getAbsolutePath()), ((mDisplayType != 3) && mBackStackLevel == 0));
            if (mBackStackLevel > 0) {
                if (mResults.get(position).getAbsolutePath().equals(mLastEpisodeViewed))
                    viewHolder.mTitleView.setTextColor(mLastViewedColor);
            }
            viewHolder.itemView.setActivated(isSelected(position));

            if (mAnimeMap != null)
                lAnime = mAnimeMap.get(mResults.get(position).getAbsolutePath());
            else
                lAnime = null;
            if (mAnime != null)
                lAnime = mAnime;

            if (lAnime != null) {
                if (mDisplayType > 0) {
                    if (mDisplayType != 3) {
                        if (viewHolder.mSubTextView != null) {
                            if (!ThemeManager.getInstance().isLightBackground())
                                viewHolder.mSubTextView.setTextColor(ThemeManager.getInstance().getInvertedTextColor());
                            String lSubText = "";
                            switch (mSecondaryType) {
                                default:
                                case NONE:
                                    break;
                                case GENRE:
                                    lSubText = lAnime.getGenres();
                                    break;
                                case AUTHOR:
                                    lSubText = lAnime.getCreator();
                                    break;
                                case LATEST_EPISODE:
                                    lSubText = lAnime.getLatestEpisode();
                                    break;
                                case SOURCE:
                                    lSubText = Parser.getNameByUrl(lAnime.getUrl());
                                    break;
                                case SOURCE_AND_LANG:
                                    String lLanguage = mContext.getResources().getString(Parser.getExistingInstance(Parser.getTypeByURL(lAnime.getUrl())).getLanguageResId());
                                    lSubText = Parser.getNameByUrl(lAnime.getUrl()) + " - "
                                            + lLanguage;
                                    break;
                            }
                            if (!TextUtils.isEmpty(lSubText)) {
                                viewHolder.mSubTextView.setText(lSubText);
                                viewHolder.mSubTextView.setVisibility(View.VISIBLE);
                            } else
                                viewHolder.mSubTextView.setVisibility(View.INVISIBLE);
                        }
                        if (viewHolder.mSourceTextView != null) {
                            if (!ThemeManager.getInstance().isLightBackground())
                                viewHolder.mSourceTextView.setTextColor(ThemeManager.getInstance().getInvertedTextColor());
                            viewHolder.mSourceTextView.setText(Parser.getNameByUrl(lAnime.getUrl()));
                        }
                    } else {
                        if (viewHolder.mAuthorView != null) {
                            if (!ThemeManager.getInstance().isLightBackground())
                                viewHolder.mAuthorView.setTextColor(ThemeManager.getInstance().getInvertedTextColor());
                            if (!TextUtils.isEmpty(lAnime.getCreator())) {
                                viewHolder.mAuthorView.setTypeface(null, Typeface.ITALIC);
                                viewHolder.mAuthorView.setText(lAnime.getCreator());
                            } else {
                                viewHolder.mAuthorView.setText("");
                            }
                        }
                        if (viewHolder.mGenreView != null) {
                            if (!ThemeManager.getInstance().isLightBackground())
                                viewHolder.mGenreView.setTextColor(ThemeManager.getInstance().getInvertedTextColor());
                            if (!TextUtils.isEmpty(lAnime.getGenres())) {
                                viewHolder.mGenreView.setText(lAnime.getGenres());
                            } else {
                                viewHolder.mGenreView.setText("");
                            }
                        }
                        if (viewHolder.mLatestView != null) {
                            if (!ThemeManager.getInstance().isLightBackground())
                                viewHolder.mLatestView.setTextColor(ThemeManager.getInstance().getInvertedTextColor());
                            if (!TextUtils.isEmpty(lAnime.getLatestEpisode())) {
                                viewHolder.mLatestView.setText(lAnime.getLatestEpisode());
                                viewHolder.mLatestView.setSelected(true);
                            } else {
                                viewHolder.mLatestView.setText("");
                            }
                        }
                        if (viewHolder.mSourceView != null) {
                            if (!ThemeManager.getInstance().isLightBackground())
                                viewHolder.mSourceView.setTextColor(ThemeManager.getInstance().getInvertedTextColor());
                            viewHolder.mSourceView.setText(Parser.getNameByUrl(lAnime.getUrl()));
                        }
                    }
                }
            }

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

            if (viewHolder.mCheckView != null) {
                //This "if-else" is just an example
                if (isSelected(position)) {
                    viewHolder.mCheckView.setChecked(true);
                } else {
                    viewHolder.mCheckView.setChecked(false);
                }
            }
        }
    }

    static int mViewedColor = Color.GRAY;
    static int mUnreadColor = Color.WHITE;
    static int mLastViewedColor = Color.RED;

    /**
     * Provide a reference to the views for each data item.
     * Complex data labels may need more than one view per item, and
     * you provide access to all the views for a data item in a view holder.
     */
    static final class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            View.OnLongClickListener {

        LibraryRecyclerAdapter mAdapter;

        public TextView mTitleView;
        public SquareImageView mImageView;
        public TextView mSubTextView = null;
        public TextView mSourceTextView = null;

        public TextView mAuthorView = null;
        public TextView mGenreView = null;
        public TextView mLatestView = null;
        public TextView mSourceView = null;
        public BadgeView mTopLeftBadgeView = null;
        public BadgeView mTopRightBadgeView = null;
        public CheckBox mCheckView = null;

        public File mFile;

        ViewHolder(View itemView, final LibraryRecyclerAdapter adapter) {
            super(itemView);

            this.mAdapter = adapter;
            switch (mAdapter.mDisplayType) {
                case 1:
                case 2:
                case 3:
                    mTitleView = (TextView) itemView.findViewById(R.id.alc_text);
                    mSubTextView = (TextView) itemView.findViewById(R.id.alc_text2);
                    mSourceTextView = (TextView) itemView.findViewById(R.id.alc_text3);
                    mImageView = (SquareImageView) itemView.findViewById(R.id.alc_image);
                    mCheckView = (CheckBox) itemView.findViewById(R.id.alc_checkbox);

                    mAuthorView = mSubTextView;
                    mGenreView = mSourceTextView;
                    mLatestView = (TextView) itemView.findViewById(R.id.alc_text4);
                    mSourceView = (TextView) itemView.findViewById(R.id.alc_text5);
                    break;
                default:
                    mCheckView = (CheckBox) itemView.findViewById(R.id.lgi_checkBox);
                    mTitleView = (TextView) itemView.findViewById(R.id.lgi_text);
                    mImageView = (SquareImageView) itemView.findViewById(R.id.lgi_image);
                    break;
            }

            this.itemView.setOnClickListener(this);
            this.itemView.setOnLongClickListener(this);
        }

        public void bindFile(File manga, boolean pIsViewed, boolean pPaletteEnabled) {
            mFile = manga;
            if (mCheckView != null && mAdapter != null) {
                if (mAdapter.getMode() == MODE_MULTI)
                    mCheckView.setVisibility(View.VISIBLE);
                else
                    mCheckView.setVisibility(View.GONE);
            }
            if (mTitleView != null) {
                if (mAdapter.mBackStackLevel > 0) {
                    if (pIsViewed && !pPaletteEnabled)
                        mTitleView.setTextColor(mViewedColor);
                    else
                        mTitleView.setTextColor(mUnreadColor);
                }
                mTitleView.setText(manga.getName());
                File lCoverFile = mAdapter.mDisplayType == 1 ? null : getDisplayImage(manga.getAbsolutePath());
                if (lCoverFile != null) {
                    String ext = FileUtils.getFileExtension(lCoverFile.getName());
                    if (FileUtils.isImage(ext)) {
                        String lCoverURL = "file:///" + lCoverFile.getAbsolutePath();
                        if (pPaletteEnabled) {
                            ImageLoaderManager.getInstance().loadImage(lCoverURL, mImageView, new PaletteTransformation.PaletteCallback(mImageView) {

                                @Override
                                public void onError() {
                                    WriteLog.appendLog("LibraryRecyclerAdapter", "Error while generating palette");
                                }

                                @Override
                                protected void onSuccess(Palette palette) {
                                    int colorTemp = ThemeManager.getInstance().getBackgroundColor(mImageView.getContext());
                                    if (mTitleView != null && palette != null)
                                        if (palette.getVibrantSwatch() != null)
                                            colorTemp = palette.getVibrantSwatch().getRgb();
                                        else if (palette.getDarkMutedSwatch() != null)
                                            colorTemp = palette.getDarkMutedSwatch().getRgb();
                                    Integer colorFrom = ThemeManager.getInstance().getBackgroundColor(mTitleView.getContext());
                                    Integer colorTo = colorTemp;
                                    ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                                    colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animator) {
                                            mTitleView.setBackgroundColor((Integer) animator.getAnimatedValue());
                                        }

                                    });
                                    colorAnimation.start();

                                }
                            }, null);
                        } else {
                            if (mAdapter.mDisplayType < 1) {
                                /**
                                if (ThemeManager.getInstance().getCurrentTheme() == ThemeManager.ThemeType.Light)
                                    mTitleView.setBackgroundResource(R.color.light_shadow_box_color);
                                else
                                    mTitleView.setBackgroundResource(R.color.shadow_box_color);
                                 **/
                            }
                            ImageLoaderManager.getInstance().loadImage(lCoverURL, mImageView);
                        }
                    }
                } else {
                    if (mAdapter.mDisplayType == 1)
                        mImageView.setVisibility(View.INVISIBLE);
                    else {
                        mImageView.setVisibility(View.VISIBLE);
                        mImageView.setImageResource(R.mipmap.ic_launcher);
                    }
                }
            }
        }

        private File getDisplayImage(String pPath) {
            File dir = new File(pPath);
            if (!dir.isDirectory())
                return dir;
            File[] file = dir.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String fileName) {
                    String ext = FileUtils.getFileExtension(fileName);
                    return FileUtils.isImage(ext);
                }
            });
            if (file != null) {
                for (File f : file) {
                    if (f.getName().contains("cover")) {
                        return f;
                    }
                }
                if (file.length > 0)
                    return file[0];
            }
            return null;
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
                mCheckView.setChecked(true);
            } else {
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

    private class FileFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            // We implement here the filter logic
            if (constraint == null || constraint.length() == 0) {
                // No filter implemented we return all the list
                results.values = mItems;
                results.count = mItems.size();
            } else {
                // We perform filtering operation
                List<File> nAnimeList = new ArrayList<File>();

                for (File p : mItems) {
                    if (p.getName().toUpperCase().startsWith(constraint.toString().toUpperCase()))
                        nAnimeList.add(p);
                }

                results.values = nAnimeList;
                results.count = nAnimeList.size();

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
                mResults = (List<File>) results.values;
                notifyDataSetChanged();
            }

        }
    }

}