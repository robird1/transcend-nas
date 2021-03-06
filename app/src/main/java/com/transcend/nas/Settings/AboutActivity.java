package com.transcend.nas.settings;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.BuildConfig;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.view.NotificationDialog;

/**
 * Created by ikeLee on 16/3/21.
 */
public class AboutActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = AboutActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = AboutActivity.class.getSimpleName();

    private static boolean isSubFragment = false;
    private static TextView mTitle = null;
    private static LinearLayout mAbout = null;
    private static Context mContext;

    private TextView mVersion;
    private int mLoaderID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
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

    @Override
    public void onDestroy() {
        mContext = null;
        super.onDestroy();
    }

    /**
     * INITIALIZATION
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.about_toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_navi_backaarow_white);
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
            Preference appVersionPref = findPreference(getString(R.string.pref_about_app_version));
            appVersionPref.setSummary("v"+ BuildConfig.VERSION_NAME);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            int id = -1;
            if (preference.getKey().equals(getString(R.string.pref_about))) {
                showNotificationDialog(R.string.app_name, R.layout.dialog_about);
            } else if (preference.getKey().equals(getString(R.string.pref_about_term_of_use))) {
                id = R.string.about_license_agreement;
            } else if (preference.getKey().equals(getString(R.string.pref_about_license))) {
                id = R.string.open_source_statement;
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

//        private void startSendMailActivity(){
//            Intent intent = new Intent();
//            intent.setAction(Intent.ACTION_SENDTO);
//            intent.setData(Uri.parse("mailto:service-tw@transcend-info.com"));
//            //intent.putExtra(Intent.EXTRA_SUBJECT, "??????????????????");
//            //intent.putExtra(Intent.EXTRA_TEXT, "?????????????????????");
//            startActivity(intent);
//        }

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
            if(id == R.string.about_license_agreement){
                v = inflater.inflate(R.layout.fragment_term_of_use, container, false);
                TextView info = (TextView) v.findViewById(R.id.info);
                info.setText(Html.fromHtml(NASUtils.readFromAssets(mContext, "NASAPPEULA.txt")));
            }
            else if(id == R.string.open_source_statement){
                v = inflater.inflate(R.layout.fragment_license, container, false);
                WebView info = (WebView) v.findViewById(R.id.info);
                info.setWebViewClient(new MyWebViewClient());
                info.loadUrl("file:///android_asset/open_source_statement2.html");
            }
            else {
                v = inflater.inflate(R.layout.fragment_about_info, container, false);
                TextView info = (TextView) v.findViewById(R.id.info);
                info.setText(getString(id));
            }
            return v;
        }
    }

    private static class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

}
