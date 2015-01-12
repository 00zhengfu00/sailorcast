package com.crixmod.sailorcast.view;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.crixmod.sailorcast.R;
import com.crixmod.sailorcast.SailorCast;
import com.crixmod.sailorcast.model.SCAlbum;
import com.crixmod.sailorcast.model.SCVideo;

import java.util.Observer;

public class RenderActivity extends ActionBarActivity {

    SCAlbum mAlbum;
    SCVideo mVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlbum = getIntent().getParcelableExtra("album");
        mVideo = getIntent().getParcelableExtra("video");
        setContentView(R.layout.activity_render);

		Fragment rendererFragment = getSupportFragmentManager().findFragmentById(R.id.RendererFragment);
		if (rendererFragment != null && rendererFragment instanceof Observer)

			SailorCast.upnpServiceController.addSelectedRendererObserver((Observer) rendererFragment);
		else
			Log.w("fire3", "No rendererFragment yet !");

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_render, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static void launch(Activity activity, SCAlbum album, SCVideo video) {
        Intent mpdIntent = new Intent(activity, RenderActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("video",video)
                .putExtra("album",album);

        activity.startActivity(mpdIntent);
    }

}
