package com.transcend.nas.management.firmware;

import android.content.Context;

/**
 * Created by steve_su on 2017/7/12.
 */

public class NTPServerLoader extends GeneralPostLoader {

    public NTPServerLoader(Context context) {
        super(context);
    }

    @Override
    protected String onRequestBody() {
        return "hash="+ getHash();
    }

    @Override
    protected String onRequestUrl() {
        return "http://"+ getHost()+"/nas/get/ntp";
    }

    @Override
    protected String onTagName() {
        return "myntp";
    }

    @Override
    protected boolean onRequestFinish() {
        return false;
    }

}
