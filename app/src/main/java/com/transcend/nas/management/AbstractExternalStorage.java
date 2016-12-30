package com.transcend.nas.management;

import android.content.Context;
import android.content.Intent;
import android.os.storage.StorageManager;
import android.util.Log;

import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.settings.BaseDrawerActivity;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.transcend.nas.NASUtils.getStoragePath;

/**
 * Created by steve_su on 2016/12/26.
 */

public abstract class AbstractExternalStorage {
    private static final String TAG = AbstractExternalStorage.class.getSimpleName();
    private Context mContext;
//    private BaseDrawerActivity mActivity;

    public AbstractExternalStorage(Context context) {
        mContext = context;
    }


    protected String getSDLocation() {
        List<File> stgList = NASUtils.getStoragePath(mContext);
        if (stgList.size() > 1) {
            for (File sd : stgList) {
                if ((!sd.getAbsolutePath().contains(NASApp.ROOT_STG)) && (!sd.getAbsolutePath().toLowerCase().contains("usb"))) {
                    Log.d(TAG, "sd.getAbsolutePath(): "+ sd.getAbsolutePath());
                    return sd.getAbsolutePath();
                }
            }
        }

        return null;
    }

    protected Context getContext() {
        return mContext;
    }
//    protected BaseDrawerActivity getActivity() {
//        return mActivity;
//    }

    protected void onNavigationItemSelected(BaseDrawerActivity activity, int itemId) {

    }

    protected void onActivityResult(FileManageActivity activity, Intent data) {

    }
//    protected List<File> getStoragePath(Context mContext) {
//        List<File> stgList = new ArrayList<File>();
//        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
//        Class<?> storageVolumeClazz = null;
//        try {
//            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
//            Method getVolumeList = null;
//            Method getPath = null;
//            Method isRemovable = null;
//            try {
//                getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
//                getPath = storageVolumeClazz.getMethod("getPath");
//                isRemovable = storageVolumeClazz.getMethod("isRemovable");
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            }
//
//            Method getSubSystem = null;
//            try {
//                getSubSystem = storageVolumeClazz.getMethod("getSubSystem");
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            }
//            Object result = getVolumeList.invoke(mStorageManager);
//            final int length = Array.getLength(result);
//            for (int i = 0; i < length; i++) {
//                Object storageVolumeElement = Array.get(result, i);
//                String path = (String) getPath.invoke(storageVolumeElement);
//                String subSystem = "";
//                if (getSubSystem != null) {
//                    subSystem = (String) getSubSystem.invoke(storageVolumeElement);
//                }
//                if (!subSystem.contains("usb")) {
//                    if (!path.toLowerCase().contains("private")) {
//                        stgList.add(new File(path));
//                    }
//
//                }
//
//            }
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        }
//        return stgList;
//    }

}
