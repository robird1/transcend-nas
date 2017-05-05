package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.management.firmware.ShareFolderManager;
import com.transcend.nas.management.firmware.TwonkyManager;

import java.util.ArrayList;

/**
 * Created by ikelee on 17/2/16.
 */
public class FileShareLinkLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = FileShareLinkLoader.class.getSimpleName();
    private ArrayList<String> mPaths;
    private ArrayList<String> mUrls;
    private ArrayList<String> mAbsolutePaths;

    public FileShareLinkLoader(Context context, ArrayList<String> paths) {
        super(context);
        mPaths = paths;
        mUrls = new ArrayList<>();
        mAbsolutePaths = new ArrayList<>();
    }

    @Override
    public Boolean loadInBackground() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String username = server.getUsername();
        for(String path : mPaths) {
            String realPath = ShareFolderManager.getInstance().getRealPath(path);
            if (path.equals(realPath) && path.startsWith("/" + username + "/"))
                realPath = "/home" + path;

            String url = TwonkyManager.getInstance().getFileUrl(server, realPath);
            if(url != null && !"".equals(url)) {
                Log.d(TAG, "file path : " + realPath  + ", twonky url " + url);
                mAbsolutePaths.add(realPath);
                mUrls.add(url);
            }
        }
        return mUrls.size() > 0;
    }

    public ArrayList<String> getFileShareLinks() {
        return mUrls;
    }

    public ArrayList<String> getFileAbsolutePaths() {
        return mAbsolutePaths;
    }
}
