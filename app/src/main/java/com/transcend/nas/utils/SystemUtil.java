package com.transcend.nas.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by ikelee on 17/5/10.
 */
public class SystemUtil {

    public static boolean isWifiMode(Context context) {
        boolean isWifi = false;
        ConnectivityManager mConnMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mConnMgr.getActiveNetworkInfo();
        if(info != null)
            isWifi = (info.getType() == ConnectivityManager.TYPE_WIFI);
        return isWifi;
    }

}
