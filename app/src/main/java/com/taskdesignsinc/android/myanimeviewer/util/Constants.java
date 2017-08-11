package com.taskdesignsinc.android.myanimeviewer.util;

import com.taskdesignsinc.android.myanimeviewer.BuildConfig;
import com.taskdesignsinc.android.myanimeviewer.R;

/**
 * Created by salma on 7/21/2017.
 */

public class Constants {

    public static final int ANIME_COMPLETED = 1;

    public static class RequestCodes {
        public static final int LOGIN = 71245;
    }

    public static class ProfileCodes {
        public static final int PROFILE_ADD_ACCOUNT = 13200;
        public static final int PROFILE_VIEW = 13202;
        public static final int PROFILE_LOGOUT = 13203;
    }

    public static class Intents {
        public static final String ANIME_UPDATED = BuildConfig.APPLICATION_ID + ".ANIME_UPDATED";
        public static final String EPISODE_PARSED = BuildConfig.APPLICATION_ID + ".EPISODE_PARSED";
        public static final String EPISODE_DOWNLOAD = BuildConfig.APPLICATION_ID + ".EPISODE_DOWNLOAD";
    }

    public static class DownloadIntents {

        public static final String TYPE = "type";
        public static final String SUBTYPE = "subtype";
        public static final String PROCESS_SPEED = "process_speed";
        public static final String PROCESS_PROGRESS = "process_progress";
        public static final String URL = "url";
        public static final String TITLE = "mTitle";
        public static final String PATH = "path";
        public static final String FILENAME = "filename";
        public static final String ERROR_CODE = "error_code";
        public static final String ERROR_INFO = "error_info";
        public static final String IS_PAUSED = "is_paused";

        public class Types {

            public static final int PROCESS = 0;
            public static final int COMPLETE = 1;

            public static final int START = 2;
            public static final int PAUSE = 3;
            public static final int DELETE = 4;
            public static final int CONTINUE = 5;
            public static final int ADD = 6;
            public static final int STOP = 7;
            public static final int ERROR = 9;

            public static final int PAUSE_ALL = 10;
            public static final int DELETE_ALL = 11;
            public static final int CONTINUE_ALL = 12;
            public static final int COMPLETE_ALL = 13;
        }

        public class SubTypes {
            public static final int DOWNLOADING = 0;
            public static final int COMPLETED = 1;
            public static final int QUEUED = 2;
            public static final int PAUSED = 3;
        }
    }

    public static final int HOME_SCREEN_CATALOG = 0;
    public static final int HOME_SCREEN_LIBRARY = 1;
    public static final int HOME_SCREEN_FAVORITES = 2;
    public static final int HOME_SCREEN_HISTORY = 3;

    // App Related
    public static final String KEY_USER_AGREEMENT = "user_agreement";
    public static final String KEY_SOURCE_FILTERS = "source_filter";
    public static final String KEY_ANIME_SOURCE = "anime_source";
    public static final String KEY_ANIME_SOURCE_URL = "anime_source_url";
    public static final String KEY_ANIME_SOURCE_LANG = "anime_source_lang";
    public static final String KEY_HOME_SCREEN_TYPE = "home_screen_type";
    public static final String KEY_SMART_HOME_SCREEN = "smart_home_screen";

    public static final String KEY_SYS_UNLOCK_CODE = "sys_unlock_code";
    public static final String KEY_SEARCH_FILTER_BY_LANG = "sys_search_filter_by_lang";

    public static final String USE_DEFAULT_ANIME_SOURCE = "use_default_anime_source";
    public static final int DEFAULT_ANIME_SOURCE = 0;

    public static final int DISPLAY_TYPE_GRID = 0;
    public static final int DISPLAY_TYPE_LIST_NO_IMAGES = 1;
    public static final int DISPLAY_TYPE_LIST = 2;
    public static final int DISPLAY_TYPE_LIST_LARGE = 3;
    public static final String KEY_GRID_COLUMN_COUNT = "grid_column_count";
    public static final String KEY_LIST_COLUMN_COUNT = "list_column_count";
    public static final String KEY_SHOW_CATALOG_AS_GRID = "catalog_as_grid";
    public static final String KEY_SHOW_FAVORITES_AS_GRID = "favorites_as_grid";

    public static final String KEY_USE_SERVER_DATABASE = "use_server_database";
    public static final String KEY_PARSE_SERVER = "parse_server";

    public static final String LOAD_TYPE = "load_type";
    public static final String GENRE_TYPE = "genre_type";
    public static final String DIRECTORY_URL = "directory_url";

    public static final String KEY_EPISODE_DEFAULT_SORT = "episode_default_sort";
    public static final String KEY_EPISODE_HISTORY = "episode_history";
    public static final String KEY_EPISODE_VIEWED_COLOR = "episode_viewed_color";
    public static final String KEY_EPISODE_UNVIEWED_COLOR = "episode_unviewed_color";
    public static final String KEY_EPISODE_LAST_VIEWED_COLOR = "episode_last_viewed_color";
    public static final String KEY_EPISODE_SORT_BEFORE_DOWNLOAD = "episode_sort_before_download";
    public static final String KEY_EPISODE_SHOW_WARNING = "episode_show_warning";

    // Library Specific
    public static final String KEY_LIBRARY_DISPLAY_TYPE = "library_display_type";
    public static final String KEY_LIBRARY_FORCE2SD = "library_data_extsd";
    public static final String KEY_LIBRARY_PATH = "library_path";
    public static final String KEY_LIBRARY_SHOW_COVERS = "library_show_covers";
    public static final String KEY_LIBRARY_SHOW_DATA = "library_show_data";
    public static final String KEY_LIBRARY_SHOW_BACKUPS = "library_show_backups";

    // Favorites Specific
    public static final String KEY_FAVORITES_SORT = "favorites_sort";
    public static final String KEY_FAVORITES_DISPLAY_TYPE = "favorites_display_type";
    public static final String KEY_FAVORITES_UPDATE_NOTIFICATION = "favorites_update_notification";

    public static final String KEY_DOWNLOAD_WIFI_ONLY = "sys_wifi_download";

    public static boolean USE_RGB565 = false;
    public static final String KEY_LOADER_TYPE = "loader_type";
    public static final String KEY_LOADER_IMAGE_QUALITY_HIGH = "loader_image_quality_high";

    public static final String ANIME_ID = "anime_id";
    public static final String ANIME_TITLE = "anime_title";
    public static final String ANIME_URL = "anime_url";
    public static final String ANIME_PATH = "anime_path";
    public static final String EPISODE_ID = "episode_id";
    public static final String EPISODE_TITLE = "episode_title";
    public static final String EPISODE_URL = "episode_url";
    public static final String EPISODE_PATH = "episode_path";
    public static final String EPISODE_VIDEO_URL = "episode_video_url";
    public static final String EPISODE_POSITION = "episode_position";
    public static final String EPISODE_START_POSITION = "episode_startPosition";

    public static final String ANIME_PREV_PATH_LIST = "anime_prev_path_list";

    public static final int UPDATE_NOTIFICATION_NONE = 0;
    public static final int UPDATE_NOTIFICATION_ONCE = 1;
    public static final int UPDATE_NOTIFICATION_PER_MANGA = 2;


    public static final int RibbonDrawable[] = {
            R.drawable.ribbon0,
            R.drawable.ribbon1,
            R.drawable.ribbon2,
            R.drawable.ribbon3,
            R.drawable.ribbon4,
            R.drawable.ribbon5,
            R.drawable.ribbon6,
            R.drawable.ribbon7,
            R.drawable.ribbon8,
            R.drawable.ribbon9,
            R.drawable.ribbon10,
            R.drawable.ribbon11,
            R.drawable.ribbon12,
            R.drawable.ribbon13,
            R.drawable.ribbon14

    };

    public static final int SELECT_SINGLE = 0;
    public static final int SELECT_MULTIPLE = 1;
    public static final int SELECT_ALL = 2;

    public static final String[] SELECTION_CHOICES = {
            "Single",
            "Multiple",
            "All"
    };
}
