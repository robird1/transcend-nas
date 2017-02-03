package com.transcend.nas.management.externalstorage;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.transcend.nas.DrawerMenuActivity;
import com.transcend.nas.NASApp;
import com.transcend.nas.R;

import static com.transcend.nas.NASUtils.getSDLocation;

/**
 * Created by steve_su on 2016/12/26.
 */

public class ExternalStorageKitKat extends AbstractExternalStorage {
    public ExternalStorageKitKat(Context context) {
        super(context);
    }

    @Override
    protected void onNavigationItemSelected(DrawerMenuActivity activity, int itemId) {
        NASApp.ROOT_SD = getSDLocation(getContext());
        activity.startFileManageActivity(itemId);
    }

    @Override
    public void handleWriteOperationFailed() {
        new AlertDialog.Builder(getContext()).setTitle("Warning").setMessage(R.string.dialog_write_operation_not_allowed)
                .setPositiveButton(R.string.confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                .setCancelable(false)
                .show();
    }

}
