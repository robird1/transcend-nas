package com.transcend.nas.tutk;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.tutk.IOTC.P2PTunnelAPIs;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by silverhsu on 16/1/5.
 */
public class P2PStautsLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = P2PStautsLoader.class.getSimpleName();
    private Context mContext;
    private ArrayList<HashMap<String, String>> mNasList;
    private ArrayList<String> mResults;

    public P2PStautsLoader(Context context, ArrayList<HashMap<String, String>> nasList) {
        super(context);
        mNasList = nasList;
        mResults = new ArrayList<>();
    }

    @Override
    public Boolean loadInBackground() {
        if(mNasList == null || mNasList.size() == 0)
            return false;

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

}
