package com.transcend.nas.management.action;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.transcend.nas.LoaderID;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.upload.LocalFileUploadLoader;
import com.transcend.nas.management.SmbFileDeleteLoader;
import com.transcend.nas.management.SmbFileListLoader;
import com.transcend.nas.management.SmbFileRenameLoader;
import com.transcend.nas.viewer.document.OpenWithUploadHandler;

import java.util.ArrayList;

/**
 * Created by ike_lee on 2016/12/21.
 */
public class FileSyncManager extends AbstractActionManager {
    private static String TAG = FileSyncManager.class.getSimpleName();
    private OpenWithUploadHandler mOpenWithUploadHandler;

    public FileSyncManager(Context context, LoaderManager.LoaderCallbacks callbacks) {
        this(context, callbacks, null);
    }

    public FileSyncManager(Context context, LoaderManager.LoaderCallbacks callbacks, RelativeLayout progressLayout) {
        super(context, callbacks, progressLayout);
    }

    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        ArrayList<String> paths;
        String path, name;
        switch (id) {
            case LoaderID.LOCAL_FILE_UPLOAD_OPEN_WITH:
                showProgress();
                paths = args.getStringArrayList("paths");
                path = args.getString("path");
                return new LocalFileUploadLoader(getContext(), paths, path, true);
            case LoaderID.SMB_FILE_DELETE_AFTER_UPLOAD:
                showProgress();
                paths = args.getStringArrayList("paths");
                return new SmbFileDeleteLoader(getContext(), paths, true);
            case LoaderID.SMB_FILE_RENAME:
                showProgress();
                path = args.getString("path");
                name = args.getString("name");
                return new SmbFileRenameLoader(getContext(), path, name);
            default:
                return null;
        }
    }

    public boolean onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (success) {
            if ((loader instanceof LocalFileUploadLoader)) {
                LocalFileUploadLoader uploadLoader = ((LocalFileUploadLoader) loader);
                if (uploadLoader.isOpenWithUpload()) {
                    String fileName = uploadLoader.getUniqueFileName();
                    mOpenWithUploadHandler.setTempFilePath(mOpenWithUploadHandler.getRemoteFileDirPath().concat(fileName));
                    ArrayList pathList = new ArrayList();
                    pathList.add(mOpenWithUploadHandler.getSelectedFile().path);
                    Bundle args = new Bundle();
                    args.putStringArrayList("paths", pathList);
                    return createLoader(LoaderID.SMB_FILE_DELETE_AFTER_UPLOAD, args);
                }
            } else if ((loader instanceof SmbFileDeleteLoader)) {
                SmbFileDeleteLoader deleteLoader = ((SmbFileDeleteLoader) loader);
                if (deleteLoader.isDeleteAfterUpload()) {
                    Bundle args = new Bundle();
                    args.putString("path", mOpenWithUploadHandler.getTempFilePath());
                    args.putString("name", mOpenWithUploadHandler.getSelectedFile().name);
                    return createLoader(LoaderID.SMB_FILE_RENAME, args);
                }
            }
        }

        return false;
    }

    public void onLoaderReset(Loader<Boolean> loader) {
    }

    public void doOpenWithUpload(FileManageActivity activity, FileInfo fileInfo, String downloadFilePath, SmbFileListLoader loader) {
        mOpenWithUploadHandler = new OpenWithUploadHandler(activity, fileInfo, downloadFilePath, loader);
        mOpenWithUploadHandler.showDialog();
    }
}
