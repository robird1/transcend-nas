package com.transcend.nas.connection;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.AnalysisFactory;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.TutkCodeID;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.TutkForgetPasswordLoader;
import com.transcend.nas.management.TutkGetNasLoader;
import com.transcend.nas.management.TutkLinkNasLoader;
import com.transcend.nas.management.TutkLoginLoader;
import com.transcend.nas.management.TutkLogoutLoader;
import com.transcend.nas.common.StyleFactory;
import com.tutk.IOTC.P2PService;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by silverhsu on 16/2/2.
 */
public class StartActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean>, View.OnClickListener {

    private static final String TAG = StartActivity.class.getSimpleName();
    public static final int REQUEST_CODE = StartActivity.class.hashCode() & 0xFFFF;

    private LinearLayout layoutInit;
    private LinearLayout layoutSignIn;
    private TextInputLayout tlEmail;
    private TextInputLayout tlPwd;
    private Button bnRemote;
    private Button bnSignIn;
    private Button bnFind;
    private TextView tvSignInForget;
    private ForgetPwdDialog mForgetDialog;
    private RelativeLayout mProgressView;
    private int mLoaderID;
    private ArrayList<HashMap<String, String>> mNASList = new ArrayList<HashMap<String, String>>();
    private boolean isInitial = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        AnalysisFactory.getInstance(this).sendScreen(AnalysisFactory.VIEW.START);
        initLayout();
        initInputText();
        initRemoteButton();
        initSignInButton();
        initSignInForgetButton();
        initFindButton();
        initProgressView();

        Intent intent = getIntent();
        if(intent != null) {
            isInitial = (boolean) intent.getBooleanExtra("Initial", false);
            changeView(!isInitial);
        }
    }

    private void initLayout(){
        layoutInit = (LinearLayout) findViewById(R.id.activity_init_layout);
        layoutSignIn = (LinearLayout) findViewById(R.id.activity_sign_in_layout);
    }

    private void initInputText() {
        tlEmail = (TextInputLayout) findViewById(R.id.activity_sign_in_email);
        tlEmail.getEditText().setText(NASPref.getCloudUsername(this));
        tlPwd = (TextInputLayout) findViewById(R.id.activity_sign_in_password);
        tlPwd.getEditText().setText(NASPref.getCloudPassword(this));
    }

    private void initRemoteButton() {
        bnRemote = (Button) findViewById(R.id.activity_remote_access_button);
        bnRemote.setOnClickListener(this);
    }

    private void initSignInButton() {
        bnSignIn = (Button) findViewById(R.id.activity_sign_in_button);
        bnSignIn.setOnClickListener(this);
    }

    private void initSignInForgetButton() {
        tvSignInForget = (TextView) findViewById(R.id.activity_sing_in_forget);
        tvSignInForget.setOnClickListener(this);
    }

    private void initFindButton() {
        bnFind = (Button) findViewById(R.id.activity_find_nas_button);
        bnFind.setOnClickListener(this);
        StyleFactory.set_button_Drawable_left(this, bnFind, R.drawable.ic_search_white_24dp, 50);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.activity_sign_in_progress_view);
    }

    @Override
    public void onClick(View v) {
        if(v.equals(bnRemote)){
            AnalysisFactory.getInstance(this).sendClickEvent(AnalysisFactory.VIEW.START, AnalysisFactory.ACTION.STARTREMOTE);
            changeView(false);
        }
        else if (v.equals(bnSignIn)) {
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

            //hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            Bundle args = new Bundle();
            args.putString("server", NASPref.getCloudServer(this));
            args.putString("email", email);
            args.putString("password", pwd);
            getLoaderManager().restartLoader(LoaderID.TUTK_LOGIN, args, this).forceLoad();
        } else if (v.equals(bnFind)) {
            AnalysisFactory.getInstance(this).sendClickEvent(AnalysisFactory.VIEW.START, AnalysisFactory.ACTION.STARTLOCAL);
            startNASFinderActivity(null, false);
        } else if (v.equals(tvSignInForget)) {
            showForgetPwdDialog();
        }
    }

    private void startFileManageActivity() {
        Intent intent = new Intent();
        intent.setClass(StartActivity.this, FileManageActivity.class);
        startActivity(intent);
        finish();
    }

    private void startInitialActivity() {
        Intent intent = new Intent();
        intent.setClass(StartActivity.this, GuideActivity.class);
        intent.putExtra("Retry", true);
        startActivity(intent);
        finish();
    }

    private void startNASFinderActivity(ArrayList<HashMap<String, String>> list, boolean isRemoteAccess) {
        Intent intent = new Intent();
        intent.setClass(StartActivity.this, NASListActivity.class);
        if (isRemoteAccess) {
            intent.putExtra("NASList", list);
            intent.putExtra("RemoteAccess", isRemoteAccess);
        }
        startActivityForResult(intent, NASListActivity.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == NASListActivity.REQUEST_CODE){
                startFileManageActivity();
            }
        }
    }

    private void checkErrorResult(Loader loader) {
        if(loader instanceof  TutkLinkNasLoader) {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, ((TutkLinkNasLoader) loader).getError(), Toast.LENGTH_SHORT).show();
        }
        else if(loader instanceof LoginLoader) {
            Bundle args = new Bundle();
            args.putBoolean("clean", true);
            getLoaderManager().restartLoader(LoaderID.TUTK_LOGOUT, args, this).forceLoad();
        }
        else {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkForgetPasswordResult(TutkForgetPasswordLoader loader) {
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

    private void checkLoginNASResult(TutkLoginLoader loader) {
        String code = loader.getCode();
        String status = loader.getStatus();

        String token = loader.getAuthToke();
        String email = loader.getEmail();
        String pwd = loader.getPassword();
        if (!token.equals("")) {
            //token not null mean login success
            NASPref.setCloudAccountStatus(this, NASPref.Status.Active.ordinal());
            NASPref.setCloudUsername(this, email);
            NASPref.setCloudPassword(this, pwd);
            NASPref.setCloudAuthToken(this, token);

            Bundle arg = new Bundle();
            arg.putString("server", loader.getServer());
            arg.putString("token", token);
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, arg, this).forceLoad();
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            if (code.equals(TutkCodeID.NOT_VERIFIED)) {
                //account not verified
                NASPref.setCloudAccountStatus(this, NASPref.Status.Padding.ordinal());
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
            mNASList = loader.getNasArrayList();
            String username = NASPref.getUsername(this);
            String password = NASPref.getPassword(this);

            if (!username.equals("") && !password.equals("") && mNASList.size() == 1) {
                Bundle arg = new Bundle();
                arg.putString("hostname", mNASList.get(0).get("hostname"));
                arg.putString("username", username);
                arg.putString("password", password);
                getLoaderManager().restartLoader(LoaderID.TUTK_NAS_LINK, arg, this).forceLoad();
            } else {
                mProgressView.setVisibility(View.INVISIBLE);
                startNASFinderActivity(mNASList, true);
            }
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLinkNASResult(TutkLinkNasLoader loader) {
        Bundle args = loader.getBundleArgs();
        args.putString("hostname", loader.getP2PHostname());
        getLoaderManager().restartLoader(LoaderID.LOGIN, args, this).forceLoad();
    }

    private void changeView(boolean init){
        layoutInit.setVisibility(init ? View.VISIBLE : View.GONE);
        layoutSignIn.setVisibility(init ? View.GONE : View.VISIBLE);
    }

    private void showForgetPwdDialog() {
        Bundle args = new Bundle();
        args.putString("title", getString(R.string.forget_password_title));
        args.putString("email", NASPref.getCloudUsername(this));
        mForgetDialog = new ForgetPwdDialog(StartActivity.this, args) {
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
        AnalysisFactory.getInstance(this).recordStartTime();
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
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
            case LoaderID.TUTK_NAS_LINK:
                return new TutkLinkNasLoader(this, args);
            case LoaderID.LOGIN:
                return new LoginLoader(this, args, true);
            case LoaderID.TUTK_LOGOUT:
                return new TutkLogoutLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        AnalysisFactory.getInstance(this).recordEndTime();
        if (!success) {
            checkErrorResult(loader);
            return;
        }

        if (loader instanceof TutkForgetPasswordLoader) {
            checkForgetPasswordResult((TutkForgetPasswordLoader) loader);
        } else if (loader instanceof TutkLoginLoader) {
            checkLoginNASResult((TutkLoginLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT, AnalysisFactory.ACTION.FINDREMOTE, success);
            checkGetNASResult((TutkGetNasLoader) loader);
        } else if (loader instanceof TutkLinkNasLoader) {
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT, AnalysisFactory.ACTION.LINKREMOTE, success);
            checkLinkNASResult((TutkLinkNasLoader) loader);
        } else if (loader instanceof LoginLoader) {
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT, AnalysisFactory.ACTION.LOGINREMOTE, success);
            startFileManageActivity();
        } else if (loader instanceof TutkLogoutLoader) {
            mProgressView.setVisibility(View.INVISIBLE);
            startNASFinderActivity(mNASList, true);
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
            if(layoutSignIn.getVisibility() == View.VISIBLE) {
                if(isInitial)
                    startInitialActivity();
                else
                    changeView(true);
            }
            else
                super.onBackPressed();
        }
    }
}
