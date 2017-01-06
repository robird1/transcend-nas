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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.transcend.nas.R;
import com.transcend.nas.management.externalstorage.ExternalStorageController;
import com.transcend.nas.management.externalstorage.ExternalStorageLollipop;
import com.transcend.nas.management.externalstorage.ViewerPagerAdapterSD;
import com.transcend.nas.viewer.photo.ViewerPager;

import java.util.ArrayList;

/**
 * Created by steve_su on 2016/12/28.
 */

public class FileActionLocateShowDeviceActivity extends AppCompatActivity implements FileManageRecyclerAdapter.OnRecyclerItemCallbackListener {
    private static final String TAG = FileActionLocateShowDeviceActivity.class.getSimpleName();
    public static final int REQUEST_CODE = FileActionLocateShowDeviceActivity.class.hashCode() & 0xFFFF;
    private ArrayList<FileInfo> mFileList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_locate);
        initData();
        initToolbar();
        initRecyclerView();
        disableFabs();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ExternalStorageLollipop.REQUEST_CODE) {
                boolean isSelectedFolderValid = new ExternalStorageLollipop(this).checkSelectedFolder(data);
                if (isSelectedFolderValid == true) {
                    backToLocateActvity(1);
                } else {
                    Toast.makeText(this, R.string.dialog_grant_permission_failed, Toast.LENGTH_LONG).show();
                    requestPermissionDialog();
                }
            }
        }
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
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initRecyclerView() {
        FileManageRecyclerAdapter adapter = new FileManageRecyclerAdapter(mFileList);
        adapter.setOnRecyclerItemCallbackListener(this);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.locate_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    private void disableFabs() {
        FloatingActionButton fabControl = (FloatingActionButton) findViewById(R.id.locate_fab_control);
        fabControl.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onRecyclerItemClick(int position) {
        Log.d(TAG, "[Enter] onRecyclerItemClick");
        Log.d(TAG, "position: "+ position);
        if (position == 0) {
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

    private void backToLocateActvity(int position) {
        FileInfo fileInfo = mFileList.get(position);
        Intent i = new Intent();
        i.putExtra("selected_device", fileInfo.name);

        this.setResult(Activity.RESULT_OK, i);
        finish();
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
