package com.crixmod.sailorcast.siteapi;

import com.crixmod.sailorcast.model.SCAlbum;
import com.crixmod.sailorcast.model.SCAlbums;

import java.util.ArrayList;

/**
 * Created by fire3 on 2014/12/26.
 */
public interface OnGetAlbumsListener {
    public void onGetAlbumsSuccess(SCAlbums albums);
    public void onGetAlbumsFailed(String failReason);
}
