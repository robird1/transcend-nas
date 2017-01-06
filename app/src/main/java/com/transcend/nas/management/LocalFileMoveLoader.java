package com.transcend.nas.management;

import android.content.Context;
import android.util.Log;

import com.transcend.nas.R;
import com.transcend.nas.common.CustomNotificationManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by silverhsu on 16/2/18.
 */
public class LocalFileMoveLoader extends LocalAbstractLoader {

    private static final String TAG = LocalFileMoveLoader.class.getSimpleName();
    private List<String> mSrcs;
    private String mDest;

    public LocalFileMoveLoader(Context context, List<String> srcs, String dest) {
        super(context);
        setType(getContext().getString(R.string.move));
        mSrcs = srcs;
        mDest = dest;
        mTotal = mSrcs.size();
        mCurrent = 0;
        mNotificationID = CustomNotificationManager.getInstance().queryNotificationID();
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return move();
        } catch (IOException e) {
            e.printStackTrace();
            closeProgressWatcher();
            updateResult(getContext().getString(R.string.error), mDest);
        }
        return false;
    }

    private boolean move() throws IOException {
        updateProgress(getContext().getResources().getString(R.string.loading), 0, 0);
        for (String path : mSrcs) {
            File source = new File(path);
            if (source.getParent().equals(mDest))
                continue;
            if (source.isDirectory())
                moveDirectory(source, mDest);
            else
                moveFile(source, mDest);
            mCurrent++;
        }
        updateResult(getContext().getString(R.string.done), mDest);
        return true;
    }

    private void moveDirectory(File source, String destination) throws IOException  {
        String name = createUniqueName(source, destination);
        File target = new File(destination, name);
        boolean isSuccess = target.mkdirs();
        if (!isSuccess) {
            throw new IOException();
        }
        File[] files = source.listFiles();
        for (File file : files) {
            if (!file.isHidden())
                mTotal++;
        }

        String path = target.getPath();
        for (File file : files) {
            if(file.isHidden())
                continue;

            if (file.isDirectory())
                moveDirectory(file, path);
            else
                moveFile(file, path);
            mCurrent++;
        }
        isSuccess = source.delete();
        if (!isSuccess) {
            throw new IOException();
        }
    }

    private void moveFile(File source, String destination) throws IOException {
        String name = createUniqueName(source, destination);
        File target = new File(destination, name);
        int total = (int) source.length();
        startProgressWatcher(name, target, total);
//        source.renameTo(target);
        FileUtils.copyFile(source, target);
        boolean isSuccess = source.delete();
        if (!isSuccess) {
            throw new IOException();
        }
        closeProgressWatcher();
        updateProgress(name, total, total);
    }

}
