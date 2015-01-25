package com.crixmod.sailorcast.utils;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import com.crixmod.sailorcast.SailorCast;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by fire3 on 14-12-30.
 */
public class HttpUtils {

    private static final String REQUEST_TAG = "okhttp";

    public static Request buildRequest(String url) {

        if (SailorCast.isNetworkAvailable()) {
            int maxAge = 60 * 60 * 24; // read from cache for 1 day
            Request request = new Request.Builder()
                    .tag(REQUEST_TAG)
                    .addHeader("Cache-Control", "public, max-age=" + maxAge)
                    //.addHeader("Cache-Control", "public, max-age=" + maxAge)
                    .url(url)
                    .build();
            Log.d("HttpUtils", "request Url: " + url);
            return request;
        } else {
           int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
             Request request = new Request.Builder()
                    .tag(REQUEST_TAG)
                     .addHeader("Cache-Control",
                             "public, only-if-cached, max-stale=" + maxStale)
                    .url(url)
                    .build();
            Log.d("HttpUtils", "request Url: " + url);
            return request;
        }
    }

    public static void asyncGet(String url, Activity activity, Callback callback) {

        Request request = buildRequest(url);
        asyncGet(request,activity,callback);
    }

    public static String syncGet(String url) {
        Request request = buildRequest(url);
        try {
            Response response =  SailorCast.getHttpClient().newCall(request).execute();
            if (response.code() == 200) {
                String ret = new String(response.body().bytes(), "utf-8");
                return ret;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void asyncGet(String url, Callback callback) {
        Request request = buildRequest(url);
        asyncGet(request, callback);
    }

    public static void asyncGet(Request request, Callback callback) {
        Log.d("HttpUtils", "request Url: " + request.urlString());
        SailorCast.getHttpClient().newCall(request).enqueue(callback);
    }

    public static void asyncGet(Request request, final Activity activity, final Callback callback) {

        Log.d("HttpUtils", "request Url: " + request.urlString());

        SailorCast.getHttpClient().newCall(request).enqueue(new Callback() {
            Handler mainHandler = new Handler(activity.getMainLooper());
            @Override
            public void onFailure(final Request request, final IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(request,e);
                    }
                });
            }

            @Override
            public void onResponse(final Response response) throws IOException {

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.onResponse(response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    public static void cancelAll() {
        SailorCast.getHttpClient().cancel(REQUEST_TAG);
    }


}
