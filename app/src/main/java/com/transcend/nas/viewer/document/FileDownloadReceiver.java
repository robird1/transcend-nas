package com.transcend.nas.viewer.document;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.transcend.nas.NASUtils;

/**
 * Created by steve_su on 2017/1/17.
 */

public class FileDownloadReceiver extends BroadcastReceiver {
    private static final String TAG = FileDownloadReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, " ");
        Log.d(TAG, "[Enter] onReceive");
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            Log.d(TAG, "downloadId: "+ downloadId);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor c = FileDownloadManager.getInstance(context).getManager().query(query);
            if (c.moveToFirst()) {
                doAction(context, c);
            }
        }
    }

    private void doAction(Context context, Cursor c) {
        switch (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_SUCCESSFUL:
                Log.d(TAG, "[Enter] DownloadManager.STATUS_SUCCESSFUL");
                String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                Log.d(TAG, "uri: "+ uri);
                if (uri.contains(NASUtils.getCacheFilesLocation(context))) {
                    notify(context, uri);
                }
                FileDownloadManager.getInstance(context).clearDownloadTaskID();
                break;
            case DownloadManager.STATUS_FAILED:
                Log.d(TAG, "[Enter] DownloadManager.STATUS_FAILED");
                Log.d(TAG, "reason: "+ c.getString(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
                break;
            default:
                Log.d(TAG, "[Enter] default block. error code: "+ c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS)));
                Log.d(TAG, "reason: "+ c.getString(c.getColumnIndex(DownloadManager.COLUMN_REASON)));
                break;
        }
    }

    private void notify(Context context, String localUri) {
        FileDownloadManager.OnFinishListener listener = FileDownloadManager.getInstance(context).getOnFinishListener();
        if (listener != null) {
            listener.onComplete(Uri.parse(localUri));
        }
    }
}
