package com.transcend.nas.management.download;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.transcend.nas.management.firmware.MediaFactory;

import java.io.File;

/**
 * Created by steve_su on 2017/1/25.
 */

public abstract class AbstractDownloadManager {
    public static final String KEY_SOURCE_PATH = "source_path";
    public static final String KEY_TARGET_PATH = "target_path";
    public static final String KEY_FILE_NAME = "file_name";
    public static final String KEY_TASK_ID = "task_id";
    protected Context mContext;
    protected DownloadManager mDownloadManager;
    private long mDownloadId;
    private Bundle mData;
    private String mFileTargetPath;

    AbstractDownloadManager(Context context) {
        mContext = context;
        mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    /**
     * If the downloaded file is for viewing, add the file source path to the bundle data (i.e. KEY_SOURCE_PATH);
     * If the downloaded file is for storing, add the source path, destination path, unique file name and task ID to the bundle data
     * (i.e. KEY_SOURCE_PATH, KEY_TARGET_PATH, KEY_FILE_NAME, KEY_TASK_ID).
     *
     * @param data The related download data.
     */
    public void start(Bundle data) {
        if (data == null) {
            return;
        }
        mData = data;
        Uri uri = MediaFactory.createUri(getContext(), data.getString(KEY_SOURCE_PATH));
        DownloadManager.Request request = new DownloadManager.Request(uri);
        mFileTargetPath = setRequestDestinationUri(request, data);

        onPreProcess();
        enqueue(request);
        onPostProcess();
    }

    public long cancel() {
        if (mDownloadId != 0L) {
            long id = mDownloadManager.remove(mDownloadId);
            mDownloadId = 0L;
            return id;
        }
        return -1L;
    }

    private long enqueue(DownloadManager.Request request) {
        return mDownloadId = mDownloadManager.enqueue(request);
    }

    private String setRequestDestinationUri(DownloadManager.Request request, Bundle data) {
        File dir = new File(onDownloadDestination(data));
        String filename = onFileName(data);
        String filePath = dir.toString().concat("/").concat(filename);
        request.setTitle(filename);
        request.setDestinationUri(Uri.fromFile(new File(filePath)));
        return filePath;
    }

    protected Context getContext() {
        return mContext;
    }

    protected long getDownloadId() {
        return mDownloadId;
    }

    protected Bundle getDownloadData() {
        return mData;
    }

    protected String getFileTargetPath() {
        return mFileTargetPath;
    }

    protected String onDownloadDestination(Bundle data) {
        return data.getString(KEY_TARGET_PATH);
    }

    protected String onFileName(Bundle data) {
        Uri uri = Uri.fromFile(new File(data.getString(KEY_SOURCE_PATH)));
        return uri.getLastPathSegment();
    }

    protected void onPreProcess() {

    }

    protected void onPostProcess() {

    }

}
