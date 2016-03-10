package com.transcend.nas.viewer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.transcend.nas.R;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileInfoActivity;

import java.util.ArrayList;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by silverhsu on 16/2/23.
 */
public class ViewerActivity extends AppCompatActivity implements
        PhotoViewAttacher.OnPhotoTapListener,
        View.OnClickListener {

    public static final String TAG = ViewerActivity.class.getSimpleName();

    private Toolbar mHeaderBar;
    private Toolbar mFooterBar;
    private ImageView mInfo;
    private ViewerPager mPager;
    private ViewerPagerAdapter mPagerAdapter;

    private String mPath;
    private ArrayList<FileInfo> mList;

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
        return super.onCreateOptionsMenu(menu);
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
    public void onClick(View v) {
        if (v.equals(mInfo)) {
            doInfo();
        }
    }

    @Override
    public void onPhotoTap(View view, float x, float y) {
        toggleFullScreen();
    }


    /**
     *
     * INITIALIZATION
     *
     */
    private void initData() {
        Bundle args = getIntent().getExtras();
        mPath = args.getString("path");
        mList = (ArrayList<FileInfo>) args.getSerializable("list");
    }

    private void initHeaderBar() {
        mHeaderBar = (Toolbar) findViewById(R.id.viewer_header_bar);
        mHeaderBar.setTitle("");
        setSupportActionBar(mHeaderBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initFooterBar() {
        mFooterBar = (Toolbar) findViewById(R.id.viewer_footer_bar);
        mInfo = (ImageView) findViewById(R.id.viewer_action_info);
        mInfo.setOnClickListener(this);
    }

    private void initPager() {
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
     *
     * ACTION
     *
     */
    private void doInfo() {
        int position = mPager.getCurrentItem();
        FileInfo info = mList.get(position);
        startFileInfoActivity(info);
    }


    /**
     *
     * UX CONTROL
     *
     */
    private void toggleFullScreen() {
        if (getSupportActionBar().isShowing()) {
            getSupportActionBar().hide();
            mFooterBar.setVisibility(View.INVISIBLE);
        }
        else {
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

}
