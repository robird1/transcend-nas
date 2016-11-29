package com.transcend.nas.connection;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.GoogleAnalysisFactory;
import com.transcend.nas.LoaderID;
import com.transcend.nas.common.StyleFactory;
import com.transcend.nas.tutk.TutkCodeID;
import com.transcend.nas.tutk.TutkCreateNasLoader;
import com.transcend.nas.tutk.TutkForgetPasswordLoader;
import com.transcend.nas.tutk.TutkGetNasLoader;
import com.transcend.nas.tutk.TutkLoginLoader;
import com.transcend.nas.tutk.TutkRegisterLoader;
import com.transcend.nas.tutk.TutkResendActivateLoader;
import com.transcend.nas.view.NotificationDialog;


/**
 * Created by ikelee on 16/8/22.
 */
public class LoginByEmailActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean>,
        View.OnClickListener {

    public static final int REQUEST_CODE = LoginByEmailActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = LoginByEmailActivity.class.getSimpleName();

    private TextView mTitle;
    private RelativeLayout mProgressView;
    private RelativeLayout loginLayout;
    private RelativeLayout registerLayout;
    private RelativeLayout normalLayout;

    private TextInputLayout tlEmail;
    private TextInputLayout tlPwd;
    private TextInputLayout tlPwdConfirm;

    private int mLoaderID;
    private boolean isRemoteAccessRegister = false;
    private ForgetPwdDialog mForgetDialog;
    private boolean isSignUp = false;
    private boolean isSignWithOther = false;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_login_by_email);
        GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.START_EMAIL_LOGIN);
        init();
        initToolbar();
        initProgressView();
        initView();
        /*String email = NASPref.getCloudUsername(mContext);
        String pwd = NASPref.getCloudPassword(mContext);
        if (!email.equals("") && !pwd.equals("")) {
            Bundle arg = new Bundle();
            arg.putString("server", NASPref.getCloudServer(mContext));
            arg.putString("email", email);
            arg.putString("password", pwd);
            getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, LoginByEmailActivity.this).forceLoad();
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startLoginActivity();
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
            startLoginActivity();
        }
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        String server, email, pwd, token, nasName, nasUUID;
        mProgressView.setVisibility(View.VISIBLE);

        switch (mLoaderID = id) {
            case LoaderID.TUTK_REGISTER:
                server = args.getString("server");
                email = args.getString("email");
                pwd = args.getString("password");
                nasName = args.getString("nasName");
                nasUUID = args.getString("nasUUID");
                return new TutkRegisterLoader(this, server, email, pwd, nasName, nasUUID);
            case LoaderID.TUTK_LOGIN:
                server = args.getString("server");
                email = args.getString("email");
                pwd = args.getString("password");
                return new TutkLoginLoader(this, server, email, pwd);
            case LoaderID.TUTK_NAS_CREATE:
                server = args.getString("server");
                token = args.getString("token");
                nasName = args.getString("nasName");
                nasUUID = args.getString("nasUUID");
                return new TutkCreateNasLoader(this, server, token, nasName, nasUUID);
            case LoaderID.TUTK_NAS_GET:
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
            case LoaderID.TUTK_NAS_RESEND_ACTIVATE:
                server = args.getString("server");
                email = args.getString("email");
                return new TutkResendActivateLoader(this, server, email);
            case LoaderID.TUTK_FORGET_PASSWORD:
                server = args.getString("server");
                email = args.getString("email");
                return new TutkForgetPasswordLoader(this, server, email);
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

        if (loader instanceof TutkForgetPasswordLoader) {
            checkForgetPasswordResult((TutkForgetPasswordLoader) loader);
        } else if (loader instanceof TutkRegisterLoader) {
            checkRegisterNASResult((TutkRegisterLoader) loader);
        } else if (loader instanceof TutkLoginLoader) {
            checkLoginNASResult((TutkLoginLoader) loader);
        } else if (loader instanceof TutkCreateNasLoader) {
            checkCreateNASResult((TutkCreateNasLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            checkGetNASResult((TutkGetNasLoader) loader);
        } else if (loader instanceof TutkResendActivateLoader) {
            checkResendActivateResult((TutkResendActivateLoader) loader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private void checkForgetPasswordResult(TutkForgetPasswordLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        String code = loader.getCode();
        String status = loader.getStatus();

        if (mForgetDialog != null)
            mForgetDialog.hideProgress();

        if (code.equals(TutkCodeID.SUCCESS)) {
            if (mForgetDialog != null) {
                mForgetDialog.dismiss();
                mForgetDialog = null;
            }

            NASPref.setCloudAccountStatus(this, NASPref.Status.Inactive.ordinal());
            NASPref.setCloudPassword(this, "");
            NASPref.setCloudAuthToken(this, "");
            NASPref.setCloudUUID(this, "");
            NASPref.setBackupScenario(this, false);
            Toast.makeText(this, getString(R.string.forget_password_send), Toast.LENGTH_SHORT).show();
        } else {
            if (!code.equals(""))
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkRegisterNASResult(TutkRegisterLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();

        if ((code.equals(TutkCodeID.SUCCESS))) {
            Bundle arg = new Bundle();
            arg.putString("server", NASPref.getCloudServer(mContext));
            arg.putString("email", loader.getEmail());
            arg.putString("password", loader.getPassword());
            getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            if (!code.equals(""))
                Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
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
            isRemoteAccessRegister = true;
            NASPref.setCloudUsername(mContext, email);
            NASPref.setCloudPassword(mContext, pwd);
            NASPref.setCloudAuthToken(mContext, loader.getAuthToke());
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", loader.getAuthToke());
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, LoginByEmailActivity.this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            if (code.equals(TutkCodeID.NOT_VERIFIED)) {
                //account not verified
                isRemoteAccessRegister = true;
                NASPref.setCloudAccountStatus(mContext, NASPref.Status.Padding.ordinal());
                NASPref.setCloudUsername(mContext, email);
                NASPref.setCloudPassword(mContext, pwd);
                NASPref.setCloudAuthToken(mContext, "");
                updateView();
            } else {
                isRemoteAccessRegister = false;
                if (!code.equals(""))
                    Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkCreateNASResult(TutkCreateNasLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();

        if (code.equals("") || code.equals(TutkCodeID.SUCCESS) || code.equals(TutkCodeID.UID_ALREADY_TAKEN)) {
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", loader.getAuthToken());
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, LoginByEmailActivity.this).forceLoad();
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkGetNASResult(TutkGetNasLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        String status = loader.getStatus();
        String code = loader.getCode();
        NASPref.setCloudAccountStatus(mContext, NASPref.Status.Active.ordinal());

        if (code.equals("")) {
            Intent intent = new Intent();
            intent.setClass(LoginByEmailActivity.this, LoginListActivity.class);
            intent.putExtra("NASList", loader.getNasArrayList());
            intent.putExtra("RemoteAccess", true);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkResendActivateResult(TutkResendActivateLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();
        if (code.equals(TutkCodeID.SUCCESS))
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        else if (code.equals(TutkCodeID.VERIFICATIONEXPIRED)) {
            isRemoteAccessRegister = false;
            updateView();
        } else
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void showNotificationDialog(String title, final int loaderID, final Bundle args) {
        Bundle value = new Bundle();
        value.putString(NotificationDialog.DIALOG_MESSAGE, title);
        NotificationDialog mNotificationDialog = new NotificationDialog(this, value) {
            @Override
            public void onConfirm() {
                if (loaderID == LoaderID.TUTK_LOGOUT) {
                    NASPref.setCloudAccountStatus(mContext, NASPref.Status.Inactive.ordinal());
                    NASPref.setCloudPassword(mContext, "");
                    NASPref.setCloudAuthToken(mContext, "");
                    NASPref.setCloudUUID(mContext, "");
                    NASPref.setBackupScenario(mContext, false);
                    isRemoteAccessRegister = false;
                    updateView();
                } else {
                    GoogleAnalysisFactory.getInstance(mContext).sendEvent(GoogleAnalysisFactory.VIEW.START_EMAIL_LOGIN, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.ResendEmail);
                    getLoaderManager().restartLoader(loaderID, args, LoginByEmailActivity.this).forceLoad();
                }
            }

            @Override
            public void onCancel() {

            }
        };
    }

    /**
     * INITIALIZATION
     */
    private void init() {
        Intent intent = getIntent();
        if (intent != null) {
            isSignUp = intent.getBooleanExtra("SignUp", false);
            isSignWithOther = intent.getBooleanExtra("SignWithOther", false);
        }
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
        mTitle = (TextView) toolbar.findViewById(R.id.settings_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.settings_progress_view);
    }

    private void initView() {
        registerLayout = (RelativeLayout) findViewById(R.id.remote_access_register_layout);
        loginLayout = (RelativeLayout) findViewById(R.id.remote_access_login_layout);
        normalLayout = (RelativeLayout) findViewById(R.id.remote_access_normal_layout);
        if(isSignWithOther) {
            isRemoteAccessRegister = false;
        } else {
            int status = NASPref.getCloudAccountStatus(this);
            if (status == NASPref.Status.Padding.ordinal()) {
                isRemoteAccessRegister = true;
            } else if (status == NASPref.Status.Active.ordinal() || status == NASPref.Status.Bind.ordinal()) {
                isRemoteAccessRegister = true;
            } else {
                isRemoteAccessRegister = false;
            }
        }
        updateView();
    }

    private void showLoginView() {
        registerLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.VISIBLE);
        normalLayout.setVisibility(View.GONE);
        mTitle.setText(getString(R.string.sign_in));
        tlEmail = (TextInputLayout) findViewById(R.id.remote_access_login_email);
        tlEmail.getEditText().setText(NASPref.getCloudUsername(mContext));
        tlPwd = (TextInputLayout) findViewById(R.id.remote_access_login_password);
        tlPwd.getEditText().setText("");
        Button btLogin = (Button) findViewById(R.id.remote_access_login_login);
        btLogin.setOnClickListener(this);
        TextView tvForget = (TextView) findViewById(R.id.remote_access_login_forget);
        tvForget.setOnClickListener(this);
        StyleFactory.set_blue_text_touch_effect(this, tvForget);
        TextView tvSignUp = (TextView) findViewById(R.id.remote_access_login_sign_up);
        tvSignUp.setOnClickListener(this);
        StyleFactory.set_blue_text_touch_effect(this, tvSignUp);
    }

    private void showRegisterView() {
        registerLayout.setVisibility(View.VISIBLE);
        loginLayout.setVisibility(View.GONE);
        normalLayout.setVisibility(View.GONE);
        mTitle.setText(getString(R.string.sign_up));
        tlEmail = (TextInputLayout) findViewById(R.id.remote_access_register_email);
        tlEmail.getEditText().setText(NASPref.getCloudUsername(mContext));
        tlPwd = (TextInputLayout) findViewById(R.id.remote_access_register_password);
        tlPwd.getEditText().setText("");
        tlPwdConfirm = (TextInputLayout) findViewById(R.id.remote_access_register_password_confirm);
        tlPwdConfirm.getEditText().setText("");
        Button btSubmit = (Button) findViewById(R.id.remote_access_register_submit);
        btSubmit.setOnClickListener(this);
        TextView tvSignIn = (TextView) findViewById(R.id.remote_access_login_sign_in);
        tvSignIn.setOnClickListener(this);
        StyleFactory.set_blue_text_touch_effect(this, tvSignIn);
    }

    private void showNormalView() {
        registerLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.GONE);
        normalLayout.setVisibility(View.VISIBLE);
        mTitle.setText(getString(R.string.remote_access));

        TextView tvEmail = (TextView) findViewById(R.id.remote_access_email);
        tvEmail.setText(NASPref.getCloudUsername(mContext));
        Button btContinue = (Button) findViewById(R.id.remote_access_continue_button);
        btContinue.setOnClickListener(this);
        TextView tvResend = (TextView) findViewById(R.id.remote_access_resend_text);
        tvResend.setOnClickListener(this);
        StyleFactory.set_blue_text_touch_effect(this, tvResend);
        Button btDelete = (Button) findViewById(R.id.remote_access_delete_button);
        btDelete.setOnClickListener(this);
    }

    private void updateView() {
        if (isRemoteAccessRegister) {
            showNormalView();
        } else {
            if (isSignUp)
                showRegisterView();
            else
                showLoginView();
        }
    }

    private void startLoginActivity() {
        Intent intent = new Intent();
        intent.setClass(LoginByEmailActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        String email, pwd, pwdConfirm;
        Bundle arg;

        switch (v.getId()) {
            case R.id.remote_access_login_sign_up:
                showRegisterView();
                break;
            case R.id.remote_access_login_sign_in:
                showLoginView();
                break;
            case R.id.remote_access_resend_text:
                arg = new Bundle();
                arg.putString("server", NASPref.getCloudServer(mContext));
                arg.putString("email", NASPref.getCloudUsername(mContext));
                showNotificationDialog(getString(R.string.remote_access_send_activate_warning), LoaderID.TUTK_NAS_RESEND_ACTIVATE, arg);
                break;
            case R.id.remote_access_continue_button:
                email = NASPref.getCloudUsername(mContext);
                pwd = NASPref.getCloudPassword(mContext);
                if (isRemoteAccessRegister && !email.equals("") && !pwd.equals("")) {
                    arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", email);
                    arg.putString("password", pwd);
                    getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, LoginByEmailActivity.this).forceLoad();
                }
                break;
            case R.id.remote_access_delete_button:
                showNotificationDialog(getString(R.string.remote_access_logout), LoaderID.TUTK_LOGOUT, null);
                break;
            case R.id.remote_access_login_login:
                email = tlEmail.getEditText().getText().toString();
                pwd = tlPwd.getEditText().getText().toString();

                if (email.equals("")) {
                    Toast.makeText(mContext, getString(R.string.empty_email), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (pwd.equals("")) {
                    Toast.makeText(mContext, getString(R.string.empty_password), Toast.LENGTH_SHORT).show();
                    return;
                }

                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START_EMAIL_LOGIN, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.LoginByEmail);
                arg = new Bundle();
                arg.putString("server", NASPref.getCloudServer(mContext));
                arg.putString("email", email);
                arg.putString("password", pwd);
                getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, LoginByEmailActivity.this).forceLoad();
                break;
            case R.id.remote_access_register_submit:
                email = tlEmail.getEditText().getText().toString();
                pwd = tlPwd.getEditText().getText().toString();
                pwdConfirm = tlPwdConfirm.getEditText().getText().toString();

                if (email.equals("")) {
                    Toast.makeText(mContext, getString(R.string.empty_email), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (pwd.equals("")) {
                    Toast.makeText(mContext, getString(R.string.empty_password), Toast.LENGTH_SHORT).show();
                    return;
                } else if (pwd.length() < 6 || pwd.length() > 20) {
                    Toast.makeText(mContext, getString(R.string.password_size) + " 6 ~ 20", Toast.LENGTH_SHORT).show();
                    return;
                }

                //check password and confirm password
                if (!pwd.equals(pwdConfirm)) {
                    Toast.makeText(mContext, getString(R.string.confirm_password_error), Toast.LENGTH_SHORT).show();
                    return;
                }

                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START_EMAIL_LOGIN, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.RegisterEmail);
                arg = new Bundle();
                arg.putString("server", NASPref.getCloudServer(mContext));
                arg.putString("email", email);
                arg.putString("password", pwd);
                getLoaderManager().restartLoader(LoaderID.TUTK_REGISTER, arg, LoginByEmailActivity.this).forceLoad();
                break;
            case R.id.remote_access_login_forget:
                arg = new Bundle();
                arg.putString("title", getString(R.string.forget_password_title));
                arg.putString("email", NASPref.getCloudUsername(mContext));
                mForgetDialog = new ForgetPwdDialog(mContext, arg) {
                    @Override
                    public void onConfirm(Bundle args) {
                        GoogleAnalysisFactory.getInstance(mContext).sendEvent(GoogleAnalysisFactory.VIEW.START_EMAIL_LOGIN, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.ForgetPassword);
                        args.putString("server", NASPref.getCloudServer(mContext));
                        getLoaderManager().restartLoader(LoaderID.TUTK_FORGET_PASSWORD, args, LoginByEmailActivity.this).forceLoad();
                    }

                    @Override
                    public void onCancel() {
                        getLoaderManager().destroyLoader(LoaderID.TUTK_FORGET_PASSWORD);
                        mForgetDialog.dismiss();
                        mForgetDialog = null;
                    }
                };
            default:
                break;
        }
    }
}
