package com.transcend.nas.settings;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.BuildConfig;
import com.transcend.nas.R;
import com.transcend.nas.view.NotificationDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by ikeLee on 16/3/21.
 */
public class AboutActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = SettingsActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = AboutActivity.class.getSimpleName();

    private static boolean isSubFragment = false;
    private static TextView mTitle = null;
    private static LinearLayout mAbout = null;

    private TextView mVersion;
    private int mLoaderID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        mAbout = (LinearLayout) findViewById(R.id.about_layout);
        mVersion = (TextView) findViewById(R.id.about_version);
        mVersion.setText(getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME);
        initToolbar();
        initFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(isSubFragment)
                    initFragment();
                else
                    finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
            if(isSubFragment)
                initFragment();
            else
                finish();
    }

    /**
     * INITIALIZATION
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.about_toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        mTitle =(TextView) toolbar.findViewById(R.id.about_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initFragment() {
        mTitle.setText(getString(R.string.about));
        mAbout.setVisibility(View.VISIBLE);
        isSubFragment = false;
        int id = R.id.about_frame;
        Fragment f = new AboutFragment();
        getFragmentManager().beginTransaction().replace(id, f).commit();
    }


    /**
     * ABOUT FRAGMENT
     */
    public static class AboutFragment extends PreferenceFragment {

        private Toast mToast;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preference_about);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            int id = -1;
            if (preference.getKey().equals(getString(R.string.pref_about))) {
                showNotificationDialog(R.string.app_name, R.layout.dialog_about);
            } else if (preference.getKey().equals(getString(R.string.pref_about_legal))) {
                id = R.string.legal;
            } else if (preference.getKey().equals(getString(R.string.pref_about_term_of_use))) {
                id = R.string.termsofuse;
            } else if (preference.getKey().equals(getString(R.string.pref_about_license))) {
                id = R.string.licenses;
            } else if (preference.getKey().equals(getString(R.string.pref_about_contact))) {
                startSendMailActivity();
            }

            if(id > 0) {
                mTitle.setText(getString(id));
                Fragment f = new InfoFragment(id);
                getFragmentManager().beginTransaction().replace(R.id.about_frame,f).commit();
                mAbout.setVisibility(View.GONE);
                isSubFragment = true;
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        private void toast(int resId) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT);
            //mToast.setGravity(Gravity.CENTER, 0, 0);
            mToast.show();
        }

        private void showNotificationDialog(int titleId, int layoutId) {
            Bundle value = new Bundle();
            value.putString(NotificationDialog.DIALOG_TITLE, getActivity().getString(titleId));
            value.putInt(NotificationDialog.DIALOG_LAYOUT, layoutId);
            NotificationDialog mNotificationDialog = new NotificationDialog(getActivity(), value, true, false) {
                @Override
                public void onConfirm() {

                }

                @Override
                public void onCancel() {

                }
            };
        }

        private void startSendMailActivity(){
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:service-tw@transcend-info.com"));
            //intent.putExtra(Intent.EXTRA_SUBJECT, "這裡是主旨。");
            //intent.putExtra(Intent.EXTRA_TEXT, "這是本文內容。");
            startActivity(intent);
        }

    }

    public static class InfoFragment extends Fragment {
        int id = -1;

        public InfoFragment(){

        }

        @SuppressLint("ValidFragment")
        public InfoFragment(int id){
            this.id = id;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View v;
            if(id == R.string.legal) {
                v = inflater.inflate(R.layout.fragment_privacy_info, container, false);
            }
            else if(id == R.string.termsofuse){
                v = inflater.inflate(R.layout.fragment_term_of_use, container, false);
                TextView info = (TextView) v.findViewById(R.id.info);
                try {
                    info.setText(Html.fromHtml(readFromAssets(getActivity(), "terms_of_use.txt")));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(id == R.string.licenses){
                v = inflater.inflate(R.layout.fragment_license, container, false);
                TextView info = (TextView) v.findViewById(R.id.info);
                try {
                    info.setText(Html.fromHtml(readFromAssets(getActivity(), "LicensedText-SJCAndroid.txt")));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                v = inflater.inflate(R.layout.fragment_about_info, container, false);
                TextView info = (TextView) v.findViewById(R.id.info);
                info.setText(getString(id));
            }
            return v;
        }

        public String readFromAssets(Context context, String filename) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));

            // do reading, usually loop until end of file reading
            StringBuilder sb = new StringBuilder();
            String mLine = reader.readLine();
            while (mLine != null) {
                if(mLine.endsWith(".") && mLine.length() < 30)
                    sb.append(String.format("<h3>%s</h3>", mLine));
                else {
                    sb.append(String.format("<p>%s</p>", mLine));
                }
                sb.append(System.getProperty("line.separator"));
                mLine = reader.readLine();
            }
            reader.close();
            return sb.toString();
        }
    }

}
