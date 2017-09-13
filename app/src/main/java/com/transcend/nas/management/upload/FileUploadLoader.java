package com.transcend.nas.management.upload;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.management.SmbAbstractLoader;
import com.transcend.nas.management.firmware.ShareFolderManager;
import com.transcend.nas.service.FileRecentFactory;
import com.transcend.nas.service.FileRecentInfo;
import com.transcend.nas.service.FileRecentManager;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedOutputStream;
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
public abstract class FileUploadLoader<T> extends SmbAbstractLoader {

    private static final String TAG = FileUploadLoader.class.getSimpleName();

    private static final int BUFFER_SIZE = 4 * 1024;
    private boolean mForbidden;
    private Timer mTimer;

    private List<T> mSrcs;
    private String mDest;
    private String mUniqueName;
    private OutputStream mOS;
    private InputStream mIS;

    public FileUploadLoader(Context context, List<T> srcs, String dest) {
        super(context);
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID(this);
        mDest = dest;
        setType(getContext().getString(R.string.upload));
        mSrcs = srcs;
    }

    @Override
    public Boolean loadInBackground() {
        super.loadInBackground();

        try {
            init();
            return upload();
        } catch (Exception e) {
            e.printStackTrace();
            setException(e);
            updateResult(getContext().getString(R.string.error), mDest);
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

    @Override
    protected void updateResult(String result, String destination) {
        if(isLoadInBackgroundCanceled()) {
            return;
        }

        CustomNotificationManager.updateResult(getContext(), mNotificationID, getType(), result, destination, true);
    }

    protected void init() {
        mCurrent = 0;
        mTotal = mSrcs != null ? mSrcs.size() : 0;
    }

    protected boolean upload() throws IOException {
        if (mSrcs == null)
            return false;

        for (T src : mSrcs) {
            if (isLoadInBackgroundCanceled())
                return true;

            boolean success = false;
            SmbFile target = null;
            String destination = getSmbUrl(mDest);
            if (isDirectory(src)) {
                success = uploadDirectory(src, destination);
            } else {
                mUniqueName = createUniqueName(src, destination);
                mIS = createInputStream(src);
                if (mUniqueName != null && mIS != null) {
                    target = new SmbFile(destination, mUniqueName);
                    mOS = new BufferedOutputStream(target.getOutputStream());
                    success = upload(getType(), mUniqueName, mIS, mOS);
                }
            }

            if (!success) {
                updateResult(getContext().getString(R.string.error), mDest);
                return false;
            }

            if (target != null) {
                FileRecentInfo info = FileRecentFactory.create(getContext(), target, FileRecentInfo.ActionType.UPLOAD);
                if (info != null && info.info != null) {
                    String filePath = TextUtils.concat(mDest, target.getName().replace("/", "")).toString();
                    info.info.path = filePath;
                    info.realPath = ShareFolderManager.getInstance().getRealPath(filePath);
                    FileRecentManager.getInstance().setAction(info);
                }
            }
            mCurrent++;
        }
        updateResult(getContext().getString(R.string.done), mDest);
        return true;
    }


    private String createUniqueName(T src, String destination) throws MalformedURLException, SmbException {
        String origin = parserFileName(src);
        if (origin == null || "".equals(origin))
            return null;

        final boolean isDirectory = isDirectory(src);
        SmbFile dir = new SmbFile(destination);
        SmbFile[] files = dir.listFiles(new SmbFileFilter() {
            @Override
            public boolean accept(SmbFile file) throws SmbException {
                return file.isDirectory() == isDirectory;
            }
        });

        List<String> names = new ArrayList<String>();
        for (SmbFile file : files)
            names.add(file.getName());
        String unique = isDirectory ? String.format("%s/", origin) : origin;
        if (unique.startsWith("."))
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

    private boolean uploadDirectory(T source, String destination) throws IOException {
        String name = createUniqueName(source, destination);
        SmbFile target = new SmbFile(destination, name);
        target.mkdirs();

        List<T> files = listDirectory(source);
        if (files != null) {
            mTotal = mTotal + files.size();
            String path = target.getPath();
            for (T file : files) {
                if (isLoadInBackgroundCanceled())
                    return false;

                if (isDirectory(file)) {
                    uploadDirectory(file, path);
                } else {
                    mUniqueName = createUniqueName(file, path);
                    mIS = createInputStream(file);
                    if (mUniqueName != null && mIS != null) {
                        target = new SmbFile(path, mUniqueName);
                        mOS = new BufferedOutputStream(target.getOutputStream());
                        success = upload(getType(), mUniqueName, mIS, mOS);
                    }
                }
                mCurrent++;
            }
        }
        return true;
    }

    private boolean upload(String type, String name, InputStream is, OutputStream os) throws IOException {
        int total = is.available();
        int count = 0;
        updateProgress(name, 0, total);

        byte[] buffer = new byte[BUFFER_SIZE];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            if (isLoadInBackgroundCanceled())
                break;

            os.write(buffer, 0, length);
            count += length;
            updateProgressPerSecond(name, count, total);
        }
        os.close();
        is.close();
        updateProgress(name, count, total);
        return true;
    }

    private void updateProgressPerSecond(String name, int count, int total) {
        if (mForbidden)
            return;
        mForbidden = true;
        updateProgress(name, count, total);
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mForbidden = false;
            }
        }, 1000);
    }

    public String getUniqueFileName() {
        return mUniqueName;
    }

    protected abstract boolean isDirectory(T src);

    protected abstract List<T> listDirectory(T src);

    protected abstract String parserFileName(T src);

    protected abstract InputStream createInputStream(T src);
}
