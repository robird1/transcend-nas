package com.transcend.nas.management.externalstorage;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.settings.BaseDrawerActivity;

import java.io.File;
import java.util.List;

/**
 * Created by steve_su on 2016/12/26.
 */

public abstract class AbstractExternalStorage {
    private static final String TAG = AbstractExternalStorage.class.getSimpleName();
    private Context mContext;

    public AbstractExternalStorage(Context context) {
        mContext = context;
    }


    protected String getSDLocation() {
        List<File> stgList = NASUtils.getStoragePath(mContext);
        if (stgList.size() > 1) {
            for (File sd : stgList) {
                if ((!sd.getAbsolutePath().contains(NASApp.ROOT_STG)) && (!sd.getAbsolutePath().toLowerCase().contains("usb"))) {
                    Log.d(TAG, "sd.getAbsolutePath(): "+ sd.getAbsolutePath());
                    return sd.getAbsolutePath();
                }
            }
        }

        return null;
    }

    public boolean isWritePermissionNotGranted() {
        return false;
    }

    public boolean isWritePermissionRequired(String... path) {return false;}

    public void handleWriteOperationFailed() {}

    protected Context getContext() {
        return mContext;
    }

    protected void onNavigationItemSelected(BaseDrawerActivity activity, int itemId) {

    }

    protected void onActivityResult(FileManageActivity activity, Intent data) {

    }

}
