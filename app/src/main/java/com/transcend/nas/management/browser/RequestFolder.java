package com.transcend.nas.management.browser;

import android.os.Bundle;

import com.transcend.nas.management.FileInfo;

/**
 * Created by steve_su on 2017/9/20.
 */

public class RequestFolder extends RequestAction {

    RequestFolder(BrowserActivity activity) {
        super(activity);
    }

    @Override
    public void onRecyclerItemClick(FileInfo fileInfo) {

    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void refresh(boolean showProgress) {
        mActivity.doRefresh();
    }

    @Override
    public void search(String text) {
        Bundle args = new Bundle();
        args.putString("keyword", text);
        startLoader(SMB_SEARCH, args);
    }

}
