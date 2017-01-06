package com.transcend.nas.management.externalstorage;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.v4.provider.DocumentFile;

import java.net.MalformedURLException;
import java.util.ArrayList;

/**
 * Created by steve_su on 2016/12/28.
 */

public class OTGFileDeleteLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = OTGFileDeleteLoader.class.getSimpleName();
    private ArrayList<DocumentFile> mDeleteFileList;

    public OTGFileDeleteLoader(Context context, ArrayList<DocumentFile> deleteDocumentFileList) {
        super(context);
        mDeleteFileList = deleteDocumentFileList;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean delete() throws MalformedURLException {
        for (DocumentFile file : mDeleteFileList) {
            file.delete();
        }
        return true;
    }
}
