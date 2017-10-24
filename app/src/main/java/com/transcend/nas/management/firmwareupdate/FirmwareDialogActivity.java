package com.transcend.nas.management.firmwareupdate;

import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.transcend.nas.LoaderID;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.common.ManageFactory;
import com.transcend.nas.connection.LoginActivityNew;
import com.transcend.nas.tutk.TutkLogoutLoader;

/**
 * Created by steve_su on 2017/6/29.
 */

public class FirmwareDialogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Boolean> {
    private static final String TAG = FirmwareDialogActivity.class.getSimpleName();
    static final String UPDATING = "updating";
    static final String FAILED = "failed";
    static final String SUCCESS = "success";
    static final String PROGRESS = "progress";
    private Context mContext;
    private AlertDialog mUpdatingDialog;
    private AlertDialog mSuccessDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        showDialog(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showDialog(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NASApp app = (NASApp) this.getApplicationContext();
        app.mIsInBackground = false;
    }

    @Override
    protected void onStop() {
        NASApp app = (NASApp) this.getApplicationContext();
        app.mIsInBackground = true;
        super.onStop();
    }

    @Override
    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LoaderID.TUTK_LOGOUT:
                return new TutkLogoutLoader(this);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Boolean> loader, Boolean isSuccess) {
        if (loader instanceof TutkLogoutLoader) {
            if (mSuccessDialog != null) {
                mSuccessDialog.dismiss();
            }

            if (mUpdatingDialog != null) {
                mUpdatingDialog.dismiss();
            }

            startSignInActivity();
        }
    }

    @Override
    public void onLoaderReset(Loader<Boolean> loader) {

    }

    private void showDialog(Intent intent) {
        String type = intent.getStringExtra("dialog_type");
        switch (type) {
            case UPDATING:
                mUpdatingDialog = showFirmwareUpdating();
                break;
            case FAILED:
                showUpdateFailed();
                break;
            case SUCCESS:
                showUpdateSuccess();
                break;
            case PROGRESS:
                String percentage = intent.getStringExtra("percentage");
                updateProgress(percentage);
                break;
        }
    }

    private AlertDialog showFirmwareUpdating() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_firmware_title_updating));
        builder.setView(R.layout.dialog_firmware_updating);
        builder.setCancelable(false);
        return builder.show();
    }

    private void showUpdateFailed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_firmware_title_notify));
        builder.setMessage(R.string.dialog_firmware_message_fail);
        builder.setPositiveButton(R.string.confirm, null);
        builder.setNegativeButton(R.string.wizard_try, null);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.show();
        Button confirm = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (mUpdatingDialog != null) {
                    mUpdatingDialog.dismiss();
                }
                stopUpdateService();
                finish();
            }
        });
        Button retry = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NASUtils.startP2PService(mContext);
                dialog.dismiss();
                if (mUpdatingDialog != null) {
                    mUpdatingDialog.dismiss();
                }
                startService(new Intent(mContext, FirmwareUpdateService.class));
            }
        });

    }

    private void showUpdateSuccess() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_firmware_title_notify));
        builder.setMessage(R.string.dialog_firmware_message_success);
        builder.setPositiveButton(R.string.confirm, null);
        builder.setCancelable(false);
        mSuccessDialog = builder.show();
        Button posBtn = mSuccessDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        posBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                getLoaderManager().restartLoader(LoaderID.TUTK_LOGOUT, args, FirmwareDialogActivity.this).forceLoad();
            }
        });
    }

    private void updateProgress(String percentage) {
        if (mUpdatingDialog == null || TextUtils.isEmpty(percentage)) {
            return;
        }
        TextView p = (TextView) mUpdatingDialog.findViewById(R.id.percentage);
        ProgressBar progressBar = (ProgressBar) mUpdatingDialog.findViewById(R.id.progress_bar);
        if (p != null) {
            p.setText(percentage.concat("%"));
        }
        if (progressBar != null) {
            progressBar.setProgress(Integer.valueOf(percentage));
        }
    }

    private void stopUpdateService() {
        if (ManageFactory.isServiceRunning(mContext, FirmwareUpdateService.class)) {
            Intent intent = new Intent(mContext, FirmwareUpdateService.class);
            stopService(intent);
        }
    }

    private void startSignInActivity() {
        NASUtils.clearAfterLogout(this.getApplicationContext());

        //show SignIn activity
        Intent intent = new Intent();
        intent.setClass(this, LoginActivityNew.class);
        startActivity(intent);
        finishAffinity();
    }

}
