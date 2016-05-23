package com.transcend.nas.viewer;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.management.AutoBackupLoader;
import com.transcend.nas.management.FileActionDeleteDialog;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileInfoActivity;
import com.transcend.nas.management.LocalFileCopyLoader;
import com.transcend.nas.management.LocalFileDeleteLoader;
import com.transcend.nas.management.LocalFileListLoader;
import com.transcend.nas.management.LocalFileMoveLoader;
import com.transcend.nas.management.LocalFileRenameLoader;
import com.transcend.nas.management.LocalFileUploadLoader;
import com.transcend.nas.management.LocalFolderCreateLoader;
import com.transcend.nas.management.SmbFileCopyLoader;
import com.transcend.nas.management.SmbFileDeleteLoader;
import com.transcend.nas.management.SmbFileDownloadLoader;
import com.transcend.nas.management.SmbFileListLoader;
import com.transcend.nas.management.SmbFileMoveLoader;
import com.transcend.nas.management.SmbFileRenameLoader;
import com.transcend.nas.management.SmbFolderCreateLoader;
import com.transcend.nas.management.TutkLinkNasLoader;
import com.transcend.nas.management.TutkLogoutLoader;

import java.io.File;
import java.util.ArrayList;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by silverhsu on 16/2/23.
 */
public class ViewerActivity extends AppCompatActivity implements
        PhotoViewAttacher.OnPhotoTapListener,
        LoaderManager.LoaderCallbacks<Boolean>,
        View.OnClickListener {

    public static final int REQUEST_CODE = ViewerActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = ViewerActivity.class.getSimpleName();

    private RelativeLayout mProgressView;
    private Toolbar mHeaderBar;
    private Toolbar mFooterBar;
    private ImageView mInfo;
    private ImageView mDelete;
    private ViewerPager mPager;
    private ViewerPagerAdapter mPagerAdapter;

    private int mLoaderID;
    private String mPath;
    private String mMode;
    private String mRoot;
    private ArrayList<FileInfo> mList;
    private boolean evenDelete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
        initData();
        initHeaderBar();
        initFooterBar();
        initPager();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_manage_editor, menu);
        menu.removeItem(NASApp.MODE_SMB.equals(mMode) ? R.id.image_manage_editor_action_upload : R.id.image_manage_editor_action_download);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                doFinish();
                break;
            case R.id.image_manage_editor_action_upload:
                startFileActionLocateActivity(NASApp.ACT_UPLOAD);
                break;
            case R.id.image_manage_editor_action_download:
                startFileActionLocateActivity(NASApp.ACT_DOWNLOAD);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mInfo)) {
            doInfo();
        } else if (v.equals(mDelete)) {
            doDelete();
        }
    }

    @Override
    public void onPhotoTap(View view, float x, float y) {
        toggleFullScreen();
    }


    /**
     * INITIALIZATION
     */
    private void initData() {
        Bundle args = getIntent().getExtras();
        mPath = args.getString("path");
        mMode = args.getString("mode");
        mRoot = args.getString("root");
        mList = (ArrayList<FileInfo>) args.getSerializable("list");
    }

    private void initHeaderBar() {
        mHeaderBar = (Toolbar) findViewById(R.id.viewer_header_bar);
        mHeaderBar.setTitle("");
        mHeaderBar.setNavigationIcon(R.drawable.ic_navigation_arrow_white_24dp);
        setSupportActionBar(mHeaderBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initFooterBar() {
        mFooterBar = (Toolbar) findViewById(R.id.viewer_footer_bar);
        mInfo = (ImageView) findViewById(R.id.viewer_action_info);
        mInfo.setOnClickListener(this);
        mInfo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mInfo.setImageResource(R.drawable.ic_info_gray_24dp);
                        break;
                    case MotionEvent.ACTION_UP:
                        mInfo.setImageResource(R.drawable.ic_info_white_24dp);
                        break;
                }
                return false;
            }
        });
        mDelete = (ImageView) findViewById(R.id.viewer_action_delete);
        mDelete.setOnClickListener(this);
        mDelete.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mDelete.setImageResource(R.drawable.ic_delete_gray_24dp);
                        break;
                    case MotionEvent.ACTION_UP:
                        mDelete.setImageResource(R.drawable.ic_delete_white_24dp);
                        break;
                }
                return false;
            }
        });
    }

    private void initPager() {
        mProgressView = (RelativeLayout) findViewById(R.id.viewer_progress_view);

        ArrayList<String> list = new ArrayList<String>();
        for (FileInfo info : mList) list.add(info.path);
        mPagerAdapter = new ViewerPagerAdapter(this);
        mPagerAdapter.setContent(list);
        mPagerAdapter.setOnPhotoTapListener(this);
        mPager = (ViewerPager) findViewById(R.id.viewer_pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(list.indexOf(mPath));
    }


    /**
     * ACTION
     */
    private void doInfo() {
        int position = mPager.getCurrentItem();
        FileInfo info = mList.get(position);
        startFileInfoActivity(info);
    }

    private void doDelete() {
        ArrayList<String> paths = new ArrayList<String>();
        int position = mPager.getCurrentItem();
        FileInfo info = mList.get(position);
        paths.add(info.path);

        new FileActionDeleteDialog(this, paths) {
            @Override
            public void onConfirm(ArrayList<String> paths) {
                int id = (NASApp.MODE_SMB.equals(mMode))
                        ? LoaderID.SMB_FILE_DELETE
                        : LoaderID.LOCAL_FILE_DELETE;
                Bundle args = new Bundle();
                args.putStringArrayList("paths", paths);
                getLoaderManager().restartLoader(id, args, ViewerActivity.this).forceLoad();
                Log.w(TAG, "doDelete: " + paths.size() + " items");
            }
        };
    }

    private void doUpload(String dest) {
        ArrayList<String> paths = new ArrayList<String>();
        int position = mPager.getCurrentItem();
        FileInfo info = mList.get(position);
        paths.add(info.path);
        int id = LoaderID.LOCAL_FILE_UPLOAD;
        Bundle args = new Bundle();
        args.putStringArrayList("paths", paths);
        args.putString("path", dest);
        getLoaderManager().restartLoader(id, args, ViewerActivity.this).forceLoad();
        Log.w(TAG, "doUpload: " + paths.size() + " item(s) to " + dest);
    }

    private void doDownload(String dest) {
        ArrayList<String> paths = new ArrayList<String>();
        int position = mPager.getCurrentItem();
        FileInfo info = mList.get(position);
        paths.add(info.path);
        int id = LoaderID.SMB_FILE_DOWNLOAD;
        Bundle args = new Bundle();
        args.putStringArrayList("paths", paths);
        args.putString("path", dest);
        getLoaderManager().restartLoader(id, args, ViewerActivity.this).forceLoad();
        Log.w(TAG, "doDownload: " + paths.size() + " item(s) to " + dest);
    }


    /**
     * UX CONTROL
     */
    private void toggleFullScreen() {
        if (getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
            mFooterBar.setVisibility(View.INVISIBLE);
        } else {
            getSupportActionBar().show();
            mFooterBar.setVisibility(View.VISIBLE);
        }
    }

    private void startFileInfoActivity(FileInfo info) {
        Bundle args = new Bundle();
        args.putSerializable("info", info);
        Intent intent = new Intent();
        intent.setClass(ViewerActivity.this, FileInfoActivity.class);
        intent.putExtras(args);
        startActivity(intent);
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        ArrayList<String> paths = args.getStringArrayList("paths");
        String path = args.getString("path");
        String name = args.getString("name");
        switch (mLoaderID = id) {
            case LoaderID.SMB_FILE_DOWNLOAD:
                return new SmbFileDownloadLoader(this, paths, path);
            case LoaderID.LOCAL_FILE_UPLOAD:
                return new LocalFileUploadLoader(this, paths, path);
            case LoaderID.SMB_FILE_DELETE:
                mProgressView.setVisibility(View.VISIBLE);
                return new SmbFileDeleteLoader(this, paths);
            case LoaderID.LOCAL_FILE_DELETE:
                mProgressView.setVisibility(View.VISIBLE);
                return new LocalFileDeleteLoader(this, paths);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        mProgressView.setVisibility(View.INVISIBLE);
        if (!success) {
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (success) {
            if (loader instanceof SmbFileDeleteLoader || loader instanceof LocalFileDeleteLoader) {
                evenDelete = true;
                int position = mPager.getCurrentItem();
                mList.remove(position);
                mPagerAdapter.removeView(position);
                if(mList.size() == 0)
                    doFinish();
            }
            else if (loader instanceof SmbFileDownloadLoader || loader instanceof LocalFileUploadLoader){
                //do nothing
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    public void onBackPressed() {
        doFinish();
    }

    private void doFinish(){
        Bundle bundle = new Bundle();
        bundle.putBoolean("delete", evenDelete);
        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
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
        intent.setClass(ViewerActivity.this, FileActionLocateActivity.class);
        intent.putExtras(args);
        startActivityForResult(intent, FileActionLocateActivity.REQUEST_CODE);
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
            }
        }
    }
}
