package com.taskdesignsinc.android.myanimeviewer.parser;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.annotations.Expose;
import com.taskdesignsinc.android.myanimeviewer.R;
import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public abstract class Parser {

    private static Parser instance = null;

    public String name;
    public boolean hasCatalog = true;
    public boolean isCatalogFilterable = true;
    public boolean isCatalogSortSupported = false;
    public boolean isLatestSortSupported = false;
    public boolean isShowAllSupported = false;
    public boolean isGridViewSupported = false;
    public boolean isGenreSortSupported = false;
    public boolean isCloudFlareDDOSEnabled = false;
    public boolean isCloudFlareDDOSPassed = false;
    public String mCookies = null;
    private static final int mHardCount = 1;

    public static String openloadLogin = "92c9a7c7a6fce945";
    public static String openloadKey = "nbqBkkQI";

    @Expose
    public String mCustomUserAgent = "";

    public int mAnimeCount = -1;
    private static int parserType = 0;

    public boolean isPremium = false;

    public static int mParseTimeOut = 120000;
    public static int mConnectTimeOut = 60000;

    protected static ArrayList<Parser> mParserArray = new ArrayList<Parser>();

    static {
        if (mParserArray.size() == 0) {
            Parser lParser = null;
            for (int i = 0; i < mHardCount; i++) {
                lParser = getNewInstance(i);
                if (lParser != null) {
                    lParser.mAnimeCount = getCountByURL(lParser.getServerUrl());
                    mParserArray.add(lParser);
                }
            }
        }
        Collections.sort(mParserArray, Order.ByNameAZ);
    }

    public static int getParserCount() {
        if (mParserArray != null)
            return mParserArray.size();
        else
            return -1;
    }

    OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(mParseTimeOut, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true).build();

    private static Parser getNewInstance(int pType) {
        switch (pType) {
            default:
                return new GoGoAnime();
        }
    }

    /**
     * @param pURL The type by url of a parser, if the type is different from the current parser, it replaces the old one.
     * @return A singleton instance of the parser
     */
    public static Parser getInstance(String pURL) {
        int lType = getTypeByURL(pURL);
        if (instance == null) {
            instance = getExistingInstance(lType);
        }
        return instance;
    }

    /**
     * @param pType The type of parser, if the type is different from the current parser, it replaces the old one.
     * @return A singleton instance of the parser
     */
    public static Parser getInstance(int pType) {
        if (instance == null || parserType != pType) {
            parserType = pType;
            instance = getParser();
        }
        WriteLog.appendLog("Parser Instance " + pType + " " + instance);
        return instance;
    }

    /**
     * @return The current parser, as set by {@link #getInstance(int)}
     */
    private static Parser getParser() {
        if (parserType > -1 && parserType < mParserArray.size())
            return getExistingInstance(parserType);
        else {
            WriteLog.appendLog("getParser() invalid parser id " + parserType);
            return getExistingInstance(0);
        }
    }

    public static ArrayList<Parser> getParserArray() {
        return mParserArray;
    }

    public static int getParseTimeOut() {
        return mParseTimeOut;
    }

    public static void setParseTimeOut(int mParseTimeOut) {
        Parser.mParseTimeOut = mParseTimeOut;
    }

    public String getUserAgent() {
        return TextUtils.isEmpty(mCustomUserAgent) ? mUserAgent : mCustomUserAgent;
    }

    public static String mUserAgent = "Mozilla/5.0 (Linux; Android 4.2.2; en-us; SAMSUNG GT-I9195 Build/JDQ39) AppleWebKit/535.19 (KHTML, like Gecko) Version/1.0 Chrome/18.0.1025.308 Mobile Safari/535.19";

    public abstract String getServerUrl();

    public abstract ArrayList<Anime> getAnimeList(String url);

    public abstract Anime getAnimeDetails(String url);

    public abstract String getEpisodeVideo(String url);

    public abstract String[] getDirectoryLinks();

    public abstract String[] getDirectoryTitles();

    public abstract String getDirectoryURL(int pType);

    public abstract String getDirectoryURL(int pType, int pPage);

    public abstract String getDirectoryURL(String pUrl, int pPage);

    public abstract int getDirectoryLowerBound();

    public abstract int getDirectoryUpperBound();

    public boolean getIsPremium() {
        return isPremium;
    }

    public int getLanguageResId() {
        return R.string.english;
    }

    @Deprecated
    public ArrayList<Episode> getEpisodeList() {
        return null;
    }

    public Date getDateFormat(String date) {
        return null;
    }

    public String getRandomAnimeURL() {
        return null;
    }

    public ArrayList<Anime> getFullAnimeList(String pURL) {
        return null;
    }

    public String getFullAnimeListURL() {
        return null;
    }

    public String getLatestAnimeListURL() {
        return null;
    }

    public ArrayList<Anime> getLatestAnimeList(String pURL) {
        return null;
    }

    public static String getNameByUrl(String pURL) {
        if (pURL.contains("animefreak")) {
            return "Anime Freak";
        } else if (pURL.contains("watchanime.me")) {
            return "Watch Anime";
        } else if (pURL.contains("animeshow")) {
            return "Anime Show";
        } else if (pURL.contains("animetv.to")) {
            return "Anime Tv";
        } else if (pURL.contains("gogoanime.io")) {
            return "GoGo Anime";
        }
        return "Unsupported " + pURL;
    }

    public static int getTypeByURL(String pURL) {
        for (int i = 0; i < mParserArray.size(); i++) {
            if (pURL.contains(mParserArray.get(i).getServerUrl()
                    .replace("https://", "")
                    .replace("http://", "")
                    .replace("www.", ""))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param pURL       The url associated with a manga to find proper parser type for.
     * @param pLangResID
     * @return The type of parser which supports it.
     */
    public static int getTypeByURL(String pURL, int pLangResID) {
        for (int i = 0; i < mParserArray.size(); i++) {
            if (pURL.contains(mParserArray.get(i).getServerUrl().replace("http://", "").replace("www.", ""))
                    && mParserArray.get(i).getLanguageResId() == pLangResID) {
                return i;
            }
        }

        return -1;
    }

    public static Parser getExistingInstance(String typeByUrl) {
        return mParserArray.get(getTypeByURL(typeByUrl));
    }

    public static Parser getExistingInstance(int parserId) {
        return mParserArray.get(parserId);
    }

    public static int getCountByURL(String pURL) {
        if (pURL.contains("anime-joy.tv") || pURL.contains("animejoy.tv")) {
            return 2478;
        } else if (pURL.contains("animefreak.tv")) {
            return 1856;
        } else if (pURL.contains("watchanimesonline.net")) {
            return 3653;
        }
        return -1;
    }

    public String getName() {
        return name;
    }

    public static enum Order implements Comparator<Parser> {
        ByNameAZ() {
            @Override
            public int compare(Parser pParser1, Parser pParser2) {

                String lTitle1 = pParser1.name.toLowerCase();
                String lTitle2 = pParser2.name.toLowerCase();

                // ascending order
                return lTitle1.compareTo(lTitle2);
            }
        },
        BySizeH2L() {
            @Override
            public int compare(Parser pParser1, Parser pParser2) {
                int lPriority1 = getCountByURL(pParser1.getServerUrl());
                int lPriority2 = getCountByURL(pParser2.getServerUrl());

                if (lPriority1 == lPriority2)
                    return 0;
                else if (lPriority1 > lPriority2)
                    return 1;
                else
                    return -1;
            }
        }
    }

    public static boolean isDeadSource(Parser existingInstance) {
        // TODO Auto-generated method stub
        return false;
    }

    protected String getVideoFromProviderByName(String name, String data) {
        if (name.equals("Estream")) {
            return getEstreamToVideo("https://estream.to/"+data);
        } else if (name.equals("Yourupload")) {
            return getYourUploadVideo("https://yourupload.com/embed/"+data);
        } else if (name.equals("Mp4Upload"))
            return getMP4UploadVideo("https://mp4upload.com/"+data);
        //yourupload.com/play/1332473.mp4?busted=1501129123931
        return "";
    }

    public String getVideoFromProvider(String url) {
        WriteLog.appendLog(name, "Video Provider: " + url);
        if (url.contains("auengine")) {
            return getAuEngineVideo(url);
        } else if (url.contains("mp4upload.com")) {
            return getMP4UploadComVideo(url);
        } else if (url.contains("mp4upload")) {
            return getMP4UploadVideo(url);
        } else if (url.contains("safeupload")) {
            return getSafeUploadVideo(url);
        } else if (url.contains("openload.co")) {
            //return getOpenloadCoVideo(url);
        } else if (url.contains("estream.to")) {
            return getEstreamToVideo(url);
        } else if (url.contains("streamango.com")) {
            return getStreamangoVideo(url);
        } else if (url.contains("vidstreaming.io")) {
            return getVidStreamingVideo(url);
        }
        WriteLog.appendLog("Unsupported Video Provider: " + url);
        return url;
    }

    private String getVidStreamingVideo(String url) {
        String videoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(getUserAgent())
                    .referrer("http://www.google.com")
                    .timeout(Parser.getParseTimeOut()).get();
            Elements eles = docdata.select("div[class=videocontent]").select("video[id=my-video-player]").select("source");
            String tempUrl = "";
            if (eles != null && !eles.isEmpty()) {
                for (Element ele : eles) {
                    tempUrl = ele.attr("abs:src");
                    if (!TextUtils.isEmpty(tempUrl))
                        videoUrl = tempUrl;
                }
            } else {
                eles = docdata.select("div[class=videocontent]").select("iframe");
                if (eles != null && !eles.isEmpty()) {
                    videoUrl = eles.attr("src");
                    return getVideoFromProvider(videoUrl);
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return videoUrl;
    }

    public static String getOpenloadCoVideo(String url) {
        String fileId = "";
        String lVideoUrl = "";
        if (url.startsWith("https://openload.co/f/")) {
            String[] splitUrl = url.replace("https://openload.co/f/", "").split("/");
            fileId = splitUrl[0];
        }
        if (url.startsWith("https://openload.co/embed/")) {
            String[] splitUrl = url.replace("https://openload.co/embed/", "").split("/");
            fileId = splitUrl[0];
        }
        /*
        DownloadTicket ticket = OpenloadManager.getInstance().getDownloadTicket(fileId);
        if (ticket.getStatus() == 200) {
            DownloadLink link = OpenloadManager.getInstance().getDownloadLink(fileId, ticket);
            if (link != null) {
                if (link.getStatus() == 200) {
                    lVideoUrl = link.getResult().getUrl();
                }
            }
        }
        */
        return lVideoUrl;
    }

    public String getMP4UploadComVideo(String url) {
        String lVideoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(Parser.getParseTimeOut()).get();
            Elements eles = null;
            if (!url.contains("embed")) {
                eles = docdata.select("div[class=leftcol]").select("iframe");
                if (eles != null && !eles.isEmpty()) {
                    url = eles.get(0).attr("src");
                    return getMP4UploadComVideo(url);
                }
            } else {
                eles = docdata.select("div[id=player_code]").select("script");
                lVideoUrl = eles.get(0).html();
                if (!TextUtils.isEmpty(lVideoUrl)) {
                    final Pattern pattern = Pattern.compile("\"file\": \"(.+?)\"");
                    final Matcher matcher = pattern.matcher(lVideoUrl);
                    matcher.find();
                    lVideoUrl = matcher.group(1);
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    public String getAuEngineVideo(String url) {
        String lVideoUrl = "";
        Document docdata;
        try {
            docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(Parser.getParseTimeOut()).get();
            Elements ele = docdata.select("script");
            Pattern p = Pattern.compile("http(s)?://([\\w-]+.)+[\\w-]+(/[\\w- ./?%&=])?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

            String innerHtml = "";
            String videoUrl = "";
            for (int i = 0; i < ele.size(); i++) {
                innerHtml = ele.get(i).html();
                if (!innerHtml.contains("file"))
                    continue;
                Matcher m = p.matcher(innerHtml);
                while (m.find()) {
                    videoUrl = m.group();
                    if (videoUrl.contains(".mp4")) {
                        WriteLog.appendLog("" + " matcher found url " + m.group());
                        lVideoUrl = videoUrl;
                    } else {
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    public String getMP4UploadVideo(String url) {
        String lVideoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(Parser.getParseTimeOut()).get();
            Elements eles = docdata.select("script + script");
            if (eles != null && !eles.isEmpty()) {
                lVideoUrl = eles.get(eles.size() - 2).html();
                if (!TextUtils.isEmpty(lVideoUrl)) {
                    final Pattern pattern = Pattern.compile("'file': '(.+?)'");
                    final Matcher matcher = pattern.matcher(lVideoUrl);
                    matcher.find();
                    lVideoUrl = matcher.group(1);
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    public String getSafeUploadVideo(String url) {
        //http://www.safeupload.org/getembed/f93fb4096e0875979215c0307dd53ff5
        String lVideoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(Parser.getParseTimeOut()).get();
            Elements eles = docdata.select("body").select("div + script");
            if (eles != null && !eles.isEmpty()) {
                lVideoUrl = eles.get(eles.size() - 2).html();
                if (!TextUtils.isEmpty(lVideoUrl)) {
                    final Pattern pattern = Pattern.compile("'file': '(.+?)'");
                    final Matcher matcher = pattern.matcher(lVideoUrl);
                    matcher.find();
                    lVideoUrl = matcher.group(1);
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    public String getEstreamToVideo(String url) {
        String lVideoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(Parser.getParseTimeOut()).get();
            Elements eles = null;
            if (!url.contains("embed")) {
                eles = docdata.select("div[class=leftcol]").select("iframe");
                if (eles != null && !eles.isEmpty()) {
                    url = eles.get(0).attr("src");
                    return getMP4UploadComVideo(url);
                }
            } else {
                eles = docdata.select("video").select("source");
                for (Element element : eles) {
                    if (element.hasAttr("type") && element.attr("type").startsWith("video")) {
                        return element.attr("abs:src");
                    }
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    public String getStreamangoVideo(String url) {
        String lVideoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(Parser.getParseTimeOut()).get();
            Elements eles = null;
            if (!url.contains("embed")) {
                eles = docdata.select("div[class=leftcol]").select("iframe");
                if (eles != null && !eles.isEmpty()) {
                    url = eles.get(0).attr("src");
                    return getMP4UploadComVideo(url);
                }
            } else {
                eles = docdata.select("div[class=videocontainer] + script");
                Pattern p = Pattern.compile("http(s)?://([\\w-]+.)+[\\w-]+(/[\\w- ./?%&=])?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

                String innerHtml = "";
                String videoUrl = "";
                for (int i = 0; i < eles.size(); i++) {
                    innerHtml = eles.get(i).html();
                    String[] innerHtmlSplit = innerHtml.split("\n");
                    for (String s : innerHtmlSplit) {
                        //https://streamango.com/v/d/lnorrkmcdnacnqsa~1500704504~108.21.0.0~uJnSFqaq/720
                        if (!s.contains("video/mp4"))
                            continue;
                        s = s.replace("srces.push({type:\"video/mp4\",src:\"", "https:").trim();
                        lVideoUrl = s.substring(0, s.lastIndexOf("\""));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    private String getYourUploadVideo(String url) {
        if (url.contains(".cdn."))
            return url;
        String lVideoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(getUserAgent())
                    .referrer("https://www.yourupload.com")
                    .timeout(Parser.getParseTimeOut()).get();
            Elements eles = null;
            if (url.contains("embed")) {
                eles = docdata.select("video").select("source");
                for (Element element : eles) {
                    if (element.hasAttr("type") && element.attr("type").startsWith("video")) {
                        return element.attr("abs:src");
                    }
                }
            } else {
                eles = docdata.select("video").select("source");
                for (Element element : eles) {
                    if (element.hasAttr("type") && element.attr("type").startsWith("video")) {
                        return element.attr("abs:src");
                    }
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    public static boolean isValidSource(Parser lParser) {
        return true;
        //return !lParser.getIsPremium();
    }

    public Document load(String url) throws IOException {
        Request request = new Request.Builder().url(url)
                .build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful())
            return parseHtml(response.body().string(), response.request().url().toString());
        else {
            WriteLog.appendLog(name + ": load(" + url + ") didn't respond with a proper response " + response.code());
            return loadDocument(url);
        }
    }

    public Document load(String url, boolean userCookies) throws IOException {
        Request request = new Request.Builder().url(url)
                .header("User-Agent", getUserAgent())
                .addHeader("Referer", getServerUrl()).build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful())
            return parseHtml(response.body().string(), response.request().url().toString());
        else {
            WriteLog.appendLog(name + ":load(" + url + ") didn't respond with a proper response " + response.code());
            return loadDocument(url);
        }
    }

    public Document load(String url, String referral) throws IOException {
        Request request = new Request.Builder().url(url)
                .header("User-Agent", getUserAgent())
                .addHeader("Referer", referral).build();
        Response response = client.newCall(request).execute();
        if (response.isSuccessful())
            return parseHtml(response.body().string(), response.request().url().toString());
        else {
            WriteLog.appendLog(name + ":load(" + url + ") didn't respond with a proper response " + response.code());
            return loadDocument(url);
        }
    }

    public Document loadDocument(String pURL) throws IOException {
        Document docdata = Jsoup.connect(pURL).userAgent(TextUtils.isEmpty(mCustomUserAgent) ? mUserAgent : mCustomUserAgent).referrer("http://www.google.com").timeout(mParseTimeOut).get();
        return docdata;
    }

    public Document parseHtml(String body, String url) {
        return Jsoup.parse(body, url);
    }

    public Map<String, String> getCategories() {
        return Collections.EMPTY_MAP;
    }
}
