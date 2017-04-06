package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.transcend.nas.service.FileRecentFactory;
import com.transcend.nas.service.FileRecentManager;

import java.util.List;

/**
 * Created by ikelee on 17/3/23.
 */
public class RecentDeleteLoader extends AsyncTaskLoader {

    private static final String TAG = RecentDeleteLoader.class.getSimpleName();

    private int mUserID;
    private List<String> mPaths;

    public RecentDeleteLoader(Context context, List<String> paths) {
        this(context, -1, paths);
    }

    public RecentDeleteLoader(Context context, int userID, List<String> paths) {
        super(context);
        mPaths = paths;
        mUserID = userID;
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

    private boolean delete() {
        if (mUserID >= 0) {
            FileRecentManager.getInstance().deleteAction(mUserID);
        } else {
            FileRecentManager.getInstance().deleteAction();
        }

        //for (String path : mPaths) {
        //    if(isLoadInBackgroundCanceled()) {
        //        Log.d(TAG, "Delete cancel");
        //        break;
        //    }
        //}
        return true;
    }

}
