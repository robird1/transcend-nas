package com.transcend.nas.management;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.viewer.ViewerActivity;
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

    private static final int GRID_PORTRAIT  = 3;
    private static final int GRID_LANDSCAPE = 5;

    private static final String ROOT_SMB = "/";

    private enum State {
        REMOTE,
        LOCAL
    }

    private Toolbar mToolbar;
    private AppCompatSpinner mDropdown;
    private FileManageDropdownAdapter mDropdownAdapter;
    private RecyclerView mRecyclerView;
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

    private State mState;
    private String mRoot;
    private String mPath;
    private ArrayList<FileInfo> mFileList;
    private Server mServer;
    private int mLoaderID;


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
        super.onResume();
        Log.w(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.w(TAG, "onPause");
        //closeEditorMode();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "onDestroy");
        // TODO: P2P case
        //P2PService.getInstance().P2PListenerRemove();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.w(TAG, "onNewIntent");
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String type = bundle.getString("type");
            String path = bundle.getString("path");
            if (FileAction.UPLOAD.equals(type))
                doUpload(path);
            if (FileAction.DOWNLOAD.equals(type))
                doDownload(path);
            if (FileAction.COPY.equals(type))
                doCopy(path);
            if (FileAction.MOVE.equals(type))
                doMove(path);
            closeEditorMode();
        }
    }


    /**
     *
     * INITIALIZATION
     *
     */
    private void init() {
        mState = State.REMOTE;
        mPath = mRoot = ROOT_SMB;
        mFileList = new ArrayList<FileInfo>();
        mServer = ServerManager.INSTANCE.getCurrentServer();
        // TODO: P2P case
        //P2PService.getInstance().P2PListenerAdd(this);
    }

    private void initToolbar() {
        mToolbar = (Toolbar)findViewById(R.id.main_toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initDropdown() {
        mDropdownAdapter = new FileManageDropdownAdapter(mPath);
        mDropdownAdapter.setOnDropdownItemSelectedListener(this);
        mDropdown = (AppCompatSpinner)findViewById(R.id.main_dropdown);
        mDropdown.setAdapter(mDropdownAdapter);
    }

    private void initRecyclerView() {
        mRecyclerAdapter = new FileManageRecyclerAdapter(mFileList);
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);
        mRecyclerView = (RecyclerView)findViewById(R.id.main_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
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
        mProgressView = (RelativeLayout)findViewById(R.id.main_progress_view);
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
        mNavHeaderSubtitle.setText(String.format("%s@%s", mServer.getUsername(), mServer.getHostname()));
    }

    private void initActionModeView() {
        mEditorModeView = (RelativeLayout)LayoutInflater.from(this).inflate(R.layout.action_mode_custom, null);
        mEditorModeTitle = (TextView) mEditorModeView.findViewById(R.id.action_mode_custom_title);
    }


    /**
     *
     * DROPDOWN ITEM CONTROL
     *
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
     *
     * MENU CONTROL
     *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_manage_viewer, menu);
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
                toast(R.string.search);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     *
     * RECYCLER ITEM CONTROL
     *
     */
    @Override
    public void onRecyclerItemClick(int position) {
        if (mEditorMode == null) {
            // browser
            FileInfo fileInfo = mFileList.get(position);
            if (FileInfo.TYPE.DIR.equals(fileInfo.type)) {
                doLoad(fileInfo.path);
            } else
            if (FileInfo.TYPE.PHOTO.equals(fileInfo.type)) {
                startViewerActivity(fileInfo.path);
            } else
            if (FileInfo.TYPE.VIDEO.equals(fileInfo.type)) {
                MediaManager.open(this, fileInfo.path);
            } else
            if (FileInfo.TYPE.MUSIC.equals(fileInfo.type)) {
                MediaManager.open(this, fileInfo.path);
            } else {
                toast(R.string.unsupported_format);
            }
        }
        else {
            // editor
            selectAtPosition(position);
        }
    }

    @Override
    public void onRecyclerItemLongClick(int position) {
        if (mPath.equals(ROOT_SMB))
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


    /**
     *
     * DRAWER CONTROL
     *
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        mDrawer.closeDrawer(GravityCompat.START);
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_storage:
                /*/ expanded fabs
                resetActionFabs();
                /*/
                //*/
                doLoad(ROOT_SMB);
                break;
            case R.id.nav_downloads:
                /*/ expanded fabs
                resetActionFabs();
                /*/
                //*/
                doLoad(NASPref.getDownloadsPath(this));
                break;
            case R.id.nav_settings:
                // TODO: settings
                break;
            case R.id.nav_help:
                // TODO: help
                break;
        }
        return true;
    }


    /**
     *
     * ACTION MODE CONTROL
     *
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
        return true ;
    }

    private void initView(ActionMode mode) {
        mEditorMode = mode;
        mEditorMode.setCustomView(mEditorModeView);
        mDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    private void initMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_manage_editor, menu);
        MenuItem item = menu.findItem(R.id.file_manage_editor_action_transmission);
        item.setTitle(mState.equals(State.REMOTE) ? R.string.download : R.string.upload);
        item.setIcon(mState.equals(State.REMOTE) ? R.drawable.ic_file_download_white_24dp : R.drawable.ic_file_upload_white_24dp);
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
                String type = mState.equals(State.REMOTE) ? FileAction.DOWNLOAD : FileAction.UPLOAD;
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
                else startFileActionLocateActivity(FileAction.COPY);
                break;
            case R.id.file_manage_editor_action_cut:
                if (isEmpty) toast(R.string.no_item_selected);
                else startFileActionLocateActivity(FileAction.MOVE);
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
     *
     * VIEW CLICK CONTROL
     *
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
     *
     * SOFT KEY BACK CONTROL
     *
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
        }
        else {
            mDrawer.openDrawer(GravityCompat.START);
        }
    }

    private boolean isOnTop() {
        if (mState.equals(State.REMOTE)) {
            return mPath.equals(mRoot);
        }
        else {
            File root = new File(NASPref.getDownloadsPath(this));
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
            updateGridView();
        resizeToolbar();
    }


    /**
     *
     * P2P CONTROL
     *
     */
    @Override
    public void onTunnelStatusChanged(int nErrCode, int nSID) {

    }

    @Override
    public void onTunnelSessionInfoChanged(sP2PTunnelSessionInfo object) {

    }


    /**
     *
     * LOADER CONTROL
     *
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
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
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof SmbFileListLoader) {
            if (success) {
                mState = State.REMOTE;
                mRoot = ROOT_SMB;
                mPath = ((SmbFileListLoader) loader).getPath();
                mFileList = ((SmbFileListLoader) loader).getFileList();
                Collections.sort(mFileList, FileInfoSort.comparator(this));
                closeEditorMode();
                /*// expanded fabs
                if (mPath.equals(mRoot))
                    hideActionFabs();
                else
                    resetActionFabs();
                /*/
                enableFabEdit(!mPath.equals(mRoot));
                //*/
                updateScreen();
            }
        } else
        if (loader instanceof LocalFileListLoader) {
            if (success) {
                mState = State.LOCAL;
                mRoot = NASPref.getDownloadsPath(this);
                mPath = ((LocalFileListLoader) loader).getPath();
                mFileList = ((LocalFileListLoader) loader).getFileList();
                Collections.sort(mFileList, FileInfoSort.comparator(this));
                closeEditorMode();
                /*// expanded fabs
                resetActionFabs();
                /*/
                enableFabEdit(true);
                //*/
                updateScreen();
            }
        }
        else {
            if (success) {
                doRefresh();
            }
        }
        toggleDrawerCheckedItem();
        mProgressView.setVisibility(View.INVISIBLE);
        Log.w(TAG, loader.getClass().getSimpleName() + " " + success);
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        Log.w(TAG, "onLoaderReset: " + loader.getClass().getSimpleName());
    }


    /**
     *
     * FILE BROWSER
     *
     */
    private void doLoad(String path) {
        int id = path.startsWith(NASPref.getDownloadsPath(this))
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
        if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager)
            updateListView();
        else
            updateGridView();
    }

    private void doSort() {
        new FileActionSortDialog(this) {
            @Override
            public void onConfirm() {
                Collections.sort(mFileList, FileInfoSort.comparator(FileManageActivity.this));
                mRecyclerAdapter.updateList(mFileList);
                mRecyclerAdapter.notifyDataSetChanged();
            }
        };
    }

    private void doNewFile() {
        // TODO: upload / download
    }


    /**
     *
     * FILE EDITOR
     *
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
                int id = (mState.equals(State.REMOTE))
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
        int id = (mState.equals(State.REMOTE))
                ? LoaderID.SMB_FILE_COPY
                : LoaderID.LOCAL_FILE_COPY;
        ArrayList<String> paths = getSelectedPaths();
        Bundle args = new Bundle();
        args.putStringArrayList("paths", paths);
        args.putString("path", dest);
        getLoaderManager().restartLoader(id, args, FileManageActivity.this).forceLoad();
        Log.w(TAG, "doCopy: " + paths.size() + " item(s) to " + dest);
    }

    private void doMove(String dest) {
        int id = (mState.equals(State.REMOTE))
                ? LoaderID.SMB_FILE_MOVE
                : LoaderID.LOCAL_FILE_MOVE;
        ArrayList<String> paths = getSelectedPaths();
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
                int id = (mState.equals(State.REMOTE))
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
                int id = (mState.equals(State.REMOTE))
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
     *
     * UX CONTROL
     *
     */
    private void updateScreen() {
        mDropdownAdapter.updateList(mPath);
        mDropdownAdapter.notifyDataSetChanged();
        mRecyclerAdapter.updateList(mFileList);
        mRecyclerAdapter.notifyDataSetChanged();
        mToggle.setDrawerIndicatorEnabled(mPath.equals(mRoot));
    }

    private void updateListView() {
        LinearLayoutManager list = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(list);
        mRecyclerView.getRecycledViewPool().clear();
        mRecyclerAdapter.notifyDataSetChanged();
    }

    private void updateGridView() {
        int orientation = getResources().getConfiguration().orientation;
        int spanCount = (orientation == Configuration.ORIENTATION_PORTRAIT)
                ? GRID_PORTRAIT : GRID_LANDSCAPE;
        GridLayoutManager grid = new GridLayoutManager(this, spanCount);
        grid.setSpanSizeLookup(new SpanSizeLookup(grid.getSpanCount()));
        mRecyclerView.setLayoutManager(grid);
        mRecyclerView.getRecycledViewPool().clear();
        mRecyclerAdapter.notifyDataSetChanged();
    }

    private void resizeToolbar() {
        TypedValue typedValue = new TypedValue();
        int[] attr = new int[]{ R.attr.actionBarSize };
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
        int resId = (mState == State.REMOTE)
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

    private void toggleFabSelectAll (boolean selectAll) {
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
        }
        else {
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
        int id = State.REMOTE.equals(mState)
                ? R.id.nav_storage : R.id.nav_downloads;
        mNavView.setCheckedItem(id);
        Log.w(TAG, "toggleDrawerCheckedItem: " + mState);
    }

    private void toast(int resId) {
        if (mToast != null)
            mToast.cancel();
        mToast = Toast.makeText(this, resId, Toast.LENGTH_SHORT);
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    private void startFileActionLocateActivity(String type) {
        String root
                = FileAction.UPLOAD.equals(type) ? ROOT_SMB
                : FileAction.DOWNLOAD.equals(type) ? NASPref.getDownloadsPath(this)
                : mRoot;
        String path
                = FileAction.UPLOAD.equals(type) ? ROOT_SMB
                : FileAction.DOWNLOAD.equals(type) ? NASPref.getDownloadsPath(this)
                : mPath;

        Bundle args = new Bundle();
        args.putString("type", type);
        args.putString("root", root);
        args.putString("path", path);
        args.putSerializable("list", mFileList);
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, FileActionLocateActivity.class);
        intent.putExtras(args);
        startActivity(intent);
    }

    private void startViewerActivity(String path) {
        ArrayList<String> list = new ArrayList<String>();
        for (FileInfo info : mFileList) {
            if (FileInfo.TYPE.PHOTO.equals(info.type))
                list.add(info.path);
        }
        Bundle args = new Bundle();
        args.putString("stat", mState.toString());
        args.putString("path", path);
        args.putStringArrayList("list", list);
        Intent intent = new Intent();
        intent.setClass(FileManageActivity.this, ViewerActivity.class);
        intent.putExtras(args);
        startActivity(intent);
    }


    /**
     *
     * GRID LAYOUT MANAGER SPAN SIZE LOOKUP
     *
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
