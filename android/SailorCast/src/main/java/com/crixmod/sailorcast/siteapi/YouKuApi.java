package com.crixmod.sailorcast.siteapi;

import android.util.Base64;

import com.crixmod.sailorcast.R;
import com.crixmod.sailorcast.SailorCast;
import com.crixmod.sailorcast.model.SCAlbum;
import com.crixmod.sailorcast.model.SCAlbums;
import com.crixmod.sailorcast.model.SCChannel;
import com.crixmod.sailorcast.model.SCChannelFilter;
import com.crixmod.sailorcast.model.SCChannelFilterItem;
import com.crixmod.sailorcast.model.SCSite;
import com.crixmod.sailorcast.model.SCVideo;
import com.crixmod.sailorcast.model.SCVideos;
import com.crixmod.sailorcast.model.youku.searchresult.Result;
import com.crixmod.sailorcast.model.youku.searchresult.SearchResults;
import com.crixmod.sailorcast.model.youku.show.ShowInfo;
import com.crixmod.sailorcast.model.youku.show.ShowVideos;
import com.crixmod.sailorcast.model.youku.show.ShowVideosResult;
import com.crixmod.sailorcast.utils.HttpUtils;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.decapi.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by fire3 on 2014/12/27.
 */
public class YouKuApi extends BaseSiteApi {

    private static final String SUCCESS = "success";
    private static final String YOUKU_PID = "0865e0628a79dfbb";
    //YouKu Search:  SEARCH_URL_BASE + URLEncode(key) + PID + GUID
    private static final String SEARCH_URL_BASE = "http://search.api.3g.youku.com/layout/android/v4/search/direct_all/";
    private static final String PID = "?pid=0865e0628a79dfbb&guid=";

    private static final String SEARCH_URL_BASE_FORMAT = "http://search.api.3g.youku.com/layout/android" +
            "/v4/search/direct_all/%s?pid=%s&guid=%s";
    //YouKu Show Info: SHOW_INFO_BASE + GUID + SHOW_ID + showId
    //Show Info中返回剧集的详细信息，比如导演、演员，描述等等。
    private static final String SHOW_INFO_BASE = "http://api.mobile.youku.com/layout/android3_0/play/detail?" +
            "pid=0865e0628a79dfbb&guid=";
    private static final String SHOW_ID = "&id=";

    //得到剧集中每一个video的ID接口： SHOW_VIDEOS_BASE + showid +  SHOW_VIDEOS_PID  + GUID + SHOW_VIDEOS_FIELDS + pageNo + SHOW_VIDEOS_PZ + pageSize
    private static final String SHOW_VIDEOS_BASE =  "http://api.mobile.youku.com/shows/";
    private static final String SHOW_VIDEOS_PID = "/reverse/videos?pid=0865e0628a79dfbb&ver=4.4&guid=";
    private static final String SHOW_VIDEOS_FIELDS ="&fields=" + URLEncoder.encode("is_new|vid|titl|lim") + "&pg=";
    //private static final String SHOW_VIDEOS_FIELDS ="&fields=" + URLEncoder.encode("is_new|vid|titl|lim") + "&pg=";
    private static final String SHOW_VIDEOS_PZ = "&pz=";

    //得到Video描述文件接口：  VIDEO_INFO_BASE + getGdid() + VIDEO_FORMAT_LAN_TYPE +  getGuid() + VIDEO_ID + videoID
    //TODO: DID需要处理一下
    private static final String VIDEO_INFO_BASE = "http://a.play.api.3g.youku.com/common/v3/play?ver=4.4&did=";
    private static final String VIDEO_FORMAT_LAN_TYPE = "&local_point=&audiolang=1&format=1,5,6,7,8&language=default&point=1&local_time=&local_vid=&ctype=20&pid=0865e0628a79dfbb&guid=";
    private static final String VIDEO_ID = "&id=";

    private static final String YOUKU_KEY = "9e3633aadde6bfec";

    private static final int CID_SHOW = 97;
    private static final int SUB_CHANNEL_ID_SHOW = 404;

    private static final int CID_MOVIE = 96;
    private static final int SUB_CHANNEL_ID_MOVIE = 567;

    private static final int CID_DOCUMENTARY = 84;
    private static final int SUB_CHANNEL_ID_DOCUMENTARY = 16;

    private static final int CID_VARIETY = 85;
    private static final int SUB_CHANNEL_ID_VARIETY = 10;

    private static final int CID_COMIC = 100;
    private static final int SUB_CHANNEL_ID_COMIC = 7;

    private static final int CID_MUSIC = 95;
    private static final int SUB_CHANNEL_ID_MUSIC = 13;

    private static final int CID_ENT = 86;
    private static final int SUB_CHANNEL_ID_ENT = 25;

    private static final int CID_SPORT = 98;
    private static final int SUB_CHANNEL_ID_SPORT = 148;

    private static final String CHANNEL_ALBUMS_LIST_FORMAT = "http://api.mobile.youku.com/layout/" +
            "android/channel/subpage?pid=%s&guid=%s&ver=4.4&cid=%s&" +
            "sub_channel_id=%s&sub_channel_type=4&filter=&ob=2&pg=%s&pz=%s";

    private static final String CHANNEL_FILTER_FORMAT = "http://api.mobile.youku.com/layout/android3_0/channel/filter?pid=" +
            "0865e0628a79dfbb&guid=%s&ver=4.4&cid=%s";


    private static final String CHANNEL_ALBUMS_LIST_FILTER_FORMAT = "http://api.mobile.youku.com/layout/" +
            "android/channel/subpage?pid=%s&guid=%s&ver=4.4&cid=%s&" +
            "sub_channel_id=%s&sub_channel_type=4&filter=%s&ob=2&pg=%s&pz=%s";

    /* filter = 中是类似  area:|movie_genre:|releaseyear:2015|pay_type:|paid:  字段，经过URLencode escape */

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

    private int channelToSubChannelID(SCChannel channel) {

        if(channel.getChannelID() == SCChannel.MOVIE)
            return SUB_CHANNEL_ID_MOVIE;
        if(channel.getChannelID() == SCChannel.SHOW)
            return SUB_CHANNEL_ID_SHOW;
        if(channel.getChannelID() == SCChannel.DOCUMENTARY)
            return SUB_CHANNEL_ID_DOCUMENTARY;
        /*
        if(channel.getChannelID() == SCChannel.ENT)
            return SUB_CHANNEL_ID_ENT;
        */
        if(channel.getChannelID() == SCChannel.COMIC)
            return SUB_CHANNEL_ID_COMIC;
        if(channel.getChannelID() == SCChannel.VARIETY)
            return SUB_CHANNEL_ID_VARIETY;
        if(channel.getChannelID() == SCChannel.MUSIC)
            return SUB_CHANNEL_ID_MUSIC;
        if(channel.getChannelID() == SCChannel.SPORT)
            return SUB_CHANNEL_ID_SPORT;
        if(channel.getChannelID() == SCChannel.UNKNOWN)
            return -1;
        return -1;
    }


    private String getChannelAlbumsListUrl(SCChannel channel, int pageNo, int pageSize) {
        int cid = channelToCid(channel);
        int subCid = channelToSubChannelID(channel);
        String pid = YOUKU_PID;
        String gid = getGUID();
        String url = String.format(CHANNEL_ALBUMS_LIST_FORMAT,pid,gid,cid,subCid,pageNo,pageSize);
        return url;
    }


    private String getChannelAlbumsListUrlByFilter(SCChannel channel, SCChannelFilter filter, int pageNo, int pageSize) {
        ArrayList<SCChannelFilterItem> items = filter.getSelectedItems();
        String filterString = "";
        int cid = channelToCid(channel);
        int subCid = channelToSubChannelID(channel);
        String pid = YOUKU_PID;
        String gid = getGUID();
        for(SCChannelFilterItem item: items) {
            if(item.getSearchKey() != null)
                filterString += item.getSearchKey() + ":" + item.getSearchVal() + "|";
        }
        String finalFilter =  URLEncoder.encode(filterString);

        String url = String.format(CHANNEL_ALBUMS_LIST_FILTER_FORMAT,pid,gid,cid,subCid,finalFilter,pageNo,pageSize);
        return url;
    }


    private String getChannelFilterUrl(SCChannel channel) {
        return String.format(CHANNEL_FILTER_FORMAT,getGUID(),channelToCid(channel));
    }

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

    private String getGDID()
    {
        String str = null;
        if ((str == null) || (str.length() <= 0))
        {
            str = md5(SailorCast.getMacAddress() + "&" + SailorCast.getIMEI());
            if ((str == null) || (str.length() <= 0))
                return "";
        }
        return str;
    }

    private String getSearchUrl(String key) {
        try {
            String enKey = URLEncoder.encode(key,"UTF-8");
            return String.format(SEARCH_URL_BASE_FORMAT,enKey,YOUKU_PID,getGUID());
            //return SEARCH_URL_BASE + URLEncoder.encode(key,"UTF-8") + PID + getGUID();
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

    private String getShowVideosUrl(String showID, int pageNo, int pageSize) {
            return (SHOW_VIDEOS_BASE + showID +  SHOW_VIDEOS_PID  + getGUID() +  SHOW_VIDEOS_FIELDS + pageNo + SHOW_VIDEOS_PZ + pageSize);
    }

    private String getVideoInfoUrl(String videoID) {
        return (VIDEO_INFO_BASE + getGDID() + VIDEO_FORMAT_LAN_TYPE + getGUID() + VIDEO_ID + videoID);
    }


    @Override
    public void doSearch(String key, final OnGetAlbumsListener listener) {

        String searchUrl = getSearchUrl(key);

        if(searchUrl == null) {
            listener.onGetAlbumsFailed("error search url");
        } else {
            HttpUtils.asyncGet(searchUrl,new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    e.printStackTrace();
                    listener.onGetAlbumsFailed("http failure");
                }

                @Override
                public void onResponse(Response response) {
                    if (!response.isSuccessful()) {
                        listener.onGetAlbumsFailed("http response fail");
                        return;
                    }
                    try {
                        SearchResults results =  SailorCast.getGson().fromJson(response.body().string(), SearchResults.class);
                        if(results.getStatus().equals(SUCCESS)) {
                            SCAlbums albums = toSCAlbums(results);
                            if(albums != null)
                                listener.onGetAlbumsSuccess(albums);
                            else
                                listener.onGetAlbumsFailed(SailorCast.getResource().getString(R.string.fail_reason_no_results));
                        }
                        else
                            listener.onGetAlbumsFailed(SailorCast.getResource().getString(R.string.fail_reason_no_results));
                    } catch (IOException e) {
                        listener.onGetAlbumsFailed("io Exception");
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
                SCAlbum sa = new SCAlbum(SCSite.YOUKU);
                sa.setTitle(StringEscapeUtils.unescapeJava(r.getShowname()));
                sa.setHorImageUrl(StringEscapeUtils.unescapeJava(r.getShowThumburl()));
                sa.setVerImageUrl(StringEscapeUtils.unescapeJava(r.getShowVthumburl()));
                sa.setAlbumId(r.getShowid());
                sa.setSubTitle(StringEscapeUtils.unescapeJava(r.getSummary()));
                sa.setTip(StringEscapeUtils.unescapeJava(r.getCats()));
                albums.add(sa);
            }
        }
        if(albums.size() > 0)
            return albums;
        else
            return null;
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
                album.setVideosTotal(showInfo.getDetail().getEpisodeTotal());
            }

        }
    }

    private void getAlbumUpdateVideosCount(final SCAlbum album, final OnGetAlbumDescListener listener) {
        String url = getShowVideosUrl(album.getAlbumId(),1,1);
        HttpUtils.asyncGet(url,new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) {
                    listener.onGetAlbumDescFailed("http response fail");
                    return;
                }
                String ret = response.body().string();
                try {
                    JSONObject jObject = new JSONObject(ret);
                    if( jObject.getInt("total") != 0) {
                        album.setVideosCount(jObject.getInt("total"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    listener.onGetAlbumDescFailed("Wrong Json");
                }
                listener.onGetAlbumDescSuccess(album);
            }
        });

    }

    @Override
    public void doGetAlbumDesc(final SCAlbum album, final OnGetAlbumDescListener listener) {

        String url = getShowInfoUrl(album.getAlbumId());
        if(url == null)
            listener.onGetAlbumDescFailed("wrong albumID");
        HttpUtils.asyncGet(url,new Callback() {
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

                    //获取Album更新的Videos个数
                    getAlbumUpdateVideosCount(album, listener);

                } catch (IOException e) {
                    listener.onGetAlbumDescFailed("io Exception");
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    public void doGetAlbumVideos(final SCAlbum album, int pageNo, int pageSize, final OnGetVideosListener listener) {
        String url = getShowVideosUrl(album.getAlbumId(),pageNo,pageSize);
        HttpUtils.asyncGet(url,new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) {
                    listener.onGetVideosFailed("response Unsuccessful");
                    return;
                }
                String ret = response.body().string();
                ShowVideos  videos = SailorCast.getGson().fromJson(ret,ShowVideos.class);

                SCVideos scVideos = new SCVideos();
                for (int i = 0; i < videos.getResults().size(); i++) {
                    SCVideo v = new SCVideo();
                    ShowVideosResult r = videos.getResults().get(i);
                    v.setVideoID(r.getVideoid());
                    v.setSeqInAlbum(r.getShowVideoseq());
                    v.setVideoTitle(r.getTitle());
                    v.setHorPic(r.getImg());
                    v.setSCSite(SCSite.YOUKU);
                    v.setAlbumID(album.getAlbumId());
                    scVideos.add(v);
                }
                if(scVideos.size() > 0)
                    listener.onGetVideosSuccess(scVideos);
                else
                    listener.onGetVideosFailed("null videos");
            }
        });

    }

    private String decrypt(String encrypted) {
        String password = "qwer3as2jin4fdsa";
        String ALG = "AES/ECB/NoPadding";
        byte[] keyStart;
        byte[] encryptedRaw = Base64.decode(encrypted,Base64.DEFAULT);
        try {
            keyStart = password.getBytes("utf-8");
            SecretKeySpec skeySpec = new SecretKeySpec(keyStart, ALG);
            Cipher cipher = Cipher.getInstance(ALG);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] decrypted = cipher.doFinal(encryptedRaw);
            return new String(decrypted,"UTF-8");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setVideoM3U8(SCVideo video, JSONObject videoJson) {

        JSONObject sidData = videoJson.optJSONObject("sid_data");
        if(sidData == null)
            return;
        String vid = video.getVideoID();
        String gdid = getGUID();
        String token = sidData.optString("token");
        String oip = sidData.optString("oip");
        String sid = sidData.optString("sid");
        String m3u8SD = null;
        String m3u8HD = null;
        String m3u8HD2 = null;
        JSONObject results = videoJson.optJSONObject("results");

        if (results.has("m3u8_flv"))
        {
            JSONArray jsonArray = results.optJSONArray("m3u8_flv");
            if ((jsonArray != null) && (jsonArray.length() > 0))
            {
                JSONObject localJSON = jsonArray.optJSONObject(0);
                if (localJSON != null)
                {
                    m3u8SD = localJSON.optString("url");
                    String m3u8 = getEncreptUrl(m3u8SD,vid,token,oip,sid,getGDID());
                    video.setM3U8Nor(m3u8);
                }
            }
        }

        if (results.has("m3u8_mp4"))
        {
            JSONArray jsonArray = results.optJSONArray("m3u8_mp4");
            if ((jsonArray != null) && (jsonArray.length() > 0))
            {
                JSONObject localJSON = jsonArray.optJSONObject(0);
                if (localJSON != null)
                {
                    m3u8HD = localJSON.optString("url");
                    String m3u8 = getEncreptUrl(m3u8HD,vid,token,oip,sid,getGDID());
                    video.setM3U8High(m3u8);
                }
            }
        }

        if (results.has("m3u8_hd"))
        {
            JSONArray jsonArray = results.optJSONArray("m3u8_hd");
            if ((jsonArray != null) && (jsonArray.length() > 0))
            {
                JSONObject localJSON = jsonArray.optJSONObject(0);
                if (localJSON != null)
                {
                    m3u8HD2 = localJSON.optString("url");
                    String m3u8 = getEncreptUrl(m3u8HD2,vid,token,oip,sid,getGDID());
                    video.setM3U8Super(m3u8);
                }
            }
        }

    }

    private byte[] encToByteArray(String txt) {
        char[] arrayOfChar = txt.toCharArray();
        byte[] arrayOfByte = new byte[arrayOfChar.length];
        for (int i = 0; i < arrayOfChar.length; i++)
            arrayOfByte[i] = ((byte)arrayOfChar[i]);
        return arrayOfByte;
    }

    private String getEncreptUrl(String url, String videoID, String token, String oip, String sid, String did)
    {
        Decryptions decryptions = new Decryptions();

        /*
        // 这里时测试代码，用的是youku破解出来的字符串。
        String plainTxt = "141984443099620f59ce3_XODU4MjM5NTky_9861";
        String ok = "PVZsdon1zT1W8rZC1vKUvHKxvhtN2nOn84fPFSC4zp7/CLS7cLkZwypPW2hDr4J9";
        String ok_ep = "PVZsdon1zT1W8rZC1vKUvHKxvhtN2nOn84fPFSC4zp7%2FCLS7cLkZwypPW2hDr4J9";
        String s = Base64.encodeToString(encToByteArray(decryptions.AESEnc(plainTxt, YOUKU_KEY)), Base64.NO_WRAP);
        if(s.equals(ok) && URLEncoder.encode(ok).equals(ok_ep)) {
            Log.d("fire4","!!!!!!!!!! passed");
        }
        */

        String plainTxt = sid + "_" + videoID + "_" + token;
        String s;
        String ep;
        s = Base64.encodeToString(encToByteArray(decryptions.AESEnc(plainTxt, YOUKU_KEY)), Base64.NO_WRAP);
        ep = URLEncoder.encode(s);

        return url + "&oip=" + oip + "&sid=" + sid + "&token=" + token + "&did=" + did + "&ev=1&ctype=20&ep=" + ep;
    }

    @Override
    public void doGetVideoPlayUrl(final SCVideo video, final OnGetVideoPlayUrlListener listener) {
        String url = getVideoInfoUrl(video.getVideoID());
        Request request = new Request.Builder().url(url).header(
                "User-Agent", "Youku;4.4;Android;4.3;Coolpad"
        ).build();

        HttpUtils.asyncGet(request, new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {

                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        String ret = response.body().string();
                        try {
                            JSONObject jObject = new JSONObject(ret);
                            String encryptData = jObject.optString("data");
                            if(encryptData != null && !encryptData.isEmpty()) {
                                String decryptData = decrypt(encryptData);
                                JSONObject videoJson = new JSONObject(decryptData);
                                setVideoM3U8(video, videoJson);
                                if (video.getM3U8Nor() != null)
                                    listener.onGetVideoPlayUrlNormal(video, video.getM3U8Nor());
                                if (video.getM3U8High() != null)
                                    listener.onGetVideoPlayUrlHigh(video, video.getM3U8High());
                                if (video.getM3U8Super() != null)
                                    listener.onGetVideoPlayUrlHigh(video, video.getM3U8Super());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    private  void doGetChannelAlbumsByUrl(String url, final OnGetAlbumsListener listener) {
        HttpUtils.asyncGet(url, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                listener.onGetAlbumsFailed("Http failure");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String ret = response.body().string();
                SCAlbums albums =  parseAlbumListResult(ret);
                if(albums != null) {
                    listener.onGetAlbumsSuccess(albums);
                } else
                    listener.onGetAlbumsFailed(SailorCast.getResource().getString(R.string.fail_reason_no_results));


            }
        });
    }

    @Override
    public void doGetChannelAlbums(SCChannel channel, int pageNo, int pageSize, final OnGetAlbumsListener listener) {
        String url = getChannelAlbumsListUrl(channel,pageNo,pageSize);
        doGetChannelAlbumsByUrl(url,listener);

    }

    private SCAlbums parseAlbumListResult(String ret) {
        try {
            JSONObject retJson = new JSONObject(ret);
            JSONArray results = retJson.optJSONArray("results");
            if(results != null) {
                SCAlbums albums = new SCAlbums();
                if(results.length() == 0)
                    return null;
                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    SCAlbum album = new SCAlbum(SCSite.YOUKU);
                    String tmp;

                    tmp = result.optString("title");
                    if (tmp != null && !tmp.isEmpty())
                        album.setTitle(tmp);

                    int type = result.optInt("type");

                    tmp = result.optString("img");
                    if (tmp != null && !tmp.isEmpty()) {
                        if (type == 1)
                            album.setHorImageUrl(tmp);
                        if (type == 2)
                            album.setVerImageUrl(tmp);
                    }

                    tmp = result.optString("tid");
                    if (tmp != null && !tmp.isEmpty())
                        album.setAlbumId(tmp);

                    tmp = result.optString("subtitle");
                    if (tmp != null && !tmp.isEmpty())
                        album.setSubTitle(tmp);

                    tmp = result.optString("stripe");
                    if (tmp != null && !tmp.isEmpty())
                        album.setTip(tmp);

                    albums.add(album);
                }
                return albums;
            }
            else
                return null;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void doGetChannelAlbumsByFilter(SCChannel channel, int pageNo, int pageSize, SCChannelFilter filter, OnGetAlbumsListener listener) {
        String url = getChannelAlbumsListUrlByFilter(channel,filter,pageNo,pageSize);
        doGetChannelAlbumsByUrl(url,listener);
    }

    @Override
    public void doGetChannelFilter(SCChannel channel, final OnGetChannelFilterListener listener) {
        String url = getChannelFilterUrl(channel);
        if(url == null) {
            listener.onGetChannelFilterFailed("wrong channel");
            return;
        }
        HttpUtils.asyncGet(url, new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                listener.onGetChannelFilterFailed("Http failed");
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String ret = response.body().string();
                try {
                    JSONObject retJson = new JSONObject(ret);
                    JSONObject resultsJson = retJson.optJSONObject("results");
                    if(resultsJson != null) {
                        JSONArray filterJsonArray = resultsJson.optJSONArray("filter");
                        if(filterJsonArray != null && filterJsonArray.length() > 0) {
                            SCChannelFilter filter = new SCChannelFilter();
                            for (int i = 0; i < filterJsonArray.length(); i++) {
                                JSONObject filterJson = filterJsonArray.getJSONObject(i);
                                JSONArray filterItemsJson = filterJson.optJSONArray("items");
                                String key = filterJson.optString("cat");
                                ArrayList<SCChannelFilterItem> filterItems = new ArrayList<SCChannelFilterItem>();
                                for (int j = 0; j < filterItemsJson.length(); j++) {
                                    JSONObject filterItemJson = filterItemsJson.getJSONObject(j);
                                    String searchVal = filterItemJson.optString("value");
                                    String displayName = filterItemJson.optString("title");
                                    if(searchVal == null)
                                        searchVal = "";
                                    SCChannelFilterItem filterItem = new SCChannelFilterItem(searchVal,displayName);
                                    filterItem.setParentKey(key);
                                    filterItem.setSearchKey(key);
                                    filterItems.add(filterItem);
                                }
                                filter.addFilter(key,filterItems);
                            }
                            listener.onGetChannelFilterSuccess(filter);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
