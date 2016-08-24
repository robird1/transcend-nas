package com.transcend.nas.management;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.FileFactory;
import com.transcend.nas.common.MediaFactory;
import com.transcend.nas.viewer.music.MusicActivity;
import com.transcend.nas.viewer.photo.ViewerActivity;
import com.tutk.IOTC.P2PService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FileSharedActivity extends AppCompatActivity implements
        FileManageDropdownAdapter.OnDropdownItemSelectedListener,
        FileManageRecyclerAdapter.OnRecyclerItemCallbackListener,
        LoaderManager.LoaderCallbacks<Boolean> {

    private static final String TAG = FileSharedActivity.class.getSimpleName();
    private static final int GRID_PORTRAIT = 3;
    private static final int GRID_LANDSCAPE = 5;
    private static final Boolean ForceClose = true;

    private int[] RETRY_CMD = new int[]{LoaderID.SMB_FILE_LIST, LoaderID.EVENT_NOTIFY};
    private Toolbar mToolbar;
    private AppCompatSpinner mDropdown;
    private FileManageDropdownAdapter mDropdownAdapter;
    private RecyclerView mRecyclerView;
    private LinearLayout mRecyclerEmptyView;
    private FileManageRecyclerAdapter mRecyclerAdapter;
    private RelativeLayout mProgressView;
    private Toast mToast;

    private String mMode;
    private String mRoot;
    private String mPath;
    private ArrayList<FileInfo> mFileList;
    private int mLoaderID;
    private int mPreviousLoaderID = -1;
    private Bundle mPreviousLoaderArgs = null;
    private ArrayList<String> mImageUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate");
        setContentView(R.layout.activity_file_shared);
        String password = NASPref.getPassword(this);
        if (password != null && !password.equals("")) {
            init();
            initToolbar();
            initDropdown();
            initRecyclerView();
            initProgressView();
            Intent intent = getIntent();
            onReceiveIntent(intent);
        } else {
            Toast.makeText(this, getString(R.string.login_not), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        onReceiveIntent(intent);
        Log.w(TAG, "onNewIntent");
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
                if (NASApp.ACT_UPLOAD.equals(type)) {
                    doUpload(path, getSelectedPaths());
                }

                if (ForceClose)
                    finish();
                else
                    doLoad(path);
            } else {
                finish();
            }
        } else if (requestCode == ViewerActivity.REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if (bundle == null) return;
                boolean delete = bundle.getBoolean("delete");
                if (delete)
                    doRefresh();
            }
        }
    }

    private void onReceiveIntent(Intent intent) {
        if (intent == null) {
            Log.d(TAG, "onReceiveIntent Empty");
            finish();
            return;
        }

        String action = intent.getAction();
        String type = intent.getType();
        Log.d(TAG, "onReceiveIntent " + action + ", " + type);
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/") || type.startsWith("video/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    if (mImageUris == null)
                        mImageUris = new ArrayList<>();
                    mImageUris.clear();
                    mImageUris.add(getRealPathFromURI(imageUri));
                    startFileActionLocateActivity(NASApp.ACT_UPLOAD);
                    Log.d(TAG, "onReceiveIntent ACTION_SEND : " + mImageUris.size());
                    return;
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/") || type.startsWith("video/")) {
                ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (imageUris != null) {
                    if (mImageUris == null)
                        mImageUris = new ArrayList<>();
                    mImageUris.clear();
                    for (Uri uri : imageUris) {
                        mImageUris.add(getRealPathFromURI(uri));
                    }
                    startFileActionLocateActivity(NASApp.ACT_UPLOAD);
                    Log.d(TAG, "onReceiveIntent ACTION_SEND_MULTIPLE : " + mImageUris.size());
                    return;
                }
            }
        }

        Toast.makeText(this, getString(R.string.unknown_format) + ": " + type, Toast.LENGTH_SHORT).show();
        finish();
    }


    /**
     * INITIALIZATION
     */
    private void init() {
        mMode = NASApp.MODE_SMB;
        mPath = mRoot = NASApp.ROOT_SMB;
        mFileList = new ArrayList<FileInfo>();
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_close_gray_24dp);
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

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.main_progress_view);
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                break;
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
            toast(R.string.unknown_format);
        }
    }

    @Override
    public void onRecyclerItemLongClick(int position) {
        if (mPath.equals(NASApp.ROOT_SMB))
            return;
    }

    @Override
    public void onRecyclerItemInfoClick(int position) {
        FileInfo fileInfo = mFileList.get(position);
        if (FileInfo.TYPE.DIR.equals(fileInfo.type)) {
            doLoad(fileInfo.path);
        } else {
            startFileInfoActivity(fileInfo);
        }
    }

    @Override
    public void onRecyclerItemIconClick(int position) {
        if (mPath.equals(NASApp.ROOT_SMB))
            return;
    }

    /**
     * SOFT KEY BACK CONTROL
     */
    @Override
    public void onBackPressed() {
        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
        } else {
            if (!isOnTop()) {
                String parent = new File(mPath).getParent();
                doLoad(parent);
            } else {
                super.onBackPressed();
            }
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.w(TAG, "onConfigurationChanged");
        if (mRecyclerView.getLayoutManager() instanceof GridLayoutManager)
            updateGridView(true);
        resizeToolbar();
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
            mPreviousLoaderID = -1;
            mPreviousLoaderArgs = null;
        }

        Log.w(TAG, "Previous Loader ID: " + id);
        return record;
    }

    /**
     * LOADER CONTROL
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        setRecordCommand(id, args);
        ArrayList<String> paths = args.getStringArrayList("paths");
        String path = args.getString("path");
        switch (mLoaderID = id) {
            case LoaderID.SMB_FILE_LIST:
                mProgressView.setVisibility(View.VISIBLE);
                return new SmbFileListLoader(this, path);
            case LoaderID.LOCAL_FILE_UPLOAD:
                return new LocalFileUploadLoader(this, paths, path);
            case LoaderID.TUTK_NAS_LINK:
                return new TutkLinkNasLoader(this, args);
            case LoaderID.TUTK_LOGOUT:
                mProgressView.setVisibility(View.VISIBLE);
                return new TutkLogoutLoader(this);
            case LoaderID.EVENT_NOTIFY:
                mProgressView.setVisibility(View.VISIBLE);
                return new EventNotifyLoader(this, args);
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
                updateScreen();
            } else if (loader instanceof TutkLinkNasLoader) {
                Log.w(TAG, "Remote Access connect success, start execute previous loader : " + mPreviousLoaderID);
                if (mPreviousLoaderArgs != null && mPreviousLoaderID >= 0) {
                    mPreviousLoaderArgs.putBoolean("retry", true);
                    getLoaderManager().restartLoader(mPreviousLoaderID, mPreviousLoaderArgs, this).forceLoad();
                    return;
                }
            } else if (loader instanceof TutkLogoutLoader) {
                finish();
            } else if (loader instanceof EventNotifyLoader) {
                Bundle args = ((EventNotifyLoader) loader).getBundleArgs();
                String path = args.getString("path");
                if (path != null && !path.equals("")) {
                    doLoad(path);
                    return;
                }
            } else {
                doRefresh();
            }
        } else {
            if (isRemoteAccess() && mPreviousLoaderID > 0 && mPreviousLoaderArgs != null) {
                if (mLoaderID == LoaderID.TUTK_NAS_LINK) {
                    mPreviousLoaderID = -1;
                    mPreviousLoaderArgs = null;
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                } else {
                    Log.w(TAG, "Remote Access connect fail, try reConnect");
                    Bundle args = new Bundle();
                    String uuid = P2PService.getInstance().getTUTKUUID();
                    if (uuid == null || "".equals(uuid)) {
                        uuid = NASPref.getUUID(this);
                        if (uuid == null || "".equals(uuid)) {
                            uuid = NASPref.getCloudUUID(this);
                            if (uuid == null || "".equals(uuid)) {
                                finish();
                                return;
                            }
                        }
                    }
                    args.putString("hostname", uuid);
                    getLoaderManager().restartLoader(LoaderID.TUTK_NAS_LINK, args, this).forceLoad();
                    return;
                }
            } else {
                checkEmptyView();
                if (loader instanceof SmbAbstractLoader)
                    Toast.makeText(this, ((SmbAbstractLoader) loader).getExceptionMessage(), Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            }
        }

        mProgressView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        Log.w(TAG, "onLoaderReset: " + loader.getClass().getSimpleName());
    }

    private boolean doEventNotify(boolean update, String path) {
        Long lastTime = Long.parseLong(NASPref.getSessionVerifiedTime(this));
        Long currTime = System.currentTimeMillis();
        Log.w(TAG, "hash key time check : " + (currTime - lastTime));
        if (currTime - lastTime >= 180000) {
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
        int id = LoaderID.SMB_FILE_LIST;
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
                Collections.sort(mFileList, FileInfoSort.comparator(FileSharedActivity.this));
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
                    Toast.makeText(FileSharedActivity.this, getString(R.string.search_result_empty), Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }
        };
    }


    /**
     * FILE EDITOR
     */
    private ArrayList<String> getSelectedPaths() {
        ArrayList<String> paths = new ArrayList<String>();
        if (mImageUris != null && mImageUris.size() > 0) {
            for (String uri : mImageUris) {
                paths.add(uri);
            }
        }
        return paths;
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    private void doUpload(String dest, ArrayList<String> paths) {
        int id = LoaderID.LOCAL_FILE_UPLOAD;
        Bundle args = new Bundle();
        args.putStringArrayList("paths", paths);
        args.putString("path", dest);
        getLoaderManager().restartLoader(id, args, FileSharedActivity.this).forceLoad();
        Log.w(TAG, "doUpload: " + paths.size() + " item(s) to " + dest);
    }

    /**
     * UX CONTROL
     */
    private void updateScreen() {
        mDropdownAdapter.updateList(mPath, mMode);
        mDropdownAdapter.notifyDataSetChanged();
        mRecyclerAdapter.updateList(mFileList);
        mRecyclerAdapter.notifyDataSetChanged();
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

        Bundle args = new Bundle();
        args.putString("mode", mode);
        args.putString("type", type);
        args.putString("root", root);
        args.putString("path", path);
        Intent intent = new Intent();
        intent.setClass(FileSharedActivity.this, FileActionLocateActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
    }

    private void startMusicActivity(String mode, String root, FileInfo fileInfo) {
        if (!MusicActivity.checkFormatSupportOrNot(fileInfo.path)) {
            startVideoActivity(fileInfo);
            return;
        }

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
        intent.setClass(FileSharedActivity.this, MusicActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, MusicActivity.REQUEST_CODE);
    }

    private void startVideoActivity(FileInfo fileInfo) {
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
        intent.setClass(FileSharedActivity.this, ViewerActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, ViewerActivity.REQUEST_CODE);
    }

    private void startFileInfoActivity(FileInfo info) {
        Bundle args = new Bundle();
        args.putSerializable("info", info);
        Intent intent = new Intent();
        intent.setClass(FileSharedActivity.this, FileInfoActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, FileInfoActivity.REQUEST_CODE);
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
