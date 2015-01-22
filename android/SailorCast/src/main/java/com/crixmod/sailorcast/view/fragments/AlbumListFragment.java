package com.crixmod.sailorcast.view.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.crixmod.sailorcast.R;
import com.crixmod.sailorcast.model.SCAlbum;
import com.crixmod.sailorcast.model.SCAlbums;
import com.crixmod.sailorcast.model.SCChannel;
import com.crixmod.sailorcast.model.SCSite;
import com.crixmod.sailorcast.siteapi.OnGetAlbumsListener;
import com.crixmod.sailorcast.siteapi.SiteApi;
import com.crixmod.sailorcast.uiutils.pagingridview.PagingGridView;
import com.crixmod.sailorcast.view.adapters.AlbumListAdapter;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AlbumListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AlbumListFragment extends Fragment implements OnGetAlbumsListener{
    private static final String ARG_CHANNEL_ID = "channelID";
    private static final String ARG_SITE_ID = "siteID";

    private int mChannelID;
    private int mSiteID;
    private PagingGridView mGridView;
    private int mPageNo = 0;
    private int mPageSize = 30;
    private AlbumListAdapter mAdapter;
    private int mColumns = 3;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AlbumListFragment.
     */
    public static AlbumListFragment newInstance(int mSiteID, int mChannelID) {
        AlbumListFragment fragment = new AlbumListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CHANNEL_ID, mChannelID);
        args.putInt(ARG_SITE_ID, mSiteID);
        fragment.setArguments(args);
        return fragment;
    }

    public AlbumListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSiteID = getArguments().getInt(ARG_SITE_ID);
            mChannelID = getArguments().getInt(ARG_CHANNEL_ID);
            loadMoreAlbums();
            mAdapter = new AlbumListAdapter(getActivity());
            if(mSiteID == SCSite.LETV)
                mAdapter.setColumns(2);

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_album_list, container, false);
        mGridView = (PagingGridView) view.findViewById(R.id.result_grid);
        if(mSiteID == SCSite.LETV)
            mGridView.setNumColumns(2);
        mGridView.setAdapter(mAdapter);
        mGridView.setHasMoreItems(true);
        mGridView.setPagingableListener(new PagingGridView.Pagingable() {
            @Override
            public void onLoadMoreItems() {
                loadMoreAlbums();
            }
        });
        return view;
    }

    public void loadMoreAlbums() {
        mPageNo ++ ;
        SiteApi.doGetChannelAlbums(mSiteID,mChannelID,mPageNo,mPageSize,this);
    }


    @Override
    public synchronized void onGetAlbumsSuccess(SCAlbums albums) {

        if (mColumns == 3 && albums.get(0).getVerImageUrl() == null) {
            mColumns = 2;
            mGridView.post(new Runnable() {
                @Override
                public void run() {
                    mGridView.setNumColumns(mColumns);
                    mAdapter.setColumns(mColumns);
                }
            });
        }

        for(SCAlbum a : albums) {
            mAdapter.addAlbum(a);
        }
        mGridView.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
                mGridView.setIsLoading(false);
            }
        });
    }

    @Override
    public void onGetAlbumsFailed(String failReason) {

        mGridView.post(new Runnable() {
            @Override
            public void run() {
                mGridView.setIsLoading(false);
                mGridView.setHasMoreItems(false);
            }
        });
    }
}
