package com.taskdesignsinc.android.myanimeviewer.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnCloseListener;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.OfflineHistoryRecyclerAdapter;
import com.taskdesignsinc.android.myanimeviewer.model.OfflineHistoryRecord;
import com.taskdesignsinc.android.myanimeviewer.util.BuildUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.util.ArrayList;
import java.util.List;

import static com.taskdesignsinc.android.myanimeviewer.adapter.base.SelectableAdapter.MODE_MULTI;
import static com.taskdesignsinc.android.myanimeviewer.adapter.base.SelectableAdapter.MODE_SINGLE;

public class OfflineHistoryMaterialFragment extends Fragment
        implements OnQueryTextListener, OnCloseListener,
        LoaderManager.LoaderCallbacks<List<OfflineHistoryRecord>> {
    private static final String mTAG = OfflineHistoryMaterialFragment.class.getSimpleName();
    OfflineHistoryRecyclerAdapter mAdapter;
    private int mSortType = 0;
    // If non-null, this is the current filter the user has provided.
    String mCurFilter;
    // The SearchView for doing filtering.
    SearchView mSearchView;
    public ActionMode mMode;
    public static boolean mHasChanged = false;

    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    private ArrayList<OfflineHistoryRecord> mList;

    private OfflineHistoryRecyclerAdapter.OnItemClickListener mClickListener = new OfflineHistoryRecyclerAdapter.OnItemClickListener() {
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

    public void cancelActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
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
            mAdapter.setMode(MODE_MULTI);
            menu.add(R.string.favorite).setIcon(ThemeManager.getInstance(getActivity()).isLightBackground()
                    ? R.drawable.ic_favorite_border_black_24dp : R.drawable.ic_favorite_border_white_24dp);
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // image_detail_fragment.xml contains just an ImageView
        final View v = inflater.inflate(R.layout.anime_recycler_simple, container, false);
        if (!BuildUtils.isHoneycombOrLater())
            v.setBackgroundColor(ThemeManager.getInstance().getBackgroundColor(v.getContext()));
        mRecyclerView = (RecyclerView) v.findViewById(R.id.list);
        mLayoutManager = new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.list_num_cols));
        mRecyclerView.setLayoutManager(mLayoutManager);

        //mLeanBackView.setItemAnimator(new ReboundItemAnimator());
        //mRecyclerView.setItemAnimator(AnimatorManager.getInstance().getAnimator());
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        setHasOptionsMenu(true);

        if (activity != null) {
            mAdapter = new OfflineHistoryRecyclerAdapter(mList, getActivity(), mClickListener);
            mRecyclerView.setAdapter(mAdapter);
            //if (mAdapter == null)mAdapter = new HistoryRecyclerAdapter(mList)
            // Prepare the loader.  Either re-connect with an existing one,
            // or start a new one.
            getLoaderManager().initLoader(0, null, this);
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
    }

    protected void deleteHistory(int index) {
        MAVApplication.getInstance().getRepository().deleteOfflineHistoryRecord(mAdapter.getItem(index).getPath());
        //mAdapter.remove(mAdapter.getItem(index));
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Make sure that we are currently visible
        if (this.isVisible()) {
            // If we are becoming invisible, then...
            if (!isVisibleToUser) {
                Log.d(mTAG, "Not visible anymore.");
            } else {
                // If we are becoming visible, then...
                if (isVisibleToUser) {
                    Log.d(mTAG, "Is visible again.");
                }
            }
        }
    }

    public void refresh() {
        Activity activity = getActivity();

        if (activity != null) {
            if (mHasChanged) {
                WriteLog.appendLog("Refreshing Offline History");
                getLoaderManager().restartLoader(0, null, this);
                mHasChanged = false;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    private int mItemBaseID = 53900;

    public static class HistorySearchView extends SearchView {
        public HistorySearchView(Context context) {
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
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(0, mItemBaseID, 1, R.string.sort)
                .setIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_sort_black_24dp : R.drawable.ic_sort_white_24dp);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        menu.add(0, mItemBaseID + 1, 2, R.string.clear)
                .setIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_delete_black_24dp : R.drawable.ic_delete_white_24dp);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        MenuItem item = menu.add("Search History");
        item.setIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_search_black_24dp : R.drawable.ic_search_white_24dp);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
                | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        mSearchView = new HistorySearchView(getActivity());
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.setIconifiedByDefault(true);
        MenuItemCompat.setActionView(item, mSearchView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem pItem) {
        if (pItem.getTitle().equals(getString(R.string.sort))) {
            final CharSequence[] items = {"Newest First", "Oldest First"};

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.sort);
            builder.setSingleChoiceItems(items, mSortType, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    dialog.dismiss();
                    if (mSortType == item)
                        return;
                    switch (item) {
                        case 0:
                            mAdapter.sort(OfflineHistoryRecord.SortByTimeStampDESC);
                            break;
                        case 1:
                            mAdapter.sort(OfflineHistoryRecord.SortByTimeStampASC);
                            break;
                    }
                    mSortType = item;
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        } else if (pItem.getTitle().equals(getString(R.string.clear))) {
            MAVApplication.getInstance().getRepository().deleteOfflineHistoryRecords();
            mAdapter.clear();
            return true;
        }
        return super.onOptionsItemSelected(pItem);
    }

    @Override
    public Loader<List<OfflineHistoryRecord>> onCreateLoader(int arg0, Bundle arg1) {
        AsyncTaskLoader<List<OfflineHistoryRecord>> loader = new AsyncTaskLoader<List<OfflineHistoryRecord>>(getActivity()) {
            @Override
            public List<OfflineHistoryRecord> loadInBackground() {
                return MAVApplication.getInstance().getRepository().getOfflineHistoryRecords();
            }
        };
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.
        loader.forceLoad();
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<List<OfflineHistoryRecord>> arg0,
                               List<OfflineHistoryRecord> data) {
        // Set the new data in the adapter.
        mAdapter.addLibraryRecordList(data);
        mAdapter.getFilter().filter(null);

    }

    @Override
    public void onLoaderReset(Loader<List<OfflineHistoryRecord>> arg0) {

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
}