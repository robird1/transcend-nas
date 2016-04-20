package com.transcend.nas.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import jcifs.smb.SmbException;

/**
 * Created by ike_lee on 2016/4/18.
 */
//繼承自BroadcastReceiver的廣播接收元件
public class RemoteAccessService extends BroadcastReceiver {
    // 接收廣播後執行這個方法
    // 第一個參數Context物件，用來顯示訊息框、啟動服務
    // 第二個參數是發出廣播事件的Intent物件，可以包含資料
    private static final String TAG = "RemoteAccessService";
    private ConnectivityManager connectivityManager;
    private NetworkInfo info;
    private boolean init = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(!init) {
            init = true;
            return;
        }

        // 執行廣播元件的工作
        String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            info = connectivityManager.getActiveNetworkInfo();
            if (info != null && info.isAvailable()) {
                Log.d(TAG, info.getTypeName() + " network action ");
                P2PService.getInstance().reStartP2PConnect();
            } else {
                P2PService.getInstance().stopP2PConnect();
            }
        }
    }
}


