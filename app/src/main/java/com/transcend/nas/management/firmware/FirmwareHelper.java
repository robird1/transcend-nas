package com.transcend.nas.management.firmware;

import android.content.Context;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.connection.LoginHelper;
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

    public boolean doReLogin(Context context) {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        server.setHostname(hostname);
        return doLogin(context, server, false);
    }

    public boolean doLogin(Context context, Server server, boolean first) {
        if (server == null)
            return false;

        Log.d(TAG, "login start");
        boolean success = server.connect(first);
        if (success) {
            Log.d(TAG, "login success");
            updateServerManager(context, server);
            if(first)
                updateLoginPreference(context, server);

        } else {
            mResult = server.getLoginError();
            Log.d(TAG, "login fail due to : " + mResult);
        }

        if (mListener != null)
            mListener.onLoginFinish(true, mResult);

        return success;
    }

    private void updateServerManager(Context context, Server server) {
        ServerManager.INSTANCE.saveServer(server);
        ServerManager.INSTANCE.setCurrentServer(server);
        Long time = System.currentTimeMillis();
        NASPref.setSessionVerifiedTime(context, Long.toString(time));
        ShareFolderManager.getInstance().setHashCreateTime(time);
    }

    private void updateLoginPreference(Context context, Server server) {
        NASPref.setHostname(context, server.getHostname());
        NASPref.setUsername(context, server.getUsername());
        NASPref.setPassword(context, server.getPassword());
        NASPref.setUUID(context, server.getTutkUUID());
        NASPref.setCloudUUID(context, server.getTutkUUID());
        NASPref.setMacAddress(context, server.getServerInfo().mac);
        NASPref.setDeviceName(context, server.getServerInfo().hostName);
        NASPref.setLocalHostname(context, server.getServerInfo().ipAddress);

        ShareFolderManager.getInstance().cleanRealPathMap();

        LoginHelper loginHelper = new LoginHelper(context);
        LoginHelper.LoginInfo account = new LoginHelper.LoginInfo();
        account.email = NASPref.getCloudUsername(context);
        account.hostname = server.getHostname();
        account.username = server.getUsername();
        account.password = server.getPassword();
        account.uuid = server.getTutkUUID();
        ServerInfo info = server.getServerInfo();
        account.macAddress = info.mac;
        account.ip = info.ipAddress;
        loginHelper.setAccount(account);
    }

    public String getResult() {
        return mResult;
    }
}
