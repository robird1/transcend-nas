package com.transcend.nas.management.externalstorage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.transcend.nas.DrawerMenuActivity;
import com.transcend.nas.management.FileManageActivity;

import java.io.File;

/**
 * Created by steve_su on 2016/12/26.
 */

public abstract class AbstractExternalStorage {
    private Context mContext;

    public AbstractExternalStorage(Context context) {
        mContext = context;
    }

    public boolean isWritePermissionNotGranted() {
        return false;
    }

    public boolean isWritePermissionRequired(String... path) {
        return false;
    }

    public void handleWriteOperationFailed() {

    }

    public Uri getSDFileUri(String path) {
       return Uri.fromFile(new File(path));
    }

    protected Context getContext() {
        return mContext;
    }

    protected void onNavigationItemSelected(DrawerMenuActivity activity, int itemId) {

    }

    protected void onActivityResult(FileManageActivity activity, Intent data) {

    }

}
