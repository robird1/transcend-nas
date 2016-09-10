package com.transcend.nas.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.Log;

import com.transcend.nas.NASApp;

public class LanCheckManager implements LanCheckTask.LanCheckCallback {
    private String TAG = "LanCheckManager";

    private static LanCheckManager mLanCheckManager;
    private static final Object mMute = new Object();

    private boolean mLanConnect = false;
    private String mLanIP = "";
    private Handler mHandler;
    private Thread mThread;
    private LanCheckTask mTask;
    private boolean isReady = false;

    public LanCheckManager() {
        mHandler = new Handler();
    }

    public static LanCheckManager getInstance() {
        synchronized (mMute) {
            if (mLanCheckManager == null)
                mLanCheckManager = new LanCheckManager();
        }
        return mLanCheckManager;
    }

    public void setLanConnect(boolean connect, String ip){
        setLanIP(ip);
        setLanConnect(connect);
    }

    public void setLanConnect(boolean connect){
        Log.d(TAG, "LanConnect : " + connect);
        mLanConnect = connect;
        isReady = true;
    }

    public boolean getLanConnect(){
        return isReady & mLanConnect;
    }

    public void setLanIP(String ip){
        mLanIP = ip;
    }

    public String getLanIP(){
        return mLanIP;
    }

    public void stopLanCheck(){
        isReady = false;
        if(mTask != null && !mTask.isCancelled())
            mTask.cancel(true);
        mTask = null;

        if (mThread != null)
            mThread.interrupt();
        mThread = null;
    }

    public void startLanCheck(){
        boolean check = false;
        stopLanCheck();

        ConnectivityManager connectivityManager = (ConnectivityManager) NASApp.mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            int type = info.getType();
            check = type == ConnectivityManager.TYPE_WIFI;
        }

        if(check) {
            mTask = new LanCheckTask();
            mTask.addListener(LanCheckManager.this);
            mThread = new Thread() {
                public void run() {
                    mHandler.post(runnable);
                }
            };
            mThread.start();
        } else {
            setLanConnect(false, "");
        }
    }

    private Runnable runnable = new Runnable() {
        public void run() {
            mTask.execute();
        }
    };

    @Override
    public void onLanCheckFinished(boolean success, String ip) {
        setLanConnect(success, ip);
    }
}
