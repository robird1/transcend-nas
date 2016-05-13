package com.transcend.nas;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.common.LoaderID;
import com.transcend.nas.connection.AutoLinkLoader;
import com.transcend.nas.connection.LoginLoader;
import com.transcend.nas.connection.NASFinderActivity;
import com.transcend.nas.connection.NASListLoader;
import com.transcend.nas.connection.SignInActivity;
import com.transcend.nas.connection.WizardInitLoader;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.TutkForgetPasswordLoader;
import com.transcend.nas.management.TutkGetNasLoader;
import com.transcend.nas.management.TutkLinkNasLoader;
import com.transcend.nas.management.TutkLoginLoader;
import com.transcend.nas.management.TutkLogoutLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

public class WizardActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean>, View.OnClickListener {

    private static final String TAG = WizardActivity.class.getSimpleName();
    private Toolbar mToolbar;
    private TextInputLayout tlPassword;
    private TextInputLayout tlConfirmPassword;
    private Button bStart;
    private TextView tVInfo;
    private RelativeLayout mProgressView;
    private String mHostname = null;
    private boolean isRemoteAccess = false;
    private int mLoaderID = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wizard);
        mToolbar = (Toolbar) findViewById(R.id.wizard_toolbar);
        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent intent = getIntent();
        mHostname = (String) intent.getExtras().getString("Hostname", null);
        isRemoteAccess = (boolean) intent.getBooleanExtra("RemoteAccess", false);
        tlPassword = (TextInputLayout) findViewById(R.id.wizard_password);
        tlConfirmPassword = (TextInputLayout) findViewById(R.id.wizard_password_confirm);
        bStart = (Button) findViewById(R.id.wizard_start);
        bStart.setOnClickListener(this);
        tVInfo = (TextView) findViewById(R.id.wizard_status);
        mProgressView = (RelativeLayout) findViewById(R.id.wizard_progress_view);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        mProgressView.setVisibility(View.VISIBLE);
        switch (mLoaderID = id) {
            case LoaderID.WIZARD_INIT:
                return new WizardInitLoader(this, args, isRemoteAccess);
            case LoaderID.LOGIN:
                return new LoginLoader(this, args);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if(loader instanceof WizardInitLoader){
            if(!success){
                mProgressView.setVisibility(View.INVISIBLE);
                Toast.makeText(this,getString(R.string.network_error),Toast.LENGTH_SHORT).show();
                return;
            }

            mProgressView.setVisibility(View.INVISIBLE);
            Intent intent = new Intent();
            intent.putExtras(((WizardInitLoader) loader).getBundleArgs());
            WizardActivity.this.setResult(RESULT_OK, intent);
            finish();

            //Bundle args = ((WizardInitLoader) loader).getBundleArgs();
            //args.putString("username", "admin");
            //getLoaderManager().restartLoader(LoaderID.LOGIN, args, this).forceLoad();
        } else if(loader instanceof LoginLoader){
            if(!success){;
                Toast.makeText(this,getString(R.string.network_error),Toast.LENGTH_SHORT).show();
                return;
            }

            mProgressView.setVisibility(View.INVISIBLE);
            startFileManageActivity();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    private void startFileManageActivity() {
        Intent intent = new Intent();
        intent.setClass(WizardActivity.this, FileManageActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.wizard_start:
                String pwd = tlPassword.getEditText().getText().toString();
                String confirm = tlConfirmPassword.getEditText().getText().toString();
                if(pwd.equals("")) {
                    Toast.makeText(this, getString(R.string.empty_password), Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!pwd.equals(confirm)){
                    Toast.makeText(this, getString(R.string.confirm_password_error), Toast.LENGTH_SHORT).show();
                    return;
                }

                String timezone = Integer.toString(TimeZone.getDefault().getRawOffset() / 3600000);
                Bundle args = new Bundle();
                args.putString("nickname", getString(R.string.success_wizard) + "!");
                args.putString("hostname", mHostname);
                args.putString("username", "admin");
                args.putString("password", pwd);
                args.putString("timezone", timezone);
                getLoaderManager().restartLoader(LoaderID.WIZARD_INIT, args, this).forceLoad();
                break;
            default:
                break;
        }
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
        } else {
            super.onBackPressed();
        }
    }
}
