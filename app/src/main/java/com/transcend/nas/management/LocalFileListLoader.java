package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by silverhsu on 16/1/15.
 */
public class LocalFileListLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = LocalFileListLoader.class.getSimpleName();

    private ArrayList<FileInfo> mFileList;
    private String mPath;

    public LocalFileListLoader(Context context, String path) {
        super(context);
        mFileList = new ArrayList<FileInfo>();
        mPath = path;
    }

    @Override
    public Boolean loadInBackground() {
        return updateFileList();
    }

    private boolean updateFileList() {
        if (mPath == null)
            return false;
        File dir = new File(mPath);
        if (!dir.exists())
            return false;
        File files[] = dir.listFiles();
        Log.w(TAG, "LocalFile[] size: " + files.length);
        for (File file : files) {
            if (file.isHidden())
                continue;
            FileInfo fileInfo = new FileInfo();
            fileInfo.path = file.getPath();
            fileInfo.name = file.getName();
            fileInfo.time = FileInfo.getTime(file.lastModified());
            fileInfo.type = file.isFile() ? FileInfo.getType(file.getPath()) : FileInfo.TYPE.DIR;
            fileInfo.size = file.length();
            mFileList.add(fileInfo);
        }
        Collections.sort(mFileList, FileInfoSort.comparator(getContext()));
        Log.w(TAG, "mFileList size: " + mFileList.size());
        return true;
    }

    public String getPath() {
        return mPath;
    }

    public ArrayList<FileInfo> getFileList() {
        return mFileList;
    }

}
