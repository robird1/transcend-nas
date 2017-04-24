package com.transcend.nas.connection;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.LoaderID;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.StyleFactory;
import com.transcend.nas.introduce.InviteLicenseActivity;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.tutk.TutkCodeID;
import com.transcend.nas.tutk.TutkCreateNasLoader;
import com.transcend.nas.tutk.TutkFBLoginLoader;
import com.transcend.nas.tutk.TutkGetNasLoader;
import com.transcend.nas.tutk.TutkLinkNasLoader;

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
import static com.transcend.nas.connection.LoginActivity.FB_PROFILE_PHOTO;

/**
 * Created by steve_su on 2017/3/3.
 */

public abstract class AbstractInviteActivity extends AppCompatActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Boolean> {
    private static final String TAG = AbstractInviteActivity.class.getSimpleName();
    private static final int REQUEST_CODE_LICENSE = 888;

    private RelativeLayout mProgressView;
    private String mUUID;
    private String mNasID;
    private String mNickName;
    private String mUserName;
    private String mPassword;
    private Context mContext;
    private CallbackManager mCallbackManager;
    private AccessToken mAccessToken;

    private Button mFBLoginButton;
    private Bitmap mPhotoBitmap;
    private ImageView mAccountImage;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FB_PROFILE_PHOTO:
                    mAccountImage.setImageBitmap(mPhotoBitmap);
                    break;
            }
        }
    };

    abstract protected void receiveInviteDeepLink();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_login_by_invite);
        setBackgroundImage();
        receiveInviteDeepLink();

        // TODO replace license button with introduce button
        initView();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            int fbRequestCode = CallbackManagerImpl.RequestCodeOffset.Login.toRequestCode();

            if (requestCode == REQUEST_CODE_LICENSE) {
//                mFBLoginButton.setEnabled(true);

                // TODO replace license button with introduce button

            } else if (requestCode == fbRequestCode) {
                if (mCallbackManager != null && FacebookSdk.isInitialized() == true) {
                    Log.d(TAG, "callbackManager.onActivityResult()");
                    mCallbackManager.onActivityResult(requestCode, resultCode, data);
                    return;
                }
            }

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_login_invite_button:
//                GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.Click, GoogleAnalysisFactory.LABEL.LoginByFacebook);

//                if (NASPref.getIsLicenseAgreed(this)) {
                    loginFBAccount();
//                } else {
//                    Toast.makeText(this, "Please confirm license agreement first", Toast.LENGTH_SHORT).show();
//                }
                break;
            case R.id.view_license:
                startActivityForResult(new Intent(this, InviteLicenseActivity.class), REQUEST_CODE_LICENSE);
                break;
            default:
                break;

        }
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LoaderID.TUTK_FB_LOGIN:
                return new TutkFBLoginLoader(this, args.getString("server"), args.getString("email"), args.getString("name"), args.getString("uid"), args.getString("token"));
            case LoaderID.TUTK_NAS_LINK:
                return new TutkLinkNasLoader(this, args);
            case LoaderID.WIZARD:
                return new WizardCheckLoader(this, args);
            case LoaderID.LOGIN:
                return new LoginLoader(this, args, true);
            case LoaderID.TUTK_NAS_CREATE:
                return new TutkCreateNasLoader(this, args);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean isSuccess) {
        if (!isSuccess) {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (loader instanceof TutkFBLoginLoader) {
            checkFBLoginNASResult((TutkFBLoginLoader) loader);
        } else if (loader instanceof TutkLinkNasLoader) {
            checkTutkLinkNasLoader(isSuccess, (TutkLinkNasLoader) loader);
        } else if (loader instanceof WizardCheckLoader) {
            checkWizardLoader(isSuccess, (WizardCheckLoader) loader);
        } else if (loader instanceof LoginLoader) {
            checkLoginLoader(isSuccess, (LoginLoader) loader);
        } else if (loader instanceof TutkCreateNasLoader) {
            checkCreateNASResult(isSuccess, (TutkCreateNasLoader) loader);
        }

    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

//    protected void extractInviteData(String url) {
//        Log.d(TAG, "[Enter] extractInviteData");
//        Log.d(TAG, "[Enter] url: "+ url);
//
//        String[] temp = url.split("uuid=")[1].split("&nasId=");
//        mUUID = temp[0];
//        String[] temp2 = temp[1].split("&nickName=");
//        mNasID = temp2[0];
//        mNickName = temp2[1];
//
//        Log.d(TAG, "uuid: "+ mUUID);
//        Log.d(TAG, "nasId: "+ mNasID);
//        Log.d(TAG, "nickName: "+ mNickName);
//    }
protected void extractInviteData(String url) {
    Log.d(TAG, "[Enter] extractInviteData");
    Log.d(TAG, "[Enter] url: "+ url);

    String[] temp = url.split("uuid=")[1].split("&nasId=");
    mUUID = temp[0];
    String[] temp2 = temp[1].split("&nickName=");
    mNasID = temp2[0];
    String[] temp3 = temp2[1].split("&username=");
    mNickName = temp3[0];
    String[] temp4 = temp3[1].split("&password=");
    mUserName = temp4[0];
    mPassword = temp4[1];

    Log.d(TAG, "uuid: "+ mUUID);
    Log.d(TAG, "nasId: "+ mNasID);
    Log.d(TAG, "nickName: "+ mNickName);
    Log.d(TAG, "mUserName: "+ mUserName);
    Log.d(TAG, "mPassword: "+ mPassword);

}


    private void initView() {
        mProgressView = (RelativeLayout) findViewById(R.id.login_progress_view);

        mAccountImage = (ImageView) findViewById(R.id.start_account_image);

        // TODO fix bug: User photo and email can not obtain if user never logins before (install after receiving invite).
        setFBPhoto();
        setFBAccountTitle();

        mFBLoginButton = (Button) findViewById(R.id.start_login_invite_button);
        mFBLoginButton.setOnClickListener(this);
//        mFBLoginButton.setEnabled(false);

        Button licenseBtn = (Button) findViewById(R.id.view_license);
        licenseBtn.setOnClickListener(this);
    }

    private void setFBAccountTitle() {
        TextView accountTitle = (TextView) findViewById(R.id.start_account_title);
        String email = NASPref.getCloudUsername(mContext);
        String pwd = NASPref.getCloudPassword(mContext);
        if (!email.equals("") && !pwd.equals("")) {
            if (email.contains("@")) {
                accountTitle.setText(String.format("%s\n%s", email.split("@")[0], email));
            } else {   // if user login by FB with no email information
                accountTitle.setText(String.format("%s", NASPref.getFBUserName(this)));
            }
        }
    }

    private void checkFBLoginNASResult(TutkFBLoginLoader loader) {
        Log.d(TAG, "[Enter] checkFBLoginNASResult");

//        mProgressView.setVisibility(View.INVISIBLE);

        String token = loader.getAuthToke();

        if (!token.equals("")) {
            //token not null mean login success
//            GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.LoginTutk,
//                    GoogleAnalysisFactory.LABEL.LoginByFacebook + "_" + GoogleAnalysisFactory.SUCCESS);
            NASPref.setFBAccountStatus(mContext, true);
            NASPref.setCloudUsername(mContext, loader.getEmail());
            NASPref.setCloudPassword(mContext, loader.getPassword());
            NASPref.setCloudAuthToken(mContext, token);
//            Bundle arg = new Bundle();
//            arg.putString("server", loader.getServer());
//            arg.putString("token", token);
//            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, this).forceLoad();

//            startLoginListActivity();
            startP2PService();

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
//            GoogleAnalysisFactory.getInstance(this).sendEvent(GoogleAnalysisFactory.VIEW.START, GoogleAnalysisFactory.ACTION.LoginTutk,
//                    GoogleAnalysisFactory.LABEL.LoginByFacebook + "_" + status);
        }
    }

    private void startP2PService() {
        storeCurrentNASInfo(mNickName, mNasID);

        Bundle args = new Bundle();
        args.putString("nasId", mNasID);
        args.putString("nickname", mNickName);
        args.putString("hostname", mUUID);
        args.putString("username", mUserName);
        args.putString("password", mPassword);
        args.putBoolean("RemoteAccess", true);
        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_LINK, args, this).forceLoad();
    }

    private void storeCurrentNASInfo(String nasName, String nasID) {
        NASPref.setCloudNickName(this, nasName);
        NASPref.setCloudNasID(this, nasID);
    }

    private void checkTutkLinkNasLoader(boolean success, TutkLinkNasLoader loader) {
        Log.d(TAG, "[Enter] checkTutkLinkNasLoader");
        if (!success) {
            //get account info from db
            LoginHelper loginHelper = new LoginHelper(this);
            LoginHelper.LoginInfo account = new LoginHelper.LoginInfo();
            account.email = NASPref.getCloudUsername(this);
            account.uuid = loader.getNasUUID();
            boolean exist = loginHelper.getAccount(account);
            loginHelper.onDestroy();

            //get network status
            boolean isWiFi = false;
            ConnectivityManager mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = mConnMgr.getActiveNetworkInfo();
            if (info != null)
                isWiFi = (info.getType() == ConnectivityManager.TYPE_WIFI);

            if (exist && isWiFi) {
                Log.d(TAG, "[Enter] exist && isWiFi");
                //Link nas's tutk server fail, try lan connect
                Bundle args = loader.getBundleArgs();
                args.putString("hostname", account.ip);
                args.putBoolean("RemoteAccess", false);
                getLoaderManager().restartLoader(LoaderID.WIZARD, args, this).forceLoad();
            } else {
                Log.d(TAG, "[Enter] ! (exist && isWiFi)");

//                mProgressView.setVisibility(View.INVISIBLE);
//                if (isWiFi)
//                    startNASListLoader(false);
//                else {
//                    String error = loader.getError();
//                    if (enableDeviceCheck) {
//                        for (HashMap<String, String> nas : mNASList) {
//                            String UID = nas.get("hostname");
//                            if (UID != null && UID.equals(account.uuid)) {
//                                error = "no".equals(nas.get("online")) ? getString(R.string.offline) : error;
//                                break;
//                            }
//                        }
//                    }
//
//                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
//                }
            }
        } else {

            Bundle args = loader.getBundleArgs();
            args.putString("hostname", loader.getP2PHostname());
            getLoaderManager().restartLoader(LoaderID.WIZARD, args, this).forceLoad();
//            getLoaderManager().restartLoader(LoaderID.LOGIN, args, this).forceLoad();







//            String hostname = P2PService.getInstance().getP2PIP() + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
//            String userName = loader.getBundleArgs().getString("username");
//            String password = loader.getBundleArgs().getString("password");
//            Log.d(TAG, "hostname: "+ hostname);
//            Log.d(TAG, "userName: "+ userName);
//            Log.d(TAG, "password: "+ password);
//
//            final Server server = new Server(hostname, userName, password);
//
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    server.connect(true);
//                }
//            }).start();
//
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            ServerManager.INSTANCE.saveServer(server);
//            ServerManager.INSTANCE.setCurrentServer(server);
//
//            Intent intent = new Intent();
//            intent.setClass(this, FileManageActivity.class);
//            intent.putExtra("is_invite", true);
//            startActivity(intent);
//            finish();
        }
    }

    private void checkWizardLoader(boolean success, WizardCheckLoader loader) {
        Log.d(TAG, "[Enter] checkWizardLoader");

        if (!success) {
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = loader.getBundleArgs();
        boolean remoteAccess = args.getBoolean("RemoteAccess", false);
        if (loader.isWizard()) {
            Log.d(TAG, "[Enter] loader.isWizard()");
            Log.d(TAG, "hostname: "+ args.getString("hostname"));
            Log.d(TAG, "username: "+ args.getString("username"));
            Log.d(TAG, "password: "+ args.getString("password"));
            startLoginLoader(args);
        }
//        else {
//            showWizardDialog(args);
//        }
    }

    private void startLoginLoader(Bundle args) {
        getLoaderManager().restartLoader(LoaderID.LOGIN, args, this).forceLoad();
    }

    private void checkLoginLoader(boolean success, LoginLoader loader) {
        Log.d(TAG, "[Enter] checkLoginLoader");
        Bundle args = loader.getBundleArgs();
//        if (!success) {
//            boolean wizard = args.getBoolean("wizard", false);
//            hideDialog(wizard);
//            Toast.makeText(this, loader.getLoginError(), Toast.LENGTH_SHORT).show();
//            return;
//        }

        startTutkCreateNasLoader(args);
//        String hostname = P2PService.getInstance().getP2PIP() + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
//        String userName = loader.getBundleArgs().getString("username");
//        String password = loader.getBundleArgs().getString("password");
//        Log.d(TAG, "hostname: "+ hostname);
//        Log.d(TAG, "userName: "+ userName);
//        Log.d(TAG, "password: "+ password);
//
////        Server server = new Server(hostname, userName, password);
////        ServerManager.INSTANCE.saveServer(server);
////        ServerManager.INSTANCE.setCurrentServer(server);
//
//        Intent intent = new Intent();
//        intent.setClass(this, FileManageActivity.class);
//        intent.putExtra("is_invite", true);
//        startActivity(intent);
//        finish();

    }

    private void startTutkCreateNasLoader(Bundle args) {
        Log.d(TAG, "[Enter] startTutkCreateNasLoader");
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String nasName = server.getServerInfo().hostName;
        String uuid = server.getTutkUUID();
        String serialNum = NASPref.getSerialNum(this);
        if (serialNum != null && !serialNum.equals(""))
            nasName = nasName + NASApp.TUTK_NAME_TAG + serialNum;

//        boolean wizard = args.getBoolean("wizard", false);
//        if (!wizard) {
//            for (HashMap<String, String> nas : mNASList) {
//                String hostname = nas.get("hostname");
//                if (hostname.equals(uuid)) {
//                    hideDialog(true);
//                    startFileManageActivity();
//                    return;
//                }
//            }
//        }

        if (uuid != null && !uuid.equals("")) {
            args.putString("server", NASPref.getCloudServer(this));
            args.putString("token", NASPref.getCloudAuthToken(this));
            args.putString("nasName", nasName);
            args.putString("nasUUID", uuid);
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_CREATE, args, this).forceLoad();
        }
//        else {
//            hideDialog(wizard);
//            Toast.makeText(LoginListActivity.this, getString(R.string.error), Toast.LENGTH_SHORT).show();
//        }
    }

    private void checkCreateNASResult(boolean success, TutkCreateNasLoader loader) {
        Log.d(TAG, "[Enter] checkCreateNASResult");
        boolean wizard = loader.getBundleArgs().getBoolean("wizard");
        if (!success) {
//            hideDialog(wizard);
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        String status = loader.getStatus();
        String code = loader.getCode();
        if (code.equals("") || code.equals(TutkCodeID.SUCCESS) || code.equals(TutkCodeID.UID_ALREADY_TAKEN)) {
//            if (wizard && mWizardDialog != null) {
//                //hide keyboard
//                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
//                mWizardDialog.showFinishView();
//            } else {
//                hideDialog(true);

                storeCurrentNASInfo(loader.getNasName(), loader.getNasID());

                startFileManageActivity();
//            }
        } else {
            Log.d(TAG, "[Enter] else section");
//            hideDialog(wizard);
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
    }


    private void startFileManageActivity() {
//        if (getCallingActivity() == null) {
            Intent intent = new Intent();
            intent.setClass(this, FileManageActivity.class);
            startActivity(intent);
            finish();
//        } else {
//            Intent intent = new Intent();
//            setResult(RESULT_OK, intent);
//            finish();
//        }
    }


    private void startLoginListActivity() {
        Log.d(TAG, "[Enter] startLoginListActivity");

        TutkGetNasLoader.TutkNasNode node = new TutkGetNasLoader.TutkNasNode();
        node.nasID = mNasID;
        node.nasUUID = mUUID;
        node.nasName = mNickName;
        HashMap<String, String> nas = new HashMap<String, String>();
        nas.put("nasId", node.nasID);
        nas.put("nickname", node.nasName);
        nas.put("hostname", node.nasUUID);

        ArrayList<HashMap<String, String>> nasList = new ArrayList();
        nasList.add(nas);

        Intent intent = new Intent();
        intent.setClass(this, LoginListActivity.class);
        intent.putExtra("NASList", nasList);
        intent.putExtra("RemoteAccess", true);
        intent.putExtra("start_by_invite", true);
        intent.putExtra("is_invite", true);
        intent.putExtra("username", mUserName);
        intent.putExtra("password", mPassword);
        startActivity(intent);
//        finish();
    }

    // TODO duplicate code in LoginActivity
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

    private void loginFBAccount() {
        mProgressView.setVisibility(View.VISIBLE);
        Log.d(TAG, "[Enter] loginFBAccount()");

        if (mCallbackManager == null) {
            registerFBLoginCallback();
        }

        mAccessToken = getCurrentAccessToken();

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

                    Toast.makeText(mContext, R.string.fb_request_email_permission, Toast.LENGTH_LONG).show();

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
//                            Log.d(TAG, "name: " + object.optString("name"));
//                            Log.d(TAG, "email: " + object.optString("email"));
//                            Log.d(TAG, "user id: " + mAccessToken.getUserId());
//                            Log.d(TAG, "token: " + mAccessToken.getToken());
//                            Log.d(TAG, "server: "+ NASPref.getCloudServer(mContext));

//                                String url = object.getJSONObject("picture").getJSONObject("data").getString("url");
                            String url = "https://graph.facebook.com/" + mAccessToken.getUserId() + "/picture?type=large";
                            NASPref.setFBProfilePhotoUrl(mContext, url);

                            Bundle arg = new Bundle();
                            arg.putString("server", NASPref.getCloudServer(mContext));
                            arg.putString("name", object.optString("name"));
                            arg.putString("email", object.optString("email"));
                            arg.putString("uid", mAccessToken.getUserId());
                            arg.putString("token", mAccessToken.getToken());
                            getLoaderManager().restartLoader(LoaderID.TUTK_FB_LOGIN, arg, AbstractInviteActivity.this).forceLoad();
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

    private void setFBPhoto() {
        final String storedUrl = NASPref.getFBProfilePhotoUrl(this);
        if (storedUrl != null) {
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


}
