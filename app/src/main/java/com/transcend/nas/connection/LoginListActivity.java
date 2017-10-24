package com.transcend.nas.connection;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerInfo;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.GoogleAnalysisFactory;
import com.transcend.nas.LoaderID;
import com.transcend.nas.tutk.P2PStautsLoader;
import com.transcend.nas.tutk.TutkCodeID;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.tutk.TutkCreateNasLoader;
import com.transcend.nas.tutk.TutkDeleteNasLoader;
import com.transcend.nas.tutk.TutkGetNasLoader;
import com.transcend.nas.tutk.TutkLinkNasLoader;
import com.transcend.nas.service.LanCheckManager;
import com.transcend.nas.view.NotificationDialog;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.P2PService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

import static com.transcend.nas.NASPref.useBrowserMinFirmwareVersion;

public class LoginListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {

    public static final int REQUEST_CODE = LoginListActivity.class.hashCode() & 0xFFFF;
    private static final String TAG = LoginListActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mAdapter;
    private RelativeLayout mProgressView;
    private int mLoaderID;
    private String mTutkUUID = "";
    private ArrayList<HashMap<String, String>> mNASList;
    private ArrayList<HashMap<String, String>> mLANList;

    private NASListDialog mNASListDialog;
    private WizardDialog mWizardDialog;
    private LoginDialog mLoginDialog;

    private boolean enableDeviceCheck = true;
    private int DeviceCheckTimeMax = 5000;
    private int DeviceCheckTimePeriod = 1000;
    private int mDeviceCheckTime = 0;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nas_finder);
        GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.START_NAS_LIST);
        initData();
        initToolbar();
        initRecyclerView();
        initProgressView();
        reloadNASListIfNotFound();
    }

    @Override
    protected void onDestroy() {
        IOTCAPIs.IOTC_DeInitialize();
        super.onDestroy();
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
                if (!mProgressView.isShown())
                    startLoginActivity();
                break;
            case R.id.action_refresh_nas_finder:
                if (!mProgressView.isShown())
                    startNASListLoader(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mProgressView.isShown()) {
            startLoginActivity();
        } else {
            if (LoaderID.TUTK_NAS_ONLINE_CHECK == mLoaderID)
                return;

            mProgressView.setVisibility(View.INVISIBLE);
            getLoaderManager().destroyLoader(mLoaderID);
        }
    }

    /**
     * INITIALIZATION
     */
    private void initData() {
        Intent intent = getIntent();
        mTutkUUID = intent.getStringExtra("uuid");
        mNASList = (ArrayList<HashMap<String, String>>) intent.getSerializableExtra("NASList");
        if (mNASList == null)
            mNASList = new ArrayList<HashMap<String, String>>();

        int init = IOTCAPIs.IOTC_Initialize2(0);
        Log.d(TAG, "IOTCAgent_Connect(.) IOTC_Initialize2=" + init);
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.nas_finder_toolbar);
        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_navi_backaarow_white);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.nas_finder_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter = new RecyclerViewAdapter());
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        //mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.nas_finder_progress_view);
    }

    private void reloadNASListIfNotFound() {
        if (mNASList == null || mNASList.size() == 0) {
            startNASListLoader(true);
        } else {
            startP2PStatusLoader();
        }
    }

    /**
     * ACTIVITY LAUNCHER
     */
    private void startLoginActivity() {
        if (getCallingActivity() == null) {
            Intent intent = new Intent();
            intent.setClass(LoginListActivity.this, LoginActivityNew.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent();
            LoginListActivity.this.setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    private void startFileManageActivity() {
        if (getCallingActivity() == null) {
            Intent intent = new Intent();
            intent.setClass(LoginListActivity.this, NASUtils.getFileManageClass());
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent();
            LoginListActivity.this.setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void startNASListLoader(boolean remoteAccess) {
        if (remoteAccess) {
            //tutk list
            Bundle args = new Bundle();
            args.putString("server", NASPref.getCloudServer(this));
            args.putString("token", NASPref.getCloudAuthToken(this));
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, args, this).forceLoad();
        } else {
            //local list

            //get network status
            boolean isWiFi = false;
            ConnectivityManager mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = mConnMgr.getActiveNetworkInfo();
            if (info != null)
                isWiFi = (info.getType() == ConnectivityManager.TYPE_WIFI);

            if(isWiFi) {
                showListDialog(null);
                getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
            } else {
                showWifiNotificationDialog();
            }
        }
    }

    private void startP2PStatusLoader() {
        if (enableDeviceCheck)
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_ONLINE_CHECK, null, this).forceLoad();
        else
            mProgressView.setVisibility(View.INVISIBLE);
    }

    private void startRemoteAccessDeleteLoader(Bundle args) {
        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_DELETE, args, this).forceLoad();
    }

    private void startLoginLoader(Bundle args) {
        getLoaderManager().restartLoader(LoaderID.LOGIN, args, this).forceLoad();
    }

    private void startTutkCreateNasLoader(Bundle args) {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String nasName = server.getServerInfo().hostName;
        String uuid = server.getTutkUUID();
        String serialNum = NASPref.getSerialNum(LoginListActivity.this);
        if (serialNum != null && !serialNum.equals(""))
            nasName = nasName + NASApp.TUTK_NAME_TAG + serialNum;

        boolean wizard = args.getBoolean("wizard", false);
        if (!wizard) {
            for (HashMap<String, String> nas : mNASList) {
                String hostname = nas.get("hostname");
                if (hostname.equals(uuid)) {
                    hideDialog(true);
                    startFileManageActivity();
                    return;
                }
            }
        }

        if (uuid != null && !uuid.equals("")) {
            args.putString("server", NASPref.getCloudServer(LoginListActivity.this));
            args.putString("token", NASPref.getCloudAuthToken(LoginListActivity.this));
            args.putString("nasName", nasName);
            args.putString("nasUUID", uuid);
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_CREATE, args, LoginListActivity.this).forceLoad();
        } else {
            hideDialog(wizard);
            Toast.makeText(LoginListActivity.this, getString(R.string.error), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * LOADER CONTROL
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        String server, token, nasId;
        switch (mLoaderID = id) {
            case LoaderID.TUTK_NAS_GET:
                mProgressView.setVisibility(View.VISIBLE);
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
            case LoaderID.TUTK_NAS_ONLINE_CHECK:
                mProgressView.setVisibility(View.VISIBLE);
                return new P2PStautsLoader(this, mNASList, 3000);
            case LoaderID.TUTK_NAS_DELETE:
                mProgressView.setVisibility(View.VISIBLE);
                server = args.getString("server");
                token = args.getString("token");
                nasId = args.getString("nasId");
                return new TutkDeleteNasLoader(this, server, token, nasId);
            case LoaderID.TUTK_NAS_LINK:
                mProgressView.setVisibility(View.VISIBLE);
                return new TutkLinkNasLoader(this, args);
            case LoaderID.NAS_LIST:
                return new NASListLoader(this, true);
            case LoaderID.WIZARD:
                mProgressView.setVisibility(View.VISIBLE);
                return new WizardCheckLoader(this, args);
            case LoaderID.WIZARD_INIT:
                return new WizardSetLoader(this, args);
            case LoaderID.LOGIN:
                return new LoginLoader(this, args);
            case LoaderID.TUTK_NAS_CREATE:
                return new TutkCreateNasLoader(this, args);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof NASListLoader) {
            checkNasListLoader(success, (NASListLoader) loader);
        } else if (loader instanceof WizardCheckLoader) {
            checkWizardLoader(success, (WizardCheckLoader) loader);
        } else if (loader instanceof LoginLoader) {
            checkLoginLoader(success, (LoginLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            checkTutkGetNasLoader(success, (TutkGetNasLoader) loader);
        } else if (loader instanceof TutkDeleteNasLoader) {
            checkTutkDeleteNasLoader(success, (TutkDeleteNasLoader) loader);
        } else if (loader instanceof TutkLinkNasLoader) {
            checkTutkLinkNasLoader(success, (TutkLinkNasLoader) loader);
        } else if (loader instanceof P2PStautsLoader) {
            checkP2PStatusLoader(success, (P2PStautsLoader) loader);
        } else if (loader instanceof WizardSetLoader) {
            checkWizardSetLoader(success, (WizardSetLoader) loader);
        } else if (loader instanceof TutkCreateNasLoader) {
            checkCreateNASResult(success, (TutkCreateNasLoader) loader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        Log.w(TAG, "onLoaderReset: " + loader.getClass().getSimpleName());
    }

    private void checkNasListLoader(boolean success, NASListLoader loader) {
        mLANList = loader.getList();

        //merge the list from android NsdManager
        LanCheckManager.getInstance().stopAndroidDiscovery();
        ArrayList<HashMap<String, String>> nasList = LanCheckManager.getInstance().getAndroidDiscoveryList();
        for (HashMap<String, String> nas : nasList) {
            boolean add = true;
            String nickname = nas.get("nickname");
            String hostname = nas.get("hostname");
            for (HashMap<String, String> tmp : mLANList) {
                String tmpHostname = tmp.get("hostname");
                if (hostname != null && hostname.equals(tmpHostname)) {
                    add = false;
                    break;
                }
            }

            if (add) {
                mLANList.add(nas);
                Log.d(TAG, "Add service " + nickname + ", " + hostname + " from NsdManager");
            } else
                Log.d(TAG, "Ignore service " + nickname + ", " + hostname + " from NsdManager");
        }

        showListDialog(mLANList);
    }

    private void checkLoginLoader(boolean success, LoginLoader loader) {
        Bundle args = loader.getBundleArgs();
        if (!success) {
            boolean wizard = args.getBoolean("wizard", false);
            hideDialog(wizard);
            Toast.makeText(this, loader.getLoginError(), Toast.LENGTH_SHORT).show();
            return;
        }

        startTutkCreateNasLoader(args);
    }

    private void checkCreateNASResult(boolean success, TutkCreateNasLoader loader) {
        boolean wizard = loader.getBundleArgs().getBoolean("wizard");
        if (!success) {
            hideDialog(wizard);
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        String status = loader.getStatus();
        String code = loader.getCode();
        if (code.equals("") || code.equals(TutkCodeID.SUCCESS) || code.equals(TutkCodeID.UID_ALREADY_TAKEN)) {
            if (wizard && mWizardDialog != null) {
                //hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                mWizardDialog.showFinishView();
            } else {
                hideDialog(true);
                startFileManageActivity();
            }
        } else {
            hideDialog(wizard);
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkTutkGetNasLoader(boolean success, TutkGetNasLoader loader) {
        if (!success) {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        String code = loader.getCode();
        String status = loader.getStatus();
        if (code.equals("")) {
            mNASList = loader.getNasArrayList();
            mAdapter.notifyDataSetChanged();
            if (mNASList != null && mNASList.size() > 0) {
                startP2PStatusLoader();
                return;
            }
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
            if (code.equals(TutkCodeID.AUTH_FAIL)) {
                startLoginActivity();
            }
        }
        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void checkTutkDeleteNasLoader(boolean success, TutkDeleteNasLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        if (!success) {
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        String code = loader.getCode();
        String status = loader.getStatus();

        if (code.equals(TutkCodeID.SUCCESS)) {
            String id = loader.getNasID();
            for (HashMap<String, String> nas : mNASList) {
                if (id.equals(nas.get("nasId"))) {
                    mNASList.remove(nas);
                    LoginHelper loginHelper = new LoginHelper(LoginListActivity.this);
                    LoginHelper.LoginInfo account = new LoginHelper.LoginInfo();
                    account.email = NASPref.getCloudUsername(LoginListActivity.this);
                    account.uuid = nas.get("hostname");
                    loginHelper.deleteAccount(account);
                    break;
                }
            }
            mAdapter.notifyDataSetChanged();
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
            if (code.equals(TutkCodeID.AUTH_FAIL)) {
                startLoginActivity();
            }
        }
    }

    private void checkTutkLinkNasLoader(boolean success, TutkLinkNasLoader loader) {
        if (!success) {
            //get account info from db
            LoginHelper loginHelper = new LoginHelper(this);
            LoginHelper.LoginInfo account = new LoginHelper.LoginInfo();
            account.email = NASPref.getCloudUsername(this);
            account.uuid = loader.getNasUUID();
            boolean exist = loginHelper.getAccount(account);

            //get network status
            boolean isWiFi = false;
            ConnectivityManager mConnMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = mConnMgr.getActiveNetworkInfo();
            if (info != null)
                isWiFi = (info.getType() == ConnectivityManager.TYPE_WIFI);

            if (exist && isWiFi) {
                //Link nas's tutk server fail, try lan connect
                Bundle args = loader.getBundleArgs();
                args.putString("hostname", account.ip);
                args.putBoolean("RemoteAccess", false);
                getLoaderManager().restartLoader(LoaderID.WIZARD, args, LoginListActivity.this).forceLoad();
            } else {
                mProgressView.setVisibility(View.INVISIBLE);
                if (isWiFi)
                    startNASListLoader(false);
                else {
                    String error = loader.getError();
                    if (enableDeviceCheck) {
                        for (HashMap<String, String> nas : mNASList) {
                            String UID = nas.get("hostname");
                            if (UID != null && UID.equals(account.uuid)) {
                                error = "no".equals(nas.get("online")) ? getString(R.string.offline) : error;
                                break;
                            }
                        }
                    }

                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }

        Bundle args = loader.getBundleArgs();
        args.putString("hostname", loader.getP2PHostname());
        getLoaderManager().restartLoader(LoaderID.WIZARD, args, this).forceLoad();
    }

    private void checkP2PStatusLoader(boolean success, final P2PStautsLoader loader) {
        if (success) {
            mDeviceCheckTime = DeviceCheckTimeMax;
            if (mHandler == null)
                mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (loader.isIOTCReady()) {
                        mNASList = loader.getNasList();
                        mAdapter.notifyDataSetChanged();
                        mProgressView.setVisibility(View.INVISIBLE);
                    } else {
                        mDeviceCheckTime -= DeviceCheckTimePeriod;
                        if (mDeviceCheckTime > 0) {
                            mHandler.postDelayed(this, DeviceCheckTimePeriod);
                            Log.d(TAG, "P2PStatusLoader remain time : " + mDeviceCheckTime);
                        } else {
                            mProgressView.setVisibility(View.INVISIBLE);
                            Toast.makeText(LoginListActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "P2PStatusLoader timeout");
                        }
                    }
                }
            }, DeviceCheckTimePeriod);
        } else {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(LoginListActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkWizardLoader(boolean success, WizardCheckLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        if (!success) {
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = loader.getBundleArgs();
        boolean remoteAccess = args.getBoolean("RemoteAccess", false);
        if (loader.isWizard()) {
            LoginHelper loginHelper = new LoginHelper(LoginListActivity.this);
            LoginHelper.LoginInfo account = new LoginHelper.LoginInfo();
            account.email = NASPref.getCloudUsername(LoginListActivity.this);
            account.macAddress = loader.getMacAddress();
            boolean exist = loginHelper.getAccount(account);

            if (account.username != null && !account.username.equals(""))
                args.putString("username", account.username);
            else
                args.putString("username", NASPref.defaultUserName);
            args.putString("password", account.password);

            showLoginDialog(args, remoteAccess, exist);
        } else {
            showWizardDialog(args);
        }
    }

    private void checkWizardSetLoader(boolean success, WizardSetLoader loader) {
        if (!success) {
            String error = loader.getErrorResult();
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            if (error.equals(getString(R.string.network_error))) {
                hideDialog(false);
            } else {
                hideDialog(true);
            }
            return;
        }

        Bundle args = loader.getBundleArgs();
        args.putBoolean("wizard", true);
        startLoginLoader(args);
    }

    private void showListDialog(ArrayList<HashMap<String, String>> nasList) {
        if (mNASListDialog != null) {
            mNASListDialog.updateListView(nasList);
        } else {
            mNASListDialog = new NASListDialog(this, nasList) {
                @Override
                public void onConfirm(Bundle args) {
                    boolean refresh = args.getBoolean("refresh", false);
                    if (refresh) {
                        startNASListLoader(false);
                    } else {
                        hideDialog(true);
                        getLoaderManager().restartLoader(LoaderID.WIZARD, args, LoginListActivity.this).forceLoad();
                    }
                }

                @Override
                public void onCancel() {
                    LanCheckManager.getInstance().stopAndroidDiscovery();
                    getLoaderManager().destroyLoader(mLoaderID);
                    hideDialog(true);
                }
            };
        }
    }

    private void hideDialog(boolean dismiss) {
        if (mWizardDialog != null) {
            if (dismiss) {
                mWizardDialog.dismiss();
                mWizardDialog = null;
            } else
                mWizardDialog.hideProgress();
        }

        if (mLoginDialog != null) {
            if (dismiss) {
                mLoginDialog.dismiss();
                mLoginDialog = null;
            } else
                mLoginDialog.hideProgress();
        }

        if (mNASListDialog != null) {
            if (dismiss) {
                mNASListDialog.dismiss();
                mNASListDialog = null;
            } else
                mNASListDialog.hideProgress();
        }
    }

    private void showWizardDialog(Bundle args) {
        mWizardDialog = new WizardDialog(this, args) {
            @Override
            public void onConfirm(Bundle args) {
                boolean finish = args.getBoolean("finish", false);
                if (finish) {
                    startFileManageActivity();
                } else {
                    //args already contain hostname, username, password
//                    String timezone = Integer.toString(TimeZone.getDefault().getRawOffset() / 3600000);
                    String timezone = TimeZone.getDefault().getID();
                    args.putString("nickname", getString(R.string.wizard_success));
                    args.putString("timezone", timezone);
                    getLoaderManager().restartLoader(LoaderID.WIZARD_INIT, args, LoginListActivity.this).forceLoad();
                }
            }

            @Override
            public void onCancel() {
                getLoaderManager().destroyLoader(mLoaderID);
                hideDialog(true);
            }
        };
    }

    private void showLoginDialog(Bundle args, final boolean remoteAccess, boolean startProgress) {
        mLoginDialog = new LoginDialog(LoginListActivity.this, args, remoteAccess, false) {
            @Override
            public void onConfirm(Bundle args) {
                if (args != null) {
                    String username = args.getString("username");
                    GoogleAnalysisFactory.getInstance(LoginListActivity.this).
                            sendEvent(GoogleAnalysisFactory.VIEW.START_NAS_LIST, GoogleAnalysisFactory.ACTION.LoginNas,
                                    NASPref.defaultUserName.equals(username) ? GoogleAnalysisFactory.LABEL.LoginByAdmin : GoogleAnalysisFactory.LABEL.LoginByNonAdmin);
                }
                startLoginLoader(args);
            }

            @Override
            public void onDelete(Bundle args) {
                startRemoteAccessDeleteLoader(args);
            }

            @Override
            public void onCancel() {
                getLoaderManager().destroyLoader(mLoaderID);
                hideDialog(true);
            }
        };
        if (startProgress) {
            mLoginDialog.showProgress();
            startLoginLoader(args);
        }
    }

    private void showWifiNotificationDialog(){
        Bundle value = new Bundle();
        value.putString(NotificationDialog.DIALOG_MESSAGE, getString(R.string.wizard_wifi_info));
        new NotificationDialog(this, value) {
            @Override
            public void onConfirm() {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }

            @Override
            public void onCancel() {

            }
        };
    }

    private void showNotificationDialog(final Bundle args) {
        Bundle value = new Bundle();
        value.putString(NotificationDialog.DIALOG_MESSAGE, getString(R.string.remote_access_delete_warning));
        new NotificationDialog(this, value) {
            @Override
            public void onConfirm() {
                startRemoteAccessDeleteLoader(args);
            }

            @Override
            public void onCancel() {

            }
        };
    }

    /**
     * RECYCLER VIEW ADAPTER
     */
    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(LoginListActivity.this);
            View view = layoutInflater.inflate(R.layout.listitem_login_finder, parent, false);
            ViewHolder holder = new ViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (mNASList != null && position < mNASList.size()) {
                holder.split.setVisibility(View.VISIBLE);
                holder.listItem.setVisibility(View.VISIBLE);
                holder.addItem.setVisibility(View.GONE);
                String nickname = mNASList.get(position).get("nickname");
                String hostname = mNASList.get(position).get("hostname");
                if (nickname != null && nickname.contains(NASApp.TUTK_NAME_TAG)) {
                    String[] splitname = nickname.split(NASApp.TUTK_NAME_TAG);
                    int length = splitname.length;
                    if (length == 0) {
                        holder.title.setText(nickname);
                        holder.subtitle.setText(hostname);
                    } else if (length == 1) {
                        holder.title.setText(splitname[0]);
                        holder.subtitle.setText(hostname);
                    } else {
                        holder.title.setText(splitname[0]);
                        holder.subtitle.setText(splitname[1]);
                    }
                } else {
                    holder.title.setText(nickname);
                    holder.subtitle.setText(hostname);
                }

                if (mTutkUUID != null && !mTutkUUID.equals(""))
                    holder.delete.setVisibility(mTutkUUID.equals(hostname) ? View.INVISIBLE : View.VISIBLE);

                if (enableDeviceCheck) {
                    int colorPrimary = ContextCompat.getColor(LoginListActivity.this, R.color.textColorPrimary);
                    int colorSecondary = ContextCompat.getColor(LoginListActivity.this, R.color.textColorSecondary);
                    String isOnLine = mNASList.get(position).get("online");
                    if (isOnLine != null && !isOnLine.equals("")) {
                        if (isOnLine.equals("yes")) {
                            holder.subtitle.setText(getString(R.string.online));
                            holder.subtitle.setTextColor(colorPrimary);
                            holder.title.setTextColor(colorPrimary);
                        } else {
                            holder.subtitle.setText(getString(R.string.offline));
                            holder.subtitle.setTextColor(colorSecondary);
                            holder.title.setTextColor(colorSecondary);
                        }
                    } else {
                        holder.subtitle.setText(getString(R.string.loading));
                        holder.subtitle.setTextColor(colorPrimary);
                        holder.title.setTextColor(colorPrimary);
                    }
                }
            } else {
                //this is the 'Add' item
                holder.listItem.setVisibility(View.GONE);
                holder.addItem.setVisibility(View.VISIBLE);
                if (mNASList == null || mNASList.size() == 0) {
                    holder.split.setVisibility(View.GONE);
                } else {
                    holder.split.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public int getItemCount() {
            int size = 1;
            if (mNASList != null)
                size = mNASList.size() + 1;
            return size;
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            View split;
            ImageView icon;
            TextView title;
            TextView subtitle;
            ImageView delete;
            RelativeLayout listItem;
            LinearLayout addItem;

            public ViewHolder(View itemView) {
                super(itemView);
                split = (View) itemView.findViewById(R.id.listitem_split);
                icon = (ImageView) itemView.findViewById(R.id.listitem_nas_finder_icon);
                title = (TextView) itemView.findViewById(R.id.listitem_nas_finder_title);
                subtitle = (TextView) itemView.findViewById(R.id.listitem_nas_finder_subtitle);
                delete = (ImageView) itemView.findViewById(R.id.listitem_nas_delete_icon);
                delete.setOnClickListener(this);
                listItem = (RelativeLayout) itemView.findViewById(R.id.listitem_nas_finder_list_layout);
                addItem = (LinearLayout) itemView.findViewById(R.id.listitem_nas_finder_add_layout);
                addItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startNASListLoader(false);
                    }
                });
                addItem.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                v.setBackground(ContextCompat.getDrawable(LoginListActivity.this, R.drawable.button_dotted_line_press));
                                break;
                            case MotionEvent.ACTION_CANCEL:
                            case MotionEvent.ACTION_UP:
                                v.setBackground(ContextCompat.getDrawable(LoginListActivity.this, R.drawable.button_dotted_line));
                                break;
                        }
                        return false;
                    }
                });
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int pos = getAdapterPosition();
                if (mNASList != null && pos < mNASList.size()) {
                    HashMap<String, String> nas = mNASList.get(pos);
                    Bundle args = new Bundle();
                    if (v.getId() == R.id.listitem_nas_delete_icon) {
                        args.putString("server", NASPref.getCloudServer(LoginListActivity.this));
                        args.putString("token", NASPref.getCloudAuthToken(LoginListActivity.this));
                        args.putString("nasId", nas.get("nasId"));
                        showNotificationDialog(args);
                    } else {
                        String nickname = nas.get("nickname");
                        String hostname = nas.get("hostname");

                        if (mTutkUUID != null && mTutkUUID.equals(hostname)) {
                            startFileManageActivity();
                            return;
                        }

                        if (nickname != null && nickname.contains(NASApp.TUTK_NAME_TAG)) {
                            String[] splitname = nickname.split(NASApp.TUTK_NAME_TAG);
                            if (splitname.length >= 1) {
                                nickname = splitname[0];
                            }
                        }

                        args.putString("nasId", nas.get("nasId"));
                        args.putString("nickname", nickname);
                        args.putString("hostname", hostname);
                        args.putString("username", NASPref.getUsername(LoginListActivity.this));
                        args.putString("password", NASPref.getPassword(LoginListActivity.this));
                        args.putBoolean("RemoteAccess", true);
                        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_LINK, args, LoginListActivity.this).forceLoad();
                    }
                } else {
                    startNASListLoader(false);
                }
            }
        }
    }

}
