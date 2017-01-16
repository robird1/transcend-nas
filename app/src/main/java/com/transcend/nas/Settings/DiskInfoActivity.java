package com.transcend.nas.settings;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.transcend.nas.R;
import com.transcend.nas.LoaderID;
import com.transcend.nas.common.AnimFactory;
import com.transcend.nas.management.firmware.FileFactory;

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
    private List<View> mViews;
    private boolean isInit = false;
    private boolean isEmpty = false;
    private int mCurrentIndex = 0;

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
        mToolbar.setNavigationIcon(R.drawable.ic_navi_backaarow_white);
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
        if(mViews == null)
            isEmpty = true;

        mViews = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        for (DiskStructDevice device : devices) {
            PieDataSet dataSet = DiskFactory.getInstance().getPieChartDataSet(false, device);
            if (dataSet != null) {
                View view = getPieChartView(dataSet, device);
                mViews.add(view);
                titles.add(device.infos.get("model"));
            }
        }
        mDiskInfoPagerAdapter = new DiskInfoPagerAdapter();
        mDiskInfoPagerAdapter.setContentList(mViews);
        mDiskInfoViewerPager.setAdapter(mDiskInfoPagerAdapter);
        mDiskInfoViewerPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurrentIndex = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //after views update, scroll to previous view's index
        if (mViews.size() > mCurrentIndex) {
            mDiskInfoViewerPager.setCurrentItem(mCurrentIndex, true);
        }

        if (titles.size() > 1) {
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
            mTitle.setVisibility(View.GONE);
        } else {
            mSpinner.setVisibility(View.GONE);
            mTitle.setVisibility(View.VISIBLE);
        }
    }

    private View getPieChartView(final PieDataSet dataSet, final DiskStructDevice device) {
        final String title = device.infos.get("model");
        final boolean isRAID = (title != null && title.contains("RAID"));
        final boolean isExternal = "yes".equals(device.infos.get("external"));

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
                                LinearLayout usedLayout = (LinearLayout) view.findViewById(R.id.listitem_disk_used_layout);
                                LinearLayout availableLayout = (LinearLayout) view.findViewById(R.id.listitem_disk_available_layout);
                                TextView usedText = (TextView) view.findViewById(R.id.listitem_disk_used_text);
                                TextView availableText = (TextView) view.findViewById(R.id.listitem_disk_available_text);
                                //ImageView next = (ImageView) view.findViewById(R.id.listitem_disk_info_next);
                                ImageView icon = (ImageView) view.findViewById(R.id.listitem_disk_info_icon);
                                TextView smartText = (TextView) view.findViewById(R.id.listitem_disk_info_smart);
                                ImageView smartImage = (ImageView) view.findViewById(R.id.listitem_disk_info_smart_image);
                                if (position == 0) {
                                    //pie chart
                                    chart.setVisibility(View.VISIBLE);
                                    layout.setVisibility(View.GONE);
                                    usedLayout.setVisibility(View.VISIBLE);
                                    availableLayout.setVisibility(View.VISIBLE);
                                    usedText.setText(getString(R.string.used) + ": "
                                            + FileFactory.getInstance().getFileSize((long) (device.totalSize - device.availableSize))
                                            + " (" + DiskFactory.getInstance().getDeviceUsedSizePercent(device) + ")");
                                    availableText.setText(getString(R.string.available) + ": "
                                            + FileFactory.getInstance().getFileSize((long) (device.availableSize))
                                            + " (" + DiskFactory.getInstance().getDeviceAvailableSizePercent(device) + ")");
                                    chart.setData(data);
                                    chart.highlightValues(null);
                                    chart.setCenterTextSize(20f);
                                    chart.setHoleRadius(72f);
                                    chart.setTransparentCircleRadius(75f);
                                    chart.setRotationAngle(270);
                                    chart.setRotationEnabled(false);
                                    chart.setUsePercentValues(true);
                                    chart.setDrawEntryLabels(false);
                                    chart.setDescription("");
                                    chart.getLegend().setEnabled(false);
                                    chart.setCenterText(FileFactory.getInstance().getFileSize((long) device.availableSize) + "\n" + getString(R.string.available));
                                    if (!isEmpty)
                                        chart.animateY(1000, Easing.EasingOption.EaseInOutQuad);
                                    else if(isEmpty && !isInit)
                                        chart.startAnimation(AnimFactory.getInstance().getAlphaAnimation());
                                    isInit = false;
                                    isEmpty = false;
                                } else {
                                    chart.setVisibility(View.GONE);
                                    layout.setVisibility(View.VISIBLE);
                                    usedLayout.setVisibility(View.GONE);
                                    availableLayout.setVisibility(View.GONE);
                                    if (isExternal) {
                                        icon.setImageResource(R.drawable.icon_usb_gray_24dp);
                                        smartText.setVisibility(View.GONE);
                                        smartImage.setVisibility(View.GONE);
                                    } else {
                                        icon.setImageResource(R.drawable.icon_hdd_gray_24dp);
                                        smartText.setVisibility(View.VISIBLE);
                                        DiskStructDevice target = null;
                                        if (isRAID) {
                                            List<DiskStructDevice> result = DiskFactory.getInstance().getRAIDPairDevice(mDevices, device.raid);
                                            if (result != null && result.size() > 0) {
                                                int index = position - 1;
                                                if (index >= 0 && index < result.size()) {
                                                    DiskStructDevice tmp = result.get(index);
                                                    Log.d(TAG,"Index : " + index + ", " + tmp.smartCheck);
                                                    if (!tmp.smartCheck)
                                                        new DiskDeviceSmartTask(getApplicationContext(), mDevices, tmp, smartText, smartImage).execute();
                                                    else
                                                        target = tmp;
                                                }
                                            }
                                        } else {
                                            if (!device.smartCheck)
                                                new DiskDeviceSmartTask(getApplicationContext(), mDevices, device, smartText, smartImage).execute();
                                            else
                                                target = device;
                                        }

                                        DiskFactory.getInstance().setDeviceSmartText(DiskInfoActivity.this, smartText, smartImage, target);
                                    }
                                }
                                return view;
                            }
                        }
        );

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (isRAID) {
                    if (position >= 1) {
                        List<DiskStructDevice> result = DiskFactory.getInstance().getRAIDPairDevice(mDevices, device.raid);
                        if (result != null && result.size() > 0) {
                            int index = position - 1;
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
        String title = "";
        if ("yes".equals(device.infos.get("external"))) {
            title = device.infos.get("model");
        } else {
            title = getString(R.string.disk) + " 1";
        }
        String path = device.infos.get("path");
        HashMap<String, String> item1 = new HashMap<String, String>();
        item1.put(ID_TITLE, title);
        item1.put(ID_SUBTITLE, path);
        myListData.add(item1);

        return myListData;
    }

    private ArrayList<HashMap<String, String>> get2BayDeviceItems(String ID_TITLE, String ID_SUBTITLE, DiskStructDevice device) {
        ArrayList<HashMap<String, String>> myListData = new ArrayList<>();

        //add Pie Chart
        HashMap<String, String> item1 = new HashMap<String, String>();
        item1.put(ID_TITLE, "");
        item1.put(ID_SUBTITLE, "");
        myListData.add(item1);

        //add RAID
        /*String title = device.infos.get("model");
        float totalSize = DiskFactory.getInstance().getDeviceTotalSize(device);
        float availSize = DiskFactory.getInstance().getDeviceAvailableSize(device);
        String size = getString(R.string.available) +" : " + FileFactory.getInstance().getFileSize((long) availSize);
        HashMap<String, String> item2 = new HashMap<String, String>();
        item2.put(ID_TITLE, title);
        item2.put(ID_SUBTITLE, size);
        myListData.add(item2);*/

        //add RAID corresponding Disks
        int index = 1;
        List<DiskStructDevice> results = DiskFactory.getInstance().getRAIDPairDevice(mDevices, device.raid);
        if (results != null && results.size() > 0) {
            for (DiskStructDevice result : results) {
                HashMap<String, String> item = new HashMap<String, String>();
                //item.put(ID_TITLE, result.infos.get("model"));
                item.put(ID_TITLE, getString(R.string.disk) + " " + (index++));
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
            case LoaderID.DISK_SMART:
                return new DiskDeviceSmartLoader(this, mDevices, mDevices.get(0));
            default:
                break;
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        mProgressView.setVisibility(View.INVISIBLE);
        if (loader instanceof DiskDeviceInfoLoader) {
            DiskDeviceInfoLoader tmp = (DiskDeviceInfoLoader) loader;
            if (!success) {
                String error = tmp.getError();
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                return;
            }


            mDevices = tmp.getDevices();
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
