package com.transcend.nas.management.action.file;

import android.content.Context;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;

/**
 * Created by ike_lee on 2016/12/21.
 */
public class SdcardActionService extends PhoneActionService {
    public SdcardActionService() {
        super();
        TAG = SdcardActionService.class.getSimpleName();
        mMode = NASApp.MODE_SDCARD;
        mRoot = NASApp.ROOT_SD;
        mPath = NASApp.ROOT_SD;
    }

    @Override
    public String getRootPath(Context context) {
        if (null == mRoot || "".equals(mRoot))
            mRoot = NASUtils.getSDLocation(context);
        return mRoot;
    }

}
