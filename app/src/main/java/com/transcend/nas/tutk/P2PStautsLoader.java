package com.transcend.nas.tutk;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.tutk.IOTC.P2PTunnelAPIs;
import com.tutk.IOTC.IOTCAPIs;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by silverhsu on 16/1/5.
 * NOTICE: if you want to use this loader to check device status (on or off),
 * you need to init IOTC Module in onCreate() and deInit IOTC Module in onDestroy() in the activity
 */
public class P2PStautsLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = P2PStautsLoader.class.getSimpleName();
    private ArrayList<HashMap<String, String>> mNasList;
    private boolean useIOTCModule = true;
    private int mTimeOut = 5000;

    public P2PStautsLoader(Context context, ArrayList<HashMap<String, String>> nasList) {
        this(context, nasList, 5000);
    }

    public P2PStautsLoader(Context context, ArrayList<HashMap<String, String>> nasList, int timeout) {
        super(context);
        mTimeOut = timeout;
        mNasList = new ArrayList<>();
        for(HashMap<String, String> nas : nasList)
            mNasList.add(nas);
    }

    @Override
    public Boolean loadInBackground() {
        if (mNasList == null || mNasList.size() == 0)
            return false;

        return useIOTCModule ? checkStatusWithIOTCModules() : checkStatusWithP2PModules();
    }

    public boolean checkStatusWithP2PModules() {
        boolean isSuccess = false;
        P2PTunnelAPIs commApis = new P2PTunnelAPIs(null);
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

        for (HashMap<String, String> nas : mNasList) {
            String mUID = nas.get("hostname");
            if (mUID == null) {
                continue;
            }

            int start = commApis.P2PTunnelAgent_Connect(mUID, baAuthData, baAuthData.length, pnErrFromDeviceCB);
            if (start >= 0) {
                isSuccess = true;
                nas.put("online", "yes");
                commApis.P2PTunnelAgent_Disconnect(start);
            } else {
                nas.put("online", "no");
            }
            Log.d(TAG, "P2PTunnelAgent_Connect(.) UID=" + mUID + ", start=" + start);
        }

        commApis.P2PTunnelAgentDeInitialize();
        return isSuccess;
    }

    public Boolean checkStatusWithIOTCModules() {
        onLineResultCBObject object = new onLineResultCBObject();
        for (HashMap<String, String> nas : mNasList) {
            String mUID = nas.get("hostname");
            if (mUID == null) {
                continue;
            }

            byte[] userData = mUID.getBytes();
            int start = IOTCAPIs.IOTC_Check_Device_On_Line(mUID, mTimeOut, userData, object);
            Log.d(TAG, "IOTCAgent_Connect(.) UID=" + mUID + ", start=" + start);
            if(start < 0) {
                return false;
            }
        }

        return true;
    }

    public class onLineResultCBObject extends Object {

        public void onLineResultCB(int result, byte[] userData) {
            String UID = new String(userData);
            Log.d(TAG, "IOTCAgent_Connect(.), result=" + result + ", UID=" + UID);
            for (HashMap<String, String> nas : mNasList) {
                String mUID = nas.get("hostname");
                if (UID.equals(mUID)) {
                    if (result >= 0) {
                        nas.put("online", "yes");
                    } else {
                        nas.put("online", "no");
                    }
                    nas.put("ready", "yes");
                    break;
                }
            }
        }
    }

    public ArrayList<HashMap<String, String>> getNasList(){
        return mNasList;
    }

    public boolean isIOTCReady(){
        for (HashMap<String, String> nas : mNasList) {
            String ready = nas.get("ready");
            if(ready == null || "".equals(ready))
                return false;
        }

        return true;
    }

}
