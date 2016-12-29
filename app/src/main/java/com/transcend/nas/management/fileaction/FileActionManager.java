package com.transcend.nas.management.fileaction;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * Created by ike_lee on 2016/12/21.
 */
public class FileActionManager implements LoaderManager.LoaderCallbacks<Boolean> {
    private Context mContext;
    private RelativeLayout mProgressLayout;
    private FileActionService mFileActionService;
    private FileActionServiceType mFileActionServiceType;
    private FileActionService.FileAction mFileActionType;
    private FileActionListener mFileActionListener;

    public enum FileActionServiceType {
        ANDROID, SMB
    }

    public interface FileActionListener {
        public void onFileActionCreate(Loader<Boolean> loader, int id, Bundle args);
        public void onFileActionFinished(Loader<Boolean> loader, Boolean success);
    }

    public FileActionManager(Context context, FileActionServiceType service, FileActionListener listener) {
        this(context, service, listener, null);
    }

    public FileActionManager(Context context, FileActionServiceType service, FileActionListener listener, RelativeLayout progressLayout) {
        mContext = context;
        mFileActionServiceType = service;
        mFileActionListener = listener;
        mProgressLayout = progressLayout;
        setServiceType(service);
    }


    public void setServiceType(FileActionServiceType type) {
        if (mFileActionService != null && mFileActionServiceType == type)
            return;

        mFileActionServiceType = type;
        switch (type) {
            case ANDROID:
                mFileActionService = new LocalFileActionService();
                break;
            case SMB:
                mFileActionService = new SmbFileActionService();
                break;
        }
    }

    public void list(String path) {
        createLoader(FileActionService.FileAction.LIST, null, path, null);
    }

    public void download(String dest, ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.DOWNLOAD, null, dest, paths);
    }

    public void upload(String dest, ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.UPLOAD, null, dest, paths);
    }

    public void rename(String path, String name) {
        createLoader(FileActionService.FileAction.RENAME, name, path, null);
    }

    public void copy(String dest, ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.COPY, null, dest, paths);
    }

    public void move(String dest, ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.MOVE, null, dest, paths);
    }

    public void delete(ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.DELETE, null, null, paths);
    }

    public void createFolder(String dest, String newName) {
        mFileActionType = FileActionService.FileAction.CreateFOLDER;
        StringBuilder builder = new StringBuilder(dest);
        if (!dest.endsWith("/"))
            builder.append("/");
        builder.append(newName);
        String path = builder.toString();

        createLoader(FileActionService.FileAction.CreateFOLDER, null, path, null);
    }


    public void share(String dest, ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.SHARE, null, dest, paths);
    }

    private void createLoader(FileActionService.FileAction type, String name, String dest, ArrayList<String> paths) {
        mFileActionType = type;

        int id = mFileActionService.getLoaderID(type);
        Bundle args = new Bundle();
        if(name != null)
            args.putString("name", name);
        if(dest != null)
            args.putString("path", dest);
        if(paths != null)
            args.putStringArrayList("paths", paths);

        ((Activity) mContext).getLoaderManager().restartLoader(id, args, this).forceLoad();
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        if (mFileActionService != null) {
            if (mProgressLayout != null && mFileActionType != null) {
                switch (mFileActionType) {
                    case LIST:
                    case RENAME:
                    case DELETE:
                    case CreateFOLDER:
                        mProgressLayout.setVisibility(View.VISIBLE);
                        break;
                    default:
                        mProgressLayout.setVisibility(View.INVISIBLE);
                        break;
                }
            }

            Loader<Boolean> loader = mFileActionService.onCreateLoader(mContext, mFileActionType, args);
            if (mFileActionListener != null)
                mFileActionListener.onFileActionCreate(loader, id, args);
            return loader;
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (mFileActionService != null) {
            mFileActionService.onLoadFinished(mContext, loader, success);

            if (mFileActionListener != null)
                mFileActionListener.onFileActionFinished(loader, success);

            if (mProgressLayout != null)
                mProgressLayout.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }


}
