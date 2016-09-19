package com.tutk.IOTC;

import java.util.ArrayList;
import java.util.List;

import com.transcend.nas.NASPref;
import com.transcend.nas.service.LanCheckManager;
import com.tutk.IOTC.P2PTunnelAPIs.IP2PTunnelCallback;

import android.util.Log;

public class P2PService implements IP2PTunnelCallback {
    private String TAG = "P2PService";
    private P2PTunnelAPIs m_commApis = null;
    private int m_nInit = -1;
    private int nStart = -1;
    private int mBufferSize = 512000;
    private String mLocalHost = "127.0.0.1";
    private List<IP2PTunnelCallback> mListener;
    private List<P2PProtocal> mProtocal;
    private String mUUID = "";

    private static P2PService mP2PService;
    private static final Object mMute = new Object();

    public P2PService() {
        mListener = new ArrayList<IP2PTunnelCallback>();
        mProtocal = new ArrayList<P2PProtocal>();
        addP2PProtocal(P2PProtocalType.HTTP, 8000, 80);
        addP2PProtocal(P2PProtocalType.SMB, 9000, 445);
        if(NASPref.useTwonkyServer)
            addP2PProtocal(P2PProtocalType.TWONKY, 10000, 9000);
    }

    public static P2PService getInstance() {
        synchronized (mMute) {
            if (mP2PService == null)
                mP2PService = new P2PService();
        }
        return mP2PService;
    }

    public void addP2PProtocal(P2PProtocalType type, int localPort, int remotePort) {
        P2PProtocal obj = new P2PProtocal(type, localPort, remotePort);
        for (P2PProtocal protocal : mProtocal) {
            if (protocal != null && protocal.getType() == type) {
                protocal.setLocalPort(localPort);
                protocal.setRemotePort(remotePort);
                return;
            }
        }
        mProtocal.add(obj);
    }

    public void remveP2PProtocal(P2PProtocalType type) {
        for (P2PProtocal protocal : mProtocal) {
            if (protocal != null && protocal.getType() == type) {
                mProtocal.remove(protocal);
                break;
            }
        }
    }

    public boolean isP2POnline(String strUID) {
        if (strUID.length() < 20) {
            Log.d(TAG, "P2P UID is short < 20");
            return false;
        }

        P2PTunnelAPIs commApis = new P2PTunnelAPIs(this);
        int init = commApis.P2PTunnelAgentInitialize(4);
        Log.d(TAG, "P2PTunnel m_nInit=" + init);
        String username = "Tutk.com", password = "P2P Platform";
        if (username.length() < 64) {
            for (int i = 0; username.length() < 64; i++) {
                username += "\0";
            }
        }
        if (password.length() < 64) {
            for (int i = 0; password.length() < 64; i++) {
                password += "\0";
            }
        }

        byte[] baAuthData = (username + password).getBytes();
        int[] pnErrFromDeviceCB = new int[1];
        int start = commApis.P2PTunnelAgent_Connect(strUID, baAuthData, baAuthData.length, pnErrFromDeviceCB);
        Log.d(TAG, "P2PTunnelAgent_Connect(.) UID=" + strUID);
        Log.d(TAG, "P2PTunnelAgent_Connect(.)=" + start);
        Log.d(TAG, "P2PTunnelAgent_Connect(.) Error Message=" + pnErrFromDeviceCB[0]);

        if (start >= 0)
            commApis.P2PTunnelAgent_Disconnect(start);
        commApis.P2PTunnelAgentDeInitialize();
        return start >= 0;
    }

    public int startP2PConnect(String strUID) {
        if (strUID.length() < 20) {
            Log.d(TAG, "P2P UID is short < 20");
            return P2PTunnelAPIs.TUNNEL_ER_INITIALIZED;
        }

        mUUID = strUID;
        if (nStart < 0) {
            m_commApis = new P2PTunnelAPIs(this);
            m_nInit = m_commApis.P2PTunnelAgentInitialize(4);
            Log.d(TAG, "P2PTunnel m_nInit=" + m_nInit);
            if (m_commApis == null)
                return nStart;

            String username = "Tutk.com", password = "P2P Platform";
            //P2P Platform
            if (username.length() < 64) {
                for (int i = 0; username.length() < 64; i++) {
                    username += "\0";
                }
            }
            if (password.length() < 64) {
                for (int i = 0; password.length() < 64; i++) {
                    password += "\0";
                }
            }
            byte[] baAuthData = (username + password).getBytes();
            int[] pnErrFromDeviceCB = new int[1];

            nStart = m_commApis.P2PTunnelAgent_Connect(strUID, baAuthData, baAuthData.length, pnErrFromDeviceCB);
            Log.d(TAG, "P2PTunnelAgent_Connect(.)=" + nStart);
            Log.d(TAG, "P2PTunnelAgent_Connect(.) Error Message=" + pnErrFromDeviceCB[0]);
            if (nStart >= 0) {
                int ret = m_commApis.P2PTunnel_SetBufSize(nStart, mBufferSize);
                //Log.d(TAG,"P2PTunnel_SetBufSize SID[" + nStart + "], result=>" + ret);

                for (P2PProtocal protocal : mProtocal) {
                    int retry = protocal.getMaxPortRetry();
                    int localPort = protocal.getLocalPort();
                    int remotePort = protocal.getRemotePort();
                    for (int j = 0; j < retry; j++) {
                        if (j == retry - 1) {
                            stopP2PConnect();
                            return P2PTunnelAPIs.TUNNEL_ER_INITIALIZED;
                        }

                        int port = localPort + j;
                        int mapIndex = m_commApis.P2PTunnelAgent_PortMapping(nStart, port, remotePort);
                        Log.d(TAG, "P2PTunnelAgent_PortMapping(" + port + "," + remotePort + ")=" + mapIndex);
                        if (mapIndex >= 0) {
                            protocal.setMapIndex(mapIndex);
                            protocal.setLocalPort(port);
                            break;
                        }
                    }
                    //Log.d(TAG,"vist:"+ mLocalHost + ":"+protocal.getLocalPort());
                }
            }
        } else {
            Log.d(TAG, "P2PTunnel Already Connect");
        }

        return nStart;
    }

    public String getTUTKUUID() {
        return mUUID;
    }

    public boolean isConnected() {
        return nStart >= 0;
    }

    public int getP2PPort(P2PProtocalType type) {
        int port = -1;
        for (P2PProtocal protocal : mProtocal) {
            if (protocal.getType() == type) {
                port = protocal.getLocalPort();
                break;
            }
        }
        return port;
    }

    public String getP2PIP() {
        return mLocalHost;
    }

    public String getIP(String hostname, P2PProtocalType type) {
        if (LanCheckManager.getInstance().getLanConnect()) {
            if(type == P2PProtocalType.TWONKY)
                hostname = LanCheckManager.getInstance().getLanIP() + ":9000";
            else
                hostname = LanCheckManager.getInstance().getLanIP();
        } else {
            hostname = mLocalHost + ":" + getP2PPort(type);
        }
        return hostname;
    }

    public void stopP2PConnect() {
        if (m_commApis != null) {
            for (P2PProtocal protocal : mProtocal) {
                int mapIndex = protocal.getMapIndex();
                if (mapIndex >= 0) {
                    Log.d(TAG, "P2PTunnelAgent_StopPortMapping=" + mapIndex);
                    m_commApis.P2PTunnelAgent_StopPortMapping(mapIndex);
                    protocal.setMapIndex(-1);
                    protocal.resetLocalPort();
                }
            }
            while (nStart >= 0) {
                m_commApis.P2PTunnelAgent_Disconnect(nStart);
                nStart--;
            }
            m_commApis.P2PTunnelAgentDeInitialize();
            m_commApis = null;

            nStart = -1;
            m_nInit = -1;

            Log.d(TAG, "P2PTunnel Close Connect");
        }
    }

    public void addP2PListener(IP2PTunnelCallback obj) {
        if (mListener.contains(obj)) {
            return;
        }
        mListener.add(obj);
    }

    public void removeP2PListener(IP2PTunnelCallback obj) {
        mListener.remove(obj);
    }

    public void onDestroy() {
        mListener.clear();
        mListener = null;
        mProtocal.clear();
        mProtocal = null;
        mP2PService = null;
    }

    @Override
    public void onTunnelStatusChanged(int nErrCode, int nSID) {
        List<IP2PTunnelCallback> copyListener = new ArrayList<IP2PTunnelCallback>();
        copyListener.addAll(mListener);
        for (IP2PTunnelCallback listener : copyListener) {
            if (listener != null)
                listener.onTunnelStatusChanged(nErrCode, nSID);
        }

        Log.d(TAG, "ErrorCode:" + nErrCode + ", SID:" + nSID);
        Log.d(TAG, "TunnelStatusCB: SID[ " + nSID + "] ErrorCode[" + nErrCode + "]");
    }

    @Override
    public void onTunnelSessionInfoChanged(sP2PTunnelSessionInfo object) {
        List<IP2PTunnelCallback> copyListener = new ArrayList<IP2PTunnelCallback>();
        copyListener.addAll(mListener);
        for (IP2PTunnelCallback listener : copyListener) {
            if (listener != null)
                listener.onTunnelSessionInfoChanged(object);
        }

        Log.d(TAG, "sessionInfo: SID[" + object.getSID() + "] IP[" + object.getRemoteIP() + "] NAT[" + object.getNatType() + "]");
        Log.d(TAG, "TunnelSessionInfoCB: SID[" + object.getSID() + "] IP[" + object.getRemoteIP() + "] NAT[" + object.getNatType() + "]");
        int ret = m_commApis.P2PTunnel_SetBufSize(object.getSID(), mBufferSize);
        Log.d(TAG, "P2PTunnel_SetBufSize SID[" + nStart + "], result=>" + ret);
        Log.d(TAG, "P2PTunnel_SetBufSize SID[" + object.getSID() + "],result=>" + ret);
    }

    public enum P2PProtocalType {
        HTTP, SMB, TWONKY
    }

    private class P2PProtocal {
        private int maxPortRetry = 1000;
        private int defaultLocalPort = -1;
        private int localPort = -1;
        private int remotePort = -1;
        private int mapIndex = -1;
        private P2PProtocalType type;

        public P2PProtocal(P2PProtocalType type, int localPort, int remotePort) {
            this(type, localPort, remotePort, 1000);
        }

        public P2PProtocal(P2PProtocalType type, int localPort, int remotePort, int maxPortRetry) {
            this.type = type;
            this.defaultLocalPort = localPort;
            this.localPort = localPort;
            this.remotePort = remotePort;
            this.maxPortRetry = maxPortRetry;
        }

        public void setLocalPort(int port) {
            if (port <= 0) {
                return;
            }
            localPort = port;
        }

        public int getLocalPort() {
            return localPort;
        }

        public void setRemotePort(int port) {
            if (port <= 0) {
                return;
            }
            remotePort = port;
        }

        public int getRemotePort() {
            return remotePort;
        }

        public void setMaxPortRetry(int retry) {
            maxPortRetry = retry > 0 ? retry : 0;
        }

        public int getMaxPortRetry() {
            return maxPortRetry;
        }

        public void setMapIndex(int index) {
            mapIndex = index;
        }

        public int getMapIndex() {
            return mapIndex;
        }

        public void resetLocalPort() {
            localPort = defaultLocalPort;
        }

        public P2PProtocalType getType() {
            return type;
        }
    }
}  
