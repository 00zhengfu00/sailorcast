package com.crixmod.sailorcast;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.crixmod.sailorcast.model.SCAlbum;
import com.crixmod.sailorcast.model.SCAlbums;
import com.crixmod.sailorcast.siteapi.OnGetAlbumDescListener;
import com.crixmod.sailorcast.siteapi.OnSearchRequestListener;
import com.crixmod.sailorcast.siteapi.SohuApi;
import com.crixmod.sailorcast.siteapi.YouKuApi;
import com.crixmod.sailorcast.ui.SCDrawerActivity;

import java.util.ArrayList;


public class MainActivity extends SCDrawerActivity
        implements OnSearchRequestListener,OnGetAlbumDescListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        createMenuDrawer(R.layout.activity_main);
        setSupportActionBar(getToolbar());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(getString(R.string.hello_world));
        }

        new YouKuApi().doSearch("奔跑吧兄弟", this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSearchSuccess(SCAlbums albums) {
        albums.debugLog();
        for (int i = 0; i < albums.size(); i++) {
            new YouKuApi().doGetAlbumDesc(albums.get(i),this);
        }
    }

    @Override
    public void onSearchFailed(String failReason) {

    }

    @Override
    public void onGetAlbumDescSuccess(SCAlbum album) {
        Log.d("fire3", album.toString());
    }

    @Override
    public void onGetAlbumDescFailed(String failReason) {

    }
}
