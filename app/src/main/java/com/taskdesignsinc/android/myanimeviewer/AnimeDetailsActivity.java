package com.taskdesignsinc.android.myanimeviewer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.WindowManager.LayoutParams;
import android.widget.RelativeLayout;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.taskdesignsinc.android.myanimeviewer.fragment.AnimeMaterialListFragment;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.ImageLoaderManager;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;


/**
 * This is a secondary activity, to show what the user has selected when the
 * screen is not large enough to show it all in one activity.
 */
public class AnimeDetailsActivity extends AppCompatActivity {

    private BroadcastReceiver mReceiver;
    IntentFilter mFilter;
    private boolean mInstanceStateSaved;

    private static Fragment mContent;

    final String mTAG = AnimeDetailsActivity.class.getSimpleName();

    // Does the user have the premium upgrade?
    private SharedPreferences mPrefs;
    Parser mParser;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.coordinator_layout)
    CoordinatorLayout mCoordinatorLayout;

    private Unbinder unbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (mPrefs == null)
            mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        ThemeManager.getInstance(this).setCurrentTheme(this);

        super.onCreate(savedInstanceState);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //if (!PlayServicesUtils.checkPlayServices(this, 9999) && !TextUtils.isEmpty(getString(R.string.cast_app_id))) {
            setContentView(R.layout.activity_new_main);
        //} else {
            //setContentView(R.layout.activity_new_main_cast);
        //}
        unbinder = ButterKnife.bind(this);
        // Handle Toolbar
        setSupportActionBar(toolbar);
        ActivityCompat.postponeEnterTransition(this);

        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        setupCast(savedInstanceState);
        ImageLoaderManager.getInstance(this);

        // Create the receiver
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleIntent(intent);
            }

            private void handleIntent(Intent intent) {

                if (intent != null
                        && intent.getAction().equals(
                        Constants.Intents.EPISODE_DOWNLOAD)) {
                    int type = intent.getIntExtra(Constants.DownloadIntents.TYPE, -1);
                    int subtype = intent.getIntExtra(Constants.DownloadIntents.SUBTYPE, -1);
                    String url;
                    String title;

                    switch (type) {
                        case Constants.DownloadIntents.Types.COMPLETE:
                            url = intent.getStringExtra(Constants.DownloadIntents.URL);
                            String mangaUrl = intent.getStringExtra(Constants.ANIME_URL);
                            if (!TextUtils.isEmpty(url)) {
                                Fragment lFrag = getSupportFragmentManager().findFragmentByTag("AnimeFrag");
                                if (lFrag != null) {
                                    //if (lFrag instanceof AnimeMaterialListFragment)
                                    //((AnimeMaterialListFragment)lFrag).refresh(mangaUrl, url);
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        };
        mFilter = new IntentFilter(Constants.Intents.EPISODE_DOWNLOAD);

        registerReceiver(mReceiver, mFilter);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            String lAnimeURL = intent.getData().toString();
            int pType = Parser.getTypeByURL(lAnimeURL);
            Parser.isValidSource(Parser.getExistingInstance(pType));
            if (pType != -1) {
                mParser = Parser.getInstance(pType);
                intent.putExtra(Constants.ANIME_URL, lAnimeURL);
            } else {
                Snackbar.make(mCoordinatorLayout, "Unknown Source Type", Snackbar.LENGTH_SHORT).show();
                //finish();
            }
        }

        String lAnimeURL = "";
        if (intent.getExtras() != null) {
            lAnimeURL = intent.getExtras().getString(Constants.ANIME_URL);
            if (!TextUtils.isEmpty(lAnimeURL)) {
                int pType = Parser.getTypeByURL(lAnimeURL);
                Parser.isValidSource(Parser.getExistingInstance(pType));
                if (pType != -1) {
                    mParser = Parser.getInstance(pType);
                } else {
                    Snackbar.make(mCoordinatorLayout, "Unknown Source Type", Snackbar.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Snackbar.make(mCoordinatorLayout, "Anime URL is not valid: " + lAnimeURL, Snackbar.LENGTH_SHORT).show();
                finish();
            }
        }

        if (savedInstanceState == null) {
            if (mParser == null) {
                if (getIntent() != null && getIntent().getExtras() != null)
                    mParser = Parser.getExistingInstance(
                            Parser.getTypeByURL(
                                    getIntent().getExtras().getString(
                                            Constants.ANIME_URL)));
            }
            // During initial setup, plug in the details fragment.
            mContent = new AnimeMaterialListFragment();
            //mContent = new EpisodeListFragment();
            //AnimeDetailFragment details = new AnimeDetailFragment();
            mContent.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, mContent, "AnimeFrag").commit();
        }
    }

    public void refresh() {
        WriteLog.appendLog(mTAG + ": refresh called");
        if (mContent != null) {
            if (mContent instanceof AnimeMaterialListFragment)
                ((AnimeMaterialListFragment)mContent).refresh();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerCast();
        if (mReceiver != null && mFilter != null)
            registerReceiver(mReceiver, mFilter);
        // Call Socialize in onResume
        refresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
        if (mReceiver != null)
            unregisterReceiver(mReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterCast();

        // Unregister the receiver
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent upIntent = new Intent(this, MainActivity.class);
            if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                // This activity is not part of the application's task, so create a new task
                // with a synthesized back stack.
                TaskStackBuilder.from(this)
                        .addNextIntent(upIntent)
                        .startActivities();
                finish();
            } else {
                // This activity is part of the application's task, so simply
                // navigate up to the hierarchical parent activity.
                NavUtils.navigateUpTo(this, upIntent);
            }
            return true;
        } else if (item.getTitle().equals("Settings")) {
            //startActivity(new Intent(this, Settings.class));
            finish();
            // overridePendingTransition(R.anim.hold, R.anim.push_out_to_left);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mInstanceStateSaved = true;
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        super.onSaveInstanceState(outState);
    }

    public void changeBrightness(int pBrightness) {
        WriteLog.appendLog("New brightness value " + pBrightness);
        //Get the current window attributes
        LayoutParams layoutpars = getWindow().getAttributes();
        //Set the brightness of this window
        layoutpars.screenBrightness = pBrightness / (float) 255;
        WriteLog.appendLog("New brightness percent value " + layoutpars.screenBrightness);
        //Apply attribute changes to this window
        getWindow().setAttributes(layoutpars);
    }

    //Casting Related
    private CastContext mCastContext;
    private final SessionManagerListener<CastSession> mSessionManagerListener =
            new MySessionManagerListener();
    private CastSession mCastSession;
    private MenuItem mediaRouteMenuItem;
    private MenuItem mQueueMenuItem;
    private CastStateListener mCastStateListener;

    private void setupCast(Bundle savedInstanceState) {
        if (TextUtils.isEmpty(getString(R.string.cast_app_id)))
            return;
        mCastStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int newState) {
                if (newState != CastState.NO_DEVICES_AVAILABLE) {

                }
            }
        };
        mCastContext = CastContext.getSharedInstance(this);
        mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);
    }

    private void registerCast() {
        if (TextUtils.isEmpty(getString(R.string.cast_app_id)))
            return;
        mCastContext.addCastStateListener(mCastStateListener);
        mCastContext.getSessionManager().addSessionManagerListener(
                mSessionManagerListener, CastSession.class);
        if (mCastSession == null) {
            mCastSession = CastContext.getSharedInstance(this).getSessionManager()
                    .getCurrentCastSession();
        }
        if (mQueueMenuItem != null) {
            mQueueMenuItem.setVisible(
                    (mCastSession != null) && mCastSession.isConnected());
        }
    }

    private void unregisterCast() {
        if (TextUtils.isEmpty(getString(R.string.cast_app_id)))
            return;
        mCastContext.removeCastStateListener(mCastStateListener);
        mCastContext.getSessionManager().removeSessionManagerListener(
                mSessionManagerListener, CastSession.class);
    }

    private class MySessionManagerListener implements SessionManagerListener<CastSession> {

        @Override
        public void onSessionEnded(CastSession session, int error) {
            if (session == mCastSession) {
                mCastSession = null;
            }
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionResumed(CastSession session, boolean wasSuspended) {
            mCastSession = session;
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionStarted(CastSession session, String sessionId) {
            mCastSession = session;
            invalidateOptionsMenu();
        }

        @Override
        public void onSessionStarting(CastSession session) {
        }

        @Override
        public void onSessionStartFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionEnding(CastSession session) {
        }

        @Override
        public void onSessionResuming(CastSession session, String sessionId) {
        }

        @Override
        public void onSessionResumeFailed(CastSession session, int error) {
        }

        @Override
        public void onSessionSuspended(CastSession session, int reason) {
        }
    }
}