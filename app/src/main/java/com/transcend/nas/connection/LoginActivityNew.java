package com.transcend.nas.connection;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.transcend.nas.LoaderID;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.GoogleAnalysisFactory;
import com.transcend.nas.common.StyleFactory;
import com.transcend.nas.tutk.TutkCodeID;
import com.transcend.nas.tutk.TutkFBLoginLoader;
import com.transcend.nas.tutk.TutkForgetPasswordLoader;
import com.transcend.nas.tutk.TutkGetNasLoader;
import com.transcend.nas.tutk.TutkLoginLoader;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static com.facebook.AccessToken.getCurrentAccessToken;
import static com.transcend.nas.NASPref.getCloudUsername;

/**
 * Created by ikelee on 16/8/22.
 */
public class LoginActivityNew extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean>,
        View.OnClickListener {

    public static final int REQUEST_CODE = LoginActivityNew.class.hashCode() & 0xFFFF;
    public static final String TAG = LoginActivityNew.class.getSimpleName();

    private TextInputLayout mInputLayoutEmail;
    private TextInputLayout mInputLayoutPassword;
    private EditText mEditTextEmail;
    private EditText mEditTextPassword;
    private RelativeLayout mProgressView;
    private int mLoaderID;

    private Context mContext;

    private CallbackManager mCallbackManager;
    private AccessToken mAccessToken;

    private ForgetPwdDialog mForgetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_login_new);
        setBackgroundImage();
        GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.START);
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
            case LoaderID.TUTK_FB_LOGIN:
                server = args.getString("server");
                email = args.getString("email");
                return new TutkFBLoginLoader(this, server, email, args.getString("name"), args.getString("uid"), args.getString("token"));
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

        if (loader instanceof TutkFBLoginLoader) {
            checkFBLoginNASResult((TutkFBLoginLoader) loader);
        } else if (loader instanceof TutkLoginLoader) {
            checkLoginNASResult((TutkLoginLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            checkGetNASResult((TutkGetNasLoader) loader);
        } else if (loader instanceof TutkForgetPasswordLoader) {
            checkForgetPasswordResult((TutkForgetPasswordLoader) loader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private void initView() {
        //init progress view
        mProgressView = (RelativeLayout) findViewById(R.id.login_progress_view);

        configFacebookButton();

        mInputLayoutEmail = (TextInputLayout) findViewById(R.id.input_layout_email);
        mInputLayoutPassword = (TextInputLayout) findViewById(R.id.input_layout_password);
        mEditTextEmail = (EditText) findViewById(R.id.edit_text_email);
        mEditTextPassword = (EditText) findViewById(R.id.edit_text_password);

        Button emailLogin = (Button) findViewById(R.id.login_by_email);
        emailLogin.setOnClickListener(this);
        StyleFactory.set_grey_button_touch_effect(this, emailLogin);

        Button createAccount = (Button) findViewById(R.id.create_account);
        createAccount.setOnClickListener(this);
        StyleFactory.set_blue_button_touch_effect(this, createAccount);

        TextView tvForget = (TextView) findViewById(R.id.forget_password);
        tvForget.setOnClickListener(this);
        StyleFactory.set_blue_text_touch_effect(this, tvForget);

        String email = getCloudUsername(mContext);
        String pwd = NASPref.getCloudPassword(mContext);
        if (!email.equals("") && !pwd.equals("")) {
            if (NASPref.getFBAccountStatus(mContext)) {
                return;
            }
            if (email.contains("@")) {
                mEditTextEmail.setText(email);
                mEditTextPassword.setText(pwd);
            }
        }
    }

    private void configFacebookButton() {
        //init login layout
        Button facebookLogin = (Button) findViewById(R.id.login_by_facebook);
        facebookLogin.setOnClickListener(this);
        StyleFactory.set_blue_button_touch_effect(this, facebookLogin);
        Drawable fbIcon = getResources().getDrawable(R.drawable.com_facebook_button_icon);
        fbIcon.setBounds(0,0,60,60);
        facebookLogin.setCompoundDrawables(fbIcon, null,null,null);
    }

    private void checkLoginNASResult(TutkLoginLoader loader) {
        String token = loader.getAuthToke();
        String email = loader.getEmail();
        String pwd = loader.getPassword();

        if (!token.equals("")) {
            //token not null mean login success
            GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.LoginTutk,
                    GoogleAnalysisFactory.LABEL.LoginByEmail + "_" + GoogleAnalysisFactory.SUCCESS);

            checkLogoutProcess(email);

            NASPref.setFBAccountStatus(mContext, false);
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
                NASPref.setFBAccountStatus(mContext, false);
                NASPref.setCloudUsername(mContext, email);
                NASPref.setCloudPassword(mContext, pwd);
                NASPref.setCloudAuthToken(mContext, "");

                // TODO check this statement
                NASPref.setCloudAccountStatus(this, NASPref.Status.Padding.ordinal());
                startLoginByEmailActivity(false);
            } else {
                if (!code.equals(""))
                    Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();

                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.LoginTutk,
                        GoogleAnalysisFactory.LABEL.LoginByEmail + "_" + status);
            }
        }
    }

    private void checkLogoutProcess(String email) {
        String cloudUserName = NASPref.getCloudUsername(this);
        boolean isLogoutNeeded =  !TextUtils.isEmpty(cloudUserName) && !cloudUserName.equals(email);
        Log.d(TAG, "[Enter] isLogoutNeeded: "+ isLogoutNeeded);
        if (isLogoutNeeded) {
            NASUtils.clearDataAfterLogout(mContext);
        }
    }

    private void checkFBLoginNASResult(TutkFBLoginLoader loader) {
        String token = loader.getAuthToke();

        if (!token.equals("")) {
            //token not null mean login success
            GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.LoginTutk,
                    GoogleAnalysisFactory.LABEL.LoginByFacebook + "_" + GoogleAnalysisFactory.SUCCESS);

            checkLogoutProcess(loader.getEmail());

            NASPref.setFBAccountStatus(mContext, true);
            NASPref.setCloudUsername(mContext, loader.getEmail());
            NASPref.setCloudPassword(mContext, loader.getPassword());
            NASPref.setCloudAuthToken(mContext, token);
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", token);
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            String code = loader.getCode();
            String status = loader.getStatus();
            if (!code.equals(""))
                Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
            // remove the FB authentication if the email address is already taken
            NASUtils.logOutFB(this);
            GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.LoginTutk,
                    GoogleAnalysisFactory.LABEL.LoginByFacebook + "_" + status);
        }
    }

    private void checkGetNASResult(TutkGetNasLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        String status = loader.getStatus();
        String code = loader.getCode();
        if (code.equals("")) {
            startLoginListActivity(loader.getNasArrayList());
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void startLoginListActivity(ArrayList<HashMap<String, String>> arrayList) {
        Intent intent = new Intent();
        intent.setClass(this, LoginListActivity.class);
        intent.putExtra("NASList", arrayList);
        intent.putExtra("RemoteAccess", true);
        startActivity(intent);
        finish();
    }

    private void startLoginByEmailActivity(boolean signUp) {
        Intent intent = new Intent();
        intent.putExtra("SignUp", signUp);
        intent.setClass(this, LoginByEmailActivityNew.class);
        startActivity(intent);
        finish();
    }

    private void loginFBAccount() {
        mProgressView.setVisibility(View.VISIBLE);
        Log.d(TAG, "[Enter] loginFBAccount()");

        if (mCallbackManager == null) {
            registerFBLoginCallback();
        }

        mAccessToken = AccessToken.getCurrentAccessToken();

        Log.d(TAG, "accessToken: " + mAccessToken);

        if (mAccessToken != null && mAccessToken.isExpired() == false) {
            Log.d(TAG, "FB has been login... AccessToken.getCurrentAccessToken(): " + getCurrentAccessToken());

            Set<String> deniedPermissions = mAccessToken.getDeclinedPermissions();

            if (deniedPermissions.contains("email")) {

                Log.d(TAG, "[Enter] deniedPermissions contains email...");

                loginWithReadPermission();

            } else {
                requestFBUserInfo();
            }

        } else {
            loginWithReadPermission();
        }

    }

    private void loginWithReadPermission() {
        Log.d(TAG, "[Enter] logInWithReadPermission()");
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
    }

    private void registerFBLoginCallback() {
        Log.d(TAG, "[Enter] registerFBLoginCallback()");
        mCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {

                Log.d(TAG, "[Enter] onSuccess()");

                mAccessToken = loginResult.getAccessToken();

                Log.d(TAG, "accessToken: " + mAccessToken);

                Set<String> deniedPermissions = loginResult.getRecentlyDeniedPermissions();

                if (deniedPermissions.contains("email")) {

                    Log.d(TAG, "[Enter] deniedPermissions contains email...");

                    loginWithReadPermission();

                    Toast.makeText(LoginActivityNew.this, R.string.fb_request_email_permission, Toast.LENGTH_LONG).show();

                } else {

                    requestFBUserInfo();
                }
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "[Enter] onCancel()");
                mProgressView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d(TAG, "[Enter] onError()");
                Log.d(TAG, exception.toString());
                mProgressView.setVisibility(View.INVISIBLE);
                Toast.makeText(mContext, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestFBUserInfo() {
        Log.d(TAG, "[Enter] requestFBUserInfo()");

        GraphRequest request = GraphRequest.newMeRequest(
                mAccessToken,
                new GraphRequest.GraphJSONObjectCallback() {

                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        Log.d(TAG, "[Enter] onCompleted");
                        if (object != null) {
                            Log.d(TAG, "name: " + object.optString("name"));
                            Log.d(TAG, "email: " + object.optString("email"));
                            Log.d(TAG, "user id: " + mAccessToken.getUserId());
                            Log.d(TAG, "token: " + mAccessToken.getToken());

//                                String url = object.getJSONObject("picture").getJSONObject("data").getString("url");
                            String url = "https://graph.facebook.com/" + mAccessToken.getUserId() + "/picture?type=large";
                            NASPref.setFBProfilePhotoUrl(LoginActivityNew.this, url);

                            Bundle arg = new Bundle();
                            arg.putString("server", NASPref.getCloudServer(mContext));
                            arg.putString("name", object.optString("name"));
                            arg.putString("email", object.optString("email"));
                            arg.putString("uid", mAccessToken.getUserId());
                            arg.putString("token", mAccessToken.getToken());
                            getLoaderManager().restartLoader(LoaderID.TUTK_FB_LOGIN, arg, LoginActivityNew.this).forceLoad();
                        } else {
                            Log.d(TAG, "[Enter] onCompleted Error");
                        }
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "[Enter] onActivityResult()");
        if (resultCode == RESULT_OK) {
            Log.d(TAG, "resultCode == RESULT_OK");
            int fbRequestCode = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode();
            if (requestCode == fbRequestCode) {
                if (mCallbackManager != null && FacebookSdk.isInitialized() == true) {
                    Log.d(TAG, "callbackManager.onActivityResult()");
                    mCallbackManager.onActivityResult(requestCode, resultCode, data);

                    return;
                }
            }
        }

        mProgressView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_by_facebook:
                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.LoginByFacebook);
                loginFBAccount();
                break;
            case R.id.login_by_email:
                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.LoginByEmail);
                loginByEmail();
                break;
            case R.id.create_account:
                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.RegisterEmail);
                startLoginByEmailActivity(true);
                break;
            case R.id.forget_password:
                Bundle arg = new Bundle();
                arg.putString("title", getString(R.string.forget_password_title));
                if (!NASPref.getFBAccountStatus(mContext)) {
                    arg.putString("email", getCloudUsername(mContext));
                }
                mForgetDialog = new ForgetPwdDialog(mContext, arg) {
                    @Override
                    public void onConfirm(Bundle args) {
                        GoogleAnalysisFactory.getInstance(mContext).sendEvent(GoogleAnalysisFactory.VIEW.START_EMAIL_LOGIN, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.ForgetPassword);
                        args.putString("server", NASPref.getCloudServer(mContext));
                        getLoaderManager().restartLoader(LoaderID.TUTK_FORGET_PASSWORD, args, LoginActivityNew.this).forceLoad();
                    }

                    @Override
                    public void onCancel() {
                        getLoaderManager().destroyLoader(LoaderID.TUTK_FORGET_PASSWORD);
                        mForgetDialog.dismiss();
                        mForgetDialog = null;
                    }
                };

                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
            return;
        }

        super.onBackPressed();
    }

    private void setBackgroundImage() {
        ImageView backgroundImage = (ImageView) this.findViewById(R.id.background_image);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowmanager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        windowmanager.getDefaultDisplay().getMetrics(displayMetrics);
        int height = Math.round(displayMetrics.heightPixels / displayMetrics.density);
        int width = Math.round(displayMetrics.widthPixels / displayMetrics.density);
//        Log.d(TAG, "displayMetrics.heightPixels: " + displayMetrics.heightPixels);
//        Log.d(TAG, "displayMetrics.widthPixels: " + displayMetrics.widthPixels);
//        Log.d(TAG, "displayMetrics.density: " + displayMetrics.density);
//        Log.d(TAG, "height: " + height);
//        Log.d(TAG, "width: " + width);
        Bitmap bitmap = StyleFactory.decodeSampledBitmapFromResource(this.getResources(), R.drawable.img_sjc_bg, width, height);
        backgroundImage.setImageBitmap(bitmap);
    }

    private void loginByEmail() {
        if (isValidEmail()) {
            String email = mEditTextEmail.getText().toString();
            String pwd = mEditTextPassword.getText().toString();

            GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START_EMAIL_LOGIN, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.LoginByEmail);
            Bundle arg = new Bundle();
            arg.putString("server", NASPref.getCloudServer(mContext));
            arg.putString("email", email);
            arg.putString("password", pwd);
            getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, this).forceLoad();
        }
    }

    private boolean isValidEmail() {
        String email = mEditTextEmail.getText().toString().trim();
        String pwd = mEditTextPassword.getText().toString();
        boolean isValid = !TextUtils.isEmpty(email) && !TextUtils.isEmpty(pwd) &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();

        if (!isValid) {
            Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
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

}
