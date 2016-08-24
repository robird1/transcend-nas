package com.transcend.nas.common;

import android.app.ActivityManager;
import android.content.Context;

import com.transcend.nas.NASApp;

import java.io.File;

/**
 * Created by ike_lee on 2016/8/18.
 */
public class ManageFactory {
    private static final String TAG = ManageFactory.class.getSimpleName();
    private static ManageFactory mManageFactory;
    private static final Object mMute = new Object();

    public ManageFactory() {
    }

    public static ManageFactory getInstance() {
        synchronized (mMute) {
            if (mManageFactory == null)
                mManageFactory = new ManageFactory();
        }
        return mManageFactory;
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTopDirectory(String mode, String root, String path) {
        if (NASApp.MODE_SMB.equals(mode)) {
            return path.equals(root);
        } else {
            File base = new File(root);
            File file = new File(path);
            return file.equals(base);
        }
    }
}
