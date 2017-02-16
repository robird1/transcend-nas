package com.transcend.nas.management;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.transcend.nas.NASPref;
import com.transcend.nas.common.HttpRequestFactory;
import com.transcend.nas.management.firmware.MediaFactory;

import java.util.Map;

/**
 * Created by ikelee on 17/2/16.
 */
public class FileInfoLoader extends SmbAbstractLoader {

    private static final String TAG = FileInfoLoader.class.getSimpleName();
    private String mPath;

    public FileInfoLoader(Context context, String path) {
        super(context);
        mPath = path;
    }

    @Override
    public Boolean loadInBackground() {
        Uri uri = MediaFactory.createUri(getContext(), mPath);
        if(uri != null) {
            String url = uri.toString();
            url = url + "&login=" + NASPref.getUsername(getContext()) + "&exif";
            Map<String, String> result = HttpRequestFactory.doHeadRequest(url);
            if(result != null && result.size() > 0) {
                for(String key : result.keySet()) {
                    Log.d(TAG, "key : " + key + ", value : " + result.get(key));
                }
                return true;
            }
        }

        return false;
    }
}
