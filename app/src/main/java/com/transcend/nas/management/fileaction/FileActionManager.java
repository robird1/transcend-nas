package com.transcend.nas.management.fileaction;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.externalstorage.ExternalStorageController;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ike_lee on 2016/12/21.
 */
public class FileActionManager extends AbstractActionManager {
    private static final String TAG = FileActionManager.class.getSimpleName();

    private Context mContext;
    private FileActionService mFileActionService;
    private Map<FileActionServiceType, FileActionService> mFileActionServicePool;
    private FileActionServiceType mFileActionServiceType;
    private LoaderManager.LoaderCallbacks<Boolean> mCallbacks;

    public enum FileActionServiceType {
        PHONE, SD, SMB
    }

    public FileActionManager(Context context, FileActionServiceType service, LoaderManager.LoaderCallbacks<Boolean> callbacks) {
        this(context, service, callbacks, null);
    }

    public FileActionManager(Context context, FileActionServiceType service, LoaderManager.LoaderCallbacks<Boolean> callbacks, RelativeLayout progressLayout) {
        mContext = context;
        mFileActionServiceType = service;
        mCallbacks = callbacks;
        mProgressLayout = progressLayout;
        setServiceType(service);
    }

    public void setServiceType(FileActionServiceType type) {
        if (mFileActionService != null && mFileActionServiceType == type)
            return;

        if (null == mFileActionServicePool)
            mFileActionServicePool = new HashMap<>();

        FileActionService service = mFileActionServicePool.get(type);
        if (null == service) {
            switch (type) {
                case SD:
                    service = new SdcardActionService();
                    break;
                case PHONE:
                    service = new PhoneActionService();
                    break;
                case SMB:
                    service = new SmbFileActionService();
                    break;
            }
            mFileActionServicePool.put(type, service);
        }

        mFileActionServiceType = type;
        mFileActionService = service;
    }

    public void checkServiceType(String path) {
        if (path.startsWith("/storage")) {
            if (NASUtils.isSDCardPath(mContext, path))
                setServiceType(FileActionManager.FileActionServiceType.SD);
            else
                setServiceType(FileActionManager.FileActionServiceType.PHONE);
        } else {
            setServiceType(FileActionManager.FileActionServiceType.SMB);
        }
    }

    public String getServiceRootPath() {
        String root = NASApp.ROOT_SMB;
        if (mFileActionService != null)
            root = mFileActionService.getRootPath(mContext);
        return root;
    }

    public String getServiceMode() {
        String mode = NASApp.MODE_SMB;
        if (mFileActionService != null)
            mode = mFileActionService.getMode(mContext);
        return mode;
    }

    public FileActionService getFileActionService() {
        return mFileActionService;
    }

    public void setCurrentPath(String path) {
        if (mFileActionService != null)
            mFileActionService.setCurrentPath(path);
    }

    public void setProgressLayout(RelativeLayout progressLayout) {
        mProgressLayout = progressLayout;
    }

    public void list(String path) {
        createLoader(FileActionService.FileAction.LIST, null, path, null);
        Log.w(TAG, "doLoad: " + path);
    }

    public void download(String dest, ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.DOWNLOAD, null, dest, paths);
        Log.w(TAG, "doDownload: " + paths.size() + " item(s) to " + dest);
    }

    public void upload(String dest, ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.UPLOAD, null, dest, paths);
        Log.w(TAG, "doUpload: " + paths.size() + " item(s) to " + dest);
    }

    public void rename(String path, String name) {
        createLoader(FileActionService.FileAction.RENAME, name, path, null);
        Log.w(TAG, "doRename: " + path + ", " + name);
    }

    public void copy(String dest, ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.COPY, null, dest, paths);
        Log.w(TAG, "doCopy: " + paths.size() + " item(s) to " + dest);
    }

    public void move(String dest, ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.MOVE, null, dest, paths);
        Log.w(TAG, "doMove: " + paths.size() + " item(s) to " + dest);
    }

    public void delete(ArrayList<String> paths) {
        createLoader(FileActionService.FileAction.DELETE, null, null, paths);
        Log.w(TAG, "doDelete: " + paths.size() + " items");
    }

    public void createFolder(String dest, String newName) {
        StringBuilder builder = new StringBuilder(dest);
        if (!dest.endsWith("/"))
            builder.append("/");
        builder.append(newName);
        String path = builder.toString();

        createLoader(FileActionService.FileAction.CreateFOLDER, null, path, null);
    }

    public void share(String dest, ArrayList<FileInfo> files) {
        if (isRemoteAction(files.get(0).path)) {
            ArrayList<String> paths = new ArrayList<String>();
            for (FileInfo file : files) {
                paths.add(file.path);
            }
            createLoader(FileActionService.FileAction.SHARE, null, dest, paths);
        } else {
            NASUtils.shareLocalFile(mContext, files);
        }
    }

    private void createLoader(FileActionService.FileAction type, String name, String dest, ArrayList<String> paths) {
        int id = mFileActionService.getLoaderID(type);
        Bundle args = new Bundle();
        if (name != null)
            args.putString("name", name);
        if (dest != null)
            args.putString("path", dest);
        if (paths != null)
            args.putStringArrayList("paths", paths);
        if (type != null)
            args.putInt("actionType", id);

        ((Activity) mContext).getLoaderManager().restartLoader(id, args, mCallbacks).forceLoad();
    }

    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        Loader<Boolean> loader = null;
        if (mFileActionService != null) {
            int type = args.getInt("actionType", -1);
            if (type > 0) {
                FileActionService.FileAction action = mFileActionService.getFileAction(type);
                if (action != null) {
                    loader = mFileActionService.onCreateLoader(mContext, action, args);
                    if (loader != null && mProgressLayout != null) {
                        switch (action) {
                            case LIST:
                            case RENAME:
                            case DELETE:
                            case CreateFOLDER:
                            case SHARE:
                                mProgressLayout.setVisibility(View.VISIBLE);
                                break;
                            default:
                                mProgressLayout.setVisibility(View.INVISIBLE);
                                break;
                        }
                    }
                }
            }
        }

        return loader;
    }

    public boolean onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (mFileActionService != null) {
            return mFileActionService.onLoadFinished(mContext, mProgressLayout, loader, success);
        }

        return false;
    }

    public void onLoaderReset(Loader<Boolean> loader) {

    }

    public boolean isTopDirectory(String path) {
        String root = getServiceRootPath();
        switch (mFileActionServiceType) {
            case SD:
            case PHONE:
                File base = new File(root);
                File file = new File(path);
                return file.equals(base);
            case SMB:
                return path.equals(root);
            default:
                return path.equals(root);
        }
    }

    public boolean isDownloadDirectory(Context context, String path) {
        String download = NASPref.getDownloadLocation(context);
        File base = new File(download);
        File file = new File(path);
        return file.equals(base);
    }

    public boolean isSubDirectory(String dest, ArrayList<String> paths) {
        for (String path : paths) {
            if (dest.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDirectorySupportFileAction(String path){
        if(isRemoteAction(path) && isTopDirectory(path))
            return false;
        else
            return true;
    }

    public boolean isDirectorySupportUpload(String path) {
        if (isRemoteAction(path) && !isTopDirectory(path))
            return true;
        else
            return false;
    }

    public boolean isRemoteAction(String path) {
        checkServiceType(path);
        String mode = getServiceMode();
        return NASApp.MODE_SMB.equals(mode);
    }

}
