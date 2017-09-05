package com.transcend.nas.management.browser;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.transcend.nas.management.FileManageDropdownAdapter;

import java.net.URLDecoder;
import java.util.ArrayList;

import static com.transcend.nas.management.browser.RequestAction.PATH_TOKEN;
import static com.transcend.nas.management.browser.RequestAction.getAPIArgs;

/**
 * Created by steve_su on 2017/8/18.
 */

public class BrowserDropdownAdapter extends FileManageDropdownAdapter {
    private static final String TAG = BrowserDropdownAdapter.class.getSimpleName();

    public BrowserDropdownAdapter(Context context) {
        super(context);
        mDisplayList = new ArrayList<>();
    }

    /**
     * Called after BrowserFragment.onLoadFinished() been invoked.
     *
     * @param path
     */
    @Override
    public void updateList(String path, String mode) {
        Log.d(TAG, "[Enter] updateList(Before) path: "+ path);
        String apiName = RequestAction.getAPIName(path);

        if (TextUtils.isEmpty(apiName)) {
            return;
        }

        mList.clear();
        mDisplayList.clear();

        String text = "";
        if ("view_all".equals(apiName)) {
            int type = Integer.valueOf(getAPIArgs(path));
            if (2 == type) {
                text = "View all photos";

            } else if (1 == type) {
                text = "View all tracks";

            } else if (3 == type) {
                text = "View all videos";
            }

        } else if ("get_photo_years".equals(apiName)) {
            text = "View by date";

        } else if ("get_photo_album".equals(apiName)) {
            text = "View by folder";

        } else if ("get_music_artists".equals(apiName)) {
            text = "View by artist";

        } else if ("get_music_genre".equals(apiName)) {
            text = "View by genre";

        } else if ("get_music_album".equals(apiName)) {
            String folderName = getSelectedFolder(path);
            if (folderName != null) {
                text = folderName;
            } else {
                text = "View by album";
            }

        } else if ("get_video_album".equals(apiName)) {
            text = "View by folder";

        } else if (apiName.startsWith("get_photo_months")) {
            text = getSelectedFolder(path);

        } else if (apiName.startsWith("get_photo") || apiName.startsWith("get_music") || apiName.startsWith("get_video")) {
            text = getSelectedFolder(path);
            if (path.contains("get_photo_months")) {
                text = TwonkyIndexLoader.mMonthMap.get(text);
            }
        }

        mList.add(text);
        mDisplayList.add(text);

    }

//    /**
//     * Called after onDropdownItemSelected() been invoked.
//     *
//     * @param position
//     * @return
//     */
//    @Override
//    public String getPath(int position) {
//        return null;
//    }

    /**
     * path example:  ||get_photo_years||get_photo_months?year=2016
     * path example:  ||get_photo_album||get_photo?folder=/home/Public/test5/
     *
     * @param path
     * @return
     */
    private String getSelectedFolder(String path) {
        Log.d(TAG, "[Enter] getSelectedFolder path: "+ path);
        if (path == null) {
            return null;
        }
        String folder = null;
        if (!path.contains("?folder=")) {
            // path example:  ||get_photo_years||get_photo_months?year=2016
            int tempIndex = path.lastIndexOf("?");
            if (tempIndex != -1) {
                String tmpString = path.substring(tempIndex + "?".length());
                Log.d(TAG, "tmpString: "+ tmpString);
                int tempIndex2 = tmpString.lastIndexOf("=");
                if (tempIndex2 != -1) {
                    folder = tmpString.substring(tempIndex2 + "=".length());
                }
            }
        } else {
            // path example:  ||get_photo_album||get_photo?folder=%2Fhome%2FPublic%2Ftest5%2F

            int tempIndex = path.lastIndexOf("%2F");
            if (tempIndex != -1) {
                String tempString = path.substring(0, tempIndex);
                tempIndex = tempString.lastIndexOf("%2F");
                if (tempIndex != -1) {
                    folder = tempString.substring(tempIndex + "%2F".length());
                }
            }
        }

        if (folder != null) {
            folder = URLDecoder.decode(folder);
        }
        Log.d(TAG, "folder: "+ folder);
        return folder;

    }

}