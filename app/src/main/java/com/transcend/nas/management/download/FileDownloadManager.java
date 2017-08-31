package com.transcend.nas.management.download;

import android.content.Context;
import android.os.Bundle;

import com.transcend.nas.NASApp;
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

public class FileDownloadManager extends AbstractDownloadManager {
    static ConcurrentHashMap<Long, Integer> mDownloadIdMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, Integer> mTaskIdMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, Integer> mTaskIdTotalMap = new ConcurrentHashMap<>();

    FileDownloadManager(Context context) {
        super(context);
    }

    @Override
    protected String onFileName(Bundle data) {
        return data.getString(KEY_FILE_NAME);
    }

    @Override
    public long cancel(Context context) {
        Set taskSet = mTaskIdMap.entrySet();
        Iterator iterator1 = taskSet.iterator();
        while (iterator1.hasNext()) {
            Map.Entry entry1 = (Map.Entry) iterator1.next();
            int taskId = Integer.parseInt(entry1.getKey().toString());
            if(cancelByNotificationId(taskId))
                invokeNotifyService(context, context.getString(R.string.download), context.getString(R.string.error), NASPref.getDownloadLocation(context), taskId);
        }
        return -1L;
    }

    public boolean cancelByNotificationId(int taskId){
        boolean remove = false;
        Set downloadSet = mDownloadIdMap.entrySet();
        Iterator iterator = downloadSet.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            int value = Integer.parseInt(entry.getValue().toString());
            if (taskId == value) {
                long downloadId = Long.parseLong(entry.getKey().toString());
                mDownloadIdMap.remove(downloadId);
                mDownloadManager.remove(downloadId);
                remove = true;
            }
        }

        mTaskIdMap.remove(taskId);
        mTaskIdTotalMap.remove(taskId);
        return remove;
    }

    public void setTotalTaskByNotificationId(int taskId, int total){
        mTaskIdTotalMap.put(taskId, total);
    }

    @Override
    protected void onPostProcess() {
        mDownloadIdMap.put(getDownloadId(), getDownloadData().getInt(KEY_TASK_ID));
    }

    private void invokeNotifyService(Context context, String type, String result, String destination, int taskId) {
        CustomNotificationManager.updateResult(context, taskId, type, result, destination);
    }

}
