package com.transcend.nas.connection_new;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Color;
import android.os.Bundle;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.AnalysisFactory;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.TutkCodeID;
import com.transcend.nas.connection.LoginDialog;
import com.transcend.nas.connection.LoginLoader;
import com.transcend.nas.connection.NASListLoader;
import com.transcend.nas.connection.P2PStautsLoader;
import com.transcend.nas.connection.WizardCheckLoader;
import com.transcend.nas.connection.WizardSetLoader;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.TutkDeleteNasLoader;
import com.transcend.nas.management.TutkGetNasLoader;
import com.transcend.nas.management.TutkLinkNasLoader;
import com.transcend.nas.view.NotificationDialog;
import com.tutk.IOTC.P2PService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

public class LoginListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {

    public static final int REQUEST_CODE = LoginListActivity.class.hashCode() & 0xFFFF;
    private static final String TAG = LoginListActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mAdapter;
    private RelativeLayout mProgressView;
    private LoginDialog mLoginDialog;
    private int mLoaderID;
    private ListDialog mListDialog;
    private WizardDialog mWizardDialog;

    private ArrayList<HashMap<String, String>> mNASList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nas_finder);
        initData();
        initToolbar();
        initRecyclerView();
        initProgressView();
        reloadNASListIfNotFound();
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
                if (mProgressView.isShown()) {
                    mProgressView.setVisibility(View.INVISIBLE);
                    getLoaderManager().destroyLoader(mLoaderID);
                }
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
        if (mProgressView.isShown()) {
            mProgressView.setVisibility(View.INVISIBLE);
            getLoaderManager().destroyLoader(mLoaderID);
        } else {
            startLoginActivity();
        }
    }

    /**
     * INITIALIZATION
     */
    private void initData() {
        Intent intent = getIntent();
        mNASList = (ArrayList<HashMap<String, String>>) intent.getSerializableExtra("NASList");
        if (mNASList == null)
            mNASList = new ArrayList<HashMap<String, String>>();
        AnalysisFactory.getInstance(this).sendScreen(AnalysisFactory.VIEW.NASLISTREMOTE);
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.nas_finder_toolbar);
        mToolbar.setTitle("");
        mToolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_gray_24dp);
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
            intent.setClass(LoginListActivity.this, LoginActivity.class);
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
            intent.setClass(LoginListActivity.this, FileManageActivity.class);
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
            showListDialog(null);
            getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
        }
    }

    private void startP2PStatusLoader() {
        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_ONLINE_CHECK, null, this).forceLoad();
    }

    private void startRemoteAccessDeleteLoader(Bundle args) {
        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_DELETE, args, this).forceLoad();
    }

    private void startLoginLoader(Bundle args) {
        boolean remoteAccess = args.getBoolean("RemoteAccess", false);
        getLoaderManager().restartLoader(remoteAccess ? LoaderID.TUTK_NAS_LINK : LoaderID.LOGIN, args, this).forceLoad();
    }

    /**
     * LOADER CONTROL
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        AnalysisFactory.getInstance(this).recordStartTime();
        String server, token, nasId;
        boolean remoteAccess = false;
        switch (mLoaderID = id) {
            case LoaderID.TUTK_NAS_GET:
                mProgressView.setVisibility(View.VISIBLE);
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
            case LoaderID.TUTK_NAS_ONLINE_CHECK:
                mProgressView.setVisibility(View.VISIBLE);
                return new P2PStautsLoader(LoginListActivity.this, mNASList);
            case LoaderID.TUTK_NAS_DELETE:
                mProgressView.setVisibility(View.VISIBLE);
                server = args.getString("server");
                token = args.getString("token");
                nasId = args.getString("nasId");
                return new TutkDeleteNasLoader(this, server, token, nasId);
            case LoaderID.TUTK_NAS_LINK:
                return new TutkLinkNasLoader(this, args);
            case LoaderID.NAS_LIST:
                return new NASListLoader(this);
            case LoaderID.WIZARD:
                mProgressView.setVisibility(View.VISIBLE);
                remoteAccess = args.getBoolean("RemoteAccess", false);
                return new WizardCheckLoader(this, args, remoteAccess);
            case LoaderID.WIZARD_INIT:
                return new WizardSetLoader(this, args, false);
            case LoaderID.LOGIN:
                return new LoginLoader(this, args, true);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        AnalysisFactory.getInstance(this).recordEndTime();
        if (loader instanceof NASListLoader) {
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT, AnalysisFactory.ACTION.FINDLOCAL, success);
            checkNasListLoader(success, (NASListLoader) loader);
        } else if (loader instanceof WizardCheckLoader) {
            checkWizardLoader(success, (WizardCheckLoader) loader);
        } else if (loader instanceof LoginLoader) {
            //TODO : add google analysis
            //AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT,AnalysisFactory.ACTION.LOGINREMOTE : AnalysisFactory.ACTION.LOGINLOCAL, success);
            checkLoginLoader(success, (LoginLoader) loader);
        } else if (loader instanceof TutkGetNasLoader) {
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT, AnalysisFactory.ACTION.FINDREMOTE, success);
            checkTutkGetNasLoader(success, (TutkGetNasLoader) loader);
        } else if (loader instanceof TutkDeleteNasLoader) {
            checkTutkDeleteNasLoader(success, (TutkDeleteNasLoader) loader);
        } else if (loader instanceof TutkLinkNasLoader) {
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT, AnalysisFactory.ACTION.LINKREMOTE, success);
            checkTutkLinkNasLoader(success, (TutkLinkNasLoader) loader);
        } else if (loader instanceof P2PStautsLoader) {
            AnalysisFactory.getInstance(this).sendTimeEvent(AnalysisFactory.EVENT.CONNECT, AnalysisFactory.ACTION.CHECKREMOTE, success);
            checkP2PStatusLoader(success, (P2PStautsLoader) loader);
        }
        if (loader instanceof WizardSetLoader) {
            //TODO : add google analysis
            checkWizardSetLoader(success, (WizardSetLoader) loader);
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        Log.w(TAG, "onLoaderReset: " + loader.getClass().getSimpleName());
    }

    private void checkNasListLoader(boolean success, NASListLoader loader) {
        showListDialog(loader.getList());
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
            showLoginDialog(args, remoteAccess, false);
        } else {
            if (remoteAccess)
                Toast.makeText(this, getString(R.string.wizard_remote_access_error), Toast.LENGTH_SHORT).show();
            else
                showWizardDialog(args);
        }
    }

    private void checkLoginLoader(boolean success, LoginLoader loader) {
        if (!success) {
            hideLoginDialog(false);
            Toast.makeText(this, ((LoginLoader) loader).getLoginError(), Toast.LENGTH_SHORT).show();
            return;
        }

        //TODO : record the nas information next connection
        hideLoginDialog(true);
        startFileManageActivity();
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
                getLoaderManager().restartLoader(LoaderID.TUTK_NAS_ONLINE_CHECK, null, this).forceLoad();
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
            hideLoginDialog(false);
            Toast.makeText(this, loader.getError(), Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = loader.getBundleArgs();
        String ip = P2PService.getInstance().getP2PIP();
        int port = P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
        args.putString("hostname", ip + ":" + port);
        getLoaderManager().restartLoader(LoaderID.LOGIN, args, this).forceLoad();
    }

    private void checkP2PStatusLoader(boolean success, P2PStautsLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        mAdapter.notifyDataSetChanged();
    }

    private void checkWizardSetLoader(boolean success, WizardSetLoader loader) {
        if (!success) {
            String error = loader.getErrorResult();
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            if (error.equals(getString(R.string.network_error))) {
                hideWizardDialog(false);
            } else {
                hideWizardDialog(true);
            }
            return;
        }
        showLoginDialog(loader.getBundleArgs(), false, false);
    }

    private void hideListDialog(boolean dismiss) {
        if (mListDialog != null) {
            if (dismiss) {
                mListDialog.dismiss();
                mListDialog = null;
            } else
                mListDialog.hideProgress();
        }
    }

    private void showListDialog(ArrayList<HashMap<String, String>> nasList) {
        if (mListDialog != null) {
            mListDialog.updateListView(nasList);
        } else {
            mListDialog = new ListDialog(this, nasList) {
                @Override
                public void onConfirm(Bundle args) {
                    boolean refresh = args.getBoolean("refresh", false);
                    if (refresh) {
                        startNASListLoader(false);
                    } else {
                        args.putString("username", NASPref.getUsername(LoginListActivity.this));
                        args.putString("password", NASPref.getPassword(LoginListActivity.this));
                        args.putBoolean("RemoteAccess", false);
                        getLoaderManager().restartLoader(LoaderID.WIZARD, args, LoginListActivity.this).forceLoad();
                        hideListDialog(true);
                    }
                }

                @Override
                public void onCancel() {
                    getLoaderManager().destroyLoader(mLoaderID);
                    hideListDialog(true);
                }
            };
        }
    }

    private void hideWizardDialog(boolean dismiss) {
        if (mWizardDialog != null) {
            if (dismiss) {
                mWizardDialog.dismiss();
                mWizardDialog = null;
            } else
                mWizardDialog.hideProgress();
        }
    }

    private void showWizardDialog(Bundle args) {
        mWizardDialog = new WizardDialog(this, args) {
            @Override
            public void onConfirm(Bundle args) {
                //args contain hostname, username, password
                String timezone = Integer.toString(TimeZone.getDefault().getRawOffset() / 3600000);
                args.putString("nickname", getString(R.string.wizard_success));
                args.putString("timezone", timezone);
                getLoaderManager().restartLoader(LoaderID.WIZARD_INIT, args, LoginListActivity.this).forceLoad();
            }

            @Override
            public void onCancel() {
                getLoaderManager().destroyLoader(mLoaderID);
                hideWizardDialog(true);
            }
        };
    }

    private void hideLoginDialog(boolean dismiss) {
        if (mLoginDialog != null) {
            P2PService.getInstance().stopP2PConnect();
            if (dismiss) {
                mLoginDialog.dismiss();
                mLoginDialog = null;
            } else
                mLoginDialog.hideProgress();
        }
    }

    private void showLoginDialog(Bundle args, boolean remoteAccess, boolean delete) {
        mLoginDialog = new LoginDialog(LoginListActivity.this, args, remoteAccess, delete) {
            @Override
            public void onConfirm(Bundle args) {
                startLoginLoader(args);
            }

            @Override
            public void onDelete(Bundle args) {
                startRemoteAccessDeleteLoader(args);
            }

            @Override
            public void onCancel() {
                getLoaderManager().destroyLoader(mLoaderID);
                hideLoginDialog(true);
            }
        };
    }

    private void showNotificationDialog(final Bundle args) {
        Bundle value = new Bundle();
        value.putString(NotificationDialog.DIALOG_MESSAGE, getString(R.string.remote_access_delete_warning));
        NotificationDialog mNotificationDialog = new NotificationDialog(this, value) {
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

                String isOnLine = mNASList.get(position).get("online");
                if (isOnLine != null && !isOnLine.equals("")) {
                    if (isOnLine.equals("yes")) {
                        holder.subtitle.setText(getString(R.string.online));
                        holder.subtitle.setTextColor(Color.GREEN);
                    } else {
                        holder.subtitle.setText(getString(R.string.offline));
                        holder.subtitle.setTextColor(ContextCompat.getColor(LoginListActivity.this, R.color.textColorSecondary));
                    }
                } else {
                    holder.subtitle.setTextColor(ContextCompat.getColor(LoginListActivity.this, R.color.textColorSecondary));
                    holder.subtitle.setText(getString(R.string.loading));
                }
            } else {
                //this is the 'Add' item
                holder.listItem.setVisibility(View.GONE);
                holder.addItem.setVisibility(View.VISIBLE);
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

            ImageView icon;
            TextView title;
            TextView subtitle;
            ImageView delete;
            RelativeLayout listItem;
            LinearLayout addItem;

            public ViewHolder(View itemView) {
                super(itemView);
                icon = (ImageView) itemView.findViewById(R.id.listitem_nas_finder_icon);
                title = (TextView) itemView.findViewById(R.id.listitem_nas_finder_title);
                subtitle = (TextView) itemView.findViewById(R.id.listitem_nas_finder_subtitle);
                delete = (ImageView) itemView.findViewById(R.id.listitem_nas_delete_icon);
                delete.setOnClickListener(this);
                listItem = (RelativeLayout) itemView.findViewById(R.id.listitem_nas_finder_list_layout);
                addItem = (LinearLayout) itemView.findViewById(R.id.listitem_nas_finder_add_layout);
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
                        String isOnLine = nas.get("online");
                        if ("no".equals(isOnLine)) {
                            return;
                        }

                        args.putString("nasId", nas.get("nasId"));
                        String nickname = nas.get("nickname");
                        if (nickname != null && nickname.contains(NASApp.TUTK_NAME_TAG)) {
                            String[] splitname = nickname.split(NASApp.TUTK_NAME_TAG);
                            if (splitname.length >= 1) {
                                nickname = splitname[0];
                            }
                        }
                        args.putString("nickname", nickname);
                        args.putString("hostname", nas.get("hostname"));
                        args.putString("username", NASPref.getUsername(LoginListActivity.this));
                        args.putString("password", NASPref.getPassword(LoginListActivity.this));
                        args.putBoolean("RemoteAccess", true);
                        getLoaderManager().restartLoader(LoaderID.WIZARD, args, LoginListActivity.this).forceLoad();
                    }
                } else {
                    startNASListLoader(false);
                }
            }
        }
    }

}
