package com.transcend.nas.management.externalstorage;

import android.content.Context;
import android.support.v4.provider.DocumentFile;

import com.transcend.nas.R;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by steve_su on 2016/12/30.
 */

public class OTGFileMoveLoader extends OTGFileCopyLoader {

    private static final String TAG = OTGFileMoveLoader.class.getSimpleName();

    public OTGFileMoveLoader(Context context, ArrayList<DocumentFile> src, DocumentFile des) {
        super(context, src, des);
        setType(getContext().getString(R.string.move));
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return move();
        } catch (IOException e) {
            e.printStackTrace();
            closeProgressWatcher();
            updateResult(getContext().getString(R.string.error), DocumentFileHelper.getPath(mActivity, mDesDocumentFile.getUri()));
        }
        return false;
    }

    private boolean move() throws IOException {
        for (DocumentFile file : mSrcDocumentFileList) {
            if (isLoadInBackgroundCanceled())
                return true;

            if (file.isDirectory()) {
                moveDirectoryTask(file, mDesDocumentFile);
            } else {
                copyFileTask(mActivity, file, mDesDocumentFile);
                file.delete();
            }
            mCurrent++;
        }
        updateResult(getContext().getString(R.string.done), DocumentFileHelper.getPath(mActivity, mDesDocumentFile.getUri()));
        return true;
    }

    private void moveDirectoryTask(DocumentFile srcFileItem, DocumentFile destFileItem) throws IOException {
        DocumentFile destDirectory = destFileItem.createDirectory(createUniqueName(srcFileItem, destFileItem));
        DocumentFile[] files = srcFileItem.listFiles();
        mTotal += files.length;

        for (DocumentFile file : files) {
            if (isLoadInBackgroundCanceled())
                return;

            if (file.isDirectory()) {
                moveDirectoryTask(file, destDirectory);
            } else {//is file
                copyFileTask(mActivity, file, destDirectory);
                file.delete();
            }
            mCurrent++;
        }
        srcFileItem.delete();
    }
}

