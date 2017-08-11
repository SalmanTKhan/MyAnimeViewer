package com.taskdesignsinc.android.myanimeviewer.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import com.squareup.leakcanary.RefWatcher;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;

import timber.log.Timber;

/**
 * Created by etiennelawlor on 6/13/15.
 * Modified by salmantkhan on 7/20/17.
 */
public abstract class BaseFragment extends Fragment {

    // region Member Variables

    // endregion

    // region Lifecycle Methods
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate()");
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Timber.d("onViewCreated()");
    }

    @Override
    public void onStop() {
        super.onStop();
        Timber.d("onStop()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Timber.d("onDestroyView()");

        String className = this.getClass().toString();
        Timber.d("onDestroyView() : className - " + className);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        RefWatcher refWatcher = MAVApplication.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }
    // region
}
