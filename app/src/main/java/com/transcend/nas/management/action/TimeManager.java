package com.transcend.nas.management.action;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.transcend.nas.LoaderID;
import com.transcend.nas.NASUtils;
import com.transcend.nas.management.firmware.ConfigNTPServerLoader;
import com.transcend.nas.management.firmware.ConfigTimeZoneLoader;
import com.transcend.nas.management.firmware.NTPServerLoader;
import com.transcend.nas.management.firmware.TimeZoneLoader;
import com.transcend.nas.settings.FirmwareVersionLoader;

/**
 * Created by ike_lee on 2017/8/8.
 */
public class TimeManager extends AbstractActionManager {
    private static String TAG = TimeManager.class.getSimpleName();
    public TimeManager(Context context, LoaderManager.LoaderCallbacks callbacks) {
        this(context, callbacks, null);
    }

    public TimeManager(Context context, LoaderManager.LoaderCallbacks callbacks, RelativeLayout progressLayout) {
        super(context, callbacks, progressLayout);
    }

    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LoaderID.FIRMWARE_VERSION:
                showProgress();
                return new FirmwareVersionLoader(getContext());
            case LoaderID.NTP_SERVER:
                showProgress();
                return new NTPServerLoader(getContext());
            case LoaderID.NTP_SERVER_CONFIG:
                showProgress();
                return new ConfigNTPServerLoader(getContext());
            case LoaderID.TIME_ZONE:
                showProgress();
                return new TimeZoneLoader(getContext());
            case LoaderID.TIME_ZONE_CONFIG:
                showProgress();
                return new ConfigTimeZoneLoader(getContext());
            default:
                return null;
        }
    }

    public boolean onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (success) {
            if (loader instanceof FirmwareVersionLoader) {
                String isUpgrade = ((FirmwareVersionLoader) loader).getIsUpgrade();
                if ("yes".equals(isUpgrade)) {
                    NASUtils.showFirmwareNotify(((Activity) getContext()));
                }
                return true;
            } else if (loader instanceof TimeZoneLoader) {
                String timeZone = ((TimeZoneLoader) loader).getValue();
                if (!isTimeZoneValid(timeZone)) {
                    configTimeZone();
                } else {
                    checkNTPServer();
                }
                return true;

            } else if (loader instanceof ConfigTimeZoneLoader) {
                String result = ((ConfigTimeZoneLoader) loader).getValue();
                if ("update time zone".equals(result)) {
                    checkNTPServer();
                    return true;
                }
            } else if (loader instanceof NTPServerLoader) {
                String server = ((NTPServerLoader) loader).getValue();
                if ("time.windows.com".equals(server)) {
                    configNTPServer();
                    return true;
                }
            } else if (loader instanceof ConfigNTPServerLoader) {
                String result = ((ConfigNTPServerLoader) loader).getValue();
                NASUtils.reLogin(getContext());
                hideProgress();
                return true;
            }
        } else {
            // force the execution of checking NTP server
            if (loader instanceof TimeZoneLoader) {
                checkNTPServer();
            }
        }

        return false;
    }

    public void onLoaderReset(Loader<Boolean> loader) {
    }

    private boolean isTimeZoneValid(String timeZone) {
        return timeZone.length() > 2;
    }

    public void checkFirmwareVersion() {
        createLoader(LoaderID.FIRMWARE_VERSION, null);
    }

    public void checkTimeZone() {
        createLoader(LoaderID.TIME_ZONE, null);
    }

    private void configTimeZone() {
        createLoader(LoaderID.TIME_ZONE_CONFIG, null);
    }

    private void checkNTPServer() {
        createLoader(LoaderID.NTP_SERVER, null);
    }

    private void configNTPServer() {
        createLoader(LoaderID.NTP_SERVER_CONFIG, null);
    }

}
