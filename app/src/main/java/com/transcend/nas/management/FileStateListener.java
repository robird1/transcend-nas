package com.transcend.nas.management;

import android.os.FileObserver;
import android.util.Log;

/**
 * Created by steve_su on 2016/11/2.
 */

public class FileStateListener extends FileObserver {
    private static final String TAG = FileStateListener.class.getSimpleName();
    private String mPath;
    private boolean mIsModified = false;

    public FileStateListener(String path) {
        super(path, FileObserver.MODIFY);
        mPath = path;
    }

    @Override
    public void onEvent(int event, String path) {
        Log.d(TAG, "event: "+ event);

        if (event == FileObserver.MODIFY) {
            mIsModified = true;
            Log.d(TAG, "mIsModified = true");
        }

        if (event == FileObserver.CLOSE_NOWRITE || event == FileObserver.CLOSE_WRITE) {
            stopWatching();
            Log.d(TAG, "stopWatching()");
        }
    }

    public String getPath()
    {
        return mPath;
    }

    public boolean isFileModified()
    {
        return mIsModified;
    }

    public void resetMonitoringFlag()
    {
        mIsModified = false;
    }

}
