package com.transcend.nas.management.firmware;

import android.content.Context;

/**
 * Created by steve_su on 2017/7/13.
 */

public class TimeZoneLoader extends GeneralPostLoader {

    public TimeZoneLoader(Context context) {
        super(context);
    }

    @Override
    protected String onRequestBody() {
        return "hash=" + getHash();
    }

    @Override
    protected String onRequestUrl() {
        return "http://" + getHost() + "/nas/get/zone";
    }

    @Override
    protected String onTagName() {
        return "zone";
    }

    @Override
    protected boolean onRequestFinish() {
        return false;
    }

}
