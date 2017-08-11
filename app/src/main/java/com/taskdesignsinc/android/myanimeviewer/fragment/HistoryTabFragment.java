package com.taskdesignsinc.android.myanimeviewer.fragment;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

import java.lang.ref.WeakReference;

public class HistoryTabFragment extends Fragment {

    public static int mPagerViewID = -1;
    public static int mPageViewID = -1;
    private Drawable oldBackground = null;
    private int currentColor = 0xFF0099CC;
    private final Handler mHandler = new Handler();

    private TabLayout mTabs;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (container == null) {
            // We have different layouts, and in one of them this
            // fragment's containing frame doesn't exist. The fragment
            // may still be created from its saved state, but there is
            // no reason to try to create its view hierarchy because it
            // won't be displayed. Note this is not needed -- we could
            // just run the code below, where we would create and return
            // the view hierarchy; it would just never be used.
            return null;
        }
        final View v = inflater.inflate(R.layout.anime_page_tab_layout, container,
                false);
        mPager = (ViewPager) v.findViewById(R.id.pager);
        mTabs = (TabLayout) v.findViewById(R.id.tabs);
        mPagerViewID = mPager.getId();
        return v;
    }

    private ViewPager mPager;
    private OnlinePagerAdapter mPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle pSavedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(pSavedInstanceState);
        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            initialisePaging();
        }

    }

    private void initialisePaging() {
        mPagerAdapter = new OnlinePagerAdapter(getChildFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                .getDisplayMetrics());
        mPager.setPageMargin(pageMargin);
        mTabs.setupWithViewPager(mPager);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                Fragment lFrag = mPagerAdapter.getItem(getInverseCurrentItem(mPager.getCurrentItem()));
                if (lFrag != null) {
                    if (lFrag instanceof HistoryMaterialFragment)
                        ((HistoryMaterialFragment) lFrag).cancelActionMode();
                    else if (lFrag instanceof OfflineHistoryMaterialFragment)
                        ((OfflineHistoryMaterialFragment) lFrag).cancelActionMode();
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
        //mTabs.setBackgroundColor(ThemeManager.getInstance(getActivity()).getBackgroundColor());
        //mTabs.setTabTextColors(ThemeManager.getInstance(getActivity()).getBackgroundColor(), ThemeManager.getInstance(getActivity()).getIndicatorColor());
        //changeColor(currentColor, false);
        getActivity().supportInvalidateOptionsMenu();
    }

    private int getInverseCurrentItem(int currentItem) {
        switch (currentItem) {
            case 1:
                return 0;
            default:
                return 1;
        }
    }

    public class OnlinePagerAdapter extends FragmentStatePagerAdapter {

        private SparseArray<WeakReference<Fragment>> weakFragmentMap = new SparseArray<WeakReference<Fragment>>();

        private final CharSequence[] mTitles = {"Online", "Offline"};

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }

        public OnlinePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public Fragment getFragment(int position) {
            WeakReference<Fragment> weakFragment = weakFragmentMap.get(position);
            if (weakFragment != null && weakFragment.get() != null) {
                return weakFragment.get();
            } else {
                return getItem(position);
            }
        }

        @Override
        public Fragment getItem(int position) {
            //Holder currentHolder = holderList.get(position);
            WeakReference<Fragment> weakFragment = weakFragmentMap.get(position);
            if (weakFragment == null) {
                if (position == 0)
                    weakFragment = new WeakReference<Fragment>(
                            Fragment.instantiate(getActivity(), HistoryMaterialFragment.class.getName(), getArguments()));
                else if (position == 1)
                    weakFragment = new WeakReference<Fragment>(
                            Fragment.instantiate(getActivity(), OfflineHistoryMaterialFragment.class.getName(), getArguments()));
                weakFragmentMap.put(position, weakFragment);
            }

            return weakFragment.get();
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            WeakReference<Fragment> weakFragment = weakFragmentMap.get(position);
            if (weakFragment != null) {
                weakFragment.clear();
            }
        }

        public void changeDataset() {
            for (int i = 0; i < weakFragmentMap.size(); i++) {
                WeakReference<Fragment> weakFragment = weakFragmentMap.valueAt(i);
                if (weakFragment != null) {
                    weakFragment.clear();
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration pNewConfig) {
        super.onConfigurationChanged(pNewConfig);
        initialisePaging();
    }

    private Drawable.Callback drawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setBackgroundDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            mHandler.postAtTime(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            mHandler.removeCallbacks(what);
        }
    };


    public void refresh() {
        WriteLog.appendLog("Refreshing History");
        Fragment lFrag = mPagerAdapter.getItem(mPager.getCurrentItem());
        if (lFrag != null) {
            if (lFrag instanceof HistoryMaterialFragment)
                ((HistoryMaterialFragment) lFrag).refresh();
            else if (lFrag instanceof OfflineHistoryMaterialFragment)
                ((OfflineHistoryMaterialFragment) lFrag).refresh();
        }
    }
}
