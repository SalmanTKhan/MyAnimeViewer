package com.taskdesignsinc.android.myanimeviewer.fragment;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
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
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnCloseListener;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.AnimeRecyclerAdapter;
import com.taskdesignsinc.android.myanimeviewer.adapter.FavoriteListAdapter;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.OnItemClickListener;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteRecord;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteTag;
import com.taskdesignsinc.android.myanimeviewer.model.helper.EpisodeUtils;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.service.ManualAnimeUpdaterService;
import com.taskdesignsinc.android.myanimeviewer.service.ParseAnimeService;
import com.taskdesignsinc.android.myanimeviewer.util.BuildUtils;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.StorageUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.myanimeviewer.view.GridRecyclerView;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.taskdesignsinc.android.myanimeviewer.adapter.base.SelectableAdapter.MODE_MULTI;
import static com.taskdesignsinc.android.myanimeviewer.adapter.base.SelectableAdapter.MODE_SINGLE;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class FavoritesFragment extends Fragment
        implements OnQueryTextListener, OnCloseListener,
        LoaderManager.LoaderCallbacks<List<Anime>> {

    public static final String mTag = FavoritesFragment.class.getSimpleName();

    // Model
    ArrayList<Anime> mList;

    // View
    // The SearchView for doing filtering.
    SearchView mSearchView;
    CoordinatorLayout mCoordinatorLayout;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    private boolean mIsGrid = true;
    private int mGridColumnCount;
    private int mListColumnCount;
    int mSortType = 0;

    private SharedPreferences mPrefs;

    private int mDisplayType;

    public static boolean mHasChanged = false;

    CharSequence[] mFilters;
    boolean[] mFiltersChecked;

    private GridRecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // Controller
    // This is the Adapter being used to display the list's data.
    private AnimeRecyclerAdapter mAdapter;

    private OnItemClickListener mAnimeClickListener = new OnItemClickListener() {
        @Override
        public boolean onListItemClick(int position) {
            if (mActionMode != null && position != INVALID_POSITION) {
                if (mSelectionMode == Constants.SELECT_MULTIPLE) {
                    if (mSelectedLowerBound == -1 || mSelectedUpperBound > position) {
                        mSelectedLowerBound = position;
                        toggleSelection(position);
                    } else if (mSelectedUpperBound == -1 || mSelectedUpperBound < position) {
                        if (position < mSelectedLowerBound) {
                            mSelectedUpperBound = mSelectedLowerBound;
                            mSelectedLowerBound = position;
                        } else
                            mSelectedUpperBound = position;
                    }
                    if (mSelectedLowerBound != -1 && mSelectedUpperBound != -1) {
                        for (int i = mSelectedLowerBound; i <= mSelectedUpperBound; i++) {
                            toggleSelection(i);
                        }
                        mAdapter.notifyItemRangeChanged(mSelectedLowerBound, mSelectedUpperBound - mSelectedLowerBound);
                    }
                } else {
                    toggleSelection(position);
                }
                return true;
            } else {
                //Notify the active callbacks interface (the activity, if the
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
     * Note that the selection must alviewedy be started (actionMode must not be null).
     *
     * @param position Position of the item to toggle the selection state
     */
    private void toggleSelection(int position) {
        if (mAdapter != null) {
            mAdapter.toggleSelection(position, false);

            int count = mAdapter.getSelectedItemCount();

            if (mActionMode != null) {
                if (count == 0) {
                    mActionMode.setTitle("");
                    //mActionMode.finish();
                } else {
                    setContextTitle(count);
                    mActionMode.invalidate();
                }
            }
        }
    }

    private void setContextTitle(int count) {
        mActionMode.setTitle(String.valueOf(count) + " " + (count == 1 ?
                getString(R.string.action_selected_one) :
                getString(R.string.action_selected_many)));
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
        if (mAdapter != null)
            mAdapter.onSaveInstanceState(outState);
        if (mActivatedPosition != AdapterView.INVALID_POSITION) {
            //Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mPrefs == null)
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        try {
            mDisplayType = Integer.parseInt(mPrefs.getString(Constants.KEY_FAVORITES_DISPLAY_TYPE, "0"));
        } catch (ClassCastException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
            mDisplayType = 0;
        }
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // image_detail_fragment.xml contains just an ImageView
        final View v = inflater.inflate(R.layout.anime_recycler_layout, container, false);
        if (!BuildUtils.isHoneycombOrLater())
            v.setBackgroundColor(ThemeManager.getInstance().getBackgroundColor(v.getContext()));
        mRecyclerView = (GridRecyclerView) v.findViewById(R.id.list);

        if (mRecyclerView != null) {
            mRecyclerView.setPopupTextColor(ThemeManager.getInstance().getTextColor());
            mRecyclerView.setPopupBgColor(ThemeManager.getInstance().getPrimaryColor(v.getContext()));
            mRecyclerView.setThumbColor(ThemeManager.getInstance().getPrimaryDarkColor(v.getContext()));
            mRecyclerView.setTrackColor(ThemeManager.getInstance().getAccentColor(v.getContext()));
        }
        //mRecyclerView.setItemAnimator(AnimatorManager.getInstance().getAnimator());
        mSwipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(ThemeManager.getInstance().getAccentColorResId()));
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    updateFavorites();
                }
            });
        }

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

    private void updateFavorites() {
        if (mAdapter != null) {
            Activity activity = getActivity();
            if (activity != null) {
                for (int i = 0; i < mAdapter.getItemCount(); i++) {
                    if (TextUtils.isEmpty(mAdapter.getItem(i).getUrl()))
                        continue;
                    if (mAdapter.getItem(i).getStatus() == 1)
                        continue;
                    Intent msgIntent = new Intent(activity,
                            ManualAnimeUpdaterService.class);
                    msgIntent.putExtra(Constants.ANIME_URL, mAdapter.getItem(i).getUrl());
                    activity.startService(msgIntent);
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            mList = new ArrayList<>();
            mCoordinatorLayout = (CoordinatorLayout) activity.findViewById(R.id.coordinator_layout);
            mSortType = mPrefs.getInt(Constants.KEY_FAVORITES_SORT, 0);
            mIsGrid = mDisplayType == 0;
            mGridColumnCount = mPrefs.getInt(Constants.KEY_GRID_COLUMN_COUNT, getResources().getInteger(R.integer.grid_num_cols));
            mListColumnCount = mPrefs.getInt(Constants.KEY_LIST_COLUMN_COUNT, getResources().getInteger(R.integer.list_num_cols));

            mLayoutManager = new GridLayoutManager(getActivity(), mIsGrid ? mGridColumnCount : mListColumnCount);
            mRecyclerView.setLayoutManager(mLayoutManager);
            // Create an empty adapter we will use to display the loaded data.
            mAdapter = new AnimeRecyclerAdapter(mList, mDisplayType, getActivity(), true, mAnimeClickListener);
            mAdapter.setRecyclerView(mRecyclerView);
            mRecyclerView.setAdapter(mAdapter);

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

    public static class MySearchView extends SearchView {
        public MySearchView(Context context) {
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
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        int mItemBaseID = 9600;
        menu.clear();
        menu.add(Menu.NONE, mItemBaseID, 1, R.string.sort)
                .setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_sort_by_alpha_black_24dp : R.drawable.ic_sort_by_alpha_white_24dp);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, mItemBaseID + 1, 2, R.string.update)
                .setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_refresh_black_24dp : R.drawable.ic_refresh_white_24dp);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, mItemBaseID + 2, 3, R.string.filter)
                .setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_filter_list_black_24dp : R.drawable.ic_filter_list_white_24dp);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, mItemBaseID, 0, mDisplayType != Constants.DISPLAY_TYPE_LIST_LARGE ? "List" : "Grid").setIcon(mDisplayType != Constants.DISPLAY_TYPE_LIST_LARGE ? R.drawable.ic_view_list_white_24dp : R.drawable.ic_view_module_white_24dp);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, mItemBaseID, 0, "Columns").setIcon(R.drawable.ic_view_column_white_24dp);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        //menu.add(Menu.NONE, mItemBaseID + 3, 3, R.string.customize_tags);
        //MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_NEVER);
        //menu.add(Menu.NONE, mItemBaseID + 4, 4, R.string.import_favorites);
        //MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_NEVER);
        //menu.add(Menu.NONE, mItemBaseID + 5, 5, R.string.export_favorites);
        //MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_NEVER);
        //menu.add(Menu.NONE, mItemBaseID + 4, 5, R.string.filter).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        //menu.add(Menu.NONE, mItemBaseID + 13, 13, R.string.clear_all_favorites);
        //MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem pItem) {
        if (pItem != null && !TextUtils.isEmpty(pItem.getTitle()))
            if (pItem.getTitle().equals("List")) {
                mDisplayType++;
                mIsGrid = false;
                mAdapter.setDisplayType(mDisplayType);
                ((GridLayoutManager) mLayoutManager).setSpanCount(mListColumnCount);
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setAdapter(mAdapter);
                return true;
            } else if (pItem.getTitle().equals("Grid")) {
                mIsGrid = true;
                mDisplayType = 0;
                mAdapter.setDisplayType(Constants.DISPLAY_TYPE_GRID);
                ((GridLayoutManager) mLayoutManager).setSpanCount(mGridColumnCount);
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setAdapter(mAdapter);
                return true;
            } else if (pItem.getTitle().equals(getString(R.string.sort))) {
                String[] lItems = getResources().getStringArray(R.array.favorite_sort_list);
                List<FavoriteTag> tags = MAVApplication.getInstance().getRepository().getFavoriteTags();
                Collections.sort(tags, FavoriteTag.Order.ByTagIdLowToHigh);
                FavoriteTag highestTag = tags.get(tags.size()-1);
                FavoriteTag lowestTag = tags.get(0);
                lItems[2] = highestTag.getTitle() +
                        " to " + lowestTag.getTitle();
                lItems[3] = lowestTag.getTitle() +
                        " to " + highestTag.getTitle();
                Builder builder = new Builder(getActivity());
                builder.setTitle("Sort By");
                builder.setSingleChoiceItems(lItems, mSortType, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        dialog.dismiss();
                        if (mSortType == item)
                            return;
                        switch (item) {
                            case 0:
                                mAdapter.sort(Anime.Order.ByNameAZ);
                                mAdapter.setSectionType(1);
                                break;
                            case 1:
                                mAdapter.sort(Collections.reverseOrder(Anime.Order.ByNameAZ));
                                mAdapter.setSectionType(1);
                                break;
                            case 2:
                                mAdapter.sort(Anime.Order.ByTagIdL2H_AZ);
                                mAdapter.setSectionType(2);
                                break;
                            case 3:
                                mAdapter.sort(Anime.Order.ByTagIdH2L_AZ);
                                mAdapter.setSectionType(2);
                                break;
                            case 4:
                                //mAdapter.sort(Anime.Order.ByLastUpdate);
                                //mAdapter.setSectionType(1);
                                break;
                        }
                        mSortType = item;
                        mPrefs.edit().putInt(Constants.KEY_FAVORITES_SORT, mSortType).apply();
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            } else if (pItem.getTitle().equals(getString(R.string.filter))) {
                Builder ListDialog = new Builder(getActivity());
                ListDialog.setTitle("Filter Favorites");
                ListDialog.setMultiChoiceItems(mFilters, mFiltersChecked, new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        //Toast.makeText(getActivity(), mFilters[which] + (isChecked ? "checked!" : "unchecked!"), Toast.LENGTH_SHORT).show();
                    }
                });
                ListDialog.setPositiveButton("OK", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String lFilters = "";
                        String lSourceFilters = "";
                        boolean lIsSourcesChecked = false;
                        List<FavoriteTag> lTagList = MAVApplication.getInstance().getRepository().getFavoriteTags();
                        for (int i = 0; i < mFilters.length; i++) {
                            if (mFiltersChecked[i]) {
                                if (i < lTagList.size())
                                    lFilters += lTagList.get(i).getTagId() + " ";
                                else if (i == lTagList.size())
                                    lFilters += -1 + " ";
                                else {
                                    lSourceFilters += mFilters[i] + " ";
                                    lIsSourcesChecked = true;
                                }
                            }
                        }
                        if (!lIsSourcesChecked)
                            lFilters += "All ";
                        else
                            lFilters += lSourceFilters;
                        //Toast.makeText(getActivity(), lFilters, Toast.LENGTH_SHORT).show();
                        mAdapter.setFilterType(1);
                        mAdapter.getFilter().filter(lFilters);
                    }
                });
                ListDialog.setNegativeButton("Cancel", new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                ListDialog.show();
                return true;
            } else if (pItem.getTitle().equals(getString(R.string.update))) {
                updateFavorites();
                return true;
            } else if (pItem.getTitle().equals(getString(R.string.clear_all_favorites))) {
                Builder lBuilder = new Builder(getActivity());
                lBuilder.setTitle(R.string.clear_all_favorites);
                lBuilder.setMessage(R.string.confirmation_msg);
                lBuilder.setPositiveButton(R.string.ok, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.dismiss();
                        getActivity().stopService(new Intent(getActivity(),
                                ParseAnimeService.class));
                        if (MAVApplication.getInstance().getRepository().deleteFavoriteRecords()) {
                            if (mAdapter != null) {
                                mAdapter.clearData();
                            }
                        }
                    }
                });
                lBuilder.setNegativeButton(R.string.cancel, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        dialog.cancel();
                    }
                });
                lBuilder.show();
            } else if (pItem.getTitle().equals(getString(R.string.customize_tags))) {
                Builder builder = new Builder(getActivity());
                builder.setTitle("Select Favorite Tag");
                ListView lv = new ListView(getActivity());
                final FavoriteListAdapter lAdapter = new FavoriteListAdapter(getActivity(), true);
                lAdapter.add(new FavoriteTag(-1, "Add New", -1));
                lv.setAdapter(lAdapter);
                builder.setView(lv);
                builder.setNegativeButton(R.string.cancel, new OnClickListener() {
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
                        // Create the fragment and show it as a dialog.
                        if (getFragmentManager().findFragmentByTag("tagDialog") == null) {
                            //DialogFragment newFragment = EditTagDialog.newInstance(lAdapter.getItem(arg2).getTagId(), lAdapter);
                            //newFragment.show(getFragmentManager(), "tagDialog");
                        }
                    }
                });
                alert.show();
                return true;
            } else if (pItem.getTitle().equals("Find Alternate Sources")) {
                //showSourceList();
                return true;
            }
        return super.onOptionsItemSelected(pItem);
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

    private void updateItemAtPosition(int position) {
        if (position == -1)
            return;
    }

    private int mSelectionMode = Constants.SELECT_SINGLE;
    private int mSelectedLowerBound = -1;
    private int mSelectedUpperBound = -1;

    @SuppressLint("NewApi")
    private final class RecordOptions implements ActionMode.Callback {
        AppCompatActivity mContext;
        Boolean mIsAllSelected = false;

        public RecordOptions(AppCompatActivity context) {
            mContext = context;
        }

        @SuppressLint("NewApi")
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mAdapter.setMode(MODE_MULTI);
            if (BuildUtils.isLollipopOrLater()) {
                mContext.getWindow().setStatusBarColor(getResources().getColor(ThemeManager.getInstance(mContext).getAccentColorResId()));
            }
            menu.add(R.string.favorite).setIcon(ThemeManager.getInstance(getActivity()).isLightBackground()
                    ? R.drawable.ic_favorite_border_black_24dp : R.drawable.ic_favorite_border_white_24dp);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            menu.add(R.string.mark_unviewed);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            menu.add(R.string.select_mode).setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_select_all_black_24dp : R.drawable.ic_select_all_white_24dp);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            menu.add(R.string.download)
                    .setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_download_white_24dp);
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
            if (item.getTitle().equals(getString(R.string.select_mode))) {
                Builder builder = new Builder(
                        getActivity());
                builder.setTitle("Selection Mode");
                builder.setSingleChoiceItems(Constants.SELECTION_CHOICES, mSelectionMode, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface pDialog, int pWhich) {
                        if (mSelectionMode != pWhich) {
                            mSelectionMode = pWhich;
                            if (pWhich == Constants.SELECT_ALL) {
                                mIsAllSelected = !mIsAllSelected;
                                if (mIsAllSelected) {
                                    WriteLog.appendLog("Selecting All:  total count "
                                            + mAdapter.getItemCount());
                                    mAdapter.selectAll();
                                    setContextTitle(mAdapter.getSelectedItemCount());
                                }
                                mSelectionMode = Constants.SELECT_SINGLE;
                            }
                        }
                        pDialog.dismiss();
                    }
                });
                final AlertDialog alert = builder.create();
                alert.show();
            } else if (item.getTitle().toString().equals(mContext.getString(R.string.favorite))) {
                Builder builder = new Builder(getActivity());
                builder.setTitle("Change Favorite Tag");
                ListView lv = new ListView(getActivity());
                FavoriteListAdapter lAdapter = new FavoriteListAdapter(mContext);
                lAdapter.add(new FavoriteTag(-1, "Remove", -1));
                final FavoriteListAdapter adapter = lAdapter;
                lv.setAdapter(lAdapter);
                builder.setView(lv);
                builder.setNegativeButton(R.string.cancel, new OnClickListener() {
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
                                if (lAnime != null) {
                                    if (arg2 < lList.size()) {
                                        MAVApplication.getInstance().getRepository().insertFavorite(lAnime.getUrl(), adapter.getItem(arg2).getTagId());
                                        lAnime.setTagId(adapter.getItem(arg2).getTagId());
                                    } else {
                                        MAVApplication.getInstance().getRepository().deleteFavorite(lAnime.getUrl());
                                        lAnime.setTagId(-1);
                                        mAdapter.removeItem(i);
                                    }
                                } else {
                                    WriteLog.appendLog(mTag, "changeFavoriteTag anime is null");
                                }
                            }
                            mAdapter.notifyDataSetChanged();
                        }
                        mode.finish();
                        refresh();
                    }
                });
                alert.show();
            } else if (item.getTitle().equals(getString(R.string.mark_unviewed))) {
                Anime lAnime = null;
                ArrayList<Anime> list = new ArrayList<>();
                for (int i : mAdapter.getSelectedItems()) {
                    lAnime = mAdapter.getItem(i);
                    lAnime.setNewEpisodes(0);
                    list.add(lAnime);
                }
                MAVApplication.getInstance().getRepository().updateAnimeList(list);
                mAdapter.notifyDataSetChanged();
                refresh();
                mode.finish();
            } else if (item.getTitle().toString().equals(getString(R.string.download))) {
                MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
                builder.title("Download Episode(s)")
                        .positiveText(R.string.yes)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();

                                //int count = mAnime.getEpisodes().size();
                                for (int i : mAdapter.getSelectedItems()) {
                                    EpisodeUtils.downloadEpisodes(getActivity(), mAdapter.getItem(i));
                                }
                                mode.finish();
                            }
                        })
                        .negativeText(R.string.no)
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.cancel();
                            }
                        });
                final MaterialDialog alert = builder.build();
                alert.show();
            } else {
                Toast.makeText(getActivity(),
                        "Got click: " + item.getTitle().toString(),
                        Toast.LENGTH_SHORT).show();
                mode.finish();
            }
            return true;
        }

        @SuppressLint("NewApi")
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            if (BuildUtils.isLollipopOrLater()) {
                mContext.getWindow().setStatusBarColor(getResources().getColor(ThemeManager.getInstance(mContext).getPrimaryDarkColorResId()));
            }
            mActionMode = null;
            mSelectedLowerBound = -1;
            mSelectedUpperBound = -1;
            mAdapter.setMode(MODE_SINGLE);
            mAdapter.clearSelection();
        }
    }

    @Override
    public Loader<List<Anime>> onCreateLoader(int id, Bundle args) {
        AsyncTaskLoader<List<Anime>> loader = new AsyncTaskLoader<List<Anime>>(getActivity()) {

            @Override
            public List<Anime> loadInBackground() {
                ArrayList<Anime> lFavoritesList = new ArrayList<>();

                Anime lAnime = null;
                List<FavoriteRecord> lList = MAVApplication.getInstance().getRepository().getFavorites();
                for (FavoriteRecord lRecord : lList) {
                    lAnime = MAVApplication.getInstance().getRepository().getAnimeByUrl(lRecord.getAnimeUrl());
                    if (lAnime != null) {
                        lAnime.setTagId(lRecord.getTagId());
                        if (lAnime.getStatus() != Constants.ANIME_COMPLETED) {
                            /*
                            Episode lEpisode = AnimeHelper.getInstance(getContext()).getLatestEpisode(lAnime.getId());
							if (lEpisode != null) {
								// Too slow to calculate every time
								if (!TextUtils.isEmpty(lEpisode.getDate())) {
									Parser lParser = Parser.getExistingInstance(lAnime.getUrl());
									Date lEpisodeDate = lParser.getDateFormat(lEpisode.getDate());
									if (lEpisodeDate != null) {
										Calendar lCalendar = Calendar.getInstance();
										lCalendar.setTime(lEpisodeDate);
										long dateInMillisecs = lCalendar.getTimeInMillis();
										lCalendar.add(Calendar.DAY_OF_MONTH, -7);
										long difference = dateInMillisecs - lCalendar.getTimeInMillis();
										long differenceInDays = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS);
										int maxDiff = Integer.parseInt(mPrefs.getString(Constants.KEY_CHAPTER_MAX_WEEK_DIFF, "7"));
										if (differenceInDays < maxDiff)
											lAnime.setUnviewedEpisodes(lAnime.getEpisodeCount() -
													MAVApplication.getInstance().getRepository().getEpisodeStatusCount(lAnime.getUrl(), true));
									}
								} else {
									lAnime.setUnviewedEpisodes(lAnime.getEpisodeCount() -
											MAVApplication.getInstance().getRepository().getEpisodeStatusCount(lAnime.getUrl(), true));
								}
							}
							*/
                        }
                        lFavoritesList.add(lAnime);
                    } else {
                        Intent msgIntent = new Intent(getContext(),
                                ParseAnimeService.class);
                        msgIntent.putExtra(Constants.ANIME_URL, lRecord.getAnimeUrl());
                        getContext().startService(msgIntent);
                    }
                }

                SparseArray<String> lPossibleSources = new SparseArray<String>();
                SparseArray<String> lPossibleGenres = new SparseArray<String>();
                List<FavoriteTag> lTagList = MAVApplication.getInstance().getRepository().getFavoriteTags();
                int j = 0;
                for (int i = 0; i < lFavoritesList.size(); i++) {
                    if (lPossibleSources.indexOfValue(Parser.getNameByUrl(lFavoritesList.get(i).getUrl())) >= 0)
                        continue;
                    lPossibleSources.put(lTagList.size() + 1 + j, Parser.getNameByUrl(lFavoritesList.get(i).getUrl()));
                    j++;
                }
                if (mFilters == null || mFilters.length < lTagList.size() + 1 + lPossibleSources.size())
                    mFilters = new CharSequence[lTagList.size() + 1 + lPossibleSources.size()];
                for (int i = 0; i < mFilters.length; i++) {
                    if (i < lTagList.size())
                        mFilters[i] = lTagList.get(i).getTitle();
                    else if (i == lTagList.size())
                        mFilters[i] = "Unsorted";
                    else {
                        if (lPossibleSources.get(i) == null)
                            continue;
                        mFilters[i] = lPossibleSources.get(i);
                    }
                }
                if (mFiltersChecked == null || mFiltersChecked.length < mFilters.length) {
                    mFiltersChecked = new boolean[mFilters.length];
                    for (int i = 0; i < mFiltersChecked.length; i++) {
                        mFiltersChecked[i] = true;
                    }
                }

                // Done!
                return lFavoritesList;
            }
        };
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.
        loader.forceLoad();
        return loader;
    }

    private void sortFavorites() {
        switch (mSortType) {
            case 0:
                mAdapter.setSectionType(1);
                mAdapter.sort(Anime.Order.ByNameAZ);
                break;
            case 1:
                mAdapter.setSectionType(1);
                mAdapter.sort(Anime.Order.ByNameZA);
                break;
            case 2:
                mAdapter.setSectionType(2);
                mAdapter.sort(Anime.Order.ByTagIdH2L_AZ);
                break;
            case 3:
                mAdapter.setSectionType(2);
                mAdapter.sort(Anime.Order.ByTagIdL2H_AZ);
                break;
            case 4:
                mAdapter.setSectionType(1);
                mAdapter.sort(Anime.Order.ByLastUpdate);
                break;
        }
    }

    @Override
    public void onLoadFinished(Loader<List<Anime>> loader, List<Anime> data) {
        if (data != null && !data.isEmpty() && mAdapter.getItemCount() > 0) {
            mAdapter.clearData();
        }
        mList = (ArrayList) data;
        mAdapter.setSectionType(1);
        mAdapter.addAnimeList(data);
        sortFavorites();
        //mAdapter.getFilter().filter(null);
    }

    @Override
    public void onLoaderReset(Loader<List<Anime>> loader) {
        // Clear the data in the adapter.
        WriteLog.appendLog("onLoaderReset called");
        mAdapter.clearData();
    }

    private void restoreFavorites(final String pFileName, Boolean pCleanImport) {
        WriteLog.appendLog("restoreFavorites(" + pFileName + ", " + pCleanImport + ") called");
        Bundle args = new Bundle();
        args.putString("fileName", pFileName);
        args.putBoolean("cleanImport", pCleanImport);
        getLoaderManager().restartLoader(1, args, this);
    }


    private void backupFavorites(final String pFileName) {
        try {
            List<FavoriteRecord> favoriteRecords = MAVApplication.getInstance().getRepository()
                    .getFavorites();
            List<FavoriteTag> lTags = MAVApplication.getInstance().getRepository()
                    .getFavoriteTags();
            Gson gson = new Gson();
            String pTagFile = pFileName.replace(".json", "") + "_" + "TAGs" + ".json";
            FileWriter writer = new FileWriter(pTagFile);
            String json = gson.toJson(lTags);
            writer.write(json);
            writer.close();

            writer = new FileWriter(pFileName);
            json = gson.toJson(favoriteRecords);
            writer.write(json);
            writer.close();

            new Builder(getActivity())
                    .setTitle(R.string.export_favorites)
                    .setMessage("Backup successful, located at:\n" + pFileName)
                    .setPositiveButton(R.string.ok,
                            new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                }
                            }).show();
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
            e.printStackTrace();
            new Builder(getActivity())
                    .setTitle(R.string.export_favorites)
                    .setMessage("Backup failed" + pFileName)
                    .setPositiveButton("Close",
                            new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int whichButton) {
                                }
                            }).show();
        }
    }

    public void refresh() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        if (activity != null) {
            if (mHasChanged) {
                //WriteLog.appendLog("Refreshing Favorites");
                getLoaderManager().restartLoader(0, null, this);
                mHasChanged = false;
            }
        }
    }

    public void refresh(final String animeUrl) {
        WriteLog.appendLog(mTag, "Refreshing Favorites");
        if (mAdapter != null) {
            //mAdapter.checkEpisodeStatus(pEpisodeURL);
            int pos = mAdapter.getItemPosition(animeUrl);
            if (pos != -1) {
                mAdapter.notifyItemChanged(pos);
            } else {
                mAdapter.addItem(MAVApplication.getInstance().getRepository().getAnimeByUrl(animeUrl));
            }
        }
    }
}
