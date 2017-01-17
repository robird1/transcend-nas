package com.transcend.nas.viewer.document;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by steve_su on 2017/1/17.
 */

public class DownloadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
            DocumentDownloadManager instance = DocumentDownloadManager.getInstance(context);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(instance.getDownloadId());
            Cursor c = instance.getManager().query(query);
            if (c.moveToFirst()) {
                if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                    String localUri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    instance.getOnFinishListener().onComplete(Uri.parse(localUri));
                    instance.clearDownloadTaskID();
                }
            }
        }

    }
}
