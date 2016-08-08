package com.transcend.nas.settings;

import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.utils.FileFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DiskDetailActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean>{

    public static final int REQUEST_CODE = DiskDetailActivity.class.hashCode() & 0xFFFF;
    private static final String TAG = DiskDetailActivity.class.getSimpleName();
    private Toolbar mToolbar;
    private TextView mTitle;
    private ScrollView mSmartLayout;
    private TextView mSmartText;
    private RelativeLayout mProgressView;
    private DiskInfoViewerPager mDiskInfoViewerPager;
    private DiskInfoPagerAdapter mDiskInfoPagerAdapter;
    private int mLoaderID = -1;
    private List<DiskStructDevice> mDevices;
    private DiskStructDevice mCurrentDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disk_info);
        init();
    }

    private void init() {
        mToolbar = (Toolbar) findViewById(R.id.disk_info_toolbar);
        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mTitle = (TextView) findViewById(R.id.disk_info_toolbar_title);
        mTitle.setText(getString(R.string.detail));
        mProgressView = (RelativeLayout) findViewById(R.id.disk_info_progress_view);
        mDiskInfoViewerPager = (DiskInfoViewerPager) findViewById(R.id.disk_info_view_pager);
        mSmartLayout = (ScrollView) findViewById(R.id.disk_info_smart_layout);
        mSmartText = (TextView) findViewById(R.id.disk_info_smart_text);

        mCurrentDevice = DiskFactory.getInstance().getCurrentDevice();
        if(mCurrentDevice != null) {
            mDevices = new ArrayList<>();
            mDevices.add(mCurrentDevice);
            setDeviceData(mDevices);
        } else {
            finish();
        }
    }

    /**
     * MENU CONTROL
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.disk_manage, menu);
        return true;
    }

    private void setDeviceData(List<DiskStructDevice> devices) {
        List<View> views = new ArrayList<>();
        for (DiskStructDevice device : devices) {
            View view = getItemView(device);
            views.add(view);
        }
        mDiskInfoPagerAdapter = new DiskInfoPagerAdapter();
        mDiskInfoPagerAdapter.setContentList(views);
        mDiskInfoViewerPager.setAdapter(mDiskInfoPagerAdapter);
    }

    private View getItemView(final DiskStructDevice device) {
        LayoutInflater mInflater = getLayoutInflater().from(this);
        View view = mInflater.inflate(R.layout.viewer_disk_info, null);

        String title = device.infos.get("model");
        TextView tv = (TextView) view.findViewById(R.id.disk_info_title_label);
        tv.setText(title);

        String ID_TITLE = "TITLE";
        String ID_SUBTITLE = "SUBTITLE";
        ArrayList<HashMap<String, String>> myListData;
        myListData = getDeviceItems(ID_TITLE, ID_SUBTITLE, device);

        ListView list = (ListView) view.findViewById(R.id.disk_info_list_view);
        list.setAdapter(new SimpleAdapter(this, myListData, R.layout.listitem_disk_detail, new String[]{ID_TITLE, ID_SUBTITLE},
                                new int[]{R.id.listitem_disk_info_title, R.id.listitem_disk_info_subtitle}) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                ImageView icon = (ImageView) view.findViewById(R.id.listitem_disk_info_icon);
                                icon.setVisibility(View.GONE);
                                return view;
                            }
                        }
        );

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 4){
                    //S.M.A.R.T
                    getLoaderManager().restartLoader(LoaderID.DISK_SMART, null, DiskDetailActivity.this).forceLoad();
                }
            }
        });

        return view;
    }

    private ArrayList<HashMap<String, String>> getDeviceItems(String ID_TITLE, String ID_SUBTITLE, DiskStructDevice device){
        ArrayList<HashMap<String, String>> myListData = new ArrayList<>();

        HashMap<String, String> model = new HashMap<String, String>();
        model.put(ID_TITLE, getString(R.string.model));
        model.put(ID_SUBTITLE, device.infos.get("model"));
        myListData.add(model);

        HashMap<String, String> path = new HashMap<String, String>();
        path.put(ID_TITLE, getString(R.string.path));
        path.put(ID_SUBTITLE, device.infos.get("path"));
        myListData.add(path);

        float totalSize = DiskFactory.getInstance().getDeviceTotalSize(device);
        HashMap<String, String> total = new HashMap<String, String>();
        total.put(ID_TITLE, getString(R.string.capacity));
        total.put(ID_SUBTITLE, FileFactory.getInstance().getFileSize((long) totalSize) + " (" + new BigDecimal(totalSize).toPlainString() + " bytes)");
        myListData.add(total);

        String external = device.infos.get("external");
        if(external != null && external.equals("no")) {
            String hddtemp = device.infos.get("temperature");
            if (hddtemp == null || "".equals(hddtemp))
                hddtemp = getString(R.string.loading);
            HashMap<String, String> temperature = new HashMap<String, String>();
            temperature.put(ID_TITLE, getString(R.string.temperature));
            temperature.put(ID_SUBTITLE, hddtemp);
            myListData.add(temperature);

            /*HashMap<String, String> smart = new HashMap<String, String>();
            smart.put(ID_TITLE, getString(R.string.disk_smart));
            smart.put(ID_SUBTITLE, getString(R.string.disk_smart_info));
            myListData.add(smart);*/
        }

        return myListData;
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        mProgressView.setVisibility(View.VISIBLE);
        switch (mLoaderID = id) {
            case LoaderID.DISK_SMART:
                return new DiskDeviceSmartLoader(this, mDevices, mCurrentDevice);
            default:
                break;
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

        if(loader instanceof DiskDeviceSmartLoader) {
            String result = ((DiskDeviceSmartLoader) loader).getResult();
            mSmartText.setText(result);
            mSmartLayout.setVisibility(View.VISIBLE);
            mTitle.setText(getString(R.string.disk_smart));
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

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
    public void onBackPressed() {
        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
        } else if(mSmartLayout.getVisibility() == View.VISIBLE){
            mSmartText.setText("");
            mSmartLayout.setVisibility(View.GONE);
            mTitle.setText(getString(R.string.detail));
        } else {
            super.onBackPressed();
        }
    }
}
