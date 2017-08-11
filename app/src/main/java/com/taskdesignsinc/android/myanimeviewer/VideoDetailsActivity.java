package com.taskdesignsinc.android.myanimeviewer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;

import com.taskdesignsinc.android.myanimeviewer.fragment.OfflineVideoDetailsFragment;
import com.taskdesignsinc.android.myanimeviewer.fragment.VideoDetailsFragment;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import butterknife.ButterKnife;

/**
 * Created by etiennelawlor on 5/23/15.
 * Modified by Salman T. Khan on 8/5/17.
 */
public class VideoDetailsActivity extends AppCompatActivity {

    // region Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance().setCurrentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_details);
        ButterKnife.bind(this);

        Fragment fragment = getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (fragment == null) {
            if (!TextUtils.isEmpty(getIntent().getExtras().getString(Constants.EPISODE_PATH, "")))
                fragment = OfflineVideoDetailsFragment.newInstance(getIntent().getExtras());
            else
                fragment = VideoDetailsFragment.newInstance(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, fragment, "")
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .attach(fragment)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    // endregion

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                supportFinishAfterTransition();
//                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
