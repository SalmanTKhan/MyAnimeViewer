package com.taskdesignsinc.android.myanimeviewer;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.mikelau.croperino.Croperino;
import com.mikepenz.crossfader.Crossfader;
import com.mikepenz.crossfader.util.UIUtils;
import com.mikepenz.fastadapter.commons.utils.RecyclerViewCacheUtil;
import com.mikepenz.itemanimators.AlphaCrossFadeAnimator;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.MiniDrawer;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.parse.ParseUser;
import com.parse.ui.ParseLoginBuilder;
import com.taskdesignsinc.android.myanimeviewer.fragment.AnimeCardGridFragment;
import com.taskdesignsinc.android.myanimeviewer.fragment.FavoritesFragment;
import com.taskdesignsinc.android.myanimeviewer.fragment.HistoryTabFragment;
import com.taskdesignsinc.android.myanimeviewer.fragment.LibraryFragment;
import com.taskdesignsinc.android.myanimeviewer.model.FavoriteTag;
import com.taskdesignsinc.android.myanimeviewer.model.helper.AnimeUtils;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;
import com.taskdesignsinc.android.myanimeviewer.util.CrossfadeWrapper;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;
import com.taskdesignsinc.android.thememanager.ThemeManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static com.taskdesignsinc.android.myanimeviewer.util.Constants.ProfileCodes.PROFILE_ADD_ACCOUNT;
import static com.taskdesignsinc.android.myanimeviewer.util.Constants.ProfileCodes.PROFILE_LOGOUT;
import static com.taskdesignsinc.android.myanimeviewer.util.Constants.ProfileCodes.PROFILE_VIEW;

public class MainActivity extends AppCompatActivity {

    static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    private AccountHeader headerResult;
    private Drawer drawerResult = null;
    private MiniDrawer miniResult = null;
    private Crossfader crossFader;
    private Unbinder unbinder;
    private Parser mParser;
    private Drawer.OnDrawerItemClickListener drawerItemClickListener = new Drawer.OnDrawerItemClickListener() {
        @Override
        public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
            //check if the drawerItem is set.
            //there are different reasons for the drawerItem to be null
            //--> click on the header
            //--> click on the footer
            //those items don't contain a drawerItem
            if (drawerItem != null) {
                Intent intent = null;

                if (drawerItem.getIdentifier() == 17) {
                    Uri uri = Uri.parse("https://www.patreon.com/mymangareader");
                    intent = new Intent(Intent.ACTION_VIEW, uri);
                } else if (drawerItem instanceof PrimaryDrawerItem) {
                    selectItem(((PrimaryDrawerItem) drawerItem).getName().getText(MainActivity.this));
                }
                if (intent != null) {
                    startActivity(intent);
                }
            }

            return false;
        }
    };
    private Fragment mCurrentFrag;
    @BindView(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;
    private CustomSearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance(this).setCurrentTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
        mParser = Parser.getExistingInstance(0);
        setSupportActionBar(toolbar);
        setupNavigationMenu(savedInstanceState);
        buildMenuList();
        setupFavoriteTags();

        if (savedInstanceState == null) {
            selectItem(getString(R.string.menu_title_catalog));
            //getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new ParseAnimeListFragment()).commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        WriteLog.appendLog(TAG, "onNewIntent() called");
        if (intent != null) {
            final String action = intent.getAction();

            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                WriteLog.appendLog(TAG, "Search Query: " + query);
            } else if (Intent.ACTION_VIEW.equals(action)) {
                String animeUrl = intent.getData().toString();
                int pType = Parser.getTypeByURL(animeUrl);
                Parser.isValidSource(Parser.getExistingInstance(pType));
                if (pType != -1) {
                    if (mCurrentFrag == null) {
                        int lHomeScreen = Integer.parseInt(MAVApplication.getInstance().getPreferences().getString(Constants.KEY_HOME_SCREEN_TYPE, "0"));
                        switch (lHomeScreen) {
                            case Constants.HOME_SCREEN_LIBRARY:
                                selectItem(getString(R.string.menu_title_library));
                                break;
                            case Constants.HOME_SCREEN_FAVORITES:
                                selectItem(getString(R.string.menu_title_favorites));
                                break;
                            case Constants.HOME_SCREEN_HISTORY:
                                selectItem(getString(R.string.menu_title_history));
                                break;
                            default:
                                if (MAVApplication.getInstance().getPreferences().getBoolean(Constants.USE_DEFAULT_ANIME_SOURCE, false) || Intent.ACTION_VIEW.equals(action))
                                    selectItem(getString(R.string.menu_title_catalog));
                                else
                                    selectItem(mParser.getName());
                                break;
                        }
                    }
                    AnimeUtils.viewAnime(this, animeUrl);
                } else {
                    Snackbar.make(coordinatorLayout, "Unknown Source Type for " + animeUrl, Snackbar.LENGTH_SHORT);
                    //Toast.makeText(this, "Unknown Source Type", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                int lItem = intent.getIntExtra(SearchManager.QUERY, -1);
                //selectItem(lItem);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuItem item = menu.add(R.string.search);
        if (ThemeManager.getInstance().isLightBackground())
            item.setIcon(R.drawable.ic_search_black_24dp);
        else
            item.setIcon(R.drawable.ic_search_white_24dp);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS
                | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = new CustomSearchView(this);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);
        MenuItemCompat.setActionView(item, searchView);
        return true;
    }

    public static class CustomSearchView extends SearchView {
        public CustomSearchView(Context context) {
            super(context);
        }

        // The normal SearchView doesn't clear its search text when
        // collapsed, so we will do this for it.
        @Override
        public void onActionViewCollapsed() {
            setQuery("", false);
            super.onActionViewCollapsed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
        //add the values which need to be saved from the drawer to the bundle
        outState = drawerResult.saveInstanceState(outState);
        //add the values which need to be saved from the accountHeader to the bundle
        outState = headerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private void setupNavigationMenu(Bundle savedInstanceState) {
        initialDrawerItems();
        buildHeader(savedInstanceState);
        buildDrawer(savedInstanceState);
    }

    //Menu Items
    PrimaryDrawerItem mDrawerItem_Parser;
    PrimaryDrawerItem mDrawerItem_Catalog;
    PrimaryDrawerItem mDrawerItem_Library;
    PrimaryDrawerItem mDrawerItem_Favorites;
    PrimaryDrawerItem mDrawerItem_History;
    PrimaryDrawerItem mDrawerItem_Downloads;
    PrimaryDrawerItem mDrawerItem_AllManga;
    PrimaryDrawerItem mDrawerItem_Genres;
    PrimaryDrawerItem mDrawerItem_LatestReleases;
    PrimaryDrawerItem mDrawerItem_RandomManga;
    PrimaryDrawerItem mDrawerItem_AdvancedSearch;
    PrimaryDrawerItem mDrawerItem_NewSeries;
    PrimaryDrawerItem mDrawerItem_ServerSearch;
    PrimaryDrawerItem mDrawerItem_Settings;
    PrimaryDrawerItem mDrawerItem_Patreon;
    SecondaryDrawerItem mDrawerItem_Version;

    private void initialDrawerItems() {
        //Menu Items
        mDrawerItem_Parser = new PrimaryDrawerItem().withName("Source").withIcon(R.mipmap.ic_launcher).withIdentifier(1);
        mDrawerItem_Catalog = new PrimaryDrawerItem().withName(R.string.menu_title_catalog).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_public_black_24dp : R.drawable.ic_public_white_24dp).withIdentifier(2);
        mDrawerItem_Library = new PrimaryDrawerItem().withName(R.string.menu_title_library).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_home_black_24dp : R.drawable.ic_home_white_24dp).withIdentifier(3);
        mDrawerItem_Favorites = new PrimaryDrawerItem().withName(R.string.menu_title_favorites).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_favorite_black_24dp : R.drawable.ic_favorite_white_24dp).withIdentifier(4);
        mDrawerItem_History = new PrimaryDrawerItem().withName(R.string.menu_title_history).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_history_black_24dp : R.drawable.ic_history_white_24dp).withIdentifier(5);
        mDrawerItem_Downloads = new PrimaryDrawerItem().withName(R.string.menu_title_downloads).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_save_black_24dp : R.drawable.ic_save_white_24dp).withIdentifier(6);
        mDrawerItem_AllManga = new PrimaryDrawerItem().withName(R.string.menu_title_all_anime).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_public_black_24dp : R.drawable.ic_public_white_24dp).withIdentifier(7);
        mDrawerItem_Genres = new PrimaryDrawerItem().withName(R.string.menu_title_genres).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_filter_list_black_24dp : R.drawable.ic_filter_list_white_24dp).withIdentifier(8);
        mDrawerItem_LatestReleases = new PrimaryDrawerItem().withName(R.string.menu_title_latest_releases).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_new_releases_black_24dp : R.drawable.ic_new_releases_white_24dp).withIdentifier(9);
        mDrawerItem_RandomManga = new PrimaryDrawerItem().withName(R.string.menu_title_random_anime).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_help_outline_black_24dp : R.drawable.ic_help_outline_white_24dp).withIdentifier(10);
        mDrawerItem_AdvancedSearch = new PrimaryDrawerItem().withName(R.string.menu_title_advanced_search).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_search_black_24dp : R.drawable.ic_search_white_24dp).withIdentifier(11);
        mDrawerItem_NewSeries = new PrimaryDrawerItem().withName(R.string.menu_title_new_series).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_today_black_24dp : R.drawable.ic_today_white_24dp).withIdentifier(12);
        //new PrimaryDrawerItem().withName(R.string.menu_title_queue).withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_comment_list)).withIdentifier(14);
        mDrawerItem_ServerSearch = new PrimaryDrawerItem().withName("Search Online").withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_search_black_24dp : R.drawable.ic_search_white_24dp).withIdentifier(15);
        mDrawerItem_Settings = new PrimaryDrawerItem().withName(R.string.menu_title_settings).withIcon(ThemeManager.getInstance().isLightBackground() ? R.drawable.ic_settings_black_24dp : R.drawable.ic_settings_white_24dp).withIdentifier(16);
        mDrawerItem_Patreon = new PrimaryDrawerItem().withName(R.string.menu_title_patreon).withIcon(R.drawable.patreon).withIdentifier(17);
        mDrawerItem_Version = new SecondaryDrawerItem().withName("Version: " + BuildConfig.VERSION_NAME).withSelectable(false);
    }

    private void buildHeader(Bundle savedInstanceState) {
        // Create the AccountHeader
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withTranslucentStatusBar(true)
                .withCompactStyle(false)
                .withHeaderBackground(R.drawable.header)
                .withCurrentProfileHiddenInList(true)
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean current) {
                        //sample usage of the onProfileChanged listener
                        //if the clicked item has the identifier 1 add a new profile ;)
                        if (profile instanceof IDrawerItem && profile.getIdentifier() == PROFILE_ADD_ACCOUNT) {
                            showLoginActivity();
                        } else if (profile instanceof IDrawerItem && profile.getIdentifier() == PROFILE_LOGOUT) {
                            IProfile activeProfile = headerResult.getActiveProfile();
                            if (activeProfile != null) {
                                if (activeProfile.getName() != null && activeProfile.getEmail() != null) {
                                    ParseUser.logOut();
                                }
                                if (miniResult != null)
                                    miniResult.createItems();
                            }
                        } else if (profile instanceof IDrawerItem && profile.getIdentifier() == PROFILE_VIEW) {
                            showProfileView();
                        } else if (!current) {
                            switchUser();
                        } else {
                            Croperino.prepareGallery(MainActivity.this);
                        }

                        //false if you have not consumed the event and it should close the drawer
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();
    }

    private void buildDrawer(Bundle savedInstanceState) {
        if (getResources().getBoolean(R.bool.is_tablet)) {
            //Create the drawer
            drawerResult = new DrawerBuilder()
                    .withActivity(this)
                    .withToolbar(toolbar)
                    .withHasStableIds(true)
                    .withItemAnimator(new AlphaCrossFadeAnimator())
                    .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
                    .addDrawerItems(
                            mDrawerItem_Parser,
                            mDrawerItem_Catalog,
                            mDrawerItem_Library,
                            mDrawerItem_Favorites,
                            mDrawerItem_History,
                            mDrawerItem_Downloads,
                            mDrawerItem_AllManga,
                            mDrawerItem_Genres,
                            mDrawerItem_LatestReleases,
                            mDrawerItem_RandomManga,
                            mDrawerItem_AdvancedSearch,
                            mDrawerItem_NewSeries,
                            //new PrimaryDrawerItem().withName(R.string.menu_title_queue).withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_comment_list)).withIdentifier(14),
                            mDrawerItem_Settings,
                            mDrawerItem_Patreon,
                            mDrawerItem_Version
                    ) // add the items we want to use with our Drawer
                    .withOnDrawerItemClickListener(drawerItemClickListener)
                    .withSavedInstance(savedInstanceState)
                    .withShowDrawerOnFirstLaunch(false)
                    .withGenerateMiniDrawer(getResources().getBoolean(R.bool.is_tablet))
                    .buildView();
            miniResult = drawerResult.getMiniDrawer();

            //get the widths in px for the first and second panel
            int firstWidth = (int) UIUtils.convertDpToPixel(360, this);
            int secondWidth = (int) UIUtils.convertDpToPixel(72, this);

            //create and build our crossfader (see the MiniDrawer is also builded in here, as the build method returns the view to be used in the crossfader)
            //the crossfader library can be found here: https://github.com/mikepenz/Crossfader
            crossFader = new Crossfader()
                    .withContent(findViewById(R.id.coordinator_layout))
                    .withFirst(drawerResult.getSlider(), firstWidth)
                    .withSecond(miniResult.build(this), secondWidth)
                    .withGmailStyleSwiping()
                    .withSavedInstance(savedInstanceState)
                    .build();

            //define the crossfader to be used with the miniDrawer. This is required to be able to automatically toggle open / close
            miniResult.withCrossFader(new CrossfadeWrapper(crossFader));

            //define a shadow (this is only for normal LTR layouts if you have a RTL app you need to define the other one
            crossFader.getCrossFadeSlidingPaneLayout().setShadowResourceLeft(R.drawable.material_drawer_shadow_left);
        } else {
            //Create the drawer
            drawerResult = new DrawerBuilder()
                    .withActivity(this)
                    .withToolbar(toolbar)
                    .withHasStableIds(true)
                    .withItemAnimator(new AlphaCrossFadeAnimator())
                    .withAccountHeader(headerResult) //set the AccountHeader we created earlier for the header
                    .addDrawerItems(
                            mDrawerItem_Parser,
                            mDrawerItem_Catalog,
                            mDrawerItem_Library,
                            mDrawerItem_Favorites,
                            mDrawerItem_History,
                            mDrawerItem_Downloads,
                            mDrawerItem_AllManga,
                            mDrawerItem_Genres,
                            mDrawerItem_LatestReleases,
                            mDrawerItem_RandomManga,
                            mDrawerItem_AdvancedSearch,
                            mDrawerItem_NewSeries,
                            //new PrimaryDrawerItem().withName(R.string.menu_title_queue).withIcon(new IconicsDrawable(this, GoogleMaterial.Icon.gmd_comment_list)).withIdentifier(14),
                            mDrawerItem_Settings,
                            mDrawerItem_Patreon,
                            mDrawerItem_Version
                    ) // add the items we want to use with our Drawer
                    .withOnDrawerItemClickListener(drawerItemClickListener)
                    .withSavedInstance(savedInstanceState)
                    .withShowDrawerOnFirstLaunch(false)
                    .withGenerateMiniDrawer(getResources().getBoolean(R.bool.is_tablet))
                    .build();
        }

        //if you have many different types of DrawerItems you can magically pre-cache those items to get a better scroll performance
        //make sure to init the cache after the DrawerBuilder was created as this will first clear the cache to make sure no old elements are in
        //RecyclerViewCacheUtil.getInstance().withCacheSize(2).init(drawerResult);
        new RecyclerViewCacheUtil<IDrawerItem>().withCacheSize(2).apply(drawerResult.getRecyclerView(), drawerResult.getDrawerItems());
    }

    private void buildMenuList() {
        ArrayList<IDrawerItem> menuList = new ArrayList<IDrawerItem>();
        int parserSpecificFeatureCount = 0;
        mDrawerItem_Parser.withName(mParser.getName());
        menuList.add(mDrawerItem_Parser);
        menuList.add(mDrawerItem_Catalog);
        menuList.add(mDrawerItem_Library);
        menuList.add(mDrawerItem_Favorites);
        menuList.add(mDrawerItem_History);
        menuList.add(mDrawerItem_Downloads);
        menuList.add(new SectionDrawerItem().withName(mParser.getName() + " Specific Features"));
        if (mParser.isShowAllSupported) {
            menuList.add(mDrawerItem_AllManga);
            parserSpecificFeatureCount++;
        }
        if (mParser.isGenreSortSupported) {
            menuList.add(mDrawerItem_Genres);
            parserSpecificFeatureCount++;
        }
        if (mParser.isLatestSortSupported) {
            menuList.add(mDrawerItem_LatestReleases);
            parserSpecificFeatureCount++;
        }
        if (parserSpecificFeatureCount == 0)
            menuList.remove(menuList.size() - 1);
        else
            menuList.add(new SectionDrawerItem().withName("MAV Features"));
        //menuList.add(mDrawerItem_ServerSearch);
        menuList.add(mDrawerItem_Settings);
        menuList.add(mDrawerItem_Patreon);
        menuList.add(mDrawerItem_Version);
        drawerResult.setItems(menuList);
        if (miniResult != null)
            miniResult.createItems();
    }

    private void showLoginActivity() {
        // User clicked to log in.
        ParseLoginBuilder loginBuilder = new ParseLoginBuilder(MainActivity.this);
        //loginBuilder.setFacebookLoginEnabled(true);
        loginBuilder.setTwitterLoginEnabled(true);
        loginBuilder.setParseLoginEmailAsUsername(false);
        startActivityForResult(loginBuilder.build(), Constants.RequestCodes.LOGIN);
    }

    private void showProfileView() {
        //TODO
    }

    private void switchUser() {
        //TODO
    }

    /**
     * Swaps fragments in the main content view
     */
    private boolean selectItem(String title) {
        if (TextUtils.isEmpty(title))
            return false;
        Fragment lFrag = getFragmentByTag(title);
        if (lFrag != null) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0)
                getSupportFragmentManager().popBackStack();
            showFragment(lFrag);
            // Highlight the selected item, update the title, and close the drawer
            setTitle(title);
            return true;
        }
        getActionByTag(title);
        return false;
    }

    private void showFragment(Fragment lFrag) {
        if (lFrag.getArguments() == null) {
            lFrag.setArguments(new Bundle());
        }
        //lFrag.getArguments().putInt
        // Insert the fragment by replacing any existing fragment

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, lFrag, "TopFragment")
                .setTransition(FragmentTransaction.TRANSIT_NONE)
                .commit();
        mCurrentFrag = lFrag;
        overridePendingTransition(0, 0);
    }

    private Fragment getFragmentByTag(String title) {
        if (title.equals(getString(R.string.menu_title_catalog)))
            return new AnimeCardGridFragment();
        else if (title.equals(getString(R.string.menu_title_favorites)))
            return new FavoritesFragment();
        else if (title.equals(getString(R.string.menu_title_history))) {
            return new HistoryTabFragment();
        } else if (title.equals(getString(R.string.menu_title_library))) {
            return new LibraryFragment();
        }
        return null;
    }

    private void getActionByTag(String pTag) {
        if (pTag.equals(getString(R.string.menu_title_downloads))) {
            startActivity(new Intent(this, DownloadListActivity.class));
        }
    }

    private void setupFavoriteTags() {
        WriteLog.appendLog(TAG, "setupFavoriteTags() called");
        List<FavoriteTag> tagList = MAVApplication.getInstance().getRepository().getFavoriteTags();
        if (tagList == null || tagList.isEmpty()) {
            String[] lTags = getResources().getStringArray(R.array.favorite_tag_list);
            for (int i = 0; i < lTags.length; i++) {
                MAVApplication.getInstance().getRepository().insertFavoriteTag(i, lTags[i], i);
            }
        }
    }
}
