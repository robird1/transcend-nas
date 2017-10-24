package com.transcend.nas.connection;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.LoaderID;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.GoogleAnalysisFactory;
import com.transcend.nas.common.StyleFactory;
import com.transcend.nas.tutk.TutkCodeID;
import com.transcend.nas.tutk.TutkGetNasLoader;
import com.transcend.nas.tutk.TutkLoginLoader;
import com.transcend.nas.tutk.TutkRegisterLoader;
import com.transcend.nas.tutk.TutkResendActivateLoader;
import com.transcend.nas.view.NotificationDialog;

/**
 * Created by steve_su on 2017/10/19.
 */

public class LoginByEmailActivityNew extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean>,
        View.OnClickListener {

    public static final int REQUEST_CODE = LoginByEmailActivityNew.class.hashCode() & 0xFFFF;
    public static final String TAG = LoginByEmailActivityNew.class.getSimpleName();

    private RelativeLayout mProgressView;
    private LinearLayout registerLayout;
    private RelativeLayout mIdentifyLayout;

    private TextInputLayout tlEmail;
    private TextInputLayout tlPwd;
    private TextInputLayout tlPwdConfirm;

    private int mLoaderID;
    private boolean isSignUp = false;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_create_account);
        setBackgroundImage();
        GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.START_EMAIL_LOGIN);
        init();
        initProgressView();
        initView();
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
            case LoaderID.TUTK_NAS_GET:
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
            case LoaderID.TUTK_NAS_RESEND_ACTIVATE:
                server = args.getString("server");
                email = args.getString("email");
                return new TutkResendActivateLoader(this, server, email);
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

        if (loader instanceof TutkRegisterLoader) {
            checkRegisterNASResult((TutkRegisterLoader) loader);
        } else if (loader instanceof TutkLoginLoader) {
            checkLoginNASResult((TutkLoginLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            checkGetNASResult((TutkGetNasLoader) loader);
        } else if (loader instanceof TutkResendActivateLoader) {
            checkResendActivateResult((TutkResendActivateLoader) loader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

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
        String token = loader.getAuthToke();
        String email = loader.getEmail();
        String pwd = loader.getPassword();

        if (!token.equals("")) {
            //token not null mean login success
            NASPref.setCloudUsername(mContext, email);
            NASPref.setCloudPassword(mContext, pwd);
            NASPref.setCloudAuthToken(mContext, token);
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", token);
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            String code = loader.getCode();
            String status = loader.getStatus();
            if (code.equals(TutkCodeID.NOT_VERIFIED)) {
                //account not verified
                NASPref.setCloudAccountStatus(mContext, NASPref.Status.Padding.ordinal());
                NASPref.setCloudUsername(mContext, email);
                NASPref.setCloudPassword(mContext, pwd);
                NASPref.setCloudAuthToken(mContext, "");
                showIdentifyView();
            } else {
                if (!code.equals(""))
                    Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkGetNASResult(TutkGetNasLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        String status = loader.getStatus();
        String code = loader.getCode();
        NASPref.setCloudAccountStatus(mContext, NASPref.Status.Active.ordinal());

        if (code.equals("")) {
            Intent intent = new Intent();
            intent.setClass(this, LoginListActivity.class);
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
            showLoginView();
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
                GoogleAnalysisFactory.getInstance(mContext).sendEvent(GoogleAnalysisFactory.VIEW.START_EMAIL_LOGIN, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.ResendEmail);
                getLoaderManager().restartLoader(loaderID, args, LoginByEmailActivityNew.this).forceLoad();
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
        }
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.settings_progress_view);
    }

    private void initView() {
        registerLayout = (LinearLayout) findViewById(R.id.remote_access_register_layout);
        mIdentifyLayout = (RelativeLayout) findViewById(R.id.remote_access_normal_layout);

        if (isSignUp) {
            showRegisterView();
        } else {
            int status = NASPref.getCloudAccountStatus(this);
            if (status == NASPref.Status.Padding.ordinal()) {
                showIdentifyView();
            } else if (status == NASPref.Status.Active.ordinal() || status == NASPref.Status.Bind.ordinal()) {
                showIdentifyView();
            } else {
                showLoginView();
            }
        }
    }

    private void showLoginView() {
        Intent intent = new Intent();
        intent.setClass(this, LoginActivityNew.class);
        startActivity(intent);
        finishAffinity();
    }

    private void showRegisterView() {
        registerLayout.setVisibility(View.VISIBLE);
        mIdentifyLayout.setVisibility(View.GONE);
        tlEmail = (TextInputLayout) findViewById(R.id.remote_access_register_email);
//        tlEmail.getEditText().setText(NASPref.getCloudUsername(mContext));
        tlEmail.getEditText().setText("");
        tlPwd = (TextInputLayout) findViewById(R.id.remote_access_register_password);
        tlPwd.getEditText().setText("");
        tlPwdConfirm = (TextInputLayout) findViewById(R.id.remote_access_register_password_confirm);
        tlPwdConfirm.getEditText().setText("");
        Button btSubmit = (Button) findViewById(R.id.remote_access_register_submit);
        btSubmit.setOnClickListener(this);
        StyleFactory.set_blue_button_touch_effect(this, btSubmit);
    }

    private void showIdentifyView() {
        registerLayout.setVisibility(View.GONE);
        mIdentifyLayout.setVisibility(View.VISIBLE);

        TextView tvEmail = (TextView) findViewById(R.id.start_account_title);
        tvEmail.setText(NASPref.getCloudUsername(mContext));
        Button loginButton = (Button) findViewById(R.id.identify_login_button);
        loginButton.setOnClickListener(this);
        StyleFactory.set_blue_button_touch_effect(this, loginButton);
        Button resendButton = (Button) findViewById(R.id.identify_resend_button);
        resendButton.setOnClickListener(this);
        StyleFactory.set_grey_button_touch_effect(this, resendButton);
    }

    private void startLoginActivity() {
        Intent intent = new Intent();
        intent.setClass(this, LoginActivityNew.class);
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
            case R.id.identify_resend_button:
                arg = new Bundle();
                arg.putString("server", NASPref.getCloudServer(mContext));
                arg.putString("email", NASPref.getCloudUsername(mContext));
                showNotificationDialog(getString(R.string.remote_access_send_activate_warning), LoaderID.TUTK_NAS_RESEND_ACTIVATE, arg);
                break;
            case R.id.identify_login_button:
                email = NASPref.getCloudUsername(mContext);
                pwd = NASPref.getCloudPassword(mContext);
                if (!email.equals("") && !pwd.equals("")) {
                    arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", email);
                    arg.putString("password", pwd);
                    getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, this).forceLoad();
                }
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
                getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, this).forceLoad();
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
                getLoaderManager().restartLoader(LoaderID.TUTK_REGISTER, arg, this).forceLoad();
                break;
            default:
                break;
        }
    }

    private void setBackgroundImage() {
        ImageView backgroundImage = (ImageView) this.findViewById(R.id.background_image);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowmanager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        windowmanager.getDefaultDisplay().getMetrics(displayMetrics);
        int height = Math.round(displayMetrics.heightPixels / displayMetrics.density);
        int width = Math.round(displayMetrics.widthPixels / displayMetrics.density);
        Bitmap bitmap = StyleFactory.decodeSampledBitmapFromResource(this.getResources(), R.drawable.sjc_bg3_logo, width, height);
        backgroundImage.setImageBitmap(bitmap);
    }

}
