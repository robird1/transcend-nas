package com.transcend.nas.management.action.file;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.support.v4.provider.DocumentFile;
import android.widget.RelativeLayout;

import com.transcend.nas.LoaderID;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.management.LocalFileCopyLoader;
import com.transcend.nas.management.LocalFileDeleteLoader;
import com.transcend.nas.management.LocalFileListLoader;
import com.transcend.nas.management.LocalFileMoveLoader;
import com.transcend.nas.management.LocalFileRenameLoader;
import com.transcend.nas.management.upload.LocalFileUploadLoader;
import com.transcend.nas.management.LocalFolderCreateLoader;
import com.transcend.nas.management.externalstorage.ExternalStorageLollipop;
import com.transcend.nas.management.externalstorage.OTGFileCopyLoader;
import com.transcend.nas.management.externalstorage.OTGFileDeleteLoader;
import com.transcend.nas.management.externalstorage.OTGFileMoveLoader;
import com.transcend.nas.management.externalstorage.OTGFileRenameLoader;
import com.transcend.nas.management.externalstorage.OTGLocalFolderCreateLoader;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.transcend.nas.management.action.file.FileActionService.FileAction.*;

/**
 * Created by ike_lee on 2016/12/21.
 */
public class PhoneActionService extends FileActionService {
    public PhoneActionService(){
        TAG = PhoneActionService.class.getSimpleName();
        mMode = NASApp.MODE_STG;
        mRoot = NASApp.ROOT_STG;
        mPath = NASApp.ROOT_STG;
    }

    @Override
    public void initLoaderID(HashMap<FileAction, Integer> ids) {
        ids.put(LIST, LoaderID.LOCAL_FILE_LIST);
        ids.put(UPLOAD, LoaderID.LOCAL_FILE_UPLOAD);
        ids.put(CreateFOLDER, LoaderID.LOCAL_NEW_FOLDER);
        ids.put(RENAME, LoaderID.LOCAL_FILE_RENAME);
        ids.put(COPY, LoaderID.LOCAL_FILE_COPY);
        ids.put(MOVE, LoaderID.LOCAL_FILE_MOVE);
        ids.put(DELETE, LoaderID.LOCAL_FILE_DELETE);
    }

    @Override
    public boolean onLoadFinished(Context context, RelativeLayout progress, Loader<Boolean> loader, Boolean success) {
        return false;
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
        if (isWritePermissionRequired(context, path))
            return new OTGFileRenameLoader(context, new ExternalStorageLollipop(context).getSDFileLocation(path), name);
        return new LocalFileRenameLoader(context, path, name);
    }

    @Override
    protected AsyncTaskLoader copy(Context context, List<String> list, String dest) {
        if (isWritePermissionRequired(context, dest))
            return new OTGFileCopyLoader(context, extractDocumentFiles(mPath, list), new ExternalStorageLollipop(context).getSDFileLocation(dest));
        return new LocalFileCopyLoader(context, list, dest);
    }

    @Override
    protected AsyncTaskLoader move(Context context, List<String> list, String dest) {
        if (list != null && list.size() > 0 && isWritePermissionRequired(context, list.get(0), dest))
            return new OTGFileMoveLoader(context, getSelectedDocumentFiles(context, mPath, list), new ExternalStorageLollipop(context).getDestination(dest));
        return new LocalFileMoveLoader(context, list, dest);
    }

    @Override
    protected AsyncTaskLoader delete(Context context, List<String> list) {
        if (isWritePermissionRequired(context, mPath))
            return new OTGFileDeleteLoader(context, getSelectedDocumentFiles(context, mPath, list));
        return new LocalFileDeleteLoader(context, list);
    }

    @Override
    protected AsyncTaskLoader createFolder(Context context, String path) {
        if (isWritePermissionRequired(context, mPath)) {
            String name = FilenameUtils.getName(path);
            return new OTGLocalFolderCreateLoader(context, new ExternalStorageLollipop(context).getSDFileLocation(mPath), name);
        }

        return new LocalFolderCreateLoader(context, path);
    }

    @Override
    protected AsyncTaskLoader share(Context context, ArrayList<String> paths, String dest) {
        return null;
    }

    @Override
    protected AsyncTaskLoader shareLink(Context context, ArrayList<String> paths) {
        return null;
    }

    private ArrayList<DocumentFile> getSelectedDocumentFiles(Context context, String path, List<String> list) {
        ArrayList<DocumentFile> files = new ArrayList<>();
        if (NASUtils.isSDCardPath(context, path)) {
            DocumentFile pickedDir = new ExternalStorageLollipop(context).getSDFileLocation(path);
            for (String name : list) {
                files.add(pickedDir.findFile(FilenameUtils.getName(name)));
            }
        } else {
            files = extractDocumentFiles(path, list);
        }
        return files;
    }

    private ArrayList<DocumentFile> extractDocumentFiles(String path, List<String> selectedFileNames) {
        ArrayList<DocumentFile> sourceFiles = new ArrayList<>();
        File file = new File(path);

        if (file.exists()) {
            DocumentFile document = DocumentFile.fromFile(file);
            for (String s : selectedFileNames) {
                DocumentFile d = document.findFile(FilenameUtils.getName(s));
                sourceFiles.add(d);
            }
        }
        return sourceFiles;
    }
}
