package com.taskdesignsinc.android.myanimeviewer.fragment;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
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
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnCloseListener;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.AnimeRecyclerAdapter;
import com.taskdesignsinc.android.myanimeviewer.adapter.FavoriteListAdapter;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.OnItemClickListener;
import com.taskdesignsinc.android.myanimeviewer.loader.AnimeListLoader;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteTag;
import com.taskdesignsinc.android.myanimeviewer.model.helper.AnimeHelper;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.BuildUtils;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.NetworkUtils;
import com.taskdesignsinc.android.myanimeviewer.util.ParseManager;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.myanimeviewer.view.GridRecyclerView;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.taskdesignsinc.android.myanimeviewer.adapter.base.SelectableAdapter.MODE_SINGLE;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ParseAnimeListFragment extends Fragment
        implements OnQueryTextListener, OnCloseListener,
        LoaderManager.LoaderCallbacks<List<Anime>> {
    private boolean mIsGrid = true;
    private int mGridColumnCount;
    private int mListColumnCount;
    private static final String mTag = ParseAnimeListFragment.class.getSimpleName();
    private Unbinder unbinder;

    private OnItemClickListener mAnimeClickListener = new OnItemClickListener() {
        @Override
        public boolean onListItemClick(int position) {
            if (mActionMode != null && position != INVALID_POSITION) {
                toggleSelection(position);
                return true;
            } else {
                //Notify the active callbacks interfaces (the activity, if the
                //fragment is attached to one) that an item has been selected.
                if (mAdapter.getItemCount() > 0) {
                    if (position != mActivatedPosition) setActivatedPosition(position);
                }
                return false;
            }
        }

        @Override
        public void onListItemLongClick(int position) {
            if (mActionMode == null) {
                mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new RecordOptions((AppCompatActivity) getActivity()));
            }
            toggleSelection(position);
        }
    };
    private boolean noMoreToLoad = false;

    /**
     * Create a new instance of AnimeGridFragment, initialized to show the text at
     * 'index'.
     */
    public static ParseAnimeListFragment newInstance(int pType, int pGenre) {
        ParseAnimeListFragment f = new ParseAnimeListFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(Constants.LOAD_TYPE, pType);
        args.putInt(Constants.GENRE_TYPE, pGenre);
        f.setArguments(args);

        return f;
    }

    // View
    //@InjectView(R.id.fab_button)
    private FloatingActionButton mFloatingActionButton;
    //@InjectView(R.id.swipe_container)
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // The SearchView for doing filtering.
    SearchView mSearchView;

    CoordinatorLayout mCoordinatorLayout;
    //@InjectView(R.id.list)
    @BindView(R.id.list)
    GridRecyclerView mRecyclerView;
    LayoutManager mLayoutManager;
    private AnimeRecyclerAdapter mAdapter;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    int mSortType = 0;

    private SharedPreferences mPrefs;

    private Parser mParser;
    private int mSelectedGenre = 0;
    int mAnimePerPage = 0;
    int mCurrentPage = 1;
    int mType = 0;
    int mCatalogLastPosition = 0;
    private int mItemBaseID = 13900;

    public ActionMode mMode;

    protected Handler mHandler;

    private int previousTotal = 0;
    private boolean loading = true;
    private int visibleThreshold = 10;
    int firstVisibleItem, visibleItemCount, totalItemCount;
    private ArrayList<Anime> mList;
    private HashMap<String, Anime> mAnimeMap;
    private HashMap<Anime, Integer> mAnimePositionMap;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSearchView = null; // now cleaning up!
        mCoordinatorLayout = null;
        mRecyclerView = null;
        mAdapter = null;
        mLayoutManager = null;
        mSwipeRefreshLayout = null;
        mAnimeMap = null;
        mList = null;
        unbinder.unbind();
    }

    private ActionMode mActionMode = null;
    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    /**
     * The current activated item position.
     */
    private static final int INVALID_POSITION = -1;
    private int mActivatedPosition = INVALID_POSITION;
    /**
     * Toggle the selection state of an item.
     * <p>
     * If the item was the last one in the selection and is unselected, the selection is stopped.
     * Note that the selection must already be started (actionMode must not be null).
     *
     * @param position Position of the item to toggle the selection state
     */
    private void toggleSelection(int position) {
        if (mAdapter != null) {
            mAdapter.toggleSelection(position, false);

            int count = mAdapter.getSelectedItemCount();

            if (mActionMode != null) {
                if (count == 0) {
                    mActionMode.finish();
                } else {
                    mActionMode.invalidate();
                }
            }
        }
    }

    private void setActivatedPosition(int position) {
        mActivatedPosition = position;
    }

    public void setSelection(final int position) {
        setActivatedPosition(position);

        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(position);
            }
        }, 1000L);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mActivatedPosition != AdapterView.INVALID_POSITION) {
            //Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mType = getArguments() != null ? getArguments().getInt(Constants.LOAD_TYPE) : 0;
        mSelectedGenre = getArguments() != null ? getArguments().getInt(Constants.GENRE_TYPE) : 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // image_detail_fragment.xml contains just an ImageView
        final View v = inflater.inflate(R.layout.anime_recycler_layout, container, false);
        unbinder = ButterKnife.bind(this, v);
        if (!BuildUtils.isHoneycombOrLater())
            v.setBackgroundColor(ThemeManager.getInstance().getBackgroundColor(v.getContext()));
        mRecyclerView = (GridRecyclerView) v.findViewById(R.id.list);
        //mLeanBackView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (mRecyclerView != null) {
            mRecyclerView.setPopupTextColor(ThemeManager.getInstance().getTextColor());
            mRecyclerView.setPopupBgColor(ThemeManager.getInstance().getPrimaryColor(v.getContext()));
            mRecyclerView.setThumbColor(ThemeManager.getInstance().getPrimaryDarkColor(v.getContext()));
            mRecyclerView.setTrackColor(ThemeManager.getInstance().getAccentColor(v.getContext()));
        }


        //mLeanBackView.setItemAnimator(new ReboundItemAnimator());
        //mRecyclerView.setItemAnimator(AnimatorManager.getInstance().getAnimator());

        // Fab Button
        mFloatingActionButton = (FloatingActionButton) v.findViewById(R.id.fab_button);
        Drawable fabBackground = mFloatingActionButton.getBackground();
        if (fabBackground instanceof ShapeDrawable) {
            ((ShapeDrawable) fabBackground).
                    getPaint().setColor(getResources().
                    getColor(ThemeManager.getInstance().getPrimaryDarkColorResId()));
        } else if (fabBackground instanceof StateListDrawable) {
            StateListDrawable temp = (StateListDrawable) fabBackground;
            DrawableContainerState drawableContainerState = (DrawableContainerState) temp.getConstantState();
            Drawable[] children = drawableContainerState.getChildren();
            GradientDrawable selectedItem = (GradientDrawable) children[0];
            GradientDrawable pressedItem = (GradientDrawable) children[1];
            GradientDrawable unselectedItem = (GradientDrawable) children[2];
            selectedItem.setStroke(100, ThemeManager.getInstance().getAccentColor(v.getContext()));
            pressedItem.setStroke(100, ThemeManager.getInstance().getAccentColor(v.getContext()));
            unselectedItem.setStroke(100, ThemeManager.getInstance().getPrimaryDarkColor(v.getContext()));
        }
        mFloatingActionButton.setImageResource(R.drawable.ic_refresh_white_24dp);
        mFloatingActionButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View pV) {
                mSwipeRefreshLayout.setRefreshing(true);
                loadMoreAnime();
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(ThemeManager.getInstance().getAccentColorResId()));
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadMoreAnime();
            }
        });

        ActivityCompat.setExitSharedElementCallback(getActivity(), new SharedElementCallback() {
            @Override
            public Parcelable onCaptureSharedElementSnapshot(View sharedElement, Matrix viewToGlobalMatrix, RectF screenBounds) {
                int bitmapWidth = Math.round(screenBounds.width());
                int bitmapHeight = Math.round(screenBounds.height());
                Bitmap bitmap = null;
                if (bitmapWidth > 0 && bitmapHeight > 0) {
                    Matrix matrix = new Matrix();
                    matrix.set(viewToGlobalMatrix);
                    matrix.postTranslate(-screenBounds.left, -screenBounds.top);
                    bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.concat(matrix);
                    sharedElement.draw(canvas);
                }
                return bitmap;
            }
        });
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            mCoordinatorLayout = (CoordinatorLayout) getActivity().findViewById(R.id.coordinator_layout);
            //setupRevealBackground(savedInstanceState);
            if (mPrefs == null)
                mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
            mHandler = new Handler();
            mParser = Parser.getExistingInstance(mPrefs.getInt(Constants.KEY_ANIME_SOURCE, Constants.DEFAULT_ANIME_SOURCE));
            mIsGrid = mPrefs.getBoolean(Constants.KEY_SHOW_CATALOG_AS_GRID, true);
            mGridColumnCount = mPrefs.getInt(Constants.KEY_GRID_COLUMN_COUNT, getResources().getInteger(R.integer.grid_num_cols));
            mListColumnCount = mPrefs.getInt(Constants.KEY_LIST_COLUMN_COUNT, getResources().getInteger(R.integer.list_num_cols));

            mLayoutManager = new GridLayoutManager(getActivity(), mIsGrid ? mGridColumnCount : mListColumnCount);
            mRecyclerView.setLayoutManager(mLayoutManager);

            if (mType == 0) {
                //TODO load from database
                mList = (ArrayList<Anime>) MAVApplication.getInstance().getRepository().getAnimeListByUrl(mParser.getServerUrl());
                mAnimeMap = MAVApplication.getInstance().getRepository().getAnimeMapByUrl(mParser.getServerUrl());
            } else {
                mList = new ArrayList<Anime>();
                mAnimeMap = new HashMap<String, Anime>();
            }
            mAnimePositionMap = new HashMap<>();
            generatePositionMap();

            // Create an empty adapter we will use to display the loaded data.
            mAdapter = new AnimeRecyclerAdapter(mRecyclerView, mType + mSelectedGenre, mList,
                    mIsGrid ? Constants.DISPLAY_TYPE_GRID : Constants.DISPLAY_TYPE_LIST,
                    getActivity(), true, mAnimeClickListener);

            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int scrollState) {
                    final Picasso picasso = Picasso.with(getActivity());
                    if (scrollState == RecyclerView.SCROLL_STATE_IDLE || scrollState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        picasso.resumeTag(getActivity());
                    } else {
                        picasso.pauseTag(getActivity());
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    visibleItemCount = mRecyclerView.getChildCount();
                    totalItemCount = mLayoutManager.getItemCount();
                    if (mLayoutManager instanceof GridLayoutManager)
                        firstVisibleItem = ((GridLayoutManager) mLayoutManager).findFirstVisibleItemPosition();

                    if (loading) {
                        if (totalItemCount > previousTotal) {
                            loading = false;
                            previousTotal = totalItemCount;
                        }
                    }
                    if (!loading && (totalItemCount - visibleItemCount)
                            <= (firstVisibleItem + visibleThreshold)) {
                        // Do something
                        loadMoreAnime();
                    }
                }
            });
        }

        //Restore previous state
        if (savedInstanceState != null) {
            //Selection
            mAdapter.onRestoreInstanceState(savedInstanceState);
            if (mAdapter.getSelectedItemCount() > 0) {
                mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new RecordOptions((AppCompatActivity) getActivity()));
            }
            //Previously serialized activated item position
            if (savedInstanceState.containsKey(STATE_ACTIVATED_POSITION))
                setSelection(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    private void generatePositionMap() {
        for (int i = 0; i < mList.size(); i++) {
            mAnimePositionMap.put(mList.get(i), i);
        }
    }

    protected void loadMoreAnime() {
        if (noMoreToLoad)
            return;
        mSwipeRefreshLayout.setRefreshing(true);
        loading = true;
        mCurrentPage++;
        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        /**
         menu.add(Menu.NONE, mItemBaseID, 0, mIsGrid ? "List" : "Grid").setIcon(mIsGrid ? R.drawable.ic_view_list_white_24dp : R.drawable.ic_view_module_white_24dp);
         MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
         menu.add(Menu.NONE, mItemBaseID, 0, "Columns").setIcon(R.drawable.ic_view_column_white_24dp);
         MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
         if (mParser.mIsCatalogSortSupported) {
         menu.add(Menu.NONE, mItemBaseID + 1, 1, R.string.sort).setIcon(android.R.drawable.ic_menu_sort_alphabetically);
         MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
         }
         **/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem pItem) {
        if (pItem != null && !TextUtils.isEmpty(pItem.getTitle())) {
            if (pItem.getTitle().equals("List")) {
                mIsGrid = false;
                mAdapter.setDisplayType(Constants.DISPLAY_TYPE_LIST);
                mRecyclerView.setLayoutManager(mLayoutManager);
                ((GridLayoutManager) mLayoutManager).setSpanCount(mListColumnCount);
                mRecyclerView.setAdapter(mAdapter);
                pItem.setIcon(R.drawable.ic_view_module_white_24dp);
                pItem.setTitle("Grid");
                mPrefs.edit().putBoolean(Constants.KEY_SHOW_CATALOG_AS_GRID, mIsGrid).apply();
            } else if (pItem.getTitle().equals("Grid")) {
                mIsGrid = true;
                mAdapter.setDisplayType(Constants.DISPLAY_TYPE_GRID);
                mRecyclerView.setLayoutManager(mLayoutManager);
                ((GridLayoutManager) mLayoutManager).setSpanCount(mGridColumnCount);
                mRecyclerView.setAdapter(mAdapter);
                pItem.setIcon(R.drawable.ic_view_list_white_24dp);
                pItem.setTitle("List");
                mPrefs.edit().putBoolean(Constants.KEY_SHOW_CATALOG_AS_GRID, mIsGrid).apply();
            } else if (pItem.getTitle().equals("Columns")) {
                new MaterialDialog.Builder(getActivity())
                        .content("Amount of columns to display")
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .input("Column count", "" + (mIsGrid ? mGridColumnCount : mListColumnCount), new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                try {
                                    int columns = Integer.parseInt(input.toString());
                                    if (mIsGrid)
                                        mGridColumnCount = columns;
                                    else
                                        mListColumnCount = columns;
                                } catch (NumberFormatException e) {
                                    WriteLog.appendLogException(mTag, "invalid column count provided", e);
                                }
                                mPrefs.edit().putInt(mIsGrid ? Constants.KEY_GRID_COLUMN_COUNT : Constants.KEY_LIST_COLUMN_COUNT, mIsGrid ? mGridColumnCount : mListColumnCount).apply();
                                ((GridLayoutManager) mLayoutManager).setSpanCount(mIsGrid ? mGridColumnCount : mListColumnCount);
                            }
                        }).show();
            }
        }
        return super.onOptionsItemSelected(pItem);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed.  Since this
        // is a simple array adapter, we can just have it do the filtering.
        mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
        //mAdapter.getFilter().filter(mCurFilter);
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
    public Loader<List<Anime>> onCreateLoader(int id, Bundle args) {
        Loader<List<Anime>> loader = new AsyncTaskLoader<List<Anime>>(getActivity()) {
            @Override
            public List<Anime> loadInBackground() {
                List<Anime> animeList = ParseManager.getInstance(getActivity()).getAnimeList(mCurrentPage);
                List<Anime> cacheAnimeList = new ArrayList<>();
                List<Anime> nonCachedAnimeList = new ArrayList<>();
                Anime cachedAnime = null;
                for (Anime anime : animeList) {
                    cachedAnime = MAVApplication.getInstance().getRepository().getAnimeByUrl(anime.getUrl());
                    if (cachedAnime != null) {
                        AnimeHelper.update(anime, cachedAnime);
                        cacheAnimeList.add(cachedAnime);
                    } else {
                        nonCachedAnimeList.add(anime);
                    }
                }
                MAVApplication.getInstance().getRepository().updateAnimeList(cacheAnimeList);
                MAVApplication.getInstance().getRepository().insertAnimeList(nonCachedAnimeList);
                return animeList;
            }
        };
        loader.forceLoad();
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<List<Anime>> loader, List<Anime> temp) {
        if (temp == null || temp.isEmpty())
            noMoreToLoad = true;
        else
            noMoreToLoad = false;
        if (mAnimePerPage == 0 && temp != null && !temp.isEmpty()) {
            mAnimePerPage = temp.size();
            if (mCurrentPage * mAnimePerPage < mList.size()) {
                mCurrentPage = (mList.size() / mAnimePerPage) - 1;
                if (mCurrentPage <= 1)
                    mCurrentPage = 1;
            }
        }
        List<Anime> data = null;
        if (mAnimeMap != null && temp != null) {
            data = new ArrayList<Anime>();
            for (int i = 0; i < temp.size(); i++) {
                temp.get(i).setCatalogPosition(temp.get(i).getCatalogPosition() + mCatalogLastPosition);
                if (!mAnimeMap.containsKey(temp.get(i).getUrl())) {
                    data.add(temp.get(i));
                }
            }
            //AnimeHelper.getInstance(getActivity()).insertAnimeListPosition(mType + mSelectedGenre, temp);
        } else {
            data = temp;
        }
        mCatalogLastPosition = mCurrentPage * mAnimePerPage;
        mAdapter.setCatalogPositions(temp);
        if (data != null) {
            mAdapter.addAnimeList(data);
            //mAdapter.sort(Anime.Order.ByCatalogIndex);
        } else {
            mCurrentPage--;
            if (mCurrentPage <= 1)
                mCurrentPage = 1;
        }
        if ((temp == null || temp.isEmpty()) && !NetworkUtils.isNetworkAvailable(getActivity()))
            Snackbar.make(mCoordinatorLayout, R.string.network_error, Snackbar.LENGTH_SHORT)
                    .setAction("View cached anime", new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mParser.getServerUrl().startsWith("http://www")) {
                                mList = (ArrayList<Anime>) MAVApplication.getInstance().getRepository().getAnimeListByUrl(mParser.getServerUrl().split("\\.")[1]);
                                mAnimeMap = MAVApplication.getInstance().getRepository().getAnimeMapByUrl(mParser.getServerUrl().split("\\.")[1]);
                            } else {
                                mList = (ArrayList<Anime>) MAVApplication.getInstance().getRepository().getAnimeListByUrl(mParser.getServerUrl().split("\\.")[0]);
                                mAnimeMap = MAVApplication.getInstance().getRepository().getAnimeMapByUrl(mParser.getServerUrl().split("\\.")[0]);
                            }
                            mAdapter.addAnimeList(mList);
                        }
                    })
                    .show();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoaderReset(Loader<List<Anime>> loader) {
        // Clear the data in the adapter.
        WriteLog.appendLog("onLoaderReset called");
        //mAdapter.setData(null);
    }

    private final class RecordOptions implements ActionMode.Callback {
        AppCompatActivity mContext;
        Toolbar mToolbar;
        Boolean mIsAllSelected = false;

        public RecordOptions(AppCompatActivity context) {
            mContext = context;
            mToolbar = (Toolbar) mContext.findViewById(R.id.toolbar);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mAdapter.setMode(AnimeRecyclerAdapter.MODE_MULTI);
            menu.add(R.string.favorite).setIcon(ThemeManager.getInstance(getActivity()).isLightBackground()
                    ? R.drawable.ic_favorite_black_24dp : R.drawable.ic_favorite_white_24dp);
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
                if (item.getTitle().toString().equals(mContext.getString(R.string.favorite))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Change Favorite Tag");
                    ListView lv = new ListView(getActivity());
                    FavoriteListAdapter lAdapter = new FavoriteListAdapter(mContext);
                    lAdapter.add(new FavoriteTag(-1, "Remove", -1));
                    final FavoriteListAdapter adapter = lAdapter;
                    lv.setAdapter(lAdapter);
                    builder.setView(lv);
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    final AlertDialog alert = builder.create();
                    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                        @Override
                        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                            alert.dismiss();
                            List<FavoriteTag> lList = MAVApplication.getInstance().getRepository().getFavoriteTags();

                            if (mAdapter != null) {
                                Anime lAnime = null;
                                for (int i : mAdapter.getSelectedItems()) {
                                    lAnime = mAdapter.getItem(i);
                                    if (arg2 < lList.size()) {
                                        MAVApplication.getInstance().getRepository().insertFavorite(lAnime.getUrl(), adapter.getItem(arg2).getTagId());
                                        lAnime.setTagId(adapter.getItem(arg2).getTagId());
                                    } else {
                                        MAVApplication.getInstance().getRepository().deleteFavorite(lAnime.getUrl());
                                        lAnime.setTagId(-1);
                                    }
                                }
                                mAdapter.notifyDataSetChanged();
                            }
                            mode.finish();
                        }
                    });
                    alert.show();
                } else {
                    mode.finish();
                }
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (getView() != null) {
                mActionMode = null;
                mAdapter.setMode(MODE_SINGLE);
                mAdapter.clearSelection();
            }
        }
    }
}