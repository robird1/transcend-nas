package com.transcend.nas.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;


/**
 * Created by silverhsu on 16/3/2.
 */
public class SettingsActivity extends AppCompatActivity {
    public static final int REQUEST_CODE = SettingsActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = SettingsActivity.class.getSimpleName();

    public BasicFragment mFragment;
    public ChangeFragmentListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initToolbar();
        NASPref.showProgressBar(this, true);

        mListener = new ChangeFragmentListener(){
            @Override
            public void onChangeFragment(BasicFragment f) {
                mFragment = f;
            }
        };
        mFragment = new SettingsFragment();
        mFragment.setListener(mFragment, mListener);
        getFragmentManager().beginTransaction().replace(R.id.settings_frame, mFragment).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
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
    public void onBackPressed(){
        if(mFragment != null)
            mFragment.showPreviousFragment();
        else
            super.onBackPressed();
    }

    /**
     * INITIALIZATION
     */
    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }


    public interface onFragmentChanged{
        public void setPreviousFragment(BasicFragment f);
        public void showPreviousFragment();
    }

    public interface ChangeFragmentListener{
        public void onChangeFragment(BasicFragment f);
    }
}

