package com.transcend.nas.management.externalstorage;

import android.content.Context;
import android.support.v4.provider.DocumentFile;

import com.transcend.nas.management.LocalAbstractLoader;

/**
 * Created by steve_su on 2017/1/6.
 */

public class OTGLocalFolderCreateLoader extends LocalAbstractLoader {
    private static final String TAG = OTGLocalFolderCreateLoader.class.getSimpleName();
    private String mName;
    private DocumentFile mDocumentFile;

    public OTGLocalFolderCreateLoader(Context context, DocumentFile documentFile, String name) {
        super(context);
        mName = name;
        mDocumentFile = documentFile;
    }

    @Override
    public Boolean loadInBackground() {
        return createNewFolder();
    }

    private boolean createNewFolder() {

        if (mDocumentFile.findFile(mName) == null) {
            mDocumentFile.createDirectory(mName);
            return true;
        }
        return false;
    }
}
