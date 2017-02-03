package com.transcend.nas.viewer.document;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.transcend.nas.NASUtils;

import java.io.File;

/**
 * Created by steve_su on 2017/1/25.
 */

public class TempFileDownloadManager extends AbstractDownloadManager {
    private OpenFileListener mOpenFileListener;

    public interface OpenFileListener {
        void onComplete(Uri destUri);
    }

    TempFileDownloadManager(Context context) {
        super(context);
    }

    @Override
    protected String onDownloadDestination(Bundle data) {
        return NASUtils.getCacheFilesLocation(getContext());
    }

    public void setOpenFileListener(OpenFileListener l) {
        mOpenFileListener = l;
    }

    OpenFileListener getOpenFileListener() {
        return mOpenFileListener;
    }

    @Override
    protected void onPreProcess() {
        deleteExistFile(getFileTargetPath());
    }

    private void deleteExistFile(String filePath) {
        File tempFile = new File(filePath);
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }

}
