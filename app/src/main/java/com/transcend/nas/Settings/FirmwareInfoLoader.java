package com.transcend.nas.settings;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.tutk.IOTC.P2PService;

/**
 * Created by ikelee on 16/12/09.
 */
public class FirmwareInfoLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = FirmwareInfoLoader.class.getSimpleName();
    private ServerInfo mInfo = null;

    public FirmwareInfoLoader(Context context) {
        super(context);
    }

    @Override
    public Boolean loadInBackground() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        boolean success = server.doGetServerInfo(hostname);
        if(success) {
            Log.d(TAG, "[Enter] success");
            mInfo = server.getServerInfo();
            Log.d(TAG, "mInfo.hostName: "+ mInfo.hostName);
        }

        return true;
    }

    public ServerInfo getServerInfo(){
        return mInfo;
    }
}
