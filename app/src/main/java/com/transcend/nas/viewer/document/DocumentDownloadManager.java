package com.transcend.nas.viewer.document;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.transcend.nas.NASUtils;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.firmware.MediaFactory;

import java.io.File;

/**
 * Created by steve_su on 2016/11/1.
 */

public class DocumentDownloadManager {

    private static final String TAG = DocumentDownloadManager.class.getSimpleName();
    private DownloadManager mDownloadManager;
    private long mDownloadId;
    private OnFinishLisener mDownloadListener;

    public interface OnFinishLisener {
        void onComplete(String destPath);
    }

    public void initialize(Context context) {
        mDownloadManager = (DownloadManager) context.getSystemService(context.DOWNLOAD_SERVICE);

        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "[Enter] onReceive()");

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(mDownloadId);
                Cursor c = mDownloadManager.query(query);
                if (c.moveToFirst()) {
                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {

                        String localUri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                        mDownloadListener.onComplete(Uri.parse(localUri).getPath());

                        NASUtils.showAppChooser(context, Uri.parse(localUri));

                        clearDownloadTaskID();
                    }
                }
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }

    public void setOnFinishLisener(OnFinishLisener listener) {
        mDownloadListener = listener;
    }

    public void downloadRemoteFile(Context context, FileInfo fileInfo) {
        Uri downloadUri = MediaFactory.createUri(fileInfo.path);
        Request request = new Request(downloadUri);
        File dirFile = new File(NASUtils.getCacheFilesLocation(context));

        setRequestDestinationUri(request, dirFile, downloadUri);
//                request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);

        start(request);
    }

    public long cancel() {
        Log.d(TAG, "[Enter] cancel()");
        long cancelId = 0L;

        if (mDownloadId != 0L) {
            cancelId = mDownloadManager.remove(mDownloadId);
        }
        Log.d(TAG, "cancelId: " + cancelId);

        return cancelId;
    }

    private void setRequestDestinationUri(Request request, File dirFile, Uri downloadUri) {
        Log.d(TAG, "[Enter] setRequestDestinationUri()");

        String filePath = dirFile.toString().concat("/").concat(downloadUri.getLastPathSegment());

        deleteExistFile(filePath);

        Uri fileUri = Uri.fromFile(new File(filePath));
        request.setDestinationUri(fileUri);
    }

    private void deleteExistFile(String filePath) {
        Log.d(TAG, "[Enter] deleteExistFile()");

        File tempFile = new File(filePath);
        if (tempFile.exists()) {
            Log.d(TAG, "tempFile.delete()");
            tempFile.delete();
        }
    }

    private void start(Request request) {
        mDownloadId = mDownloadManager.enqueue(request);
    }

    private void clearDownloadTaskID() {
        mDownloadId = 0L;
    }

}
