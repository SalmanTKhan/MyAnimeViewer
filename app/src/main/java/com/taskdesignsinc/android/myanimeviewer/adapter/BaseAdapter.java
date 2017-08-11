package com.taskdesignsinc.android.myanimeviewer.adapter;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by salma on 7/21/2017.
 */

public abstract class BaseAdapter extends RecyclerView.Adapter {

    @LayoutRes
    public abstract int getLayouResId();
}
