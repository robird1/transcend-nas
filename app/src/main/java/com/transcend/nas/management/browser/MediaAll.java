package com.transcend.nas.management.browser;

import android.content.Context;
import android.view.Menu;

import com.transcend.nas.R;
import com.transcend.nas.management.action.FileActionManager;

/**
 * Created by steve_su on 2017/7/20.
 */

public class MediaAll extends MediaGeneral {
    private FileActionManager mFileActionManager;

    MediaAll(Context context) {
        super(context);
        mFileActionManager = mActivity.mFileActionManager;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        mActivity.getMenuInflater().inflate(R.menu.option_menu_all_file, menu);

        boolean isUploadEnabled = mFileActionManager.isDirectorySupportUpload(mActivity.mPath);
        menu.findItem(R.id.file_manage_viewer_action_upload).setVisible(isUploadEnabled);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        // Does nothing.
    }

    @Override
    public void refresh(boolean showProgress) {
        mActivity.doRefresh();
    }

}
