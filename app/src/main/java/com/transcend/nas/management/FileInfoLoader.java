package com.transcend.nas.management;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.common.HttpRequestFactory;
import com.transcend.nas.management.firmware.MediaFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ikelee on 17/2/16.
 */
public class FileInfoLoader extends SmbAbstractLoader {

    private static final String TAG = FileInfoLoader.class.getSimpleName();
    private String mPath;
    private Map<String, String> mResult;

    public FileInfoLoader(Context context, String path) {
        super(context);
        mPath = path;
        mResult = new HashMap<>();
    }

    @Override
    public Boolean loadInBackground() {
        if (mPath.startsWith(NASApp.ROOT_STG) || NASUtils.isSDCardPath(getContext(), mPath)) {
            //TODO : get local file information
        } else {
            Uri uri = MediaFactory.createUri(getContext(), mPath);
            if (uri != null) {
                String url = uri.toString();
                url = url + "&login=" + NASPref.getUsername(getContext()) + "&exif";
                Map<String, String> result = HttpRequestFactory.doHeadRequest(url);
                if (result != null && result.size() > 0) {
                    for (String key : result.keySet()) {
                        //Log.d(TAG, "key : " + key + ", value : " + result.get(key));
                        if (key != null && !"".equals(key) && !key.contains("Android"))
                            mResult.put(key, result.get(key));
                    }

                    mResult.remove("Content-Length");
                    mResult.remove("Last-Modified");
                    return true;
                }
            }
        }
        return false;
    }

    public Map<String, String> getFileInfo() {
        return mResult;
    }
}
