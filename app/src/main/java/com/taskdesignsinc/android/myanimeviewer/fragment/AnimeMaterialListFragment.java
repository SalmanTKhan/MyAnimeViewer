package com.taskdesignsinc.android.myanimeviewer.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer.DrawableContainerState;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnCloseListener;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.taskdesignsinc.android.myanimeviewer.AnimeDetailsActivity;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.EpisodeListAdapter;
import com.taskdesignsinc.android.myanimeviewer.adapter.FavoriteListAdapter;
import com.taskdesignsinc.android.myanimeviewer.fragment.base.HeaderGridCompatFragment;
import com.taskdesignsinc.android.myanimeviewer.loader.AnimeLoader;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.helper.EpisodeUtils;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.BuildUtils;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.FileUtils;
import com.taskdesignsinc.android.myanimeviewer.util.ImageLoaderManager;
import com.taskdesignsinc.android.myanimeviewer.util.NetworkUtils;
import com.taskdesignsinc.android.myanimeviewer.util.StorageUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.myanimeviewer.view.HeaderGridView;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnimeMaterialListFragment extends HeaderGridCompatFragment
        implements OnQueryTextListener, OnCloseListener, SwipeRefreshLayout.OnRefreshListener,
        LoaderManager.LoaderCallbacks<Anime> {
    //private AnimeRecord mAnimeRecord = null;
    private boolean mIsSavedInParse = false;
    private boolean mIsTablet = false;
    private boolean mLoadImageCalled = false;

    /**
     * Create a new instance of AnimeMaterialListFragment, initialized to show the text at
     * 'index'.
     */
    public static AnimeMaterialListFragment newInstance(int pAnimeID, String pAnimeURL) {
        AnimeMaterialListFragment f = new AnimeMaterialListFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(Constants.ANIME_ID, pAnimeID);
        args.putString(Constants.ANIME_URL, pAnimeURL);
        f.setArguments(args);

        return f;
    }

    /**
     * Create a new instance of AnimeEpisodeFragment, initialized to show the text at
     * 'index'.
     */
    public static AnimeMaterialListFragment newInstance(Bundle args) {
        AnimeMaterialListFragment f = new AnimeMaterialListFragment();

        f.setArguments(args);

        return f;
    }

    final String mTAG = AnimeMaterialListFragment.class.getSimpleName();

    // This is the Adapter being used to display the list's data.
    EpisodeListAdapter mAdapter;

    // The SearchView for doing filtering.
    SearchView mSearchView;
    CoordinatorLayout mCoordinatorLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private AppCompatImageButton mFloatingActionButton;

    private View mHeaderInfoView;

    private ImageView mHeaderImageView;

    private TextView mTitleAuthorTextView;
    private TextView mTitleAliasTextView;
    private TextView mTitleGenreTextView;
    private TextView mTitleStatusTextView;
    private TextView mTitleSummaryTextView;

    private TextView mNameTextView;
    private TextView mSummaryTextView;
    private TextView mAuthorTextView;
    private TextView mAliasTextView;
    private TextView mGenreTextView;
    private TextView mStatusTextView;

    private RelativeLayout mHeaderEpisodeView;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    int mSortType = 0;

    private SharedPreferences mPrefs;

    private Parser mParser;

    private String mAnimeUrl;
    private long mAnimeId;

    Handler mHandler;

    public ActionMode mMode;

    Anime mAnime;
    private int mSelectionMode = Constants.SELECT_SINGLE;
    private int mSelectedLowerBound = -1;
    private int mSelectedUpperBound = -1;
    private boolean mEpisodeHistory = true;
    private boolean mIsViewCounted = false;

    public String mLastEpisodeDownloaded = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAnimeId = getArguments() != null ? getArguments().getLong(Constants.ANIME_ID) : -1;
        mAnimeUrl = getArguments() != null ? getArguments().getString(Constants.ANIME_URL) : "";
        mAnime = MAVApplication.getInstance().getRepository().getAnimeByUrl(mAnimeUrl, true);
        mIsViewCounted = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View layout = super.onCreateView(inflater, container,
                savedInstanceState);
        HeaderGridView lv = layout.findViewById(android.R.id.list);
        ViewGroup parent = (ViewGroup) lv.getParent();

        // Remove ListView and add CustomView  in its place
        int lvIndex = parent.indexOfChild(lv);
        parent.removeViewAt(lvIndex);
        FrameLayout mLinearLayout = (FrameLayout) inflater.inflate(
                R.layout.anime_details_layout, container, false);
        parent.addView(mLinearLayout, lvIndex, lv.getLayoutParams());

        mSwipeRefreshLayout = (SwipeRefreshLayout) layout.findViewById(R.id.anime_refresher);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(ThemeManager.getInstance().getAccentColorResId()));
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    mSwipeRefreshLayout.setRefreshing(true);
                    getLoaderManager().restartLoader(0, null, AnimeMaterialListFragment.this);
                }
            });
        }

        View parentView = null;
        if (mIsTablet) {
            parentView = mLinearLayout;
        } else
            parentView = mHeaderInfoView = inflater.inflate(R.layout.header_anime_info, null);

        mHeaderImageView = parentView.findViewById(R.id.headerImageView);
        //mMaskImageView = parentView.findViewById(R.id.maskImageView);

        mTitleAuthorTextView = parentView.findViewById(R.id.authorTitleTextView);
        mTitleAliasTextView = parentView.findViewById(R.id.aliasTitleTextView);
        mTitleGenreTextView = parentView.findViewById(R.id.genreTitleTextView);
        mTitleStatusTextView = parentView.findViewById(R.id.statusTitleTextView);
        mTitleSummaryTextView = parentView.findViewById(R.id.descriptionTitleTextView);

        if (mTitleAuthorTextView != null)
            mTitleAuthorTextView.setTextColor(ThemeManager.getInstance().getPrimaryTextColor(parentView.getContext()));
        if (mTitleAliasTextView != null)
            mTitleAliasTextView.setTextColor(ThemeManager.getInstance().getPrimaryTextColor(parentView.getContext()));
        if (mTitleGenreTextView != null)
            mTitleGenreTextView.setTextColor(ThemeManager.getInstance().getPrimaryTextColor(parentView.getContext()));
        if (mTitleStatusTextView != null)
            mTitleStatusTextView.setTextColor(ThemeManager.getInstance().getPrimaryTextColor(parentView.getContext()));
        if (mTitleSummaryTextView != null)
            mTitleSummaryTextView.setTextColor(ThemeManager.getInstance().getPrimaryTextColor(parentView.getContext()));

        mSummaryTextView = parentView.findViewById(R.id.descriptionTextView);
        mAuthorTextView = parentView.findViewById(R.id.authorTextView);
        mAliasTextView = parentView.findViewById(R.id.aliasTextView);
        mGenreTextView = parentView.findViewById(R.id.genreTextView);
        mStatusTextView = parentView.findViewById(R.id.statusTextView);

        mNameTextView = parentView.findViewById(R.id.nameTextView);
        mNameTextView.setTextColor(ThemeManager.getInstance().getPrimaryTextColor(parentView.getContext()));

        // Fab Button
        mFloatingActionButton = parentView.findViewById(R.id.fab_button);
        if (mFloatingActionButton != null) {
            Drawable fabBackground = mFloatingActionButton.getBackground();
            if (fabBackground instanceof ShapeDrawable) {
                ((ShapeDrawable) fabBackground).getPaint().setColor(
                        getResources().getColor(ThemeManager.getInstance().getPrimaryDarkColorResId()));
            } else if (fabBackground instanceof StateListDrawable) {
                StateListDrawable tempDrawableList = (StateListDrawable) fabBackground;
                DrawableContainerState drawableContainerState = (DrawableContainerState) tempDrawableList.getConstantState();
                Drawable[] children = drawableContainerState.getChildren();
                GradientDrawable selectedItem = (GradientDrawable) children[0];
                GradientDrawable pressedItem = (GradientDrawable) children[1];
                GradientDrawable unselectedItem = (GradientDrawable) children[2];
                selectedItem.setStroke(100, ThemeManager.getInstance().getAccentColor(parent.getContext()));
                pressedItem.setStroke(100, ThemeManager.getInstance().getAccentColor(parent.getContext()));
                unselectedItem.setStroke(100, ThemeManager.getInstance().getPrimaryDarkColor(parent.getContext()));
            }
            mFloatingActionButton.setImageResource(R.drawable.ic_file_download_white_24dp);
            mFloatingActionButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View pV) {
                    MaterialDialog downloadAllorSelect = new MaterialDialog
                            .Builder(getActivity())
                            .title(R.string.download)
                            .content("Download episodes, either all or select which one.")
                            .positiveText(R.string.all)
                            .neutralText(R.string.select)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                    if (StorageUtils.isStorageAllowed()) {
                                        for (int i = 0; i < getGridAdapter().getCount(); i++) {

                                            AnimeMaterialListFragment.this.directDownloadEpisode((Episode) getGridAdapter().getItem(i));
                                        }
                                    } else {
                                        if (BuildUtils.isMarshmallowOrLater()) {
                                            StorageUtils.requestStoragePermissions(new PermissionListener() {
                                                @Override
                                                public void onPermissionGranted(PermissionGrantedResponse response) {
                                                    for (int i = 0; i < getGridAdapter().getCount(); i++) {
                                                        directDownloadEpisode((Episode) getGridAdapter().getItem(i));
                                                    }
                                                }

                                                @Override
                                                public void onPermissionDenied(PermissionDeniedResponse response) {
                                                    Snackbar.make(mCoordinatorLayout, "My Anime Viewer will be unable to store any anime for offline reading.", Snackbar.LENGTH_SHORT).show();
                                                }

                                                @Override
                                                public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                                    token.continuePermissionRequest();
                                                }
                                            });
                                        }
                                    }
                                }
                            }).onNeutral(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                    getGridView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                                    getGridView().invalidateViews();
                                    mMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new RecordOptions((AppCompatActivity) getActivity()));
                                }
                            })
                            .build();
                    downloadAllorSelect.show();
                }
            });
        }

        mHeaderEpisodeView = (RelativeLayout) inflater.inflate(R.layout.header_anime_episode, null);
        mHeaderEpisodeView.setBackgroundColor(ThemeManager.getInstance().getPrimaryColor(getActivity()));
        ActivityCompat.setEnterSharedElementCallback(getActivity(), new SharedElementCallback() {
            @Override
            public View onCreateSnapshotView(Context context, Parcelable snapshot) {
                View view = new View(context);
                if (snapshot instanceof Bitmap) {
                    view.setBackground(new BitmapDrawable((Bitmap) snapshot));
                }
                return view;
            }

            @Override
            public void onSharedElementStart(List<String> sharedElementNames,
                                             List<View> sharedElements,
                                             List<View> sharedElementSnapshots) {
                ImageView sharedElement = (ImageView) mHeaderImageView;
                for (int i = 0; i < sharedElements.size(); i++) {
                    if (sharedElements.get(i) == sharedElement) {
                        View snapshot = sharedElementSnapshots.get(i);
                        Drawable snapshotDrawable = snapshot.getBackground();
                        sharedElement.setBackground(snapshotDrawable);
                        sharedElement.setImageAlpha(0);
                        forceSharedElementLayout();
                        break;
                    }
                }
            }

            private void forceSharedElementLayout() {
                ImageView sharedElement = (ImageView) mHeaderImageView;
                int widthSpec = View.MeasureSpec.makeMeasureSpec(sharedElement.getWidth(),
                        View.MeasureSpec.EXACTLY);
                int heightSpec = View.MeasureSpec.makeMeasureSpec(sharedElement.getHeight(),
                        View.MeasureSpec.EXACTLY);
                int left = sharedElement.getLeft();
                int top = sharedElement.getTop();
                int right = sharedElement.getRight();
                int bottom = sharedElement.getBottom();
                sharedElement.measure(widthSpec, heightSpec);
                sharedElement.layout(left, top, right, bottom);
            }

            @Override
            public void onSharedElementEnd(List<String> sharedElementNames,
                                           List<View> sharedElements,
                                           List<View> sharedElementSnapshots) {
                ImageView sharedElement = (ImageView) mHeaderImageView;
                sharedElement.setBackground(null);
                sharedElement.setImageAlpha(255);
                String lCover = MAVApplication.getInstance().getRepository().getCoverByUrl(mAnimeUrl);
                if (!TextUtils.isEmpty(lCover)) {
                    ImageLoaderManager.getInstance(getActivity()).loadImage(lCover, mHeaderImageView);
                    mLoadImageCalled = true;
                }
            }
        });
        if (BuildUtils.isLollipopOrLater())
            layout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    layout.getViewTreeObserver().removeOnPreDrawListener(this);
                    ActivityCompat.startPostponedEnterTransition(getActivity());
                    return true;
                }
            });

        return layout;
    }

    public void setDetails(final Anime anime) {
        final AppCompatActivity pActivity = ((AppCompatActivity) getActivity());
        if (anime != null) {
            if (this.isAdded())
                setGridShown(true);
            WriteLog.appendLog("Setting anime details for " + anime.getTitle());
            mAnime = anime;
            if (pActivity != null && !TextUtils.isEmpty(mAnime.getTitle())) {
                if (mNameTextView != null)
                    mNameTextView.setText(mAnime.getTitle());
            }
            if (mAliasTextView != null) {
                if (!TextUtils.isEmpty(mAnime.getAliases()))
                    mAliasTextView.setText(mAnime.getAliases());
                else {
                    mTitleAliasTextView.setVisibility(View.GONE);
                    mAliasTextView.setVisibility(View.GONE);
                }
            }
            if (mAuthorTextView != null) {
                if (!TextUtils.isEmpty(mAnime.getCreator()))
                    mAuthorTextView.setText(mAnime.getCreator());
                else {
                    mTitleAuthorTextView.setVisibility(View.GONE);
                    mAuthorTextView.setVisibility(View.GONE);
                }
            }
            if (mGenreTextView != null) {
                if (!TextUtils.isEmpty(mAnime.getGenres()))
                    mGenreTextView.setText(mAnime.getGenres());
                else {
                    mTitleGenreTextView.setVisibility(View.GONE);
                    mGenreTextView.setVisibility(View.GONE);
                }
            }
            if (mStatusTextView != null)
                mStatusTextView.setText(((mAnime.getStatus() == 0) ? " Ongoing" : " Completed"));
            if (mSummaryTextView != null) {
                if (!TextUtils.isEmpty(mAnime.getSummary()))
                    mSummaryTextView.setText(mAnime.getSummary());
                else {
                    mTitleSummaryTextView.setVisibility(View.GONE);
                    mSummaryTextView.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        final AppCompatActivity activity = ((AppCompatActivity) getActivity());
        if (activity != null) {
            mCoordinatorLayout = (CoordinatorLayout) activity.findViewById(R.id.coordinator_layout);
            if (mHandler == null)
                mHandler = new Handler();
            if (mPrefs == null)
                mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
            mParser = Parser.getExistingInstance(Parser.getTypeByURL(mAnimeUrl));
            mSortType = mPrefs.getBoolean(Constants.KEY_EPISODE_DEFAULT_SORT, true) ? 0 : 1;
            mEpisodeHistory = mPrefs.getBoolean(Constants.KEY_EPISODE_HISTORY, true);

            // Create an empty adapter we will use to display the loaded data.
            mAdapter = new EpisodeListAdapter(getActivity(), false);
            mAdapter.setData(mAnime);
            if (mSortType == 1)
                mAdapter.sort(Collections.reverseOrder(Episode.SortByPosition));
            initListViewHeaders();
            setGridAdapter(mAdapter);
            getGridView().setOnItemLongClickListener(new RecordOptions(activity));
        }

        if (getGridAdapter().getCount() == 0) {
            if (this.isAdded())
                setGridShown(false);
            // Prepare the loader.  Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(0, null, this);
        } else {
            //mAnimeActionBar.setVisibility(View.INVISIBLE);
            String lCover = MAVApplication.getInstance().getRepository().getCoverByUrl(mAnimeUrl);
            if (!BuildUtils.isLollipopOrLater()) {
                if (this.isAdded())
                    setGridShown(false);
                if (!TextUtils.isEmpty(lCover))
                    ImageLoaderManager.getInstance(getActivity()).loadImage(lCover, mHeaderImageView);
            } else {
                if (!mLoadImageCalled) {
                    if (!TextUtils.isEmpty(lCover))
                        ImageLoaderManager.getInstance(getActivity()).loadImage(lCover, mHeaderImageView);
                }
            }
            if (mPrefs.getBoolean(Constants.KEY_EPISODE_SHOW_WARNING, true))
                if (!TextUtils.isEmpty(mAnime.getWarning()))
                    showCustomAlertDialog("Warning", mAnime.getWarning());
            setDetails(mAnime);
            //mAnime.save(getActivity());
            activity.supportInvalidateOptionsMenu();
        }
    }

    private void initListViewHeaders() {
        if (mHeaderInfoView != null) {
            getGridView().addHeaderView(mHeaderInfoView, null, false);
        }
        if (mHeaderEpisodeView != null) {
            getGridView().addHeaderView(mHeaderEpisodeView, null, false);
        }
    }

    public static class EpisodeSearchView extends SearchView {
        public EpisodeSearchView(Context context) {
            super(context);
        }

        // The normal SearchView doesn't clear its search text when
        // collapsed, so we will do this for it.
        @Override
        public void onActionViewCollapsed() {
            setQuery("", false);
            super.onActionViewCollapsed();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    private int mItemBaseID = 10100;

    public static boolean mIsViewing = false;

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        // Place an action bar item for searching.
        if (MAVApplication.getInstance().getRepository().isFavorite(mAnimeUrl))
            menu.add(Menu.NONE, mItemBaseID, 0, R.string.favorite).setIcon(ThemeManager.getInstance(getActivity()).isLightBackground()
                    ? R.drawable.ic_favorite_black_24dp : R.drawable.ic_favorite_white_24dp);
        else
            menu.add(Menu.NONE, mItemBaseID, 0, R.string.favorite).setIcon(ThemeManager.getInstance(getActivity()).isLightBackground()
                    ? R.drawable.ic_favorite_border_black_24dp : R.drawable.ic_favorite_border_white_24dp);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, mItemBaseID + 1, 0, R.string.update)
                .setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_refresh_black_24dp : R.drawable.ic_refresh_white_24dp);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItem item = menu.add(Menu.NONE, mItemBaseID + 2, 0, R.string.search_episodes);
        item.setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_search_black_24dp : R.drawable.ic_search_white_24dp);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
                | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        mSearchView = new EpisodeSearchView(getActivity());
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.setIconifiedByDefault(true);
        MenuItemCompat.setActionView(item, mSearchView);
        menu.add(Menu.NONE, mItemBaseID + 3, 0, R.string.sort_episodes).setIcon(android.R.drawable.ic_menu_sort_alphabetically);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, mItemBaseID + 4, 0, R.string.download).setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_download_white_24dp);
        menu.add(Menu.NONE, mItemBaseID + 5, 0, R.string.mark_viewed);
        menu.add(Menu.NONE, mItemBaseID + 6, 0, R.string.mark_unviewed);
        //menu.add(Menu.NONE, mItemBaseID + 9, 0, "Show/Hide Viewed Episodes");
        //menu.add(Menu.NONE, mItemBaseID + 10, 0, R.string.share);
        //menu.add(Menu.NONE, mItemBaseID + 11, 0, "Find Alternate Sources");
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem pItem) {
        if (getActivity() == null)
            return false;
        if (!TextUtils.isEmpty(pItem.getTitle())) {
            if (pItem.getTitle().toString().equals("Find Alternate Sources")) {
                if (mAnime != null) {
                    /**
                     getFragmentManager()
                     .beginTransaction()
                     .hide(this)
                     .addToBackStack(null)
                     .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                     .add(R.id.content_frame, ResultFragment.newInstance(mAnime.getTitle()), "ResultFrag")
                     .commit();
                     **/
                }
            } else if (pItem.getTitle().toString().equals(getString(R.string.favorite))) {
                if (mAnime != null) {
                    if (TextUtils.isEmpty(mAnime.getTitle())) {
                        Snackbar.make(mCoordinatorLayout, "The parser didn't load the data yet", Snackbar.LENGTH_SHORT).show();
                        return true;
                    }
                    if (!MAVApplication.getInstance().getRepository().isFavorite(
                            mAnime.getUrl())) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                getActivity());
                        builder.setTitle("Add to Favorites");
                        ListView lv = new ListView(getActivity());
                        lv.setAdapter(new FavoriteListAdapter(getActivity()));
                        builder.setView(lv);
                        builder.setNegativeButton("Cancel",
                                new OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        dialog.cancel();
                                    }
                                });
                        final AlertDialog alert = builder.create();
                        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                            @Override
                            public void onItemClick(AdapterView<?> arg0, View arg1,
                                                    int arg2, long arg3) {
                                MAVApplication.getInstance().getRepository()
                                        .insertFavorite(
                                                mAnime.getUrl(), arg2);
                                Snackbar.make(mCoordinatorLayout, mAnime.getTitle()
                                        + " updated in Favorites", Snackbar.LENGTH_SHORT).show();
                                alert.dismiss();
                                pItem.setIcon(ThemeManager.getInstance(getActivity()).isLightBackground()
                                        ? R.drawable.ic_favorite_black_24dp : R.drawable.ic_favorite_white_24dp);
                            }

                        });
                        alert.show();
                    } else {
                        MAVApplication.getInstance().getRepository()
                                .deleteFavorite(mAnime.getUrl());
                        Snackbar.make(mCoordinatorLayout, mAnime.getTitle() + " removed from Favorites", Snackbar.LENGTH_SHORT).show();
                        pItem.setIcon(ThemeManager.getInstance(getActivity()).isLightBackground()
                                ? R.drawable.ic_favorite_border_black_24dp : R.drawable.ic_favorite_border_white_24dp);
                    }
                } else
                    Snackbar.make(mCoordinatorLayout, mAnimeUrl + " not found in database.", Snackbar.LENGTH_SHORT).show();
                return true;
            } else if (pItem.getTitle().equals(getString(R.string.update))) {
                if (mAnime == null)
                    return true;
                if (mAnime.getStatus() == 1)
                    return true;
                // Start out with a progress indicator.
                if (this.isAdded())
                    setGridShown(false);
                // Prepare the loader.  Either re-connect with an existing one,
                // or start a new one.
                getLoaderManager().restartLoader(0, null, this);
                return true;
            } else if (pItem.getTitle().equals(getString(R.string.sort_episodes))) {
                mSortType = mSortType == 0 ? 1 : 0;
                if (mSortType == 0)
                    mAdapter.sort(Episode.SortByPosition);
                else if (mSortType == 1)
                    mAdapter.sort(Collections.reverseOrder(Episode.SortByPosition));
                return true;
            } else if (pItem.getTitle().equals(getString(R.string.download))) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                alertDialogBuilder.setTitle(R.string.download);
                alertDialogBuilder.setMessage(getGridAdapter().getCount() + " episodes will be added to the download queue. Are you sure?");
                //null should be your on click listener
                alertDialogBuilder.setPositiveButton(R.string.ok, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (StorageUtils.isStorageAllowed()) {
                            for (int i = 0; i < getGridAdapter().getCount(); i++) {
                                downloadEpisode((Episode) getGridAdapter().getItem(i));
                            }
                        } else {
                            if (BuildUtils.isMarshmallowOrLater()) {
                                StorageUtils.requestStoragePermissions(new PermissionListener() {
                                    @Override
                                    public void onPermissionGranted(PermissionGrantedResponse response) {
                                        for (int i = 0; i < getGridAdapter().getCount(); i++) {
                                            downloadEpisode((Episode) getGridAdapter().getItem(i));
                                        }
                                    }

                                    @Override
                                    public void onPermissionDenied(PermissionDeniedResponse response) {
                                        Snackbar.make(mCoordinatorLayout, "My Anime Viewer will be unable to store any anime for offline viewing.", Snackbar.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                                        token.continuePermissionRequest();
                                    }
                                });
                            }
                        }
                    }
                });
                alertDialogBuilder.setNegativeButton(R.string.cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialogBuilder.show();
                return true;
            } else if (pItem.getTitle().toString().contains("Watched Episodes")) {
                mAdapter.setHideViewed(!mAdapter.isHideViewed());
                return true;
            } else if (pItem.getTitle().toString().equals(getString(R.string.mark_viewed))) {
                showMarkEpisodeDialog(1);
            } else if (pItem.getTitle().toString().equals(getString(R.string.mark_unviewed))) {
                showMarkEpisodeDialog(0);
            }
        }
        return super.

                onOptionsItemSelected(pItem);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed.  Since this
        // is a simple array adapter, we can just have it do the filtering.
        mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
        mAdapter.getFilter().filter(mCurFilter);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        // Don't care about this.
        return true;
    }

    @Override
    public boolean onClose() {
        if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }
        return true;
    }

    @Override
    public void onGridItemClick(HeaderGridView l, View v, final int position, long id) {
        final int actualPosition = position - getGridView().getHeaderViewsCount();
        if (mMode == null) {
            // Insert desired behavior here.
            Episode lObject = (Episode) l.getAdapter().getItem(position);
            if (lObject != null) {
                getGridView().setSelection(actualPosition);
                getGridView().setSelected(false);

                final Episode lEpisode = lObject;
                if (lEpisode != null) {
                    //if (!CastUtils.showQueuePopup(getActivity(), v, mAnime, lEpisode))
                    viewEpisode(actualPosition, lEpisode);
                    updateItemAtPosition(actualPosition);
                }
            }
        } else {
            if (mSelectionMode == Constants.SELECT_MULTIPLE) {
                if (mSelectedLowerBound == -1 || mSelectedUpperBound > actualPosition) {
                    mSelectedLowerBound = actualPosition;
                } else if (mSelectedUpperBound == -1 || mSelectedUpperBound < actualPosition) {
                    if (actualPosition < mSelectedLowerBound) {
                        mSelectedUpperBound = mSelectedLowerBound;
                        mSelectedLowerBound = actualPosition;
                    } else
                        mSelectedUpperBound = actualPosition;
                }
                if (mSelectedLowerBound != -1 && mSelectedUpperBound != -1) {
                    for (int i = mSelectedLowerBound; i <= mSelectedUpperBound; i++) {
                        getGridView().setItemChecked(i, true);
                        updateItemAtPosition(i);
                    }
                }
                mAdapter.notifyDataSetChanged();
            } else {
                if (mSelectionMode == Constants.SELECT_SINGLE) {
                    updateItemAtPosition(actualPosition);
                }
            }
        }
    }

    private void updateItemAtPosition(int position) {
        if (position == -1)
            return;
        int visiblePosition = getGridView().getFirstVisiblePosition();
        View view = getGridView().getChildAt(position - visiblePosition + getGridView().getHeaderViewsCount());
        getGridView().getAdapter().getView(position + getGridView().getHeaderViewsCount(), view, getGridView());
    }

    private void viewEpisode(int position, Episode episode) {
        if (episode != null) {
            episode = MAVApplication.getInstance().getRepository().getEpisodeByUrl(episode.getUrl());
        }
        //WriteLog.appendLog(mTAG + ": viewEpisode({0}, {1})", "" + position, episode.toString());
        mIsViewing = true;
        if (mEpisodeHistory) {
            if (getGridAdapter() != null) {
                mAdapter.setLastEpisodeViewed(episode);
                episode.setViewed(true);
                String lastEpisodeUrl = MAVApplication.getInstance().getRepository().getLastViewedEpisode(mAnimeUrl);
                if (mAdapter.getItem(position) != null)
                    mAdapter.getItem(position).setViewed(true);
                updateItemAtPosition(mAdapter.getItemPosition(lastEpisodeUrl));
                MAVApplication.getInstance().getRepository().updateEpisode(episode);
            }
        }
        if (TextUtils.isEmpty(episode.getLocalPath())) {
            File lDownloadDir = new File(StorageUtils.getDataDirectory(), mAnime.getTitle() + "/" + episode.getTitle() + "/");
            if (lDownloadDir.exists()) {
                episode.setLocalPath(lDownloadDir.getAbsolutePath());
            } else {
                lDownloadDir = new File(StorageUtils.getDataDirectory(), FileUtils.getValidFileName(mAnime.getTitle()) + "/" + FileUtils.getValidFileName(episode.getTitle()) + "/");
                if (lDownloadDir.exists()) {
                    episode.setLocalPath(lDownloadDir.getAbsolutePath());
                }
            }
        }
        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            MAVApplication.getInstance().getRepository().insertHistoryRecord(mAnime.getUrl(), episode.getUrl(), true);
            EpisodeUtils.viewEpisode(mTAG, getActivity(), mAnime, episode, mCoordinatorLayout);
        } else if (!TextUtils.isEmpty(episode.getLocalPath())) {
            MAVApplication.getInstance().getRepository().insertOfflineHistoryRecord(episode.getLocalPath(), true);
            EpisodeUtils.viewEpisodeOffline(mTAG, getActivity(), mAnime, episode, mCoordinatorLayout);
        }
    }

    private void downloadEpisodes(ArrayList<Episode> episodes) {
        if (mPrefs.getBoolean(Constants.KEY_EPISODE_SORT_BEFORE_DOWNLOAD, true))
            Collections.sort(episodes, Episode.SortByPosition);
        for (int i = 0; i < episodes.size(); i++)
            directDownloadEpisode(episodes.get(i));
    }

    private void downloadEpisode(Episode pEpisode) {
        if (pEpisode != null) {
            if (MAVApplication.getInstance().getRepository().getDownloadTask(pEpisode.getUrl(), false) != null
                    && !mLastEpisodeDownloaded.equals(pEpisode.getUrl())) {
                Snackbar.make(mCoordinatorLayout, "Are you sure you want to download " + pEpisode.getTitle() + " again, then press download again", Snackbar.LENGTH_SHORT).show();
                mLastEpisodeDownloaded = pEpisode.getUrl();
                return;
            }
            EpisodeUtils.downloadEpisode(getActivity(), pEpisode);
        } else {
            WriteLog.appendLog("downloadEpisode because of null episode");
            Snackbar.make(mCoordinatorLayout, "Something went wrong, episode object was null", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void directDownloadEpisode(Episode pEpisode) {
        if (pEpisode != null) {
            EpisodeUtils.downloadEpisode(getActivity(), pEpisode);
        } else {
            WriteLog.appendLog("downloadEpisode because of null episode");
            Snackbar.make(mCoordinatorLayout, "Something went wrong, episode object was null", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void changeViewedStatusByEpisode(Episode pEpisode, boolean pIsViewed) {
        if (pEpisode != null) {
            if (mAnime != null)
                MAVApplication.getInstance().getRepository().insertHistoryRecord(mAnime.getUrl(), pEpisode.getUrl(), pIsViewed);
            pEpisode.setViewed(pIsViewed);
        }
    }

    private void changeViewedStatusByPosition(int pPosition, boolean pIsViewed) {
        WriteLog.appendLog("changeViewedStatusByPosition(" + pPosition + "," + pIsViewed + ")");
        Episode pEpisode = mAdapter.getItem(pPosition);
        if (pEpisode != null) {
            MAVApplication.getInstance().getRepository().insertHistoryRecord(mAnime.getUrl(), pEpisode.getUrl(), pIsViewed);
            pEpisode.setViewed(pIsViewed);
        }
    }

    @Override
    public Loader<Anime> onCreateLoader(int id, Bundle args) {
        WriteLog.appendLog(mTAG, "Anime loader started");
        AnimeLoader loader = new AnimeLoader(getActivity(), mParser, mAnimeUrl);
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Anime> loader, final Anime data) {
        WriteLog.appendLog(mTAG, "Anime loader finished");
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(false);
        // Set the new data in the adapter.
        if (data != null) {
            if (mAnime == null)
                MAVApplication.getInstance().getRepository().insertAnime(data);
            if (getArguments() != null)
                getArguments().putString(Constants.ANIME_URL, data.getUrl());
            mAnimeUrl = data.getUrl();
            mAnime = data;
            if (!TextUtils.isEmpty(data.getCover()))
                ImageLoaderManager.getInstance(getActivity()).loadImage(data.getCover(), mHeaderImageView);
            mAdapter.setData(data);
            mAdapter.getFilter().filter(null);
            //mAnime.save(getActivity());
        }

        // The list should now be shown.
        if (isResumed()) {
            setGridShown(true);
        } else {
            setGridShownNoAnimation(true);
        }
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (data != null) {
                    if (mPrefs.getBoolean(Constants.KEY_EPISODE_SHOW_WARNING, true))
                        if (!TextUtils.isEmpty(data.getWarning()))
                            showCustomAlertDialog("Warning", data.getWarning());
                    setDetails(mAnime);
                }
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Anime> loader) {
        WriteLog.appendLog("Anime loader reset");
        // Clear the data in the adapter.
        mAdapter.setData(null);
    }

    public void dismissDialog() {
        final FragmentManager fm = getFragmentManager();
        if (fm == null)
            return;
        Fragment prev = fm.findFragmentByTag("cadfragment");
        if (prev != null) {
            DialogFragment df = (DialogFragment) prev;
            df.dismiss();
        }
    }

    private synchronized void showCustomAlertDialog(String title, String msg) {
        final FragmentManager fm = getFragmentManager();
        if (fm == null)
            return;
        // Create the fragment and show it as a dialog.
        dismissDialog();
        //DialogFragment newFragment = CustomAlertDialog.CADFragment.newInstance(android.R.drawable.ic_dialog_alert, title, msg);
        //newFragment.show(fm, "cadfragment");
    }

    private synchronized void showMarkEpisodeDialog(int type) {
        if (mAnime == null)
            return;
        final FragmentManager fm = getFragmentManager();
        if (fm == null)
            return;
        // Create the fragment and show it as a dialog.
        dismissDialog();
        DialogFragment newFragment = new MarkEpisodeDialog();
        newFragment.setTargetFragment(this, 0);
        Bundle bundle = new Bundle();
        bundle.putInt("type", type);
        bundle.putInt("itype", InputType.TYPE_CLASS_NUMBER);
        bundle.putInt("itype2", InputType.TYPE_CLASS_NUMBER);
        bundle.putInt("min", 0);
        bundle.putInt("max", mAnime.getEpisodes().size());
        newFragment.setArguments(bundle);
        newFragment.show(fm,
                "mcdfragment");
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private final class RecordOptions implements ActionMode.Callback, AdapterView.OnItemLongClickListener {
        AppCompatActivity mContext;
        Toolbar mToolbar;
        Boolean mIsAllSelected = false;

        public RecordOptions(AppCompatActivity context) {
            mContext = context;
            mToolbar = (Toolbar) mContext.findViewById(R.id.toolbar);
        }

        @Override
        public boolean onItemLongClick(AdapterView<?> view, View row,
                                       int position, long id) {

            if (mMode == null) {
                getGridView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
                getGridView().invalidateViews();
                mMode = mContext.startSupportActionMode(this);
            } else {
                getGridView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                getGridView().invalidateViews();
                mMode.finish();
            }

            return (true);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(R.string.select_mode).setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_select_all_black_24dp : R.drawable.ic_select_all_white_24dp);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            menu.add(R.string.download)
                    .setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_download_white_24dp);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            menu.add(R.string.delete).setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_delete_black_24dp : R.drawable.ic_delete_white_24dp);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            menu.add(R.string.mark_viewed);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            menu.add(R.string.mark_unviewed);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(final ActionMode mode,
                                           MenuItem item) {
            if (isAdded() && getView() != null) {
                if (item.getTitle().equals(getString(R.string.select_mode))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(
                            getActivity());
                    builder.setTitle("Selection Mode");
                    builder.setSingleChoiceItems(Constants.SELECTION_CHOICES, mSelectionMode, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface pDialog, int pWhich) {
                            if (mSelectionMode != pWhich) {
                                mSelectionMode = pWhich;
                                if (pWhich == Constants.SELECT_ALL) {
                                    mIsAllSelected = !mIsAllSelected;
                                    if (mIsAllSelected)
                                        WriteLog.appendLog("Selecting All:  total count "
                                                + mAdapter.getCount());
                                    else
                                        WriteLog.appendLog("Unselecting All: total count "
                                                + mAdapter.getCount());
                                    for (int i = 0; i < mAdapter.getCount(); i++)
                                        getGridView().setItemChecked(i, mIsAllSelected);
                                    mAdapter.notifyDataSetChanged();
                                    mSelectedLowerBound = -1;
                                    mSelectedUpperBound = -1;
                                    mSelectionMode = Constants.SELECT_SINGLE;
                                }
                            }
                            pDialog.dismiss();
                        }
                    });
                    final AlertDialog alert = builder.create();
                    alert.show();
                } else if (item.getTitle().toString().equals(getString(R.string.delete))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Delete Episode(s)");
                    builder.setPositiveButton(android.R.string.yes, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface pDialog, int pWhich) {
                            pDialog.dismiss();
                            SparseBooleanArray checkArray = getGridView().getCheckedItemPositions();

                            int count = checkArray.size();
                            Episode lEpisode = null;
                            for (int i = 0; i < count; i++) {
                                if (checkArray.valueAt(i)) {
                                    lEpisode = mAdapter.getItem(checkArray.keyAt(i) - getGridView().getHeaderViewsCount());
                                    if (lEpisode != null && !TextUtils.isEmpty(lEpisode.getLocalPath()))
                                        FileUtils.deleteDirectory(lEpisode.getLocalPath());
                                }
                            }
                            mAdapter.checkEpisodeStatus();
                            mode.finish();
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    final AlertDialog alert = builder.create();
                    alert.show();
                } else if (item.getTitle().toString().equals(getString(R.string.download))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Download Episode(s)");
                    builder.setPositiveButton(android.R.string.yes, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface pDialog, int pWhich) {
                            pDialog.dismiss();
                            SparseBooleanArray checkArray = getGridView().getCheckedItemPositions();

                            int count = checkArray.size();
                            for (int i = 0; i < count; i++) {
                                if (checkArray.valueAt(i)) {
                                    downloadEpisode(mAdapter.getItem(checkArray.keyAt(i) - getGridView().getHeaderViewsCount()));
                                }
                            }
                            mode.finish();
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    final AlertDialog alert = builder.create();
                    alert.show();
                } else if (item.getTitle().toString().equals(getString(R.string.mark_viewed))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.mark_viewed);
                    builder.setPositiveButton(android.R.string.yes, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface pDialog, int pWhich) {
                            pDialog.dismiss();
                            SparseBooleanArray checkArray = getGridView().getCheckedItemPositions();

                            int count = checkArray.size();
                            for (int i = 0; i < count; i++) {
                                if (checkArray.valueAt(i)) {
                                    changeViewedStatusByEpisode(mAdapter.getItem(checkArray.keyAt(i) - getGridView().getHeaderViewsCount()), true);
                                }
                            }
                            mAdapter.checkEpisodeStatus();
                            mode.finish();
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    final AlertDialog alert = builder.create();
                    alert.show();
                } else if (item.getTitle().toString().equals(getString(R.string.mark_unviewed))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.mark_unviewed);
                    builder.setPositiveButton(android.R.string.yes, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface pDialog, int pWhich) {
                            pDialog.dismiss();
                            SparseBooleanArray checkArray = getGridView().getCheckedItemPositions();

                            int count = checkArray.size();
                            for (int i = 0; i < count; i++) {
                                if (checkArray.valueAt(i)) {
                                    changeViewedStatusByEpisode(mAdapter.getItem(checkArray.keyAt(i) - getGridView().getHeaderViewsCount()), false);
                                }
                            }
                            mAdapter.checkEpisodeStatus();
                            mode.finish();
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    final AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    mode.finish();
                }
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mSelectionMode = Constants.SELECT_SINGLE;
            mSelectedLowerBound = -1;
            mSelectedUpperBound = -1;
            if (getView() != null) {
                getGridView().clearChoices();
                getGridView().setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                getGridView().invalidateViews();
            }
            mMode = null;
        }
    }

    public void changeAnime(String animeUrl) {
        mAnimeUrl = animeUrl;
        getLoaderManager().restartLoader(0, null, this);
    }

    public void refresh() {
        if (mAdapter != null) {
            mAnime.setEpisodes(MAVApplication.getInstance().getRepository().getEpisodes(mAnimeUrl));
            mAdapter.setData(mAnime);
        }
    }

    public void refresh(String animeUrl) {
        if (mAdapter != null) {
            if (TextUtils.equals(animeUrl, mAnimeUrl) && !mIsViewing) {
                mAdapter.checkEpisodeStatus();
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    public void refresh(final String animeUrl, final String episodeUrl) {
        WriteLog.appendLog("Refreshing episodes");
        if (mAdapter != null) {
            //mAdapter.checkEpisodeStatus(animeUrl, episodeUrl);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void refreshEpisode(String animeURL, String episodeURL, int episodePos) {
        if (mAdapter != null) {
            if (TextUtils.equals(animeURL, mAnimeUrl) && !mIsViewing) {
                //mAdapter.checkEpisodeStatus(episodeURL, episodePos);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MarkEpisodeDialog extends DialogFragment {

        private EditText mRangeStart;
        private EditText mRangeEnd;
        protected int mType;
        protected int mRangeMin;
        protected int mRangeMax;
        protected int mInputType;
        protected int mInputType2;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
        }

        public MarkEpisodeDialog newInstance(int type) {
            MarkEpisodeDialog f = new MarkEpisodeDialog();
            // Supply index input as an argument.
            Bundle args = new Bundle();
            args.putInt("type", type);
            f.setArguments(args);
            return f;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            final View lView = inflater.inflate(R.layout.dialog_input,
                    null);

            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            mType = args.getInt("type", -1);
            mInputType = args.getInt("itype", -1);
            mInputType2 = args.getInt("itype2", -1);
            mRangeMin = args.getInt("min", 0);
            mRangeMax = args.getInt("max", 1);

            mRangeStart = (EditText) lView.findViewById(R.id.dlg_input1);
            mRangeEnd = (EditText) lView.findViewById(R.id.dlg_input2);

            if (mInputType == -1)
                mInputType = InputType.TYPE_NULL;
            if (mInputType2 == -1)
                mInputType2 = mInputType;
            mRangeStart.setText("" + mRangeMin);
            mRangeEnd.setText("" + mRangeMax);

            if (mRangeStart != null) {
                mRangeStart.setInputType(mInputType);
            }

            if (mRangeEnd != null) {
                mRangeEnd.setInputType(mInputType2);
            }

            return new AlertDialog.Builder(getActivity())
                    .setView(lView)
                    .setCancelable(true)
                    .setTitle(mType == 1 ? getString(R.string.mark_viewed) : getString(R.string.mark_unviewed))
                    .setPositiveButton(R.string.ok, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                int rangeStart = Integer.parseInt(mRangeStart.getText().toString());
                                int rangeEnd = Integer.parseInt(mRangeEnd.getText().toString());
                                if (rangeStart >= mRangeMin - 1 && rangeEnd <= mRangeMax) {
                                    dialog.dismiss();
                                    for (int i = rangeStart; i < rangeEnd; i++) {
                                        ((AnimeMaterialListFragment) getTargetFragment()).changeViewedStatusByPosition(i, mType == 1 ? true : false);
                                    }
                                    ((AnimeDetailsActivity) getActivity()).refresh();
                                }
                            } catch (NumberFormatException e) {
                                WriteLog.appendLog(Log.getStackTraceString(e));
                                e.printStackTrace();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).create();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public void onDestroyView() // necessary for restoring the dialog
        {
            if (getDialog() != null && getRetainInstance())
                getDialog().setOnDismissListener(null);

            super.onDestroyView();
        }

    }

    @Override
    public void onRefresh() {
        getLoaderManager().restartLoader(0, null, this);
    }
}