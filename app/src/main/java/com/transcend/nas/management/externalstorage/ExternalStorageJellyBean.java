package com.transcend.nas.management.externalstorage;

import android.content.Context;

import com.transcend.nas.DrawerMenuActivity;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;

/**
 * Created by steve_su on 2016/12/26.
 */

public class ExternalStorageJellyBean extends AbstractExternalStorage {
    public ExternalStorageJellyBean(Context context) {
        super(context);
    }

    @Override
    protected void onNavigationItemSelected(DrawerMenuActivity activity, int itemId) {
        NASApp.ROOT_SD = NASUtils.getSDLocation(getContext());
        activity.startFileManageActivity(itemId);
    }
}