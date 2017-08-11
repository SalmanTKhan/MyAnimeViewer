package com.taskdesignsinc.android.myanimeviewer.view;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.GridLayoutAnimationController;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

/**
 * Created by Satyarth on 11/03/16.
 */
public class GridRecyclerView extends FastScrollRecyclerView {

    private View emptyView;
    private AdapterDataObserver emptyObserver = new AdapterDataObserver() {


        @Override
        public void onChanged() {
            Adapter<?> adapter = getAdapter();
            if (adapter != null && emptyView != null) {
                if (adapter.getItemCount() == 0) {
                    emptyView.setVisibility(View.VISIBLE);
                    GridRecyclerView.this.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    GridRecyclerView.this.setVisibility(View.VISIBLE);
                }
            }

        }
    };

    public GridRecyclerView(Context context) {
        super(context);
    }

    public GridRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setLayoutManager(RecyclerView.LayoutManager layout) {
        if (!isInEditMode())
            if (layout instanceof GridLayoutManager) {
                super.setLayoutManager(layout);
            } else {
                throw new ClassCastException("You should only use a GridLayoutManager with GridRecyclerView.");
            }
    }

    @Override
    protected void attachLayoutAnimationParameters(View child, ViewGroup.LayoutParams params, int index, int count) {
        if (!isInEditMode())
            if (getAdapter() != null && getLayoutManager() instanceof GridLayoutManager) {

                GridLayoutAnimationController.AnimationParameters animationParams =
                        (GridLayoutAnimationController.AnimationParameters) params.layoutAnimationParameters;

                if (animationParams == null) {
                    animationParams = new GridLayoutAnimationController.AnimationParameters();
                    params.layoutAnimationParameters = animationParams;
                }

                int columns = ((GridLayoutManager) getLayoutManager()).getSpanCount();

                animationParams.count = count;
                animationParams.index = index;
                animationParams.columnsCount = columns;
                animationParams.rowsCount = count / columns;

                final int invertedIndex = count - 1 - index;
                animationParams.column = columns - 1 - (invertedIndex % columns);
                animationParams.row = animationParams.rowsCount - 1 - invertedIndex / columns;

            } else {
                super.attachLayoutAnimationParameters(child, params, index, count);
            }
    }

    @Override
    public boolean isShown() {
        return super.isShown() && getVisibility() == VISIBLE;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        if (getAdapter() != null)
            getAdapter().unregisterAdapterDataObserver(emptyObserver);
        super.setAdapter(adapter);

        if (adapter != null) {
            adapter.registerAdapterDataObserver(emptyObserver);
        }

        emptyObserver.onChanged();
    }

    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
    }
}