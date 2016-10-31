package com.transcend.nas.management;

import android.app.LoaderManager;
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
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.transcend.nas.R;
import com.transcend.nas.common.AnalysisFactory;
import com.transcend.nas.common.FileFactory;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.ManageFactory;
import com.transcend.nas.common.MediaFactory;
import com.transcend.nas.connection.StartActivity;
import com.transcend.nas.connection_new.LoginActivity;
import com.transcend.nas.connection_new.LoginListActivity;
import com.transcend.nas.service.AutoBackupService;
import com.transcend.nas.service.LanCheckManager;
import com.transcend.nas.service.TwonkyManager;
import com.transcend.nas.settings.AboutActivity;
import com.transcend.nas.settings.DiskFactory;
import com.transcend.nas.settings.DiskInfoActivity;
import com.transcend.nas.settings.NewSettingsActivity;
import com.transcend.nas.settings.SettingsActivity;
import com.transcend.nas.view.NotificationDialog;
import com.transcend.nas.view.ProgressDialog;
import com.transcend.nas.viewer.music.MusicActivity;
import com.transcend.nas.viewer.music.MusicService;
import com.transcend.nas.viewer.photo.ViewerActivity;
import com.tutk.IOTC.P2PService;
import com.tutk.IOTC.P2PTunnelAPIs;
import com.tutk.IOTC.sP2PTunnelSessionInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileManageActivity extends AppCompatActivity implements
        FileManageDropdownAdapter.OnDropdownItemSelectedListener,
        FileManageRecyclerAdapter.OnRecyclerItemCallbackListener,
        NavigationView.OnNavigationItemSelectedListener,
        LoaderManager.LoaderCallbacks<Boolean>,
        P2PTunnelAPIs.IP2PTunnelCallback,
        View.OnClickListener,
        ActionMode.Callback {

    private static final String TAG = FileManageActivity.class.getSimpleName();

    private static final int GRID_PORTRAIT = 3;
    private static final int GRID_LANDSCAPE = 5;

    private int[] RETRY_CMD = new int[]{LoaderID.SMB_FILE_LIST, LoaderID.SMB_FILE_RENAME, LoaderID.SMB_FILE_DELETE,
            LoaderID.SMB_NEW_FOLDER, LoaderID.EVENT_NOTIFY};
    private Toolbar mToolbar;
    private AppCompatSpinner mDropdown;
    private FileManageDropdownAdapter mDropdownAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayout mRecyclerEmptyView;
    private FileManageRecyclerAdapter mRecyclerAdapter;
    /*// expanded fabs
    private FloatingActionButton mFabControl;
    private FloatingActionButton mFabNewFolder;
    private FloatingActionButton mFabNewFile;
    /*/
    private FloatingActionButton mFab;
    //*/
    private RelativeLayout mProgressView;
    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mToggle;
    private NavigationView mNavView;
    private View mNavHeader;
    private TextView mNavHeaderTitle;
    private TextView mNavHeaderSubtitle;
    private ImageView mNavHeaderIcon;
    private Snackbar mSnackbar;
    private ActionMode mEditorMode;
    private RelativeLayout mEditorModeView;
    private TextView mEditorModeTitle;
    private Toast mToast;
    private ProgressDialog mShareDialog;

    private String mMode;
    private String mRoot;
    private String mPath;
    private boolean mDevice = false;
    private ArrayList<FileInfo> mFileList;
    private Server mServer;
    private int mLoaderID;
    private int mPreviousLoaderID = -1;
    private Bundle mPreviousLoaderArgs = null;
    private boolean isAutoBackupServiceInit = false;
    private boolean isNeedEventNotify = false;

    private VideoCastManager mCastManager;
    private VideoCastConsumer mCastConsumer;
    private MenuItem mMediaRouteMenuItem;

    private SmbFileShareLoader mSmbFileShareLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate");
        setContentView(R.layout.activity_file_manage);
        AnalysisFactory.getInstance(this).sendScreen(AnalysisFactory.VIEW.BROWSERREMOTE);
        String password = NASPref.getPassword(this);
        if (password != null && !password.equals("")) {
            init();
            initToolbar();
            initDropdown();
            initRecyclerView();
            initFabs();
            initProgressView();
            initDrawer();
            initActionModeView();
            NASPref.initDownloadManager(this, mProgressView);
            doRefresh();
            NASPref.setInitial(this, true);

            // Get intent, action and MIME type
            Intent intent = getIntent();
            onReceiveIntent(intent);
        } else {
            startSignInActivity(true);
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

        Log.d(TAG, "onResume is need event notify : " + isNeedEventNotify);
        if (isNeedEventNotify) {
            isNeedEventNotify = false;
            if (!mProgressView.isShown()) {
                doEventNotify(false, mPath);
            }
        }

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
        isNeedEventNotify = true;
        super.onStop();
        Log.w(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        P2PService.getInstance().removeP2PListener(this);
        NASPref.resetDownloadManager();
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
                if (bundle == null) return;
                String type = bundle.getString("type");
                String path = bundle.getString("path");
                if (NASApp.ACT_UPLOAD.equals(type))
                    doUpload(path, getSelectedPaths());
                if (NASApp.ACT_DOWNLOAD.equals(type))
                    doDownload(path, getSelectedPaths());
                if (NASApp.ACT_COPY.equals(type))
                    doCopy(path);
                if (NASApp.ACT_MOVE.equals(type))
                    doMove(path);
            }
            //force close editor mode
            closeEditorMode();
        } else if (requestCode == ViewerActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle == null) return;
                boolean delete = bundle.getBoolean("delete");
                if (delete)
                    doRefresh();
            }
        } else if (requestCode == FileActionPickerActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle == null) return;
                String type = bundle.getString("type");
                ArrayList<String> paths = bundle.getStringArrayList("paths");
                if (NASApp.ACT_PICK_UPLOAD.equals(type)) {
                    doUpload(mPath, paths);
                }
                if (NASApp.ACT_PICK_DOWNLOAD.equals(type)) {
                    doDownload(mPath, paths);
                }
            }
        } else if (requestCode == LoginListActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                boolean isRunning = false;

                //clean email and account information
                NASPref.clearDataAfterSwitch(this);

                //stop auto backup service
                isRunning = ManageFactory.isServiceRunning(this, AutoBackupService.class);
                if (isRunning) {
                    Intent intent = new Intent(FileManageActivity.this, AutoBackupService.class);
                    stopService(intent);
                }

                //stop music service
                isRunning = ManageFactory.isServiceRunning(this, MusicService.class);
                if (isRunning) {
                    Intent intent = new Intent(FileManageActivity.this, MusicService.class);
                    stopService(intent);
                }

                //clean disk info
                DiskFactory.getInstance().cleanDiskDevices();

                //clean path map
                FileFactory.getInstance().cleanRealPathMap();

                //clean twonky map
                TwonkyManager.getInstance().cleanTwonkyMap();

                //clean lan check
                LanCheckManager.getInstance().setLanConnect(false, "");

                if (Build.VERSION.SDK_INT >= 11) {
                    recreate();
                } else {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }
                return;
            }
        }

        isNeedEventNotify = false;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return mCastManager.onDispatchVolumeKeyEvent(event, 0.05) || super.dispatchKeyEvent(event);
    }

    private void onReceiveIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "onReceiveIntent Empty");
            return;
        }

        String action = intent.getAction();
        String type = intent.getType();
        Log.d(TAG, "onReceiveIntent Other " + action + ", " + type);
        // Handle other intents, such as being started from the home screen
        String path = intent.getStringExtra("path");
        if (path != null && !path.equals("")) {
            doLoad(path);
        }
    }


    /**
     * INITIALIZATION
     */
    private void init() {
        mMode = NASApp.MODE_SMB;
        mPath = mRoot = NASApp.ROOT_SMB;
        mFileList = new ArrayList<FileInfo>();
        mServer = ServerManager.INSTANCE.getCurrentServer();
        P2PService.getInstance().addP2PListener(this);
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

        isNeedEventNotify = false;

        String hostname = mServer.getHostname();
        if (hostname.contains(P2PService.getInstance().getP2PIP())) {
            LanCheckManager.getInstance().setLanConnect(false, "");
            LanCheckManager.getInstance().startLanCheck();
        } else {
            LanCheckManager.getInstance().setLanConnect(true, hostname);
        }

        String firmware = NASPref.defaultFirmwareVersion;
        ServerInfo info = mServer.getServerInfo();
        if (info != null)
            firmware = info.firmwareVer;
        if (NASPref.useTwonkyServer && firmware != null && !firmware.equals("")) {
            int version = Integer.parseInt(firmware);
            NASPref.useTwonkyServer = version >= NASPref.useTwonkyMinFirmwareVersion;
        } else {
            NASPref.useTwonkyServer = false;
        }
        Log.d(TAG, "Firmware version : " + firmware + ", Use Twonky Thumbnail : " + NASPref.useTwonkyServer);
    }

    private boolean initAutoBackUpService() {
        isAutoBackupServiceInit = true;
        boolean checked = NASPref.getBackupSetting(this);
        Intent intent = new Intent(this, AutoBackupService.class);
        boolean isRunning = ManageFactory.isServiceRunning(this, AutoBackupService.class);
        if (checked) {
            if (!isRunning) {
                Bundle args = new Bundle();
                getLoaderManager().restartLoader(LoaderID.AUTO_BACKUP, args, this).forceLoad();
                return true;
            }
        } else
            stopService(intent);
        return false;
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initDropdown() {
        mDropdownAdapter = new FileManageDropdownAdapter(false);
        mDropdownAdapter.setOnDropdownItemSelectedListener(this);
        mDropdown = (AppCompatSpinner) findViewById(R.id.main_dropdown);
        mDropdown.setAdapter(mDropdownAdapter);
        mDropdown.setDropDownVerticalOffset(10);
    }

    private void initRecyclerView() {
        FileManageRecyclerAdapter.LayoutType type = NASPref.getFileViewType(this);
        mRecyclerAdapter = new FileManageRecyclerAdapter(mFileList);
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
        /*// expanded fabs
        mFabControl = (FloatingActionButton) findViewById(R.id.main_fab_control);
        mFabControl.setOnClickListener(this);
        mFabNewFolder = (FloatingActionButton) findViewById(R.id.main_fab_new_folder);
        mFabNewFolder.setOnClickListener(this);
        mFabNewFile = (FloatingActionButton) findViewById(R.id.main_fab_new_file);
        mFabNewFile.setOnClickListener(this);
        /*/
        mFab = (FloatingActionButton) findViewById(R.id.main_fab);
        mFab.setOnClickListener(this);
        //*/
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.main_progress_view);
    }

    private void initDrawer() {
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mToggle = new ActionBarDrawerToggle(
                this, mDrawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.setDrawerListener(mToggle);
        mToggle.syncState();
        mToggle.setToolbarNavigationClickListener(this);
        mNavView = (NavigationView) findViewById(R.id.activity_file_manage_drawer);
        mNavView.setNavigationItemSelectedListener(this);
        mNavHeader = mNavView.inflateHeaderView(R.layout.activity_file_manage_drawer_header);
        mNavHeaderTitle = (TextView) mNavHeader.findViewById(R.id.drawer_header_title);
        mNavHeaderSubtitle = (TextView) mNavHeader.findViewById(R.id.drawer_header_subtitle);
        mNavHeaderIcon = (ImageView) mNavHeader.findViewById(R.id.drawer_header_icon);
        mNavHeaderTitle.setText(mServer.getUsername());
        String email = NASPref.getCloudUsername(this);
        if (!email.equals(""))
            mNavHeaderSubtitle.setText(String.format("%s", email));
        else
            mNavHeaderSubtitle.setText(String.format("%s@%s", mServer.getUsername(), mServer.getHostname()));
        mNavView.getMenu().findItem(R.id.nav_disk_info).setVisible(NASPref.defaultUserName.equals(mServer.getUsername()));
        mNavView.getMenu().findItem(R.id.nav_switch).setVisible(NASPref.useSwitchNas);
    }

    private void initActionModeView() {
        mEditorModeView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.action_mode_custom, null);
        mEditorModeTitle = (TextView) mEditorModeView.findViewById(R.id.action_mode_custom_title);
    }

    /**
     * DROPDOWN ITEM CONTROL
     */
    @Override
    public void onDropdownItemSelected(int position) {
        Log.w(TAG, "onDropdownItemSelected: " + position);
        if (position > 0) {
            String path = mDropdownAdapter.getPath(position);
            doLoad(path);
        }
    }


    /**
     * MENU CONTROL
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_manage_viewer, menu);
        if (NASApp.MODE_STG.equals(mMode) || (NASApp.MODE_SMB.equals(mMode) && NASApp.ROOT_SMB.equals(mPath)))
            menu.findItem(R.id.file_manage_viewer_action_upload).setVisible(false);
        FileManageRecyclerAdapter.LayoutType type = NASPref.getFileViewType(this);
        switch (type) {
            case GRID:
                menu.findItem(R.id.file_manage_viewer_action_view).setIcon(R.drawable.ic_view_list_gray_24dp);
                break;
            default:
                menu.findItem(R.id.file_manage_viewer_action_view).setIcon(R.drawable.ic_view_module_gray_24dp);
                break;
        }
        mMediaRouteMenuItem = mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.file_manage_viewer_action_refresh:
                doRefresh();
                break;
            case R.id.file_manage_viewer_action_view:
                doChangeView();
                break;
            case R.id.file_manage_viewer_action_sort:
                doSort();
                break;
            case R.id.file_manage_viewer_action_search:
                doSearch();
                break;
            case R.id.file_manage_viewer_action_upload:
                startFileActionPickerActivity(NASApp.ACT_PICK_UPLOAD);
                break;
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
            FileInfo fileInfo = mFileList.get(position);
            if (FileInfo.TYPE.DIR.equals(fileInfo.type)) {
                doLoad(fileInfo.path);
            } else if (FileInfo.TYPE.PHOTO.equals(fileInfo.type)) {
                startViewerActivity(mMode, mRoot, fileInfo.path);
            } else if (FileInfo.TYPE.VIDEO.equals(fileInfo.type)) {
                startVideoActivity(fileInfo);
            } else if (FileInfo.TYPE.MUSIC.equals(fileInfo.type)) {
                startMusicActivity(mMode, mRoot, fileInfo);
            } else {
                Log.d(TAG, "class name: " + this.getClass().getSimpleName());

                mProgressView.setVisibility(View.VISIBLE);
                NASPref.openFileBy3rdApp(this, fileInfo);
            }
        } else {
            // editor
            selectAtPosition(position);
        }
    }

    @Override
    public void onRecyclerItemLongClick(int position) {
        if (mPath.equals(NASApp.ROOT_SMB))
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
        if (mPath.equals(NASApp.ROOT_SMB))
            return;
        if (mEditorMode == null) {
            startEditorMode();
        }
        selectAtPosition(position);
    }


    /**
     * DRAWER CONTROL
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id != R.id.nav_logout)
            mDrawer.closeDrawer(GravityCompat.START);

        switch (id) {
            case R.id.nav_storage:
                mDevice = false;
                doLoad(NASApp.ROOT_SMB);
                AnalysisFactory.getInstance(this).sendScreen(AnalysisFactory.VIEW.BROWSERREMOTE);
                break;
            case R.id.nav_device:
                mDevice = true;
                doLoad(NASApp.ROOT_STG);
                AnalysisFactory.getInstance(this).sendScreen(AnalysisFactory.VIEW.BROWSERLOCAL);
                break;
            case R.id.nav_downloads:
                mDevice = false;
                doLoad(NASPref.getDownloadLocation(this));
                AnalysisFactory.getInstance(this).sendScreen(AnalysisFactory.VIEW.BROWSERLOCALDOWNLOAD);
                break;
            case R.id.nav_disk_info:
                startDiskInfoActivity();
                break;
            case R.id.nav_settings:
                startSettingsActivity();
                break;
            case R.id.nav_about:
                startAboutActivity();
                break;
            //case R.id.nav_help:
            //    startHelpActivity();
            //    break;
            case R.id.nav_switch:
                startLoginListActivity();
                break;
            case R.id.nav_logout:
                showLogoutDialog();
                break;
        }
        return true;
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
        /*// expanded fabs
        toggleSelectFabs(false);
        /*/
        toggleFabSelectAll(false);
        //*/
        toast(R.string.edit_mode);
        return true;
    }

    private void initView(ActionMode mode) {
        mEditorMode = mode;
        mEditorMode.setCustomView(mEditorModeView);
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    private void initMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_manage_editor, menu);
        MenuItem item = menu.findItem(R.id.file_manage_editor_action_transmission);
        item.setTitle(NASApp.MODE_SMB.equals(mMode) ? R.string.download : R.string.upload);
        item.setIcon(NASApp.MODE_SMB.equals(mMode) ? R.drawable.ic_file_download_white_24dp : R.drawable.ic_file_upload_white_24dp);
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        boolean isEmpty = (getSelectedCount() == 0);
        switch (item.getItemId()) {
            case R.id.file_manage_editor_action_transmission:
                String type = NASApp.MODE_SMB.equals(mMode) ? NASApp.ACT_DOWNLOAD : NASApp.ACT_UPLOAD;
                if (isEmpty) toast(R.string.no_item_selected);
                else startFileActionLocateActivity(type);
                break;
            case R.id.file_manage_editor_action_rename:
                if (isEmpty) toast(R.string.no_item_selected);
                else doRename();
                break;
            case R.id.file_manage_editor_action_share:
                if (isEmpty) toast(R.string.no_item_selected);
                else doShare();
                break;
            case R.id.file_manage_editor_action_copy:
                if (isEmpty) toast(R.string.no_item_selected);
                else startFileActionLocateActivity(NASApp.ACT_COPY);
                break;
            case R.id.file_manage_editor_action_cut:
                if (isEmpty) toast(R.string.no_item_selected);
                else startFileActionLocateActivity(NASApp.ACT_MOVE);
                break;
            case R.id.file_manage_editor_action_delete:
                if (isEmpty) toast(R.string.no_item_selected);
                else doDelete();
                break;
            case R.id.file_manage_editor_action_new_folder:
                doNewFolder();
                break;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        mEditorMode = null;
        clearAllSelection();
        /*/ expanded fabs
        resetActionFabs();
        /*/
        enableFabEdit(true);
        //*/
    }


    /**
     * VIEW CLICK CONTROL
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            /*// expanded fabs
            case R.id.main_fab_control:
                if (mEditorMode == null)
                    toggleActionFabs();
                else
                    toggleSelectAll();
                break;
            case R.id.main_fab_new_folder:
                doNewFolder();
                break;
            case R.id.main_fab_new_file:
                doNewFile();
                break;
            /*/
            case R.id.main_fab:
                if (mEditorMode == null)
                    startEditorMode();
                else
                    toggleSelectAll();
                break;
            //*/
            default:
                if (v instanceof ImageButton && v.getParent() instanceof Toolbar) {
                    // setToolbarNavigationClickListener
                    String parent = new File(mPath).getParent();
                    doLoad(parent);
                }
                break;
        }
    }


    /**
     * SOFT KEY BACK CONTROL
     */
    @Override
    public void onBackPressed() {
        toggleDrawerCheckedItem();
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
            return;
        }
        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
            return;
        }
        if (!ManageFactory.isTopDirectory(mMode, mRoot, mPath)) {
            String parent = new File(mPath).getParent();
            doLoad(parent);
        } else {
            mDrawer.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mDrawer.closeDrawer(GravityCompat.START);
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
     * P2P CONTROL
     */
    @Override
    public void onTunnelStatusChanged(int nErrCode, int nSID) {
        //Log.d("ike", "TEST " + nErrCode + "," + nSID);
    }

    @Override
    public void onTunnelSessionInfoChanged(sP2PTunnelSessionInfo object) {
        //Log.d("ike", "TEST CHANGE ");
    }

    public boolean setRecordCommand(int id, Bundle args) {
        if (id == LoaderID.TUTK_NAS_LINK) {
            Log.w(TAG, "TUTK_NAS_LINK don't need to record");
            return false;
        }

        boolean record = false;
        for (int cmd : RETRY_CMD) {
            if (id == cmd) {
                boolean retry = args.getBoolean("retry");
                if (args != null && !retry) {
                    mPreviousLoaderID = id;
                    mPreviousLoaderArgs = args;
                    record = true;
                }
                break;
            }
        }

        if (!record) {
            cleanRecordCommand();
        }

        Log.w(TAG, "Previous Loader ID: " + id);
        return record;
    }

    public void cleanRecordCommand() {
        mPreviousLoaderID = -1;
        mPreviousLoaderArgs = null;
    }

    /**
     * LOADER CONTROL
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        setRecordCommand(id, args);
        ArrayList<String> paths = args.getStringArrayList("paths");
        String path = args.getString("path");
        String name = args.getString("name");
        switch (mLoaderID = id) {
            case LoaderID.SMB_FILE_LIST:
                mProgressView.setVisibility(View.VISIBLE);
                return new SmbFileListLoader(this, path);
            case LoaderID.LOCAL_FILE_LIST:
                mProgressView.setVisibility(View.VISIBLE);
                return new LocalFileListLoader(this, path);
            case LoaderID.SMB_NEW_FOLDER:
                mProgressView.setVisibility(View.VISIBLE);
                return new SmbFolderCreateLoader(this, path);
            case LoaderID.LOCAL_NEW_FOLDER:
                mProgressView.setVisibility(View.VISIBLE);
                return new LocalFolderCreateLoader(this, path);
            case LoaderID.SMB_FILE_RENAME:
                mProgressView.setVisibility(View.VISIBLE);
                return new SmbFileRenameLoader(this, path, name);
            case LoaderID.LOCAL_FILE_RENAME:
                mProgressView.setVisibility(View.VISIBLE);
                return new LocalFileRenameLoader(this, path, name);
            case LoaderID.SMB_FILE_DELETE:
                mProgressView.setVisibility(View.VISIBLE);
                return new SmbFileDeleteLoader(this, paths);
            case LoaderID.LOCAL_FILE_DELETE:
                mProgressView.setVisibility(View.VISIBLE);
                return new LocalFileDeleteLoader(this, paths);
            case LoaderID.SMB_FILE_COPY:
                return new SmbFileCopyLoader(this, paths, path);
            case LoaderID.LOCAL_FILE_COPY:
                return new LocalFileCopyLoader(this, paths, path);
            case LoaderID.SMB_FILE_MOVE:
                return new SmbFileMoveLoader(this, paths, path);
            case LoaderID.LOCAL_FILE_MOVE:
                return new LocalFileMoveLoader(this, paths, path);
            case LoaderID.SMB_FILE_DOWNLOAD:
                return new SmbFileDownloadLoader(this, paths, path);
            case LoaderID.LOCAL_FILE_UPLOAD:
                return new LocalFileUploadLoader(this, paths, path);
            case LoaderID.TUTK_NAS_LINK:
                return new TutkLinkNasLoader(this, args);
            case LoaderID.TUTK_LOGOUT:
                mProgressView.setVisibility(View.VISIBLE);
                return new TutkLogoutLoader(this);
            case LoaderID.AUTO_BACKUP:
                return new AutoBackupLoader(this);
            case LoaderID.MEDIA_PLAYER:
                mProgressView.setVisibility(View.VISIBLE);
                return new MediaManagerLoader(this, args);
            case LoaderID.EVENT_NOTIFY:
                mProgressView.setVisibility(View.VISIBLE);
                return new EventNotifyLoader(this, args);
            case LoaderID.SMB_FILE_SHARE:
                mSmbFileShareLoader = new SmbFileShareLoader(this, paths, path);
                return mSmbFileShareLoader;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        Log.w(TAG, "onLoaderFinished: " + loader.getClass().getSimpleName() + " " + success);
        if (success) {
            if (loader instanceof SmbFileListLoader) {
                //file list change, stop previous image loader
                ImageLoader.getInstance().stop();
                mMode = NASApp.MODE_SMB;
                mRoot = NASApp.ROOT_SMB;
                mPath = ((SmbFileListLoader) loader).getPath();
                mFileList = ((SmbFileListLoader) loader).getFileList();
                Collections.sort(mFileList, FileInfoSort.comparator(this));
                FileFactory.getInstance().addFolderFilterRule(mPath, mFileList);
                FileFactory.getInstance().addFileTypeSortRule(mFileList);
                closeEditorMode();
                enableFabEdit(!mPath.equals(mRoot));
                updateScreen();
            } else if (loader instanceof LocalFileListLoader) {
                //file list change, stop previous image loader
                ImageLoader.getInstance().stop();
                mMode = NASApp.MODE_STG;
                mRoot = NASApp.ROOT_STG;
                mPath = ((LocalFileListLoader) loader).getPath();
                mFileList = ((LocalFileListLoader) loader).getFileList();
                Collections.sort(mFileList, FileInfoSort.comparator(this));
                FileFactory.getInstance().addFileTypeSortRule(mFileList);
                closeEditorMode();
                enableFabEdit(true);
                updateScreen();
            } else if (loader instanceof TutkLinkNasLoader) {
                Log.w(TAG, "Remote Access connect success, start execute previous loader : " + mPreviousLoaderID);
                if (mPreviousLoaderArgs != null && mPreviousLoaderID >= 0) {
                    mPreviousLoaderArgs.putBoolean("retry", true);
                    getLoaderManager().restartLoader(mPreviousLoaderID, mPreviousLoaderArgs, this).forceLoad();
                    return;
                }
            } else if (loader instanceof TutkLogoutLoader) {
                startSignInActivity(true);
            } else if (loader instanceof EventNotifyLoader) {
                Bundle args = ((EventNotifyLoader) loader).getBundleArgs();
                String path = args.getString("path");
                if (path != null && !path.equals("")) {
                    doLoad(path);
                    return;
                }
            } else if (loader instanceof AutoBackupLoader) {
                //do nothing
            } else if (loader instanceof MediaManagerLoader) {
                Bundle args = ((MediaManagerLoader) loader).getBundleArgs();
                MediaFactory.open(this, args);
            } else if (loader instanceof SmbFileShareLoader) {
                ArrayList<FileInfo> files = ((SmbFileShareLoader) loader).getShareList();
                if (files != null && files.size() > 0)
                    doLocalShare(files);
            } else {
                doRefresh();
                if (loader instanceof SmbAbstractLoader) {
                    String type = ((SmbAbstractLoader) loader).getType();
                    if (type != null && !type.equals("")) {
                        Toast.makeText(FileManageActivity.this, type + " - " + getString(R.string.done), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            if (!LanCheckManager.getInstance().getLanConnect() && mPreviousLoaderID > 0 && mPreviousLoaderArgs != null) {
                if (mLoaderID == LoaderID.TUTK_NAS_LINK) {
                    cleanRecordCommand();
                    LanCheckManager.getInstance().startLanCheck();
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                } else {
                    Bundle args = new Bundle();
                    String uuid = NASPref.getUUID(this);
                    if (uuid == null || "".equals(uuid)) {
                        uuid = NASPref.getCloudUUID(this);
                        if (uuid == null || "".equals(uuid)) {
                            startSignInActivity(false);
                            return;
                        }
                    }
                    args.putString("hostname", uuid);
                    getLoaderManager().restartLoader(LoaderID.TUTK_NAS_LINK, args, this).forceLoad();
                    return;
                }
            } else {
                checkEmptyView();
                if (loader instanceof SmbAbstractLoader) {
                    LanCheckManager.getInstance().startLanCheck();
                    Toast.makeText(this, ((SmbAbstractLoader) loader).getExceptionMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    if (loader instanceof EventNotifyLoader)
                        LanCheckManager.getInstance().startLanCheck();
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            }
        }

        toggleDrawerCheckedItem();
        if (!isAutoBackupServiceInit && initAutoBackUpService()) {
            return;
        }
        mProgressView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        Log.w(TAG, "onLoaderReset: " + loader.getClass().getSimpleName());
    }


    private boolean doEventNotify(boolean update, String path) {
        int id = path.startsWith(NASApp.ROOT_STG)
                ? LoaderID.LOCAL_FILE_LIST : LoaderID.SMB_FILE_LIST;

        Long lastTime = Long.parseLong(NASPref.getSessionVerifiedTime(this));
        Long currTime = System.currentTimeMillis();
        Log.w(TAG, "hash key time check : " + (currTime - lastTime));
        if (id == LoaderID.SMB_FILE_LIST && currTime - lastTime >= 180000) {
            Bundle args = new Bundle();
            args.putString("path", update ? path : "");
            getLoaderManager().restartLoader(LoaderID.EVENT_NOTIFY, args, this).forceLoad();
            Log.w(TAG, "doEventNotify");
            return false;
        }

        return true;
    }

    /**
     * FILE BROWSER
     */
    private void doLoad(String path) {
        int id = path.startsWith(NASApp.ROOT_STG)
                ? LoaderID.LOCAL_FILE_LIST : LoaderID.SMB_FILE_LIST;

        boolean pass = doEventNotify(true, path);
        if (pass) {
            Bundle args = new Bundle();
            args.putString("path", path);
            getLoaderManager().restartLoader(id, args, this).forceLoad();
            Log.w(TAG, "doLoad: " + path);
        }
    }

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

    private void doUpload(String dest, ArrayList<String> paths) {
        int id = LoaderID.LOCAL_FILE_UPLOAD;
        Bundle args = new Bundle();
        args.putStringArrayList("paths", paths);
        args.putString("path", dest);
        getLoaderManager().restartLoader(id, args, FileManageActivity.this).forceLoad();
        Log.w(TAG, "doUpload: " + paths.size() + " item(s) to " + dest);
    }

    private void doDownload(String dest, ArrayList<String> paths) {
        int id = LoaderID.SMB_FILE_DOWNLOAD;
        Bundle args = new Bundle();
        args.putStringArrayList("paths", paths);
        args.putString("path", dest);
        getLoaderManager().restartLoader(id, args, FileManageActivity.this).forceLoad();
        Log.w(TAG, "doDownload: " + paths.size() + " item(s) to " + dest);
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
        final String name = target.name;
        boolean ignoreType = target.type.equals(FileInfo.TYPE.DIR);
        new FileActionRenameDialog(this, ignoreType, name, names) {
            @Override
            public void onConfirm(String newName) {
                if (newName.equals(name))
                    return;
                int id = (NASApp.MODE_SMB.equals(mMode))
                        ? LoaderID.SMB_FILE_RENAME
                        : LoaderID.LOCAL_FILE_RENAME;
                Bundle args = new Bundle();
                args.putString("path", path);
                args.putString("name", newName);
                getLoaderManager().restartLoader(id, args, FileManageActivity.this).forceLoad();
                Log.w(TAG, "doRename: " + path + ", " + newName);
            }
        };
    }

    private void doShare() {
        final int id = (NASApp.MODE_SMB.equals(mMode))
                ? LoaderID.SMB_FILE_SHARE
                : LoaderID.LOCAL_FILE_SHARE;

        ArrayList<FileInfo> files = getSelectedFiles();
        if (id == LoaderID.LOCAL_FILE_SHARE) {
            doLocalShare(files);
        } else {
            Bundle value = new Bundle();
            value.putString(ProgressDialog.DIALOG_TITLE, getString(R.string.share));
            //String format = getResources().getString(files.size() <= 1 ? R.string.msg_file_selected : R.string.msg_files_selected);
            //value.putString(ProgressDialog.DIALOG_MESSAGE, String.format(format, files.size()));
            mShareDialog = new ProgressDialog(this, value) {
                @Override
                public void onConfirm() {
                    Bundle args = new Bundle();
                    args.putStringArrayList("paths", getSelectedPaths());
                    args.putString("path", NASPref.getShareLocation(FileManageActivity.this));
                    getLoaderManager().restartLoader(id, args, FileManageActivity.this).forceLoad();
                    closeEditorMode();
                }

                @Override
                public void onCancel() {
                    getLoaderManager().destroyLoader(id);
                    if (mSmbFileShareLoader != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mSmbFileShareLoader.cancelLoad();
                        }
                        mSmbFileShareLoader = null;
                    }
                    mShareDialog = null;
                }
            };
        }
    }

    private void doLocalShare(ArrayList<FileInfo> files) {
        boolean onlyImage = true;
        ArrayList<Uri> imageUris = new ArrayList<Uri>();
        for (FileInfo file : files) {
            Uri uri = Uri.fromFile(new File(file.path));
            imageUris.add(uri);
            if (!file.type.equals(FileInfo.TYPE.PHOTO))
                onlyImage = false;
        }

        Intent shareIntent = new Intent();
        shareIntent.setType(onlyImage ? "image/*" : "*/*");

        if (imageUris.size() == 1) {
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUris.get(0));
        } else {
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
        }
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share)));
        if (mShareDialog != null) {
            mShareDialog.dismiss();
            mShareDialog = null;
        }

        Log.w(TAG, "doShare: " + files.size() + " item(s)");
    }

    private void doCopy(String dest) {
        ArrayList<String> paths = getSelectedPaths();
        for (String path : paths) {
            if (dest.startsWith(path)) {
                Toast.makeText(this, getString(R.string.select_folder_error), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int id = (NASApp.MODE_SMB.equals(mMode))
                ? LoaderID.SMB_FILE_COPY
                : LoaderID.LOCAL_FILE_COPY;
        Bundle args = new Bundle();
        args.putStringArrayList("paths", paths);
        args.putString("path", dest);
        getLoaderManager().restartLoader(id, args, FileManageActivity.this).forceLoad();
        Log.w(TAG, "doCopy: " + paths.size() + " item(s) to " + dest);
    }

    private void doMove(String dest) {
        ArrayList<String> paths = getSelectedPaths();
        for (String path : paths) {
            if (dest.startsWith(path)) {
                Toast.makeText(this, getString(R.string.select_folder_error), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int id = (NASApp.MODE_SMB.equals(mMode))
                ? LoaderID.SMB_FILE_MOVE
                : LoaderID.LOCAL_FILE_MOVE;
        Bundle args = new Bundle();
        args.putStringArrayList("paths", paths);
        args.putString("path", dest);
        getLoaderManager().restartLoader(id, args, FileManageActivity.this).forceLoad();
        Log.w(TAG, "doMove: " + paths.size() + " item(s) to " + dest);
    }

    private void doDelete() {
        ArrayList<String> paths = new ArrayList<String>();
        FileInfo target = new FileInfo();
        for (FileInfo file : mFileList) {
            if (file.checked)
                paths.add(file.path);
        }
        new FileActionDeleteDialog(this, paths) {
            @Override
            public void onConfirm(ArrayList<String> paths) {
                int id = (NASApp.MODE_SMB.equals(mMode))
                        ? LoaderID.SMB_FILE_DELETE
                        : LoaderID.LOCAL_FILE_DELETE;
                Bundle args = new Bundle();
                args.putStringArrayList("paths", paths);
                getLoaderManager().restartLoader(id, args, FileManageActivity.this).forceLoad();
                Log.w(TAG, "doDelete: " + paths.size() + " items");
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
                int id = (NASApp.MODE_SMB.equals(mMode))
                        ? LoaderID.SMB_NEW_FOLDER
                        : LoaderID.LOCAL_NEW_FOLDER;
                StringBuilder builder = new StringBuilder(mPath);
                if (!mPath.endsWith("/"))
                    builder.append("/");
                builder.append(newName);
                String path = builder.toString();
                Bundle args = new Bundle();
                args.putString("path", path);
                getLoaderManager().restartLoader(id, args, FileManageActivity.this).forceLoad();
                Log.w(TAG, "doNewFolder: " + path);
            }
        };
    }

    /**
     * UX CONTROL
     */
    private void updateScreen() {
        mDropdownAdapter.updateList(mPath, mMode);
        mDropdownAdapter.notifyDataSetChanged();
        mRecyclerAdapter.updateList(mFileList);
        mRecyclerAdapter.notifyDataSetChanged();
        mToggle.setDrawerIndicatorEnabled(mPath.equals(mRoot));
        checkEmptyView();
        invalidateOptionsMenu();
    }

    private void checkEmptyView() {
        if (mFileList != null)
            mRecyclerEmptyView.setVisibility(mFileList.size() == 0 ? View.VISIBLE : View.GONE);
        else
            mRecyclerEmptyView.setVisibility(View.GONE);
    }

    private void updateListView(boolean update) {
        LinearLayoutManager list = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(list);
        if (update) {
            mRecyclerView.getRecycledViewPool().clear();
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }

    private void showLogoutDialog() {
        Bundle value = new Bundle();
        value.putString(NotificationDialog.DIALOG_MESSAGE, getString(R.string.remote_access_logout));
        NotificationDialog mNotificationDialog = new NotificationDialog(this, value) {
            @Override
            public void onConfirm() {
                Bundle args = new Bundle();
                args.putBoolean("clean", true);
                getLoaderManager().restartLoader(LoaderID.TUTK_LOGOUT, args, FileManageActivity.this).forceLoad();
            }

            @Override
            public void onCancel() {

            }
        };
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
        mFab.setImageResource(R.drawable.ic_edit_white_24dp);
        mFab.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
    }

    private void toggleFabSelectAll(boolean selectAll) {
        int resId = selectAll
                ? R.drawable.ic_clear_all_white_24dp
                : R.drawable.ic_done_all_white_24dp;
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

    private void toggleDrawerCheckedItem() {
        int id = NASApp.MODE_SMB.equals(mMode) ? R.id.nav_storage :
                mDevice ? R.id.nav_device : R.id.nav_downloads;
        mNavView.setCheckedItem(id);
        Log.w(TAG, "toggleDrawerCheckedItem: " + mMode);
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
                : mMode;
        String root
                = NASApp.ACT_UPLOAD.equals(type) ? NASApp.ROOT_SMB
                : NASApp.ACT_DOWNLOAD.equals(type) ? NASApp.ROOT_STG
                : mRoot;
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
            builder.setIcon(R.drawable.ic_file_download_gray_24dp);
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
        if (NASApp.ACT_PICK_UPLOAD.equals(type) && NASApp.MODE_SMB.equals(mMode) && NASApp.ROOT_SMB.equals(mPath)) {
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
            } catch (TransientNetworkDisconnectionException e) {
                e.printStackTrace();
            } catch (NoConnectionException e) {
                e.printStackTrace();
            }
            MediaInfo info = MediaFactory.createMediaInfo(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK, fileInfo.path);
            mCastManager.startVideoCastControllerActivity(this, info, 0, true);
        } else {
            ArrayList<FileInfo> list = new ArrayList<FileInfo>();
            for (FileInfo info : mFileList) {
                if (FileInfo.TYPE.MUSIC.equals(info.type) && MusicActivity.checkFormatSupportOrNot(info.path)) {
                    list.add(info);
                }
            }
            FileFactory.getInstance().setMusicList(list);

            Bundle args = new Bundle();
            args.putString("path", fileInfo.path);
            args.putString("mode", mode);
            args.putString("root", root);
            Intent intent = new Intent();
            intent.setClass(FileManageActivity.this, MusicActivity.class);
            intent.putExtras(args);
            startActivityForResult(intent, MusicActivity.REQUEST_CODE);
        }
    }

    private void startVideoActivity(FileInfo fileInfo) {
        if (!fileInfo.path.startsWith(NASApp.ROOT_STG) && mCastManager != null && mCastManager.isConnected()) {
            try {
                //clean image
                mCastManager.sendDataMessage("close");
            } catch (TransientNetworkDisconnectionException e) {
                e.printStackTrace();
            } catch (NoConnectionException e) {
                e.printStackTrace();
            }
            MediaInfo info = MediaFactory.createMediaInfo(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK, fileInfo.path);
            mCastManager.startVideoCastControllerActivity(this, info, 0, true);
        } else {
            MediaFactory.open(this, fileInfo.path);
        }
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
        startActivityForResult(intent, ViewerActivity.REQUEST_CODE);
    }

    private void startFileInfoActivity(FileInfo info) {
        Bundle args = new Bundle();
        args.putSerializable("info", info);
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, FileInfoActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, FileInfoActivity.REQUEST_CODE);
    }

    private void startDiskInfoActivity() {
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, DiskInfoActivity.class);
        startActivityForResult(intent, DiskInfoActivity.REQUEST_CODE);
    }

    private void startSettingsActivity() {
        Intent intent = new Intent();
        if (NASPref.useNewLoginFlow) {
            intent.setClass(FileManageActivity.this, NewSettingsActivity.class);
            startActivityForResult(intent, NewSettingsActivity.REQUEST_CODE);
        } else {
            intent.setClass(FileManageActivity.this, SettingsActivity.class);
            startActivityForResult(intent, SettingsActivity.REQUEST_CODE);
        }
    }

    private void startAboutActivity() {
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, AboutActivity.class);
        startActivityForResult(intent, AboutActivity.REQUEST_CODE);
    }

    private void startHelpActivity() {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://help.storejetcloud.com.s3-website-ap-northeast-1.amazonaws.com/TW/start.html"));
        startActivity(i);
    }

    private void startLoginListActivity() {
        Intent intent = new Intent();
        intent.putExtra("uuid", NASPref.getCloudUUID(this));
        intent.setClass(FileManageActivity.this, LoginListActivity.class);
        startActivityForResult(intent, LoginListActivity.REQUEST_CODE);
    }

    private void startSignInActivity(boolean clear) {
        boolean isRunning = false;

        //clean email and account information
        if (clear) {
            if (NASPref.useFacebookLogin && NASPref.getFBAccountStatus(this))
                NASPref.logOutFB();
            NASPref.clearDataAfterLogout(this);
        }

        //stop auto backup service
        isRunning = ManageFactory.isServiceRunning(this, AutoBackupService.class);
        if (isRunning) {
            Intent intent = new Intent(FileManageActivity.this, AutoBackupService.class);
            stopService(intent);
        }

        //stop music service
        isRunning = ManageFactory.isServiceRunning(this, MusicService.class);
        if (isRunning) {
            Intent intent = new Intent(FileManageActivity.this, MusicService.class);
            stopService(intent);
        }

        //clean disk info
        DiskFactory.getInstance().cleanDiskDevices();

        //clean path map
        FileFactory.getInstance().cleanRealPathMap();

        //clean twonky map
        TwonkyManager.getInstance().cleanTwonkyMap();

        //clean lan check
        LanCheckManager.getInstance().setLanConnect(false, "");

        //show SignIn activity
        Intent intent = new Intent();
        if (NASPref.useNewLoginFlow)
            intent.setClass(FileManageActivity.this, LoginActivity.class);
        else
            intent.setClass(FileManageActivity.this, StartActivity.class);
        startActivity(intent);
        finish();
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

