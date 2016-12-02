package com.transcend.nas.settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.transcend.nas.NASUtils;
import com.transcend.nas.R;

import org.apache.http.HttpEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by steve_su on 2016/11/25.
 */

public class DeviceInfoActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = DeviceInfoActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = DeviceInfoActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolbar();
        NASUtils.showProgressBar(this, true);
        initFragment();
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

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_white_24dp);
        TextView title = (TextView) toolbar.findViewById(R.id.settings_title);
        title.setText(getString(R.string.settings_device_info_title));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initFragment() {
        getFragmentManager().beginTransaction().replace(R.id.settings_frame, new DeviceInfoFragment()).commit();
    }

    public static class DeviceInfoFragment extends PreferenceFragment {

        private String mDeviceName;
        private String mIPAddress;
        private String mMACAddress;

        public DeviceInfoFragment() {}

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_device_info);
            refreshDeviceInfo();
        }

        private void refreshDeviceInfo() {

            final Handler handler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (getActivity() != null) {
                        Preference prefDeviceName = findPreference(getString(R.string.pref_device_name));
                        prefDeviceName.setSummary(mDeviceName);
                        Preference prefIPAddress = findPreference(getString(R.string.pref_ip_address));
                        prefIPAddress.setSummary(mIPAddress);
                        Preference prefMACAddress = findPreference(getString(R.string.pref_mac));
                        prefMACAddress.setSummary(mMACAddress);

                        NASUtils.showProgressBar(getActivity(), false);
                    }
                }
            };

            new Thread(new Runnable() {
                @Override
                public void run() {
                    doProcess();
                    handler.sendMessage(new Message());
                }
            }).start();

        }

        private void doProcess() {
            HttpEntity entity = NASUtils.sendGetRequest();
            InputStream inputStream = null;
            String inputEncoding = null;

            if (entity != null) {
                try {
                    inputStream = entity.getContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                inputEncoding = EntityUtils.getContentCharSet(entity);
            }

            if (inputEncoding == null) {
                inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
            }

            if (inputStream != null) {
                doParse(inputStream, inputEncoding);
            }

        }

        private void doParse(InputStream inputStream, String inputEncoding) {
            Log.d(TAG, "[Enter] doParse()");

            XmlPullParserFactory factory;
            try {
                factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(inputStream, inputEncoding);
                int eventType = parser.getEventType();

                do {
                    String tagName = parser.getName();
//                    Log.d(TAG, "tagName: " + tagName);

                    if (eventType == XmlPullParser.START_TAG) {
//                        Log.d(TAG, "START_TAG");

                        switch (tagName) {
                            case "ipaddr":
                                parser.next();
                                mIPAddress = parser.getText();
                                break;
                            case "hostname":
                                parser.next();
                                mDeviceName = parser.getText();
                                break;
                            case "hwaddr":
                                parser.next();
                                mMACAddress = parser.getText();
                                break;
                        }
                    }

                    eventType = parser.next();

                } while (eventType != XmlPullParser.END_DOCUMENT);

            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }


}
