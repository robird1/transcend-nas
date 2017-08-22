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
            doErrorHandling(statusLoader);
            return;
        }

        if (!isProcessFinished(statusLoader.getPercentage())) {
            showDialog(FirmwareDialogActivity.PROGRESS, statusLoader.getPercentage());
            requestStatus(statusLoader);

        } else {
            showDialog(FirmwareDialogActivity.PROGRESS, "99");
            startUpdateLoader(statusLoader);
        }

    }

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
        if (msg == null) {
            showDialog(FirmwareDialogActivity.FAILED);
            return;
        }

        boolean isP2PSuccess = "".equals(msg);
        if (isP2PSuccess) {
            startStatusLoader(loader);
        } else {
            boolean isNoNetwork = msg.equals(mContext.getString(R.string.network_error));
            if (isNoNetwork) {
                requestStatus(loader);
            } else {
                showDialog(FirmwareDialogActivity.FAILED);
            }
        }
    }

    private void startUpdateLoader(FirmwareStatusLoader statusLoader) {
        FirmwareUpgradeLoader updateLoader = new FirmwareUpgradeLoader(mContext, statusLoader.getData());
        boolean isSuccess = updateLoader.loadInBackground();
        if (isSuccess) {
            showDialog(FirmwareDialogActivity.SUCCESS);
            stopSelf();
        } else {
            showDialog(FirmwareDialogActivity.FAILED);
        }
    }

    private boolean reLogin() {
        String msg = NASUtils.startP2PService(mContext);
        boolean isP2PSuccess = "".equals(msg);
        if (isP2PSuccess) {
            Bundle bundle = new Bundle();
            bundle.putString("hostname", getIP());
            bundle.putString("username", NASPref.getUsername(mContext));
            bundle.putString("password", NASPref.getPassword(mContext));
            new LoginLoader(mContext, bundle).loadInBackground();

            return true;
        } else {
            return false;
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

    private String getIP() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
    }

}
