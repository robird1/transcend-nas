package com.transcend.nas.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ike_lee on 2016/5/23.
 */
public class CustomNotificationManager {

    private static final String TAG = CustomNotificationManager.class.getSimpleName();
    private static CustomNotificationManager mCustomNotificationManager;
    private static final Object mMute = new Object();
    private List<String> mNotificationList;

    public CustomNotificationManager() {
        mNotificationList = new ArrayList<String>();
    }

    public static CustomNotificationManager getInstance() {
        synchronized (mMute) {
            if (mCustomNotificationManager == null)
                mCustomNotificationManager = new CustomNotificationManager();
        }
        return mCustomNotificationManager;
    }

    public int queryNotificationID() {
        int id = 1;
        if (mNotificationList.size() > 0) {
            String value = mNotificationList.get(mNotificationList.size() - 1);
            id = Integer.parseInt(value) + 1;
            mNotificationList.add(Integer.toString(id));
        } else {
            mNotificationList.add(Integer.toString(id));
        }
        return id;
    }

    public void releaseNotificationID(int id) {
        String value = "" + id;
        mNotificationList.remove(value);
    }
}
