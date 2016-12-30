package com.transcend.nas.management;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.transcend.nas.settings.BaseDrawerActivity;

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

    public void onDrawerItemSelected(BaseDrawerActivity activity, int itemId) {
        mInstance.onNavigationItemSelected(activity, itemId);
    }

    public void onActivityResult(FileManageActivity activity, Intent data) {
        mInstance.onActivityResult(activity, data);
    }

    public String getSDLocation() {
        return mInstance.getSDLocation();
    }

    private AbstractExternalStorage getInstance() {
        AbstractExternalStorage instance;
        final int version = Build.VERSION.SDK_INT;
        Log.d(TAG, "sdk version: "+ version);
        if (version > 19) {                          // Android 5.0, 6.0, 7.0, ...
            instance = new ExternalStorageLollipop(mContext);
        } else if (version == 19) {                  // KIKAT 4.4
            instance = new ExternalStorageKitKat(mContext);
        } else {                                     // Android 4.3, 4.2, 4.1
            instance = new ExternalStorageJellyBean(mContext);
        }

        return instance;
    }

}
