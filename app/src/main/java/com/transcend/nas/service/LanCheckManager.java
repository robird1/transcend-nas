package com.transcend.nas.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;

import com.transcend.nas.NASApp;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class LanCheckManager implements LanCheckTask.LanCheckCallback {
    private String TAG = "LanCheckManager";

    private static LanCheckManager mLanCheckManager;
    private static final Object mMute = new Object();

    private static final String TYPE = "_http._tcp.";
    private static final String SERVER = " - Web administration";

    private ArrayList<NsdServiceInfo> mServiceInfoList;
    private HashMap<String, String> mServiceList;
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private boolean isStartResolve = false;

    private boolean mLanConnect = false;
    private String mLanIP = "";
    private Handler mHandler;
    private Thread mThread;
    private LanCheckTask mTask;
    private boolean isReady = false;
    private boolean isInit = false;

    public LanCheckManager() {
        mHandler = new Handler();
        mNsdManager = (NsdManager) NASApp.getContext().getSystemService(Context.NSD_SERVICE);
        mServiceList = new HashMap<>();
        mServiceInfoList = new ArrayList<>();
    }

    public static LanCheckManager getInstance() {
        synchronized (mMute) {
            if (mLanCheckManager == null)
                mLanCheckManager = new LanCheckManager();
        }
        return mLanCheckManager;
    }

    /**
     * LanCheck Module
     */
    public void setInit(boolean init) {
        isInit = init;
    }

    public void setLanConnect(boolean connect, String ip) {
        setLanIP(ip);
        setLanConnect(connect);
    }

    public void setLanConnect(boolean connect) {
        Log.d(TAG, "LanConnect : " + connect);
        mLanConnect = connect;
        isReady = true;
    }

    public boolean getLanConnect() {
        return isReady & mLanConnect;
    }

    public void setLanIP(String ip) {
        mLanIP = ip;
    }

    public String getLanIP() {
        return mLanIP;
    }

    public void stopLanCheck() {
        isReady = false;
        if (mTask != null && !mTask.isCancelled())
            mTask.cancel(true);
        mTask = null;

        if (mThread != null)
            mThread.interrupt();
        mThread = null;
    }

    public void startLanCheck() {
        if (!isInit) {
            Log.d(TAG, "Ignore the command");
            return;
        }

        boolean check = false;
        stopLanCheck();

        ConnectivityManager connectivityManager = (ConnectivityManager) NASApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            int type = info.getType();
            check = type == ConnectivityManager.TYPE_WIFI;
        }

        if (check) {
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
            try {
                mTask.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onLanCheckFinished(boolean success, String ip) {
        setLanConnect(success, ip);
    }


    /**
     * NsdManager Module
     */
    public ArrayList<HashMap<String, String>> getAndroidDiscoveryList() {
        ArrayList<HashMap<String, String>> nasList = new ArrayList<HashMap<String, String>>();
        for (String name : mServiceList.keySet()) {
            HashMap<String, String> nas = new HashMap<String, String>();
            nas.put("nasId", "-1");
            nas.put("nickname", mServiceList.get(name));
            nas.put("hostname", name);
            nasList.add(nas);
        }
        return nasList;
    }

    public void startAndroidDiscovery() {
        mServiceList.clear();
        mServiceInfoList.clear();
        ConnectivityManager connectivityManager = (ConnectivityManager) NASApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            int type = info.getType();
            switch (type) {
                case ConnectivityManager.TYPE_WIFI:
                    stopAndroidDiscovery();  // Cancel any existing discovery request
                    initializeResolveListener();
                    initializeDiscoveryListener();
                    mNsdManager.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
                    break;
            }
        }
    }

    public void stopAndroidDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //do nothing
            }
            mDiscoveryListener = null;
        }

        isStartResolve = false;
        if (mResolveListener != null)
            mResolveListener = null;
    }

    private void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                if (!service.getServiceType().equals(TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().contains(SERVER)) {
                    Log.d(TAG, "Service discovery success : " + service);
                    mServiceInfoList.add(service);
                    if (!isStartResolve) {
                        isStartResolve = true;
                        mNsdManager.resolveService(service, mResolveListener);
                    }
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded. " + serviceInfo);
                String nickname = serviceInfo.getServiceName();
                String hostname = serviceInfo.getHost().getHostAddress();
                if (nickname.contains(SERVER) && !hostname.contains(":")) {
                    //because the hostname(ip) is unique, use hostname as key
                    nickname = nickname.replace(SERVER, "");
                    mServiceList.put(hostname, nickname);
                }

                //get next service in mServiceInfoList
                int size = mServiceInfoList.size();
                if (size > 0) {
                    mServiceInfoList.remove(0);
                    if (size - 1 > 0) {
                        mNsdManager.resolveService(mServiceInfoList.get(0), mResolveListener);
                        return;
                    }
                }

                isStartResolve = false;
            }
        };
    }
}
