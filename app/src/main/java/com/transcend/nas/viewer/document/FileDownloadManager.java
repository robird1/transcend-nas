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
import java.util.concurrent.ConcurrentHashMap;

import static android.media.CamcorderProfile.get;

/**
 * Created by steve_su on 2016/11/1.
 */

public class FileDownloadManager {

    private static final String TAG = FileDownloadManager.class.getSimpleName();
    private static FileDownloadManager mInstance;
    private DownloadManager mDownloadManager;
    private long mDownloadId;
    private OpenFileListener mOpenFileListener;
    static ConcurrentHashMap<Long, Integer> mDownloadIdMap = new ConcurrentHashMap <>();
    static ConcurrentHashMap<Integer, Integer> mTaskIdMap = new ConcurrentHashMap <>();

    public interface OpenFileListener {
        void onComplete(Uri destUri);
    }

    private FileDownloadManager(Context context) {
        mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public static synchronized FileDownloadManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FileDownloadManager(context);
        }
        return mInstance;
    }

    public void setOpenFileListener(OpenFileListener l) {
        mOpenFileListener = l;
    }

    /**
     * For downloading temporary file. i.e. open file by 3rd app.
     *
     */
    public void start(Context context, FileInfo fileInfo) {
        Uri downloadUri = MediaFactory.createUri(context, fileInfo.path);
        Log.d(TAG, "downloadUri: "+ downloadUri);

        Request request = new Request(downloadUri);
        setRequestDestinationUri(request, new File(NASUtils.getCacheFilesLocation(context)), downloadUri);
//                request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
        enqueue(request);
    }

    /**
     * For downloading file from NAS.
     *
     */
    public void start(Context context, String srcPath, String destPath, String localUniqueName, int taskId) {
        Log.d(TAG, "[Enter] start");
//        Log.d(TAG, "path: "+ srcPath);
        Uri downloadUri = MediaFactory.createUri(context, srcPath);
//        Log.d(TAG, "downloadUri: "+ downloadUri);
        Request request = new Request(downloadUri);
        setRequestDestinationUri(request, new File(destPath), localUniqueName);

        int filesCount;
        if (mTaskIdMap.containsKey(taskId)) {
            filesCount = mTaskIdMap.get(taskId) + 1;
        } else {
            filesCount = 1;
        }
        mTaskIdMap.put(taskId, filesCount);

        long downloadId = enqueue(request);
        mDownloadIdMap.put(downloadId, taskId);

        Log.d(TAG, "[Enter] mDownloadIdMap.put() downloadId: "+ downloadId+ " taskId: "+ taskId+ " filesCount: "+ filesCount);

    }

    private void setRequestDestinationUri(Request request, File dirFile, String localUniqueName) {
        String filePath = dirFile.toString().concat("/").concat(localUniqueName);
        request.setDestinationUri(Uri.fromFile(new File(filePath)));
    }

    //TODO check this method
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

    OpenFileListener getOpenFileListener() {
        return mOpenFileListener;
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

    private long enqueue(Request request) {
        return mDownloadId = mDownloadManager.enqueue(request);
    }

    //TODO check this method
    void clearDownloadQueue() {
        mDownloadId = 0L;
    }

}
