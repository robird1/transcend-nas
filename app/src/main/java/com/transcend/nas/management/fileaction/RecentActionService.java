package com.transcend.nas.management.fileaction;

import android.content.AsyncTaskLoader;
import android.content.Context;

import com.transcend.nas.LoaderID;
import com.transcend.nas.NASApp;
import com.transcend.nas.management.RecentCheckLoader;
import com.transcend.nas.management.RecentDeleteLoader;
import com.transcend.nas.management.RecentListLoader;

import java.util.List;

/**
 * Created by ike_lee on 2016/12/21.
 */
class RecentActionService extends SmbFileActionService {
    public RecentActionService() {
        super();
        TAG = RecentActionService.class.getSimpleName();
        OPEN = LoaderID.SMB_FILE_CHECK;
        mMode = NASApp.MODE_RECENT;
        mRoot = NASApp.ROOT_RECENT;
        mPath = NASApp.ROOT_RECENT;
    }

    @Override
    protected AsyncTaskLoader open(Context context, String path) {
        return new RecentCheckLoader(context, path);
    }

    @Override
    protected AsyncTaskLoader list(Context context, String path) {
        return list(context, -1, path);
    }

    protected AsyncTaskLoader list(Context context, int userID, String path) {
        return new RecentListLoader(context, userID, path);
    }

    @Override
    protected AsyncTaskLoader delete(Context context, List<String> list) {
        return new RecentDeleteLoader(context, list);
    }
}
