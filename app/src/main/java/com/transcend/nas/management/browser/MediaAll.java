package com.transcend.nas.management.browser;

import android.content.Context;
import android.util.Log;
import android.view.Menu;

import com.transcend.nas.R;
import com.transcend.nas.management.fileaction.FileActionManager;

/**
 * Created by steve_su on 2017/7/20.
 */

public class MediaAll extends MediaGeneral {
    private static final String TAG = MediaAll.class.getSimpleName();

//    private String mPath;
    private FileActionManager mFileActionManager;
//    private ArrayList<FileInfo> mFileList;

    MediaAll(Context context) {
        super(context);
//        mPath = mActivity.mPath;
//        mFileList = mActivity.mFileList;
        mFileActionManager = mActivity.mFileActionManager;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "[Enter] onPrepareOptionsMenu");
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

    //    private void doChangeView() {
//        Log.d(TAG, "[Enter] doChangeView");
//        if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
//            mActivity.updateListView(true);
//            NASPref.setFileViewType(mContext, FileManageRecyclerAdapter.LayoutType.LIST);
//            BrowserData.getInstance(mTabPosition).setViewMode(mContext, Browser.LayoutType.LIST);
//        } else {
//            mActivity.updateGridView(true);
//            NASPref.setFileViewType(mContext, FileManageRecyclerAdapter.LayoutType.GRID);
//            BrowserData.getInstance(mTabPosition).setViewMode(mContext, Browser.LayoutType.GRID);
//        }
//        ((Activity) mContext).invalidateOptionsMenu();
//    }


}
