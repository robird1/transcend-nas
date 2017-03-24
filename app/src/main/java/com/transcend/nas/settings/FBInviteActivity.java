package com.transcend.nas.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;
import com.transcend.nas.NASPref;

/**
 * Created by steve_su on 2017/2/17.
 */

public class FBInviteActivity extends AppCompatActivity {
    private static String TAG = FBInviteActivity.class.getSimpleName();
    private CallbackManager mCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerFBCallback();
        showInviteDialog();
    }

    private void registerFBCallback() {
        mCallbackManager = CallbackManager.Factory.create();
    }

    private void showInviteDialog() {
        mCallbackManager = CallbackManager.Factory.create();

        AppInviteDialog invititeDialog = new AppInviteDialog(this);
        invititeDialog.registerCallback(mCallbackManager,
                new FacebookCallback<AppInviteDialog.Result>() {

                    @Override
                    public void onSuccess(AppInviteDialog.Result result) {
                        Log.d(TAG, "[Enter] onSuccess");
                        Bundle data = result.getData();
                        if (data == null) {
                            Log.d(TAG, "data == null");

                        } else {
                            Log.d(TAG, "data != null");

                        }
                        finish();
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "[Enter] onCancel");
                        finish();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        Log.d(TAG, "[Enter] onError: " + exception.getMessage());
                        finish();
                    }
                });

        String uuid = NASPref.getCloudUUID(getApplicationContext());
        String nasId = NASPref.getCloudNasID(getApplicationContext());
        String nickName = NASPref.getCloudNickName(getApplicationContext());
        Log.d(TAG, "uuid: "+ uuid);
        Log.d(TAG, "nasId: "+ nasId);
        Log.d(TAG, "nickName: "+ nickName);
        String userName = NASPref.getUsername(this);
        String password = NASPref.getPassword(this);

        String appLinkUrl = "https://s3-ap-northeast-1.amazonaws.com/appinvite.storejetcloud.com/fb_invite_test.html?uuid="
                + uuid+ "&nasId="+ nasId+ "&nickName="+ nickName+ "&username="+ userName+ "&password="+ password;

        Log.d(TAG, "appLinkUrl: "+ appLinkUrl);

        if (AppInviteDialog.canShow()) {
            String url = "https://s3-ap-northeast-1.amazonaws.com/appinvite.storejetcloud.com/SJC-banner.jpg";
            AppInviteContent content = new AppInviteContent.Builder()
                    .setApplinkUrl(appLinkUrl).setPreviewImageUrl(url).build();
            AppInviteDialog.show(this, content);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CallbackManagerImpl.RequestCodeOffset.AppInvite.toRequestCode()) {
                mCallbackManager.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

}
