package com.transcend.nas.connection;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.DividerItemDecoration;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.common.TutkCodeID;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.TutkDeleteNasLoader;
import com.transcend.nas.management.TutkGetNasLoader;
import com.transcend.nas.management.TutkLinkNasLoader;
import com.tutk.IOTC.P2PService;

import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.HashMap;

public class NASFinderActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {

    private static final String TAG = NASFinderActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mAdapter;
    private RelativeLayout mProgressView;
    private LoginDialog mLoginDialog;
    private boolean isRemoteAccess = false;
    private int mLoaderID;

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
                startSignInActivity();
                break;
            case R.id.action_refresh_nas_finder:
                startNASListLoader();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mLoginDialog != null) {
            getLoaderManager().destroyLoader(mLoaderID);
            mLoginDialog.dismiss();
            mLoginDialog = null;
        } else if (mProgressView.isShown()) {
            mProgressView.setVisibility(View.INVISIBLE);
            getLoaderManager().destroyLoader(mLoaderID);
        } else {
            startSignInActivity();
        }
    }


    /**
     * INITIALIZATION
     */
    private void initData() {
        Intent intent = getIntent();
        isRemoteAccess = (boolean) intent.getBooleanExtra("RemoteAccess", false);
        mNASList = (ArrayList<HashMap<String, String>>) intent.getSerializableExtra("NASList");
        if (mNASList == null)
            mNASList = new ArrayList<HashMap<String, String>>();
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.nas_finder_toolbar);
        mToolbar.setTitle("");
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
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout) findViewById(R.id.nas_finder_progress_view);
    }

    private void reloadNASListIfNotFound() {
        if (mNASList.size() == 0) {
            startNASListLoader();
        }
    }


    /**
     * ACTIVITY LAUNCHER
     */
    private void startSignInActivity() {
        Intent intent = new Intent();
        intent.setClass(NASFinderActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    private void startFileManageActivity() {
        Intent intent = new Intent();
        intent.setClass(NASFinderActivity.this, FileManageActivity.class);
        startActivity(intent);
        finish();
    }

    private void startNASListLoader() {
        if (isRemoteAccess) {
            Bundle args = new Bundle();
            args.putString("server", NASPref.getCloudServer(this));
            args.putString("token", NASPref.getCloudAuthToken(this));
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_GET, args, this).forceLoad();
        }
        else
            getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
    }

    private void startRemoteAccessDeleteLoader(Bundle args) {
        getLoaderManager().restartLoader(LoaderID.TUTK_NAS_DELETE, args, this).forceLoad();
    }

    private void startLoginLoader(Bundle args) {
        if(isRemoteAccess){
            getLoaderManager().restartLoader(LoaderID.TUTK_NAS_LINK, args, this).forceLoad();
        }
        else
            getLoaderManager().restartLoader(LoaderID.LOGIN, args, this).forceLoad();
    }


    /**
     * LOADER CONTROL
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        String server,token, nasId;
        switch (mLoaderID = id) {
            case LoaderID.NAS_LIST:
                mProgressView.setVisibility(View.VISIBLE);
                return new NASListLoader(this);
            case LoaderID.LOGIN:
                return new LoginLoader(this, args);
            case LoaderID.TUTK_NAS_LINK:
                return new TutkLinkNasLoader(this, args);
            case LoaderID.TUTK_NAS_GET:
                mProgressView.setVisibility(View.VISIBLE);
                server = args.getString("server");
                token = args.getString("token");
                return new TutkGetNasLoader(this, server, token);
            case LoaderID.TUTK_NAS_DELETE:
                server = args.getString("server");
                token = args.getString("token");
                nasId = args.getString("nasId");
                return new TutkDeleteNasLoader(this, server, token, nasId);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof NASListLoader) {
            mProgressView.setVisibility(View.INVISIBLE);
            mNASList = ((NASListLoader) loader).getList();
            mAdapter.notifyDataSetChanged();
        } else if (loader instanceof LoginLoader) {
            if (mLoginDialog != null)
                mLoginDialog.hideProgress();
            if (success)
                startFileManageActivity();
        } else if(loader instanceof TutkGetNasLoader){
            mProgressView.setVisibility(View.INVISIBLE);
            if(!success){
                Toast.makeText(this, getString(R.string.network_error),Toast.LENGTH_SHORT).show();
                return;
            }

            TutkGetNasLoader listLoader = (TutkGetNasLoader) loader;
            String code = listLoader.getCode();
            String status = listLoader.getStatus();

            if (code.equals("")) {
                mNASList = listLoader.getNasArrayList();
                mAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
                if(code.equals(TutkCodeID.AUTH_FAIL)){
                    startSignInActivity();
                }
            }
        } else if(loader instanceof TutkDeleteNasLoader){
            if(!success){
                if (mLoginDialog != null)
                    mLoginDialog.hideProgress();
                Toast.makeText(this, getString(R.string.network_error),Toast.LENGTH_SHORT).show();
                return;
            }

            TutkDeleteNasLoader deleteLoader = (TutkDeleteNasLoader) loader;
            String code = deleteLoader.getCode();
            String status = deleteLoader.getStatus();
            if (mLoginDialog != null) {
                mLoginDialog.dismiss();
                mLoginDialog = null;
            }

            if (code.equals(TutkCodeID.SUCCESS)) {
                String id = deleteLoader.getNasID();
                for(HashMap<String, String> nas : mNASList){
                    if(id.equals(nas.get("nasId"))){
                        mNASList.remove(nas);
                        break;
                    }
                }
                mAdapter.notifyDataSetChanged();
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, code + " : " + status, Toast.LENGTH_SHORT).show();
                if(code.equals(TutkCodeID.AUTH_FAIL)){
                    startSignInActivity();
                }
            }
        } else if(loader instanceof  TutkLinkNasLoader){
            TutkLinkNasLoader linkLoader = (TutkLinkNasLoader) loader;
            String code = linkLoader.getCode();
            String status = linkLoader.getStatus();

            if(!success){
                Toast.makeText(this, linkLoader.getError(), Toast.LENGTH_SHORT).show();
                if (mLoginDialog != null)
                    mLoginDialog.hideProgress();
                return;
            }

            Bundle args = linkLoader.getBundleArgs();
            String ip = P2PService.getInstance().getP2PIP();
            int port = P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
            args.putString("hostname",ip+":"+port);
            getLoaderManager().restartLoader(LoaderID.LOGIN, args, this).forceLoad();
        }

    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        Log.w(TAG, "onLoaderReset: " + loader.getClass().getSimpleName());
    }


    /**
     * RECYCLER VIEW ADAPTER
     */
    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(NASFinderActivity.this);
            View view = layoutInflater.inflate(R.layout.listitem_nas_finder, parent, false);
            ViewHolder holder = new ViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String nickname = mNASList.get(position).get("nickname");
            String hostname = mNASList.get(position).get("hostname");
            holder.title.setText(nickname);
            holder.subtitle.setText(hostname);
        }

        @Override
        public int getItemCount() {
            return mNASList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            ImageView icon;
            TextView title;
            TextView subtitle;

            public ViewHolder(View itemView) {
                super(itemView);
                icon = (ImageView) itemView.findViewById(R.id.listitem_nas_finder_icon);
                title = (TextView) itemView.findViewById(R.id.listitem_nas_finder_title);
                subtitle = (TextView) itemView.findViewById(R.id.listitem_nas_finder_subtitle);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int pos = getAdapterPosition();
                HashMap<String, String> nas = mNASList.get(pos);
                Bundle args = new Bundle();
                args.putString("nasId", nas.get("nasId"));
                args.putString("nickname", nas.get("nickname"));
                args.putString("hostname", nas.get("hostname"));
                args.putString("username", "admin");
                args.putString("password", "Realtek");
                mLoginDialog = new LoginDialog(NASFinderActivity.this, args, isRemoteAccess) {
                    @Override
                    public void onConfirm(Bundle args) {
                        startLoginLoader(args);
                    }

                    @Override
                    public void onDelete(Bundle args){
                        startRemoteAccessDeleteLoader(args);
                    }

                    @Override
                    public void onCancel() {
                        getLoaderManager().destroyLoader(mLoaderID);
                        mLoginDialog.dismiss();
                        mLoginDialog = null;
                    }
                };
            }
        }
    }

}
