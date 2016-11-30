package com.transcend.nas.connection;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.transcend.nas.R;
import com.transcend.nas.common.GoogleAnalysisFactory;
import com.transcend.nas.common.StyleFactory;
import com.transcend.nas.tutk.TutkCodeID;
import com.transcend.nas.tutk.TutkFBLoginLoader;
import com.transcend.nas.tutk.TutkGetNasLoader;
import com.transcend.nas.tutk.TutkLoginLoader;
import com.transcend.nas.view.NotificationDialog;

import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import static com.facebook.AccessToken.getCurrentAccessToken;

/**
 * Created by ikelee on 16/8/22.
 */
public class LoginActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean>,
        View.OnClickListener {

    private static final int FB_PROFILE_PHOTO = 999;

    public static final int REQUEST_CODE = LoginActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = LoginActivity.class.getSimpleName();

    private RelativeLayout mLoginLayout;
    private RelativeLayout mStartLayout;
    private RelativeLayout mProgressView;
    private int mLoaderID;

    private Context mContext;

    private CallbackManager mCallbackManager;
    private AccessToken mAccessToken;

    private ImageView mAccountImage;
    private Bitmap mPhotoBitmap;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case FB_PROFILE_PHOTO:
                    mAccountImage.setImageBitmap(mPhotoBitmap);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_login);

        setBackgroundImage();

        GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.START);

        initView();

        if (NASPref.getFBAccountStatus(this))
        {
            setFBPhoto();
        }

    }

    private void setFBPhoto()
    {
        final String storedUrl = NASPref.getFBProfilePhotoUrl(this);
        if (storedUrl != null)
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(storedUrl);
                        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                        HttpsURLConnection.setFollowRedirects(true);
                        connection.setInstanceFollowRedirects(true);
                        mPhotoBitmap = BitmapFactory.decodeStream(connection.getInputStream());
                        if (mPhotoBitmap != null) {
                            Message msg = new Message();
                            msg.what = FB_PROFILE_PHOTO;
                            mHandler.sendMessage(msg);
                        }

                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
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
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private void initView() {
        //init progress view
        mProgressView = (RelativeLayout) findViewById(R.id.login_progress_view);

        //init login layout
        mLoginLayout = (RelativeLayout) findViewById(R.id.login_layout);
        Button facebookLogin = (Button) findViewById(R.id.login_by_facebook);
        facebookLogin.setOnClickListener(this);
        StyleFactory.set_white_button_touch_effect(this, facebookLogin);
        Button emailLogin = (Button) findViewById(R.id.login_by_email);
        emailLogin.setOnClickListener(this);
        StyleFactory.set_white_button_touch_effect(this, emailLogin);
        LinearLayout signUp = (LinearLayout) findViewById(R.id.login_sign_up_layout);
        signUp.setOnClickListener(this);
        TextView signInText = (TextView) findViewById(R.id.login_sign_up);
        signInText.setOnClickListener(this);
        StyleFactory.set_blue_text_touch_effect(this, signInText);
        if(!NASPref.useFacebookLogin){
            RelativeLayout fbLayout = (RelativeLayout) findViewById(R.id.login_fb_layout);
            fbLayout.setVisibility(View.GONE);
//            LinearLayout orLayout = (LinearLayout) findViewById(R.id.login_or_layout);
//            orLayout.setVisibility(View.INVISIBLE);
        }

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
        mAccountImage = (ImageView) findViewById(R.id.start_account_image);
        if(NASPref.getFBAccountStatus(mContext))
            mAccountImage.setImageResource(R.drawable.icon_facebook_24dp);

        String email = NASPref.getCloudUsername(mContext);
        String pwd = NASPref.getCloudPassword(mContext);
        if (!email.equals("") && !pwd.equals("")) {
            accountContent.setText(email);
            String title = email.split("@")[0];
            accountTitle.setText(title);
            showStartView();
        } else {
            showLoginView();
        }
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
        String token = loader.getAuthToke();
        String email = loader.getEmail();
        String pwd = loader.getPassword();

        if (!token.equals("")) {
            //token not null mean login success
            GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.LoginTutk,
                    GoogleAnalysisFactory.LABEL.LoginByEmail + "_" + GoogleAnalysisFactory.SUCCESS);
            NASPref.setFBAccountStatus(mContext, false);
            NASPref.setCloudUsername(mContext, email);
            NASPref.setCloudPassword(mContext, pwd);
            NASPref.setCloudAuthToken(mContext, token);
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", token);
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, LoginActivity.this).forceLoad();
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
                startLoginByEmailActivity(false, false);
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

    private void checkFBLoginNASResult(TutkFBLoginLoader loader) {
        String token = loader.getAuthToke();

        if (!token.equals("")) {
            //token not null mean login success
            GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.LoginTutk,
                    GoogleAnalysisFactory.LABEL.LoginByFacebook + "_" + GoogleAnalysisFactory.SUCCESS);
            NASPref.setFBAccountStatus(mContext, true);
            NASPref.setCloudUsername(mContext, loader.getEmail());
            NASPref.setCloudPassword(mContext, loader.getPassword());
            NASPref.setCloudAuthToken(mContext, token);
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", token);
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, LoginActivity.this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            String code = loader.getCode();
            String status = loader.getStatus();
            if (!code.equals(""))
                Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
            // remove the FB authentication if the email address is already taken
            NASPref.logOutFB(this);
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
        intent.setClass(LoginActivity.this, LoginListActivity.class);
        intent.putExtra("NASList", arrayList);
        intent.putExtra("RemoteAccess", true);
        startActivity(intent);
        finish();
    }

    private void startLoginByEmailActivity(boolean signUp, boolean signWithOther) {
        Intent intent = new Intent();
        intent.putExtra("SignUp", signUp);
        intent.putExtra("SignWithOther", signWithOther);
        intent.setClass(LoginActivity.this, LoginByEmailActivity.class);
        startActivity(intent);
        finish();
    }

    private void loginFBAccount() {
        mProgressView.setVisibility(View.VISIBLE);
        Log.d(TAG, "[Enter] loginFBAccount()");

        if (mCallbackManager ==  null) {
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
        LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, Arrays.asList("public_profile", "email"));
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

                    Toast.makeText(LoginActivity.this, R.string.fb_request_email_permission, Toast.LENGTH_LONG).show();

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
                        if(object != null) {
                            Log.d(TAG, "name: " + object.optString("name"));
                            Log.d(TAG, "email: " + object.optString("email"));
                            Log.d(TAG, "user id: " + mAccessToken.getUserId());
                            Log.d(TAG, "token: " + mAccessToken.getToken());

//                                String url = object.getJSONObject("picture").getJSONObject("data").getString("url");
                            String url = "https://graph.facebook.com/" + mAccessToken.getUserId() + "/picture?type=large";
                            NASPref.setFBProfilePhotoUrl(LoginActivity.this, url);

                            Bundle arg = new Bundle();
                            arg.putString("server", NASPref.getCloudServer(mContext));
                            arg.putString("name", object.optString("name"));
                            arg.putString("email", object.optString("email"));
                            arg.putString("uid", mAccessToken.getUserId());
                            arg.putString("token", mAccessToken.getToken());
                            getLoaderManager().restartLoader(LoaderID.TUTK_FB_LOGIN, arg, LoginActivity.this).forceLoad();
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
                startLoginByEmailActivity(false, true);
                break;
            case R.id.login_sign_up_layout:
            case R.id.login_sign_up:
                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.RegisterEmail);
                startLoginByEmailActivity(true, false);
                break;
            case R.id.start_login_button:
                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.LoginByStart);
                boolean isFacebook = NASPref.getFBAccountStatus(mContext);
                if (isFacebook) {
                    loginFBAccount();
                } else {
                    Bundle arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", NASPref.getCloudUsername(mContext));
                    arg.putString("password", NASPref.getCloudPassword(mContext));
                    getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, LoginActivity.this).forceLoad();
                }
                break;
            case R.id.start_sign_in_with_other:
                Bundle value = new Bundle();
                value.putString(NotificationDialog.DIALOG_MESSAGE, getString(R.string.remote_access_logout));
                NotificationDialog mNotificationDialog = new NotificationDialog(this, value) {
                    @Override
                    public void onConfirm() {
                        GoogleAnalysisFactory.getInstance(mContext).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.Logout);
                        if(NASPref.useFacebookLogin && NASPref.getFBAccountStatus(mContext))
                            NASPref.logOutFB(LoginActivity.this);
                        NASPref.clearDataAfterLogout(mContext);
                        showLoginView();
                    }

                    @Override
                    public void onCancel() {

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
        Log.d(TAG, "displayMetrics.heightPixels: "+ displayMetrics.heightPixels);
        Log.d(TAG, "displayMetrics.widthPixels: "+ displayMetrics.widthPixels);
        Log.d(TAG, "height: "+ height);
        Log.d(TAG, "width: "+ width);
        Bitmap bitmap = StyleFactory.decodeSampledBitmapFromResource(this.getResources(), R.drawable.sjc_bg3_logo, width, height);
        backgroundImage.setImageBitmap(bitmap);
    }

}
