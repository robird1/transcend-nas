package com.transcend.nas.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by ike_lee on 2017/3/1.
 */
public class CustomNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "CustomReceiver";
    public static final String NOTIFICATION_KEY = "notification_id";
    public static final String NOTIFICATION_CANCEL = "notification_cancelled";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent != null) {
            String action = intent.getAction();
            if(NOTIFICATION_CANCEL.equals(action)) {
                int id = intent.getIntExtra(NOTIFICATION_KEY, -1);
                Log.d(TAG, "onReceiver: " + action + ", " + id);
                CustomNotificationManager.getInstance().cancelTask(id);
            }
        }
    }
}


