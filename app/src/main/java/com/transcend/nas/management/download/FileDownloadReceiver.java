package com.transcend.nas.management.download;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.FileManageActivity;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.transcend.nas.NASApp.getContext;
import static com.transcend.nas.management.download.FileDownloadManager.mDownloadIdMap;
import static com.transcend.nas.management.download.FileDownloadManager.mTaskIdMap;

/**
 * Created by steve_su on 2017/1/17.
 */

public class FileDownloadReceiver extends BroadcastReceiver {
    private static final String TAG = FileDownloadReceiver.class.getSimpleName();
    private Context mContext;
    private long mDownloadId;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "[Enter] onReceive");
        mContext = context;
        String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            mDownloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(mDownloadId);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor c = manager.query(query);
            try {
                if (c.moveToFirst()) {
                    doAction(c);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null)
                    c.close();
            }
        }
    }

    private void doAction(Cursor c) {
        switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_SUCCESSFUL:
                Log.d(TAG, "[Enter] DownloadManager.STATUS_SUCCESSFUL");
                String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                Log.d(TAG, "uri: " + uri);
                if (uri.contains(NASUtils.getCacheFilesLocation(mContext))) {
                    notifyOpenFileListener(uri);
                } else {
                    checkNotifyService();
                }
                break;
            case DownloadManager.STATUS_FAILED:
                Log.d(TAG, "[Enter] DownloadManager.STATUS_FAILED");
                Log.d(TAG, "reason: " + c.getString(c.getColumnIndex(DownloadManager.COLUMN_REASON)));

                long id = DownloadFactory.getManager(mContext, DownloadFactory.Type.TEMPORARY).getDownloadId();
                if (mDownloadId == id) {
                    notifyOpenFileListener(null);
                }
                break;
            default:
                Log.d(TAG, "[Enter] default block. error code: " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                Log.d(TAG, "reason: " + c.getString(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
                break;
        }
    }

    private void checkNotifyService() {
        int taskId;
        if (mDownloadIdMap.containsKey(mDownloadId)) {
            taskId = mDownloadIdMap.get(mDownloadId);
            mDownloadIdMap.remove(mDownloadId);
        } else {
            return;
        }

        int remainTaskFiles;
        if (mTaskIdMap.containsKey(taskId)) {
            remainTaskFiles = mTaskIdMap.get(taskId) - 1;
        } else {
            return;
        }

        Log.d(TAG, "taskId: " + taskId + " remainTaskFiles: " + remainTaskFiles);
        mTaskIdMap.put(taskId, remainTaskFiles);

        if (remainTaskFiles == 0) {
            invokeNotifyService(mContext.getString(R.string.download), mContext.getString(R.string.done), NASPref.getDownloadLocation(mContext), taskId);
            mTaskIdMap.remove(taskId);
            showMap();
        }
    }

    private void notifyOpenFileListener(String localUri) {
        TempFileDownloadManager manager = (TempFileDownloadManager) DownloadFactory.getManager(mContext, DownloadFactory.Type.TEMPORARY);
        TempFileDownloadManager.OpenFileListener listener = manager.getOpenFileListener();
        if (listener != null) {
            if (localUri != null) {
                listener.onComplete(Uri.parse(localUri));
            } else {
                listener.onFail();
            }
        }

    }

    private void invokeNotifyService(String type, String result, String destination, int taskId) {
        CustomNotificationManager.updateResult(getContext(), taskId, type, result, destination);
    }

    private void showMap() {
        Log.d(TAG, "[Enter] showMap");
        Set taskIdSet = mTaskIdMap.entrySet();
        Iterator iterator = taskIdSet.iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Log.d(TAG, "task id: " + entry.getKey() + " remain files: " + entry.getValue());
        }

        Set downloadIdSet = mDownloadIdMap.entrySet();
        Iterator iterator2 = downloadIdSet.iterator();
        while (iterator2.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator2.next();
            Log.d(TAG, "download id: " + entry.getKey() + " task id: " + entry.getValue());
        }
    }
}
