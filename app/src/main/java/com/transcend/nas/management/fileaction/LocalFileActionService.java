package com.transcend.nas.management.fileaction;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;

import com.transcend.nas.LoaderID;
import com.transcend.nas.management.LocalFileCopyLoader;
import com.transcend.nas.management.LocalFileDeleteLoader;
import com.transcend.nas.management.LocalFileListLoader;
import com.transcend.nas.management.LocalFileMoveLoader;
import com.transcend.nas.management.LocalFileRenameLoader;
import com.transcend.nas.management.LocalFileUploadLoader;
import com.transcend.nas.management.LocalFolderCreateLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ike_lee on 2016/12/21.
 */
class LocalFileActionService extends FileActionService {
    public LocalFileActionService(){
        TAG = SmbFileActionService.class.getSimpleName();
        LIST = LoaderID.LOCAL_FILE_LIST;
        UPLOAD = LoaderID.LOCAL_FILE_UPLOAD;
        CreateFOLDER = LoaderID.LOCAL_NEW_FOLDER;
        RENAME = LoaderID.LOCAL_FILE_RENAME;
        COPY = LoaderID.LOCAL_FILE_COPY;
        MOVE = LoaderID.LOCAL_FILE_MOVE;
        DELETE = LoaderID.LOCAL_FILE_DELETE;
        SHARE = LoaderID.LOCAL_FILE_SHARE;
    }

    @Override
    public void onLoadFinished(Context context, Loader<Boolean> loader, Boolean success) {

    }

    @Override
    protected AsyncTaskLoader open(Context context, String path) {
        return null;
    }

    @Override
    protected AsyncTaskLoader list(Context context, String path) {
        return new LocalFileListLoader(context, path);
    }

    @Override
    protected AsyncTaskLoader download(Context context, List<String> list, String dest) {
        return null;
    }

    @Override
    protected AsyncTaskLoader upload(Context context, List<String> list, String dest) {
        return new LocalFileUploadLoader(context, list, dest);
    }

    @Override
    protected AsyncTaskLoader rename(Context context, String path, String name) {
        return new LocalFileRenameLoader(context, path, name);
    }

    @Override
    protected AsyncTaskLoader copy(Context context, List<String> list, String dest) {
        return new LocalFileCopyLoader(context, list, dest);
    }

    @Override
    protected AsyncTaskLoader move(Context context, List<String> list, String dest) {
        return new LocalFileMoveLoader(context, list, dest);
    }

    @Override
    protected AsyncTaskLoader delete(Context context, List<String> list) {
        return new LocalFileDeleteLoader(context, list);
    }

    @Override
    protected AsyncTaskLoader createFolder(Context context, String path) {
        return new LocalFolderCreateLoader(context, path);
    }

    @Override
    protected AsyncTaskLoader share(Context context, ArrayList<String> paths, String dest) {
        return null;
    }
}
