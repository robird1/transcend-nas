package com.transcend.nas.settings;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.BuildConfig;
import com.transcend.nas.R;

/**
 * Created by ikeLee on 16/3/21.
 */
public class AboutActivity extends AppCompatActivity {

    public static final String TAG = AboutActivity.class.getSimpleName();

    private static boolean isSubFragment = false;
    private static TextView mTitle = null;

    private int mLoaderID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
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
                id = R.string.about;
            } else if (preference.getKey().equals(getString(R.string.pref_about_legal))) {
                id = R.string.legal;
            } else if (preference.getKey().equals(getString(R.string.pref_about_term_of_use))) {
                id = R.string.term_of_use;
            } else if (preference.getKey().equals(getString(R.string.pref_about_license))) {
                id = R.string.licenses;
            } else if (preference.getKey().equals(getString(R.string.pref_about_contact))) {
                id = R.string.contact_us;
            }

            if(id > 0) {
                mTitle.setText(getString(id));
                Fragment f = new InfoFragment(id);
                getFragmentManager().beginTransaction().replace(R.id.about_frame,f).commit();
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
            View v = inflater.inflate(R.layout.fragment_about_info, container, false);
            TextView info = (TextView) v.findViewById(R.id.info);
            if(id == R.string.about){
                int versionCode = BuildConfig.VERSION_CODE;
                String versionName = BuildConfig.VERSION_NAME;
                info.setText(versionName);
            }
            else {
                info.setText(getString(id));
            }
            return v;
        }

    }

}
