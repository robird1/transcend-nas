package com.transcend.nas.management.firmware;

import android.content.Context;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.tutk.IOTC.P2PService;

/**
 * Created by ike_lee on 2017/05/11.
 */
public class FirmwareHelper {
    private static final String TAG = FirmwareHelper.class.getSimpleName();
    private LoginListener mListener;
    private String mResult;

    public interface LoginListener {
        void onLoginFinish(boolean success, String result);
    }

    public FirmwareHelper() {
        this(null);
    }

    public FirmwareHelper(LoginListener listener) {
        mListener = listener;
    }

    public boolean doLogin(Context context, Server server) {
        if (server == null)
            return false;

        Log.d(TAG, "login start");
        String newHostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        server.setHostname(newHostname);
        boolean success = server.connect(false);
        if (success) {
            ServerManager.INSTANCE.saveServer(server);
            ServerManager.INSTANCE.setCurrentServer(server);
            Long time = System.currentTimeMillis();
            NASPref.setSessionVerifiedTime(context, Long.toString(time));
            ShareFolderManager.getInstance().setHashCreateTime(time);
            Log.d(TAG, "login success");
        } else {
            mResult = server.getLoginError();
            Log.d(TAG, "login fail due to : " + mResult);
        }

        if (mListener != null)
            mListener.onLoginFinish(true, mResult);

        return success;
    }

    public boolean doReLogin(Context context) {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return doLogin(context, server);
    }

    public String getResult() {
        return mResult;
    }
}
