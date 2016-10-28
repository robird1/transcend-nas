package com.transcend.nas.connection;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import com.transcend.nas.connection_new.LoginActivity;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.view.DividerItemDecoration;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.view.NotificationDialog;
import com.transcend.nas.common.TutkCodeID;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.TutkDeleteNasLoader;
import com.transcend.nas.management.TutkGetNasLoader;
import com.transcend.nas.management.TutkLinkNasLoader;
import com.tutk.IOTC.P2PService;

import java.util.ArrayList;
import java.util.HashMap;

public class NASListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {

    public static final int REQUEST_CODE = NASListActivity.class.hashCode() & 0xFFFF;
    private static final String TAG = NASListActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private LinearLayout mRecyclerEmtpyView;
    private RecyclerViewAdapter mAdapter;
    private RelativeLayout mProgressView;
    private LoginDialog mLoginDialog;
    private boolean isRemoteAccess = false;
    private boolean isWizard = false;
    private int mLoaderID;
    private boolean isEnableTutkDeviceCheck = false;

    private ArrayList<HashMap<String, String>> mNASList;
    private ArrayList<String> mResults;

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
                if (isWizard)
                    startInitialActivity();
                else
                    startSignInActivity();
                break;
            case R.id.action_refresh_nas_finder:
                if (!mProgressView.isShown())
                    startNASListLoader();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mLoginDialog != null) {
            getLoaderManager().destroyLoader(mLoaderID);
            hideLoginDialog(true);
        } else if (mProgressView.isShown()) {
            mProgressView.setVisibility(View.INVISIBLE);
            getLoaderManager().destroyLoader(mLoaderID);
            if (mLoaderID == LoaderID.NAS_LIST || mLoaderID == LoaderID.TUTK_NAS_GET) {
                mRecyclerEmtpyView.setVisibility((mNASList != null && mNASList.size() > 0) ? View.GONE : View.VISIBLE);
            }
        } else {
            if (isWizard)
                startInitialActivity();
            else
                startSignInActivity();
        }
    }


    /**
     * INITIALIZATION
     */
    private void initData() {
        Intent intent = getIntent();
        isWizard = (boolean) intent.getBooleanExtra("Wizard", false);
        isRemoteAccess = (boolean) intent.getBooleanExtra("RemoteAccess", false);
        mNASList = (ArrayList<HashMap<String, String>>) intent.getSerializableExtra("NASList");
        if (mNASList == null)
            mNASList = new ArrayList<HashMap<String, String>>();
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
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        mRecyclerEmtpyView = (LinearLayout) findViewById(R.id.nas_finder_recycler_empty_view);
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.nas_finder_progress_view);
    }

    private void reloadNASListIfNotFound() {
        if (mNASList == null || mNASList.size() == 0) {
            startNASListLoader();
        } else {
            if (isRemoteAccess)
                startP2PStatusLoader();
        }
    }


    /**
     * ACTIVITY LAUNCHER
     */
    private void startSignInActivity() {
        if (getCallingActivity() == null) {
            Intent intent = new Intent();
            intent.setClass(NASListActivity.this, StartActivity.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent();
            NASListActivity.this.setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    private void startInitialActivity() {
        Intent intent = new Intent();
        intent.setClass(NASListActivity.this, GuideActivity.class);
        intent.putExtra("Retry", false);
        startActivity(intent);
        finish();
    }

    private void startWizardActivity(Bundle args) {
        Intent intent = new Intent();
        intent.setClass(NASListActivity.this, WizardActivity.class);
        intent.putExtra("Hostname", args.getString("hostname"));
        intent.putExtra("RemoteAccess", isRemoteAccess);
        startActivityForResult(intent, WizardActivity.REQUEST_CODE);
    }

    private void startFileManageActivity() {
        if (getCallingActivity() == null) {
            Intent intent = new Intent();
            intent.setClass(NASListActivity.this, FileManageActivity.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent();
            NASListActivity.this.setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void startNASListLoader() {
        if (isRemoteAccess) {
            Bundle args = new Bundle();
            args.putString("server", NASPref.getCloudServer(this));
            args.putString("token", NASPref.getCloudAuthToken(this));
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, args, this).forceLoad();
        } else
            getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
    }

    private void startP2PStatusLoader() {
        if(!isEnableTutkDeviceCheck){
            if (mProgressView.isShown())
                mProgressView.setVisibility(View.INVISIBLE);
            return;
        }
        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_ONLINE_CHECK, null, this).forceLoad();
    }

    private void startRemoteAccessDeleteLoader(Bundle args) {
        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_DELETE, args, this).forceLoad();
    }

    private void startLoginLoader(Bundle args) {
        getLoaderManager().restartLoader(LoaderID.LOGIN, args, this).forceLoad();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == WizardActivity.REQUEST_CODE) {
                Bundle args = data.getExtras();
                showLoginDialog(args, false);
            }
        }
    }


    /**
     * LOADER CONTROL
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        String server, token, nasId;
        switch (mLoaderID = id) {
            case LoaderID.NAS_LIST:
                mProgressView.setVisibility(View.VISIBLE);
                return new NASListLoader(this);
            case LoaderID.LOGIN:
                return new LoginLoader(this, args, true);
            case LoaderID.TUTK_NAS_LINK:
                mProgressView.setVisibility(View.VISIBLE);
                return new TutkLinkNasLoader(this, args);
            case LoaderID.TUTK_NAS_GET:
                mProgressView.setVisibility(View.VISIBLE);
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
            case LoaderID.TUTK_NAS_DELETE:
                if (mLoginDialog == null)
                    mProgressView.setVisibility(View.VISIBLE);
                server = args.getString("server");
                token = args.getString("token");
                nasId = args.getString("nasId");
                return new TutkDeleteNasLoader(this, server, token, nasId);
            case LoaderID.WIZARD:
                mProgressView.setVisibility(View.VISIBLE);
                return new WizardCheckLoader(this, args);
            case LoaderID.TUTK_NAS_ONLINE_CHECK:
                mProgressView.setVisibility(View.VISIBLE);
                return new P2PStautsLoader(NASListActivity.this, mNASList);
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
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        Log.w(TAG, "onLoaderReset: " + loader.getClass().getSimpleName());
    }

    private void checkNasListLoader(boolean success, NASListLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        mNASList = loader.getList();
        mAdapter.notifyDataSetChanged();
        mRecyclerEmtpyView.setVisibility((mNASList != null && mNASList.size() > 0) ? View.GONE : View.VISIBLE);
    }

    private void checkWizardLoader(boolean success, WizardCheckLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        Bundle args = loader.getBundleArgs();
        args.putString("Model", loader.getModel());
        if (!success) {
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        if (loader.isWizard()) {
            showLoginDialog(args, false);
        } else {
            if (isRemoteAccess)
                Toast.makeText(this, getString(R.string.wizard_remote_access_error), Toast.LENGTH_SHORT).show();
            else
                startWizardActivity(args);
        }
    }

    private void checkLoginLoader(boolean success, LoginLoader loader) {
        if (!success) {
            hideLoginDialog(false);
            Toast.makeText(this, ((LoginLoader) loader).getLoginError(), Toast.LENGTH_SHORT).show();
            return;
        }

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
                mRecyclerEmtpyView.setVisibility(View.GONE);
                startP2PStatusLoader();
                return;
            } else {
                mRecyclerEmtpyView.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
            if (code.equals(TutkCodeID.AUTH_FAIL)) {
                startSignInActivity();
            }
        }
        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void checkTutkDeleteNasLoader(boolean success, TutkDeleteNasLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        if (!success) {
            hideLoginDialog(false);
            Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
            return;
        }

        hideLoginDialog(true);
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
            mRecyclerEmtpyView.setVisibility((mNASList != null && mNASList.size() > 0) ? View.GONE : View.VISIBLE);
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
            if (code.equals(TutkCodeID.AUTH_FAIL)) {
                startSignInActivity();
            }
        }
    }

    private void checkTutkLinkNasLoader(boolean success, TutkLinkNasLoader loader) {
        if (!success) {
            mProgressView.setVisibility(View.INVISIBLE);
            Toast.makeText(this, loader.getError(), Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = loader.getBundleArgs();
        args.putString("hostname", loader.getP2PHostname());
        getLoaderManager().restartLoader(LoaderID.WIZARD, args, this).forceLoad();
    }

    private void checkP2PStatusLoader(boolean success, P2PStautsLoader loader) {
        mProgressView.setVisibility(View.INVISIBLE);
        if (!success) {
            return;
        }

        mAdapter.notifyDataSetChanged();
    }

    private void hideLoginDialog(boolean dismiss) {
        if (mLoginDialog != null) {
            if (isRemoteAccess)
                P2PService.getInstance().stopP2PConnect();

            if (dismiss) {
                mLoginDialog.dismiss();
                mLoginDialog = null;
            } else
                mLoginDialog.hideProgress();
        }
    }

    private void showLoginDialog(Bundle args, boolean delete) {
        mLoginDialog = new LoginDialog(NASListActivity.this, args, isRemoteAccess, delete) {
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
            LayoutInflater layoutInflater = LayoutInflater.from(NASListActivity.this);
            View view = layoutInflater.inflate(R.layout.listitem_nas_finder, parent, false);
            ViewHolder holder = new ViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
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

            if (isEnableTutkDeviceCheck && isRemoteAccess) {
                String isOnLine = mNASList.get(position).get("online");
                if (isOnLine != null && !isOnLine.equals("")) {
                    if (isOnLine.equals("yes")) {
                        holder.subtitle.setText(getString(R.string.online));
                        holder.subtitle.setTextColor(Color.GREEN);
                    } else {
                        holder.subtitle.setText(getString(R.string.offline));
                        holder.subtitle.setTextColor(ContextCompat.getColor(NASListActivity.this, R.color.textColorSecondary));
                    }
                } else {
                    holder.subtitle.setTextColor(ContextCompat.getColor(NASListActivity.this, R.color.textColorSecondary));
                    holder.subtitle.setText(getString(R.string.loading));
                }
            }
        }

        @Override
        public int getItemCount() {
            int size = 0;
            if (mNASList != null)
                size = mNASList.size();
            return size;
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            ImageView icon;
            TextView title;
            TextView subtitle;
            ImageView delete;
            ImageView next;

            public ViewHolder(View itemView) {
                super(itemView);
                icon = (ImageView) itemView.findViewById(R.id.listitem_nas_finder_icon);
                title = (TextView) itemView.findViewById(R.id.listitem_nas_finder_title);
                subtitle = (TextView) itemView.findViewById(R.id.listitem_nas_finder_subtitle);
                next = (ImageView) itemView.findViewById(R.id.listitem_nas_next_icon);
                next.setVisibility(isRemoteAccess ? View.INVISIBLE : View.VISIBLE);
                delete = (ImageView) itemView.findViewById(R.id.listitem_nas_delete_icon);
                delete.setVisibility(isRemoteAccess ? View.VISIBLE : View.INVISIBLE);
                delete.setOnClickListener(this);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int pos = getAdapterPosition();
                HashMap<String, String> nas = mNASList.get(pos);
                Bundle args = new Bundle();

                if (v.getId() == R.id.listitem_nas_delete_icon) {
                    args.putString("server", NASPref.getCloudServer(NASListActivity.this));
                    args.putString("token", NASPref.getCloudAuthToken(NASListActivity.this));
                    args.putString("nasId", nas.get("nasId"));
                    showNotificationDialog(args);
                } else {
                    String isOnLine = nas.get("online");
                    if (isRemoteAccess && isEnableTutkDeviceCheck && (isOnLine == null || "no".equals(isOnLine))) {
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
                    args.putString("username", NASPref.getUsername(NASListActivity.this));
                    args.putString("password", NASPref.getPassword(NASListActivity.this));
                    args.putBoolean("RemoteAccess", true);
                    getLoaderManager().restartLoader(isRemoteAccess ? LoaderID.TUTK_NAS_LINK : LoaderID.WIZARD, args, NASListActivity.this).forceLoad();
                }
            }
        }
    }

}
