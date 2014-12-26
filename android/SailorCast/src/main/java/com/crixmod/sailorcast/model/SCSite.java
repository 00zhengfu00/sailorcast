package com.crixmod.sailorcast.model;

/**
 * Created by fire3 on 2014/12/26.
 */
public class SCSite {
    public static int UNKNOWN = 0;
    public static int SOHU = 1;
    public static int IQIYI = 2;
    public static int YOUKU = 3;
    private String mSiteName;

    private int mSiteID;

    public SCSite(int mSiteID) {
        this.mSiteID = mSiteID;
        if(mSiteID == SOHU)
            mSiteName = "Sohu";
        if(mSiteID == YOUKU)
            mSiteName = "Youku";
        if(mSiteID == IQIYI)
            mSiteName = "IQiyi";
        if(mSiteID == UNKNOWN)
            mSiteName = "Unknown";
    }

    @Override
    public String toString() {
        return "SCSite{" +
                "mSiteID=" + mSiteName +
                '}';
    }
}
