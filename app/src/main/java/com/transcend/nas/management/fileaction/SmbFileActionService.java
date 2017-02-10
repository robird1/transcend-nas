package com.transcend.nas.management.fileaction;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.transcend.nas.LoaderID;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.management.FileDownloadLoader;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.LocalFileUploadLoader;
import com.transcend.nas.management.SmbFileCopyLoader;
import com.transcend.nas.management.SmbFileDeleteLoader;
import com.transcend.nas.management.SmbFileDownloadLoader;
import com.transcend.nas.management.SmbFileListLoader;
import com.transcend.nas.management.SmbFileMoveLoader;
import com.transcend.nas.management.SmbFileRenameLoader;
import com.transcend.nas.management.SmbFileShareLoader;
import com.transcend.nas.management.SmbFolderCreateLoader;
import com.transcend.nas.management.externalstorage.ExternalStorageLollipop;
import com.transcend.nas.management.externalstorage.OTGFileDownloadLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ike_lee on 2016/12/21.
 */
class SmbFileActionService extends FileActionService {
    public SmbFileActionService(){
        TAG = SmbFileActionService.class.getSimpleName();
        LIST = LoaderID.SMB_FILE_LIST;
        DOWNLOAD = LoaderID.FILE_DOWNLOAD;
        CreateFOLDER = LoaderID.SMB_NEW_FOLDER;
        RENAME = LoaderID.SMB_FILE_RENAME;
        COPY = LoaderID.SMB_FILE_COPY;
        MOVE = LoaderID.SMB_FILE_MOVE;
        DELETE = LoaderID.SMB_FILE_DELETE;
        SHARE = LoaderID.SMB_FILE_SHARE;
        mMode = NASApp.MODE_SMB;
        mRoot = NASApp.ROOT_SMB;
        mPath = NASApp.ROOT_SMB;
    }

    @Override
    public boolean onLoadFinished(Context context, RelativeLayout progress, Loader<Boolean> loader, Boolean success) {
        if(loader instanceof SmbFileShareLoader) {
            ArrayList<FileInfo> shareList = ((SmbFileShareLoader) loader).getShareList();
            NASUtils.shareLocalFile(context, shareList);
            if(progress != null)
                progress.setVisibility(View.INVISIBLE);
            return true;
        }
        return false;
    }

    @Override
    protected AsyncTaskLoader open(Context context, String path) {
        return null;
    }

    @Override
    protected AsyncTaskLoader list(Context context, String path) {
        return new SmbFileListLoader(context, path);
    }

    @Override
    protected AsyncTaskLoader download(Context context, List<String> list, String dest) {
        if(isWritePermissionRequired(context, dest))
            return new OTGFileDownloadLoader(context, list, dest, new ExternalStorageLollipop(context).getSDFileLocation(dest));
        return new FileDownloadLoader(context, list, dest);
    }

    @Override
    protected AsyncTaskLoader upload(Context context, List<String> list, String dest) {
        return new LocalFileUploadLoader(context, list, dest);
    }

    @Override
    protected AsyncTaskLoader rename(Context context, String path, String name) {
        return new SmbFileRenameLoader(context, path, name);
    }

    @Override
    protected AsyncTaskLoader copy(Context context, List<String> list, String dest) {
        return new SmbFileCopyLoader(context, list, dest);
    }

    @Override
    protected AsyncTaskLoader move(Context context, List<String> list, String dest) {
        return new SmbFileMoveLoader(context, list, dest);
    }

    @Override
    protected AsyncTaskLoader delete(Context context, List<String> list) {
        return new SmbFileDeleteLoader(context, list);
    }

    @Override
    protected AsyncTaskLoader createFolder(Context context, String path) {
        return new SmbFolderCreateLoader(context, path);
    }

    @Override
    protected AsyncTaskLoader share(Context context, ArrayList<String> paths, String dest) {
        return new SmbFileShareLoader(context, paths, dest);
    }
}
