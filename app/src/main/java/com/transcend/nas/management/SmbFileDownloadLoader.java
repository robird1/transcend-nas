package com.transcend.nas.management;

import android.content.Context;
import android.util.Log;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.firmware.FileFactory;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * Created by silverhsu on 16/2/18.
 */
public class SmbFileDownloadLoader extends SmbAbstractLoader {

    private static final String TAG = SmbFileDownloadLoader.class.getSimpleName();
    private static final int BUFFER_SIZE = 4 * 1024;

    private boolean mForbidden;
    private Timer mTimer;

    private List<String> mSrcs;
    private String mDest;

    private OutputStream mOS;
    private InputStream mIS;

    public SmbFileDownloadLoader(Context context, List<String> srcs, String dest) {
        super(context);
        mSrcs = srcs;
        mDest = dest;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID(this);
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
        } finally {
            try {
                if (mOS != null) mOS.close();
                if (mIS != null) mIS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean download() throws IOException {
        for (String path : mSrcs) {
            SmbFile source = new SmbFile(getSmbUrl(path));
            if (source.isDirectory())
                downloadDirectory(source, mDest);
            else
                downloadFile(source, mDest);
            mCurrent++;
        }
        updateResult(mType, getContext().getString(R.string.done), mDest);
        return true;
    }

    private void downloadDirectory(SmbFile source, String destination) throws IOException {
        String name = createLocalUniqueName(source, destination);
        File target = new File(destination, name);
        target.mkdirs();
        SmbFile[] files = source.listFiles();
        for (SmbFile file : files) {
            if (!file.isHidden())
                mTotal++;
        }

        String path = target.getPath();
        for (SmbFile file : files) {
            if(file.isHidden())
                continue;

            if (file.isDirectory())
                downloadDirectory(file, path);
            else
                downloadFile(file, path);
            mCurrent++;
        }
    }

    private void downloadFile(SmbFile source, String destination) throws IOException {
        int total = source.getContentLength();
        int count = 0;
        String name = createLocalUniqueName(source, destination);
        File target = new File(destination, name);
        mOS = new BufferedOutputStream(new FileOutputStream(target));
        mIS = new BufferedInputStream(source.getInputStream());
        updateProgress(mType, name, count, total);
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = 0;
        while ((length = mIS.read(buffer)) != -1) {
            mOS.write(buffer, 0, length);
            count += length;
            updateProgressPerSecond(name, count, total);
        }
        mOS.close();
        mIS.close();
        updateProgressPerSecond(name, count, total);
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
