package com.transcend.nas.connection;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.NotificationDialog;
import com.transcend.nas.common.TutkCodeID;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.TutkForgetPasswordLoader;
import com.transcend.nas.management.TutkGetNasLoader;
import com.transcend.nas.management.TutkLoginLoader;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by silverhsu on 16/2/2.
 */
public class SignInActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean>, View.OnClickListener {

    private static final String TAG = SignInActivity.class.getSimpleName();

    private TextInputLayout tlEmail;
    private TextInputLayout tlPwd;
    private Button bnSignIn;
    private TextView tvSignInForget;
    private TextView tvFindDevice;
    private ForgetPwdDialog mForgetDialog;
    private RelativeLayout mProgressView;
    private int mLoaderID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        initInputText();
        initSignInButton();
        initSignInForgetButton();
        initFindDeviceTextView();
        initProgressView();
    }

    private void initInputText() {
        tlEmail = (TextInputLayout) findViewById(R.id.activity_sign_in_email);
        tlEmail.getEditText().setText(NASPref.getCloudUsername(this));
        tlPwd = (TextInputLayout) findViewById(R.id.activity_sign_in_password);
    }

    private void initSignInButton() {
        bnSignIn = (Button) findViewById(R.id.activity_sign_in_button);
        bnSignIn.setOnClickListener(this);
    }

    private void initSignInForgetButton() {
        tvSignInForget = (TextView) findViewById(R.id.activity_sing_in_forget);
        tvSignInForget.setOnClickListener(this);
    }

    private void initFindDeviceTextView() {
        tvFindDevice = (TextView) findViewById(R.id.activity_sign_in_find_device);
        tvFindDevice.setOnClickListener(this);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.activity_sign_in_progress_view);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(bnSignIn)) {
            // TODO : connect to tutk db server and sign in
            String email = tlEmail.getEditText().getText().toString();
            String pwd = tlPwd.getEditText().getText().toString();
            if (email.equals("")) {
                Toast.makeText(this, getString(R.string.empty_email), Toast.LENGTH_SHORT).show();
                return;
            }

            if (pwd.equals("")) {
                Toast.makeText(this, getString(R.string.empty_password), Toast.LENGTH_SHORT).show();
                return;
            }

            Bundle args = new Bundle();
            args.putString("server", NASPref.getCloudServer(this));
            args.putString("email", email);
            args.putString("password", pwd);
            getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, args, this).forceLoad();
        } else if (v.equals(tvFindDevice)) {
            startNASFinderActivity(null, false);
        } else if (v.equals(tvSignInForget)) {
            // TODO : pop up forget password dialog
            showForgetPwdDialog();
        }
    }

    private void startFileManageActivity() {
        Intent intent = new Intent();
        intent.setClass(SignInActivity.this, FileManageActivity.class);
        startActivity(intent);
        finish();
    }

    private void startNASFinderActivity(ArrayList<HashMap<String, String>> list, boolean isRemoteAccess) {
        Intent intent = new Intent();
        intent.setClass(SignInActivity.this, NASFinderActivity.class);
        if(isRemoteAccess) {
            intent.putExtra("NASList", list);
            intent.putExtra("RemoteAccess", isRemoteAccess);
        }
        startActivity(intent);
        finish();
    }

    private void checkForgetPasswordResult(TutkForgetPasswordLoader loader){
        String code = loader.getCode();
        String status = loader.getStatus();

        if (mForgetDialog != null)
            mForgetDialog.hideProgress();

        if (code.equals(TutkCodeID.SUCCESS)) {
            if (mForgetDialog != null) {
                mForgetDialog.dismiss();
                mForgetDialog = null;
            }
            Toast.makeText(this, getString(R.string.forget_password_send), Toast.LENGTH_SHORT).show();
        } else {
            if(!code.equals(""))
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLoginNASResult(TutkLoginLoader loader) {
        String code = loader.getCode();
        String status = loader.getStatus();

        String token = loader.getAuthToke();
        String email = loader.getEmail();
        String pwd = loader.getPassword();
        if (!token.equals("")) {
            //token not null mean login success
            NASPref.setCloudUsername(this, email);
            NASPref.setCloudPassword(this, pwd);
            NASPref.setCloudAuthToken(this, token);

            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", token);
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, this).forceLoad();
        } else {
            if (code.equals(TutkCodeID.NOT_VERIFIED)) {
                //account not verified
                NASPref.setCloudUsername(this, email);
                NASPref.setCloudPassword(this, pwd);
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
            } else {
                if (!code.equals(""))
                    Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkGetNASResult(TutkGetNasLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();

        if (code.equals("")) {
            ArrayList<HashMap<String, String>> mNASList = loader.getNasArrayList();
            startNASFinderActivity(mNASList, true);
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void showForgetPwdDialog() {
        Bundle args = new Bundle();
        args.putString("title", getString(R.string.forget_password_title));
        args.putString("email", "");
        mForgetDialog = new ForgetPwdDialog(SignInActivity.this, args) {
            @Override
            public void onConfirm(Bundle args) {
                startForgetPwdLoader(args);
            }

            @Override
            public void onCancel() {
                getLoaderManager().destroyLoader(LoaderID.TUTK_FORGET_PASSWORD);
                mForgetDialog.dismiss();
                mForgetDialog = null;
            }
        };
    }

    private void startForgetPwdLoader(Bundle args) {
        getLoaderManager().restartLoader(LoaderID.TUTK_FORGET_PASSWORD, args, this).forceLoad();
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        String server, email, pwd, token;
        switch (mLoaderID = id) {
            case LoaderID.TUTK_FORGET_PASSWORD:
                server = NASPref.getCloudServer(this);
                email = args.getString("email");
                return new TutkForgetPasswordLoader(this, server, email);
            case LoaderID.TUTK_LOGIN:
                mProgressView.setVisibility(View.VISIBLE);
                server = args.getString("server");
                email = args.getString("email");
                pwd = args.getString("password");
                return new TutkLoginLoader(this, server, email, pwd);
            case LoaderID.TUTK_NAS_GET:
                mProgressView.setVisibility(View.VISIBLE);
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);

        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        mProgressView.setVisibility(View.INVISIBLE);
        if(!success){
            Toast.makeText(this,getString(R.string.network_error),Toast.LENGTH_SHORT).show();
            return;
        }

        if (loader instanceof TutkForgetPasswordLoader) {
            checkForgetPasswordResult((TutkForgetPasswordLoader) loader);
        } else if (loader instanceof TutkLoginLoader) {
            checkLoginNASResult((TutkLoginLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            checkGetNASResult((TutkGetNasLoader) loader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    @Override
    public void onBackPressed() {
        if (mForgetDialog != null) {
            getLoaderManager().destroyLoader(LoaderID.TUTK_FORGET_PASSWORD);
            mForgetDialog.dismiss();
            mForgetDialog = null;
        } else if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
        } else {
            super.onBackPressed();
        }
    }
}
