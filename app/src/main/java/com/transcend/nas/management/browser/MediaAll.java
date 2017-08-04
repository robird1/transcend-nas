package com.transcend.nas.management.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.R;
import com.transcend.nas.management.FileActionPickerActivity;
import com.transcend.nas.management.FileActionSortDialog;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileInfoSort;
import com.transcend.nas.management.FileManageRecyclerAdapter;
import com.transcend.nas.management.browser_framework.Browser;
import com.transcend.nas.management.browser_framework.BrowserData;
import com.transcend.nas.management.fileaction.FileActionManager;
import com.transcend.nas.management.firmware.FileFactory;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by steve_su on 2017/7/20.
 */

public class MediaAll extends MediaType {
    private static final String TAG = MediaAll.class.getSimpleName();

    private int mTabPosition;
    private String mPath;
    private FileActionManager mFileActionManager;
    private VideoCastManager mCastManager;
    private RecyclerView mRecyclerView;
    private ArrayList<FileInfo> mFileList;
    protected FileManageRecyclerAdapter mRecyclerAdapter;

    MediaAll(Context context) {
        super(context);
        mTabPosition = BrowserData.ALL.getTabPosition();
        mPath = mActivity.mPath;
        mFileActionManager = mActivity.mFileActionManager;
        mCastManager = mActivity.mCastManager;
        mFileList = mActivity.mFileList;

        mRecyclerView = mActivity.mRecyclerView;
        mRecyclerAdapter = mActivity.mRecyclerAdapter;
    }

    @Override
    public boolean createOptionsMenu(Menu menu) {
        super.createOptionsMenu(menu);

        menu.findItem(R.id.file_manage_viewer_action_upload).setVisible(mFileActionManager.isDirectorySupportUpload(mPath));

        return true;
    }

    @Override
    public boolean optionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.file_manage_viewer_action_refresh:
                doRefresh();
                return true;
            case R.id.file_manage_viewer_action_view:
                doChangeView();
                return true;
            case R.id.file_manage_viewer_action_sort:
                doSort();
                return true;
//            case R.id.file_manage_viewer_action_search:
//                doSearch();
//                return true;
            case R.id.file_manage_viewer_action_upload:
                doUpload();
                return true;
            //case R.id.file_manage_viewer_action_download:
            //    startFileActionPickerActivity(NASApp.ACT_PICK_DOWNLOAD);
            //    break;
        }
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.file_manage_viewer, menu);
    }

    @Override
    public void load(int position) {
        mActivity.doRefresh();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
//        if (id == VIEW_ALL) {
//            new ViewAllLoader(getContext(), args);
//        } else if (id == VIEW_ALBUM) {
//            new ViewAlbumLoader(getContext(), args);
//        }
//        return new ViewAllLoader(getContext(), args);
        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, ArrayList data) {
//        super.onLoadFinished(loader, data);
//
//        if (loader instanceof ViewAllLoader) {
//            int nextLoadingIndex = ((ViewAllLoader) loader).nextLoadingIndex();
//            mModels.get(getTabPosition()).setLoadingIndex(nextLoadingIndex);
//        } else if (loader instanceof ViewAlbumLoader) {
//
//        }
    }


    private void doChangeView() {
        Log.d(TAG, "[Enter] doChangeView");
        if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
            mActivity.updateListView(true);
            BrowserData.getInstance(mTabPosition).setViewMode(mContext, Browser.LayoutType.LIST);
        } else {
            mActivity.updateGridView(true);
            BrowserData.getInstance(mTabPosition).setViewMode(mContext, Browser.LayoutType.GRID);
        }
        ((Activity) mContext).invalidateOptionsMenu();
    }

    private void doSort() {
        new FileActionSortDialog(mContext) {
            @Override
            public void onConfirm() {
                Collections.sort(mFileList, FileInfoSort.comparator(mContext));
                FileFactory.getInstance().addFolderFilterRule(mPath, mFileList);
                FileFactory.getInstance().addFileTypeSortRule(mFileList);
                mRecyclerAdapter.updateList(mFileList);
                mRecyclerAdapter.notifyDataSetChanged();
            }
        };
    }

    private void doUpload() {
        startFileActionPickerActivity(NASApp.ACT_PICK_UPLOAD);
    }

    private void startFileActionPickerActivity(String type) {
        if (NASApp.ACT_PICK_UPLOAD.equals(type) && !mFileActionManager.isDirectorySupportUpload(mPath)) {
            Toast.makeText(mContext, "Can't upload to this folder", Toast.LENGTH_SHORT).show();
            return;
        }

        String mode = NASApp.ACT_PICK_UPLOAD.equals(type) ? NASApp.MODE_STG : NASApp.MODE_SMB;
        String root = NASApp.ACT_PICK_UPLOAD.equals(type) ? NASApp.ROOT_STG : NASApp.ROOT_SMB;
        String path = NASApp.ACT_PICK_UPLOAD.equals(type) ? NASApp.ROOT_STG : NASApp.ROOT_SMB;
        Bundle args = new Bundle();
        args.putString("mode", mode);
        args.putString("type", type);
        args.putString("root", root);
        args.putString("path", path);
        args.putString("target", mPath);
        Intent intent = new Intent();
        intent.setClass(mContext, FileActionPickerActivity.class);
        intent.putExtras(args);
        mActivity.startActivityForResult(intent, FileActionPickerActivity.REQUEST_CODE);
    }

    private void doRefresh() {

    }


}
