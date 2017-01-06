package com.transcend.nas.management;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by silverhsu on 16/2/1.
 */
public class LocalFileDeleteLoader extends LocalAbstractLoader {

    private static final String TAG = LocalFileDeleteLoader.class.getSimpleName();
    private List<String> mPaths;

    public LocalFileDeleteLoader(Context context, List<String> paths) {
        super(context);
        mPaths = paths;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return delete();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean delete() throws IOException {
        boolean isSuccess;
        for (String path : mPaths) {
            File target = new File(path);
            if (target.isDirectory())
                isSuccess = deleteDirectory(target);
            else
                isSuccess = target.delete();

            if (!isSuccess) {
                throw new IOException();
            }
        }
        return true;
    }

    private boolean deleteDirectory(File dir) {
        for (File target : dir.listFiles()) {
            if (target.isDirectory())
                deleteDirectory(target);
            else
                target.delete();
        }
        return dir.delete();
    }

}
