package com.transcend.nas.management;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.transcend.nas.R;
import com.transcend.nas.management.externalstorage.ExternalStorageController;
import com.transcend.nas.management.externalstorage.ExternalStorageLollipop;
import com.transcend.nas.management.externalstorage.SDCardReceiver;
import com.transcend.nas.management.externalstorage.ViewerPagerAdapterSD;
import com.transcend.nas.viewer.photo.ViewerPager;

import java.util.ArrayList;

/**
 * Created by steve_su on 2016/12/28.
 */

public class FileActionLocateShowDeviceActivity extends AppCompatActivity implements FileManageRecyclerAdapter.OnRecyclerItemCallbackListener, SDCardReceiver.SDCardObserver {
    private static final String TAG = FileActionLocateShowDeviceActivity.class.getSimpleName();
    public static final int REQUEST_CODE = FileActionLocateShowDeviceActivity.class.hashCode() & 0xFFFF;
    private static final int POSITION_PRIMARY_STORAGE = 0;
    private static final int POSITION_SD_CARD = 1;
    private ArrayList<FileInfo> mFileList;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_locate);
        initData();
        initToolbar();
        initRecyclerView();
        disableFabs();

        SDCardReceiver.registerObserver(this);
    }

    @Override
    protected void onDestroy() {
        SDCardReceiver.unregisterObserver(this);
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ExternalStorageLollipop.REQUEST_CODE) {
                boolean isSelectedFolderValid = new ExternalStorageLollipop(this).checkSelectedFolder(data);
                if (isSelectedFolderValid == true) {
                    backToLocateActvity(POSITION_SD_CARD);
                } else {
                    Toast.makeText(this, R.string.dialog_grant_permission_failed, Toast.LENGTH_LONG).show();
                    requestPermissionDialog();
                }
            }
        }
    }

    @Override
    public void onRecyclerItemLongClick(int position) {

    }

    @Override
    public void onRecyclerItemInfoClick(int position) {
        onRecyclerItemClick(position);
    }

    @Override
    public void onRecyclerItemIconClick(int position) {

    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRecyclerItemClick(int position) {
        if (position == POSITION_PRIMARY_STORAGE) {
            backToLocateActvity(position);
        } else {
            ExternalStorageController controller = new ExternalStorageController(this);
            if (controller.isWritePermissionNotGranted() == true) {
                Toast.makeText(this, R.string.dialog_request_write_permission, Toast.LENGTH_LONG).show();
                requestPermissionDialog();
            } else {
                backToLocateActvity(position);
            }
        }

    }

    /**
     * It will not show this activity when there is only one device.
     */
    @Override
    public void notifyMounted() {

    }

    @Override
    public void notifyUnmounted() {
        mFileList.remove(POSITION_SD_CARD);
        mRecyclerView.getAdapter().notifyItemChanged(POSITION_SD_CARD);
        mRecyclerView.getAdapter().notifyItemRangeRemoved(POSITION_SD_CARD, 1);
    }

    private void initData() {
        mFileList = new ArrayList<>();
        ArrayList<String> deviceList = getIntent().getStringArrayListExtra("device_list");
        for (String device : deviceList) {
            FileInfo file = new FileInfo();
            file.name = device;
            file.path = "";
            file.time = "";
            file.type = FileInfo.TYPE.DIR;
            mFileList.add(file);
        }
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.locate_toolbar);
        toolbar.setTitle(R.string.storage_name);
        toolbar.setNavigationIcon(R.drawable.ic_toolbar_close_white);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initRecyclerView() {
        FileManageRecyclerAdapter adapter = new FileManageRecyclerAdapter(this, mFileList);
        adapter.setOnRecyclerItemCallbackListener(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.locate_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void disableFabs() {
        FloatingActionButton fabControl = (FloatingActionButton) findViewById(R.id.locate_fab_control);
        fabControl.setVisibility(View.INVISIBLE);
    }

    private void backToLocateActvity(int position) {
        FileInfo fileInfo = mFileList.get(position);
        Intent i = new Intent();
        i.putExtra("selected_device", fileInfo.name);

        this.setResult(Activity.RESULT_OK, i);
        finish();
    }

    private void requestPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(this.getResources().getString(R.string.sdcard));
        builder.setIcon(R.drawable.ic_sdcard_gray_24dp);
        builder.setView(R.layout.dialog_connect_sd);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.confirm, null);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.show();
        Button posBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        posBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                FileActionLocateShowDeviceActivity.this.startActivityForResult(intent, ExternalStorageLollipop.REQUEST_CODE);
                dialog.dismiss();
            }
        });
        posBtn.setTextSize(18);
        Button negBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        negBtn.setTextSize(18);

        ViewerPager viewerPager = (ViewerPager) dialog.findViewById(R.id.viewer_pager_sd);
        viewerPager.setAdapter(new ViewerPagerAdapterSD(this));
        viewerPager.setCurrentItem(0);
    }

}
