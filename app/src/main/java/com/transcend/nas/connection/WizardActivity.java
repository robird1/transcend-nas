package com.transcend.nas.connection;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.common.StyleFactory;

import java.util.TimeZone;

public class WizardActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean>, View.OnClickListener {

    public static final int REQUEST_CODE = WizardActivity.class.hashCode() & 0xFFFF;
    private static final String TAG = WizardActivity.class.getSimpleName();
    private Toolbar mToolbar;
    private TextView tAccount;
    private TextInputLayout tlPassword;
    private TextInputLayout tlConfirmPassword;
    private ImageView iBackground;
    private RelativeLayout mProgressView;
    private RelativeLayout mWizardLayout;
    private RelativeLayout mReadyLayout;
    private SwitchCompat mCameraBackupSwitch;
    private String mHostname = null;
    private boolean isRemoteAccess = false;
    private String mModel = "";
    private Bundle mBundle = null;
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
        isRemoteAccess = (boolean) intent.getBooleanExtra("Wizard", false);
        mModel = (String) intent.getExtras().getString("Model", null);
        initWizardLayout();
        initReadyLayout();
        mProgressView = (RelativeLayout) findViewById(R.id.wizard_progress_view);
    }

    public void initWizardLayout(){
        mWizardLayout = (RelativeLayout) findViewById(R.id.wizard_layout);
        tAccount = (TextView) findViewById(R.id.wizard_account);
        tAccount.setText(getString(R.string.account) + " : admin");
        tlPassword = (TextInputLayout) findViewById(R.id.wizard_password);
        tlConfirmPassword = (TextInputLayout) findViewById(R.id.wizard_password_confirm);
        Button button = (Button) findViewById(R.id.wizard_start);
        button.setOnClickListener(this);
        StyleFactory.set_gray_button_touch_effect(this, button);
        StyleFactory.set_button_Drawable_right(this, button, R.drawable.ic_navigation_arrow_rotation, 50);
        iBackground = (ImageView) findViewById(R.id.wizard_bg);
        Point p = new Point();
        getWindowManager().getDefaultDisplay().getSize(p);
        iBackground.setImageBitmap(createBitmapFromResource(R.drawable.bg_wizard, p.x, p.y));
    }

    public void initReadyLayout(){
        mReadyLayout = (RelativeLayout) findViewById(R.id.ready_layout);
        TextView text = (TextView) findViewById(R.id.ready_nas_ip);
        text.setText(mHostname);
        ImageView icon = (ImageView) findViewById(R.id.ready_nas_icon);
        if(mModel != null){
            if(mModel.equals("SJC110")){
                icon.setImageResource(R.drawable.icon_1bay);
            }else if(mModel.equals("SJC210")){
                icon.setImageResource(R.drawable.icon_2bay);
            }else{
                icon.setImageResource(R.drawable.ic_logo_storejetcloud_big);
            }
        }
        Button button = (Button) findViewById(R.id.ready_button);
        button.setOnClickListener(this);
        StyleFactory.set_gray_button_touch_effect(this, button);
        mCameraBackupSwitch = (SwitchCompat) findViewById(R.id.wizard_camera_backup_indicator);
    }

    public void changeView(boolean isReady){
        mWizardLayout.setVisibility(isReady ? View.GONE : View.VISIBLE);
        mReadyLayout.setVisibility(isReady ? View.VISIBLE : View.GONE);
    }

    public Bitmap createBitmapFromResource(int drawableId, int reqWidth,int reqHeight){
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), drawableId);
        int oldwidth = bmp.getWidth();
        int oldheight = bmp.getHeight();
        float scaleWidth = reqWidth / (float)oldwidth;
        float scaleHeight = reqHeight / (float)oldheight;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, oldwidth,oldheight, matrix, true);
        return resizedBitmap;
    }


    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        mProgressView.setVisibility(View.VISIBLE);
        switch (mLoaderID = id) {
            case LoaderID.WIZARD_INIT:
                return new WizardSetLoader(this, args, isRemoteAccess);
            case LoaderID.LOGIN:
                return new LoginLoader(this, args, true);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof WizardSetLoader) {
            if (!success) {
                mProgressView.setVisibility(View.INVISIBLE);
                String error = ((WizardSetLoader) loader).getErrorResult();
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                if (error.equals(getString(R.string.network_error))) {
                    return;
                } else {
                    finish();
                    return;
                }
            }

            mProgressView.setVisibility(View.INVISIBLE);
            mBundle = ((WizardSetLoader) loader).getBundleArgs();
            changeView(true);
        } else if (loader instanceof LoginLoader) {
            if (!success) {
                Toast.makeText(this, ((LoginLoader) loader).getLoginError(), Toast.LENGTH_SHORT).show();
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
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

        int id = v.getId();
        switch (id) {
            case R.id.wizard_start:
                String pwd = tlPassword.getEditText().getText().toString();
                String confirm = tlConfirmPassword.getEditText().getText().toString();
                if (pwd.equals("")) {
                    Toast.makeText(this, getString(R.string.empty_password), Toast.LENGTH_SHORT).show();
                    return;
                } else if (pwd.length() < 1 || pwd.length() > 32) {
                    Toast.makeText(this, getString(R.string.password_size) + " 1 ~ 32", Toast.LENGTH_SHORT).show();
                    return;
                } else if (pwd.contains(" ")){
                    Toast.makeText(this, getString(R.string.wizard_password_space_error), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!pwd.equals(confirm)) {
                    Toast.makeText(this, getString(R.string.confirm_password_error), Toast.LENGTH_SHORT).show();
                    return;
                }

                String timezone = Integer.toString(TimeZone.getDefault().getRawOffset() / 3600000);
                Bundle args = new Bundle();
                args.putString("nickname", getString(R.string.wizard_success));
                args.putString("hostname", mHostname);
                args.putString("username", "admin");
                args.putString("password", pwd);
                args.putString("timezone", timezone);
                getLoaderManager().restartLoader(LoaderID.WIZARD_INIT, args, this).forceLoad();
                break;
            case R.id.ready_button:
                if(mBundle != null) {
                    Log.d(TAG, "wizard finish, check camera backup : " + mCameraBackupSwitch.isChecked());
                    NASPref.setBackupSetting(this, mCameraBackupSwitch.isChecked());
                    Intent intent = new Intent();
                    intent.putExtras(mBundle);
                    WizardActivity.this.setResult(RESULT_OK, intent);
                }
                finish();
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
            if(mBundle != null) {
                Log.d(TAG, "wizard finish, check camera backup : " + mCameraBackupSwitch.isChecked());
                NASPref.setBackupSetting(this, mCameraBackupSwitch.isChecked());
                Intent intent = new Intent();
                intent.putExtras(mBundle);
                WizardActivity.this.setResult(RESULT_OK, intent);
                finish();
            }
            else
                super.onBackPressed();
        }
    }
}
