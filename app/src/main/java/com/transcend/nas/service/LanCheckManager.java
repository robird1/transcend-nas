package com.transcend.nas.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.tutk.IOTC.P2PService;

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
    private Thread mThread;
    private boolean mThreadRunning = false;
    private boolean isInit = false;

    public LanCheckManager() {
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
    public void initLanCheck() {
        if (!isInit) {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = server.getHostname();
            if (hostname.contains(P2PService.getInstance().getP2PIP())) {
                setLanConnect(false, "");
                startLanCheck();
            } else {
                setLanConnect(true, hostname);
            }
        }

        isInit = true;
    }

    public void destroy() {
        setLanConnect(false, "");
        isInit = false;
    }

    public void setLanConnect(boolean connect, String ip) {
        setLanIP(ip);
        setLanConnect(connect);
    }

    public void setLanConnect(boolean connect) {
        Log.d(TAG, "LanConnect : " + connect);
        mLanConnect = connect;
    }

    public boolean getLanConnect() {
        return mLanConnect;
    }

    public void setLanIP(String ip) {
        mLanIP = ip;
    }

    public String getLanIP() {
        return mLanIP;
    }

    public void stopLanCheck() {
        if (mThread != null && mThread.isAlive()) {
            mThread.interrupt();
        }
        mThread = null;
    }

    public void startLanCheck() {
        if (!isInit) {
            Log.d(TAG, "Ignore the command");
            return;
        }

        if (!mThreadRunning) {
            mThreadRunning = true;
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    LanCheckTask mTask = new LanCheckTask();
                    mTask.addListener(LanCheckManager.this);
                    mTask.execute();
                    mThreadRunning = false;
                }
            });
            mThread.start();
        }
    }

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
        if (mNsdManager == null)
            mNsdManager = (NsdManager) NASApp.getContext().getSystemService(Context.NSD_SERVICE);

        ConnectivityManager connectivityManager = (ConnectivityManager) NASApp.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            int type = info.getType();
            switch (type) {
                case ConnectivityManager.TYPE_WIFI:
                    stopAndroidDiscovery();  // Cancel any existing discovery request
                    initializeResolveListener();
                    initializeDiscoveryListener();
                    if (mNsdManager != null)
                        mNsdManager.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
                    break;
            }
        }
    }

    public void stopAndroidDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                if (mNsdManager != null)
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
                        if (mNsdManager != null)
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
                        if (mNsdManager != null)
                            mNsdManager.resolveService(mServiceInfoList.get(0), mResolveListener);
                        return;
                    }
                }

                isStartResolve = false;
            }
        };
    }
}
