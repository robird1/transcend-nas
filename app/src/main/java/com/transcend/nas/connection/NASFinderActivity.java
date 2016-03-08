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

import com.transcend.nas.R;
import com.transcend.nas.common.DividerItemDecoration;
import com.transcend.nas.common.LoaderID;
import com.transcend.nas.management.FileManageActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class NASFinderActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {

    private static final String TAG = NASFinderActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mAdapter;
    private RelativeLayout mProgressView;
    private LoginDialog mLoginDialog;

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
            getLoaderManager().destroyLoader(LoaderID.LOGIN);
            mLoginDialog.dismiss();
            mLoginDialog = null;
        } else
        if (mProgressView.isShown()) {
            mProgressView.setVisibility(View.INVISIBLE);
            getLoaderManager().destroyLoader(LoaderID.NAS_LIST);
        }
        else {
            startSignInActivity();
        }
    }


    /**
     *
     * INITIALIZATION
     *
     */
    private void initData() {
        mNASList = (ArrayList<HashMap<String, String>>)getIntent().getSerializableExtra("NASList");
        if (mNASList == null) mNASList = new ArrayList<HashMap<String, String>>();
    }

    private void initToolbar() {
        mToolbar = (Toolbar)findViewById(R.id.nas_finder_toolbar);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initRecyclerView() {
        mRecyclerView = (RecyclerView)findViewById(R.id.nas_finder_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter = new RecyclerViewAdapter());
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
    }

    private void initProgressView() {
        mProgressView = (RelativeLayout)findViewById(R.id.nas_finder_progress_view);
    }

    private void reloadNASListIfNotFound() {
        if (mNASList.size() == 0)
            startNASListLoader();
    }


    /**
     *
     * ACTIVITY LAUNCHER
     *
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
        getLoaderManager().restartLoader(LoaderID.NAS_LIST, null, this).forceLoad();
    }

    private void startLoginLoader(Bundle args) {
        getLoaderManager().restartLoader(LoaderID.LOGIN, args, this).forceLoad();
    }


    /**
     *
     * LOADER CONTROL
     *
     */
    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LoaderID.NAS_LIST:
                mProgressView.setVisibility(View.VISIBLE);
                return new NASListLoader(this);
            case LoaderID.LOGIN:
                return new LoginLoader(this, args);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (loader instanceof NASListLoader) {
            mProgressView.setVisibility(View.INVISIBLE);
            mNASList = ((NASListLoader)loader).getList();
            /*
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("name", "MyNAS");
            hashMap.put("addr", "0.0.0.0");
            ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
            list.add(hashMap);
            mNASList = list;
            */
            mAdapter.notifyDataSetChanged();
        }
        if (loader instanceof LoginLoader) {
            if (mLoginDialog != null)
                mLoginDialog.hideProgress();
            if (success)
                startFileManageActivity();
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {
        Log.w(TAG, "onLoaderReset: " + loader.getClass().getSimpleName());
    }


    /**
     *
     * RECYCLER VIEW ADAPTER
     *
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
                icon = (ImageView)itemView.findViewById(R.id.listitem_nas_finder_icon);
                title = (TextView)itemView.findViewById(R.id.listitem_nas_finder_title);
                subtitle = (TextView)itemView.findViewById(R.id.listitem_nas_finder_subtitle);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int pos = getAdapterPosition();
                HashMap<String, String > nas = mNASList.get(pos);
                Bundle args = new Bundle();
                args.putString("nickname", nas.get("nickname"));
                args.putString("hostname", nas.get("hostname"));
                args.putString("username", "admin");
                args.putString("password", "Realtek");
                mLoginDialog = new LoginDialog(NASFinderActivity.this, args) {
                    @Override
                    public void onConfirm(Bundle args) {
                        startLoginLoader(args);
                    }
                    @Override
                    public void onCancel() {
                        getLoaderManager().destroyLoader(LoaderID.LOGIN);
                        mLoginDialog.dismiss();
                        mLoginDialog = null;
                    }
                };
            }
        }
    }

}
