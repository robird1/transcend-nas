package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.v4.provider.DocumentFile;

/**
 * Created by steve_su on 2016/12/27.
 */

public class OTGFileRenameLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = OTGFileRenameLoader.class.getSimpleName();
    private DocumentFile mSelectDocumentFile;
    private String mName;

    public OTGFileRenameLoader(Context context, DocumentFile selectDocumentFile, String name) {
        super(context);
        mSelectDocumentFile = selectDocumentFile;
        mName = name;
    }

    @Override
    public Boolean loadInBackground() {
        return rename();
    }

    private boolean rename() {

        if (mSelectDocumentFile.exists())
            return mSelectDocumentFile.renameTo(mName);
        return false;
    }
}
