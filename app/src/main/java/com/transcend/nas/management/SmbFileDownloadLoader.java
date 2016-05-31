package com.transcend.nas.management;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

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
        mNotificationID = FileFactory.getInstance().getNotificationID();
        mType = getContext().getString(R.string.download);
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return download();
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

    private boolean download() throws IOException {
        for (String path : mSrcs) {
            SmbFile source = new SmbFile(getSmbUrl(path));
            if (source.isDirectory())
                downloadDirectory(source, mDest);
            else
                downloadFile(source, mDest);
        }
        updateResult(mType, "Done");
        return true;
    }

    private void downloadDirectory(SmbFile source, String destination) throws IOException {
        String name = createLocalUniqueName(source, destination);
        File target = new File(destination, name);
        target.mkdirs();
        SmbFile[] files = source.listFiles();
        String path = target.getPath();
        for (SmbFile file : files) {
            if (file.isDirectory())
                downloadDirectory(file, path);
            else
                downloadFile(file, path);
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

    private String createLocalUniqueName(SmbFile source, String destination) throws MalformedURLException, SmbException {
        final boolean isDirectory = source.isDirectory();
        File dir = new File(destination);
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() == isDirectory;
            }
        });
        List<String> names = new ArrayList<String>();
        for (File file : files) names.add(file.getName());
        String origin = source.getName().replace("/", ""); // remove last character "/"
        String unique = origin;
        String ext = FilenameUtils.getExtension(origin);
        String prefix = FilenameUtils.getBaseName(origin);
        String suffix = ext.isEmpty() ? "" : String.format(".%s", ext);
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
