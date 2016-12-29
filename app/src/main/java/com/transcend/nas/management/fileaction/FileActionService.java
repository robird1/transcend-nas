package com.transcend.nas.management.fileaction;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ike_lee on 2016/12/21.
 */
abstract class FileActionService {
    public String TAG = FileActionService.class.getSimpleName();
    protected int OPEN;
    protected int LIST;
    protected int DOWNLOAD;
    protected int UPLOAD;
    protected int CreateFOLDER;
    protected int RENAME;
    protected int COPY;
    protected int MOVE;
    protected int DELETE;
    protected int SHARE;

    public enum FileAction {
        OPEN, LIST, DOWNLOAD, UPLOAD, RENAME, COPY, MOVE, DELETE, CreateFOLDER, SHARE
    }

    public int getLoaderID(FileAction action){
        Log.d(TAG, action.toString());
        int id = -1;
        switch (action) {
            case OPEN:
                id = OPEN;
                break;
            case LIST:
                id = LIST;
                break;
            case DOWNLOAD:
                id = DOWNLOAD;
                break;
            case UPLOAD:
                id = UPLOAD;
                break;
            case RENAME:
                id = RENAME;
                break;
            case COPY:
                id = COPY;
                break;
            case MOVE:
                id = MOVE;
                break;
            case DELETE:
                id = DELETE;
                break;
            case CreateFOLDER:
                id = CreateFOLDER;
                break;
            case SHARE:
                id = SHARE;
                break;
        }

        return id;
    }

    public Loader<Boolean> onCreateLoader(Context context, FileAction id, Bundle args){
        ArrayList<String> paths = args.getStringArrayList("paths");
        String path = args.getString("path");
        String name = args.getString("name");
        switch (id) {
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
        }

        return null;
    }

    public abstract void onLoadFinished(Context context, Loader<Boolean> loader, Boolean success);

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
}
