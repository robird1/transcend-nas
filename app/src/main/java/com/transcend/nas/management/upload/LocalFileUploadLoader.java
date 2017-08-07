package com.transcend.nas.management.upload;

import android.content.Context;

import com.transcend.nas.management.upload.FileUploadLoader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by silverhsu on 16/2/22.
 */
public class LocalFileUploadLoader extends FileUploadLoader {

    private boolean mIsOpenWithUpload = false;

    public LocalFileUploadLoader(Context context, List<String> srcs, String dest) {
        super(context, srcs, dest);
    }

    public LocalFileUploadLoader(Context context, List<String> srcs, String dest, boolean isOpenWithUpload) {
        this(context, srcs, dest);
        mIsOpenWithUpload = isOpenWithUpload;
    }

    public boolean isOpenWithUpload() {
        return mIsOpenWithUpload;
    }

    private File convertObject(Object src) {
        try {
            return new File(src.toString());
        } catch (ClassCastException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected boolean isDirectory(Object src) {
        File file = convertObject(src);
        if (file != null)
            return file.isDirectory();
        return false;
    }

    @Override
    protected List listDirectory(Object src) {
        File file = convertObject(src);
        if (file != null) {
            List<String> list = new ArrayList<>();
            File[] files = file.listFiles();
            for (File tmp : files) {
                if (!tmp.isHidden())
                    list.add(tmp.getPath());
            }
            return list;
        }

        return null;
    }

    @Override
    protected String parserFileName(Object src) {
        File file = convertObject(src);
        if (file != null)
            return file.getName();
        return null;
    }

    @Override
    protected InputStream createInputStream(Object src) {
        File file = convertObject(src);
        if (file != null) {
            try {
                return new BufferedInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
