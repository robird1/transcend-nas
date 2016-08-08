package com.transcend.nas.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.NotificationDialog;
import com.transcend.nas.common.TutkCodeID;
import com.transcend.nas.connection.ForgetPwdDialog;
import com.transcend.nas.management.TutkCreateNasLoader;
import com.transcend.nas.management.TutkForgetPasswordLoader;
import com.transcend.nas.management.TutkGetNasLoader;
import com.transcend.nas.management.TutkLoginLoader;
import com.transcend.nas.management.TutkRegisterLoader;
import com.transcend.nas.management.TutkResendActivateLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ikelee on 16/8/4.
 */
public class RemoteAccessActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Boolean>,
        View.OnClickListener {

    public static final int REQUEST_CODE = RemoteAccessActivity.class.hashCode() & 0xFFFF;
    public static final String TAG = RemoteAccessActivity.class.getSimpleName();

    private TextView mTitle;
    private RelativeLayout mProgressView;
    LinearLayout initLayout;
    LinearLayout loginLayout;
    LinearLayout registerLayout;
    RelativeLayout normalLayout;

    TextInputLayout tlEmail;
    TextInputLayout tlPwd;
    TextInputLayout tlPwdConfirm;

    private int mLoaderID;
    private boolean isInitFragment = true;
    private boolean isRemoteAccessRegister = false;
    private boolean isRemoteAccessActive = false;
    private List<TutkGetNasLoader.TutkNasNode> naslist;
    private ForgetPwdDialog mForgetDialog;

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_remote_access);
        initToolbar();
        initProgressView();
        initView();
        String email = NASPref.getCloudUsername(mContext);
        String pwd = NASPref.getCloudPassword(mContext);
        if (!email.equals("") && !pwd.equals("")) {
            Bundle arg = new Bundle();
            arg.putString("server", NASPref.getCloudServer(mContext));
            arg.putString("email", email);
            arg.putString("password", pwd);
            getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, RemoteAccessActivity.this).forceLoad();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.nas_finder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!isInitFragment) {
                    showInitView();
                } else
                    finish();
                break;
            case R.id.action_refresh_nas_finder:
                String email = NASPref.getCloudUsername(mContext);
                String pwd = NASPref.getCloudPassword(mContext);
                if (isRemoteAccessRegister && !email.equals("") && !pwd.equals("")) {
                    isRemoteAccessRegister = true;
                    isRemoteAccessActive = false;
                    Bundle arg = new Bundle();
                    arg.putString("server", NASPref.getCloudServer(mContext));
                    arg.putString("email", email);
                    arg.putString("password", pwd);
                    getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, RemoteAccessActivity.this).forceLoad();
                } else {
                    Toast.makeText(this, getString(R.string.remote_access_no_login), Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mForgetDialog != null) {
            getLoaderManager().destroyLoader(LoaderID.TUTK_FORGET_PASSWORD);
            mForgetDialog.dismiss();
            mForgetDialog = null;
        }

        if (mProgressView.isShown()) {
            getLoaderManager().destroyLoader(mLoaderID);
            mProgressView.setVisibility(View.INVISIBLE);
        } else {
            if (!isInitFragment) {
                showInitView();
            } else
                super.onBackPressed();
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
            String[] scenarios = getResources().getStringArray(R.array.backup_scenario_values);
            NASPref.setBackupScenario(this, scenarios[1]);
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
            isRemoteAccessActive = true;
            NASPref.setCloudUsername(mContext, email);
            NASPref.setCloudPassword(mContext, pwd);
            NASPref.setCloudAuthToken(mContext, loader.getAuthToke());
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", loader.getAuthToke());
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, RemoteAccessActivity.this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            if (code.equals(TutkCodeID.NOT_VERIFIED)) {
                //account not verified
                isRemoteAccessRegister = true;
                isRemoteAccessActive = false;
                NASPref.setCloudAccountStatus(mContext, NASPref.Status.Padding.ordinal());
                NASPref.setCloudUsername(mContext, email);
                NASPref.setCloudPassword(mContext, pwd);
            } else {
                isRemoteAccessRegister = false;
                isRemoteAccessActive = false;
                if (!code.equals(""))
                    Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, getString(R.string.error_format), Toast.LENGTH_SHORT).show();
            }

            updateView();
        }
    }

    private void checkCreateNASResult(TutkCreateNasLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();

        if (code.equals("") || code.equals(TutkCodeID.SUCCESS) || code.equals(TutkCodeID.UID_ALREADY_TAKEN)) {
            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", loader.getAuthToken());
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, RemoteAccessActivity.this).forceLoad();
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkGetNASResult(TutkGetNasLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();
        NASPref.setCloudAccountStatus(mContext, NASPref.Status.Active.ordinal());

        if (code.equals("")) {
            naslist = loader.getNasList();
            //check nas uuid and record it
            Server mServer = ServerManager.INSTANCE.getCurrentServer();
            String uuid = mServer.getTutkUUID();
            if (uuid == null) {
                uuid = NASPref.getUUID(mContext);
            }

            for (TutkGetNasLoader.TutkNasNode nas : naslist) {
                if (nas.nasUUID.equals(uuid)) {
                    NASPref.setCloudUUID(mContext, uuid);
                    NASPref.setCloudAccountStatus(mContext, NASPref.Status.Bind.ordinal());
                    break;
                }
            }

            updateView();
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void checkResendActivateResult(TutkResendActivateLoader loader) {
        String status = loader.getStatus();
        String code = loader.getCode();
        if (code.equals(TutkCodeID.SUCCESS))
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        else if (code.equals(TutkCodeID.VERIFICATIONEXPIRED)) {
            isRemoteAccessRegister = false;
            isRemoteAccessActive = false;
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
                    String[] scenarios = mContext.getResources().getStringArray(R.array.backup_scenario_values);
                    NASPref.setBackupScenario(mContext, scenarios[1]);
                    isRemoteAccessRegister = false;
                    isRemoteAccessActive = false;
                    updateView();
                } else
                    getLoaderManager().restartLoader(loaderID, args, RemoteAccessActivity.this).forceLoad();
            }

            @Override
            public void onCancel() {

            }
        };
    }


    /**
     * INITIALIZATION
     */
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
        initLayout = (LinearLayout) findViewById(R.id.remote_access_init_layout);
        registerLayout = (LinearLayout) findViewById(R.id.remote_access_register_layout);
        loginLayout = (LinearLayout) findViewById(R.id.remote_access_login_layout);
        normalLayout = (RelativeLayout) findViewById(R.id.remote_access_normal_layout);
        int status = NASPref.getCloudAccountStatus(this);
        if(status == NASPref.Status.Padding.ordinal()){
            isRemoteAccessRegister = true;
            isRemoteAccessActive = false;
        }
        else if(status == NASPref.Status.Active.ordinal() || status == NASPref.Status.Bind.ordinal()){
            isRemoteAccessRegister = true;
            isRemoteAccessActive = true;
        }
        else{
            isRemoteAccessRegister = false;
            isRemoteAccessActive = false;
        }
        updateView();
    }

    private void showInitView(){
        initLayout.setVisibility(View.VISIBLE);
        registerLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.GONE);
        normalLayout.setVisibility(View.GONE);
        mTitle.setText(getString(R.string.remote_access));
        Button btLogin = (Button) findViewById(R.id.remote_access_init_login);
        btLogin.setOnClickListener(this);
        Button btRegister = (Button) findViewById(R.id.remote_access_init_register);
        btRegister.setOnClickListener(this);
        isInitFragment = true;
    }

    private void showLoginView(){
        initLayout.setVisibility(View.GONE);
        registerLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.VISIBLE);
        normalLayout.setVisibility(View.GONE);
        mTitle.setText(getString(R.string.login));
        tlEmail = (TextInputLayout) findViewById(R.id.remote_access_login_email);
        tlEmail.getEditText().setText(NASPref.getCloudUsername(mContext));
        tlPwd = (TextInputLayout) findViewById(R.id.remote_access_login_password);
        tlPwd.getEditText().setText("");
        Button btLogin = (Button) findViewById(R.id.remote_access_login_login);
        btLogin.setOnClickListener(this);
        TextView tvForget = (TextView) findViewById(R.id.remote_access_login_forget);
        tvForget.setOnClickListener(this);
        isInitFragment = false;
    }

    private void showRegisterView(){
        initLayout.setVisibility(View.GONE);
        registerLayout.setVisibility(View.VISIBLE);
        loginLayout.setVisibility(View.GONE);
        normalLayout.setVisibility(View.GONE);
        mTitle.setText(getString(R.string.register));
        tlEmail = (TextInputLayout) findViewById(R.id.remote_access_register_email);
        tlEmail.getEditText().setText(NASPref.getCloudUsername(mContext));
        tlPwd = (TextInputLayout) findViewById(R.id.remote_access_register_password);
        tlPwd.getEditText().setText("");
        tlPwdConfirm = (TextInputLayout) findViewById(R.id.remote_access_register_password_confirm);
        tlPwdConfirm.getEditText().setText("");
        Button btSubmit = (Button) findViewById(R.id.remote_access_register_submit);
        btSubmit.setOnClickListener(this);
        isInitFragment = false;
    }

    private void showNormalView(){
        initLayout.setVisibility(View.GONE);
        registerLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.GONE);
        normalLayout.setVisibility(View.VISIBLE);
        mTitle.setText(getString(R.string.remote_access));

        TextView tvEmail = (TextView) findViewById(R.id.remote_access_email);
        tvEmail.setText(NASPref.getCloudUsername(mContext));
        Button btResend = (Button) findViewById(R.id.remote_access_resend_button);
        btResend.setOnClickListener(this);
        Button btBind = (Button) findViewById(R.id.remote_access_bind_button);
        btBind.setOnClickListener(this);
        Button btDelete = (Button) findViewById(R.id.remote_access_delete_button);
        btDelete.setOnClickListener(this);
        ListView lvList = (ListView) findViewById(R.id.remote_access_list);
        TextView tvStatus = (TextView) findViewById(R.id.remote_access_status);

        if (isRemoteAccessActive) {
            btResend.setVisibility(View.GONE);

            int status = NASPref.getCloudAccountStatus(mContext);
            boolean isBind = status == NASPref.Status.Bind.ordinal();
            if(isBind) {
                tvStatus.setText(getString(R.string.remote_access_success_info));
                btBind.setVisibility(View.GONE);
            }
            else {
                tvStatus.setText(getString(R.string.remote_access_bind_info));
                btBind.setVisibility(View.VISIBLE);
            }

            if (naslist != null) {
                Server mServer = ServerManager.INSTANCE.getCurrentServer();
                String uuid = mServer.getTutkUUID();
                if(uuid == null)
                    uuid = NASPref.getUUID(this);
                String serialNum = NASPref.getSerialNum(this);
                Log.d(TAG, "Current user: " + mServer.getUsername());
                Log.d(TAG, "Current UUID: " + uuid);
                Log.d(TAG, "Current SerialNum: " + serialNum);
                String ID_TITLE = "TITLE", ID_SUBTITLE = "SUBTITLE";
                ArrayList<HashMap<String, String>> myListData = new ArrayList<HashMap<String, String>>();

                for (TutkGetNasLoader.TutkNasNode node : naslist) {
                    HashMap<String, String> item = new HashMap<String, String>();
                    item.put(ID_TITLE, node.nasName);
                    item.put(ID_SUBTITLE, node.nasUUID);
                    if (node.nasUUID.equals(uuid))
                        myListData.add(0, item);
                    else
                        myListData.add(item);
                }

                lvList.setAdapter(new SimpleAdapter(mContext,
                                myListData,
                                android.R.layout.simple_list_item_2,
                                new String[]{ID_TITLE, ID_SUBTITLE},
                                new int[]{android.R.id.text1, android.R.id.text2})
                );
            }
        } else {
            tvStatus.setText(getString(R.string.remote_access_send_activate_info));
            btResend.setVisibility(View.VISIBLE);
            btBind.setVisibility(View.GONE);
        }

        isInitFragment = true;
    }

    private void updateView() {
        if (isRemoteAccessRegister) {
            showNormalView();
        } else {
            showInitView();
        }
    }

    @Override
    public void onClick(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        String email, pwd, pwdConfirm;
        Bundle arg;

        switch (v.getId()) {
            case R.id.remote_access_init_login:
                showLoginView();
                break;
            case R.id.remote_access_init_register:
                showRegisterView();
                break;
            case R.id.remote_access_resend_button:
                arg = new Bundle();
                arg.putString("server", NASPref.getCloudServer(mContext));
                arg.putString("email", NASPref.getCloudUsername(mContext));
                showNotificationDialog(getString(R.string.remote_access_send_activate_warning), LoaderID.TUTK_NAS_RESEND_ACTIVATE, arg);
                break;
            case R.id.remote_access_bind_button:
                arg = new Bundle();
                arg.putString("server", NASPref.getCloudServer(mContext));
                arg.putString("token", NASPref.getCloudAuthToken(mContext));
                Server server = ServerManager.INSTANCE.getCurrentServer();
                String nasName = server.getServerInfo().hostName;
                String serialNum = NASPref.getSerialNum(this);
                if (serialNum != null && !serialNum.equals(""))
                    nasName = nasName + NASApp.TUTK_NAME_TAG + serialNum;
                arg.putString("nasName", nasName);
                String uuid = server.getTutkUUID();
                if (uuid == null)
                    uuid = NASPref.getUUID(this);
                if (uuid != null && !uuid.equals("")) {
                    arg.putString("nasUUID", uuid);
                    getLoaderManager().restartLoader(LoaderID.TUTK_NAS_CREATE, arg, RemoteAccessActivity.this).forceLoad();
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

                arg = new Bundle();
                arg.putString("server", NASPref.getCloudServer(mContext));
                arg.putString("email", email);
                arg.putString("password", pwd);
                getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, arg, RemoteAccessActivity.this).forceLoad();
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
                    Toast.makeText(mContext, getString(R.string.password_size) + "", Toast.LENGTH_SHORT).show();
                    return;
                }

                //check password and confirm password
                if (!pwd.equals(pwdConfirm)) {
                    Toast.makeText(mContext, getString(R.string.confirm_password_error), Toast.LENGTH_SHORT).show();
                    return;
                }

                arg = new Bundle();
                arg.putString("server", NASPref.getCloudServer(mContext));
                arg.putString("email", email);
                arg.putString("password", pwd);
                Server mServer = ServerManager.INSTANCE.getCurrentServer();
                String name = mServer.getServerInfo().hostName;
                String serial = NASPref.getSerialNum(this);
                if (serial != null && !serial.equals(""))
                    name = name + NASApp.TUTK_NAME_TAG + serial;
                arg.putString("nasName", name);
                String id = mServer.getTutkUUID();
                if (id == null)
                    id = NASPref.getUUID(this);
                arg.putString("nasUUID", id);
                getLoaderManager().restartLoader(LoaderID.TUTK_REGISTER, arg, RemoteAccessActivity.this).forceLoad();
                break;
            case R.id.remote_access_login_forget:
                arg = new Bundle();
                arg.putString("title", getString(R.string.forget_password_title));
                arg.putString("email", NASPref.getCloudUsername(mContext));
                mForgetDialog = new ForgetPwdDialog(mContext, arg) {
                    @Override
                    public void onConfirm(Bundle args) {
                        args.putString("server", NASPref.getCloudServer(mContext));
                        getLoaderManager().restartLoader(LoaderID.TUTK_FORGET_PASSWORD, args, RemoteAccessActivity.this).forceLoad();
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
