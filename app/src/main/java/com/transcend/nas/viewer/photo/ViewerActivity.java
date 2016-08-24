package com.transcend.nas.viewer.photo;

import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.management.FileActionDeleteDialog;
import com.transcend.nas.management.FileActionLocateActivity;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileInfoActivity;
import com.transcend.nas.management.LocalFileDeleteLoader;
import com.transcend.nas.management.LocalFileUploadLoader;
import com.transcend.nas.management.SmbAbstractLoader;
import com.transcend.nas.management.SmbFileDeleteLoader;
import com.transcend.nas.management.SmbFileDownloadLoader;
import com.transcend.nas.common.FileFactory;

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
    private ImageView mUpload;
    private ImageView mDownload;
    private ViewerPager mPager;
    private ViewerPagerAdapter mPagerAdapter;
    private VideoCastManager mCastManager;
    private VideoCastConsumer mCastConsumer;
    private MenuItem mMediaRouteMenuItem;

    private int mLoaderID;
    private String mPath;
    private String mMode;
    private String mRoot;
    private ArrayList<FileInfo> mList;
    private int mCurrentIndex = -1;
    private boolean evenDelete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);
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
                doPhotoCast(mCurrentIndex);
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
        initData();
        initHeaderBar();
        initFooterBar();
        initPager();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_manage_editor, menu);
        menu.findItem(R.id.image_manage_editor_action_upload).setVisible(false);
        menu.findItem(R.id.image_manage_editor_action_download).setVisible(false);
        mMediaRouteMenuItem = mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
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
        } else if (v.equals(mDownload)) {
            startFileActionLocateActivity(NASApp.ACT_DOWNLOAD);
        } else if (v.equals(mUpload)) {
            startFileActionLocateActivity(NASApp.ACT_UPLOAD);
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
        mList = FileFactory.getInstance().getFileList();
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

        mDownload = (ImageView) findViewById(R.id.viewer_action_download);
        mDownload.setVisibility(NASApp.MODE_SMB.equals(mMode) ? View.VISIBLE : View.GONE);
        mDownload.setOnClickListener(this);
        mDownload.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mDownload.setImageResource(R.drawable.ic_file_download_gray_24dp);
                        break;
                    case MotionEvent.ACTION_UP:
                        mDownload.setImageResource(R.drawable.ic_file_download_white_24dp);
                        break;
                }
                return false;
            }
        });


        mUpload = (ImageView) findViewById(R.id.viewer_action_upload);
        mUpload.setVisibility(NASApp.MODE_SMB.equals(mMode) ? View.GONE : View.VISIBLE);
        mUpload.setOnClickListener(this);
        mUpload.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mUpload.setImageResource(R.drawable.ic_file_upload_gray_24dp);
                        break;
                    case MotionEvent.ACTION_UP:
                        mUpload.setImageResource(R.drawable.ic_file_upload_white_24dp);
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
        int index = list.indexOf(mPath);
        mPagerAdapter = new ViewerPagerAdapter(this);
        mPagerAdapter.setContent(list);
        mPagerAdapter.setOnPhotoTapListener(this);
        mPager = (ViewerPager) findViewById(R.id.viewer_pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(index);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                doPhotoCast(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        doPhotoCast(index);
    }

    private void doPhotoCast(int position) {
        if (position >= mList.size())
            return;

        mCurrentIndex = position;
        if (NASApp.MODE_SMB.equals(mMode) && mCastManager != null && mCastManager.isConnected()) {
            try {
                Log.d(TAG, "CastManager loaded : " + mCastManager.isRemoteMediaLoaded());
                if (mCastManager.isRemoteMediaLoaded()) {
                    mCastManager.stop();
                    mCastManager.clearMediaSession();
                }
            } catch (CastException e) {
                e.printStackTrace();
            } catch (TransientNetworkDisconnectionException e) {
                e.printStackTrace();
            } catch (NoConnectionException e) {
                e.printStackTrace();
            }

            try {
                mCastManager.sendDataMessage(FileFactory.getInstance().getPhotoPath(false, mList.get(position).path));
            } catch (TransientNetworkDisconnectionException e) {
                e.printStackTrace();
            } catch (NoConnectionException e) {
                e.printStackTrace();
            }
        }
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
            if (loader instanceof SmbAbstractLoader)
                Toast.makeText(this, ((SmbAbstractLoader) loader).getExceptionMessage(), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (success) {
            if (loader instanceof SmbFileDeleteLoader || loader instanceof LocalFileDeleteLoader) {
                evenDelete = true;
                int position = mPager.getCurrentItem();
                mList.remove(position);
                mPagerAdapter.removeView(position);
                if (mList.size() == 0) {
                    if (NASApp.MODE_SMB.equals(mMode) && mCastManager != null && mCastManager.isConnected()) {
                        try {
                            mCastManager.sendDataMessage("close");
                        } catch (TransientNetworkDisconnectionException e) {
                            e.printStackTrace();
                        } catch (NoConnectionException e) {
                            e.printStackTrace();
                        }
                    }
                    doFinish();
                } else {
                    //check is last photo or not
                    if (position >= mList.size()) {
                        position = mList.size() - 1;
                    }
                    doPhotoCast(position);
                }
            } else if (loader instanceof SmbFileDownloadLoader || loader instanceof LocalFileUploadLoader) {
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

    private void doFinish() {
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
        final String path
                = NASApp.ACT_UPLOAD.equals(type) ? NASApp.ROOT_SMB
                : NASApp.ACT_DOWNLOAD.equals(type) ? NASPref.getDownloadLocation(this)
                : mPath;

        //for Action Download, we use default download folder
        if (NASApp.ACT_DOWNLOAD.equals(type) && NASPref.useDefaultDownloadFolder) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.download);
            builder.setIcon(R.drawable.ic_file_download_gray_24dp);
            builder.setMessage(String.format(getString(R.string.msg_file_selected), 1));
            builder.setNegativeButton(R.string.cancel, null);
            builder.setPositiveButton(R.string.confirm, null);
            builder.setCancelable(true);
            final AlertDialog dialog = builder.show();
            Button bnPos = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            bnPos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //force close dialog
                    dialog.dismiss();
                    //start download
                    doDownload(path);
                }
            });
        } else {
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
