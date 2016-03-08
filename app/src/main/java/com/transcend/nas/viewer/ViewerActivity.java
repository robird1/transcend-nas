package com.transcend.nas.viewer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.R;

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

    private String mMode;
    private String mPath;
    private ArrayList<String> mList;
    private ArrayList<String> mUrls;

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
        mMode = args.getString("mode");
        mList = args.getStringArrayList("list");
        mUrls = new ArrayList<String>();
        for (String path : mList) {
            mUrls.add(parseImageURL(path));
        }
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
        mPagerAdapter = new ViewerPagerAdapter(this);
        mPagerAdapter.setContent(mUrls);
        mPagerAdapter.setOnPhotoTapListener(this);
        mPager = (ViewerPager) findViewById(R.id.viewer_pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(mList.indexOf(mPath));
    }

    private String parseImageURL(String path) {
        String url = null;
        if (NASApp.MODE_STG.equals(mMode)) {
            url = "file://" + path;
        }
        else {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = server.getHostname();
            String filepath = path.replaceFirst(Server.HOME, "/");
            String hash = server.getHash();
            url = "http://" + hostname + "/dav/home/" + filepath + "?session=" + hash + "&webview";
        }
        return url;
    }


    /**
     *
     * ACTION
     *
     */
    private void doInfo() {
        Log.w(TAG, "doInfo");
        // TODO: show info
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


}
