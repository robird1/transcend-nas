package com.transcend.nas.management.externalstorage;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.SmbAbstractLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jcifs.smb.SmbFile;

/**
 * Created by steve_su on 2017/1/3.
 */

public class OTGFileDownloadLoader extends SmbAbstractLoader {
    private static final String TAG = OTGFileDownloadLoader.class.getSimpleName();

    private List<String> mSrcs;
    private String mDest;
    private DocumentFile mDestFileItem;

    private boolean mForbidden;
    private Timer mTimer;

    public OTGFileDownloadLoader(Context context, List<String> srcs, String dest, DocumentFile destFileItem) {
        super(context);
        mSrcs = srcs;
        mDest = dest;
        mDestFileItem = destFileItem;

        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID();
        mType = getContext().getString(R.string.download);
        mTotal = mSrcs.size();
        mCurrent = 0;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return download();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
            updateResult(mType, getContext().getString(R.string.error), mDest);
        }
        return false;
    }

    private boolean download() throws IOException {
        for (String path : mSrcs) {
            SmbFile source = new SmbFile(getSmbUrl(path));
            if (source.isDirectory())
                downloadDirectoryTask(mActivity, source, mDestFileItem);
            else
                downloadFileTask(mActivity, source, mDestFileItem);
            mCurrent++;
        }
        updateResult(mType, getContext().getString(R.string.done), mDest);
        return true;
    }

    private void downloadDirectoryTask(Context context, SmbFile srcFileItem, DocumentFile destFileItem) throws IOException {
        String dirName = createLocalUniqueName(srcFileItem, DocumentFileHelper.getPath(context, destFileItem.getUri()));
        DocumentFile destDirectory = destFileItem.createDirectory(dirName);
        SmbFile[] files = srcFileItem.listFiles();
        mTotal += files.length;

        for (SmbFile file : files) {
            Log.d(TAG, "file.getPath(): "+ file.getPath());
            if (file.isDirectory()) {
                downloadDirectoryTask(mActivity, file, destDirectory);
            } else {
                downloadFileTask(mActivity, file, destDirectory);
            }
            mCurrent++;
        }
    }

    private void downloadFileTask(Context context, SmbFile srcFileItem, DocumentFile destFileItem) throws IOException {
        Log.d(TAG, "[Enter] downloadFileTask()");
        String fileName = createLocalUniqueName(srcFileItem, DocumentFileHelper.getPath(context, destFileItem.getUri()));
        DocumentFile destfile = destFileItem.createFile(null, fileName);
        updateProgress(mType, fileName, 0, srcFileItem.getContentLength());
        downloadFile(context, srcFileItem, destfile);
    }

    public boolean downloadFile(Context context, SmbFile srcFileItem, DocumentFile destFileItem) throws IOException {
        if (srcFileItem.isFile()) {
            try {
                InputStream in = srcFileItem.getInputStream();
                OutputStream out = context.getContentResolver().openOutputStream(destFileItem.getUri());
                byte[] buf = new byte[8192];
                int len;
                int count = 0;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                    count += len;
                    updateProgressPerSecond(destFileItem.getName(), count, srcFileItem.getContentLength());
                }
                in.close();
                out.close();
                updateProgressPerSecond(destFileItem.getName(), count, srcFileItem.getContentLength());

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.d(TAG, "[Enter] FileNotFoundException");
                throw new FileNotFoundException();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "[Enter] IOException");
                throw new IOException();
            }
            return true;
        } else if (srcFileItem.isDirectory()) {
            return true;
        } else {
            throw new FileNotFoundException("item is not a file");
        }
    }

    private void updateProgressPerSecond(String name, int count, int total) {
        if (mForbidden)
            return;
        mForbidden = true;
        updateProgress(mType, name, count, total);
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mForbidden = false;
            }
        }, 1000);
    }
}
