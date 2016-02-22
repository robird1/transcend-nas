package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;

import java.io.File;

/**
 * Created by silverhsu on 16/1/30.
 */
public class LocalFileRenameLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = LocalFileRenameLoader.class.getSimpleName();

    private String mPath;
    private String mName;

    public LocalFileRenameLoader(Context context, String path, String name) {
        super(context);
        mPath = path;
        mName = name;
    }

    @Override
    public Boolean loadInBackground() {
        return rename();
    }

    private boolean rename() {
        File target = new File(mPath);
        File parent = target.getParentFile();
        File rename = new File(parent, mName);
        if (target.exists())
            return target.renameTo(rename);
        return false;
    }
}
