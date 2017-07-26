package com.transcend.nas.management.browser_framework;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.transcend.nas.R;
import com.transcend.nas.management.browser.MediaController;

/**
 * Created by steve_su on 2017/7/19.
 */

public abstract class BrowserActivity extends AppCompatActivity {
    private MediaController mMediaControl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manage_fragment);
//        mMediaControl = new MediaController(this, BrowserData.ALL.getTabPosition());

        replaceFragment(onFragmentClass());
    }

    protected void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        transaction.setCustomAnimations(R.anim.appear, 0);
//        transaction.replace(R.id.fragment_container, onFragmentClass(), tag);
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
        getSupportFragmentManager().executePendingTransactions();
    }

//    void onPageChanged(int position) {
//        mMediaControl = new MediaController(this, position);
//    }

    protected abstract Fragment onFragmentClass();

}
