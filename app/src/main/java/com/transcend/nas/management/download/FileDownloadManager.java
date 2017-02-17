package com.transcend.nas.management.download;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.FileManageActivity;

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
        int icon = R.mipmap.ic_launcher;
        String name = getContext().getResources().getString(R.string.app_name);
        String text = String.format("%s - %s", type, result);

        NotificationManager ntfMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent();
        intent.setClass(getContext(), FileManageActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(destination != null && !destination.equals(""))
            intent.putExtra("path", destination);

        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext());
        builder.setSmallIcon(icon);
        builder.setContentTitle(name);
        builder.setContentText(text);
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(true);
        ntfMgr.notify(taskId, builder.build());
        CustomNotificationManager.getInstance().releaseNotificationID(taskId);
    }

}
