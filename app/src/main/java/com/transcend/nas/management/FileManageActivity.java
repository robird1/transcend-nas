package com.transcend.nas.management;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
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
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.GoogleAnalysisFactory;
import com.transcend.nas.common.ManageFactory;
import com.transcend.nas.connection.LoginListActivity;
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
import com.transcend.nas.service.LanCheckManager;
import com.transcend.nas.DrawerMenuActivity;
import com.transcend.nas.DrawerMenuController;
import com.transcend.nas.tutk.TutkLogoutLoader;
import com.transcend.nas.view.ProgressDialog;
import com.transcend.nas.viewer.music.MusicActivity;
import com.transcend.nas.viewer.music.MusicManager;
import com.transcend.nas.viewer.photo.ViewerActivity;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileManageActivity extends DrawerMenuActivity implements
        FileManageDropdownAdapter.OnDropdownItemSelectedListener,
        FileManageRecyclerAdapter.OnRecyclerItemCallbackListener,
        ActionMode.Callback {

    private static final String TAG = FileManageActivity.class.getSimpleName();

    private static final int GRID_PORTRAIT = 3;
    private static final int GRID_LANDSCAPE = 5;

    private Context mContext;
    private Toolbar mToolbar;
    private AppCompatSpinner mDropdown;
    private FileManageDropdownAdapter mDropdownAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayout mRecyclerEmptyView;
    private FileManageRecyclerAdapter mRecyclerAdapter;
    private FloatingActionButton mFab;
    private RelativeLayout mProgressView;
    private Snackbar mSnackbar;
    private ActionMode mEditorMode;
    private RelativeLayout mEditorModeView;
    private TextView mEditorModeTitle;
    private Toast mToast;

    private String mPath;
    private ArrayList<FileInfo> mFileList;
    private boolean isDownloadFolder = false;

    private VideoCastManager mCastManager;
    private VideoCastConsumer mCastConsumer;

    private SmbFileListLoader mSmbFileListLoader;
    private FileInfo mFileInfo;
    private String mDownloadFilePath;
    private String mOriginMD5Checksum;

    private ExternalStorageController mStorageController;

    private ActionHelper mActionHelper;
    private FileActionManager mFileActionManager;
    private CustomActionManager mCustomActionManager;

    @Override
    public int onLayoutID() {
        return R.layout.activity_file_manage;
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
            initDropdown();
            initRecyclerView();
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
            } else {
                LanCheckManager.getInstance().setInit(true);
            }
        } else if (requestCode == ExternalStorageLollipop.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mStorageController.onActivityResult(this, data);
            } else {
                toggleDrawerCheckedItem();
            }

        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return mCastManager.onDispatchVolumeKeyEvent(event, 0.05) || super.dispatchKeyEvent(event);
    }

    private void onReceiveIntent(Intent intent) {
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

        mFileActionManager = new FileActionManager(this, FileActionManager.FileActionServiceType.SMB, this);
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
        String hostname = server.getHostname();

        LanCheckManager.getInstance().setInit(true);
        if (hostname.contains(P2PService.getInstance().getP2PIP())) {
            LanCheckManager.getInstance().setLanConnect(false, "");
            LanCheckManager.getInstance().startLanCheck();
        } else {
            LanCheckManager.getInstance().setLanConnect(true, hostname);
        }

        ServerInfo info = server.getServerInfo();
        if (info != null && info.hostName != null && !"".equals(info.hostName)) {
            NASPref.setDeviceName(this, info.hostName);
        }

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
        mDropdown = (AppCompatSpinner) findViewById(R.id.main_dropdown);
        mDropdown.setAdapter(mDropdownAdapter);
        mDropdown.setDropDownVerticalOffset(10);
    }

    private void initRecyclerView() {
        FileManageRecyclerAdapter.LayoutType type = NASPref.getFileViewType(this);
        mRecyclerAdapter = new FileManageRecyclerAdapter(this, mFileList);
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.main_recycler_view);
        switch (type) {
            case GRID:
                updateGridView(false);
                break;
            default:
                updateListView(false);
                break;
        }
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setOnScrollListener(new FileManageRecyclerListener(ImageLoader.getInstance(), true, false));
        mRecyclerEmptyView = (LinearLayout) findViewById(R.id.main_recycler_empty_view);
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
        getMenuInflater().inflate(R.menu.file_manage_viewer, menu);
        FileManageRecyclerAdapter.LayoutType type = NASPref.getFileViewType(this);
        switch (type) {
            case GRID:
                menu.findItem(R.id.file_manage_viewer_action_view).setIcon(R.drawable.ic_toolbar_list_white);
                break;
            default:
                menu.findItem(R.id.file_manage_viewer_action_view).setIcon(R.drawable.ic_toolbar_module_white);
                break;
        }

        menu.findItem(R.id.file_manage_viewer_action_upload).setVisible(mFileActionManager.isDirectorySupportUpload(mPath));
        mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
            case R.id.file_manage_viewer_action_search:
                doSearch();
                return true;
            case R.id.file_manage_viewer_action_upload:
                startFileActionPickerActivity(NASApp.ACT_PICK_UPLOAD);
                return true;
            //case R.id.file_manage_viewer_action_download:
            //    startFileActionPickerActivity(NASApp.ACT_PICK_DOWNLOAD);
            //    break;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * RECYCLER ITEM CONTROL
     */
    @Override
    public void onRecyclerItemClick(int position) {
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
    }

    private void initMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_manage_editor, menu);
        MenuItem item = menu.findItem(R.id.file_manage_editor_action_transmission);
        if (mFileActionManager.isRemoteAction()) {
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
                    String type = mFileActionManager.isRemoteAction() ? NASApp.ACT_DOWNLOAD : NASApp.ACT_UPLOAD;
                    startFileActionLocateActivity(type);
                    break;
                case R.id.file_manage_editor_action_rename:
                    doRename();
                    break;
                case R.id.file_manage_editor_action_share:
                    doShare(getSelectedFiles());
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
    }

    /**
     * SOFT KEY BACK CONTROL
     */
    @Override
    public void onBackPressed() {
        Log.w(TAG, "[Enter] onBackPressed()");
        clearDownloadTask();
        toggleDrawerCheckedItem();

        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mActionHelper.getCurrentLoaderID());
            mActionHelper.destroyLoader();
            mProgressView.setVisibility(View.INVISIBLE);
            return;
        }

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


    /**
     * LOADER CONTROL
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        Loader<Boolean> loader = mActionHelper.onCreateLoader(id, args);
        if (loader instanceof SmbFileListLoader)
            mSmbFileListLoader = (SmbFileListLoader) loader;
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (mActionHelper.onLoadFinished(loader, success))
            return;

        if (loader instanceof TutkLogoutLoader) {
            super.onLoadFinished(loader, success);
            return;
        }

        if (success) {
            if (loader instanceof SmbFileListLoader || loader instanceof LocalFileListLoader) {
                //file list change, stop previous image loader
                ImageLoader.getInstance().stop();
                if (loader instanceof SmbFileListLoader) {
                    mPath = ((SmbFileListLoader) loader).getPath();
                    mFileList = ((SmbFileListLoader) loader).getFileList();
                } else {
                    mPath = ((LocalFileListLoader) loader).getPath();
                    mFileList = ((LocalFileListLoader) loader).getFileList();
                }
                mFileActionManager.setCurrentPath(mPath);
                Collections.sort(mFileList, FileInfoSort.comparator(this));
                FileFactory.getInstance().addFolderFilterRule(mPath, mFileList);
                FileFactory.getInstance().addFileTypeSortRule(mFileList);
                closeEditorMode();
                enableFabEdit(mFileActionManager.isDirectorySupportFileAction(mPath));
                updateScreen();
                toggleDrawerCheckedItem();
            } else {
                doRefresh();
                if (loader instanceof SmbAbstractLoader || loader instanceof LocalAbstractLoader ) {
                    String type = null;
                    if(loader instanceof SmbAbstractLoader)
                        type = ((SmbAbstractLoader) loader).getType();
                    else
                        type = ((LocalAbstractLoader) loader).getType();
                    if (type != null && !type.equals("") && !type.equals(getString(R.string.download))) {
                        Toast.makeText(FileManageActivity.this, type + " - " + getString(R.string.done), Toast.LENGTH_SHORT).show();
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
                if (mCustomActionManager.doNasTUTKLink(loader)) {
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
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        mActionHelper.onLoaderReset(loader);
    }

    /**
     * FILE BROWSER
     */

    private void doRefresh() {
        Log.w(TAG, "doRefresh");
        doLoad(mPath);
    }

    private void doChangeView() {
        if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
            updateListView(true);
            NASPref.setFileViewType(this, FileManageRecyclerAdapter.LayoutType.LIST);
        } else {
            updateGridView(true);
            NASPref.setFileViewType(this, FileManageRecyclerAdapter.LayoutType.GRID);
        }
        invalidateOptionsMenu();
    }

    private void doSort() {
        new FileActionSortDialog(this) {
            @Override
            public void onConfirm() {
                Collections.sort(mFileList, FileInfoSort.comparator(FileManageActivity.this));
                FileFactory.getInstance().addFolderFilterRule(mPath, mFileList);
                FileFactory.getInstance().addFileTypeSortRule(mFileList);
                mRecyclerAdapter.updateList(mFileList);
                mRecyclerAdapter.notifyDataSetChanged();
            }
        };
    }

    private void doSearch() {
        new FileActionSearchDialog(this) {
            @Override
            public void onConfirm(String keyword) {
                keyword = keyword.toLowerCase();
                ArrayList<FileInfo> fileInfo = new ArrayList<FileInfo>();
                for (FileInfo file : mFileList) {
                    if (file.name.toLowerCase().contains(keyword)) {
                        fileInfo.add(file);
                    }
                }

                if (fileInfo.size() > 0) {
                    mFileList.clear();
                    mFileList = fileInfo;
                    mRecyclerAdapter.updateList(mFileList);
                    mRecyclerAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(FileManageActivity.this, getString(R.string.search_result_empty), Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }
        };
    }

    private void doNewFolder() {
        List<String> folderNames = new ArrayList<String>();
        for (FileInfo file : mFileList) {
            if (file.type.equals(FileInfo.TYPE.DIR))
                folderNames.add(file.name);
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
    private int getSelectedCount() {
        int count = 0;
        for (FileInfo file : mFileList) {
            if (file.checked) count++;
        }
        return count;
    }

    private ArrayList<String> getSelectedPaths() {
        ArrayList<String> paths = new ArrayList<String>();
        for (FileInfo file : mFileList) {
            if (file.checked) paths.add(file.path);
        }
        return paths;
    }

    private ArrayList<FileInfo> getSelectedFiles() {
        ArrayList<FileInfo> files = new ArrayList<FileInfo>();
        for (FileInfo file : mFileList) {
            if (file.checked) files.add(file);
        }
        return files;
    }

    public void doLoad(String path) {
        mFileActionManager.checkServiceType(path);
        if (mFileActionManager.isRemoteAction() && mCustomActionManager.doNasHashKeyTimeOutCheck(path)) {
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
                names.add(file.name);
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
                mFileActionManager.share(NASPref.getShareLocation(FileManageActivity.this), files);
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

    private void doDelete(ArrayList<String> paths) {
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
    private void updateScreen() {
        mDropdownAdapter.updateList(mPath, mFileActionManager.getServiceMode());
        mDropdownAdapter.notifyDataSetChanged();
        mRecyclerAdapter.updateList(mFileList);
        mRecyclerAdapter.notifyDataSetChanged();
        checkTopView();
        checkEmptyView();
        invalidateOptionsMenu();
    }

    private void checkTopView() {
        mDrawerController.setDrawerIndicatorEnabled(mFileActionManager.isTopDirectory(mPath) || (isDownloadFolder && mFileActionManager.isDownloadDirectory(this, mPath)));
    }

    private void checkEmptyView() {
        mRecyclerEmptyView.setVisibility((mFileList != null && mFileList.size() > 0) ? View.GONE : View.VISIBLE);
    }

    private void updateListView(boolean update) {
        LinearLayoutManager list = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(list);
        if (update) {
            mRecyclerView.getRecycledViewPool().clear();
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }

    private void updateGridView(boolean update) {
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

    private void resizeToolbar() {
        TypedValue typedValue = new TypedValue();
        int[] attr = new int[]{R.attr.actionBarSize};
        TypedArray array = obtainStyledAttributes(typedValue.resourceId, attr);
        mToolbar.getLayoutParams().height = array.getDimensionPixelSize(0, 0);
        array.recycle();
    }

    private void enableFabEdit(boolean enabled) {
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
                    FileManageActivity.this.finish();
                }
            });
            mSnackbar.show();
        } else {
            mSnackbar.dismiss();
            mSnackbar = null;
        }
    }

    private void startEditorMode() {
        if (mEditorMode == null)
            startSupportActionMode(this);
    }

    private void closeEditorMode() {
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

    private void toggleSelectAll() {
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

    private void toggleEditorModeAction(int count) {
        boolean visible = (count == 1);
        boolean containFolder = false;
        if (visible) {
            ArrayList<FileInfo> files = getSelectedFiles();
            for (FileInfo file : files) {
                if (file.type.equals(FileInfo.TYPE.DIR))
                    containFolder = true;
            }
        }
        mEditorMode.getMenu().findItem(R.id.file_manage_editor_action_rename).setVisible(visible);
        mEditorMode.getMenu().findItem(R.id.file_manage_editor_action_share).setVisible(!containFolder & visible);
        mEditorMode.getMenu().findItem(R.id.file_manage_editor_action_new_folder).setVisible(count == 0);
    }

    @Override
    public void toggleDrawerCheckedItem() {
        int id;
        mFileActionManager.checkServiceType(mPath);
        if (mFileActionManager.isRemoteAction()) {
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

    private void toast(int resId) {
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
            intent.setClass(FileManageActivity.this, FileActionLocateActivity.class);
            intent.putExtras(args);
            startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
        }
    }

    private void startFileActionPickerActivity(String type) {
        if (NASApp.ACT_PICK_UPLOAD.equals(type) && !mFileActionManager.isDirectorySupportUpload(mPath)) {
            Toast.makeText(this, "Can't upload to this folder", Toast.LENGTH_SHORT).show();
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
        intent.setClass(FileManageActivity.this, FileActionPickerActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, FileActionPickerActivity.REQUEST_CODE);
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

                MediaInfo info = MediaFactory.createMediaInfo(this, MediaMetadata.MEDIA_TYPE_MUSIC_TRACK, fileInfo.path);
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

        ArrayList<FileInfo> list = new ArrayList<FileInfo>();
        for (FileInfo info : mFileList) {
            if (FileInfo.TYPE.MUSIC.equals(info.type) && MusicActivity.checkFormatSupportOrNot(info.path)) {
                list.add(info);
            }
        }
        MusicManager.getInstance().setMusicList(list);

        Bundle args = new Bundle();
        args.putString("path", fileInfo.path);
        args.putString("mode", mode);
        args.putString("root", root);
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, MusicActivity.class);
        intent.putExtras(args);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            startActivityForResult(intent, MusicActivity.REQUEST_CODE, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        else
            startActivityForResult(intent, MusicActivity.REQUEST_CODE);
    }

    private void startVideoActivity(FileInfo fileInfo) {
        if (mFileActionManager.isRemoteAction() && mCastManager != null && mCastManager.isConnected()) {
            try {
                //clean image
                mCastManager.sendDataMessage("close");

                MediaInfo info = MediaFactory.createMediaInfo(this, MediaMetadata.MEDIA_TYPE_MOVIE, fileInfo.path);
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

        MediaFactory.open(this, fileInfo.path);
    }

    private void startViewerActivity(String mode, String root, String path) {
        ArrayList<FileInfo> list = new ArrayList<FileInfo>();
        for (FileInfo info : mFileList) {
            if (FileInfo.TYPE.PHOTO.equals(info.type))
                list.add(info);
        }
        FileFactory.getInstance().setFileList(list);

        Bundle args = new Bundle();
        args.putString("path", path);
        args.putString("mode", mode);
        args.putString("root", root);
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, ViewerActivity.class);
        intent.putExtras(args);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            startActivityForResult(intent, ViewerActivity.REQUEST_CODE, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        else
            startActivityForResult(intent, ViewerActivity.REQUEST_CODE);
    }

    private void startFileInfoActivity(FileInfo info) {
        Bundle args = new Bundle();
        args.putSerializable("info", info);
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, FileInfoActivity.class);
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
                }

                NASUtils.showAppChooser(FileManageActivity.this, destUri);
            }

            @Override
            public void onFail() {
                if (mProgressView != null) {
                    mProgressView.setVisibility(View.INVISIBLE);
                }
                Toast.makeText(mContext, getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearDownloadTask() {
        DownloadFactory.getManager(mContext, DownloadFactory.Type.TEMPORARY).cancel();
    }

    public void openFileBy3rdApp(Context context, FileInfo fileInfo) {
        mFileInfo = fileInfo;
        if (fileInfo.isLocalFile()) {
            fileInfo.openLocalFile(context);
        } else if (NASUtils.isSDCardPath(context, fileInfo.path)) {
            fileInfo.openSDCardFile(context);
        } else {
            if (mProgressView != null) {
                mProgressView.setVisibility(View.VISIBLE);
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
                mCustomActionManager.doOpenWithUpload(this, mFileInfo, mDownloadFilePath, mSmbFileListLoader);
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

