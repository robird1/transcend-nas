package com.transcend.nas.service;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.transcend.nas.management.download.DownloadFactory;

/**
 * Created by ike_lee on 2016/4/18.
 */
public class LanCheckReceiver extends BroadcastReceiver {
    private static final String TAG = "LanCheckReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "onReceiver: " + action);
            LanCheckManager.getInstance().startLanCheck();
            DownloadFactory.getManager(context, DownloadFactory.Type.PERSIST).cancel();
        }
    }
}


