package com.transcend.nas.management;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.transcend.nas.R;
import com.tutk.IOTC.P2PService;
import com.tutk.IOTC.P2PTunnelAPIs;


/**
 * Created by ikelee on 06/4/16.
 */
public class TutkLogoutLoader extends TutkBasicLoader {

    public TutkLogoutLoader(Context context) {
        super(context);
    }

    @Override
    public Boolean loadInBackground() {
        P2PService.getInstance().stopP2PConnect();
        return true;
    }

    @Override
    protected boolean doParserResult(String result) {
        return true;
    }

    @Override
    protected String doGenerateUrl() {
        return null;
    }
}
