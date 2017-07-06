package com.transcend.nas.management.firmwareupdate;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.connection.LoginLoader;
import com.tutk.IOTC.P2PService;

/**
 * Created by steve_su on 2017/6/28.
 */

public class FirmwareUpdateService extends Service {
    private static final String TAG = FirmwareUpdateService.class.getSimpleName();
    private static final long QUERY_STATUS_INTERVAL = 5000;
    private Context mContext;
    private Handler mHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        initHandler();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "[Enter] onDestroy");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showDialog(FirmwareDialogActivity.UPDATING);
        requestFirmwareDownload();
        return super.onStartCommand(intent, flags, startId);
    }

    private void requestFirmwareDownload() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                FirmwareDownloadLoader loader = new FirmwareDownloadLoader(mContext);
                boolean isSuccess = loader.loadInBackground();
                if (isSuccess) {
                    startStatusLoader(loader);
                } else {
                    Log.d(TAG, "FAIL in FirmwareDownloadLoader=======================================");
                    if (!loader.isHashValid()) {
                        boolean result = reLogin();
                        if (result) {
                            requestFirmwareDownload();
                        } else {
                            showDialog(FirmwareDialogActivity.FAILED);
                        }
                        return;
                    }
                    showDialog(FirmwareDialogActivity.FAILED);
                }
            }
        });
    }

    private void startStatusLoader(final FirmwareLoader loader) {
        FirmwareStatusLoader statusLoader = new FirmwareStatusLoader(mContext, loader.getData());
        boolean isSuccess = statusLoader.loadInBackground();
        if (!isSuccess) {
//            doErrorHandling(loader, statusLoader.getReturnCode());
            doErrorHandling(statusLoader);
            return;
        }

        Log.d(TAG, "getReturnCode: "+ statusLoader.getReturnCode()+ " getPercentage: "+ statusLoader.getPercentage());

        if (!isProcessFinished(statusLoader.getPercentage())) {
            showDialog(FirmwareDialogActivity.PROGRESS, statusLoader.getPercentage());
//            requestStatus(loader);
            requestStatus(statusLoader);

        } else {
            Log.d(TAG, "[Enter] isProcessFinished");
            showDialog(FirmwareDialogActivity.PROGRESS, "99");
            startUpdateLoader(statusLoader);
        }

    }

    private boolean reLogin() {
        Log.d(TAG, "[Enter] reLogin");
        String msg = NASUtils.startP2PService(mContext);
        boolean isP2PSuccess = "".equals(msg);
        if (isP2PSuccess) {
            Log.d(TAG, "[Enter] isP2PSuccess");

            Server server = ServerManager.INSTANCE.getCurrentServer();
            String ip = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);

            Bundle bundle = new Bundle();
            bundle.putString("hostname", ip);
            bundle.putString("username", NASPref.getUsername(mContext));
            bundle.putString("password", NASPref.getPassword(mContext));
            new LoginLoader(mContext, bundle, true).loadInBackground();

            return true;
        } else {

            return false;
        }
    }

//    private void doErrorHandling(FirmwareLoader loader, String returnCode) {
private void doErrorHandling(FirmwareStatusLoader loader) {
    if (isUnknownError(loader.getReturnCode())) {
            showDialog(FirmwareDialogActivity.FAILED);
            return;
        }

        if (!loader.isHashValid()) {
            boolean isSuccess = reLogin();
            if (isSuccess) {
                startStatusLoader(loader);
            } else {
                showDialog(FirmwareDialogActivity.FAILED);
            }
            return;
        }

        String msg = NASUtils.startP2PService(mContext);
        Log.d(TAG, "[Enter] doErrorHandling msg: "+ msg);
        if (msg == null) {
            showDialog(FirmwareDialogActivity.FAILED);
            return;
        }

        boolean isP2PSuccess = "".equals(msg);
        if (isP2PSuccess) {
            Log.d(TAG, "[Enter] isP2PSuccess");
            startStatusLoader(loader);
        } else {
            boolean isNoNetwork = msg.equals(mContext.getString(R.string.network_error));
            if (isNoNetwork) {
                Log.d(TAG, "[Enter] isNoNetwork");
                requestStatus(loader);
            } else {
                Log.d(TAG, "[Enter] FAILED");
                showDialog(FirmwareDialogActivity.FAILED);
            }
        }
    }

    private void startUpdateLoader(FirmwareStatusLoader statusLoader) {
        Log.d(TAG, "[Enter] startUpdateLoader");
        FirmwareUpgradeLoader updateLoader = new FirmwareUpgradeLoader(mContext, statusLoader.getData());
        boolean isSuccess = updateLoader.loadInBackground();
        if (isSuccess) {
            showDialog(FirmwareDialogActivity.SUCCESS);
            stopSelf();
        } else {
            Log.d(TAG, "FAIL in startUpdateLoader=======================================");
            showDialog(FirmwareDialogActivity.FAILED);
        }
    }

    private void requestStatus(final FirmwareLoader loader) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startStatusLoader(loader);
            }
        }, QUERY_STATUS_INTERVAL);
    }

    private void initHandler() {
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());
    }

    private void showDialog(String type) {
        showDialog(type, null);
    }

    private void showDialog(String type, String percentage) {
        Intent i = new Intent(mContext, FirmwareDialogActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("dialog_type", type);
        if (!TextUtils.isEmpty(percentage)) {
            i.putExtra("percentage", percentage);
        }

        if (type.equals(FirmwareDialogActivity.PROGRESS)) {
            NASApp app = (NASApp) this.getApplicationContext();
            if (app.mIsInBackground)
                return;
        }
        startActivity(i);
    }

    private boolean isProcessFinished(String percentage) {
        return "100".equals(percentage);
    }

    private boolean isUnknownError(String returnCode) {
        return "1".equals(returnCode);
    }

}
