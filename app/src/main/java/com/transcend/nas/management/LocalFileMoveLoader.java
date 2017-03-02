package com.transcend.nas.management;

import android.content.Context;
import com.transcend.nas.R;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by silverhsu on 16/2/18.
 */
public class LocalFileMoveLoader extends LocalFileCopyLoader {

    private static final String TAG = LocalFileMoveLoader.class.getSimpleName();

    public LocalFileMoveLoader(Context context, List<String> srcs, String dest) {
        super(context, srcs, dest);
        setType(getContext().getString(R.string.move));
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return move();
        } catch (IOException e) {
            e.printStackTrace();
            closeProgressWatcher();
            updateResult(getContext().getString(R.string.error), mDest);
        }
        return false;
    }

    private boolean move() throws IOException {
        for (String path : mSrcs) {
            if (isLoadInBackgroundCanceled())
                return true;

            File source = new File(path);
            if (source.getParent().equals(mDest))
                continue;
            if (source.isDirectory()) {
                moveDirectory(source, mDest);
            } else {
                copyFile(source, mDest);
                deleteFile(source);
            }
            mCurrent++;
        }
        updateResult(getContext().getString(R.string.done), mDest);
        return true;
    }

    private void moveDirectory(File source, String destination) throws IOException {
        String name = createUniqueName(source, destination);
        File target = new File(destination, name);
        boolean isSuccess = target.mkdirs();
        if (!isSuccess) {
            throw new IOException();
        }
        File[] files = source.listFiles();
        for (File file : files) {
            if (!file.isHidden())
                mTotal++;
        }

        String path = target.getPath();
        for (File file : files) {
            if (isLoadInBackgroundCanceled())
                return;

            if (file.isHidden())
                continue;

            if (file.isDirectory()) {
                moveDirectory(file, path);
            } else {
                copyFile(file, path);
                deleteFile(file);
            }
            mCurrent++;
        }
        deleteFile(source);
    }

    private void deleteFile(File file) throws IOException{
        boolean isSuccess = file.delete();
        if (!isSuccess) {
            throw new IOException();
        }
    }
}
