package com.transcend.nas;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.transcend.nas.common.StyleFactory;
import com.transcend.nas.connection.StartActivity;

/**
 * Created by ikelee on 16/8/22.
 */
public class LoginActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean>,
        View.OnClickListener {

    public static final int REQUEST_CODE = LoginActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = LoginActivity.class.getSimpleName();

    private Button mFacebookLogin;
    private Button mEmailLogin;
    private TextView mSignin;
    private RelativeLayout mProgressView;
    private int mLoaderID;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_login);
        initView();
        String email = NASPref.getCloudUsername(mContext);
        String pwd = NASPref.getCloudPassword(mContext);
        if (!email.equals("") && !pwd.equals("")) {
            startAppSignInActivity();
        }
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        mProgressView.setVisibility(View.VISIBLE);
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        mProgressView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private void initView() {
        mFacebookLogin = (Button) findViewById(R.id.login_by_facebook);
        mFacebookLogin.setOnClickListener(this);
        StyleFactory.set_white_button_touch_effect(this, mFacebookLogin);
        mEmailLogin = (Button) findViewById(R.id.login_by_email);
        mEmailLogin.setOnClickListener(this);
        StyleFactory.set_white_button_touch_effect(this, mEmailLogin);
        mSignin = (TextView) findViewById(R.id.login_sign_in);
        StyleFactory.set_blue_text_touch_effect(this, mSignin);
        mSignin.setOnClickListener(this);
        mProgressView = (RelativeLayout) findViewById(R.id.login_progress_view);
    }

    private void startAppSignInActivity(){
        Intent intent = new Intent();
        intent.setClass(LoginActivity.this, StartActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.login_by_facebook:
                //TODO : start facebook login
                break;
            case R.id.login_by_email:
                intent = new Intent();
                intent.putExtra("SignUp", true);
                intent.setClass(LoginActivity.this, LoginByEmailActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.login_sign_in:
                intent = new Intent();
                intent.putExtra("SignUp", false);
                intent.setClass(LoginActivity.this, LoginByEmailActivity.class);
                startActivity(intent);
                finish();
                break;
            default:
                break;
        }
    }
}
