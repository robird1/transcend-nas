package com.transcend.nas.management.firmware;

import android.content.Context;

/**
 * Created by steve_su on 2017/7/12.
 */

public class ConfigNTPServerLoader extends GeneralPostLoader {

    public ConfigNTPServerLoader(Context context) {
        super(context);
    }

    @Override
    protected String onRequestBody() {
        return "hash="+ getHash()+ "&date=now&sync=daily&manual=yes&myntp=time.google.com";
    }

    @Override
    protected String onRequestUrl() {
        return "http://"+ getHost()+"/nas/set/date";
    }

    @Override
    protected String onTagName() {
        return "date";
    }

    @Override
    protected boolean onRequestFinish() {
        FirmwareHelper helper = new FirmwareHelper();
        helper.doReLogin(getContext());
        return false;
    }

}
