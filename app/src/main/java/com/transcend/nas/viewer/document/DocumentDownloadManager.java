package com.transcend.nas.viewer.document;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
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
    private static DocumentDownloadManager mInstance;
    private DownloadManager mDownloadManager;
    private long mDownloadId;
    private OnFinishListener mOnFinishListener;

    public interface OnFinishListener {
        void onComplete(Uri destUri);
    }

    private DocumentDownloadManager(Context context) {
        mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public static synchronized DocumentDownloadManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DocumentDownloadManager(context);
        }
        return mInstance;
    }

    public void setOnFinishListener(OnFinishListener listener) {
        mOnFinishListener = listener;
    }

    public void start(Context context, FileInfo fileInfo) {
        Uri downloadUri = MediaFactory.createUri(context, fileInfo.path);
        Request request = new Request(downloadUri);
        setRequestDestinationUri(request, new File(NASUtils.getCacheFilesLocation(context)), downloadUri);
//                request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
        enqueue(request);
    }

    public long cancel() {
        long cancelId = 0L;
        if (mDownloadId != 0L) {
            cancelId = mDownloadManager.remove(mDownloadId);
        }
        return cancelId;
    }

    DownloadManager getManager() {
        return mDownloadManager;
    }

    long getDownloadId() {
        return mDownloadId;
    }

    OnFinishListener getOnFinishListener() {
        return mOnFinishListener;
    }

    private void setRequestDestinationUri(Request request, File dirFile, Uri downloadUri) {
        String filePath = dirFile.toString().concat("/").concat(downloadUri.getLastPathSegment());
        deleteExistFile(filePath);
        request.setDestinationUri(Uri.fromFile(new File(filePath)));
    }

    private void deleteExistFile(String filePath) {
        File tempFile = new File(filePath);
        if (tempFile.exists()) {
            Log.d(TAG, "tempFile.delete()");
            tempFile.delete();
        }
    }

    private void enqueue(Request request) {
        mDownloadId = mDownloadManager.enqueue(request);
    }

    void clearDownloadTaskID() {
        mDownloadId = 0L;
    }

}
