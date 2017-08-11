package com.taskdesignsinc.android.myanimeviewer.adapter;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.bumptech.glide.Glide;
import com.readystatesoftware.viewbadger.BadgeView;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;
import com.squareup.picasso.Picasso;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.FlexibleAdapter;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.OnItemClickListener;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteRecord;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteTag;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.picasso.PaletteTransformation;
import com.taskdesignsinc.android.myanimeviewer.model.helper.AnimeUtils;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.DisplayUtils;
import com.taskdesignsinc.android.myanimeviewer.util.ImageLoaderManager;
import com.taskdesignsinc.android.myanimeviewer.view.GridRecyclerView;
import com.taskdesignsinc.android.myanimeviewer.view.SquareImageView;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


public class AnimeRecyclerAdapter extends FlexibleAdapter<AnimeRecyclerAdapter.ViewHolder, Anime> implements Filterable, FastScrollRecyclerView.SectionedAdapter {

    private static final String TAG = AnimeRecyclerAdapter.class.getSimpleName();
    private final int mLoaderType = 0;
    private GridRecyclerView mRecyclerView;
    private int mSectionType = 0;
    private HashMap<Long, FavoriteTag> mTagHashMap;

    public void sort(Comparator<Anime> comparator) {
        Collections.sort(mResults, comparator);
        notifyDataSetChanged();
    }

    public void setFilterType(int filterType) {
        mFilterType = filterType;
    }

    public Filter getFilter(boolean priorityCheck, boolean sourceCheck) {
        return mFilter = new AnimeCustomFilter(priorityCheck, sourceCheck);
    }

    @Override
    public Filter getFilter() {
        switch (mFilterType) {
            case 0:
                mFilter = new AnimeFilter();
                break;
            case 1:
                mFilter = new AnimeCustomFilter();
                break;
        }
        return mFilter;
    }

    public void setCatalogPositions(List<Anime> animeWithPositions) {
        if (animeWithPositions != null) {
            HashMap<String, Integer> positionMap = new HashMap<>();
            for (Anime anime : animeWithPositions) {
                //positionMap.put(anime.getUrl(), anime.getCatalogPosition());
            }
            for (Anime anime : mItems) {
                if (positionMap.containsKey(anime.getUrl())) {
                    //anime.setCatalogPosition(positionMap.get(anime.getUrl()));
                }
            }
        }
    }

    public int getCatalogType() {
        return mCatalogType;
    }

    public void setCatalogType(int mCatalogType) {
        this.mCatalogType = mCatalogType;
    }

    public List<Anime> getList() {
        return mResults;
    }

    public void setDisplayType(int displayType) {
        this.mDisplayType = displayType;
        //notifyDataSetChanged();
    }

    int mOriginalTextSize = -1;


    public void setSectionType(int type) {
        if (mOriginalTextSize == -1)
            mOriginalTextSize = mRecyclerView.getPopupTextSize();
        mSectionType = type;
        if (mSectionType == 2) {
            mRecyclerView.setPopupTextSize(DisplayUtils.dpToPx(24));
            mTagHashMap = new HashMap<>();
            for (FavoriteTag tag : MAVApplication.getInstance().getRepository().getFavoriteTags())
                mTagHashMap.put(tag.getId(), tag);
        } else {
            mRecyclerView.setPopupTextSize(mOriginalTextSize);
        }
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        switch (mSectionType) {
            case 1:
                return String.valueOf(getItem(position).getTitle().charAt(0));
            case 2:
                if (mTagHashMap.isEmpty() || mTagHashMap.get(getItem(position).getTagId()) == null) {
                    return String.valueOf((position + 1));
                }
                return String.valueOf(mTagHashMap.get(getItem(position).getTagId()).getTitle());
            default:
                return String.valueOf((position + 1));
        }
    }

    private Context mContext;
    List<Anime> mResults;
    private LayoutInflater mInflater;
    private OnItemClickListener mClickListener;
    //Selection fields
    private boolean
            mLastItemInActionMode = false,
            mSelectAll = false;

    private boolean mShowBadge = false;
    private HashMap<Long, Integer> mRibbonIDs = null;
    SharedPreferences mPrefs;

    private Filter mFilter;
    private int mFilterType = 0;
    private int mDisplayType = 0;
    private int mSecondaryType = -1;
    private int mCatalogType = -1;

    public static final int NONE = -1;
    public static final int GENRE = 0;
    public static final int CREATOR = 1;
    public static final int LATEST_EPISODE = 2;
    public static final int SOURCE = 3;
    public static final int SOURCE_AND_LANG = 4;

    public void setRecyclerView(GridRecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    public AnimeRecyclerAdapter(GridRecyclerView recyclerView, List<Anime> animeList, int displayType, Context context, boolean showBadge, OnItemClickListener listener) {
        this.mContext = context;
        this.mClickListener = listener;
        this.mShowBadge = showBadge;
        this.mDisplayType = displayType;
        this.mRecyclerView = recyclerView;
        mResults = new ArrayList<Anime>();
        checkAnime(animeList);
        //Collections.sort(animeList, Anime.Order.ByCatalogIndex);
        this.mItems = animeList;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mReadColor = mPrefs.getInt(Constants.KEY_EPISODE_VIEWED_COLOR, Color.GRAY);
        mUnreadColor = mPrefs.getInt(Constants.KEY_EPISODE_UNVIEWED_COLOR, ThemeManager.getInstance(context).getTextColor());
        if (!ThemeManager.getInstance().isValidTextColor(mUnreadColor))
            mUnreadColor = ThemeManager.getInstance(context).getTextColor();
        mLastReadColor = mPrefs.getInt(Constants.KEY_EPISODE_LAST_VIEWED_COLOR, Color.RED);
        mRibbonIDs = new HashMap<>();
        for (FavoriteTag tag : MAVApplication.getInstance().getRepository().getFavoriteTags()) {
            mRibbonIDs.put(tag.getId(), tag.getRibbonID());
        }
    }

    public AnimeRecyclerAdapter(GridRecyclerView recyclerView, int catalogType, List<Anime> animeList, int displayType, Context context, boolean showBadge, OnItemClickListener listener) {
        this.mContext = context;
        this.mClickListener = listener;
        this.mShowBadge = showBadge;
        this.mDisplayType = displayType;
        this.mCatalogType = catalogType;
        this.mRecyclerView = recyclerView;
        if (animeList == null)
            animeList = new ArrayList<>();
        checkAnime(animeList);
        this.mItems = animeList;
        mResults = (List<Anime>) ((ArrayList<Anime>) mItems).clone();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mReadColor = mPrefs.getInt(Constants.KEY_EPISODE_VIEWED_COLOR, Color.GRAY);
        mUnreadColor = mPrefs.getInt(Constants.KEY_EPISODE_UNVIEWED_COLOR, ThemeManager.getInstance(context).getTextColor());
        if (!ThemeManager.getInstance().isValidTextColor(mUnreadColor))
            mUnreadColor = ThemeManager.getInstance(context).getTextColor();
        mLastReadColor = mPrefs.getInt(Constants.KEY_EPISODE_LAST_VIEWED_COLOR, Color.RED);
        mRibbonIDs = new HashMap<>();
        for (FavoriteTag tag : MAVApplication.getInstance().getRepository().getFavoriteTags()) {
            mRibbonIDs.put(tag.getId(), tag.getRibbonID());
        }
    }

    public AnimeRecyclerAdapter(List<Anime> animeList, int displayType, Context context, boolean showBadge, OnItemClickListener listener) {
        this.mContext = context;
        this.mClickListener = listener;
        this.mShowBadge = showBadge;
        this.mDisplayType = displayType;
        mResults = new ArrayList<Anime>();
        checkAnime(animeList);
        this.mItems = animeList;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mReadColor = mPrefs.getInt(Constants.KEY_EPISODE_VIEWED_COLOR, Color.GRAY);
        mUnreadColor = mPrefs.getInt(Constants.KEY_EPISODE_UNVIEWED_COLOR, ThemeManager.getInstance(context).getTextColor());
        if (!ThemeManager.getInstance().isValidTextColor(mUnreadColor))
            mUnreadColor = ThemeManager.getInstance(context).getTextColor();
        mLastReadColor = mPrefs.getInt(Constants.KEY_EPISODE_LAST_VIEWED_COLOR, Color.RED);
        mRibbonIDs = new HashMap<>();
        for (FavoriteTag tag : MAVApplication.getInstance().getRepository().getFavoriteTags()) {
            mRibbonIDs.put(tag.getId(), tag.getRibbonID());
        }
    }

    public AnimeRecyclerAdapter(List<Anime> animeList, int displayType, int secondaryType, Context context, boolean showBadge, OnItemClickListener listener) {
        this.mContext = context;
        this.mClickListener = listener;
        this.mShowBadge = showBadge;
        this.mDisplayType = displayType;
        this.mSecondaryType = secondaryType;
        mResults = new ArrayList<Anime>();
        checkAnime(animeList);
        this.mItems = animeList;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mReadColor = mPrefs.getInt(Constants.KEY_EPISODE_VIEWED_COLOR, Color.GRAY);
        mUnreadColor = mPrefs.getInt(Constants.KEY_EPISODE_UNVIEWED_COLOR, ThemeManager.getInstance(context).getTextColor());
        if (!ThemeManager.getInstance().isValidTextColor(mUnreadColor))
            mUnreadColor = ThemeManager.getInstance(context).getTextColor();
        mLastReadColor = mPrefs.getInt(Constants.KEY_EPISODE_LAST_VIEWED_COLOR, Color.RED);
        mRibbonIDs = new HashMap<>();
        for (FavoriteTag tag : MAVApplication.getInstance().getRepository().getFavoriteTags()) {
            mRibbonIDs.put(tag.getId(), tag.getRibbonID());
        }
    }

    public static final int totalSpan = 100;
    private int currentSpan;
    private int spanConstant = 20;

    public GridLayoutManager.SpanSizeLookup getScalableSpanSizeLookUp() {
        return scalableSpanSizeLookUp;
    }

    public int calculateRange() {
        int start = ((GridLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        int end = ((GridLayoutManager) mRecyclerView.getLayoutManager()).findLastVisibleItemPosition();
        if (start < 0)
            start = 0;
        if (end < 0)
            end = 0;
        return end - start;
    }


    private GridLayoutManager.SpanSizeLookup scalableSpanSizeLookUp = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            return getCurrentSpan();
        }
    };


    public int getCurrentSpan() {
        return currentSpan;
    }

    public void setCurrentSpan(int span) {
        this.currentSpan = span;

    }

    public void delayedNotify(final int pos, final int range) {
        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                notifyItemRangeChanged(pos - range > 0 ? pos - range : 0, range * 2 < getItemCount() ? range * 2 : range);
            }
        }, 100);
    }

    public void updateDataSet(String param) {
        //Fill mItems with your custom list
        //this.mItems = data;
    }

    public void clearData() {
        if (mResults != null) {
            int size = mResults.size();
            if (size > 0) {
                if (mItems != null)
                    mItems.clear();
                if (mResults != null)
                    mResults.clear();
                if (mRibbonIDs != null)
                    mRibbonIDs.clear();
                this.notifyDataSetChanged();
            }
        } else {
            this.notifyDataSetChanged();
        }
    }

    public void addItem(Anime anime) {
        if (anime == null)
            return;
        checkAnime(anime);
        if (mItems != null) {
            this.mItems.add(anime);
        }
        if (mResults != null)
            mResults.add(anime);
        notifyItemInserted(mResults.size() - 1);
    }

    public void addItem(int position, Anime anime) {
        if (anime == null)
            return;
        checkAnime(anime);
        if (mItems != null) {
            this.mItems.add(position, anime);
        }
        if (mResults != null)
            mResults.add(position, anime);
        notifyItemInserted(position);
    }

    public void setAnimeList(ArrayList<Anime> animeList) {
        if (animeList == null || animeList == Collections.EMPTY_LIST)
            return;
        checkAnime(animeList);
        int startPos = 0;
        this.mItems = animeList;
        mResults = (List<Anime>) ((ArrayList<Anime>) mItems).clone();
        this.notifyItemRangeInserted(startPos, animeList.size() - 1);
    }

    public void addAnimeList(List<Anime> animeList) {
        if (animeList == null || animeList == Collections.EMPTY_LIST)
            return;
        checkAnime(animeList);
        int startPos = 0;
        if (mItems != null) {
            startPos = this.mItems.size();
            this.mItems.addAll(animeList);
        } else {
            this.mItems = animeList;
        }
        mResults = (List<Anime>) ((ArrayList<Anime>) mItems).clone();
        this.notifyItemRangeInserted(startPos, animeList.size() - 1);
    }

    public void removeItem(int position) {
        if (position < 0) return;
        if (position < mItems.size()) {
            mItems.remove(position);
            mResults = (List<Anime>) ((ArrayList<Anime>) mItems).clone();
            notifyItemRemoved(position);
        }
    }

    public void checkAnime(Anime anime) {
        if (anime != null) {
            if (mShowBadge) {
                if (mRibbonIDs == null)
                    mRibbonIDs = new HashMap<>();
                FavoriteTag lTag = null;
                FavoriteRecord lRecord = null;
                if (anime.getTagId() == -1) {
                    lRecord = MAVApplication.getInstance().getRepository().getFavoriteByAnimeUrl(anime.getUrl());
                    if (lRecord != null) {
                        anime.setTagId(lRecord.getTagId());
                    }
                }
                if (anime.getTagId() != -1 && mRibbonIDs.get(anime.getTagId()) < 0) {
                    lTag = MAVApplication.getInstance().getRepository().getFavoriteTag(anime.getTagId());
                    if (lTag != null)
                        mRibbonIDs.put(lTag.getId(), lTag.getRibbonID());
                }
            }
        }
    }

    public void checkAnime(List<Anime> animeList) {
        if (animeList != null) {
            if (mShowBadge) {
                if (mRibbonIDs == null)
                    mRibbonIDs = new HashMap<>();
                FavoriteTag lTag = null;
                FavoriteRecord lRecord = null;
                for (Anime anime : animeList) {
                    if (mCatalogType != -1) {
                        //anime.setCatalogPosition(AnimeHelper.getInstance(mContext).getAnimePositionByUrl(mCatalogType, anime.getUrl()));
                    }
                    if (anime.getTagId() == -1) {
                        lRecord = MAVApplication.getInstance().getRepository().getFavoriteByAnimeUrl(anime.getUrl());
                        if (lRecord != null) {
                            anime.setTagId(lRecord.getTagId());
                        }
                    }
                    if (anime.getTagId() != -1 && !mRibbonIDs.containsKey(anime.getTagId())) {
                        lTag = MAVApplication.getInstance().getRepository().getFavoriteTag(anime.getTagId());
                        if (lTag != null)
                            mRibbonIDs.put(lTag.getId(), lTag.getRibbonID());
                    }
                }
            }
        }
    }

    public int getOriginalItemCount() {
        return mItems.size();
    }

    @Override
    public int getItemCount() {
        return mResults.size();
    }

    public Anime getItem(int position) {
        if (position >= mResults.size() || position < 0)
            return null;
        return mResults.get(position);
    }

    public Anime getItem(String url) {
        for (Anime lAnime : mResults) {
            if (lAnime.getUrl().equals(url))
                return lAnime;
        }
        return null;
    }

    public int getItemPosition(String url) {
        for (int i = 0; i < mResults.size(); i++) {
            if (mResults.get(i).getUrl().equals(url))
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
        //Log.d(TAG, "onCreateViewHolder for viewType " + viewType);
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
                if (mLoaderType < 4)
                    v = mInflater.inflate(R.layout.anime_grid_item, parent, false);
                viewHolder = new ViewHolder(v, this);
                if (mShowBadge) {
                    viewHolder.mTopLeftBadgeView = new BadgeView(mContext, (View) v.findViewById(R.id.agi_badge_view_left));
                    viewHolder.mTopLeftBadgeView.setBadgePosition(BadgeView.POSITION_TOP_LEFT);
                    viewHolder.mTopRightBadgeView = new BadgeView(mContext, (View) v.findViewById(R.id.agi_badge_view_right));
                    viewHolder.mTopRightBadgeView.setBadgePosition(BadgeView.POSITION_TOP_RIGHT);
                }
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, final int position) {
        final Anime anime = mResults.get(position);

        //When user scrolls this bind the correct selection status
        viewHolder.bindAnime(position, anime);
        ViewCompat.setActivated(viewHolder.itemView, isSelected(position));
        //viewHolder.itemView.setActivated(isSelected(position));
        if (!TextUtils.isEmpty(anime.getCover()) && !(mDisplayType == 1)) {
            viewHolder.mImageView.setVisibility(View.VISIBLE);
            ImageLoaderManager.getInstance().loadImage(anime.getCover(), viewHolder.mImageView, new PaletteTransformation.PaletteCallback(viewHolder.mImageView) {

                @Override
                public void onError() {

                }

                @Override
                protected void onSuccess(Palette palette) {
                    if (mDisplayType < 1) {
                        viewHolder.mTitleView.setTextColor(ThemeManager.getInstance().getTextColor());
                        int colorTemp = ThemeManager.getInstance().getBackgroundColor(mContext);
                        if (viewHolder.mTitleView != null && palette != null)
                            if (palette.getVibrantSwatch() != null)
                                colorTemp = palette.getVibrantSwatch().getRgb();
                            else if (palette.getDarkMutedSwatch() != null)
                                colorTemp = palette.getDarkMutedSwatch().getRgb();
                        Integer colorFrom = ThemeManager.getInstance().getBackgroundColor(mContext);
                        Integer colorTo = colorTemp;
                        viewHolder.mPaletteColor = colorTo;
                        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                            @Override
                            public void onAnimationUpdate(ValueAnimator animator) {
                                if (viewHolder.mCardView == null)
                                    viewHolder.mTitleView.setBackgroundColor((Integer) animator.getAnimatedValue());
                            }

                        });
                        colorAnimation.start();
                    }
                }
            }, viewHolder);
        } else {
            viewHolder.mImageView.setVisibility(View.GONE);
        }

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
                            lSubText = anime.getGenres();
                            break;
                        case CREATOR:
                            lSubText = anime.getCreator();
                            break;
                        case LATEST_EPISODE:
                            lSubText = anime.getLatestEpisode();
                            break;
                        case SOURCE:
                            lSubText = Parser.getNameByUrl(anime.getUrl());
                            break;
                        case SOURCE_AND_LANG:
                            String lLanguage = mContext.getResources().getString(Parser.getExistingInstance(Parser.getTypeByURL(anime.getUrl())).getLanguageResId());
                            lSubText = Parser.getNameByUrl(anime.getUrl()) + " - "
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
                    viewHolder.mSourceTextView.setText(Parser.getNameByUrl(anime.getUrl()));
                }
            } else {
                if (viewHolder.mAuthorView != null) {
                    if (!TextUtils.isEmpty(anime.getCreator())) {
                        viewHolder.mAuthorView.setTypeface(null, Typeface.ITALIC);
                        viewHolder.mAuthorView.setText(anime.getCreator());
                    } else {
                        viewHolder.mAuthorView.setText("");
                    }
                }
                if (viewHolder.mGenreView != null) {
                    if (!TextUtils.isEmpty(anime.getGenres())) {
                        viewHolder.mGenreView.setText(anime.getGenres());
                    } else {
                        viewHolder.mGenreView.setText("");
                    }
                }
                if (viewHolder.mLatestView != null) {
                    //if (!ThemeManager.getInstance().isLightBackground())
                    //  viewHolder.mLatestView.setTextColor(ThemeManager.getInstance().getInvertedTextColor());
                    if (!TextUtils.isEmpty(anime.getLatestEpisode())) {
                        viewHolder.mLatestView.setText(anime.getLatestEpisode());
                        viewHolder.mLatestView.setSelected(true);
                    } else {
                        viewHolder.mLatestView.setText("");
                    }
                }
                if (viewHolder.mSourceView != null) {
                    //if (!ThemeManager.getInstance().isLightBackground())
                    //    viewHolder.mSourceView.setTextColor(ThemeManager.getInstance().getInvertedTextColor());
                    viewHolder.mSourceView.setText(Parser.getNameByUrl(anime.getUrl()));
                }
            }
        }

        if (mShowBadge) {
            if (anime.getTagId() != -1) {
                if (mRibbonIDs != null && mRibbonIDs.containsKey(anime.getTagId()))
                    viewHolder.mTopRightBadgeView.setBackgroundResource(Constants.RibbonDrawable[mRibbonIDs.get(anime.getTagId())]);
                else {
                    if (anime.getTagId() < Constants.RibbonDrawable.length)
                        viewHolder.mTopRightBadgeView.setBackgroundResource(Constants.RibbonDrawable[anime.getTagId()]);
                }
                viewHolder.mTopRightBadgeView.show();
            } else {
                viewHolder.mTopRightBadgeView.setBackgroundResource(0);
                viewHolder.mTopRightBadgeView.hide();
            }
            if (viewHolder.mTopLeftBadgeView != null) {
                if (anime.hasNewEpisodes()) {
                    viewHolder.mTopLeftBadgeView.setText("New");
                    viewHolder.mTopLeftBadgeView.show();
                } else {
                    viewHolder.mTopLeftBadgeView.setBackgroundResource(0);
                    viewHolder.mTopLeftBadgeView.hide();
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
        }

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

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        Glide.clear(holder.mImageView);
        Picasso.with(mContext).cancelRequest(holder.mImageView);
    }

    static int mReadColor = Color.GRAY;
    static int mUnreadColor = Color.WHITE;
    static int mLastReadColor = Color.RED;

    /**
     * Provide a reference to the views for each data item.
     * Complex data labels may need more than one view per item, and
     * you provide access to all the views for a data item in a view holder.
     */
    public static final class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnTouchListener,
            View.OnClickListener,
            View.OnLongClickListener {

        AnimeRecyclerAdapter mAdapter;

        public AppCompatTextView mTitleView;
        AppCompatImageView mImageView;
        AppCompatTextView mSubTextView = null;
        AppCompatTextView mSourceTextView = null;

        AppCompatTextView mAuthorView = null;
        AppCompatTextView mGenreView = null;
        AppCompatTextView mLatestView = null;
        AppCompatTextView mSourceView = null;
        BadgeView mTopLeftBadgeView = null;
        BadgeView mTopRightBadgeView = null;
        AppCompatCheckBox mCheckView = null;
        CardView mCardView = null;

        Anime mAnime;

        public int mPosition = -1, mLastTouchX, mLastTouchY, mPaletteColor = -1;

        ViewHolder(View itemView, final AnimeRecyclerAdapter adapter) {
            super(itemView);

            this.mAdapter = adapter;
            switch (mAdapter.mDisplayType) {
                case 1:
                case 2:
                case 3:
                    mCardView = itemView.findViewById(R.id.alc_cardLayout);
                    mTitleView = itemView.findViewById(R.id.alc_text);
                    mSubTextView = itemView.findViewById(R.id.alc_text2);
                    mSourceTextView = itemView.findViewById(R.id.alc_text3);
                    mImageView = (SquareImageView) itemView.findViewById(R.id.alc_image);
                    mCheckView = itemView.findViewById(R.id.alc_checkbox);

                    mAuthorView = mSubTextView;
                    mGenreView = mSourceTextView;
                    mSourceView = itemView.findViewById(R.id.alc_text4);
                    mLatestView = itemView.findViewById(R.id.alc_text5);
                    break;
                default:
                    mCheckView = itemView.findViewById(R.id.agi_checkBox);
                    mTitleView = itemView.findViewById(R.id.agi_text);
                    mImageView = itemView.findViewById(R.id.agi_image);
                    break;
            }
            this.itemView.setOnTouchListener(this);
            this.itemView.setOnClickListener(this);
            this.itemView.setOnLongClickListener(this);

            /**
             if (mCardView != null) {
             if (ThemeManager.getInstance().isLightBackground())
             mCardView.setCardBackgroundColor(ThemeManager.getInstance(mAdapter.mContext).getTextColor());
             else
             mCardView.setCardBackgroundColor(Color.parseColor("#f9f9f9"));
             }
             **/
        }

        public void bindAnime(int position, Anime anime) {
            mLastTouchX = 0;
            mLastTouchY = 0;
            mPosition = position;
            mAnime = anime;
            if (mCheckView != null) {
                if (mAdapter.getMode() == MODE_MULTI)
                    mCheckView.setVisibility(View.VISIBLE);
                else {
                    mCheckView.setChecked(false);
                    mCheckView.setVisibility(View.GONE);
                }
            }
            mTitleView.setText(mAnime.getTitle());
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
            //itemView.setActivated(mAdapter.isSelected(getAdapterPosition()));
            ViewCompat.setActivated(itemView, mAdapter.isSelected(getAdapterPosition()));
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
                mAnime.setNewEpisodes(0);
                AnimeUtils.viewAnime(mAdapter.mContext, mImageView, mAnime);
                if (mPosition != -1)
                    mAdapter.notifyItemChanged(mPosition);
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

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mLastTouchX = (int) event.getRawX();
                mLastTouchY = (int) event.getRawY();
            }
            return false;
        }
    }

    private class AnimeFilter extends Filter {
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
                List<Anime> nAnimeList = new ArrayList<Anime>();

                for (Anime p : mItems) {
                    if (p.getTitle().toUpperCase().startsWith(constraint.toString().toUpperCase()))
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
                mResults = (List<Anime>) results.values;
                notifyDataSetChanged();
            }

        }
    }

    private class AnimeCustomFilter extends Filter {
        boolean mSourceCheck = true;
        boolean mPriorityCheck = true;

        public AnimeCustomFilter() {
        }

        public AnimeCustomFilter(boolean priorityCheck, boolean sourceCheck) {
            mPriorityCheck = priorityCheck;
            mSourceCheck = sourceCheck;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            // NOTE: this function is *always* called from a background thread, and
            // not the UI thread.
            constraint = constraint.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if (constraint != null && constraint.toString().length() > 0) {
                mSourceCheck = !constraint.toString().contains("all");
                ArrayList<Anime> filt = new ArrayList<Anime>();
                ArrayList<Anime> lItems = new ArrayList<Anime>();
                synchronized (this) {
                    lItems.addAll(mItems);
                }
                for (int i = 0, l = lItems.size(); i < l; i++) {
                    Anime m = lItems.get(i);
                    String lPriority = String.valueOf(m.getTagId());
                    String lSource = Parser.getNameByUrl(m.getUrl()).toLowerCase();
                    boolean lPriorityCheck = !mPriorityCheck || constraint.toString().contains(lPriority);
                    boolean lSourceCheck = !mSourceCheck || constraint.toString().contains(lSource);
                    if (lPriorityCheck && lSourceCheck)
                        filt.add(m);
                }
                result.count = filt.size();
                result.values = filt;
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
                mResults = (List<Anime>) results.values;
                notifyDataSetChanged();
            }
        }
    }
}