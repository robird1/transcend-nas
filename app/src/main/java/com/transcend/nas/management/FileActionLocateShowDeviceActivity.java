package com.transcend.nas.management;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.transcend.nas.R;

import java.util.ArrayList;

/**
 * Created by steve_su on 2016/12/28.
 */

public class FileActionLocateShowDeviceActivity extends AppCompatActivity implements FileManageRecyclerAdapter.OnRecyclerItemCallbackListener {
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

}
