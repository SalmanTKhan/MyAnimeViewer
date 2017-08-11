package com.taskdesignsinc.android.myanimeviewer.base;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * based of https://raw.githubusercontent.com/android/platform_frameworks_base/d6c1919779acb042392615637b9007e0c4b89023/core/java/android/app/ListActivity.java
 * Created by elcuco on 5/27/2014.
 */
@SuppressWarnings("UnusedDeclaration")
public class SupportListActivity extends AppCompatActivity {
    protected ListAdapter mAdapter;
    protected ListView mList;
    protected TextView mEmptyMessage;

    @Override
    protected void onCreate(Bundle savedBundle) {
        super.onCreate(savedBundle);
    }

    @Override
    public void setContentView(int layoutResID) {
        if (layoutResID != -1) {
            super.setContentView(layoutResID);
            mList = (ListView) findViewById(android.R.id.list);
            mEmptyMessage = (TextView) findViewById(android.R.id.empty);
            mList.setEmptyView(mEmptyMessage);
        } else {
            mEmptyMessage = new TextView(this);
            mEmptyMessage.setText("No results");
            mList = new ListView(this);
            mList.setId(android.R.id.list);
            mList.setEmptyView(mEmptyMessage);
            mEmptyMessage.setId(android.R.id.text1);
            LinearLayout ll = new LinearLayout(this);
            ll.addView(mEmptyMessage, TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
            ll.addView(mList, TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT);
            setContentView(ll);
        }
    }

    ;

    public void setListAdapter(ListAdapter adapter) {
        synchronized (this) {
            mAdapter = adapter;
            mList.setAdapter(adapter);
        }
    }

    /**
     * Get the activity's list view widget.
     */
    public ListView getListView() {
        return mList;
    }

    /**
     * Set the currently selected list item to the specified
     * position with the adapter's data
     *
     * @param position the position on list to select
     */
    public void setSelection(int position) {
        mList.setSelection(position);
    }

    /**
     * Get the position of the currently selected list item.
     */
    public int getSelectedItemPosition() {
        return mList.getSelectedItemPosition();
    }

    /**
     * Get the cursor row ID of the currently selected list item.
     */
    public long getSelectedItemId() {
        return mList.getSelectedItemId();
    }

    private AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            onListItemClick((ListView) parent, v, position, id);
        }
    };

    /**
     * This method will be called when an item in the list is selected.
     * Subclasses should override. Subclasses can call
     * getListView().getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param l        The ListView where the click happened
     * @param v        The view that was clicked within the ListView
     * @param position The position of the view in the list
     * @param id       The row id of the item that was clicked
     */
    protected void onListItemClick(ListView l, View v, int position, long id) {
    }
}