package com.transcend.nas.management.fileaction;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.view.View;
import android.widget.RelativeLayout;

import com.transcend.nas.LoaderID;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.management.FileDownloadLoader;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileShareLinkLoader;
import com.transcend.nas.management.LocalFileUploadLoader;
import com.transcend.nas.management.SmbFileCopyLoader;
import com.transcend.nas.management.SmbFileDeleteLoader;
import com.transcend.nas.management.SmbFileListLoader;
import com.transcend.nas.management.SmbFileMoveLoader;
import com.transcend.nas.management.SmbFileRenameLoader;
import com.transcend.nas.management.SmbFileShareLoader;
import com.transcend.nas.management.SmbFolderCreateLoader;
import com.transcend.nas.management.externalstorage.ExternalStorageLollipop;
import com.transcend.nas.management.externalstorage.OTGFileDownloadLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.transcend.nas.management.fileaction.FileActionService.FileAction.*;

/**
 * Created by ike_lee on 2016/12/21.
 */
class SmbFileActionService extends FileActionService {
    public SmbFileActionService() {
        TAG = SmbFileActionService.class.getSimpleName();
        mMode = NASApp.MODE_SMB;
        mRoot = NASApp.ROOT_SMB;
        mPath = NASApp.ROOT_SMB;
    }

    @Override
    public void initLoaderID(HashMap<FileAction, Integer> ids) {
        ids.put(OPEN, LoaderID.SMB_FILE_CHECK);
        ids.put(LIST, LoaderID.SMB_FILE_LIST);
        ids.put(DOWNLOAD, LoaderID.FILE_DOWNLOAD);
        ids.put(UPLOAD, LoaderID.LOCAL_FILE_UPLOAD);
        ids.put(CreateFOLDER, LoaderID.SMB_NEW_FOLDER);
        ids.put(RENAME, LoaderID.SMB_FILE_RENAME);
        ids.put(COPY, LoaderID.SMB_FILE_COPY);
        ids.put(MOVE, LoaderID.SMB_FILE_MOVE);
        ids.put(DELETE, LoaderID.SMB_FILE_DELETE);
        ids.put(SHARE, LoaderID.SMB_FILE_SHARE);
        ids.put(ShareLINK, LoaderID.FILE_SHARE_LINK);
    }

    @Override
    public boolean onLoadFinished(Context context, RelativeLayout progress, Loader<Boolean> loader, Boolean success) {
        if (loader instanceof SmbFileShareLoader) {
            ArrayList<FileInfo> shareList = ((SmbFileShareLoader) loader).getShareList();
            NASUtils.shareLocalFile(context, shareList);
            if (progress != null)
                progress.setVisibility(View.INVISIBLE);
            return true;
        } else if (loader instanceof FileShareLinkLoader && success) {
            String uuid = NASPref.getCloudUUID(context);
            ArrayList<String> urls = ((FileShareLinkLoader) loader).getFileShareLinks();
            ArrayList<String> absolutePaths = ((FileShareLinkLoader) loader).getFileAbsolutePaths();
            //TODO : open third-party app to delivery message

            NASUtils.sendFileSharedLink(context, ((FileShareLinkLoader) loader));

            if (progress != null)
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
        if (isWritePermissionRequired(context, dest))
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

    @Override
    protected AsyncTaskLoader shareLink(Context context, ArrayList<String> paths) {
        return new FileShareLinkLoader(context, paths);
    }
}
