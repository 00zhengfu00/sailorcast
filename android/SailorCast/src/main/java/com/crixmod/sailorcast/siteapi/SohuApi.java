package com.crixmod.sailorcast.siteapi;

import com.crixmod.sailorcast.R;
import com.crixmod.sailorcast.SailorCast;
import com.crixmod.sailorcast.model.SCAlbum;
import com.crixmod.sailorcast.model.SCAlbums;
import com.crixmod.sailorcast.model.SCChannel;
import com.crixmod.sailorcast.model.SCChannelFilter;
import com.crixmod.sailorcast.model.SCSite;
import com.crixmod.sailorcast.model.SCVideo;
import com.crixmod.sailorcast.model.SCVideos;
import com.crixmod.sailorcast.model.sohu.album.Album;
import com.crixmod.sailorcast.model.sohu.searchresult.SearchResultAlbum;
import com.crixmod.sailorcast.model.sohu.searchresult.SearchResults;
import com.crixmod.sailorcast.model.sohu.videos.Video;
import com.crixmod.sailorcast.model.sohu.videos.Videos;
import com.crixmod.sailorcast.utils.HttpUtils;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.lang.StringEscapeUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by fire3 on 14-12-26.
 */
public class SohuApi extends BaseSiteApi {
    private final static String API_KEY = "plat=6&poid=1&api_key=9854b2afa779e1a6bff1962447a09dbd&sver=4.5.0&sysver=4.4.2&partner=47";
    private final static String API_ALBUM_INFO = "http://api.tv.sohu.com/v4/album/info/" ;
    private final static String API_ALBUM_VIDOES = "http://api.tv.sohu.com/v4/album/videos/" ;
    private final static String API_CATEGORY_FILTER = "http://api.tv.sohu.com/v4/search/channel.json?";
    private final static String API_SEARCH = "http://api.tv.sohu.com/v4/search/album.json?o=&all=0&ds=&" + API_KEY + "&key=";

    private static int ORDER_DESCENDING = 1;
    private static int ORDER_ASCENDING = 0;

    private final static String API_CHANNEL_ALBUM_FORMAT = "http://api.tv.sohu.com/v4/search/channel.json" +
            "?cid=%s&o=1&plat=6&poid=1&api_key=9854b2afa779e1a6bff1962447a09dbd&" +
            "sver=4.5.0&sysver=4.4.2&partner=47&page=%s&page_size=%s";

    private final static int CID_SHOW = 2;
    private final static int CID_MOVIE = 1;
    private final static int CID_COMIC = 16;
    private final static int CID_VARIETY = 7;
    private final static int CID_DOCUMENTARY = 8;
    private final static int CID_MUSIC = 24;
    private final static int CID_ENT = 13;
    private final static int CID_SPORT = 9009;

    private int channelToCid(SCChannel channel) {

        if(channel.getChannelID() == SCChannel.MOVIE)
            return CID_MOVIE;
        if(channel.getChannelID() == SCChannel.SHOW)
            return CID_SHOW;
        if(channel.getChannelID() == SCChannel.DOCUMENTARY)
            return CID_DOCUMENTARY;
        /*
        if(channel.getChannelID() == SCChannel.ENT)
            return CID_ENT;
        */
        if(channel.getChannelID() == SCChannel.COMIC)
            return CID_COMIC;
        if(channel.getChannelID() == SCChannel.VARIETY)
            return CID_VARIETY;
        if(channel.getChannelID() == SCChannel.MUSIC)
            return CID_MUSIC;
        if(channel.getChannelID() == SCChannel.SPORT)
            return CID_SPORT;
        if(channel.getChannelID() == SCChannel.UNKNOWN)
            return -1;
        return -1;
    }


    private String getChannelAlbumUrl(SCChannel channel, int pageNo, int pageSize) {
        return String.format(API_CHANNEL_ALBUM_FORMAT,channelToCid(channel),pageNo,pageSize);
    }

    @Override
    public void doSearch(String key, final OnGetAlbumsListener listener) {

        String searchKey = null;
        try {
            searchKey = URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            listener.onGetAlbumsFailed("Error search key");
            e.printStackTrace();
            return;
        }
        String url = API_SEARCH + searchKey;

        HttpUtils.asyncGet(url, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
                listener.onGetAlbumsFailed("http failure");
            }

            @Override
            public void onResponse(Response response) {
                if (!response.isSuccessful()) {
                    listener.onGetAlbumsFailed("response failed");
                    return;
                }
                SearchResults results = null;
                try {
                    results = SailorCast.getGson().fromJson(response.body().string(), SearchResults.class);

                    SCAlbums albums = toSCAlbums(results);
                    if(albums != null)
                        listener.onGetAlbumsSuccess(albums);
                    else
                        listener.onGetAlbumsFailed(SailorCast.getResource().getString(R.string.fail_reason_no_results));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void doGetAlbumVideos(final SCAlbum album, final int pageNo, final int pageSize, final OnGetVideosListener listener) {
        String url;
        url = API_ALBUM_VIDOES + album.getAlbumId() + ".json?" + "page=" + pageNo + "&page_size=" + pageSize +
                "&order=" + ORDER_ASCENDING + "&site=1&with_trailer=1&" + API_KEY;
        HttpUtils.asyncGet(url,new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) {
                    listener.onGetVideosFailed("response failed");
                    return;
                }
                Videos videos = SailorCast.getGson().fromJson(response.body().string(),Videos.class);
                if(videos.getData() != null) {
                    SCVideos scVideos = new SCVideos();
                    int i = 0;
                    for (Video v : videos.getData().getVideos()) {
                        i++ ;
                        SCVideo scVideo = new SCVideo();
                        scVideo.setSCSite(SCSite.SOHU);
                        scVideo.setHorPic(v.getHorHighPic());
                        scVideo.setVerPic(v.getVerHighPic());
                        scVideo.setVideoID(v.getVid().toString());
                        scVideo.setSeqInAlbum((pageNo - 1) * pageSize + i);
                        scVideo.setVideoTitle(v.getVideoName());
                        scVideo.setM3U8Nor(v.getUrlNor());
                        scVideo.setM3U8High(v.getUrlHigh());
                        scVideo.setM3U8Super(v.getUrlSuper());
                        scVideo.setAlbumID(album.getAlbumId());
                        scVideos.add(scVideo);
                    }
                    listener.onGetVideosSuccess(scVideos);
                }

            }
        });
    }


    private void fillAlbumDesc(SCAlbum album, Album sohuAlbum, OnGetAlbumDescListener listener) {
        if(sohuAlbum.getData() != null) {
            album.setVideosCount(sohuAlbum.getData().getLatestVideoCount());
            //TotalVideoCount is 0 sometimes, use latestVideoCount instead.
            if (sohuAlbum.getData().getTotalVideoCount() > 0)
                album.setVideosTotal(sohuAlbum.getData().getTotalVideoCount());
            else
                album.setVideosTotal(sohuAlbum.getData().getLatestVideoCount());

            album.setDesc(sohuAlbum.getData().getAlbumDesc());
            album.setMainActor(sohuAlbum.getData().getMainActor());
            album.setDirector(sohuAlbum.getData().getDirector());
            listener.onGetAlbumDescSuccess(album);
        } else
            listener.onGetAlbumDescFailed("Sohu Album Data is null");
    }

    @Override
    public void doGetAlbumDesc(final SCAlbum album, final OnGetAlbumDescListener listener) {
        String url = API_ALBUM_INFO + album.getAlbumId() + ".json?" + API_KEY;
        HttpUtils.asyncGet(url, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) {
                    listener.onGetAlbumDescFailed("response failed");
                    return;
                }
                Album a = SailorCast.getGson().fromJson(response.body().string(), Album.class);
                fillAlbumDesc(album, a, listener);
            }
        });
    }


    @Override
    public void doGetVideoPlayUrl(SCVideo video, OnGetVideoPlayUrlListener listener) {
        if(video.getM3U8Nor() != null)
            listener.onGetVideoPlayUrlNormal(video,video.getM3U8Nor());
        if(video.getM3U8High() != null)
            listener.onGetVideoPlayUrlHigh(video,video.getM3U8High());
        if(video.getM3U8Super() != null)
            listener.onGetVideoPlayUrlHigh(video,video.getM3U8Super());
    }

    @Override
    public void doGetChannelAlbums(SCChannel channel, int pageNo, int pageSize, final OnGetAlbumsListener listener) {
        String url = getChannelAlbumUrl(channel, pageNo, pageSize);
        HttpUtils.asyncGet(url, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                listener.onGetAlbumsFailed("Http failure");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) {
                    listener.onGetAlbumsFailed("response failed");
                    return;
                }
                SearchResults results = null;
                try {
                    results = SailorCast.getGson().fromJson(response.body().string(), SearchResults.class);

                    SCAlbums albums = toSCAlbums(results);
                    if(albums != null)
                        listener.onGetAlbumsSuccess(albums);
                    else
                        listener.onGetAlbumsFailed(SailorCast.getResource().getString(R.string.fail_reason_no_results));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void doGetChannelAlbumsByFilter(SCChannel channel, int pageNo, int pageSize, SCChannelFilter filter, OnGetAlbumsListener listener) {

    }

    @Override
    public void doGetChannelFilter(SCChannel channel, OnGetChannelFilterListener listener) {

    }

    private SCAlbums toSCAlbums(SearchResults results) {
        if(results.getData().getSearchResultAlbums() == null)
            return null;
        if(results.getData().getSearchResultAlbums().size() == 0 && results.getData().getSearchResultVideos().size() == 0)
            return null;

        if(results.getData().getSearchResultAlbums().size() > 0) {
            SCAlbums albums = new SCAlbums();
            for (SearchResultAlbum a : results.getData().getSearchResultAlbums()) {
                SCAlbum sa = new SCAlbum(SCSite.SOHU);
                sa.setDesc(a.getTvDesc());
                sa.setDirector(a.getDirector());
                sa.setHorImageUrl(a.getHorHighPic());
                sa.setVerImageUrl(a.getVerHighPic());
                sa.setMainActor(a.getMainActor());
                sa.setTitle(a.getAlbumName());
                sa.setSubTitle(a.getTip());
                sa.setAlbumId(a.getAid().toString());
                albums.add(sa);
            }
            return albums;
        }

        if(results.getData().getSearchResultVideos().size() > 0) {
            SCAlbums albums = new SCAlbums();
            for (SearchResultAlbum a : results.getData().getSearchResultVideos()) {
                SCAlbum sa = new SCAlbum(SCSite.SOHU);
                sa.setDesc(a.getTvDesc());
                sa.setDirector(a.getDirector());
                sa.setHorImageUrl(a.getHorHighPic());
                sa.setVerImageUrl(a.getVerHighPic());
                sa.setMainActor(a.getMainActor());
                sa.setTitle(a.getAlbumName());
                sa.setSubTitle(a.getTip());
                if(a.getAid() != null) {
                    sa.setAlbumId(a.getAid().toString());
                    albums.add(sa);
                }
            }
            if(albums.size() > 0)
                return albums;
        }

        return null;
    }
}
