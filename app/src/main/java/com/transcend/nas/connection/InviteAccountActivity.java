package com.transcend.nas.connection;

import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.LoaderID;
import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.settings.FBInviteActivity;
import com.tutk.IOTC.P2PService;

import java.util.ArrayList;

/**
 * Created by steve_su on 2017/4/12.
 */

public class InviteAccountActivity extends AppCompatActivity implements View.OnClickListener, LoaderManager.LoaderCallbacks<Boolean>{
    private static final String TAG = InviteAccountActivity.class.getSimpleName();
    private Context mContext;
    private RecyclerView mRecyclerView;
    private RelativeLayout mProgressView;
    private RelativeLayout mDialogProgressView;
    private ListAdapter mListAdapter;
    private ArrayList<String> mAccountList;
    private AlertDialog mDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_invite_account);
        mRecyclerView = (RecyclerView) findViewById(R.id.account_list);
        mProgressView = (RelativeLayout) findViewById(R.id.progress_view);

        startAccountListLoader();

        Button okButton = (Button) findViewById(R.id.button_ok);
        okButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_ok:
                showPasswordDialog();
                break;
        }
    }

    private void startAccountListLoader() {
        Log.d(TAG, "[Enter] startAccountListLoader");
        getLoaderManager().restartLoader(LoaderID.INVITE_NAS_ACCOUNT, null, this).forceLoad();
    }

    private void checkPasswordLoader(String input) {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String ip = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        Bundle data = new Bundle();
        data.putString("hostname", ip);
        data.putString("username", mAccountList.get(mListAdapter.mLastSelected));
        data.putString("password", input);
        getLoaderManager().restartLoader(LoaderID.LOGIN, data, this).forceLoad();
    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(NASPref.getCloudNickName(mContext));
        builder.setView(R.layout.dialog_login);
        builder.setCancelable(true);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.login, null);
        mDialog = builder.show();
        mDialog.setCanceledOnTouchOutside(false);
        initDialogProgressView();
        setDialogAccountText();
        setConfirmBtnListener();
    }

    private void initDialogProgressView() {
        mDialogProgressView = (RelativeLayout) mDialog.findViewById(R.id.dialog_login_progress_view);
    }

    private void setDialogAccountText() {
        TextView accountView = (TextView) mDialog.findViewById(R.id.dialog_login_account);
        accountView.setText(mAccountList.get(mListAdapter.mLastSelected));
        accountView.setEnabled(false);
    }

    private void setConfirmBtnListener() {
        Button dlgBtnPos = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        dlgBtnPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPasswordLoader(getDialogPasswordField().getText().toString());
            }
        });
    }

    private AppCompatEditText getDialogPasswordField() {
        return (AppCompatEditText) mDialog.findViewById(R.id.dialog_login_password);
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle bundle) {
        switch (id) {
            case LoaderID.INVITE_NAS_ACCOUNT:
                mProgressView.setVisibility(View.VISIBLE);
                return new InviteNASAccountLoader(this);
            case LoaderID.LOGIN:
                mDialogProgressView.setVisibility(View.VISIBLE);
                return new InviteLoginLoader(this, bundle);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean isSuccess) {
        mProgressView.setVisibility(View.INVISIBLE);
        if (loader instanceof InviteNASAccountLoader) {
            Log.d(TAG, "[Enter] loader instanceof InviteNASAccountLoader");
            Log.d(TAG, "isSuccess: "+ isSuccess);
            mAccountList = ((InviteNASAccountLoader) loader).getAccountList();
            Log.d(TAG, "mAccountList.size(): "+ mAccountList.size());

            mRecyclerView.setAdapter(mListAdapter = new ListAdapter(mAccountList));
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else if (loader instanceof InviteLoginLoader) {
            mDialogProgressView.setVisibility(View.INVISIBLE);
            if (isSuccess) {
                mDialog.dismiss();
                Intent intent = new Intent(this, FBInviteActivity.class);
                intent.putExtra("username", ((InviteLoginLoader) loader).getUserName());
                intent.putExtra("password", ((InviteLoginLoader) loader).getPassword());
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(mContext, getString(R.string.invite_wrong_password),Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private class ListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private ArrayList mAccountList;
        private int mLastSelected;
        private RadioButton mLastSelectedBtn;

        private ListAdapter(ArrayList<String> accountList) {
            mAccountList = accountList;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d(TAG, "[Enter] onCreateViewHolder");
            View view = LayoutInflater.from(mContext).inflate(R.layout.listitem_nas_account, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            Log.d(TAG, "[Enter] onBindViewHolder position: "+ position);

            ViewHolder myViewHolder = (ViewHolder) holder;
            myViewHolder.mAccountBtn.setText((String) mAccountList.get(position));
            myViewHolder.mAccountBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    RadioButton btn = (RadioButton) view;
                    if (position != mLastSelected) {
                        btn.setChecked(true);
                        mLastSelectedBtn.setChecked(false);
                    }

                    mLastSelected = position;
                    mLastSelectedBtn = btn;
                }
            });

            if (position == 0) {
                myViewHolder.mAccountBtn.setChecked(true);
                mLastSelected = 0;
                mLastSelectedBtn = myViewHolder.mAccountBtn;
            }

        }

        @Override
        public int getItemCount() {
            Log.d(TAG, "[Enter] getItemCount");
            return mAccountList.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder{
            RadioButton mAccountBtn;

            ViewHolder(View itemView){
                super(itemView);
                mAccountBtn = (RadioButton) itemView.findViewById(R.id.account_title);
            }
        }

    }

}