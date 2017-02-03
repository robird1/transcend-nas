package com.transcend.nas.management.download;

import android.content.Context;
import android.os.Bundle;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by steve_su on 2017/1/25.
 */

class FileDownloadManager extends AbstractDownloadManager {
    static ConcurrentHashMap<Long, Integer> mDownloadIdMap = new ConcurrentHashMap <>();
    static ConcurrentHashMap<Integer, Integer> mTaskIdMap = new ConcurrentHashMap <>();

    FileDownloadManager(Context context) {
        super(context);
    }

    @Override
    protected String onFileName(Bundle data) {
        return data.getString(KEY_FILE_NAME);
    }

    @Override
    protected void onPreProcess() {
        int taskId = getDownloadData().getInt(KEY_TASK_ID);
        int filesCount;
        if (mTaskIdMap.containsKey(taskId)) {
            filesCount = mTaskIdMap.get(taskId) + 1;
        } else {
            filesCount = 1;
        }
        mTaskIdMap.put(taskId, filesCount);
    }

    @Override
    protected void onPostProcess() {
        mDownloadIdMap.put(getDownloadId(), getDownloadData().getInt(KEY_TASK_ID));
    }

}
