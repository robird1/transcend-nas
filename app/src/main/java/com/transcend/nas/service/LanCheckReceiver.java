package com.transcend.nas.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
        }
    }
}


