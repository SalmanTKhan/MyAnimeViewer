package com.taskdesignsinc.android.myanimeviewer.provider;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.taskdesignsinc.android.myanimeviewer.BuildConfig;
import com.taskdesignsinc.android.myanimeviewer.MAVApplication;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.parser.Parser;
import com.taskdesignsinc.android.myanimeviewer.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnimeProvider extends ContentProvider {
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".AnimeProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/anime");
    //public final static int MODE = DATABASE_MODE_QUERIES;

    private static final String[] COLUMN_NAMES = new String[]{"_id",
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA};
    private static final int SEARCH_SUGGEST = 3;

    public AnimeProvider() {
        //setupSuggestions(AUTHORITY, MODE);
    }

    private static UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY,
                SEARCH_SUGGEST);
        uriMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String query = null;
        if (uri.getPathSegments().size() > 1) {
            query = uri.getLastPathSegment().toLowerCase();
        }
        return getSuggestions(query, projection);
    }

    private Cursor getSuggestions(String query, String[] projection) {
        String processedQuery = query == null ? "" : query.toLowerCase();
        String queryProcessed = processedQuery.replace("?limit=50", "");
        SharedPreferences lPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        Parser lParser = Parser.getExistingInstance(lPrefs.getInt(Constants.KEY_ANIME_SOURCE, 0));
        String serverUrl = lParser.getName().contains("Kiss Anime") ? lParser.getServerUrl().split("\\.")[0] : lParser.getServerUrl().split("\\.")[1];
        serverUrl = ((lParser.getName().contains("Anime Here") || lParser.getName().contains("Nine Anime")) ? (lParser.getServerUrl().replace("http://", "").split("\\.")[0] + ".") : "") + serverUrl;
        List<Anime> animeList = null;
        List<Anime> tempAnimeList = null;
        if (!TextUtils.isEmpty(query)) {
            //animeList = MAVApplication.getInstance().getRepository().getAnimeListByTitle(query);
            tempAnimeList = MAVApplication.getInstance().getRepository().getAnimeListByTitle(query, serverUrl);
        } else {
            tempAnimeList = MAVApplication.getInstance().getRepository().getAnimeList();
        }
        boolean checkLang = lPrefs.getBoolean(Constants.KEY_SEARCH_FILTER_BY_LANG, true);
        if (animeList != null)
            for (int i = 0; i < tempAnimeList.size(); i++) {
                if (Parser.isValidSource(Parser.getExistingInstance(Parser.getTypeByURL(tempAnimeList.get(i).getUrl())))) {
                    if (checkLang) {
                        if (getContext().getString(Parser.getExistingInstance(Parser.getTypeByURL(tempAnimeList.get(i).getUrl())).getLanguageResId()).equals(
                                getContext().getString(lParser.getLanguageResId())))
                            animeList.add(tempAnimeList.get(i));
                    } else
                        animeList.add(tempAnimeList.get(i));
                }
            }
        else if (tempAnimeList != null) {
            animeList = new ArrayList<Anime>();
            for (int i = 0; i < tempAnimeList.size(); i++) {
                if (Parser.isValidSource(Parser.getExistingInstance(Parser.getTypeByURL(tempAnimeList.get(i).getUrl())))) {
                    if (checkLang) {
                        if (getContext().getString(Parser.getExistingInstance(Parser.getTypeByURL(tempAnimeList.get(i).getUrl())).getLanguageResId()).equals(
                                getContext().getString(lParser.getLanguageResId())))
                            animeList.add(tempAnimeList.get(i));
                    } else
                        animeList.add(tempAnimeList.get(i));
                }
            }
        }
        Collections.sort(animeList, Anime.Order.ByNameAZ);
        MatrixCursor cursor = new MatrixCursor(COLUMN_NAMES);
        if (animeList != null) {
            long id = 0;
            for (Anime lWord : animeList) {
                cursor.addRow(columnValuesOfWord(id++, lWord));
            }
        } else {
            cursor.addRow(new Object[]{0, // _id
                    "No Results", // text1
                    "Report if this occurs on every search", // text2
                    "salmantkhan@gmail.com", // intent_data (included when clicking on
                    // item)
            });
        }

        return cursor;
    }

    private Object[] columnValuesOfWord(long id, Anime anime) {
        Parser lParser = Parser.getExistingInstance(Parser.getTypeByURL(anime.getUrl()));
        return new Object[]{id, // _id
                anime.getTitle(), // text1
                getContext().getString(lParser.getLanguageResId()) + " - " + lParser.getName(), // text2
                anime.getUrl(), // intent_data (included when clicking on
                // item)
        };
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

}