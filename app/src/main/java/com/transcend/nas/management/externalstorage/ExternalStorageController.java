package com.transcend.nas.management.externalstorage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.transcend.nas.DrawerMenuActivity;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.FileManageFragmentActivity;

/**
 * Created by steve_su on 2016/12/26.
 */

public class ExternalStorageController {
    private static final String TAG = ExternalStorageController.class.getSimpleName();
    private Context mContext;
    private AbstractExternalStorage mInstance;

    public ExternalStorageController(Context context) {
        mContext = context;
        mInstance = getInstance();
    }

    public void onDrawerItemSelected(DrawerMenuActivity activity, int itemId) {
        mInstance.onNavigationItemSelected(activity, itemId);
    }

    public void onActivityResult(FileManageActivity activity, Intent data) {
        mInstance.onActivityResult(activity, data);
    }
    public void onActivityResult(FileManageFragmentActivity activity, Intent data) {
        mInstance.onActivityResult(activity, data);
    }

    public boolean isWritePermissionNotGranted() {
        return mInstance.isWritePermissionNotGranted();
    }

    public boolean isWritePermissionRequired(String... path) {
        return mInstance.isWritePermissionRequired(path);
    }

    public void handleWriteOperationFailed() {
        mInstance.handleWriteOperationFailed();
    }

    public Uri getSDFileUri(String path) {
        return mInstance.getSDFileUri(path);
    }

    private AbstractExternalStorage getInstance() {
        AbstractExternalStorage instance;
        final int version = Build.VERSION.SDK_INT;
        Log.d(TAG, "sdk version: "+ version);
        if (version > Build.VERSION_CODES.KITKAT) {                          // Android 5.0, 6.0, 7.0.
            instance = new ExternalStorageLollipop(mContext);
        } else if (version == Build.VERSION_CODES.KITKAT) {                  // Android 4.4
            instance = new ExternalStorageKitKat(mContext);
        } else {                                                             // Android 4.1, 4.2, 4.3
            instance = new ExternalStorageJellyBean(mContext);
        }

        return instance;
    }

}
