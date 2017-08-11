package com.taskdesignsinc.android.myanimeviewer.parser;

import android.text.TextUtils;
import android.util.Log;

import com.taskdesignsinc.android.myanimeviewer.model.Anime;
import com.taskdesignsinc.android.myanimeviewer.model.Episode;
import com.taskdesignsinc.android.myanimeviewer.util.WriteLog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class AnimeTv extends Parser {

    public AnimeTv() {
        name = "Watch Anime Online";
        isGridViewSupported = true;
    }

    @Override
    public String getServerUrl() {
        return "https://www2.animetv.to";
    }

    private String[] mDirectoryLinks = {"/search/character=special", "/search/character=A", "/search/character=B", "/search/character=C", "/search/character=D", "/search/character=E", "/search/character=F", "/search/character=G", "/search/character=H", "/search/character=I", "/search/character=J", "/search/character=K", "/search/character=L", "/search/character=M", "/search/character=N", "/search/character=O", "/search/character=P", "/search/character=Q", "/search/character=R", "/search/character=S", "/search/character=T", "/search/character=U", "/search/character=V", "/search/character=W", "/search/character=X", "/search/character=Y", "/search/character=Z"};

    private String[] mDirectoryTitles = {"#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    @Override
    public int getDirectoryLowerBound() {
        return 1;
    }

    @Override
    public int getDirectoryUpperBound() {
        return mDirectoryLinks.length;
    }

    @Override
    public String[] getDirectoryLinks() {
        return mDirectoryLinks;
    }

    @Override
    public String[] getDirectoryTitles() {
        return mDirectoryTitles;
    }

    @Override
    public String getDirectoryURL(int pType) {
        return getServerUrl() + mDirectoryLinks[pType];
    }

    @Override
    public String getDirectoryURL(int pType, int pPage) {
        return getDirectoryURL(pType);
    }

    @Override
    public String getDirectoryURL(String pUrl, int pPage) {
        return null;
    }

    @Override
    public Date getDateFormat(String date) {
        //2009-12-31 16:00:00
        SimpleDateFormat lDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        try {
            return lDateFormat.parse(date);
        } catch (ParseException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return null;
    }

    @Override
    public ArrayList<Anime> getAnimeList(String url) {
        WriteLog.appendLog(name, "getAnimeList( " + url + " )");
        ArrayList<Anime> lList = new ArrayList<Anime>();

        Document docdata;
        try {
            docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(mParseTimeOut).get();
            Elements ele = docdata.select("ul[class=items]").select("li");

            for (int i = 0; i < ele.size(); i++) {
                String lTitle = ele.get(i).select("div[class=name]").text().trim()
                        .replaceAll("\"", "").replace("'", "");
                String lLink = ele.get(i).select("a").attr("abs:href");
                String lCover = ele.get(i).select("div[class=thumb_anime]").attr("style").replace("background: url('", "").replace("');", "");

                if (lTitle == null || lTitle.length() == 0 || lLink == null
                        || lLink.length() == 0) {
                    if (lTitle == null || lTitle.length() == 0)
                        WriteLog.appendLog(name, "skipped missing title "
                                + lLink);
                    else if (lLink == null || lLink.length() == 0)
                        WriteLog.appendLog(name, "skipped missing link "
                                + lTitle);
                    else
                        WriteLog.appendLog(name, "skipped no data ");
                    continue;
                }
                Anime lAnime = new Anime();
                lAnime.setTitle(lTitle);
                lAnime.setUrl(lLink);
                lAnime.setCover(lCover);
                lList.add(lAnime);
            }
            WriteLog.appendLog(name, "anime loaded " + lList.size() + " from " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lList;
    }

    @Override
    public Anime getAnimeDetails(String url) {
        Anime lAnime = new Anime();
        Document docdata;
        try {
            docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(mParseTimeOut).get();
            lAnime.setUrl(docdata.baseUri());
            Elements animeDetails = docdata.select("div[class=container]")
                    .select("div[class=info]");

            // Find Title
            String animeTitle = docdata.select("div[class=main_body]")
                    .select("div[class=right]").select("h1").text();
            lAnime.setTitle(animeTitle);

            String animeCover = docdata
                    .select("div[class=main_body]")
                    .select("div[class=left]").select("img").attr("abs:src");
            lAnime.setCover(animeCover);

            String data;
            for (Element element : docdata.select("div[class=main_body]")
                    .select("div[class=right]").select("p")) {
                data = element.text();
                if (data.contains("Status")) {
                    int status = data.contains("Completed") ? 1 : 0;
                    lAnime.setStatus(status);
                } else if (data.contains("Genre")) {
                    lAnime.setGenres(data.replace("Genre:", "").trim());
                }
            }

            Elements summaryEle = docdata.select("div[class=main_body]")
                    .select("div[class=right]").select("div + p");
            if (summaryEle != null && !summaryEle.isEmpty()) {
                String summary = summaryEle.first().text();
                lAnime.setSummary(summary);
            }

            ArrayList<Episode> lList = new ArrayList<Episode>();
            Elements episodeEles = docdata.select("div[class=list_episode]").select("ul");
            for (int i = episodeEles.size() - 1; i >= 0; i--) {
                Elements episodeListEles = episodeEles.get(i).select("li").select("a");
                for (int j = 0; j < episodeListEles.size(); j++) {
                    String lTitle = episodeListEles.get(j).select("span[class=name]").text().trim()
                            .replaceAll("\"", "").replace("'", "");
                    String lLink = episodeListEles.get(j).attr("abs:href");
                    if (TextUtils.isEmpty(lTitle) || TextUtils.isEmpty(lLink)) {
                        WriteLog.appendLog(name, "episode skipped, invalid data Name: " + lTitle + " or Link: " + lLink);
                        continue;
                    }
                    Episode lEpisode = new Episode();
                    lEpisode.setIndex(j);
                    lEpisode.setTitle(lTitle);
                    lEpisode.setUrl(lLink);
                    lList.add(lEpisode);
                }
            }
            Collections.reverse(lList);
            for (int i = 0; i < lList.size(); i++) {
                lList.get(i).setIndex(i);
            }
            lAnime.setEpisodes(lList);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return lAnime;
    }

    @Override
    public ArrayList<Episode> getEpisodeList() {
        ArrayList<Episode> lList = new ArrayList<Episode>();

        Document docdata;
        try {
            docdata = Jsoup.connect("http://www.animeseason.com/anime-list/").userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(mParseTimeOut).get();
            Elements ele = docdata.select("div[class=content_bloc]").select("table")
                    .select("tr");

            Elements tdEles = null;
            for (int i = 0; i < ele.size(); i++) {
                tdEles = ele.get(i).select("td");
                String lTitle = "";
                String lLink = "";
                if (tdEles.size() == 2) {
                    lTitle = tdEles.get(0).text().trim()
                            .replaceAll("\"", "").replace("'", "");
                    lLink = tdEles.get(0).attr("abs:href");
                } else {
                    lTitle = tdEles.get(1).text().trim()
                            .replaceAll("\"", "").replace("'", "");
                    lLink = tdEles.get(1).attr("abs:href");
                }
                if (lTitle == null || lTitle.length() == 0 || lLink == null
                        || lLink.length() == 0) {
                    if (lTitle == null || lTitle.length() == 0)
                        WriteLog.appendLog(name, "skipped missing title "
                                + lLink);
                    else if (lLink == null || lLink.length() == 0)
                        WriteLog.appendLog(name, "skipped missing link "
                                + lTitle);
                    else
                        WriteLog.appendLog(name, "skipped no data ");
                    continue;
                }
                Episode lEpisode = new Episode();
                lEpisode.setTitle(lTitle);
                lEpisode.setUrl(lLink);
                lEpisode.setId(lLink.hashCode());
                lList.add(lEpisode);
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lList;
    }

    @Override
    public String getEpisodeVideo(String url) {
        String lVideoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(getUserAgent())
                    .referrer(getServerUrl())
                    .timeout(Parser.getParseTimeOut()).get();
            Element scriptElement = docdata.select("head").select("script").last();
            Elements downloadEle = docdata.select("li[class=bg-download]").select("a");
            if (downloadEle != null && !downloadEle.isEmpty()) {
                String downloadLink = downloadEle.get(0).attr("href");
                if (!TextUtils.isEmpty(downloadLink)) {
                    lVideoUrl = getInternalVideoUrlAniUploader(downloadLink);
                }
            } else {
                String temp = "";
                String tempArray[];
                String tempVideoUrl = "";
                if (scriptElement != null) {
                    temp = scriptElement.data();
                    if (!TextUtils.isEmpty(temp)) {
                        tempArray = temp.split("\n");
                        for (String text : tempArray) {
                            if (text.contains("var stream")) {
                                lVideoUrl = text.substring(text.indexOf("\"") + 1, text.lastIndexOf("\""));
                            }
                        }
                    }
                }
                if (lVideoUrl.contains("cdn-stream")) {
                    lVideoUrl = getInternalVideoUrl(lVideoUrl);
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    private String getInternalVideoUrlAniUploader(String url) {
        WriteLog.appendLog("getInternalVideoUrlAniUploader(" + url + ") called");
        String lVideoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(getUserAgent())
                    .referrer(getServerUrl())
                    .timeout(Parser.getParseTimeOut()).get();
            Elements scriptEles = docdata.select("div[class=dowload]").select("a");
            Element script = null;
            String text = "";
            String providerUrl = "";
            if (scriptEles != null) {
                for (int i = 0; i < scriptEles.size(); i++) {
                    script = scriptEles.get(i);
                    if (script != null && script.hasAttr("href")) {
                        providerUrl = script.attr("href");
                        lVideoUrl = getVideoFromProvider(providerUrl);
                        if (!TextUtils.isEmpty(lVideoUrl))
                            break;
                    }
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    private String getInternalVideoUrl(String url) {
        WriteLog.appendLog("getInternalVideoUrl(" + url + ") called");
        String lVideoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(getUserAgent())
                    .referrer(getServerUrl())
                    .timeout(Parser.getParseTimeOut()).get();
            Elements scriptEles = docdata.select("div[class=videocontent]").select("video").select("source");
            Element script = null;
            String text = "";
            if (scriptEles != null) {
                for (int i = 0; i < scriptEles.size(); i++) {
                    script = scriptEles.get(i);
                    if (script != null && script.hasAttr("src"))
                        lVideoUrl = script.attr("src");
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }
}
