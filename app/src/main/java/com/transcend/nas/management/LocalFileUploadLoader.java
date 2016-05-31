package com.transcend.nas.management;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.transcend.nas.R;
import com.transcend.nas.utils.FileFactory;
import com.transcend.nas.utils.MathUtil;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
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
import jcifs.smb.SmbFileFilter;

/**
 * Created by silverhsu on 16/2/22.
 */
public class LocalFileUploadLoader extends SmbAbstractLoader {

    private static final String TAG = LocalFileUploadLoader.class.getSimpleName();

    private static final int BUFFER_SIZE = 4 * 1024;

    private boolean mForbidden;
    private Timer mTimer;

    private List<String> mSrcs;
    private String mDest;

    private OutputStream mOS;
    private InputStream mIS;

    public LocalFileUploadLoader(Context context, List<String> srcs, String dest) {
        super(context);
        mSrcs = srcs;
        mDest = dest;
        mNotificationID = FileFactory.getInstance().getNotificationID();
        mType = getContext().getString(R.string.upload);
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return upload();
        } catch (Exception e) {
            e.printStackTrace();
            updateResult(mType, "Error");
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

    private boolean upload() throws IOException {
        for (String path : mSrcs) {
            File source = new File(path);
            if (source.isDirectory())
                uploadDirectory(source, getSmbUrl(mDest));
            else
                uploadFile(source, getSmbUrl(mDest));
        }
        updateResult(mType, "Done");
        return true;
    }

    private void uploadDirectory(File source, String destination) throws IOException {
        String name = createUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        target.mkdirs();
        File[] files = source.listFiles();
        String path = target.getPath();
        for (File file : files) {
            if(file.isHidden())
                continue;

            if (file.isDirectory())
                uploadDirectory(file, path);
            else
                uploadFile(file, path);
        }
    }

    private void uploadFile(File source, String destination) throws IOException {
        int total = (int)source.length();
        int count = 0;
        String name = createUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        mOS = new BufferedOutputStream(target.getOutputStream());
        mIS = new BufferedInputStream(new FileInputStream(source));
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

    private String createUniqueName(File source, String destination) throws MalformedURLException, SmbException {
        final boolean isDirectory = source.isDirectory();
        SmbFile dir = new SmbFile(destination);
        SmbFile[] files = dir.listFiles(new SmbFileFilter() {
            @Override
            public boolean accept(SmbFile file) throws SmbException {
                return file.isDirectory() == isDirectory;
            }
        });
        List<String> names = new ArrayList<String>();
        for (SmbFile file : files) names.add(file.getName());
        String origin = source.getName();
        String unique = isDirectory ? String.format("%s/", origin) : origin;
        String ext = FilenameUtils.getExtension(origin);
        String prefix = FilenameUtils.getBaseName(origin);
        String suffix = isDirectory ? "/" : ext.isEmpty() ? "" : String.format(".%s", ext);
        int index = 2;
        while (names.contains(unique)) {
            unique = String.format(prefix + "_%d" + suffix, index++);
        }
        Log.w(TAG, "unique name: " + unique);
        return unique;
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
