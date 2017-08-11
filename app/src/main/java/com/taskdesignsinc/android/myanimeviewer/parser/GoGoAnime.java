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

public class GoGoAnime extends Parser {

    public GoGoAnime() {
        name = "GoGo Anime";
        isGridViewSupported = true;
        isGenreSortSupported = false;
        isCloudFlareDDOSEnabled = true;
        isCloudFlareDDOSPassed = false;
        mCustomUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36";
    }

    @Override
    public String getServerUrl() {
        return "https://www.gogoanime.io";
    }

    ;

    @Override
    public String getLatestAnimeListURL() {
        return getServerUrl();
    }

    private String[] mDirectoryLinks = {"/popular.html"};

    private String[] mDirectoryTitles = {"Popular"};

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
        return getDirectoryURL(pType) + "?page=" + pPage;
    }

    @Override
    public String getDirectoryURL(String pUrl, int pPage) {
        return null;
    }

    @Override
    public ArrayList<Anime> getAnimeList(String url) {
        WriteLog.appendLog(name + ": getAnimeList(" + url + ")");
        ArrayList<Anime> lList = new ArrayList<Anime>();

        Document docdata;
        try {
            docdata = load(url, true);
            Elements ele = docdata.select("div[class=main_body]")
                    .select("div[class=last_episodes]").select("ul[class=items]").select("li");

            for (int i = 0; i < ele.size(); i++) {
                String lTitle = ele.get(i).select("p[class=name]").select("a").text().trim()
                        .replaceAll("\"", "").replace("'", "");
                String lLink = ele.get(i).select("a").attr("abs:href");
                String lCover = ele.get(i)
                        .select("img").attr("abs:src");
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
                lAnime.setCover(lCover);
                lList.add(lAnime);
            }
            WriteLog.appendLog(name + ": anime loaded " + lList.size() + " from " + url);
        } catch (IOException e) {
            WriteLog.appendLogException(name, "", e);
        }
        return lList;
    }

    @Override
    public Anime getAnimeDetails(String url) {
        WriteLog.appendLog(name, "getAnimeDetails( "+url+" )");
        Anime lAnime = new Anime();
        Document docdata;
        try {
            docdata = Jsoup.connect(url).userAgent(mUserAgent)
                    .referrer("http://www.google.com")
                    .timeout(mParseTimeOut).get();
            lAnime.setUrl(docdata.baseUri());
            Elements animeDetails = docdata
                    .select("div[class=anime_info_body]").select("p[class=type]");

            // Find ID
            int animeID = lAnime.getUrl().hashCode();
            lAnime.setId(animeID);
            // Find Title
            String animeTitle = docdata.select("div[class=anime_info_body_bg]").select("h1").first().text();
            if (animeTitle.lastIndexOf("(") != -1)
                lAnime.setTitle(animeTitle.substring(0, animeTitle.lastIndexOf("(")).trim());
            else
                lAnime.setTitle(animeTitle);

            String animeCover = docdata.select("div[class=anime_info_body_bg]").select("img").attr("abs:src");
            lAnime.setCover(animeCover);

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
                                lAnime.setGenres(tempArr[1].trim());
                        } else if (tempArr[0].contains("Status"))
                            lAnime.setStatus(tempArr[1].contains("complete") ? 1 : 0);
                        else if (tempArr[0].contains("Plot Summary"))
                            lAnime.setSummary(tempArr[1].trim());
                    }
                }
            }

            //var base_url = 'https://' + document.domain + '/';
            //input#movie_id = 3337
            //var url = base_url + '/load-list-episode?ep_start='+ep_start+'&ep_end='+ep_end+'&id='+id+'&default_ep='+default_ep;
            //https://gogoanime.io/load-list-episode?ep_start=0&ep_end=50&id=3337&default_ep=0

            ArrayList<Episode> lList = new ArrayList<Episode>();
            Elements episodeEles = docdata.select("ul[id=episode_page]").select("li").select("a");
            int episodeStart = 0;
            int chapterEnd = -1;
            Element element;
            for (int i = 0; i < episodeEles.size(); i++) {
                element = episodeEles.get(i);
                if (i == 0) {
                    String ep_start = element.attr("ep_start");
                    episodeStart = Integer.parseInt(ep_start);
                }
                if (i == episodeEles.size() - 1) {
                    String ep_end = element.attr("ep_end");
                    chapterEnd = Integer.parseInt(ep_end);
                }
            }
            for (int i = 0; i < chapterEnd; i++) {
                String lTitle = "Episode " + (i + 1);
                String lLink = lAnime.getUrl().replace("/category", "") + "-episode-" + (i + 1);
                if (TextUtils.isEmpty(lTitle) || TextUtils.isEmpty(lLink)) {
                    WriteLog.appendLog(name + ": episode skipped, invalid data Name: " + lTitle + " or Link: " + lLink);
                    continue;
                }
                Episode lEpisode = new Episode();
                lEpisode.setTitle(lTitle);
                lEpisode.setUrl(lLink);
                lEpisode.setIndex(i);
                lEpisode.setAnime(lAnime);
                lList.add(lEpisode);
            }
            lAnime.setEpisodes(lList);
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return lAnime;
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

    @Override
    public String getEpisodeVideo(String url) {
        String videoUrl = "";
        try {
            Document docdata = load(url, true);
            Elements videoEle = docdata.select("div[class=play-video]").select("iframe");
            if (videoEle != null && !videoEle.isEmpty()) {
                String tempUrl = videoEle.attr("abs:src");
                videoUrl = getVideoFromProvider(tempUrl);
                if (!TextUtils.isEmpty(videoUrl))
                    return videoUrl;
            }
            Elements downloadEle = docdata.select("div[class=anime_video_body]")
                    .select("div[class=download-anime]").select("a");
            if (downloadEle != null && !downloadEle.isEmpty()) {
                String downloadLink = downloadEle.get(0).attr("href");
                if (!TextUtils.isEmpty(downloadLink)) {
                    videoUrl = getInternalVideoUrlAniUploader(downloadLink);
                    if (!TextUtils.isEmpty(videoUrl)) {
                        return videoUrl;
                    }
                }
            } else {
                Elements eles = docdata.select("div[id=video_container_div]").select("script");
                if (eles != null && !eles.isEmpty()) {
                    Element playerScript = eles.last();
                    if (playerScript != null) {
                        String scriptText[] = playerScript.html().split("\n");
                        if (scriptText != null && scriptText.length > 0) {
                            for (int i = 0; i < scriptText.length; i++) {
                                if (scriptText[i].contains("file:")) {
                                    videoUrl = scriptText[i].trim().replace("file: '", "").replace("',", "");
                                    break;
                                }
                            }
                        }
                    }
                    //videoUrl = eles.select("video").select("source").attr("abs:src");
                } else {
                    eles = docdata.select("iframe[class=mirrorVid]");
                    if (eles != null && !eles.isEmpty()) {
                        videoUrl = eles.attr("src");
                        if (!TextUtils.isEmpty(videoUrl))
                            videoUrl = getVideoFromProvider(videoUrl);
                    }
                }
            }
        } catch (IOException e) {
            WriteLog.appendLog(Log.getStackTraceString(e));
        }
        return videoUrl;
    }

    @Override
    public ArrayList<Anime> getLatestAnimeList(String pURL) {
        WriteLog.appendLog(name + ": getLatestAnimeList(String) " + pURL);
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
                        WriteLog.appendLog(name + ": skipped missing title " + lLink);
                    else if (lLink == null || lLink.length() == 0)
                        WriteLog.appendLog(name + ": skipped missing link " + lTitle);
                    else
                        WriteLog.appendLog(name + ": skipped no data ");
                    continue;
                }
                Anime lAnime = new Anime();
                lAnime.setTitle(lTitle);
                lAnime.setUrl(lLink);
                lAnime.setCover(lCover);
                lAnime.setLatestEpisode(lLastChapter);
                lAnime.setLatestUpdateTime(lUpdateTime);
                lAnime.setId(lAnime.getUrl().hashCode());
                lAnimeList.add(lAnime);
            }
        } catch (IOException e) {
            WriteLog.appendLog(name + ": parseLatestAnimes(String) error parsing " + pURL);
            WriteLog.appendLog(Log.getStackTraceString(e));
            e.printStackTrace();
        }
        return lAnimeList;
    }

    @Override
    public ArrayList<Anime> getFullAnimeList(String pURL) {
        WriteLog.appendLog(name + ": getFullAnimeList(String) " + pURL);
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
                        WriteLog.appendLog(name + ": skipped missing title " + lLink);
                    else if (lLink == null || lLink.length() == 0)
                        WriteLog.appendLog(name + ": skipped missing link " + lTitle);
                    else
                        WriteLog.appendLog(name + ": skipped no data ");
                    continue;
                }
                Anime lAnime = new Anime();
                lAnime.setTitle(lTitle);
                lAnime.setUrl(lLink);
                lAnime.setId(lAnime.getUrl().hashCode());
                lAnimeList.add(lAnime);
            }
        } catch (IOException e) {
            WriteLog.appendLog(name + ": getFullAnimeList(String) error parsing " + pURL);
            WriteLog.appendLog(Log.getStackTraceString(e));
            e.printStackTrace();
        }
        return lAnimeList;
    }

}
