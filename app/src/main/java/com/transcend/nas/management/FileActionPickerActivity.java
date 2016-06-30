package com.transcend.nas.management;

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
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.utils.FileFactory;
import com.transcend.nas.utils.MediaFactory;
import com.transcend.nas.viewer.ViewerActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by silverhsu on 16/2/2.
 */
public class FileActionPickerActivity extends AppCompatActivity implements
        FileManageDropdownAdapter.OnDropdownItemSelectedListener,
        FileManageRecyclerAdapter.OnRecyclerItemCallbackListener,
        LoaderManager.LoaderCallbacks<Boolean>,
        View.OnClickListener {

    public static final int REQUEST_CODE = FileActionPickerActivity.class.hashCode() & 0xFFFF;

    private static final String TAG = FileActionPickerActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private AppCompatSpinner mDropdown;
    private FileManageDropdownAdapter mDropdownAdapter;
    private RecyclerView mRecyclerView;
    private FileManageRecyclerAdapter mRecyclerAdapter;
    private FloatingActionButton mFabControl;
    private RelativeLayout mProgressView;
    private Toast mToast;

    private String mMode;
    private String mType;
    private String mRoot;
    private String mPath;
    private String mTarget;
    private ArrayList<FileInfo> mFileList;
    private int mLoaderID;


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
        //toast(getHintResId(), Toast.LENGTH_SHORT);
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
        mTarget = args.getString("target");
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
        mDropdownAdapter = new FileManageDropdownAdapter(true);
        mDropdownAdapter.setOnDropdownItemSelectedListener(this);
        mDropdownAdapter.updateList(mPath, mMode);
        mDropdown = (AppCompatSpinner)findViewById(R.id.locate_dropdown);
        mDropdown.setAdapter(mDropdownAdapter);
        mDropdown.setDropDownVerticalOffset(10);
    }

    private void initRecyclerView() {
        mRecyclerAdapter = new FileManageRecyclerAdapter(mFileList);
        mRecyclerAdapter.setOnRecyclerItemCallbackListener(this);
        mRecyclerView = (RecyclerView)findViewById(R.id.locate_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void initFabs() {
        mFabControl = (FloatingActionButton) findViewById(R.id.locate_fab_control);
        mFabControl.setOnClickListener(this);
        if (NASApp.ACT_PICK_UPLOAD.equals(mType))
            mFabControl.setImageResource(R.drawable.ic_file_upload_white_24dp);
        if (NASApp.ACT_PICK_DOWNLOAD.equals(mType))
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
        } else{
            selectAtPosition(position);
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
        selectAtPosition(position);
    }


    /**
     *
     * MENU CONTROL
     *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.file_picker, menu);
        int count = getSelectedCount();
        if(mFileList != null && mFileList.size() == count && count > 0)
            menu.findItem(R.id.file_locate_action_selected_all).setIcon(R.drawable.ic_clear_all_white_24dp);
        else
            menu.findItem(R.id.file_locate_action_selected_all).setIcon(R.drawable.ic_done_all_white_24dp);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.file_locate_action_selected_all:
                toggleSelectAll();
                break;

        }
        return super.onOptionsItemSelected(item);
    }


    private void toggleSelectAll() {
        int count = getSelectedCount();
        boolean selectAll = (count != 0) && (count == mFileList.size());
        if (selectAll)
            clearAllSelection();
        else
            checkAllSelection();
        invalidateOptionsMenu();
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

    private void selectAtPosition(int position) {
        boolean checked = mFileList.get(position).checked;
        mFileList.get(position).checked = !checked;
        mRecyclerAdapter.notifyItemChanged(position);
        invalidateOptionsMenu();
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
        switch (mLoaderID = id) {
            case LoaderID.SMB_FILE_LIST:
                return new SmbFileListLoader(this, path);
            case LoaderID.LOCAL_FILE_LIST:
                return new LocalFileListLoader(this, path);
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
                mFileList = ((SmbFileListLoader) loader).getFileList();
                Collections.sort(mFileList, FileInfoSort.comparator(this));
                FileFactory.getInstance().addFolderFilterRule(mPath, mFileList);
                FileFactory.getInstance().addFileTypeSortRule(mFileList);
                updateScreen();
                mFabControl.setVisibility(mRoot.equals(mPath) ? View.INVISIBLE : View.VISIBLE);
            }
        } else if (loader instanceof LocalFileListLoader) {
            if (success) {
                //file list change, stop previous image loader
                ImageLoader.getInstance().stop();
                mPath = ((LocalFileListLoader) loader).getPath();
                mFileList = ((LocalFileListLoader) loader).getFileList();
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
        invalidateOptionsMenu();
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
        return NASApp.ACT_PICK_UPLOAD.equals(mType) ? R.string.msg_upload_to
                : NASApp.ACT_PICK_DOWNLOAD.equals(mType) ? R.string.msg_download_to
                : R.string.msg_direct_to;
    }

    private void popupConfirmDialog() {
        int count = getSelectedCount();
        if(count == 0){
            Toast.makeText(this, getString(R.string.no_item_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        String format = getResources().getString(count <= 1 ? R.string.msg_file_selected : R.string.msg_files_selected);
        String message = String.format(format, count);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(getHintResId()).replace(".","") + " " + mTarget);
        builder.setMessage(message);
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

    private void startFileInfoActivity(FileInfo info) {
        Bundle args = new Bundle();
        args.putSerializable("info", info);
        Intent intent = new Intent();
        intent.setClass(FileActionPickerActivity.this, FileInfoActivity.class);
        intent.putExtras(args);
        startActivity(intent);
    }

    private void backToMainActivity() {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString("mode", mMode);
        bundle.putString("path", mPath);
        bundle.putString("type", mType);
        bundle.putStringArrayList("paths", getSelectedPaths());
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

}
