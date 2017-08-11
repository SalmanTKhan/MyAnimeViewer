package com.taskdesignsinc.android.myanimeviewer.fragment;

import android.annotation.TargetApi;
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
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnCloseListener;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.squareup.picasso.Picasso;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.EpisodeRecyclerAdapter;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.OnItemClickListener;
import com.taskdesignsinc.android.myanimeviewer.loader.AnimeListLoader;
import com.taskdesignsinc.android.myanimeviewer.loader.AnimeLoader;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.BuildUtils;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.NetworkUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.myanimeviewer.view.GridRecyclerView;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class EpisodesFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Anime> {
    private boolean mIsGrid = false;
    private int mGridColumnCount;
    private int mListColumnCount;
    private static final String mTag = EpisodesFragment.class.getSimpleName();
    private Unbinder unbinder;
    private OnItemClickListener itemClickListener = new OnItemClickListener() {
        @Override
        public boolean onListItemClick(int position) {
            return false;
        }

        @Override
        public void onListItemLongClick(int position) {

        }
    };
    private boolean noMoreToLoad = false;
    private String mAnimeUrl;

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

    // Controller
    // This is the Adapter being used to display the list's data.
    private EpisodeRecyclerAdapter mAdapter;

    // Model

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

    private Anime mAnime;

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSearchView = null; // now cleaning up!
        mCoordinatorLayout = null;
        mRecyclerView = null;
        mAdapter = null;
        mLayoutManager = null;
        mSwipeRefreshLayout = null;
        unbinder.unbind();
    }

    private ActionMode mActionMode = null;

    public void setSelection(final int position) {
        //setActivatedPosition(position);

        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(position);
            }
        }, 1000L);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mAnimeUrl = getArguments() != null ? getArguments().getString(Constants.ANIME_URL) : "";
        mAnime = MAVApplication.getInstance().getRepository().getAnimeByUrl(mAnimeUrl, true);
        mParser = Parser.getExistingInstance(Parser.getTypeByURL(mAnimeUrl));
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
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(ThemeManager.getInstance().getAccentColorResId()));
        mSwipeRefreshLayout.setRefreshing(true);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
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

            // Create an empty adapter we will use to display the loaded data.
            mAdapter = new EpisodeRecyclerAdapter(getActivity(), itemClickListener);

            mRecyclerView.setAdapter(mAdapter);
        }

        //Restore previous state
        if (savedInstanceState == null) {
            // Prepare the loader.  Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(0, null, this);
        }
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
        }
        return super.onOptionsItemSelected(pItem);
    }

    @Override
    public Loader<Anime> onCreateLoader(int id, Bundle args) {
        Loader<Anime> loader = new AnimeLoader(getActivity(), mParser, mAnimeUrl);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Anime> loader, Anime temp) {
        if (temp != null) {
            mAdapter.setData(temp.getEpisodes());
        }
        if (temp == null && !NetworkUtils.isNetworkAvailable(getActivity()))
            Snackbar.make(mCoordinatorLayout, R.string.network_error, Snackbar.LENGTH_SHORT)
                    .setAction("View cached anime", new OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    })
                    .show();
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onLoaderReset(Loader<Anime> loader) {
        // Clear the data in the adapter.
        WriteLog.appendLog("onLoaderReset called");
        //mAdapter.setData(null);
    }
}