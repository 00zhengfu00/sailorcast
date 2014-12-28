package com.crixmod.sailorcast.siteapi;

import android.util.Log;

import com.crixmod.sailorcast.SailorCast;
import com.crixmod.sailorcast.model.SCAlbum;
import com.crixmod.sailorcast.model.SCAlbums;
import com.crixmod.sailorcast.model.SCSite;
import com.crixmod.sailorcast.model.youku.searchresult.Result;
import com.crixmod.sailorcast.model.youku.searchresult.SearchResults;
import com.crixmod.sailorcast.model.youku.show.ShowInfo;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by fire3 on 2014/12/27.
 */
public class YouKuApi extends BaseSiteApi {

    private static final String SUCCESS = "success";
    //YouKu Search:  SEARCH_URL_BASE + URLEncode(key) + PID + GUID
    private final String SEARCH_URL_BASE = "http://search.api.3g.youku.com/layout/android/v4/search/direct_all/";
    private final String PID = "?pid=0865e0628a79dfbb&guid=";

    //YouKu Show Info: SHOW_INFO_BASE + GUID + SHOW_ID + showId
    //Show Info中返回剧集的详细信息，比如导演、演员，描述等等。
    private final String SHOW_INFO_BASE = "http://api.mobile.youku.com/layout/android3_0/play/detail?pid=0865e0628a79dfbb&guid=";
    private final String SHOW_ID = "&id=";

    //得到剧集中每一个video的ID接口： SHOW_VIDEOS_BASE + GUID + SHOW_IDS + showID
    private final String SHOW_VIDEOS_BASE =  "http://api.mobile.youku.com/common/batch/showinfo?pid=0865e0628a79dfbb&guid=";
    private final String SHOW_IDS = "&showids=";

    private String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getGUID()  {
        String str = null;
        str = md5(SailorCast.getMacAddress() + "&" + SailorCast.getIMEI() + "&" + "&");
        if ((str == null) || (str.length() <= 0)) {
            return "";
        }
        return str;
    }

    private String getSearchUrl(String key) {
        try {
            return SEARCH_URL_BASE + URLEncoder.encode(key,"UTF-8") + PID + getGUID();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getShowInfoUrl(String showID) {

        if(showID == null || showID.equals(""))
            return null;
        else
            return SHOW_INFO_BASE + getGUID() + SHOW_ID + showID;
    }

    @Override
    public void doSearch(String key, final OnSearchRequestListener listener) {

        String searchUrl = getSearchUrl(key);

        Log.d("fire3","searchUrl: "  + searchUrl);
        if(searchUrl == null) {
            listener.onSearchFailed("error search url");
        } else {
            Request request = new Request.Builder()
                    .url(searchUrl)
                    .build();
            SailorCast.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    e.printStackTrace();
                    listener.onSearchFailed("http failure");
                }

                @Override
                public void onResponse(Response response) {
                    if (!response.isSuccessful()) {
                        listener.onSearchFailed("http response fail");
                        return;
                    }
                    try {
                        SearchResults results =  SailorCast.getGson().fromJson(response.body().string(), SearchResults.class);
                        if(results.getStatus().equals(SUCCESS))
                            listener.onSearchSuccess(toSCAlbums(results));
                        else
                            listener.onSearchFailed("search failed");
                    } catch (IOException e) {
                        listener.onSearchFailed("io Exception");
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private SCAlbums toSCAlbums(SearchResults results) {
        SCAlbums albums = new SCAlbums();
        for(Result r : results.getResults()) {
            if(r.getIsYouku() == 1) {
                SCAlbum sa = new SCAlbum();
                sa.setTitle(StringEscapeUtils.unescapeJava(r.getShowname()));
                sa.setHorImageUrl(StringEscapeUtils.unescapeJava(r.getShowThumburl()));
                sa.setVerImageUrl(StringEscapeUtils.unescapeJava(r.getShowVthumburl()));
                sa.setAlbumId(r.getShowid());
                sa.setSubTitle(StringEscapeUtils.unescapeJava(r.getSummary()));
                sa.setTip(StringEscapeUtils.unescapeJava(r.getNotice()));
                sa.setSite(new SCSite(SCSite.YOUKU));
                albums.add(sa);
            }
        }
        return albums;
    }

    private void fillAlbumDesc(SCAlbum album, ShowInfo showInfo) {
        if(showInfo.getStatus().equals(SUCCESS)) {
            album.setDesc(StringEscapeUtils.unescapeJava(showInfo.getDetail().getDesc()));

            if(showInfo.getDetail().getPerformer() != null) {
                String actors = "";
                for (String actor : showInfo.getDetail().getPerformer()) {
                    actors = actors + StringEscapeUtils.unescapeJava(actor) + " ";
                }
                album.setMainActor(actors);
            }

            if(showInfo.getDetail().getDirector() != null) {
                String directors = "";
                for(String d : showInfo.getDetail().getDirector()) {
                    directors  = directors + StringEscapeUtils.unescapeJava(d) + " ";
                }
                album.setDirector(directors);
            }

            if(showInfo.getDetail().getEpisodeTotal() != null) {
                album.setVideosCount(showInfo.getDetail().getEpisodeTotal());
            }
        }
    }

    @Override
    public void doGetAlbumVideos(SCAlbum album, OnGetAlbumVideosListener listener) {


    }

    @Override
    public void doGetAlbumDesc(final SCAlbum album, final OnGetAlbumDescListener listener) {

        String url = getShowInfoUrl(album.getAlbumId());
        Log.d("fire3","desc Url: "  + url);
        if(url == null)
            listener.onGetAlbumDescFailed("wrong albumID");

        Request request = new Request.Builder()
                .url(url)
                .build();
        SailorCast.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                listener.onGetAlbumDescFailed("http failure");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) {
                    listener.onGetAlbumDescFailed("http response fail");
                    return;
                }
                try {
                    ShowInfo showInfo = SailorCast.getGson().fromJson(response.body().string(),ShowInfo.class);
                    if(showInfo.getStatus().equals(SUCCESS))
                        fillAlbumDesc(album,showInfo);
                    else
                        listener.onGetAlbumDescFailed("get desc failed");
                    listener.onGetAlbumDescSuccess(album);
                } catch (IOException e) {
                    listener.onGetAlbumDescFailed("io Exception");
                    e.printStackTrace();
                }
            }
        });
    }



}
