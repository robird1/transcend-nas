package com.transcend.nas.management.download;

import android.content.Context;
import android.os.Bundle;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by steve_su on 2017/1/25.
 */

class FileDownloadManager extends AbstractDownloadManager {
    static ConcurrentHashMap<Long, Integer> mDownloadIdMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, Integer> mTaskIdMap = new ConcurrentHashMap<>();

    FileDownloadManager(Context context) {
        super(context);
    }

    @Override
    protected String onFileName(Bundle data) {
        return data.getString(KEY_FILE_NAME);
    }

    @Override
    public long cancel() {
        boolean remove = false;
        Set taskSet = mTaskIdMap.entrySet();
        Iterator iterator1 = taskSet.iterator();
        while (iterator1.hasNext()) {
            Map.Entry entry1 = (Map.Entry) iterator1.next();
            int taskId = Integer.parseInt(entry1.getKey().toString());

            Set downloadSet = mDownloadIdMap.entrySet();
            Iterator iterator2 = downloadSet.iterator();
            while (iterator2.hasNext()) {
                Map.Entry entry2 = (Map.Entry) iterator2.next();
                int value = Integer.parseInt(entry2.getValue().toString());
                if (taskId == value) {
                    long downloadId = Long.parseLong(entry2.getKey().toString());
                    mDownloadIdMap.remove(downloadId);
                    mDownloadManager.remove(downloadId);
                    remove = true;
                }
            }

            mTaskIdMap.remove(taskId);
            if(remove)
                invokeNotifyService(mContext.getString(R.string.download), mContext.getString(R.string.error), NASPref.getDownloadLocation(mContext), taskId);
        }
        return -1L;
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

    private void invokeNotifyService(String type, String result, String destination, int taskId) {
        CustomNotificationManager.updateResult(getContext(), taskId, type, result, destination);
    }

}
