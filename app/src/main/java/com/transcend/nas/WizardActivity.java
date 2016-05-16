package com.transcend.nas;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
    private ImageView iBackground;
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
        iBackground = (ImageView) findViewById(R.id.wizard_bg);
        //Point p = new Point();
        //getWindowManager().getDefaultDisplay().getSize(p);
        //Log.d(TAG, "TEST " + p.x);
        //iBackground.setImageBitmap(decodeSampledBitmapFromResource(
        //        getResources(), R.drawable.bg_wizard, p.x, p.y));
        tVInfo = (TextView) findViewById(R.id.wizard_status);
        mProgressView = (RelativeLayout) findViewById(R.id.wizard_progress_view);
    }


    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height
                    / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
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
