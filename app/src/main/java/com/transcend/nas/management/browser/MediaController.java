package com.transcend.nas.management.browser;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuItem;

import com.transcend.nas.management.browser_framework.BrowserData;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/7/20.
 */

public class MediaController {
    private MediaType mControl;
    private Context mContext;

    public MediaController(Context context, int position) {
        mContext = context;
        mControl = getInstance(position);
    }

    public boolean createOptionsMenu(Menu menu) {
        return mControl.createOptionsMenu(menu);
    }

    public boolean optionsItemSelected(MenuItem item) {
        return mControl.optionsItemSelected(item);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        mControl.onPrepareOptionsMenu(menu);
    }

    public void load(int position) { mControl.load(position);}

    public Loader onCreateLoader(int id, Bundle args) {
        return mControl.onCreateLoader(id, args);
    }

    public void onLoadFinished(Loader loader, ArrayList data) {
        mControl.onLoadFinished(loader, data);
    }

    private MediaType getInstance(int position) {
        if (position == BrowserData.ALL.getTabPosition()) {
            return new MediaAll(mContext);
        } else if (position == BrowserData.PHOTO.getTabPosition()) {
            return new MediaPhoto(mContext);
        } else if (position == BrowserData.MUSIC.getTabPosition()) {
            return new MediaMusic(mContext);
        } else if (position == BrowserData.VIDEO.getTabPosition()) {
            return new MediaVideo(mContext);
        }
        return new MediaAll(mContext);
    }
}
