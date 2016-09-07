package com.transcend.nas.connection_new;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.StyleFactory;
import com.transcend.nas.common.TutkCodeID;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.TutkGetNasLoader;
import com.transcend.nas.management.TutkLoginLoader;

/**
 * Created by ikelee on 16/8/22.
 */
public class LoginActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean>,
        View.OnClickListener {

    public static final int REQUEST_CODE = LoginActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = LoginActivity.class.getSimpleName();

    private RelativeLayout mLoginLayout;
    private RelativeLayout mStartLayout;
    private RelativeLayout mProgressView;
    private int mLoaderID;
    private boolean isSignWithOther = false;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_login);
        init();
        initView();
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        mProgressView.setVisibility(View.VISIBLE);
        String server, email, pwd, token;
        switch (mLoaderID = id) {
            case LoaderID.TUTK_LOGIN:
                server = args.getString("server");
                email = args.getString("email");
                pwd = args.getString("password");
                return new TutkLoginLoader(this, server, email, pwd);
            case LoaderID.TUTK_NAS_GET:
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (!success) {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (loader instanceof TutkLoginLoader) {
            checkLoginNASResult((TutkLoginLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            checkGetNASResult((TutkGetNasLoader) loader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private void init(){
        Intent intent = getIntent();
        if(intent != null) {
            isSignWithOther = intent.getBooleanExtra("SignWithOther", false);
        }
    }

    private void initView() {
        //init login layout
        mLoginLayout = (RelativeLayout) findViewById(R.id.login_layout);
        Button facebookLogin = (Button) findViewById(R.id.login_by_facebook);
        facebookLogin.setOnClickListener(this);
        StyleFactory.set_white_button_touch_effect(this, facebookLogin);
        Button emailLogin = (Button) findViewById(R.id.login_by_email);
        emailLogin.setOnClickListener(this);
        StyleFactory.set_white_button_touch_effect(this, emailLogin);
        LinearLayout signIn = (LinearLayout) findViewById(R.id.login_sign_in_layout);
        signIn.setOnClickListener(this);
        TextView signInText = (TextView) findViewById(R.id.login_sign_in);
        signInText.setOnClickListener(this);
        StyleFactory.set_blue_text_touch_effect(this, signInText);

        //init start layout
        mStartLayout = (RelativeLayout) findViewById(R.id.start_layout);
        Button startLogin = (Button) findViewById(R.id.start_login_button);
        startLogin.setOnClickListener(this);
        StyleFactory.set_white_button_touch_effect(this, startLogin);
        TextView signInOther = (TextView) findViewById(R.id.start_sign_in_with_other);
        StyleFactory.set_blue_text_touch_effect(this, signInOther);
        signInOther.setOnClickListener(this);
        TextView accountTitle = (TextView) findViewById(R.id.start_account_title);
        TextView accountContent = (TextView) findViewById(R.id.start_account_content);
        ImageView accountImage = (ImageView) findViewById(R.id.start_account_image);

        String email = NASPref.getCloudUsername(mContext);
        String pwd = NASPref.getCloudPassword(mContext);
        if (!email.equals("") && !pwd.equals("")) {
            accountContent.setText(email);
            String title = email.split("@")[0];
            accountTitle.setText(title);
            if(isSignWithOther)
                showLoginView();
            else
                showStartView();
        } else {
            showLoginView();
        }

        //init progress view
        mProgressView = (RelativeLayout) findViewById(R.id.login_progress_view);
    }

    private void showLoginView() {
        mLoginLayout.setVisibility(View.VISIBLE);
        mStartLayout.setVisibility(View.GONE);
    }

    private void showStartView() {
        mLoginLayout.setVisibility(View.GONE);
        mStartLayout.setVisibility(View.VISIBLE);
    }

    private void checkLoginNASResult(TutkLoginLoader loader) {
        String code = loader.getCode();
        String status = loader.getStatus();
        String token = loader.getAuthToke();
        String email = loader.getEmail();
        String pwd = loader.getPassword();

        if (!token.equals("")) {
            //token not null mean login success
            NASPref.setCloudUsername(mContext, email);
            NASPref.setCloudPassword(mContext, pwd);
            NASPref.setCloudAuthToken(mContext, loader.getAuthToke());
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", loader.getAuthToke());
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, LoginActivity.this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            if (code.equals(TutkCodeID.NOT_VERIFIED)) {
                //account not verified
                NASPref.setCloudAccountStatus(mContext, NASPref.Status.Padding.ordinal());
                NASPref.setCloudUsername(mContext, email);
                NASPref.setCloudPassword(mContext, pwd);
                startLoginByEmailActivity(false, false);
            } else {
                if (!code.equals(""))
                    Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkGetNASResult(TutkGetNasLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();
        NASPref.setCloudAccountStatus(mContext, NASPref.Status.Active.ordinal());

        if (code.equals("")) {
            NASPref.setInitial(this, true);
            Intent intent = new Intent();
            intent.setClass(LoginActivity.this, LoginListActivity.class);
            intent.putExtra("NASList", loader.getNasArrayList());
            intent.putExtra("RemoteAccess", true);
            startActivityForResult(intent, LoginListActivity.REQUEST_CODE);
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void startLoginByEmailActivity(boolean signUp, boolean signWithOther) {
        Intent intent = new Intent();
        intent.putExtra("SignUp", signUp);
        intent.putExtra("SignWithOther", signWithOther);
        intent.setClass(LoginActivity.this, LoginByEmailActivity.class);
        startActivity(intent);
        finish();
    }

    private void startFileManageActivity() {
        Intent intent = new Intent();
        intent.setClass(LoginActivity.this, FileManageActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == LoginListActivity.REQUEST_CODE) {
                startFileManageActivity();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_by_facebook:
                //TODO : start facebook login
                break;
            case R.id.login_by_email:
                startLoginByEmailActivity(true, isSignWithOther);
                break;
            case R.id.start_login_button:
                String email = NASPref.getCloudUsername(mContext);
                String pwd = NASPref.getCloudPassword(mContext);
                if (!email.equals("") && !pwd.equals("")) {
                    Bundle arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", email);
                    arg.putString("password", pwd);
                    getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, LoginActivity.this).forceLoad();
                }
                break;
            case R.id.login_sign_in_layout:
            case R.id.login_sign_in:
                startLoginByEmailActivity(false, isSignWithOther);
                break;
            case R.id.start_sign_in_with_other:
                isSignWithOther = true;
                showLoginView();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if(mProgressView.isShown()){
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
            return;
        }

        if (isSignWithOther) {
            isSignWithOther = false;
            showStartView();
        } else
            super.onBackPressed();
    }

}
