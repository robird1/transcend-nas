package com.transcend.nas.management;

import android.app.ActivityManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
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
import com.nostra13.universalimageloader.core.ImageLoader;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.common.NotificationDialog;
import com.transcend.nas.connection.SignInActivity;
import com.transcend.nas.service.AutoBackupService;
import com.transcend.nas.settings.AboutActivity;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.settings.SettingsActivity;
import com.transcend.nas.utils.FileFactory;
import com.transcend.nas.utils.MediaFactory;
import com.transcend.nas.viewer.ViewerActivity;
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

    private String mMode;
    private String mRoot;
    private String mPath;
    private ArrayList<FileInfo> mFileList;
    private Server mServer;
    private int mLoaderID;
    private int mPreviousLoaderID = -1;
    private Bundle mPreviousLoaderArgs = null;
    private boolean isAutoBackupServiceInit = false;

    private VideoCastManager mCastManager;
    private VideoCastConsumer mCastConsumer;
    private MenuItem mMediaRouteMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate");
        setContentView(R.layout.activity_file_manage);
        init();
        initToolbar();
        initDropdown();
        initRecyclerView();
        initFabs();
        initProgressView();
        initDrawer();
        initActionModeView();
        doRefresh();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.w(TAG, "onStart");
        //doRefresh();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.w(TAG, "onRestart");
    }

    @Override
    protected void onResume() {
        mCastManager = VideoCastManager.getInstance();
        if (null != mCastManager) {
            mCastManager.addVideoCastConsumer(mCastConsumer);
            mCastManager.incrementUiCounter();
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
        super.onStop();
        Log.w(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        P2PService.getInstance().removeP2PListener(this);
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
                    doUpload(path);
                if (NASApp.ACT_DOWNLOAD.equals(type))
                    doDownload(path);
                if (NASApp.ACT_COPY.equals(type))
                    doCopy(path);
                if (NASApp.ACT_MOVE.equals(type))
                    doMove(path);
                closeEditorMode();
            }
        }
        else if(requestCode == ViewerActivity.REQUEST_CODE){
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle == null) return;
                boolean delete = bundle.getBoolean("delete");
                if(delete)
                    doRefresh();
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return mCastManager.onDispatchVolumeKeyEvent(event, 0.05) || super.dispatchKeyEvent(event);
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

    }

    private boolean initAutoBackUpService() {
        isAutoBackupServiceInit = true;
        boolean checked = NASPref.getBackupSetting(this);
        Intent intent = new Intent(this, AutoBackupService.class);
        boolean isRunning = isMyServiceRunning(AutoBackupService.class);
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

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
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
        if (isRemoteAccess() && !email.equals(""))
            mNavHeaderSubtitle.setText(String.format("%s", email));
        else
            mNavHeaderSubtitle.setText(String.format("%s@%s", mServer.getUsername(), mServer.getHostname()));
    }

    private void initActionModeView() {
        mEditorModeView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.action_mode_custom, null);
        mEditorModeTitle = (TextView) mEditorModeView.findViewById(R.id.action_mode_custom_title);
    }

    private boolean isRemoteAccess() {
        String hostname = ServerManager.INSTANCE.getCurrentServer().getHostname();
        String p2pIP = P2PService.getInstance().getP2PIP();
        return hostname.contains(p2pIP);
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
                startVideoActivity(fileInfo);
            } else {
                toast(R.string.unknown_format);
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
        /*/ expanded fabs
        else {
            closeEditorMode();
        }
        //*/
    }

    @Override
    public void onRecyclerItemInfoClick(int position) {
        startFileInfoActivity(mFileList.get(position));
    }


    /**
     * DRAWER CONTROL
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();
        if(id != R.id.nav_logout)
            mDrawer.closeDrawer(GravityCompat.START);

        switch (id) {
            case R.id.nav_storage:
                /*/ expanded fabs
                resetActionFabs();
                /*/
                //*/
                doLoad(NASApp.ROOT_SMB);
                break;
            case R.id.nav_downloads:
                /*/ expanded fabs
                resetActionFabs();
                /*/
                //*/
                doLoad(NASPref.getDownloadLocation(this));
                break;
            case R.id.nav_settings:
                startSettingsActivity();
                break;
            case R.id.nav_about:
                startAboutActivity();
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
        if (!isOnTop()) {
            String parent = new File(mPath).getParent();
            doLoad(parent);
        } else {
            mDrawer.openDrawer(GravityCompat.START);
        }
    }

    private boolean isOnTop() {
        if (NASApp.MODE_SMB.equals(mMode)) {
            return mPath.equals(mRoot);
        } else {
            File root = new File(mRoot);
            File file = new File(mPath);
            return file.equals(root);
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

    private void setRecordLoader(int id, Bundle args) {
        mPreviousLoaderID = id;
        mPreviousLoaderArgs = args;
    }

    /**
     * LOADER CONTROL
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        if (LoaderID.SMB_MIN_COMMAND <= id && id <= LoaderID.SMB_MAX_COMMAND)
            setRecordLoader(id, args);

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
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof SmbFileListLoader) {
            if (success) {
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
            }
        } else if (loader instanceof LocalFileListLoader) {
            if (success) {
                //file list change, stop previous image loader
                ImageLoader.getInstance().stop();

                mMode = NASApp.MODE_STG;
                mRoot = NASApp.ROOT_STG;
                mPath = ((LocalFileListLoader) loader).getPath();
                mFileList = ((LocalFileListLoader) loader).getFileList();
                Collections.sort(mFileList, FileInfoSort.comparator(this));
                FileFactory.getInstance().addFileTypeSortRule(mFileList);
                closeEditorMode();
                /*// expanded fabs
                resetActionFabs();
                /*/
                enableFabEdit(true);
                updateScreen();
            }
        } else if (loader instanceof TutkLinkNasLoader) {
            TutkLinkNasLoader linkLoader = (TutkLinkNasLoader) loader;
            if (!success) {
                Log.w(TAG, "Remote Access connect fail: " + linkLoader.getError());
                Toast.makeText(this, linkLoader.getError(), Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "Remote Access connect success, start execute previous loader");
                if (mPreviousLoaderArgs != null && mPreviousLoaderID >= 0) {
                    mPreviousLoaderArgs.putBoolean("retry", true);
                    getLoaderManager().restartLoader(mPreviousLoaderID, mPreviousLoaderArgs, this).forceLoad();
                    mPreviousLoaderID = -1;
                    mPreviousLoaderArgs = null;
                    return;
                }
            }
        } else if (loader instanceof TutkLogoutLoader) {
            startSignInActivity(true);
        } else if (loader instanceof AutoBackupLoader) {
            //do nothing
        } else if (loader instanceof MediaManagerLoader) {
            if(!success){
                Toast.makeText(this,getString(R.string.network_error),Toast.LENGTH_SHORT).show();
            }
            else {
                Bundle args =  ((MediaManagerLoader) loader).getBundleArgs();
                MediaFactory.open(this, args);
            }
        } else {
            if (success) {
                doRefresh();
            }
        }

        if (!success && LoaderID.SMB_MIN_COMMAND <= mLoaderID && mLoaderID <= LoaderID.SMB_MAX_COMMAND) {
            if (isRemoteAccess() && mPreviousLoaderArgs != null && !mPreviousLoaderArgs.getBoolean("retry")) {
                Log.w(TAG, "Remote Access connect fail, try reConnect");
                Bundle args = new Bundle();
                String uuid = P2PService.getInstance().getTUTKUUID();
                if(uuid == null || "".equals(uuid)) {
                    uuid = NASPref.getUUID(this);
                    if(uuid == null || "".equals(uuid)) {
                        uuid = NASPref.getCloudUUID(this);
                        if(uuid == null || "".equals(uuid)) {
                            startSignInActivity(false);
                            return;
                        }
                    }
                }
                args.putString("hostname", uuid);
                getLoaderManager().restartLoader(LoaderID.TUTK_NAS_LINK, args, this).forceLoad();
                return;
            } else {
                checkEmptyView();
                if(loader instanceof SmbAbstractLoader)
                    Toast.makeText(this, ((SmbAbstractLoader) loader).getExceptionMessage(), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
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


    /**
     * FILE BROWSER
     */
    private void doLoad(String path) {
        int id = path.startsWith(NASApp.ROOT_STG)
                ? LoaderID.LOCAL_FILE_LIST : LoaderID.SMB_FILE_LIST;
        Bundle args = new Bundle();
        args.putString("path", path);
        getLoaderManager().restartLoader(id, args, this).forceLoad();
        Log.w(TAG, "doLoad: " + path);
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

    private void doUpload(String dest) {
        ArrayList<String> paths = getSelectedPaths();
        int id = LoaderID.LOCAL_FILE_UPLOAD;
        Bundle args = new Bundle();
        args.putStringArrayList("paths", paths);
        args.putString("path", dest);
        getLoaderManager().restartLoader(id, args, FileManageActivity.this).forceLoad();
        Log.w(TAG, "doUpload: " + paths.size() + " item(s) to " + dest);
    }

    private void doDownload(String dest) {
        ArrayList<String> paths = getSelectedPaths();
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
        new FileActionRenameDialog(this, name, names) {
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
        // TODO: share local file with data
        // TODO: share remote file with link
        toast(R.string.share);
    }

    private void doCopy(String dest) {
        ArrayList<String> paths = getSelectedPaths();
        for(String path : paths){
            if(dest.startsWith(path)) {
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
        for(String path : paths){
            if(dest.startsWith(path)) {
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
    }

    private void checkEmptyView(){
        if(mFileList != null)
            mRecyclerEmptyView.setVisibility(mFileList.size() == 0 ? View.VISIBLE : View.GONE);
        else
            mRecyclerEmptyView.setVisibility(View.GONE);
    }

    private void updateListView(boolean update) {
        LinearLayoutManager list = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(list);
        if(update) {
            mRecyclerView.getRecycledViewPool().clear();
            mRecyclerAdapter.notifyDataSetChanged();
        }
    }

    private void showLogoutDialog() {
        Bundle value = new Bundle();
        value.putString(NotificationDialog.DIALOG_MESSAGE, getString(R.string.nas_logout));
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
        if(update) {
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

    /*// expanded fabs
    private void toggleActionFabs() {
        boolean isShown = (mFabNewFolder.isShown() && mFabNewFile.isShown());
        int visibility = isShown ? View.INVISIBLE : View.VISIBLE;
        int resId = isShown ? R.drawable.ic_add_white_24dp : R.drawable.ic_close_white_24dp;
        mFabControl.setImageResource(resId);
        mFabNewFolder.setVisibility(visibility);
        mFabNewFile.setVisibility(visibility);
    }

    private void resetActionFabs() {
        int resId = (NASApp.MODE_SMB.equals(mMode))
                ? R.drawable.ic_file_upload_white_24dp
                : R.drawable.ic_file_download_white_24dp;
        mFabNewFile.setImageResource(resId);
        mFabControl.setImageResource(R.drawable.ic_add_white_24dp);
        mFabControl.setVisibility(View.VISIBLE);
        mFabNewFolder.setVisibility(View.INVISIBLE);
        mFabNewFile.setVisibility(View.INVISIBLE);
    }

    private void hideActionFabs() {
        mFabControl.setVisibility(View.INVISIBLE);
        mFabNewFolder.setVisibility(View.INVISIBLE);
        mFabNewFile.setVisibility(View.INVISIBLE);
    }

    private void toggleSelectFabs (boolean selectAll) {
        int resId = selectAll
                ? R.drawable.ic_clear_all_white_24dp
                : R.drawable.ic_done_all_white_24dp;
        mFabControl.setImageResource(resId);
        mFabControl.setVisibility(View.VISIBLE);
        mFabNewFolder.setVisibility(View.INVISIBLE);
        mFabNewFile.setVisibility(View.INVISIBLE);
    }
    /*/

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
        String format = getResources().getString(R.string.conj_selected);
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
        /*/ expanded fabs
        toggleSelectFabs(selectAll);
        /*/
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
        /*/ expanded fabs
        toggleSelectFabs(selectAll);
        /*/
        toggleFabSelectAll(selectAll);
        //*/
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
        mEditorMode.getMenu().findItem(R.id.file_manage_editor_action_rename).setVisible(visible);
        mEditorMode.getMenu().findItem(R.id.file_manage_editor_action_share).setVisible(visible);
    }

    private void toggleDrawerCheckedItem() {
        int id = NASApp.MODE_SMB.equals(mMode)
                ? R.id.nav_storage : R.id.nav_downloads;
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
        String path
                = NASApp.ACT_UPLOAD.equals(type) ? NASApp.ROOT_SMB
                : NASApp.ACT_DOWNLOAD.equals(type) ? NASPref.getDownloadLocation(this)
                : mPath;
        Bundle args = new Bundle();
        args.putString("mode", mode);
        args.putString("type", type);
        args.putString("root", root);
        args.putString("path", path);
        //args.putSerializable("list", mFileList);
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, FileActionLocateActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
    }

    private void startVideoActivity(FileInfo fileInfo){
        if (!fileInfo.path.startsWith(NASApp.ROOT_STG) && mCastManager != null && mCastManager.isConnected()) {
            MediaInfo info = MediaFactory.createMediaInfo(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK, fileInfo.path);
            mCastManager.startVideoCastControllerActivity(this, info, 0, true);
        } else {
            MediaFactory.open(this, fileInfo.path);
            /*Server server = ServerManager.INSTANCE.getCurrentServer();
            if(!fileInfo.path.startsWith(NASApp.ROOT_STG) && isRemoteAccess()) {
                Bundle args = new Bundle();
                args.putString("path", MediaFactory.createTranslatePath(fileInfo.path));
                args.putString("name", MediaFactory.parseName(fileInfo.name));
                args.putString("type", MimeUtil.getMimeType(fileInfo.path));
                getLoaderManager().restartLoader(LoaderID.MEDIA_PLAYER, args, this).forceLoad();
            }
            else{
                MediaFactory.open(this, fileInfo.path);
            }*/
        }
    }

    private void startViewerActivity(String mode, String root, String path) {
        ArrayList<FileInfo> list = new ArrayList<FileInfo>();
        for (FileInfo info : mFileList) {
            if (FileInfo.TYPE.PHOTO.equals(info.type))
                list.add(info);
        }
        Bundle args = new Bundle();
        args.putString("path", path);
        args.putString("mode", mode);
        args.putString("root", root);
        args.putSerializable("list", list);
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
        startActivity(intent);
    }

    private void startSettingsActivity() {
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    private void startAboutActivity() {
        //TODO: implement about page
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, AboutActivity.class);
        startActivity(intent);
    }

    private void startSignInActivity(boolean clear) {
        boolean isRunning = false;

        //stop auto backup service
        isRunning = isMyServiceRunning(AutoBackupService.class);
        if (isRunning) {
            Intent intent = new Intent(FileManageActivity.this, AutoBackupService.class);
            stopService(intent);
        }

        //clean hostname, account, password, token
        if (clear) {
            NASPref.setHostname(this, "");
            NASPref.setPassword(this, "");
            NASPref.setUUID(this, "");
            String[] scenarios = getResources().getStringArray(R.array.backup_scenario_values);
            NASPref.setBackupScenario(this, scenarios[1]);
            NASPref.setBackupSetting(this, false);
        }

        //show SignIn activity
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, SignInActivity.class);
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
