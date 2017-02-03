package com.transcend.nas.viewer.document;

import android.content.Context;

import java.util.HashMap;

/**
 * Created by steve_su on 2017/1/25.
 */

public class DownloadFactory {
    private static HashMap<Type, AbstractDownloadManager> mManagerMap = new HashMap<>();

    public enum Type {
        TEMPORARY, PERSIST
    }

    public static AbstractDownloadManager getManager(Context context, Type type) {
        if (mManagerMap.get(type) == null) {
            AbstractDownloadManager instance;
            if (type == Type.PERSIST) {
                instance = new FileDownloadManager(context);
            } else {
                instance = new TempFileDownloadManager(context);
            }
            mManagerMap.put(type, instance);
            return instance;
        } else {
            return mManagerMap.get(type);
        }
    }

}
