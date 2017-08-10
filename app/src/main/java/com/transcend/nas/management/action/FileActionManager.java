package com.transcend.nas.management.action;

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
import com.transcend.nas.management.action.file.FileActionService;
import com.transcend.nas.management.action.file.PhoneActionService;
import com.transcend.nas.management.action.file.RecentActionService;
import com.transcend.nas.management.action.file.SdcardActionService;
import com.transcend.nas.management.action.file.SmbFileActionService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ike_lee on 2016/12/21.
 */
public class FileActionManager extends AbstractActionManager {
    private static String TAG = FileActionManager.class.getSimpleName();
    private FileActionService mFileActionService;
    private Map<FileActionServiceType, FileActionService> mFileActionServicePool;
    private FileActionServiceType mFileActionServiceType;
    private boolean isLockType = false;

    public enum FileActionServiceType {
        PHONE, SD, SMB, RECENT
    }

    public FileActionManager(Context context, FileActionServiceType service, LoaderManager.LoaderCallbacks<Boolean> callbacks) {
        this(context, service, callbacks, null);
    }

    public FileActionManager(Context context, FileActionServiceType service, LoaderManager.LoaderCallbacks<Boolean> callbacks, RelativeLayout progressLayout) {
        super(context, callbacks, progressLayout);
        mFileActionServiceType = service;
        setServiceType(service);
        isLockType = false;
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
                case RECENT:
                    service = new RecentActionService();
                    break;
            }
            mFileActionServicePool.put(type, service);
        }

        mFileActionServiceType = type;
        mFileActionService = service;
    }

    public void setServiceType(String path) {
        if (isLockType)
            return;

        if (path.startsWith("/storage")) {
            if (NASUtils.isSDCardPath(getContext(), path))
                setServiceType(FileActionManager.FileActionServiceType.SD);
            else
                setServiceType(FileActionManager.FileActionServiceType.PHONE);
        } else {
            if (path.startsWith(NASApp.ROOT_RECENT))
                setServiceType(FileActionManager.FileActionServiceType.RECENT);
            else
                setServiceType(FileActionManager.FileActionServiceType.SMB);
        }
    }

    public String getServiceRootPath() {
        String root = NASApp.ROOT_SMB;
        if (mFileActionService != null)
            root = mFileActionService.getRootPath(getContext());
        return root;
    }

    public String getServiceMode() {
        String mode = NASApp.MODE_SMB;
        if (mFileActionService != null)
            mode = mFileActionService.getMode(getContext());
        return mode;
    }

    public FileActionService getFileActionService() {
        return mFileActionService;
    }

    public void setCurrentPath(String path) {
        if (mFileActionService != null)
            mFileActionService.setCurrentPath(path);
    }

    public void open(String path) {
        createLoader(FileActionService.FileAction.OPEN, null, path, null);
        Log.w(TAG, "doOpen: " + path);
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
            NASUtils.shareLocalFile(getContext(), files);
        }
    }

    public void shareLink(ArrayList<FileInfo> files) {
        ArrayList<String> paths = new ArrayList<String>();
        for (FileInfo file : files) {
            paths.add(file.path);
        }
        createLoader(FileActionService.FileAction.ShareLINK, null, null, paths);
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

        createLoader(id, args);
    }

    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        Loader<Boolean> loader = null;
        if (mFileActionService != null) {
            int type = args.getInt("actionType", -1);
            if (type > 0) {
                FileActionService.FileAction action = mFileActionService.getFileAction(type);
                if (action != null) {
                    loader = mFileActionService.onCreateLoader(getContext(), action, args);
                    if (loader != null) {
                        switch (action) {
                            case LIST:
                            case RENAME:
                            case DELETE:
                            case CreateFOLDER:
                            case SHARE:
                            case ShareLINK:
                            case OPEN:
                                showProgress();
                                break;
                            default:
                                hideProgress();
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
            return mFileActionService.onLoadFinished(getContext(), getProgressLayout(), loader, success);
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
            case RECENT:
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

    public boolean isDirectorySupportFileAction(String path) {
        if (isRemoteAction(path) && isTopDirectory(path))
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
        setServiceType(path);
        return isRemoteAction();
    }

    public boolean isRemoteAction() {
        String mode = getServiceMode();
        return NASApp.MODE_SMB.equals(mode) || NASApp.MODE_RECENT.equals(mode);
    }

    public void doLockActionType() {
        isLockType = true;
    }

    public void doUnLockActionType() {
        isLockType = false;
    }
}
