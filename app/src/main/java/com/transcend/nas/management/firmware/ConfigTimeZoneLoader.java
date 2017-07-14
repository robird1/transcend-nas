package com.transcend.nas.management.firmware;

import android.content.Context;
import android.util.Log;

import java.util.TimeZone;

/**
 * Created by steve_su on 2017/7/14.
 */

public class ConfigTimeZoneLoader extends GeneralPostLoader {
    private static final String TAG = ConfigTimeZoneLoader.class.getSimpleName();

    public ConfigTimeZoneLoader(Context context) {
        super(context);
    }

    @Override
    protected String onRequestBody() {
        String timezone = TimeZone.getDefault().getID();
        Log.d(TAG, "timezone: "+ timezone);
        return "hash="+ getHash()+ "&zone="+ timezone;
    }

    @Override
    protected String onRequestUrl() {
        return "http://"+ getHost()+"/nas/set/zone";
    }

    @Override
    protected String onTagName() {
        return "date";
    }

}
