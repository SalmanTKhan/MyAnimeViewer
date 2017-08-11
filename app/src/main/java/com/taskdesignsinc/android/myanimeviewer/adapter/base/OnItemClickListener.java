package com.taskdesignsinc.android.myanimeviewer.adapter.base;

public interface OnItemClickListener {
        /**
         *
         * @param position
         * @return true if MULTI selection is enabled, false for SINGLE selection
         */
        boolean onListItemClick(int position);

        /**
         *
         * @param position
         */
        void onListItemLongClick(int position);
    }