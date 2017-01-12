package com.transcend.nas.management;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.transcend.nas.LoaderID;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.management.externalstorage.ExternalStorageController;
import com.transcend.nas.management.externalstorage.ExternalStorageLollipop;
import com.transcend.nas.management.externalstorage.OTGLocalFolderCreateLoader;
import com.transcend.nas.management.firmware.FileFactory;
import com.transcend.nas.management.firmware.MediaFactory;
import com.transcend.nas.viewer.photo.ViewerActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.transcend.nas.NASUtils.getDeviceName;

/**
 * Created by silverhsu on 16/2/2.
 */
public class FileActionLocateActivity extends AppCompatActivity implements
        FileManageDropdownAdapter.OnDropdownItemSelectedListener,
        FileManageRecyclerAdapter.OnRecyclerItemCallbackListener,
        LoaderManager.LoaderCallbacks<Boolean>,
        View.OnClickListener {

    public static final int REQUEST_CODE = FileActionLocateActivity.class.hashCode() & 0xFFFF;

    private static final String TAG = FileActionLocateActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private AppCompatSpinner mDropdown;
    private FileManageDropdownAdapter mDropdownAdapter;
    private RecyclerView mRecyclerView;
    private FileManageRecyclerAdapter mRecyclerAdapter;
    private FloatingActionButton mFabControl;
    private RelativeLayout mProgressView;
    private MenuItem mNewFolder;
    private Toast mToast;

    private String mMode;
    private String mType;
    private String mRoot;
    private String mPath;
    private ArrayList<FileInfo> mFileList;
    private int mLoaderID;

    private Map<String, String> mDeviceMap = new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate");
        setContentView(R.layout.activity_file_locate);
        initData();
        initToolbar();
        initDropdown();
        initRecyclerView();
        initFabs();
        initProgressView();
        doRefresh();
        toast(getHintResId(), Toast.LENGTH_SHORT);

        String type = getIntent().getStringExtra("type");
        if (type != null && !type.equals(NASApp.ACT_UPLOAD)) {
            checkExternalDeviceCount();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.w(TAG, "onActivityResult");

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FileActionLocateShowDeviceActivity.REQUEST_CODE) {
                String device = data.getStringExtra("selected_device");
                Log.d(TAG, "selected device: "+ device);
                mPath = mDeviceMap.get(device);

                doRefresh();
            }
        } else {
            finish();
        }
    }

        /**
         *
         * INITIALIZATION
         *
         */
    private void initData() {
        Bundle args = getIntent().getExtras();
        mMode = args.getString("mode");
        mType = args.getString("type");
        mRoot = args.getString("root");
        mPath = args.getString("path");
        mFileList = new ArrayList<FileInfo>();
    }

    private void initToolbar() {
        mToolbar = (Toolbar)findViewById(R.id.locate_toolbar);
        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initDropdown() {
        mDropdownAdapter = new FileManageDropdownAdapter(this, true);
        mDropdownAdapter.setOnDropdownItemSelectedListener(this);
        mDropdownAdapter.updateList(mPath, mMode);
        mDropdown = (AppCompatSpinner)findViewById(R.id.locate_dropdown);
        mDropdown.setAdapter(mDropdownAdapter);
        mDropdown.setDropDownVerticalOffset(10);
    }

    private void initRecyclerView() {
        mRecyclerAdapter = new FileManageRecyclerAdapter(this, mFileList);
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);
        mRecyclerView = (RecyclerView)findViewById(R.id.locate_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void initFabs() {
        mFabControl = (FloatingActionButton) findViewById(R.id.locate_fab_control);
        mFabControl.setOnClickListener(this);
        if (NASApp.ACT_COPY.equals(mType) || NASApp.ACT_MOVE.equals(mType))
            mFabControl.setImageResource(R.drawable.ic_content_copy_white_24dp);
        if (NASApp.ACT_UPLOAD.equals(mType))
            mFabControl.setImageResource(R.drawable.ic_file_upload_white_24dp);
        if (NASApp.ACT_DOWNLOAD.equals(mType))
            mFabControl.setImageResource(R.drawable.ic_file_download_white_24dp);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout)findViewById(R.id.locate_progress_view);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resizeToolbar();
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
     * RECYCLER ITEM CONTROL
     *
     */
    @Override
    public void onRecyclerItemClick(int position) {
        FileInfo fileInfo = mFileList.get(position);
        if (fileInfo.type.equals(FileInfo.TYPE.DIR)) {
            doLoad(fileInfo.path);
        } else
        if (fileInfo.type.equals(FileInfo.TYPE.PHOTO)) {
            startViewerActivity(fileInfo.path);
        } else
        if (fileInfo.type.equals(FileInfo.TYPE.VIDEO)) {
            MediaFactory.open(this, fileInfo.path);
        } else
        if (fileInfo.type.equals(FileInfo.TYPE.MUSIC)) {
            MediaFactory.open(this, fileInfo.path);
        } else {
            toast(R.string.unknown_format, Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onRecyclerItemLongClick(int position) {

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

    }


    /**
     *
     * MENU CONTROL
     *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_locate, menu);
        mNewFolder = menu.findItem(R.id.file_locate_action_new_folder);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.file_locate_action_new_folder:
                doNewFolder();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     *
     * VIEW CLICK CONTROL
     *
     */
    @Override
    public void onClick(View v) {
        if (v.equals(mFabControl)) {
            popupConfirmDialog();
        }
    }


    /**
     *
     * SOFT KEY BACK CONTROL
     *
     */
    @Override
    public void onBackPressed() {
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
            toast(R.string.msg_location_on_top, Toast.LENGTH_SHORT);
        }
    }

    private boolean isOnTop() {
        if (NASApp.MODE_SMB.equals(mMode)) {
            return mPath.equals(mRoot);
        }
        else  {
            if (NASUtils.isSDCardPath(this, mPath)) {
                mRoot = NASUtils.getSDLocation(this);
            } else {
                mRoot = NASApp.ROOT_STG;
            }

            File root = new File(mRoot);
            File file = new File(mPath);
            return file.equals(root);
        }

    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return false;
    }


    /**
     *
     * LOADER CONTROL
     *
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        mProgressView.setVisibility(View.VISIBLE);
        String path = args.getString("path");
        String name = args.getString("name");
        switch (mLoaderID = id) {
            case LoaderID.SMB_FILE_LIST:
                return new SmbFileListLoader(this, path);
            case LoaderID.LOCAL_FILE_LIST:
                return new LocalFileListLoader(this, path);
            case LoaderID.SMB_NEW_FOLDER:
                return new SmbFolderCreateLoader(this, path);
            case LoaderID.LOCAL_NEW_FOLDER:
                return new LocalFolderCreateLoader(this, path);
            case LoaderID.OTG_LOCAL_NEW_FOLDER:
                return new OTGLocalFolderCreateLoader(this, new ExternalStorageLollipop(this).getSDFileLocation(path), name);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof SmbFileListLoader) {
            if (success) {
                //file list change, stop previous image loader
                ImageLoader.getInstance().stop();
                mPath = ((SmbFileListLoader) loader).getPath();
                ArrayList<FileInfo> list = ((SmbFileListLoader) loader).getFileList();
                if(mFileList == null)
                    mFileList = new ArrayList<FileInfo>();
                else
                    mFileList.clear();
                for (FileInfo info : list) {
                    if (FileInfo.TYPE.DIR.equals(info.type))
                        mFileList.add(info);
                }
                Collections.sort(mFileList, FileInfoSort.comparator(this));
                FileFactory.getInstance().addFolderFilterRule(mPath, mFileList);
                FileFactory.getInstance().addFileTypeSortRule(mFileList);
                updateScreen();
                if(mNewFolder != null)
                    mNewFolder.setVisible(!mRoot.equals(mPath));
                mFabControl.setVisibility(mRoot.equals(mPath) ? View.INVISIBLE : View.VISIBLE);
            }
        } else if (loader instanceof LocalFileListLoader) {
            if (success) {
                //file list change, stop previous image loader
                ImageLoader.getInstance().stop();
                mPath = ((LocalFileListLoader) loader).getPath();
                ArrayList<FileInfo> list = ((LocalFileListLoader) loader).getFileList();
                if(mFileList == null)
                    mFileList = new ArrayList<FileInfo>();
                else
                    mFileList.clear();
                for (FileInfo info : list) {
                    if (FileInfo.TYPE.DIR.equals(info.type))
                        mFileList.add(info);
                }
                Collections.sort(mFileList, FileInfoSort.comparator(this));
                FileFactory.getInstance().addFileTypeSortRule(mFileList);
                updateScreen();
            }
        }
        else {
            if (success) {
                doRefresh();
            }
        }

        if(!success){
            if(loader instanceof SmbAbstractLoader)
                Toast.makeText(this, ((SmbAbstractLoader) loader).getExceptionMessage(), Toast.LENGTH_SHORT).show();
            else if (loader instanceof LocalFolderCreateLoader)
                new ExternalStorageController(this).handleWriteOperationFailed();
            else
                Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
        }

        mProgressView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }


    /**
     *
     * FILE BROWSER
     *
     */
    private void doLoad(String path) {
        int id = NASApp.MODE_SMB.equals(mMode)
                ? LoaderID.SMB_FILE_LIST : LoaderID.LOCAL_FILE_LIST;
        Bundle args = new Bundle();
        args.putString("path", path);
        getLoaderManager().restartLoader(id, args, this).forceLoad();
        Log.w(TAG, "doLoad: " + path);
    }

    private void doRefresh() {
        Log.w(TAG, "doRefresh");
        doLoad(mPath);
    }

    // TODO duplicated method in FileManageActivity
    private void doNewFolder() {
        List<String> folderNames = new ArrayList<String>();
        for (FileInfo file : mFileList) {
            if (file.type.equals(FileInfo.TYPE.DIR))
                folderNames.add(file.name);
        }
        new FileActionNewFolderDialog(this, folderNames) {
            @Override
            public void onConfirm(String newName) {
                ExternalStorageController storageController = new ExternalStorageController(FileActionLocateActivity.this);
                int id = (NASApp.MODE_SMB.equals(mMode))
                        ? LoaderID.SMB_NEW_FOLDER :
                        (storageController.isWritePermissionRequired(mPath) ? LoaderID.OTG_LOCAL_NEW_FOLDER : LoaderID.LOCAL_NEW_FOLDER);

                StringBuilder builder = new StringBuilder(mPath);
                if (!mPath.endsWith("/"))
                    builder.append("/");
                if (!storageController.isWritePermissionRequired(mPath)) {
                    builder.append(newName);
                }
                String path = builder.toString();
                Bundle args = new Bundle();
                args.putString("path", path);
                args.putString("name", newName);
                getLoaderManager().restartLoader(id, args, FileActionLocateActivity.this).forceLoad();
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
        mDropdownAdapter.updateList(mPath, mMode);
        mDropdownAdapter.notifyDataSetChanged();
        mRecyclerAdapter.updateList(mFileList);
        mRecyclerAdapter.notifyDataSetChanged();
    }

    private void resizeToolbar() {
        TypedValue typedValue = new TypedValue();
        int[] attr = new int[]{ R.attr.actionBarSize };
        TypedArray array = obtainStyledAttributes(typedValue.resourceId, attr);
        mToolbar.getLayoutParams().height = array.getDimensionPixelSize(0, 0);
        array.recycle();
    }

    private void toast(int resId, int duration) {
        if (mToast != null)
            mToast.cancel();
        mToast = Toast.makeText(this, resId, duration);
        mToast.setGravity(Gravity.CENTER, 0, 0);
        mToast.show();
    }

    private int getHintResId() {
        return NASApp.ACT_COPY.equals(mType) ? R.string.msg_paste_to
                : NASApp.ACT_MOVE.equals(mType) ? R.string.msg_move_to
                : NASApp.ACT_UPLOAD.equals(mType) ? R.string.msg_upload_to
                : NASApp.ACT_DOWNLOAD.equals(mType) ? R.string.msg_download_to
                : R.string.msg_direct_to;
    }

    private void popupConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getHintResId());
        builder.setMessage(mPath);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.confirm, null);
        builder.setCancelable(true);
        AlertDialog dialog = builder.show();
        Button bnPos = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        bnPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToMainActivity();
            }
        });
    }

    private void startViewerActivity(String path) {
        ArrayList<FileInfo> list = new ArrayList<FileInfo>();
        for (FileInfo info : mFileList) {
            if (FileInfo.TYPE.PHOTO.equals(info.type))
                list.add(info);
        }
        Bundle args = new Bundle();
        args.putString("path", path);
        args.putSerializable("list", list);
        Intent intent = new Intent();
        intent.setClass(FileActionLocateActivity.this, ViewerActivity.class);
        intent.putExtras(args);
        startActivity(intent);
    }

    private void startFileInfoActivity(FileInfo info) {
        Bundle args = new Bundle();
        args.putSerializable("info", info);
        Intent intent = new Intent();
        intent.setClass(FileActionLocateActivity.this, FileInfoActivity.class);
        intent.putExtras(args);
        startActivity(intent);
    }

    private void backToMainActivity() {
        Bundle bundle = new Bundle();
        bundle.putString("mode", mMode);
        bundle.putString("path", mPath);
        bundle.putString("type", mType);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void checkExternalDeviceCount() {
        List<File> stgList = NASUtils.getStoragePath(this);
        if (stgList.size() > 1) {
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < stgList.size(); i++) {
                String name;
                if (i == 0) {
                    name = getDeviceName();
                } else {
                    name = stgList.get(i).getName();
                }
                list.add(name);
                Log.d(TAG, "device: " + name);
                mDeviceMap.put(name, stgList.get(i).getAbsolutePath());
            }

            Intent i = new Intent(this, FileActionLocateShowDeviceActivity.class);
            i.putStringArrayListExtra("device_list", list);
            startActivityForResult(i, FileActionLocateShowDeviceActivity.REQUEST_CODE);
        }
    }

}
