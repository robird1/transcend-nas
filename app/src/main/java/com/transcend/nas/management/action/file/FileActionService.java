package com.transcend.nas.management.action.file;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.transcend.nas.management.externalstorage.ExternalStorageController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ike_lee on 2016/12/21.
 */
public abstract class FileActionService {
    public String TAG = FileActionService.class.getSimpleName();
    protected String mMode;
    protected String mRoot;
    protected String mPath;
    protected ExternalStorageController mExternalStorageController;
    protected HashMap<FileAction, Integer> mFileActionIDs;

    public enum FileAction {
        OPEN, LIST, DOWNLOAD, UPLOAD, RENAME, COPY, MOVE, DELETE, CreateFOLDER, SHARE, ShareLINK
    }

    public FileActionService (){
        mFileActionIDs = new HashMap<>();
        initLoaderID(mFileActionIDs);
    }

    public abstract void initLoaderID(HashMap<FileAction, Integer> ids);

    public String getMode(Context context){
        return mMode;
    }

    public String getRootPath(Context context){
        return mRoot;
    }

    public void setCurrentPath(String path){
        mPath = path;
    }

    protected boolean isWritePermissionRequired(Context context, String... path) {
        if(mExternalStorageController == null)
            mExternalStorageController = new ExternalStorageController(context);
        return mExternalStorageController.isWritePermissionRequired(path);
    }

    public FileAction getFileAction(int action){
        for(FileAction type : mFileActionIDs.keySet()) {
            int id = mFileActionIDs.get(type);
            if(id > 0 && id == action)
                return type;
        }
        return null;
    }

    public int getLoaderID(FileAction action){
        int id = mFileActionIDs.get(action);
        return id;
    }

    public Loader<Boolean> onCreateLoader(Context context, FileAction id, Bundle args){
        ArrayList<String> paths = args.getStringArrayList("paths");
        String path = args.getString("path");
        String name = args.getString("name");
        switch (id) {
            case OPEN:
                return open(context, path);
            case LIST:
                return list(context, path);
            case UPLOAD:
                return upload(context, paths, path);
            case DOWNLOAD:
                return download(context, paths, path);
            case RENAME:
                return rename(context, path, name);
            case COPY:
                return copy(context, paths, path);
            case MOVE:
                return move(context, paths, path);
            case DELETE:
                return delete(context, paths);
            case CreateFOLDER:
                return createFolder(context, path);
            case SHARE:
                return share(context, paths, path);
            case ShareLINK:
                return shareLink(context, paths);
        }

        return null;
    }

    public boolean onLoadFinished(Context context, Loader<Boolean> loader, Boolean success) {
        return onLoadFinished(context, null, loader, success);
    }

    public abstract boolean onLoadFinished(Context context, RelativeLayout progress, Loader<Boolean> loader, Boolean success);

    protected abstract AsyncTaskLoader open(Context context, String path);

    protected abstract AsyncTaskLoader list(Context context, String path);

    protected abstract AsyncTaskLoader download(Context context, List<String> list, String dest);

    protected abstract AsyncTaskLoader upload(Context context, List<String> list, String dest);

    protected abstract AsyncTaskLoader rename(Context context, String path, String name);

    protected abstract AsyncTaskLoader copy(Context context, List<String> list, String dest);

    protected abstract AsyncTaskLoader move(Context context, List<String> list, String dest);

    protected abstract AsyncTaskLoader delete(Context context, List<String> list);

    protected abstract AsyncTaskLoader createFolder(Context context, String path);

    protected abstract AsyncTaskLoader share(Context context, ArrayList<String> paths, String dest);

    protected abstract AsyncTaskLoader shareLink(Context context, ArrayList<String> paths);
}
