package com.transcend.nas.settings;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.utils.FileFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DiskInfoActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {

    public static final int REQUEST_CODE = DiskInfoActivity.class.hashCode() & 0xFFFF;
    private static final String TAG = DiskInfoActivity.class.getSimpleName();
    private Toolbar mToolbar;
    private TextView mTitle;
    private AppCompatSpinner mSpinner;
    private DiskInfoDropdownAdapter mSpinnerAdapter;
    private RelativeLayout mProgressView;
    private DiskInfoViewerPager mDiskInfoViewerPager;
    private DiskInfoPagerAdapter mDiskInfoPagerAdapter;
    private int mLoaderID = -1;
    private List<DiskStructDevice> mDevices;
    private boolean isInit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disk_info);
        init();
        getLoaderManager().restartLoader(LoaderID.DISK_INFO, null, this).forceLoad();
    }

    private void init() {
        mToolbar = (Toolbar) findViewById(R.id.disk_info_toolbar);
        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mProgressView = (RelativeLayout) findViewById(R.id.disk_info_progress_view);
        mDiskInfoViewerPager = (DiskInfoViewerPager) findViewById(R.id.disk_info_view_pager);
        mSpinner = (AppCompatSpinner) findViewById(R.id.disk_info_dropdown);
        mSpinner.setDropDownVerticalOffset(10);
        mTitle = (TextView) findViewById(R.id.disk_info_toolbar_title);

        mDevices = DiskFactory.getInstance().getDiskDevices();
        if (mDevices != null && mDevices.size() > 0) {
            isInit = true;
            setDeviceData(mDevices);
        }
    }

    /**
     * MENU CONTROL
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.disk_manage, menu);
        return true;
    }

    private void setDeviceData(List<DiskStructDevice> devices) {
        List<View> views = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        for (DiskStructDevice device : devices) {
            PieDataSet dataSet = DiskFactory.getInstance().getPieChartDataSet(false, device);
            if (dataSet != null) {
                View view = getPieChartView(dataSet, device);
                views.add(view);
                titles.add(device.infos.get("model"));
            }
        }
        mDiskInfoPagerAdapter = new DiskInfoPagerAdapter();
        mDiskInfoPagerAdapter.setContentList(views);
        mDiskInfoViewerPager.setAdapter(mDiskInfoPagerAdapter);
        if(titles.size() > 1) {
            mSpinnerAdapter = new DiskInfoDropdownAdapter();
            mSpinnerAdapter.setContentList(titles);
            mSpinnerAdapter.setOnDropdownItemSelectedListener(new DiskInfoDropdownAdapter.OnDropdownItemSelectedListener() {
                @Override
                public void onDropdownItemSelected(int position) {
                    mDiskInfoViewerPager.setCurrentItem(position, true);
                }
            });
            mSpinner.setAdapter(mSpinnerAdapter);
            mSpinner.setVisibility(View.VISIBLE);
        }
        else{
            mSpinner.setVisibility(View.GONE);
        }
    }

    private View getPieChartView(final PieDataSet dataSet, final DiskStructDevice device) {
        final String title = device.infos.get("model");
        final boolean isRAID = (title != null && title.contains("RAID"));

        ArrayList<Integer> colors = new ArrayList<Integer>();
        colors.add(Color.rgb(211, 211, 211));
        colors.add(Color.rgb(135, 206, 250));

        dataSet.setSliceSpace(0f);
        dataSet.setSelectionShift(5f);

        dataSet.setColors(colors);
        final PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextColor(Color.TRANSPARENT);

        LayoutInflater mInflater = getLayoutInflater().from(this);
        View view = mInflater.inflate(R.layout.viewer_disk_info, null);
        TextView tv = (TextView) view.findViewById(R.id.disk_info_title_label);
        tv.setText(title);
        tv.setVisibility(View.VISIBLE);

        String ID_TITLE = "TITLE";
        String ID_SUBTITLE = "SUBTITLE";
        ArrayList<HashMap<String, String>> myListData;
        if (isRAID)
            myListData = get2BayDeviceItems(ID_TITLE, ID_SUBTITLE, device);
        else
            myListData = get1BayDeviceItems(ID_TITLE, ID_SUBTITLE, device);

        ListView list = (ListView) view.findViewById(R.id.disk_info_list_view);
        list.setAdapter(new SimpleAdapter(this, myListData, R.layout.listitem_disk_info, new String[]{ID_TITLE, ID_SUBTITLE},
                                new int[]{R.id.listitem_disk_info_title, R.id.listitem_disk_info_subtitle}) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                PieChart chart = (PieChart) view.findViewById(R.id.listitem_disk_info_pie_chart);
                                RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.listitem_disk_info_layout);
                                ImageView next = (ImageView) view.findViewById(R.id.listitem_disk_info_next);
                                if (position == 0) {
                                    //pie chart
                                    chart.setVisibility(View.VISIBLE);
                                    layout.setVisibility(View.GONE);
                                    chart.setData(data);
                                    chart.highlightValues(null);
                                    chart.setDescription(FileFactory.getInstance().getFileSize((long) device.totalSize));
                                    chart.setCenterTextSize(20f);
                                    chart.setHoleRadius(72f);
                                    chart.setTransparentCircleRadius(75f);
                                    chart.setRotationAngle(270);
                                    chart.setRotationEnabled(false);
                                    chart.setUsePercentValues(true);
                                    chart.setDrawEntryLabels(false);
                                    chart.setCenterText(DiskFactory.getInstance().getDeviceAvailableSizePercent(device) + "\n" + getString(R.string.available));
                                    Legend l = chart.getLegend();
                                    l.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
                                    l.setXEntrySpace(7);
                                    l.setYEntrySpace(5);
                                    l.setOrientation(Legend.LegendOrientation.VERTICAL);
                                    if(!isInit)
                                        chart.animateY(1000, Easing.EasingOption.EaseInOutQuad);
                                    isInit = false;
                                } else {
                                    chart.setVisibility(View.GONE);
                                    layout.setVisibility(View.VISIBLE);
                                    if(isRAID && position == 1)
                                        next.setVisibility(View.INVISIBLE);
                                    else
                                        next.setVisibility(View.VISIBLE);
                                }
                                return view;
                            }
                        }
        );

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (isRAID) {
                        if(position >= 2) {
                            List<DiskStructDevice> result = getRAIDPairDevice(device.raid);
                            if (result != null && result.size() > 0) {
                                int index = position - 2;
                                if (index >= 0 && index < result.size())
                                    startDiskDetailActivity(result.get(index));
                            }
                        }
                    } else {
                        if (position >= 1) {
                            startDiskDetailActivity(device);
                        }
                    }
            }
        });

        return view;
    }

    private ArrayList<HashMap<String, String>> get1BayDeviceItems(String ID_TITLE, String ID_SUBTITLE, DiskStructDevice device) {
        ArrayList<HashMap<String, String>> myListData = new ArrayList<>();

        //add Pie Chart
        HashMap<String, String> item = new HashMap<String, String>();
        item.put(ID_TITLE, "");
        item.put(ID_SUBTITLE, "");
        myListData.add(item);

        //add Disk 1
        String title = device.infos.get("model");
        float totalSize = DiskFactory.getInstance().getDeviceTotalSize(device);
        float availSize = DiskFactory.getInstance().getDeviceAvailableSize(device);
        String size = getString(R.string.available) +" : " + FileFactory.getInstance().getFileSize((long) availSize);
        HashMap<String, String> item1 = new HashMap<String, String>();
        item1.put(ID_TITLE, title);
        item1.put(ID_SUBTITLE, size);
        myListData.add(item1);
        return myListData;
    }

    private List<DiskStructDevice> getRAIDPairDevice(DiskStructRAID deviceRAID) {
        List<DiskStructDevice> result = new ArrayList<>();
        if (deviceRAID != null) {
            String raiddevices = deviceRAID.infos.get("raiddevice");
            if (raiddevices != null) {
                String[] raidList = raiddevices.split(",");
                for (String raid : raidList) {
                    for (DiskStructDevice tmp : mDevices) {
                        boolean find = false;
                        for (DiskStructPartition partition : tmp.partitions) {
                            if (raid.equals(partition.infos.get("path"))) {
                                result.add(tmp);
                                break;
                            }
                        }
                        if (find)
                            break;
                    }
                }
            }
        }

        return result;
    }

    private ArrayList<HashMap<String, String>> get2BayDeviceItems(String ID_TITLE, String ID_SUBTITLE, DiskStructDevice device) {
        ArrayList<HashMap<String, String>> myListData = new ArrayList<>();

        //add Pie Chart
        HashMap<String, String> item1 = new HashMap<String, String>();
        item1.put(ID_TITLE, "");
        item1.put(ID_SUBTITLE, "");
        myListData.add(item1);

        //add RAID
        String title = device.infos.get("model");
        float totalSize = DiskFactory.getInstance().getDeviceTotalSize(device);
        float availSize = DiskFactory.getInstance().getDeviceAvailableSize(device);
        String size = getString(R.string.available) +" : " + FileFactory.getInstance().getFileSize((long) availSize);
        HashMap<String, String> item2 = new HashMap<String, String>();
        item2.put(ID_TITLE, title);
        item2.put(ID_SUBTITLE, size);
        myListData.add(item2);

        //add RAID corresponding Disks
        List<DiskStructDevice> results = getRAIDPairDevice(device.raid);
        if (results != null && results.size() > 0) {
            for (DiskStructDevice result : results) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(ID_TITLE, result.infos.get("model"));
                item.put(ID_SUBTITLE, result.infos.get("path"));
                myListData.add(item);
            }
        }

        return myListData;
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (mLoaderID = id) {
            case LoaderID.DISK_INFO:
                mProgressView.setVisibility(View.VISIBLE);
                return new DiskDeviceInfoLoader(this);
            case LoaderID.DISK_INFO_TEMPERATURE:
                return new DiskDeviceTemperatureLoader(this);
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

        if (loader instanceof DiskDeviceInfoLoader) {
            mDevices = ((DiskDeviceInfoLoader) loader).getDevices();
            if (mDevices != null && mDevices.size() > 0) {
                setDeviceData(mDevices);
                getLoaderManager().restartLoader(LoaderID.DISK_INFO_TEMPERATURE, null, this).forceLoad();
            }
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
            case R.id.disk_info_action_refresh:
                getLoaderManager().restartLoader(LoaderID.DISK_INFO, null, this).forceLoad();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startDiskDetailActivity(DiskStructDevice device) {
        DiskFactory.getInstance().setCurrentDevice(device);
        Intent intent = new Intent();
        intent.setClass(DiskInfoActivity.this, DiskDetailActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
        } else {
            super.onBackPressed();
        }
    }
}
