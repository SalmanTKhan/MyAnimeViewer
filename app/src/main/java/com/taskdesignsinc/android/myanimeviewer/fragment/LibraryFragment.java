
package com.taskdesignsinc.android.myanimeviewer.fragment;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnCloseListener;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.LibraryRecyclerAdapter;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.OnItemClickListener;
import com.taskdesignsinc.android.myanimeviewer.recyclerview.animator.SlideInLeftAnimator;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.helper.EpisodeUtils;
import com.taskdesignsinc.android.myanimeviewer.model.helper.AnimeUtils;
import com.taskdesignsinc.android.myanimeviewer.util.BuildUtils;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.FileUtils;
import com.taskdesignsinc.android.myanimeviewer.util.StorageUtils;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.myanimeviewer.widget.ControllableAppBarLayout;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import static com.taskdesignsinc.android.myanimeviewer.adapter.base.SelectableAdapter.MODE_SINGLE;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LibraryFragment extends Fragment
        implements OnQueryTextListener, OnCloseListener,
        LoaderManager.LoaderCallbacks<List<File>> {

    public static final String TAG = LibraryFragment.class.getSimpleName();

    // Model
    List<File> mList;

    // View
    // The SearchView for doing filtering.
    SearchView mSearchView;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    private String mPath;

    int mSortType = 0;

    private SharedPreferences mPrefs;

    public ActionMode mMode;

    boolean mIsViewing = false;

    private int mDisplayType;
    private Stack<String> mPrevPath;

    Anime mAnime = null;
    HashMap<String, Episode> mEpisodes = null;
    HashMap<String, Anime> mAnimeMap = null;
    public String mLastEpisodeDownloaded = "";

    /**
     * Create a new instance of LibraryFragment, initialized to show the text at
     * 'index'.
     */
    public static LibraryFragment newInstance(String pAnimePath) {
        LibraryFragment f = new LibraryFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putString(Constants.EPISODE_VIDEO_URL, pAnimePath);
        f.setArguments(args);

        return f;
    }

    CoordinatorLayout mCoordinatorLayout;
    ControllableAppBarLayout mAppBarLayout;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;

    // Controller
    // This is the Adapter being used to display the list's data.
    private LibraryRecyclerAdapter mAdapter;

    private int mSelectionMode = Constants.SELECT_SINGLE;
    private int mSelectedLowerBound = -1;
    private int mSelectedUpperBound = -1;

    private OnItemClickListener mLibraryClickListener = new OnItemClickListener() {
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
                mAppBarLayout.expandToolbar();
                File lFile = mAdapter.getItem(position);
                if (lFile != null) {
                    if (lFile.isDirectory()) {
                        FileFilter fileFilter = new FileFilter() {
                            @Override
                            public boolean accept(File file) {
                                return !file.isDirectory();
                            }
                        };
                        FileFilter dirFilter = new FileFilter() {
                            @Override
                            public boolean accept(File file) {
                                return file.isDirectory();
                            }
                        };
                        File lTempFiles[] = lFile.listFiles(fileFilter);
                        boolean lImagesOnly = true;
                        String lExt = "";
                        if (lTempFiles != null)
                            for (int i = 0; i < lTempFiles.length; i++) {
                                lExt = FileUtils.getFileExtension(lTempFiles[i].getAbsolutePath());
                                if (!FileUtils.isImage(lExt) && !lTempFiles[i].getName().contains(".png")) {
                                    lImagesOnly = false;
                                    break;
                                }
                            }
                        lTempFiles = lFile.listFiles(dirFilter);
                        File pAnimeFile = new File(lFile, "anime.json");
                        boolean lAnimeDataExists = (pAnimeFile != null && pAnimeFile.exists()) ? true : false;
                        boolean lShowCovers = mPrefs.getBoolean(Constants.KEY_LIBRARY_SHOW_COVERS, false);
                        if (lAnimeDataExists) {
                            viewOfflineAnime(lFile.getAbsolutePath());
                            return false;
                        }
                        if ((lTempFiles != null && lTempFiles.length != 0) || !lImagesOnly) {
                            WriteLog.appendLog("Last folder path: " + mPath);
                            mPrevPath.push(mPath);
                            mPath = lFile.getAbsolutePath();
                            WriteLog.appendLog("New folder path: " + mPath);
                            mAdapter.addFileList(null, mPrevPath.size());
                            getLoaderManager().restartLoader(0, null, LibraryFragment.this);
                        } else {
                            fileFilter = new FileFilter() {
                                @Override
                                public boolean accept(File file) {
                                    return !file.isDirectory();
                                }
                            };
                            lTempFiles = lFile.listFiles(fileFilter);
                            mPath = lFile.getAbsolutePath();
                            viewPath(mPath);
                        }
                    } else {
                        mPath = lFile.getAbsolutePath();
                        viewPath(mPath);
                    }
                }
                return false;
            }
        }

        @Override
        public void onListItemLongClick(int position) {
            if (mActionMode == null) {
                mActionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(new RecordOptions((AppCompatActivity) getActivity()));
            }
            //toggleSelection(position);
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
     * <p/>
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
                    //mActionMode.finish();
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
        outState.putString(Constants.ANIME_PATH, mPath);
        if (!mPrevPath.empty()) {
            ArrayList<String> copy = new ArrayList<String>(mPrevPath);
            outState.putStringArrayList(Constants.ANIME_PREV_PATH_LIST, copy);
        }
        if (mAdapter != null)
            mAdapter.onSaveInstanceState(outState);

        if (mActivatedPosition != AdapterView.INVALID_POSITION) {
            //Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
            Log.d(TAG, STATE_ACTIVATED_POSITION + "=" + mActivatedPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mPath = getArguments() != null ? getArguments().getString(Constants.ANIME_PATH) : "";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // image_detail_fragment.xml contains just an ImageView
        final View v = inflater.inflate(R.layout.anime_recycler_layout, container, false);
        if (!BuildUtils.isHoneycombOrLater())
            v.setBackgroundColor(ThemeManager.getInstance().getBackgroundColor(v.getContext()));
        mRecyclerView = (RecyclerView) v.findViewById(R.id.list);
        //mLeanBackView.setLayoutManager(new LinearLayoutManager(getActivity()));
        if (mDisplayType < 2)
            mLayoutManager = new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.grid_num_cols));
        else {
            mLayoutManager = new LinearLayoutManager(getActivity());
        }
        mRecyclerView.setLayoutManager(mLayoutManager);

        //mLeanBackView.setItemAnimator(new ReboundItemAnimator());
        mRecyclerView.setItemAnimator(new SlideInLeftAnimator());

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
            mCoordinatorLayout = (CoordinatorLayout) activity.findViewById(R.id.coordinator_layout);
            mAppBarLayout = (ControllableAppBarLayout) activity.findViewById(R.id.appbar_layout);
            if (mPrefs == null)
                mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
            mDisplayType = Integer.parseInt(mPrefs.getString(Constants.KEY_LIBRARY_DISPLAY_TYPE, "0"));
            mAnimeMap = new HashMap<String, Anime>();
            mPrevPath = new Stack<String>();
            if (TextUtils.isEmpty(mPath)) {
                if (StorageUtils.getDataDirectory() != null)
                    mPath = StorageUtils.getDataDirectory().getAbsolutePath();
                else
                    mPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            }
            // Create an empty adapter we will use to display the loaded data.
            mAdapter = new LibraryRecyclerAdapter(getActivity(), mDisplayType, mLibraryClickListener);
            mRecyclerView.setAdapter(mAdapter);
        }

        //Restore previous state
        if (savedInstanceState != null) {
            ArrayList<String> mPrevPathList = savedInstanceState.getStringArrayList(Constants.ANIME_PREV_PATH_LIST);
            if (mPrevPathList != null && !mPrevPathList.isEmpty() && mPrevPath.size() != mPrevPathList.size()) {
                for (String s : mPrevPathList) {
                    mPrevPath.push(s);
                }
            }
            if (mAdapter != null) {
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

    private int mItemBaseID = 9700;

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        menu.clear();
        // Place an action bar item for searching.
        MenuItem item = menu.add(R.string.search);
        if (ThemeManager.getInstance().isLightBackground())
            item.setIcon(R.drawable.ic_search_black_24dp);
        else
            item.setIcon(R.drawable.ic_search_white_24dp);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
                | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        mSearchView = new MySearchView(getActivity());
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.setIconifiedByDefault(true);
        MenuItemCompat.setActionView(item, mSearchView);
        //menu.add(Menu.NONE, mItemBaseID, 0, R.string.open).setIcon(ThemeManager.getInstance().getCurrentTheme() == ThemeType.Light ? R.drawable.open_light : R.drawable.open_dark).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, mItemBaseID + 1, 1, R.string.sort)
                .setIcon(ThemeManager.getInstance(getActivity()).isLightBackground() ? R.drawable.ic_sort_by_alpha_black_24dp : R.drawable.ic_sort_by_alpha_white_24dp);
        MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        //menu.add(R.string.search_anime).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem pItem) {
        if (pItem.getTitle().equals("Sort")) {
            final CharSequence[] items = {"Title A-Z", "Title Z-A"};

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Sort By");
            builder.setSingleChoiceItems(items, mSortType, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    dialog.dismiss();
                    switch (item) {
                        case 0:
                            mAdapter.sort(new FileUtils.SortByFileName());
                            break;
                        case 1:
                            mAdapter.sort(Collections.reverseOrder(new FileUtils.SortByFileName()));
                            break;
                    }
                    mSortType = item;
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        } else if (pItem.getTitle().equals("Find Current Anime")) {
            if (mAnime != null) {
                viewAnime(mAnime.getUrl());
            } else {
                Toast.makeText(getActivity(), "No anime data found", Toast.LENGTH_SHORT).show();
                WriteLog.appendLog("No anime data found");
            }
            return true;
        }
        return super.onOptionsItemSelected(pItem);
    }

    public void viewAnime(String animeUrl) {
        AnimeUtils.viewAnime(getActivity(), animeUrl);
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private final class RecordOptions implements ActionMode.Callback {
        AppCompatActivity mContext;

        public RecordOptions(AppCompatActivity context) {
            mContext = context;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mAdapter.setMode(LibraryRecyclerAdapter.MODE_MULTI);
            menu.add(R.string.delete).setIcon(android.R.drawable.ic_menu_delete);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            menu.add(R.string.mark_viewed);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            menu.add(R.string.mark_unviewed);
            MenuItemCompat.setShowAsAction(menu.getItem(menu.size() - 1), MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
            menu.add(R.string.rename);
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
            if (item.getTitle().toString().equals(getString(R.string.delete))) {
                if (mAdapter != null) {
                    File lFile = null;
                    for (int i : mAdapter.getSelectedItems()) {
                        lFile = mAdapter.getItem(i);
                        if (lFile.getAbsolutePath().equals(mPath))
                            mPath = lFile.getParent();
                        if (mPath == null) {
                            if (StorageUtils.getDataDirectory() != null)
                                mPath = StorageUtils.getDataDirectory().getAbsolutePath();
                            else
                                mPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                        }
                        FileUtils.deleteDirectory(lFile);
                    }
                }
                mode.finish();
                getLoaderManager().restartLoader(0, null, LibraryFragment.this);
            } else if (item.getTitle().toString().equals(getString(R.string.rename))) {
                if (mAdapter != null) {
                    for (int i : mAdapter.getSelectedItems()) {
                        final File file = mAdapter.getItem(i);
                        // Set an EditText view to get user input
                        final EditText input = new EditText(mContext);
                        new AlertDialog.Builder(mContext)
                                .setTitle(R.string.rename)
                                .setMessage("What would you like to rename " + file.getName() + " to?")
                                .setView(input)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        String value = input.getText().toString();
                                        File to = new File(file.getParentFile(), value);
                                        if (file.exists())
                                            file.renameTo(to);
                                        getLoaderManager().restartLoader(0, null, LibraryFragment.this);
                                        dialog.dismiss();
                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                            }
                        }).show();
                        break;
                    }
                }
                mode.finish();
            } else if (item.getTitle().toString().equals(getString(R.string.mark_viewed))) {
                if (mAdapter != null) {
                    File lFile = null;
                    for (int i : mAdapter.getSelectedItems()) {
                        lFile = mAdapter.getItem(i);
                        changeViewedStatus(lFile.getAbsolutePath(), true);
                    }
                }
                if (mAnime != null)
                    AnimeUtils.saveAsync(mAnime);
                mode.finish();
                mAdapter.notifyDataSetChanged();
            } else if (item.getTitle().toString().equals(getString(R.string.mark_unviewed))) {
                if (mAdapter != null) {
                    File lFile = null;
                    for (int i : mAdapter.getSelectedItems()) {
                        lFile = mAdapter.getItem(i);
                        changeViewedStatus(lFile.getAbsolutePath(), false);
                    }
                }
                if (mAnime != null)
                    AnimeUtils.saveAsync(mAnime);
                mAdapter.notifyDataSetChanged();
                mode.finish();
            } else if (item.getTitle().toString().equals(getString(R.string.download))) {
                if (mAdapter != null) {
                    File chapterFile = null;
                    for (int i : mAdapter.getSelectedItems()) {
                        chapterFile = mAdapter.getItem(i);
                        if (chapterFile != null)
                            downloadEpisode(chapterFile.getAbsolutePath());
                    }
                }
                mode.finish();
                mAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getActivity(),
                        "Got click: " + item.getTitle().toString(),
                        Toast.LENGTH_SHORT).show();
                mode.finish();
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mSelectedLowerBound = -1;
            mSelectedUpperBound = -1;
            mAdapter.setMode(MODE_SINGLE);
            mAdapter.clearSelection();
        }
    }

    @Override
    public AsyncTaskLoader<List<File>> onCreateLoader(int id, Bundle args) {
        AsyncTaskLoader<List<File>> loader = new AsyncTaskLoader<List<File>>(getActivity()) {

            private File mCurrentFile;

            @Override
            public List<File> loadInBackground() {
                mAnimeMap.clear();
                StorageUtils.setLastPath(mPath);
                mCurrentFile = new File(mPath);
                try {
                    mCurrentFile.mkdirs();
                } catch (SecurityException e) {
                    Log.e(TAG, "unable to write on the sd card " + e.toString());
                }

                File pAnimeFile = new File(mCurrentFile, "anime.json");
                if (pAnimeFile.exists()) {
                    WriteLog.appendLog("Anime data found at " + pAnimeFile.getAbsolutePath());
                    try {
                        Gson gson = new Gson();
                        BufferedReader br = new BufferedReader(new FileReader(pAnimeFile));

                        // This is how you tell gson about the generic type you want to get
                        // back:
                        // convert the json string back to object
                        mAnime = gson.fromJson(br, Anime.class);
                        br.close();
                    } catch (IOException e) {
                        WriteLog.appendLog("Loading anime data error");
                        WriteLog.appendLog(Log.getStackTraceString(e));
                        e.printStackTrace();
                    } catch (JsonSyntaxException e) {
                        WriteLog.appendLog("Old format, deleting to prevent crash");
                        pAnimeFile.delete();
                    }
                } else {
                    mAnime = null;
                }
                if (mAnime != null) {
                    if (!mCurrentFile.getAbsolutePath().contains(FileUtils.getValidFileName(mAnime.getTitle()))) {
                        mAnime = null;
                        mEpisodes = null;
                    } else {
                        mEpisodes = new HashMap<String, Episode>();
                        Episode lEpisode = null;
                        for (int i = 0; i < mAnime.getEpisodes().size(); i++) {
                            lEpisode = mAnime.getEpisodes().get(i);
                            if (TextUtils.isEmpty(lEpisode.getLocalPath()))
                                continue;
                            mEpisodes.put(lEpisode.getLocalPath(), lEpisode);
                        }
                    }
                }

                if (mCurrentFile.exists()) {
                    FileFilter fileFilter = new FileFilter() {
                        @Override
                        public boolean accept(File file) {
                            boolean lShowBackup = mPrefs.getBoolean(Constants.KEY_LIBRARY_SHOW_BACKUPS, false);
                            boolean lShowCovers = mPrefs.getBoolean(Constants.KEY_LIBRARY_SHOW_COVERS, false);
                            boolean lShowData = mPrefs.getBoolean(Constants.KEY_LIBRARY_SHOW_COVERS, false);
                            return !file.getName().equals("Cache")
                                    && !file.getName().equals(".nomedia")
                                    && (lShowCovers ?
                                    true : !file.getName().contains("cover"))
                                    && (lShowBackup ?
                                    true : !file.getName().equals("Backup"))
                                    && (lShowData ?
                                    true : (!file.getName().equals("Data")
                                    && !file.getName().endsWith(".json")));
                        }
                    };
                    File[] lFileList = mCurrentFile.listFiles(fileFilter);
                    if (lFileList != null) {
                        Arrays.sort(lFileList, new FileUtils.SortByFileName());
                        Arrays.sort(lFileList, new FileUtils.SortByFolder());

                        Anime lAnime = null;
                        for (int i = 0; i < lFileList.length; i++) {
                            if (lFileList[i].isDirectory()) {
                                lAnime = null;
                                pAnimeFile = new File(lFileList[i], "anime.json");
                                if (pAnimeFile.exists()) {
                                    WriteLog.appendLog("Anime data found at " + pAnimeFile.getAbsolutePath());
                                    try {
                                        Gson gson = new Gson();
                                        BufferedReader br = new BufferedReader(new FileReader(pAnimeFile));

                                        // This is how you tell gson about the generic type you want to get
                                        // back:
                                        // convert the json string back to object
                                        lAnime = gson.fromJson(br, Anime.class);
                                        br.close();
                                    } catch (IOException e) {
                                        WriteLog.appendLog("Loading anime data error");
                                        WriteLog.appendLog(Log.getStackTraceString(e));
                                        e.printStackTrace();
                                    } catch (JsonSyntaxException e) {
                                        WriteLog.appendLog("Old format, deleting to prevent crash");
                                        pAnimeFile.delete();
                                    }
                                }
                                if (lAnime != null) {
                                    mAnimeMap.put(lFileList[i].getAbsolutePath(), lAnime);
                                }
                            }
                        }
                        return new ArrayList<File>(Arrays.asList(lFileList));
                    }
                }
                return null;
            }

        };
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.
        loader.forceLoad();
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<List<File>> loader, List<File> data) {
        mAppBarLayout.expandToolbar();
        mLastEpisodeDownloaded = "";
        // Set the new data in the adapter.
        if (data != null) {
            mList = data;

            mAdapter.setAnime(mAnime, mEpisodes);
            mAdapter.addFileList(mList, mPrevPath.size());
            mAdapter.getFilter().filter(null);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<File>> loader) {
        // Clear the data in the adapter.
        WriteLog.appendLog("onLoaderReset called");
        mAdapter.clearData();
    }

    private void changeViewedStatus(String path, boolean isViewed) {
        MAVApplication.getInstance().getRepository().insertOfflineHistoryRecord(path, isViewed);
        if (mAnime != null) {
            Episode lEpisode = mEpisodes.get(path);
            if (lEpisode != null) {
                MAVApplication.getInstance().getRepository().insertHistoryRecord(mAnime.getUrl(), lEpisode.getUrl(), isViewed);
                //lEpisode.setIsViewed(isViewed);
            }
        }
    }

    private void downloadEpisode(String path) {
        if (mAnime != null) {
            Episode episode = mEpisodes.get(path);
            if (episode != null) {
                if (MAVApplication.getInstance().getRepository().getDownloadTask(episode.getUrl(), false) != null
                        && !mLastEpisodeDownloaded.equals(episode.getUrl())) {
                    Toast.makeText(getActivity(), "Are you sure you want to download " + episode.getTitle() + " again, then press download again", Toast.LENGTH_SHORT).show();
                    mLastEpisodeDownloaded = episode.getUrl();
                    return;
                }
                EpisodeUtils.downloadEpisode(getActivity(), episode);
            }
        }
    }

    private void viewPath(String path) {
        mIsViewing = true;
        changeViewedStatus(path, true);
        if (mAnime != null) {
            Episode lEpisode = mEpisodes.get(path);
            if (lEpisode != null) {
                //AnimeUtils.saveAsync(mAnime);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void viewOfflineAnime(String pPath) {
        mIsViewing = true;
        changeViewedStatus(pPath, true);
        if (mAnime != null) {
            Episode lEpisode = mEpisodes.get(pPath);
            if (lEpisode != null) {
                //AnimeUtils.saveAsync(mAnime);
            }
        }
        mAdapter.notifyDataSetChanged();
        FragmentTransaction ft = getActivity()
                .getSupportFragmentManager().beginTransaction();
        ft.hide(this);
        ft.addToBackStack(null);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.add(R.id.content_frame, LibraryMaterialFragment.newInstance(pPath), "LibraryAnimeFrag");
        ft.commit();
    }

    public boolean onBackPressed() {
        if (mPrevPath == null)
            return false;
        if (mPrevPath.isEmpty())
            return false;
        mPath = mPrevPath.pop();
        WriteLog.appendLog("Last folder path: " + mPath);
        File lFile = new File(mPath);
        if (lFile.getAbsolutePath().equals(StorageUtils.getExtSDCard(getActivity())) || lFile.getParent() == null)
            return false;
        if (!lFile.isDirectory()) {
            mPath = lFile.getParent();
            return false;
        }
        WriteLog.appendLog("New folder path: " + mPath);
        mAdapter.addFileList(null, mPrevPath.size());
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }
}
