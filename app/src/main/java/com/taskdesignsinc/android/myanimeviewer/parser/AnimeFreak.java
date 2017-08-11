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
import java.net.URLDecoder;
import java.util.ArrayList;

public class AnimeFreak extends Parser {

    public AnimeFreak() {
        name = "Anime Freak";
        isGridViewSupported = false;
    }

    @Override
    public String getServerUrl() {
        return "http://www.animefreak.tv";
    }

    @Override
    public String getFullAnimeListURL() {
        return getServerUrl() + "/book";
    }

    public ArrayList<Anime> getFullAnimeList(String url) {
        WriteLog.appendLog(name + ": getFullAnimeList() " + url);
        ArrayList<Anime> lList = new ArrayList<Anime>();

        Document docdata;
        try {
            docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(mParseTimeOut).get();
            Elements ele = docdata.select("div[class=item-list]").select("ul").select("li").select("a");

            for (int i = 0; i < ele.size(); i++) {
                String lTitle = ele.get(i).text().trim()
                        .replaceAll("\"", "").replace("'", "");
                String lLink = ele.get(i).attr("abs:href");
                if (lTitle == null || lTitle.length() == 0 || lLink == null
                        || lLink.length() == 0) {
                    if (lTitle == null || lTitle.length() == 0)
                        WriteLog.appendLog(name + ": skipped missing title "
                                + lLink);
                    else if (lLink == null || lLink.length() == 0)
                        WriteLog.appendLog(name + ": skipped missing link "
                                + lTitle);
                    else
                        WriteLog.appendLog(name + ": skipped no data ");
                    continue;
                }
                Anime lAnime = new Anime();
                lAnime.setTitle(lTitle);
                lAnime.setUrl(lLink);
                lList.add(lAnime);
            }
            WriteLog.appendLog(name + ": anime loaded " + lList.size() + " from " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lList;
    }

    @Override
    public ArrayList<Anime> getAnimeList(String url) {
        if (TextUtils.isEmpty(url))
            url = "http://www.animefreak.tv/book";
        WriteLog.appendLog(name, "getAnimeList()");
        ArrayList<Anime> lList = new ArrayList<Anime>();

        Document docdata;
        try {
            docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(mParseTimeOut).get();
            Elements ele = docdata.select("div[class=item-list]").select("ul").select("li").select("a");

            for (int i = 0; i < ele.size(); i++) {
                String lTitle = ele.get(i).text().trim()
                        .replaceAll("\"", "").replace("'", "");
                String lLink = ele.get(i).attr("abs:href");
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
            Elements animeDetails = docdata.select("div[id=primary]")
                    .select("div[class=singlepage]")
                    .select("div[class=node]")
                    .select("div[class=content]");

            // Find Title
            String animeTitle = docdata.select("div[id=primary]").select("div[class=singlepage]").select("h1").text();
            lAnime.setTitle(animeTitle);

            String animeCover = animeDetails.select("p > img").attr("abs:src");
            lAnime.setCover(animeCover);

            String detailsText = animeDetails.select("blockquote").select("p").html().replace("<br />", "\n");
            String tempText = animeDetails.select("blockquote").select("p").text();
            for (String text : detailsText.split("\n")) {
                if (text.contains("Genre")) {
                    tempText = text.replace("<strong>", "").replace("</strong>", "").replace("Genre:", "").trim();
                    lAnime.setGenres(tempText);
                } else if (text.contains("Synonyms")) {
                    tempText = text.replace("<strong>", "").replace("</strong>", "").replace("Synonyms:", "").trim();
                    lAnime.setAliases(tempText);
                }
            }
            Elements summaryEle = animeDetails.select("h2 + blockquote").select("p");
            if (summaryEle != null && !summaryEle.isEmpty()) {
                String summary = summaryEle.first().text();
                lAnime.setSummary(summary);
            }

            ArrayList<Episode> lList = new ArrayList<Episode>();
            Elements episodeEles = animeDetails.select("ul[class=menu]")
                    .select("li").select("a");
            for (int i = 0; i < episodeEles.size(); i++) {
                String lTitle = episodeEles.get(i).text().trim()
                        .replaceAll("\"", "").replace("'", "");
                String lLink = episodeEles.get(i).attr("abs:href");
                if (TextUtils.isEmpty(lTitle) || TextUtils.isEmpty(lLink)) {
                    WriteLog.appendLog(name, "episode skipped, invalid data Name: " + lTitle + " or Link: " + lLink);
                    continue;
                }
                Episode lEpisode = new Episode();
                lEpisode.setAnime(lAnime);
                lEpisode.setIndex(i);
                lEpisode.setTitle(lTitle);
                lEpisode.setUrl(lLink);
                lList.add(lEpisode);
            }
            lAnime.setEpisodes(lList);
        } catch (IOException e) {
            WriteLog.appendLogException(name, "getAnimeDetails failed", e);
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
            Elements frameEles = docdata.select("script");
            Elements eles = docdata.select("div[class=content]").select("a");
            String temp = "";
            String tempArray[];
            String tempVideoUrl = "";
            for (int i = 0; i < eles.size(); i++) {
                if (!eles.get(i).hasAttr("onclick"))
                    continue;
                temp = eles.get(i).attr("onclick");
                if (temp.contains("javascript:loadParts('")) {
                    tempArray = temp.replace("javascript:loadParts('", "").split(",");
                    if (tempArray != null && tempArray.length > 0) {
                        temp = URLDecoder.decode(tempArray[0].replace("'", ""), "UTF-8");
                        tempArray = temp.split("\"");
                        for (int j = 0; j < tempArray.length; j++) {
                            temp = tempArray[j];
                            if (temp.contains("src=")) {
                                if (j + 1 < tempArray.length) {
                                    temp = tempArray[j + 1];
                                    tempVideoUrl = temp.trim().substring(0, temp.length() - 1).replace("src=", "").replace("\"", "").replace(" ", "%20");
                                }
                            }
                        }
                    }
                }
                lVideoUrl = tempVideoUrl;
                if (!TextUtils.isEmpty(lVideoUrl) && lVideoUrl.contains(getServerUrl())) {
                    break;
                }
            }
            if (lVideoUrl.contains(getServerUrl())) {
                lVideoUrl = getInternalVideoUrl(lVideoUrl);
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
            Elements scriptEles = docdata.select("script");
            Element script = null;
            String text = "";
            if (scriptEles != null) {
                for (int i = 0; i < scriptEles.size(); i++) {
                    script = scriptEles.get(i);
                    text = script.data();
                    if (!text.contains("var v_src"))
                        continue;
                    String[] scriptText = text.split("\r\n");
                    if (scriptText != null) {
                        for (int j = 0; j < scriptText.length; j++) {
                            if (scriptText[j].contains("file:")) {
                                lVideoUrl = scriptText[j].trim();
                                lVideoUrl = lVideoUrl.substring(lVideoUrl.indexOf("h"), lVideoUrl.lastIndexOf(",") - 1).replace(" ", "%20");
                                break;
                            }
                        }
                    }
                    if (!TextUtils.isEmpty(lVideoUrl))
                        break;
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    @Override
    public String[] getDirectoryLinks() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getDirectoryTitles() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDirectoryURL(int pType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDirectoryURL(int pType, int pPage) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDirectoryURL(String pUrl, int pPage) {
        return null;
    }

    @Override
    public int getDirectoryLowerBound() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getDirectoryUpperBound() {
        // TODO Auto-generated method stub
        return 0;
    }

}
