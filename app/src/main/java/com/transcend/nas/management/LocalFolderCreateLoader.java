package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import java.io.File;

/**
 * Created by silverhsu on 16/1/21.
 */
public class LocalFolderCreateLoader extends LocalAbstractLoader {

    private static final String TAG = LocalFolderCreateLoader.class.getSimpleName();
    private String mPath;

    public LocalFolderCreateLoader(Context context, String path) {
        super(context);
        mPath = path;
    }

    @Override
    public Boolean loadInBackground() {
        return createNewFolder();
    }

    private boolean createNewFolder() {
        File dir = new File(mPath);
        if (!dir.exists()) {
            boolean b = dir.mkdirs();
            return true;
        }
        return false;
    }

}
