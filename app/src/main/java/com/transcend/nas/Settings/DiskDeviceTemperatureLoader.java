package com.transcend.nas.settings;

import android.content.AsyncTaskLoader;
import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ikelee on 16/7/22.
 */
public class DiskDeviceTemperatureLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = DiskDeviceTemperatureLoader.class.getSimpleName();
    private Map<String, String> mTemperature;

    public DiskDeviceTemperatureLoader(Context context) {
        super(context);
        mTemperature = new HashMap<String,String>();
    }

    @Override
    public Boolean loadInBackground() {
        getDevicesTemperature();
        return true;
    }

    private boolean getDevicesTemperature() {
        mTemperature = DiskFactory.getInstance().createDiskDevicesTemperature();
        return (mTemperature != null && mTemperature.size() > 0);
    }

    public Map<String,String> getDevices(){
        return mTemperature;
    }
}
