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
import java.io.FileInputStream;
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

    private String mUniqueName;
    private boolean mIsOpenWithUpload = false;

    public LocalFileUploadLoader(Context context, List<String> srcs, String dest) {
        super(context);
        mSrcs = srcs;
        mDest = dest;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID(this);
        mTotal = mSrcs.size();
        mCurrent = 0;
        setType(getContext().getString(R.string.upload));
    }

    public LocalFileUploadLoader(Context context, List<String> srcs, String dest, boolean isOpenWithUpload) {
        this(context, srcs, dest);
        mIsOpenWithUpload = isOpenWithUpload;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            super.loadInBackground();
            return upload();
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

    private boolean upload() throws IOException {
        for (String path : mSrcs) {
            if(isLoadInBackgroundCanceled())
                return true;

            File source = new File(path);
            if (source.isDirectory())
                uploadDirectory(source, getSmbUrl(mDest));
            else
                uploadFile(source, getSmbUrl(mDest));
            mCurrent++;
        }
        updateResult(mType, getContext().getString(R.string.done), mDest);
        return true;
    }

    private void uploadDirectory(File source, String destination) throws IOException {
        String name = createUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        target.mkdirs();
        File[] files = source.listFiles();
        for (File file : files) {
            if (!file.isHidden())
                mTotal++;
        }
        String path = target.getPath();
        for (File file : files) {
            if(isLoadInBackgroundCanceled())
                return;

            if(file.isHidden())
                continue;

            if (file.isDirectory())
                uploadDirectory(file, path);
            else
                uploadFile(file, path);
            mCurrent++;
        }
    }

    private void uploadFile(File source, String destination) throws IOException {
        int total = (int)source.length();
        int count = 0;
        mUniqueName = createUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, mUniqueName);
        mOS = new BufferedOutputStream(target.getOutputStream());
        mIS = new BufferedInputStream(new FileInputStream(source));
        updateProgress(mType, mUniqueName, 0, total);
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = 0;
        while ((length = mIS.read(buffer)) != -1) {
            if(isLoadInBackgroundCanceled())
                break;

            mOS.write(buffer, 0, length);
            count += length;
            updateProgressPerSecond(mUniqueName, count, total);
        }
        mOS.close();
        mIS.close();
        updateProgress(mType, mUniqueName, count, total);
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
        if(unique.startsWith("."))
            unique = unique.substring(1);
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

    public String getUniqueFileName()
    {
        return mUniqueName;
    }

    public boolean isOpenWithUpload()
    {
        return mIsOpenWithUpload;
    }

}
