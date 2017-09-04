package com.transcend.nas.management.firmware;

import android.content.Context;

/**
 * Created by steve_su on 2017/7/31.
 */

public class FirmwareVersionLoader extends GeneralPostLoader {
    public FirmwareVersionLoader(Context context) {
        super(context);
    }

    @Override
    protected String onRequestBody() {
        return "hash=" + getHash();
    }

    @Override
    protected String onRequestUrl() {
        return "http://"+ getHost()+"/nas/firmware/getversion";
    }

    @Override
    protected String onTagName() {
        return "remote_ver";
    }
}
