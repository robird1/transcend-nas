package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by silverhsu on 16/2/1.
 */
public class LocalFileDeleteLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = LocalFileDeleteLoader.class.getSimpleName();

    private List<String> mPaths;

    public LocalFileDeleteLoader(Context context, List<String> paths) {
        super(context);
        mPaths = paths;
    }

    @Override
    public Boolean loadInBackground() {
        return delete();
    }

    private boolean delete() {
        for (String path : mPaths) {
            File target = new File(path);
            if (target.isDirectory())
                deleteDirectory(target);
            else
                target.delete();
        }
        return true;
    }

    private void deleteDirectory(File dir) {
        for (File target : dir.listFiles()) {
            if (target.isDirectory())
                deleteDirectory(target);
            else
                target.delete();
        }
        dir.delete();
    }

}
