package com.transcend.nas.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by ike_lee on 2016/4/18.
 */
public class LanCheckReceiver extends BroadcastReceiver {
    private static final String TAG = "LanCheckReceiver";
    private ConnectivityManager connectivityManager;
    private NetworkInfo info;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "onReceiver: " + action);
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            info = connectivityManager.getActiveNetworkInfo();
            if (info != null && info.isAvailable()) {
                int type = info.getType();
                switch (type) {
                    case ConnectivityManager.TYPE_WIFI:
                        LanCheckManager.getInstance().startLanCheck();
                        break;
                    default:
                        LanCheckManager.getInstance().stopLanCheck();
                        LanCheckManager.getInstance().setLanConnect(false);
                        break;
                }
            } else {
                LanCheckManager.getInstance().stopLanCheck();
                LanCheckManager.getInstance().setLanConnect(false);
            }
        }
    }
}


