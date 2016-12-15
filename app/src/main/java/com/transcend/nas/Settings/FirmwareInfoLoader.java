package com.transcend.nas.settings;

import android.content.AsyncTaskLoader;
import android.content.Context;
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
        mInfo = server.getServerInfo();
        if(null == mInfo) {
            String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
            boolean success = server.doGetServerInfo(hostname);
            if(success)
                mInfo = server.getServerInfo();
        }
        return true;
    }

    public ServerInfo getServerInfo(){
        return mInfo;
    }
}
