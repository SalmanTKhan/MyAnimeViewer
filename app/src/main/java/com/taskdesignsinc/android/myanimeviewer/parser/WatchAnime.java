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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WatchAnime extends Parser {

    public WatchAnime() {
        name = "Watch Anime";
        isGridViewSupported = true;
        isGenreSortSupported = true;
        hasCatalog = false;
    }

    @Override
    public String getServerUrl() {
        return "http://watchanime.me";
    }

    @Override
    public Map<String, String> getCategories() {
        String url = getServerUrl() + "/anime-list";
        WriteLog.appendLog(name, "getCategories( " + url + " )");
        HashMap<String, String> categoryMap = new HashMap<>();
        Document docdata;
        try {
            docdata = Jsoup.connect(url).userAgent(Parser.mUserAgent)
                    .referrer("http://www.google.com").timeout(Parser.mParseTimeOut)
                    .get();
            Elements ele = docdata.select("div[class=tags]").select("ul[class=scroll genre]").select("li").select("a");
            ArrayList<String> lList = new ArrayList<>();
            for (int i = 0; i < ele.size(); i++) {
                //if (ele.get(i).text().trim().length() == 1)
                //continue;
                lList.add("\"" + ele.get(i).text().trim() + "\"");
            }
            //Collections.sort(lList);
            System.out.println(lList.toString());
            lList.clear();
            for (int i = 0; i < ele.size(); i++) {
                //if (ele.get(i).text().trim().length() == 1)
                //continue;
                lList.add("\"" + ele.get(i).attr("href").replace(getServerUrl(), "") + "\"");
            }
            for (int i = 0; i < ele.size(); i++) {
                categoryMap.put(ele.get(i).attr("href").replace("", ""), ele
                        .get(i).text().trim());
            }
            //Collections.sort(lList);
            System.out.println(lList.toString());
        } catch (IOException e) {
            WriteLog.appendLogException(name, "failed to load categories", e);
        }
        return categoryMap;
    }

    @Override
    public String getLatestAnimeListURL() {
        return getServerUrl();
    }

    @Override
    public String getFullAnimeListURL() {
        return getServerUrl() + "/animelist";
    }

    private String[] mDirectoryLinks = {"/anime-list?cat_id=43", "/anime-list?cat_id=44", "/anime-list?cat_id=45", "/anime-list?cat_id=46", "/anime-list?cat_id=47", "/anime-list?cat_id=48", "/anime-list?cat_id=49", "/anime-list?cat_id=50", "/anime-list?cat_id=51", "/anime-list?cat_id=52", "/anime-list?cat_id=53", "/anime-list?cat_id=54", "/anime-list?cat_id=55", "/anime-list?cat_id=56", "/anime-list?cat_id=57", "/anime-list?cat_id=58", "/anime-list?cat_id=59", "/anime-list?cat_id=60", "/anime-list?cat_id=61", "/anime-list?cat_id=62", "/anime-list?cat_id=63", "/anime-list?cat_id=64", "/anime-list?cat_id=65", "/anime-list?cat_id=66", "/anime-list?cat_id=67", "/anime-list?cat_id=68", "/anime-list?cat_id=69", "/anime-list?cat_id=70", "/anime-list?cat_id=71", "/anime-list?cat_id=72", "/anime-list?cat_id=73", "/anime-list?cat_id=74", "/anime-list?cat_id=75", "/anime-list?cat_id=76", "/anime-list?cat_id=77", "/anime-list?cat_id=78", "/anime-list?cat_id=79", "/anime-list?cat_id=80", "/anime-list?cat_id=81", "/anime-list?cat_id=82", "/anime-list?cat_id=83", "/anime-list?cat_id=84"};

    private String[] mDirectoryTitles = {"Action", "Adventure", "Cars", "Comedy", "Dementia", "Demons", "Drama", "Ecchi", "Fantasy", "Game", "Harem", "Historical", "Horror", "Josei", "Kids", "Magic", "Martial Arts", "Mecha", "Military", "Music", "Mystery", "Parody", "Police", "Psychological", "Romance", "Samurai", "School", "Sci-Fi", "Seinen", "Shoujo", "Shoujo Ai", "Shounen", "Shounen Ai", "Slice of Life", "Space", "Sports", "Super Power", "Supernatural", "Thriller", "Vampire", "Yaoi", "Yuri"};

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
        return getDirectoryURL(pType).replace("/anime-list", "/anime-list/page/" + pPage);
    }

    @Override
    public String getDirectoryURL(String pUrl, int pPage) {
        return null;
    }

    @Override
    public ArrayList<Anime> getAnimeList(String url) {
        WriteLog.appendLog(name, "getAnimeList(" + url + ")");
        ArrayList<Anime> lList = new ArrayList<Anime>();

        Document docdata;
        try {
            docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(mParseTimeOut).get();
            Elements ele = docdata.select("div[class=movie-preview-content]");

            for (int i = 0; i < ele.size(); i++) {
                String title = ele.get(i).select("span[class=movie-title]").text().trim()
                        .replaceAll("\"", "").replace("'", "");
                String animeUrl = ele.get(i).select("span[class=movie-title]").select("a").attr("abs:href");
                String coverUrl = ele.get(i).select("div[class=movie-poster]")
                        .select("img").attr("abs:src");
                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(animeUrl)) {
                    if (TextUtils.isEmpty(title))
                        WriteLog.appendLog(name, "skipped missing title "
                                + animeUrl);
                    else if (TextUtils.isEmpty(animeUrl))
                        WriteLog.appendLog(name, "skipped missing link "
                                + title);
                    else
                        WriteLog.appendLog(name, "skipped no data ");
                    continue;
                }
                Anime lAnime = new Anime();
                lAnime.setTitle(title);
                lAnime.setUrl(animeUrl);
                lAnime.setCover(coverUrl);
                lList.add(lAnime);
            }
            WriteLog.appendLog(name, "anime loaded " + lList.size() + " from " + url);
        } catch (IOException e) {
            WriteLog.appendLogException(name, "getAnimeList failed to load", e);
        }
        return lList;
    }

    @Override
    public Anime getAnimeDetails(String url) {
        WriteLog.appendLog(name, "getAnimeDetails( " + url + " )");
        Anime lAnime = new Anime();
        Document docdata;
        try {
            docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(mParseTimeOut).get();
            lAnime.setUrl(docdata.baseUri());

            // Find Title
            String animeTitle = docdata.select("div[class=title]").select("h1").first().text();
            lAnime.setTitle(animeTitle);

            String animeCover = docdata.select("div[class=poster]").select("img").attr("abs:src");
            lAnime.setCover(animeCover);

            Elements animeDetails = docdata.select("div[class=cast]");
            Element tempEle = null;
            String text = "";
            String tempArr[];
            for (int i = 0; i < animeDetails.size(); i++) {
                tempEle = animeDetails.get(i);
                text = tempEle.text();
                if (!TextUtils.isEmpty(text)) {
                    tempArr = text.split(":");
                    if (tempArr != null && tempArr.length > 1) {
                        if (tempArr[0].contains("Alternative Name"))
                            lAnime.setAliases(tempArr[1].trim());
                        else if (tempArr[0].contains("Creator"))
                            lAnime.setCreator(tempArr[1].trim());
                        else if (tempArr[0].contains("Type"))
                            lAnime.setType(tempArr[1].trim());
                        else if (tempArr[0].contains("Genre")) {
                            if (!TextUtils.isEmpty(tempArr[1]))
                                lAnime.setGenres(tempArr[1].substring(0, tempArr[1].lastIndexOf(",") - 1).replaceAll(" , ", ",").trim());
                        } else if (tempArr[0].contains("Status"))
                            lAnime.setStatus(tempArr[1].contains("complete") ? 1 : 0);
                    }
                }
            }

            // Find Title
            String animeSummary = docdata.select("div[class=excerpt more line-hide]").text();
            if (!TextUtils.isEmpty(animeSummary))
                lAnime.setSummary(animeSummary.replace("Summary:", "").trim());


            ArrayList<Episode> lList = new ArrayList<Episode>();
            Elements episodeEles = docdata.select("div[class=single-content detail]").first().select("a");
            for (int i = 0; i < episodeEles.size(); i++) {
                String lTitle = episodeEles.get(i).text().trim()
                        .replaceAll("\"", "").replace("'", "");
                String lLink = episodeEles.get(i).attr("abs:href");
                //String lDate = episodeEles.get(i).select("span").text().trim().replaceAll("\"", "").replace("'", "");
                if (TextUtils.isEmpty(lTitle) || TextUtils.isEmpty(lLink)) {
                    WriteLog.appendLog(name, "episode skipped, invalid data Name: " + lTitle + " or Link: " + lLink);
                    continue;
                }
                Episode lEpisode = new Episode();
                lEpisode.setTitle(lTitle);
                lEpisode.setUrl(lLink);
                lEpisode.setIndex(i);
                lEpisode.setAnime(lAnime);
                //lEpisode.setDate(lDate);
                lList.add(lEpisode);
            }
            lAnime.setEpisodes(lList);
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lAnime;
    }

    @Override
    public String getEpisodeVideo(String url) {
        String lVideoUrl = "";
        try {
            Document docdata = Jsoup.connect(url).userAgent(getUserAgent())
                    .referrer("http://www.google.com")
                    .timeout(Parser.getParseTimeOut()).get();
            Elements eles = docdata.select("div[class=video-content]").select("iframe");
            if (eles != null && !eles.isEmpty()) {
                String videoUrlFromProvider = "";
                for (Element ele : eles) {
                    lVideoUrl = ele.attr("src");
                    if (!TextUtils.isEmpty(lVideoUrl))
                        videoUrlFromProvider = getVideoFromProvider(lVideoUrl);
                    if (!TextUtils.isEmpty(videoUrlFromProvider)) {
                        lVideoUrl = videoUrlFromProvider;
                        break;
                    }
                }
            } else {
                eles = docdata.select("div[id=videocontainer]").select("script");
                if (eles != null && !eles.isEmpty()) {
                    Element playerScript = eles.last();
                    if (playerScript != null) {
                        String scriptText[] = playerScript.html().split("\n");
                        if (scriptText != null && scriptText.length > 0) {
                            for (int i = 0; i < scriptText.length; i++) {
                                if (scriptText[i].contains("file:")) {
                                    lVideoUrl = scriptText[i].trim().replace("file: '", "").replace("',", "");
                                    break;
                                }
                            }
                        }
                    }
                }
                //lVideoUrl = eles.select("video").select("source").attr("abs:src");
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lVideoUrl;
    }

    @Override
    public ArrayList<Anime> getLatestAnimeList(String pURL) {
        WriteLog.appendLog(name, "getLatestAnimeList(String) " + pURL);
        final ArrayList<Anime> lAnimeList = new ArrayList<Anime>();
        try {
            Document docdata = Jsoup.connect(pURL).userAgent(mUserAgent)
                    .referrer("http://www.google.com").timeout(mParseTimeOut)
                    .get();
            Elements ele = docdata.select("div[class=animelist]").select(
                    "div[class=hanime]");

            for (int i = 0; i < ele.size(); i++) {
                String lTitle = ele.get(i).select("div[class=hanimeh1]").select("a").text()
                        .trim().replaceAll("\"", "").replace("'", "");
                String lLink = ele.get(i).select("div[class=hanimeh1]").select("a")
                        .attr("abs:href");
                String lUpdateTime = ele.get(i).select("div[class=hanimeh1]").select("span")
                        .text().trim();
                String lCover = ele.get(i).select("div[class=hanimeleft]").select("img").attr("abs:src");
                String lLastChapter = "";
                if (!ele.get(i).select("div[class=hanimeh2]").isEmpty())
                    lLastChapter = ele.get(i).select("div[class=hanimeh2]").select("a")
                            .text().trim();
                if (lTitle == null || lTitle.length() == 0 || lLink == null
                        || lLink.length() == 0) {
                    if (lTitle == null || lTitle.length() == 0)
                        WriteLog.appendLog(name, "skipped missing title " + lLink);
                    else if (lLink == null || lLink.length() == 0)
                        WriteLog.appendLog(name, "skipped missing link " + lTitle);
                    else
                        WriteLog.appendLog(name, "skipped no data ");
                    continue;
                }
                Anime lAnime = new Anime();
                lAnime.setTitle(lTitle);
                lAnime.setUrl(lLink);
                lAnime.setCover(lCover);
                lAnime.setLatestEpisode(lLastChapter);
                lAnime.setLatestUpdateTime(lUpdateTime);
                lAnimeList.add(lAnime);
            }
        } catch (IOException e) {
            WriteLog.appendLog(name, "parseLatestAnimes(String) error parsing " + pURL);
            WriteLog.appendLog(Log.getStackTraceString(e));
            e.printStackTrace();
        }
        return lAnimeList;
    }

    @Override
    public ArrayList<Anime> getFullAnimeList(String pURL) {
        WriteLog.appendLog(name, "getFullAnimeList(String) " + pURL);
        final ArrayList<Anime> lAnimeList = new ArrayList<Anime>();
        try {
            Document docdata = Jsoup.connect(pURL).userAgent(mUserAgent)
                    .referrer("http://www.google.com").timeout(mParseTimeOut)
                    .get();
            Elements ele = docdata.select("div[class=animlist]");

            for (int i = 0; i < ele.size(); i++) {
                String lTitle = ele.get(i).select("div[class=anim]").select("a").text()
                        .trim().replaceAll("\"", "").replace("'", "");
                String lLink = ele.get(i).select("div[class=anim]").select("a")
                        .attr("abs:href");
                if (lTitle == null || lTitle.length() == 0 || lLink == null
                        || lLink.length() == 0) {
                    if (lTitle == null || lTitle.length() == 0)
                        WriteLog.appendLog(name, "skipped missing title " + lLink);
                    else if (lLink == null || lLink.length() == 0)
                        WriteLog.appendLog(name, "skipped missing link " + lTitle);
                    else
                        WriteLog.appendLog(name, "skipped no data ");
                    continue;
                }
                Anime lAnime = new Anime();
                lAnime.setTitle(lTitle);
                lAnime.setUrl(lLink);
                lAnimeList.add(lAnime);
            }
        } catch (IOException e) {
            WriteLog.appendLog(name, "getFullAnimeList(String) error parsing " + pURL);
            WriteLog.appendLog(Log.getStackTraceString(e));
            e.printStackTrace();
        }
        return lAnimeList;
    }

}
