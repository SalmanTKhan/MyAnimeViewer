package com.taskdesignsinc.android.myanimeviewer.fragment.filter;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.allattentionhere.fabulousfilter.AAH_FabulousFragment;
import com.google.android.flexbox.FlexboxLayout;
import com.taskdesignsinc.android.myanimeviewer.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Created by krupenghetiya on 23/06/17.
 */

public class FabFilterFragment extends AAH_FabulousFragment {

    public static String TAG = FabFilterFragment.class.getSimpleName();

    public interface FilterReqs {
        ArrayMap<String, List<String>> getAppliedFilters();

        List<String> getUniqueGenreKeys();

        List<String> getUniqueRatingKeys();

        List<String> getUniqueYearKeys();
    }


    ArrayMap<String, List<String>> appliedFilters = new ArrayMap<>();
    List<TextView> textviews = new ArrayList<>();

    TabLayout tabs_types;

    ImageButton imgbtn_refresh, imgbtn_apply;
    SectionsPagerAdapter mAdapter;
    private DisplayMetrics metrics;
    FilterReqs filterReq;


    public static FabFilterFragment newInstance() {
        FabFilterFragment fabFilterFragment = new FabFilterFragment();
        return fabFilterFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appliedFilters = filterReq.getAppliedFilters();
        metrics = this.getResources().getDisplayMetrics();

        for (Map.Entry<String, List<String>> entry : appliedFilters.entrySet()) {
            Log.d(TAG, "from activity: " + entry.getKey());
            for (String s : entry.getValue()) {
                Log.d(TAG, "from activity val: " + s);

            }
        }
    }

    @Override

    public void setupDialog(Dialog dialog, int style) {
        View contentView = View.inflate(getContext(), R.layout.filter_view, null);

        RelativeLayout rl_content = (RelativeLayout) contentView.findViewById(R.id.rl_content);
        LinearLayout ll_buttons = (LinearLayout) contentView.findViewById(R.id.ll_buttons);
        imgbtn_refresh = (ImageButton) contentView.findViewById(R.id.imgbtn_refresh);
        imgbtn_apply = (ImageButton) contentView.findViewById(R.id.imgbtn_apply);
        ViewPager vp_types = (ViewPager) contentView.findViewById(R.id.vp_types);
        tabs_types = (TabLayout) contentView.findViewById(R.id.tabs_types);

        imgbtn_apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFilter(appliedFilters);
            }
        });
        imgbtn_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (TextView tv : textviews) {
                    tv.setTag("unselected");
                    tv.setBackgroundResource(R.drawable.chip_unselected);
                    tv.setTextColor(ContextCompat.getColor(getContext(), R.color.filters_chips));
                }
                appliedFilters.clear();
            }
        });

        mAdapter = new SectionsPagerAdapter();
        vp_types.setOffscreenPageLimit(4);
        vp_types.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
        tabs_types.setupWithViewPager(vp_types);


        //params to set
        setAnimationDuration(600); //optional; default 500ms
        setPeekHeight(300); // optional; default 400dp
        setCallbacks((Callbacks) getActivity()); //optional; to get back result
        setAnimationListener((AnimationListener) getActivity()); //optional; to get animation callbacks
        setViewgroupStatic(ll_buttons); // optional; layout to stick at bottom on slide
        setViewPager(vp_types); //optional; if you use viewpager that has scrollview
        setViewMain(rl_content); //necessary; main bottomsheet view
        setMainContentView(contentView); // necessary; call at end before super
        super.setupDialog(dialog, style); //call super at last
    }

    public class SectionsPagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.view_filters_sorters, collection, false);
            FlexboxLayout fbl = (FlexboxLayout) layout.findViewById(R.id.fbl);
//            LinearLayout ll_scroll = (LinearLayout) layout.findViewById(R.id.ll_scroll);
//            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (metrics.heightPixels-(104*metrics.density)));
//            ll_scroll.setLayoutParams(lp);
            switch (position) {
                case 0:
                    inflateLayoutWithFilters("genre", fbl);
                    break;
                case 1:
                    inflateLayoutWithFilters("rating", fbl);
                    break;
                case 2:
                    inflateLayoutWithFilters("year", fbl);
                    break;
                case 3:
                    inflateLayoutWithFilters("quality", fbl);
                    break;
            }
            collection.addView(layout);
            return layout;

        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "GENRE";
                case 1:
                    return "RATING";
                case 2:
                    return "YEAR";
                case 3:
                    return "QUALITY";

            }
            return "";
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

    }

    private void inflateLayoutWithFilters(final String filter_category, FlexboxLayout fbl) {
        List<String> keys = new ArrayList<>();
        switch (filter_category) {
            case "genre":
                keys = filterReq.getUniqueGenreKeys();
                break;
            case "rating":
                keys = filterReq.getUniqueRatingKeys();
                break;
            case "year":
                keys = filterReq.getUniqueYearKeys();
                break;
        }

        for (int i = 0; i < keys.size(); i++) {
            View subchild = getActivity().getLayoutInflater().inflate(R.layout.single_chip, null);
            final TextView tv = ((TextView) subchild.findViewById(R.id.txt_title));
            tv.setText(keys.get(i));
            final int finalI = i;
            final List<String> finalKeys = keys;
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (tv.getTag() != null && tv.getTag().equals("selected")) {
                        tv.setTag("unselected");
                        tv.setBackgroundResource(R.drawable.chip_unselected);
                        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.filters_chips));
                        removeFromSelectedMap(filter_category, finalKeys.get(finalI));
                    } else {
                        tv.setTag("selected");
                        tv.setBackgroundResource(R.drawable.chip_selected);
                        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.filters_header));
                        addToSelectedMap(filter_category, finalKeys.get(finalI));
                    }
                }
            });
            try {
                Log.d("k9res", "key: " + filter_category + " |val:" + keys.get(finalI));
                Log.d("k9res", "appliedFilters != null: " + (appliedFilters != null));
                Log.d("k9res", "appliedFilters.get(key) != null: " + (appliedFilters.get(filter_category) != null));
                Log.d("k9res", "appliedFilters.get(key).contains(keys.get(finalI)): " + (appliedFilters.get(filter_category).contains(keys.get(finalI))));
            } catch (Exception e) {

            }
            if (appliedFilters != null && appliedFilters.get(filter_category) != null && appliedFilters.get(filter_category).contains(keys.get(finalI))) {
                tv.setTag("selected");
                tv.setBackgroundResource(R.drawable.chip_selected);
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.filters_header));
            } else {
                tv.setBackgroundResource(R.drawable.chip_unselected);
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.filters_chips));
            }
            textviews.add(tv);

            fbl.addView(subchild);
        }


    }

    private void addToSelectedMap(String key, String value) {
        if (appliedFilters.get(key) != null && !appliedFilters.get(key).contains(value)) {
            appliedFilters.get(key).add(value);
        } else {
            List<String> temp = new ArrayList<>();
            temp.add(value);
            appliedFilters.put(key, temp);
        }
    }

    private void removeFromSelectedMap(String key, String value) {
        if (appliedFilters.get(key).size() == 1) {
            appliedFilters.remove(key);
        } else {
            appliedFilters.get(key).remove(value);
        }
    }
}
