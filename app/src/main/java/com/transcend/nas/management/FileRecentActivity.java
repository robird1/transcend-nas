package com.transcend.nas.management;

import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.transcend.nas.NASApp;
import com.transcend.nas.R;
import com.transcend.nas.management.fileaction.FileActionManager;
import com.transcend.nas.service.FileRecentInfo;
import com.transcend.nas.service.FileRecentManager;

import java.util.ArrayList;

public class FileRecentActivity extends FileManageActivity {

    private SectionDecoration mSectionDecoration;
    private ArrayList<FileRecentInfo> mFileRecentList;
    private ArrayList<String> mFileIDList;
    private int mFileIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mDefaultType = FileActionManager.FileActionServiceType.RECENT;
        mChoiceAllSameTypeFile = false;
        super.onCreate(savedInstanceState);
        enableFabEdit(false);
        mFileActionManager.doLockActionType();
    }

    @Override
    protected void onResume() {
        super.onResume();
        moveData(mFileIndex, 0);
    }

    @Override
    protected void initRecyclerView() {
        mRecyclerRefresh = (SwipeRefreshLayout) findViewById(R.id.main_recycler_refresh);
        mRecyclerRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                doRefresh();
            }
        });
        mRecyclerAdapter = new FileRecentRecyclerAdapter(this, mFileList);
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.main_recycler_view);
        updateListView(false);
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setOnScrollListener(new FileManageRecyclerListener(ImageLoader.getInstance(), true, false));
        mRecyclerEmptyView = (LinearLayout) findViewById(R.id.main_recycler_empty_view);
        ImageView image = (ImageView) findViewById(R.id.main_recycler_empty_image);
        image.setImageResource(R.drawable.image_recent_empty);
        mSectionDecoration = new SectionDecoration(null, mContext);
        mRecyclerView.addItemDecoration(mSectionDecoration);
    }

    @Override
    protected void onReceiveIntent(Intent intent) {
        doRefresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.file_manage_viewer_action_search).setVisible(false);
        menu.findItem(R.id.file_manage_viewer_action_sort).setVisible(false);
        menu.findItem(R.id.file_manage_viewer_action_view).setVisible(false);
        MenuItem item = menu.findItem(R.id.file_manage_viewer_action_upload);
        item.setVisible(true);
        item.setIcon(R.drawable.ic_toolbar_delete_white);
        item.setTitle(getString(R.string.delete));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.file_manage_viewer_action_upload:
                if (mFileList != null && mFileList.size() > 0) {
                    ArrayList<String> paths = new ArrayList<String>();
                    for (FileInfo file : mFileList)
                        paths.add(file.path);
                    doDelete(paths);
                } else {
                    toast(R.string.no_item_selected);
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onRecyclerItemClick(int position) {
        if (0 <= position && position < mFileList.size()) {
            mFileIndex = position;
            FileInfo info = mFileList.get(position);
            mFileActionManager.open(info.path);
        }
    }

    @Override
    public void onRecyclerItemLongClick(int position) {
        return;
    }

    @Override
    public void onRecyclerItemIconClick(int position) {
        return;
    }

    @Override
    public void onRecyclerItemInfoClick(int position) {
        removeData(position);
    }

    @Override
    public void toggleDrawerCheckedItem() {
        mDrawerController.setCheckedItem(R.id.nav_recent);
    }

    @Override
    protected void updateScreen() {
        mSectionDecoration.updateList(mFileIDList);
        super.updateScreen();
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof RecentListLoader) {
            ImageLoader.getInstance().stop();
            checkRecentListLoader(success, (RecentListLoader) loader);
            toggleDrawerCheckedItem();
            updateScreen();
            checkEmptyView();
            mProgressView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            mRecyclerRefresh.setRefreshing(false);
            return;
        } else if (loader instanceof RecentCheckLoader && success) {
            checkRecentCheckLoader(success, (RecentCheckLoader) loader);
            mProgressView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            return;
        }

        super.onLoadFinished(loader, success);
    }

    private void checkRecentListLoader(boolean success, RecentListLoader loader) {
        mPath = loader.getPath();
        mFileRecentList = loader.getFileList();
        mFileIDList = loader.getFileDayIDList();
        mFileActionManager.setCurrentPath(mPath);

        if (mFileList == null)
            mFileList = new ArrayList<>();
        mFileList.clear();

        for (FileRecentInfo info : mFileRecentList) {
            FileInfo tmp = new FileInfo();
            tmp.name = info.info.name;
            tmp.path = info.info.path;
            tmp.type = info.info.type;
            tmp.size = info.info.size;
            tmp.time = convertActionTypeToString(info.actionType) + " : " + info.actionTime.replaceAll("-", "/");
            mFileList.add(tmp);
        }
    }

    private void checkRecentCheckLoader(boolean success, RecentCheckLoader loader) {
        if (success && mFileRecentList != null && mFileRecentList.size() > mFileIndex && mFileIndex >= 0) {
            boolean exist = loader.isExistFile();
            if (exist) {
                FileInfo info = mFileList.get(mFileIndex);
                switch (info.type) {
                    case DIR:
                        startFileManageActivity(R.id.nav_storage, info.path);
                        break;
                    case PHOTO:
                        startViewerActivity(NASApp.MODE_SMB, NASApp.ROOT_SMB, info.path);
                        break;
                    default:
                        super.onRecyclerItemClick(mFileIndex);
                }

            } else {
                removeData(mFileIndex);
                Toast.makeText(this, getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void moveData(int position, final int target) {
        if (mFileRecentList != null && mFileRecentList.size() > position && position >= 0) {
            FileRecentInfo recentInfo = mFileRecentList.get(position);
            mFileRecentList.remove(position);
            mFileRecentList.add(target, recentInfo);

            FileInfo info;
            ArrayList<FileRecentInfo> lists = FileRecentManager.getInstance().getAction(1);
            if (lists != null && lists.size() > 0) {
                FileRecentInfo tmp = lists.get(0);
                info = tmp.info;
                info.time = convertActionTypeToString(tmp.actionType) + " : " + tmp.actionTime.replaceAll("-", "/");
            } else {
                info = mFileList.get(position);
                info.time = getString(R.string.last_opened) + " : " + FileInfo.getTime(System.currentTimeMillis());
            }
            mFileList.remove(position);
            mFileList.add(target, info);
            mRecyclerAdapter.updateList(mFileList);

            mFileIDList.remove(position);
            mFileIDList.add(target, getString(R.string.today));
            mSectionDecoration.updateList(mFileIDList);

            mRecyclerAdapter.notifyItemMoved(position, target);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecyclerAdapter.notifyItemChanged(target);
                }
            }, 200);
        }
        mFileIndex = -1;
    }

    private String convertActionTypeToString(FileRecentInfo.ActionType actionType) {
        String actionString;
        switch (actionType) {
            case UPLOAD:
                actionString = getString(R.string.last_uploaded);
                break;
            case RENAME:
                actionString = getString(R.string.last_renamed);
                break;
            case MOVE:
                actionString = getString(R.string.last_moved);
                break;
            case OPEN:
                actionString = getString(R.string.last_opened);
                break;
            default:
                actionString = "Last " + actionType.toString().toLowerCase();
                break;
        }
        return actionString;
    }

    private void removeData(int position) {
        if (mFileRecentList != null && mFileRecentList.size() > position && position >= 0) {
            FileRecentInfo info = mFileRecentList.get(position);
            FileRecentManager.getInstance().deleteAction(info);

            mFileRecentList.remove(position);
            mFileList.remove(position);
            mFileIDList.remove(position);
            if (mFileRecentList.size() > 0) {
                mSectionDecoration.updateList(mFileIDList);
                mRecyclerAdapter.updateList(mFileList);
                mRecyclerAdapter.notifyItemRemoved(position);
            } else {
                doRefresh();
                //updateScreen();
                //checkEmptyView();
            }
        }
        mFileIndex = -1;
    }

    @Override
    public void onBackPressed() {
        toggleDrawerCheckedItem();
        if (!stopRunningLoader()) {
            if (mDrawerController.isDrawerOpen()) {
                mDrawerController.closeDrawer();
            } else {
                mDrawerController.openDrawer();
            }
        }
    }
}

