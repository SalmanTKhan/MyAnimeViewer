package com.taskdesignsinc.android.myanimeviewer.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.adapter.EpisodeRecyclerAdapter;
import com.taskdesignsinc.android.myanimeviewer.adapter.base.OnItemClickListener;
import com.taskdesignsinc.android.myanimeviewer.loader.AnimeLoader;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.model.helper.EpisodeUtils;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.view.GridRecyclerView;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class EpisodeListFragment extends BaseFragment implements
        LoaderManager.LoaderCallbacks<Anime> {

    String TAG = EpisodeListFragment.class.getSimpleName();

    private OnItemClickListener itemClickListener = new OnItemClickListener() {
        @Override
        public boolean onListItemClick(int position) {
            Episode episode = mAdapter.getItem(position);
            if (episode != null) {
                String lastEpisodeUrl = MAVApplication.getInstance().getRepository().getLastViewedEpisode(mAnimeUrl);
                episode.setViewed(true);
                MAVApplication.getInstance().getRepository().updateEpisode(episode);
                mAdapter.setLastEpisodeViewed(episode.getUrl());
                EpisodeUtils.viewEpisode(TAG, getActivity(), episode.getAnime(), episode, null);
                if (!TextUtils.isEmpty(lastEpisodeUrl)) {
                    int previousLastEpisodePosition = mAdapter.getItemPosition(lastEpisodeUrl);
                    if (previousLastEpisodePosition != -1)
                        mAdapter.notifyItemChanged(previousLastEpisodePosition);
                }
                mAdapter.notifyDataSetChanged();
            }
            return false;
        }

        @Override
        public void onListItemLongClick(int position) {

        }
    };

    /**
     * Create a new instance of EpisodeListFragment, initialized to show the text at
     * 'index'.
     */
    public static EpisodeListFragment newInstance(int pAnimeID, String pAnimeURL) {
        EpisodeListFragment f = new EpisodeListFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(Constants.ANIME_ID, pAnimeID);
        args.putString(Constants.ANIME_URL, pAnimeURL);
        f.setArguments(args);

        return f;
    }

    /**
     * Create a new instance of EpisodeListFragment, initialized to show the text at
     * 'index'.
     */
    public static EpisodeListFragment newInstance(Bundle args) {
        EpisodeListFragment f = new EpisodeListFragment();

        f.setArguments(args);

        return f;
    }

    EpisodeRecyclerAdapter mAdapter;

    private Unbinder unbinder;
    private Parser mParser;
    private String mAnimeUrl;
    private long mAnimeId;

    @BindView(R.id.recyclerView)
    GridRecyclerView recyclerView;
    private GridLayoutManager layoutManager;

    Anime mAnime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAnimeId = getArguments() != null ? getArguments().getLong(Constants.ANIME_ID) : -1;
        mAnimeUrl = getArguments() != null ? getArguments().getString(Constants.ANIME_URL) : "";
        mAnime = MAVApplication.getInstance().getRepository().getAnimeByUrl(mAnimeUrl, true);
        mParser = Parser.getExistingInstance(Parser.getTypeByURL(mAnimeUrl));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_episode_list, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        if (recyclerView != null) {
            recyclerView.setPopupTextColor(ThemeManager.getInstance().getTextColor());
            recyclerView.setPopupBgColor(ThemeManager.getInstance().getPrimaryColor(rootView.getContext()));
            recyclerView.setThumbColor(ThemeManager.getInstance().getPrimaryDarkColor(rootView.getContext()));
            recyclerView.setTrackColor(ThemeManager.getInstance().getAccentColor(rootView.getContext()));
        }
        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutManager = new GridLayoutManager(getActivity(), 1);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new EpisodeRecyclerAdapter(getActivity(), itemClickListener);
        recyclerView.setAdapter(mAdapter);

        getLoaderManager().initLoader(EpisodeListFragment.class.getSimpleName().hashCode(), null, this);
    }

    @Override
    public Loader<Anime> onCreateLoader(int id, Bundle args) {
        AnimeLoader loader = new AnimeLoader(getActivity(), mParser, mAnimeUrl);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Anime> loader, Anime data) {
        mAdapter.setData(data.getEpisodes());
    }

    @Override
    public void onLoaderReset(Loader<Anime> loader) {

    }

    private void updateItemAtPosition(int position) {
        if (position == -1)
            return;
        //View view = recyclerView.getChildAt(position);
        mAdapter.notifyItemChanged(position);
    }
}