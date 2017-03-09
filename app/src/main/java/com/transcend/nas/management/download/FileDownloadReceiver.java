package com.transcend.nas.management.download;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.transcend.nas.management.download.FileDownloadManager.mDownloadIdMap;
import static com.transcend.nas.management.download.FileDownloadManager.mTaskIdMap;
import static com.transcend.nas.management.download.FileDownloadManager.mTaskIdTotalMap;

/**
 * Created by steve_su on 2017/1/17.
 */

public class FileDownloadReceiver extends BroadcastReceiver {
    private static final String TAG = FileDownloadReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor c = manager.query(query);
            try {
                if (c.moveToFirst()) {
                    doAction(context, c, downloadId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (c != null)
                    c.close();
            }
        } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
            Intent dm = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            dm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(dm);
        }
    }

    private void doAction(Context context, Cursor c, long downloadId) {
        switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_SUCCESSFUL:
                String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                Log.d(TAG, "[Enter] DownloadManager.STATUS_SUCCESSFUL : " + uri);
                if (uri.contains(NASUtils.getCacheFilesLocation(context))) {
                    notifyOpenFileListener(context, uri);
                } else {
                    checkNotifyService(context, downloadId);
                }
                break;
            case DownloadManager.STATUS_FAILED:
                Log.d(TAG, "[Enter] DownloadManager.STATUS_FAILED : " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
                long id = DownloadFactory.getManager(context, DownloadFactory.Type.TEMPORARY).getDownloadId();
                if (id == downloadId) {
                    notifyOpenFileListener(context, null);
                } else {
                    checkNotifyService(context, downloadId);
                }
                break;
            default:
                Log.d(TAG, "[Enter] default block. error code: " + c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                Log.d(TAG, "reason: " + c.getString(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
                break;
        }
    }

    private void checkNotifyService(Context context, long downloadId) {
        if (!mDownloadIdMap.containsKey(downloadId))
            return;

        int taskId = mDownloadIdMap.get(downloadId);
        mDownloadIdMap.remove(downloadId);

        int currentTaskCount = 1;
        if (mTaskIdMap.containsKey(taskId))
            currentTaskCount = mTaskIdMap.get(taskId) + 1;
        mTaskIdMap.put(taskId, currentTaskCount);

        if (!mTaskIdTotalMap.containsKey(taskId))
            return;

        int totalTaskCount = mTaskIdTotalMap.get(taskId);
        Log.d(TAG, "taskId: " + taskId + ", progress : " + currentTaskCount + "/" + totalTaskCount);

        if (currentTaskCount == totalTaskCount) {
            invokeNotifyService(context, context.getString(R.string.download), context.getString(R.string.done), NASPref.getDownloadLocation(context), taskId);
            Toast.makeText(context, context.getString(R.string.download) + " - " + context.getString(R.string.done), Toast.LENGTH_SHORT).show();
            mTaskIdMap.remove(taskId);
            mTaskIdTotalMap.remove(taskId);
            showMap();
        }
    }

    private void notifyOpenFileListener(Context context, String localUri) {
        TempFileDownloadManager manager = (TempFileDownloadManager) DownloadFactory.getManager(context, DownloadFactory.Type.TEMPORARY);
        TempFileDownloadManager.OpenFileListener listener = manager.getOpenFileListener();
        if (listener != null) {
            if (localUri != null) {
                listener.onComplete(Uri.parse(localUri));
            } else {
                listener.onFail();
            }
        }

    }

    private void invokeNotifyService(Context context, String type, String result, String destination, int taskId) {
        CustomNotificationManager.updateResult(context, taskId, type, result, destination);
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
