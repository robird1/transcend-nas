package com.transcend.nas.management.browser;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.DrawerMenuActivity;
import com.transcend.nas.DrawerMenuController;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.GoogleAnalysisFactory;
import com.transcend.nas.common.ManageFactory;
import com.transcend.nas.connection.LoginListActivity;
import com.transcend.nas.management.FileActionDeleteDialog;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.FileActionNewFolderDialog;
import com.transcend.nas.management.FileActionPickerActivity;
import com.transcend.nas.management.FileActionRenameDialog;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileInfoActivity;
import com.transcend.nas.management.FileInfoSort;
import com.transcend.nas.management.FileManageDropdownAdapter;
import com.transcend.nas.management.FileManageRecyclerAdapter;
import com.transcend.nas.management.FileManageRecyclerListener;
import com.transcend.nas.management.LocalAbstractLoader;
import com.transcend.nas.management.LocalFileCopyLoader;
import com.transcend.nas.management.LocalFileDeleteLoader;
import com.transcend.nas.management.LocalFileListLoader;
import com.transcend.nas.management.LocalFileMoveLoader;
import com.transcend.nas.management.LocalFileRenameLoader;
import com.transcend.nas.management.LocalFolderCreateLoader;
import com.transcend.nas.management.SmbAbstractLoader;
import com.transcend.nas.management.SmbFileListLoader;
import com.transcend.nas.management.browser_framework.Browser;
import com.transcend.nas.management.browser_framework.BrowserData;
import com.transcend.nas.management.browser_framework.BrowserFragment;
import com.transcend.nas.management.download.AbstractDownloadManager;
import com.transcend.nas.management.download.DownloadFactory;
import com.transcend.nas.management.download.TempFileDownloadManager;
import com.transcend.nas.management.externalstorage.ExternalStorageController;
import com.transcend.nas.management.externalstorage.ExternalStorageLollipop;
import com.transcend.nas.management.fileaction.AbstractActionManager;
import com.transcend.nas.management.fileaction.ActionHelper;
import com.transcend.nas.management.fileaction.CustomActionManager;
import com.transcend.nas.management.fileaction.FileActionManager;
import com.transcend.nas.management.firmware.FileFactory;
import com.transcend.nas.management.firmware.MediaFactory;
import com.transcend.nas.management.firmware.TwonkyManager;
import com.transcend.nas.service.AutoBackupService;
import com.transcend.nas.service.FileRecentFactory;
import com.transcend.nas.service.FileRecentInfo;
import com.transcend.nas.service.FileRecentManager;
import com.transcend.nas.service.LanCheckManager;
import com.transcend.nas.tutk.TutkLogoutLoader;
import com.transcend.nas.view.ProgressDialog;
import com.transcend.nas.viewer.music.MusicActivity;
import com.transcend.nas.viewer.music.MusicManager;
import com.transcend.nas.viewer.photo.ViewerActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by steve_su on 2017/6/3.
 */

public class SJC_FileManageActivity extends DrawerMenuActivity implements
        FileManageDropdownAdapter.OnDropdownItemSelectedListener,
        FileManageRecyclerAdapter.OnRecyclerItemCallbackListener,
        ActionMode.Callback {

    private static final String TAG = SJC_FileManageActivity.class.getSimpleName();

    protected static final int GRID_PORTRAIT = 3;
    protected static final int GRID_LANDSCAPE = 5;
    private static final int FRAGMENT_APP_START = 0;
    private static final int FRAGMENT_NAVIGATE_FROM_ROOT = 1;

    protected Context mContext;
    protected Toolbar mToolbar;
    protected AppCompatSpinner mDropdown;
    protected FileManageDropdownAdapter mDropdownAdapter;
    protected SwipeRefreshLayout mRecyclerRefresh;
    protected RecyclerView mRecyclerView;
    protected LinearLayout mRecyclerEmptyView;
    protected FileManageRecyclerAdapter mRecyclerAdapter;
    protected FloatingActionButton mFab;
    protected RelativeLayout mProgressView;
    protected ProgressBar mProgressBar;
    protected Snackbar mSnackbar;
    protected ActionMode mEditorMode;
    protected RelativeLayout mEditorModeView;
    protected TextView mEditorModeTitle;
    protected Toast mToast;

    protected String mPath;
    protected ArrayList<FileInfo> mFileList;
    protected boolean isDownloadFolder = false;

    protected VideoCastManager mCastManager;
    protected VideoCastConsumer mCastConsumer;

    protected SmbFileListLoader mSmbFileListLoader;
    protected FileInfo mFileInfo;
    protected String mDownloadFilePath;
    protected String mOriginMD5Checksum;

    protected ExternalStorageController mStorageController;

    protected ActionHelper mActionHelper;
    protected FileActionManager mFileActionManager;
    protected CustomActionManager mCustomActionManager;
    protected FileActionManager.FileActionServiceType mDefaultType = FileActionManager.FileActionServiceType.SMB;
    protected boolean mChoiceAllSameTypeFile = true;
    private int mTabPosition;
    MediaController mMediaControl;

    @Override
    public int onLayoutID() {
        return R.layout.activity_file_manage_fragment;
    }

    @Override
    public int onToolbarID() {
        return R.id.main_toolbar;
    }

    @Override
    public DrawerMenuController.DrawerMenu onActivityDrawer() {
        return DrawerMenuController.DrawerMenu.DRAWER_DEFAULT;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate");

        String password = NASPref.getPassword(this);
        if (password != null && !password.equals("")) {
            init();
            mMediaControl = new MediaController(this, BrowserData.ALL.getTabPosition());
            initDropdown();
//            initRecyclerView();
            initFabs();
            initProgressView();
            initActionModeView();
            initAutoBackUpService();
            initExternalStorageController();
            initDownloadManager();
            onReceiveIntent(getIntent());
            NASPref.setInitial(this, true);

        } else {
            startSignInActivity();
        }
    }

    private void checkCurrentSelectedItem(Intent intent) {
        int selectedItemId = intent.getIntExtra("selectedItemId", -1);
        switch (selectedItemId) {
            case R.id.nav_device:
                GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.BROWSER_LOCAL);
                break;
            case R.id.nav_sdcard:
                GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.BROWSER_LOCAL_SDCARD);
                break;
            case R.id.nav_downloads:
                GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.BROWSER_LOCAL_DOWNLOAD);
                break;
            default:
                GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.BROWSER_REMOTE);
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        onReceiveIntent(intent);
        Log.w(TAG, "onNewIntent");
    }

    @Override
    protected void onResume() {
        mCastManager = VideoCastManager.getInstance();
        if (null != mCastManager) {
            mCastManager.addVideoCastConsumer(mCastConsumer);
            mCastManager.incrementUiCounter();
        }

        checkCacheFileState();
        super.onResume();
        Log.w(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        if (null != mCastManager) {
            mCastManager.decrementUiCounter();
            mCastManager.removeVideoCastConsumer(mCastConsumer);
        }
        super.onPause();
        Log.w(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        clearDownloadTask();
        super.onDestroy();
        Log.w(TAG, "onDestroy");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.w(TAG, "onActivityResult");

        if (requestCode == FileActionLocateActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle != null) {
                    String type = bundle.getString("type");
                    String path = bundle.getString("path");
                    ArrayList<String> paths = getSelectedPaths();
                    if (NASApp.ACT_UPLOAD.equals(type))
                        doUpload(path, paths);
                    else if (NASApp.ACT_DOWNLOAD.equals(type))
                        doDownload(path, paths);
                    else if (NASApp.ACT_COPY.equals(type))
                        doCopy(path, paths);
                    else if (NASApp.ACT_MOVE.equals(type))
                        doMove(path, paths);
                }
            }
            //force close editor mode
            closeEditorMode();
        } else if (requestCode == ViewerActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle != null) {
                    boolean delete = bundle.getBoolean("delete");
                    if (delete)
                        doRefresh();
                }
            }
        } else if (requestCode == FileActionPickerActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle != null) {
                    String type = bundle.getString("type");
                    ArrayList<String> paths = bundle.getStringArrayList("paths");
                    if (NASApp.ACT_PICK_UPLOAD.equals(type))
                        doUpload(mPath, paths);
                    else if (NASApp.ACT_PICK_DOWNLOAD.equals(type))
                        doDownload(mPath, paths);
                }
            }
        } else if (requestCode == LoginListActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                clearDataAfterSwitch();
                if (Build.VERSION.SDK_INT >= 11) {
                    recreate();
                } else {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
            }
        } else if (requestCode == ExternalStorageLollipop.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mStorageController.onActivityResult(this, data);                // TODO
            } else {
                toggleDrawerCheckedItem();
            }

        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return mCastManager.onDispatchVolumeKeyEvent(event, 0.05) || super.dispatchKeyEvent(event);
    }

    protected void onReceiveIntent(Intent intent) {
        checkCurrentSelectedItem(intent);
        if (intent != null) {
            String action = intent.getAction();
            String type = intent.getType();
            String path = intent.getStringExtra("path");
            Log.d(TAG, "onReceiveIntent : " + action + ", " + type + ", " + path);
            if (path != null && !path.equals("")) {
                isDownloadFolder = mFileActionManager.isDownloadDirectory(this, path);
                doLoad(path);
                return;
            }
        }

        doRefresh();
    }

    /**
     * INITIALIZATION
     */
    private void init() {
        mContext = this;
        mFileList = new ArrayList<FileInfo>();

        mFileActionManager = new FileActionManager(this, mDefaultType, this);
        mPath = mFileActionManager.getServiceRootPath();

        mCustomActionManager = new CustomActionManager(this, this);

        List<AbstractActionManager> actionManagerList = new ArrayList<>();
        actionManagerList.add(mFileActionManager);
        actionManagerList.add(mCustomActionManager);
        mActionHelper = new ActionHelper(actionManagerList);

        mCastManager = VideoCastManager.getInstance();
        mCastConsumer = new VideoCastConsumerImpl() {
            @Override
            public void onFailed(int resourceId, int statusCode) {
                String reason = "Not Available";
                if (resourceId > 0) {
                    reason = getString(resourceId);
                }
                Log.e(TAG, "Action failed, reason:  " + reason + ", status code: " + statusCode);
            }

            @Override
            public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId,
                                               boolean wasLaunched) {
                invalidateOptionsMenu();
            }

            @Override
            public void onDisconnected() {
                invalidateOptionsMenu();
            }

            @Override
            public void onConnectionSuspended(int cause) {
                Log.d(TAG, "onConnectionSuspended() was called with cause: " + cause);
            }
        };

        Server server = ServerManager.INSTANCE.getCurrentServer();
        ServerInfo info = server.getServerInfo();
        if (info != null && info.hostName != null && !"".equals(info.hostName)) {
            NASPref.setDeviceName(this, info.hostName);
        }

        LanCheckManager.getInstance().initLanCheck(this, server);
        TwonkyManager.getInstance().initTwonky();
    }

    private void initAutoBackUpService() {
        boolean enable = NASPref.getBackupSetting(this);
        Intent intent = new Intent(this, AutoBackupService.class);
        if (!enable) {
            stopService(intent);
            return;
        }

        if (!ManageFactory.isServiceRunning(this, AutoBackupService.class))
            startService(intent);

    }

    @Override
    protected void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_navi_backaarow_white);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initDropdown() {
        mDropdownAdapter = new FileManageDropdownAdapter(this);
        mDropdownAdapter.setOnDropdownItemSelectedListener(this);
        mDropdownAdapter.updateList(mPath, mFileActionManager.getServiceMode());
        mDropdown = (AppCompatSpinner) findViewById(R.id.main_dropdown);
        mDropdown.setAdapter(mDropdownAdapter);
        mDropdown.setDropDownVerticalOffset(10);
        mDropdownAdapter.notifyDataSetChanged();
    }

    public void onRecyclerViewInit(BrowserFragment fragment) {
        Log.d(TAG, "[Enter] onRecyclerViewInit");
        mRecyclerView = fragment.getRecyclerView();
        mRecyclerEmptyView = fragment.getRecyclerEmptyView();

        Browser.LayoutType type = BrowserData.getInstance(fragment.getPosition()).getViewMode(this);
        if (type == Browser.LayoutType.GRID) {
            updateGridView(false, mRecyclerView);
        } else {
            updateListView(false, mRecyclerView);
        }

        mRecyclerAdapter = (FileManageRecyclerAdapter) mRecyclerView.getAdapter();
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addOnScrollListener(new FileManageRecyclerListener(ImageLoader.getInstance(), true, false));

        mFileList = mRecyclerAdapter.getList();
        mRecyclerAdapter.updateList(mFileList);
        mRecyclerAdapter.notifyDataSetChanged();

        mTabPosition = fragment.getPosition();

        mMediaControl = new MediaController(this, fragment.getPosition());
        Log.d(TAG, "[Enter] invalidateOptionsMenu");
        invalidateOptionsMenu();

    }

    void onRecyclerViewInit() {
        FileManageRecyclerAdapter.LayoutType type = NASPref.getFileViewType(this);
        switch (type) {
            case GRID:
                updateGridView(false);
                break;
            default:
                updateListView(false);
                break;
        }
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);

        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setOnScrollListener(new FileManageRecyclerListener(ImageLoader.getInstance(), true, false));
    }

    private void initFabs() {
        mFab = (FloatingActionButton) findViewById(R.id.main_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditorMode == null)
                    startEditorMode();
                else
                    toggleSelectAll();
            }
        });
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.main_progress_view);
        mProgressView.setVisibility(View.VISIBLE);
        mProgressBar = (ProgressBar) findViewById(R.id.main_progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);
        mRecyclerRefresh = (SwipeRefreshLayout) findViewById(R.id.main_recycler_refresh);
        mActionHelper.setProgressLayout(mProgressView);
    }

    void onProgressViewInit(AbstractFileManageFragment fragment) {
        mProgressView.setVisibility(View.INVISIBLE);
        mProgressView = fragment.mProgressView;
        mProgressView.setVisibility(View.VISIBLE);
        mProgressBar = fragment.mProgressBar;
        mProgressBar.setVisibility(View.VISIBLE);
        mRecyclerRefresh = fragment.mSwipeRefreshLayout;
        mActionHelper.setProgressLayout(mProgressView);
    }
    void onProgressViewInit(Browser fragment) {
        mProgressView = fragment.mProgressView;
        mProgressBar = fragment.mProgressBar;
        mRecyclerRefresh = fragment.mSwipeRefreshLayout;
        mActionHelper.setProgressLayout(mProgressView);
    }

    @Override
    protected void initDrawer() {
        super.initDrawer();
        mDrawerController.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String parent = new File(mPath).getParent();
                doLoad(parent);
            }
        });
    }

    private void initActionModeView() {
        mEditorModeView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.action_mode_custom, null);
        mEditorModeTitle = (TextView) mEditorModeView.findViewById(R.id.action_mode_custom_title);
    }

    private void initExternalStorageController() {
        mStorageController = new ExternalStorageController(mContext);
    }

    /**
     * DROPDOWN ITEM CONTROL
     */
    @Override
    public void onDropdownItemSelected(int position) {
        Log.w(TAG, "onDropdownItemSelected: " + position);
        if (position > 0) {
            String path = mDropdownAdapter.getPath(position);
            if (isDownloadFolder) {
                String download = NASPref.getDownloadLocation(this);
                if (download.contains(path) && !download.equals(path)) {
                    isDownloadFolder = false;
                    toggleDrawerCheckedItem();
                }
            }
            doLoad(path);
        }
    }


    /**
     * MENU CONTROL
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return mMediaControl.createOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mMediaControl.optionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "[Enter] onPrepareOptionsMenu");
        mMediaControl.onPrepareOptionsMenu(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * RECYCLER ITEM CONTROL
     */
    @Override
    public void onRecyclerItemClick(int position) {
        Log.d(TAG, "\n[Enter] onRecyclerItemClick");
        if (mEditorMode == null) {
            // browser
            String mode = mFileActionManager.getServiceMode();
            String root = mFileActionManager.getServiceRootPath();
            FileInfo fileInfo = mFileList.get(position);
            if (FileInfo.TYPE.DIR.equals(fileInfo.type)) {
                doLoad(fileInfo.path);
            } else if (FileInfo.TYPE.PHOTO.equals(fileInfo.type)) {
                startViewerActivity(mode, root, fileInfo.path);
            } else if (FileInfo.TYPE.VIDEO.equals(fileInfo.type)) {
                startVideoActivity(fileInfo);
            } else if (FileInfo.TYPE.MUSIC.equals(fileInfo.type)) {
                startMusicActivity(mode, root, fileInfo);
            } else {
                openFileBy3rdApp(this, fileInfo);
            }
        } else {
            // editor
            selectAtPosition(position);
        }
    }

    @Override
    public void onRecyclerItemLongClick(int position) {
        if (!mFileActionManager.isDirectorySupportFileAction(mPath))
            return;

        if (mEditorMode == null) {
            startEditorMode();
            selectAtPosition(position);
        }
    }

    @Override
    public void onRecyclerItemInfoClick(int position) {
        FileInfo fileInfo = mFileList.get(position);
        if (FileInfo.TYPE.DIR.equals(fileInfo.type)) {
            if (mEditorMode == null) {
                doLoad(fileInfo.path);
            } else {
                selectAtPosition(position);
            }
        } else {
            startFileInfoActivity(fileInfo);
        }
    }

    @Override
    public void onRecyclerItemIconClick(int position) {
        if (!mFileActionManager.isDirectorySupportFileAction(mPath))
            return;

        if (mEditorMode == null) {
            startEditorMode();
        }
        selectAtPosition(position);
    }

    /**
     * ACTION MODE CONTROL
     */
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        initView(mode);
        initMenu(menu);
        updateEditorModeTitle(0);
        toggleEditorModeAction(0);
        toggleFabSelectAll(false);
        toast(R.string.edit_mode);
        return true;
    }

    private void initView(ActionMode mode) {
        mEditorMode = mode;
        mEditorMode.setCustomView(mEditorModeView);
        mDrawerController.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        enableRecyclerRefresh(false);
    }

    protected void initMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_manage_editor, menu);
        MenuItem item = menu.findItem(R.id.file_manage_editor_action_transmission);
        if (mFileActionManager.isRemoteAction(mPath)) {
            item.setTitle(R.string.download);
            item.setIcon(R.drawable.ic_toolbar_download_white);
        } else {
            item.setTitle(R.string.upload);
            item.setIcon(R.drawable.ic_toolbar_upload_white);
        }
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean isEmpty = (getSelectedCount() == 0);
        int id = item.getItemId();
        if (isEmpty && id != R.id.file_manage_editor_action_new_folder && id != R.id.file_manage_editor_action_more) {
            toast(R.string.no_item_selected);
        } else {
            switch (id) {
                case R.id.file_manage_editor_action_transmission:
                    String type = mFileActionManager.isRemoteAction(mPath) ? NASApp.ACT_DOWNLOAD : NASApp.ACT_UPLOAD;
                    startFileActionLocateActivity(type);
                    break;
                case R.id.file_manage_editor_action_rename:
                    doRename();
                    break;
                case R.id.file_manage_editor_action_share:
                    doShare(getSelectedFiles());
                    break;
                case R.id.file_manage_editor_action_share_link:
                    doShareLink(getSelectedFiles());
                    break;
                case R.id.file_manage_editor_action_copy:
                    startFileActionLocateActivity(NASApp.ACT_COPY);
                    break;
                case R.id.file_manage_editor_action_cut:
                    startFileActionLocateActivity(NASApp.ACT_MOVE);
                    break;
                case R.id.file_manage_editor_action_delete:
                    doDelete(getSelectedPaths());
                    break;
                case R.id.file_manage_editor_action_new_folder:
                    doNewFolder();
                    break;
            }
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mDrawerController.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        mEditorMode = null;
        clearAllSelection();
        enableFabEdit(true);
        enableRecyclerRefresh(true);
    }

    /**
     * SOFT KEY BACK CONTROL
     */
    @Override
    public void onBackPressed() {
        Log.w(TAG, "\n[Enter] onBackPressed()");
        clearDownloadTask();
        toggleDrawerCheckedItem();

        if (!stopRunningLoader()) {
            if (mDrawerController.isDrawerOpen()) {
                mDrawerController.closeDrawer();
            } else {
                if (mFileActionManager.isTopDirectory(mPath) || (isDownloadFolder && mFileActionManager.isDownloadDirectory(this, mPath))) {
                    mDrawerController.openDrawer();
                } else {
                    String parent = new File(mPath).getParent();
                    doLoad(parent);
                }
            }
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mDrawerController.closeDrawer();
            toggleSnackbar();
            return true;
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.w(TAG, "onConfigurationChanged");
        if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager)
            updateGridView(true);
        resizeToolbar();
    }

    protected boolean stopRunningLoader() {
        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mActionHelper.getCurrentLoaderID());
            mActionHelper.destroyLoader();
            mProgressView.setVisibility(View.INVISIBLE);
            Log.d(TAG, "[Enter] stopRunningLoader() mProgressView.setVisibility(View.INVISIBLE");
            mProgressBar.setVisibility(View.VISIBLE);
            mRecyclerRefresh.setRefreshing(false);
            return true;
        }

        return false;
    }


    /**
     * LOADER CONTROL
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "[Enter] onCreateLoader");

        Loader<Boolean> loader = mActionHelper.onCreateLoader(id, args);
        if (loader instanceof SmbFileListLoader)
            mSmbFileListLoader = (SmbFileListLoader) loader;
//        if (mRecyclerRefresh.isRefreshing())
//            mProgressBar.setVisibility(View.INVISIBLE);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        Log.d(TAG, "[Enter] onLoadFinished");
        if (mActionHelper.onLoadFinished(loader, success)) {
            return;
        }

        if (loader instanceof TutkLogoutLoader) {
            super.onLoadFinished(loader, success);
            return;
        }

        if (success) {
            Log.d(TAG, "[Enter] success");

            if (loader instanceof SmbFileListLoader || loader instanceof LocalFileListLoader) {
                //file list change, stop previous image loader
                ImageLoader.getInstance().stop();
                if (loader instanceof SmbFileListLoader) {
                    Log.d(TAG, "[Enter] loader instanceof SmbFileListLoader");

                    mPath = ((SmbFileListLoader) loader).getPath();
                    mFileList = ((SmbFileListLoader) loader).getFileList();
                } else {
                    Log.d(TAG, "[Enter] ! loader instanceof SmbFileListLoader");

                    mPath = ((LocalFileListLoader) loader).getPath();
                    mFileList = ((LocalFileListLoader) loader).getFileList();
                }
                Log.d(TAG, "mPath: "+ mPath+ " mFileList.size(): "+ mFileList.size());
//                MediaType.getInstance(MediaType.ALL.getTabPosition()).updateFileList(mFileList, true);
                BrowserData.getInstance(BrowserData.ALL.getTabPosition()).updateFileList(mFileList, true);

                mFileActionManager.setCurrentPath(mPath);
                Collections.sort(mFileList, FileInfoSort.comparator(this));
                FileFactory.getInstance().addFolderFilterRule(mPath, mFileList);
                FileFactory.getInstance().addFileTypeSortRule(mFileList);
                closeEditorMode();
                enableFabEdit(mFileActionManager.isDirectorySupportFileAction(mPath));
                postCheckFragment();
                updateScreen();
                toggleDrawerCheckedItem();
            } else {
                if (mProgressView.isShown()) {
                    doRefresh();
                    return;
                }

                if (loader instanceof SmbAbstractLoader || loader instanceof LocalAbstractLoader) {
                    String type;
                    if (loader instanceof SmbAbstractLoader)
                        type = ((SmbAbstractLoader) loader).getType();
                    else
                        type = ((LocalAbstractLoader) loader).getType();
                    if (type != null && !type.equals("") && !type.equals(getString(R.string.download))) {
                        Toast.makeText(mContext, type + " - " + getString(R.string.done), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            if (LanCheckManager.getInstance().getLanConnect()) {
                LanCheckManager.getInstance().startLanCheck();
                if (loader instanceof SmbAbstractLoader) {
                    Toast.makeText(this, ((SmbAbstractLoader) loader).getExceptionMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            } else {
                if (mCustomActionManager.getRecordCommandID() > 0 && mCustomActionManager.doNasTUTKLink(loader)) {
                    //Toast.makeText(this, getString(R.string.try_remote_access), Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    LanCheckManager.getInstance().startLanCheck();
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            }

            if (loader instanceof LocalFileCopyLoader || loader instanceof LocalFileMoveLoader || loader instanceof LocalFileRenameLoader ||
                    loader instanceof LocalFileDeleteLoader || loader instanceof LocalFolderCreateLoader) {
                mStorageController.handleWriteOperationFailed();
            }
        }

        checkEmptyView();
        mProgressView.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        mRecyclerRefresh.setRefreshing(false);
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        mActionHelper.onLoaderReset(loader);
    }

    /**
     * FILE BROWSER
     */

    protected void doRefresh() {
        Log.w(TAG, "doRefresh");
        doLoad(mPath);
    }

    private void doNewFolder() {
        List<String> folderNames = new ArrayList<String>();
        for (FileInfo file : mFileList) {
            if (file.type.equals(FileInfo.TYPE.DIR))
                folderNames.add(file.name.toLowerCase());
        }
        new FileActionNewFolderDialog(this, folderNames) {
            @Override
            public void onConfirm(String newName) {
                mFileActionManager.createFolder(mPath, newName);
            }
        };
    }


    /**
     * FILE EDITOR
     */
    protected int getSelectedCount() {
        int count = 0;
        for (FileInfo file : mFileList) {
            if (file.checked) count++;
        }
        return count;
    }

    protected ArrayList<String> getSelectedPaths() {
        ArrayList<String> paths = new ArrayList<String>();
        for (FileInfo file : mFileList) {
            if (file.checked) paths.add(file.path);
        }
        return paths;
    }

    protected ArrayList<FileInfo> getSelectedFiles() {
        ArrayList<FileInfo> files = new ArrayList<FileInfo>();
        for (FileInfo file : mFileList) {
            if (file.checked) files.add(file);
        }
        return files;
    }

    public void doLoad(String path) {
        if (mFileActionManager.isRemoteAction(path) && mCustomActionManager.doNasHashKeyTimeOutCheck(path)) {
            return;
        }
        mFileActionManager.list(path);
    }

    private void doUpload(String dest, ArrayList<String> paths) {
        mFileActionManager.upload(dest, paths);
    }

    private void doDownload(String dest, ArrayList<String> paths) {
        mFileActionManager.download(dest, paths);
    }

    private void doRename() {
        List<String> names = new ArrayList<String>();
        FileInfo target = new FileInfo();
        for (FileInfo file : mFileList) {
            if (file.checked)
                target = file;
            else
                names.add(file.name.toLowerCase());
        }

        final String path = target.path;
        boolean ignoreType = target.type.equals(FileInfo.TYPE.DIR);
        new FileActionRenameDialog(this, ignoreType, target.name, names) {
            @Override
            public void onConfirm(String newName) {
                mFileActionManager.rename(path, newName);
                closeEditorMode();
            }
        };
    }

    private void doShare(final ArrayList<FileInfo> files) {
        Bundle value = new Bundle();
        value.putString(ProgressDialog.DIALOG_TITLE, mContext.getString(R.string.share));
        value.putInt(ProgressDialog.DIALOG_ICON, R.drawable.ic_toolbar_share_gray);
        String format = mContext.getResources().getString(files.size() <= 1 ? R.string.msg_file_selected : R.string.msg_files_selected);
        value.putString(ProgressDialog.DIALOG_MESSAGE, String.format(format, files.size()));
        new ProgressDialog(mContext, value) {
            @Override
            public void onConfirm() {
                mFileActionManager.share(NASPref.getShareLocation(mContext), files);
                dismiss();
            }

            @Override
            public void onCancel() {

            }
        };
        closeEditorMode();
    }

    private void doShareLink(final ArrayList<FileInfo> files) {
        Bundle value = new Bundle();
        value.putString(ProgressDialog.DIALOG_TITLE, "Share Link");
        value.putInt(ProgressDialog.DIALOG_ICON, R.drawable.ic_toolbar_share_gray);
        String format = mContext.getResources().getString(files.size() <= 1 ? R.string.msg_file_selected : R.string.msg_files_selected);
        value.putString(ProgressDialog.DIALOG_MESSAGE, String.format(format, files.size()));
        new ProgressDialog(mContext, value) {
            @Override
            public void onConfirm() {
                mFileActionManager.shareLink(files);
                dismiss();
            }

            @Override
            public void onCancel() {

            }
        };
        closeEditorMode();
    }


    private void doCopy(String dest, ArrayList<String> paths) {
        if (mFileActionManager.isSubDirectory(dest, paths)) {
            Toast.makeText(this, getString(R.string.select_folder_error), Toast.LENGTH_SHORT).show();
            return;
        }

        mFileActionManager.copy(dest, paths);
    }

    private void doMove(String dest, ArrayList<String> paths) {
        if (mFileActionManager.isSubDirectory(dest, paths)) {
            Toast.makeText(this, getString(R.string.select_folder_error), Toast.LENGTH_SHORT).show();
            return;
        }

        mFileActionManager.move(dest, paths);
    }

    protected void doDelete(ArrayList<String> paths) {
        new FileActionDeleteDialog(this, paths) {
            @Override
            public void onConfirm(ArrayList<String> paths) {
                mFileActionManager.delete(paths);
                closeEditorMode();
            }
        };
    }

    /**
     * UX CONTROL
     */
    protected void updateScreen() {
        Log.d(TAG, "\n[Enter] updateScreen");
        mDropdownAdapter.updateList(mPath, mFileActionManager.getServiceMode());
        mDropdownAdapter.notifyDataSetChanged();
        mRecyclerAdapter.updateList(mFileList);
        mRecyclerAdapter.notifyDataSetChanged();
        checkTopView();
        invalidateOptionsMenu();
    }

    protected void checkTopView() {
        mDrawerController.setDrawerIndicatorEnabled(mFileActionManager.isTopDirectory(mPath) || (isDownloadFolder && mFileActionManager.isDownloadDirectory(this, mPath)));
    }

    protected void checkEmptyView() {
        mRecyclerEmptyView.setVisibility((mFileList != null && mFileList.size() > 0) ? View.GONE : View.VISIBLE);
    }

    protected void updateListView(boolean update) {
        LinearLayoutManager list = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(list);
        if (update) {
            mRecyclerView.getRecycledViewPool().clear();
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }
    protected void updateListView(boolean update, RecyclerView view) {
        mRecyclerView.setLayoutManager(view.getLayoutManager());
        if (update) {
            mRecyclerView.getRecycledViewPool().clear();
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }

    protected void updateGridView(boolean update) {
        int orientation = getResources().getConfiguration().orientation;
        int spanCount = (orientation == Configuration.ORIENTATION_PORTRAIT)
                ? GRID_PORTRAIT : GRID_LANDSCAPE;
        GridLayoutManager grid = new GridLayoutManager(this, spanCount);
        grid.setSpanSizeLookup(new SpanSizeLookup(grid.getSpanCount()));
        mRecyclerView.setLayoutManager(grid);
        if (update) {
            mRecyclerView.getRecycledViewPool().clear();
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }
    protected void updateGridView(boolean update, RecyclerView view) {
        GridLayoutManager grid = (GridLayoutManager) view.getLayoutManager();
        grid.setSpanSizeLookup(new SpanSizeLookup(grid.getSpanCount()));
        mRecyclerView.setLayoutManager(grid);
        if (update) {
            mRecyclerView.getRecycledViewPool().clear();
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }

    private void resizeToolbar() {
        TypedValue typedValue = new TypedValue();
        int[] attr = new int[]{R.attr.actionBarSize};
        TypedArray array = obtainStyledAttributes(typedValue.resourceId, attr);
        mToolbar.getLayoutParams().height = array.getDimensionPixelSize(0, 0);
        array.recycle();
    }

    protected void enableRecyclerRefresh(boolean enable) {
        mRecyclerRefresh.setEnabled(enable);
    }

    protected void enableFabEdit(boolean enabled) {
        mFab.setImageResource(R.drawable.ic_floating_edit_white);
        mFab.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
    }

    private void toggleFabSelectAll(boolean selectAll) {
        int resId = selectAll
                ? R.drawable.ic_floating_unselectall_white
                : R.drawable.ic_floating_selectall_white;
        mFab.setImageResource(resId);
        mFab.setVisibility(View.VISIBLE);
    }
    //*/

    private void toggleSnackbar() {
        if (mSnackbar == null || !mSnackbar.isShown()) {
            mSnackbar = Snackbar.make(mRecyclerView, R.string.msg_exit_app, Snackbar.LENGTH_SHORT);
            mSnackbar.setAction(R.string.exit, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SJC_FileManageActivity.this.finish();
                }
            });
            mSnackbar.show();
        } else {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    public void startEditorMode() {
        if (mEditorMode == null)
            startSupportActionMode(this);
    }

    protected void closeEditorMode() {
        if (mEditorMode != null)
            mEditorMode.finish();
    }

    private void updateEditorModeTitle(int count) {
        String format = getResources().getString(count <= 1 ? R.string.msg_file_selected : R.string.msg_files_selected);
        mEditorModeTitle.setText(String.format(format, count));
    }

    private void selectAtPosition(int position) {
        boolean checked = mFileList.get(position).checked;
        mFileList.get(position).checked = !checked;
        mRecyclerAdapter.notifyItemChanged(position);
        int count = getSelectedCount();
        boolean selectAll = (count == mFileList.size());
        updateEditorModeTitle(count);
        toggleEditorModeAction(count);
        toggleFabSelectAll(selectAll);
    }

    public void toggleSelectAll() {
        int count = getSelectedCount();
        boolean selectAll = (count != 0) && (count == mFileList.size());
        if (selectAll)
            clearAllSelection();
        else
            checkAllSelection();
        selectAll = !selectAll;
        count = selectAll ? mFileList.size() : 0;
        updateEditorModeTitle(count);
        toggleEditorModeAction(count);
        toggleFabSelectAll(selectAll);
    }

    private void checkAllSelection() {
        for (FileInfo file : mFileList)
            file.checked = true;
        mRecyclerAdapter.notifyDataSetChanged();
    }

    private void clearAllSelection() {
        for (FileInfo file : mFileList)
            file.checked = false;
        mRecyclerAdapter.notifyDataSetChanged();
    }

    protected void toggleEditorModeAction(int count) {
        boolean visible = (count == 1);
        boolean containFolder = false;
        boolean containFile = false;
        if (visible) {
            ArrayList<FileInfo> files = getSelectedFiles();
            for (FileInfo file : files) {
                if (file.type.equals(FileInfo.TYPE.DIR))
                    containFolder = true;
                if (file.type.equals(FileInfo.TYPE.FILE))
                    containFile = true;
            }
        }
        mEditorMode.getMenu().findItem(R.id.file_manage_editor_action_rename).setVisible(visible);
        mEditorMode.getMenu().findItem(R.id.file_manage_editor_action_share).setVisible(!containFolder & visible);
        //mEditorMode.getMenu().findItem(R.id.file_manage_editor_action_share_link).setVisible(!containFolder & !containFile & visible);
        mEditorMode.getMenu().findItem(R.id.file_manage_editor_action_share_link).setVisible(false);
    }

    @Override
    public void toggleDrawerCheckedItem() {
        int id;
        if (mFileActionManager.isRemoteAction(mPath)) {
            id = R.id.nav_storage;
        } else {
            if (isDownloadFolder) {
                id = R.id.nav_downloads;
            } else {
                String root = mFileActionManager.getServiceRootPath();
                if (root.equals(NASUtils.getSDLocation(this))) {
                    id = R.id.nav_sdcard;
                } else {
                    id = R.id.nav_device;
                }
            }
        }

        mDrawerController.setCheckedItem(id);
    }

    protected void toast(int resId) {
        if (mToast != null)
            mToast.cancel();
        mToast = Toast.makeText(this, resId, Toast.LENGTH_SHORT);
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    private void startFileActionLocateActivity(String type) {
        String mode
                = NASApp.ACT_UPLOAD.equals(type) ? NASApp.MODE_SMB
                : NASApp.ACT_DOWNLOAD.equals(type) ? NASApp.MODE_STG
                : mFileActionManager.getServiceMode();
        String root
                = NASApp.ACT_UPLOAD.equals(type) ? NASApp.ROOT_SMB
                : NASApp.ACT_DOWNLOAD.equals(type) ? NASApp.ROOT_STG
                : mFileActionManager.getServiceRootPath();
        final String path
                = NASApp.ACT_UPLOAD.equals(type) ? NASApp.ROOT_SMB
                : NASApp.ACT_DOWNLOAD.equals(type) ? NASPref.getDownloadLocation(this)
                : mPath;

        //for Action Download, we use default download folder
        if (NASApp.ACT_DOWNLOAD.equals(type) && NASPref.useDefaultDownloadFolder) {
            int count = getSelectedCount();
            String format = getString(count <= 1 ? R.string.msg_file_selected : R.string.msg_files_selected);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.download);
            builder.setIcon(R.drawable.ic_toolbar_download_gray);
            builder.setMessage(String.format(format, count));
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.confirm, null);
            builder.setCancelable(true);
            final AlertDialog dialog = builder.show();
            Button bnPos = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            bnPos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //start download
                    doDownload(path, getSelectedPaths());
                    //force close dialog
                    dialog.dismiss();
                    //force close editor mode
                    closeEditorMode();
                }
            });
        } else {
            Bundle args = new Bundle();
            args.putString("mode", mode);
            args.putString("type", type);
            args.putString("root", root);
            args.putString("path", path);
            Intent intent = new Intent();
            intent.setClass(mContext, FileActionLocateActivity.class);
            intent.putExtras(args);
            startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
        }
    }

    public void startMusicPrepare(String path) {
        ArrayList<FileInfo> list = new ArrayList<FileInfo>();
        int index = -1;
        if (mChoiceAllSameTypeFile) {
            for (FileInfo info : mFileList) {
                if (FileInfo.TYPE.MUSIC.equals(info.type) && MusicActivity.checkFormatSupportOrNot(info.path)) {
                    list.add(info);
                }
            }
        } else {
            for (FileInfo info : mFileList) {
                if (FileInfo.TYPE.MUSIC.equals(info.type) && path.equals(info.path)) {
                    list.add(info);
                    break;
                }
            }
        }

        for (FileInfo info : list) {
            index++;
            if (path.equals(info.path))
                break;
        }
        MusicManager.getInstance().setMusicList(list, index, mFileActionManager.isRemoteAction(path));
    }

    private void startMusicActivity(String mode, String root, FileInfo fileInfo) {
        if (!MusicActivity.checkFormatSupportOrNot(fileInfo.path)) {
            startVideoActivity(fileInfo);
            return;
        }

        if (!fileInfo.path.startsWith(NASApp.ROOT_STG) && mCastManager != null && mCastManager.isConnected()) {
            try {
                //clean image
                mCastManager.sendDataMessage("close");

                MediaInfo info = MediaFactory.createChromeCastMediaInfo(this, MediaMetadata.MEDIA_TYPE_MUSIC_TRACK, fileInfo.path);
                if (info != null) {
                    mCastManager.startVideoCastControllerActivity(this, info, 0, true);
                    return;
                }
            } catch (TransientNetworkDisconnectionException e) {
                e.printStackTrace();
            } catch (NoConnectionException e) {
                e.printStackTrace();
            }
        }

        startMusicPrepare(fileInfo.path);
        Bundle args = new Bundle();
        args.putString("path", fileInfo.path);
        args.putString("mode", mode);
        args.putString("root", root);
        Intent intent = new Intent();
        intent.setClass(mContext, MusicActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, MusicActivity.REQUEST_CODE);
    }

    private void startVideoActivity(FileInfo fileInfo) {
        if (mFileActionManager.isRemoteAction(fileInfo.path)) {
            FileRecentManager.getInstance().setAction(FileRecentFactory.create(this, fileInfo, FileRecentInfo.ActionType.OPEN));

            if (mCastManager != null && mCastManager.isConnected()) {
                try {
                    //clean image
                    mCastManager.sendDataMessage("close");

                    MediaInfo info = MediaFactory.createChromeCastMediaInfo(this, MediaMetadata.MEDIA_TYPE_MOVIE, fileInfo.path);
                    if (info != null) {
                        mCastManager.startVideoCastControllerActivity(this, info, 0, true);
                        return;
                    }
                } catch (TransientNetworkDisconnectionException e) {
                    e.printStackTrace();
                } catch (NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        }

        MediaFactory.open(this, fileInfo.path);
    }

    public void startViewerPrepare(String path) {
        ArrayList<FileInfo> list = new ArrayList<FileInfo>();
        if (mChoiceAllSameTypeFile) {
            for (FileInfo info : mFileList) {
                if (FileInfo.TYPE.PHOTO.equals(info.type))
                    list.add(info);
            }
        } else {
            for (FileInfo info : mFileList) {
                if (FileInfo.TYPE.PHOTO.equals(info.type) && path.equals(info.path)) {
                    list.add(info);
                    break;
                }
            }
        }
        FileFactory.getInstance().setFileList(list);
    }

    public void startViewerActivity(String mode, String root, String path) {
        startViewerPrepare(path);
        Bundle args = new Bundle();
        args.putString("path", path);
        args.putString("mode", mode);
        args.putString("root", root);
        Intent intent = new Intent();
        intent.setClass(mContext, ViewerActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, ViewerActivity.REQUEST_CODE);
    }

    private void startFileInfoActivity(FileInfo info) {
        Bundle args = new Bundle();
        args.putSerializable("info", info);
        Intent intent = new Intent();
        intent.setClass(mContext, FileInfoActivity.class);
        intent.putExtras(args);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            startActivityForResult(intent, FileInfoActivity.REQUEST_CODE, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        else
            startActivityForResult(intent, FileInfoActivity.REQUEST_CODE);
    }

    private void initDownloadManager() {
        TempFileDownloadManager manager = (TempFileDownloadManager) DownloadFactory.getManager(mContext, DownloadFactory.Type.TEMPORARY);
        manager.setOpenFileListener(new TempFileDownloadManager.OpenFileListener() {
            @Override
            public void onComplete(Uri destUri) {
                mDownloadFilePath = destUri.getPath();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mOriginMD5Checksum = getMD5Checksum();
                    }
                }).start();

                if (mProgressView != null) {
                    mProgressView.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "[Enter] initDownloadManager().onComplete() mProgressView.setVisibility(View.INVISIBLE");

                    mProgressBar.setVisibility(View.VISIBLE);
                }

                NASUtils.showAppChooser(mContext, destUri);
            }

            @Override
            public void onFail() {
                if (mProgressView != null) {
                    mProgressView.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "[Enter] initDownloadManager().onFail() mProgressView.setVisibility(View.INVISIBLE");

                    mProgressBar.setVisibility(View.VISIBLE);
                }
                Toast.makeText(mContext, getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearDownloadTask() {
        DownloadFactory.getManager(mContext, DownloadFactory.Type.TEMPORARY).cancel();
    }

    public void openFileBy3rdApp(Context context, FileInfo fileInfo) {
        if (mFileActionManager.isRemoteAction(fileInfo.path))
            FileRecentManager.getInstance().setAction(FileRecentFactory.create(this, fileInfo, FileRecentInfo.ActionType.OPEN));

        mFileInfo = fileInfo;
        if (fileInfo.isLocalFile()) {
            fileInfo.openLocalFile(context);
        } else if (NASUtils.isSDCardPath(context, fileInfo.path)) {
            fileInfo.openSDCardFile(context);
        } else {
            if (mProgressView != null) {
                mProgressView.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
            }

            Bundle data = new Bundle();
            data.putString(AbstractDownloadManager.KEY_SOURCE_PATH, fileInfo.path);
            DownloadFactory.getManager(context, DownloadFactory.Type.TEMPORARY).start(data);
        }
    }

    private void checkCacheFileState() {
        if (mOriginMD5Checksum != null) {
            String checksum = getMD5Checksum();
            if (checksum != null && !mOriginMD5Checksum.equals(checksum)) {
                //TODO
//                mCustomActionManager.doOpenWithUpload(this, mFileInfo, mDownloadFilePath, mSmbFileListLoader);
            }
        }

        mOriginMD5Checksum = null;
    }

    private String getMD5Checksum() {
        String checksum = null;
        try {
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            checksum = getFileChecksum(md5Digest, new File(mDownloadFilePath));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return checksum;

    }

    private String getFileChecksum(MessageDigest digest, File file) throws IOException {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }

    private void replaceFragment(Fragment fragment, String tag) {
        Log.d(TAG, "[Enter] replaceFragment");
        if (fragment instanceof SJC_Browser) {
            Log.d(TAG, "[Enter] enableFabEdit mPath: "+ mPath);
            enableFabEdit(mFileActionManager.isDirectorySupportFileAction(mPath));
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.setCustomAnimations(R.anim.appear, 0);
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.addToBackStack(null);
        transaction.commit();
        getSupportFragmentManager().executePendingTransactions();
    }

    private void postCheckFragment() {
        Log.d(TAG, "\n[Enter] postCheckFragment");
        switch (getSupportFragmentManager().getBackStackEntryCount()) {
            case FRAGMENT_APP_START:
                // initialize root fragment

                if (NASApp.ROOT_SMB.equals(mPath)) {
                    replaceFragment(new FileManageFragment(), FileManageFragment.TAG);
                }
                break;
            case FRAGMENT_NAVIGATE_FROM_ROOT:
                if (NASApp.ROOT_SMB.equals(new File(mPath).getParent())) {
                    replaceFragment(new SJC_Browser(), SJC_Browser.TAG);

                }
                break;
            default:
                // back to root fragment

                if (NASApp.ROOT_SMB.equals(mPath)) {
                    getSupportFragmentManager().popBackStackImmediate();
                    Log.d(TAG, "[Enter] popBackStackImmediate()");
                }
                break;
        }
    }


    /**
     * GRID LAYOUT MANAGER SPAN SIZE LOOKUP
     */
    private class SpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

        private int spanSize;

        public SpanSizeLookup(int spanCount) {
            spanSize = spanCount;
        }

        @Override
        public int getSpanSize(int position) {
            return mRecyclerAdapter.isFooter(position) ? spanSize : 1;
        }
    }

}
