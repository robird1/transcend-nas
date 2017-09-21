package com.transcend.nas.management.browser;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.transcend.nas.management.FileInfo;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/9/20.
 */

public class SambaSearchLoader extends AsyncTaskLoader<Boolean> {
    private String mKeyword;
    private ArrayList<FileInfo> mFileList;
    private ArrayList<FileInfo> mResultList;

    public SambaSearchLoader(Context context, Bundle args, ArrayList<FileInfo> list) {
        super(context);
        mKeyword = args.getString("keyword");
        mFileList = list;
    }

    @Override
    public Boolean loadInBackground() {
        if (TextUtils.isEmpty(mKeyword)) {
            return true;
        }

        mKeyword = mKeyword.toLowerCase();
        mResultList = new ArrayList<>();
        for (FileInfo file : mFileList) {
            if (file.name.toLowerCase().contains(mKeyword)) {
                mResultList.add(file);
            }
        }

        return true;
    }

    public ArrayList<FileInfo> getFileList() {
        return mResultList;
    }

}
