package com.transcend.nas.tutk;

import android.content.Context;

import com.tutk.IOTC.P2PService;


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
