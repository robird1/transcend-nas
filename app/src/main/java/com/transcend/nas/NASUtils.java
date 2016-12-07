package com.transcend.nas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.connection.LoginHelper;
import com.transcend.nas.connection.LoginListActivity;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

/**
 * Created by steve_su on 2016/12/2.
 */

public final class NASUtils {
    private static final String TAG = NASUtils.class.getSimpleName();

    private NASUtils() {}


    public static String getCacheFilesLocation(Context context) {
        return context.getExternalCacheDir().getPath();
    }

    public static void clearDataAfterLogout(Context context) {
        clearDatabaseData(context);

        if (NASPref.getFBAccountStatus(context)) {
            NASPref.setCloudUsername(context, "");
            NASPref.setFBAccountStatus(context, false);
        }

        NASPref.setHostname(context, "");
        NASPref.setPassword(context, "");
        NASPref.setUUID(context, "");
        NASPref.setMacAddress(context, "");
        NASPref.setSerialNum(context, "");
        NASPref.setCloudPassword(context, "");
        NASPref.setCloudAuthToken(context, "");
        NASPref.setCloudAccountStatus(context, NASPref.Status.Inactive.ordinal());
        NASPref.setCloudUUID(context, "");
        NASPref.setBackupScenario(context, false);
        NASPref.setBackupSetting(context, false);
        NASPref.setBackupLocation(context, "/homes/" + Build.MODEL + "/");
        NASPref.setBackupSource(context, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
    }

    private static void clearDatabaseData(Context context) {
        LoginHelper loginHelper = new LoginHelper(context);
        LoginHelper.LoginInfo account = new LoginHelper.LoginInfo();
        account.email = NASPref.getCloudUsername(context);
        account.uuid = NASPref.getCloudUUID(context);
        loginHelper.deleteAccount(account);
        loginHelper.onDestroy();
    }

    public static void logOutFB(Context context) {
        if (AccessToken.getCurrentAccessToken() != null) {
            new GraphRequest(AccessToken.getCurrentAccessToken(), "/me/permissions/", null, HttpMethod.DELETE, new GraphRequest
                    .Callback() {
                @Override
                public void onCompleted(GraphResponse graphResponse) {

                    LoginManager.getInstance().logOut();

                    Log.d(TAG, "LoginManager.getInstance().logOut()");

                }
            }).executeAsync();
        }

        NASPref.setFBProfilePhotoUrl(context, null);
    }

    public static String readFromAssets(Context context, String filename) {

        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(context.getAssets().open(filename)));
            String mLine = reader.readLine();
            while (mLine != null) {
                if (mLine.endsWith(".") && mLine.length() < 30)
                    sb.append(String.format("<h3>%s</h3>", mLine));
                else {
                    sb.append(String.format("<p>%s</p>", mLine));
                }
                sb.append(System.getProperty("line.separator"));
                mLine = reader.readLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }

    public static HttpEntity sendGetRequest() {
        HttpEntity entity = null;
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        String commandURL = "http://" + hostname + "/nas/get/info";
        Log.d(TAG, commandURL);

        HttpResponse response;
        try {
            HttpGet httpGet = new HttpGet(commandURL);
            response = HttpClientManager.getClient().execute(httpGet);

            if (response != null) {
                entity = response.getEntity();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return entity;
    }

    /**
     * Display the progress bar of the R.layout.activity_settings
     *
     * @param activity
     * @param isShow
     */
    public static void showProgressBar(@NonNull Activity activity, boolean isShow) {
        if (activity != null) {
            View progressBar = activity.findViewById(R.id.settings_progress_view);
            progressBar.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public static void showAppChooser(final Context context, final Uri fileUri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        Log.d(TAG, "extension: " + extension);
        Log.d(TAG, "mimeType: " + mimeType);

        intent.setDataAndType(fileUri, mimeType);

        try {
            Log.d(TAG, "decoded path"+ fileUri.getPath());
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(context).setTitle(R.string.open_with).setItems(getItemList(context),
                    getClickListener(context, fileUri, intent)).create().show();
        }
    }

    @NonNull
    private static String[] getItemList(Context context) {
        return new String[]{context.getResources().getString(R.string.file_format_document),
                        context.getResources().getString(R.string.file_format_image),
                        context.getResources().getString(R.string.file_format_video),
                                context.getResources().getString(R.string.file_format_audio)};
    }

    @NonNull
    private static DialogInterface.OnClickListener getClickListener(final Context context, final Uri fileUri, final Intent intent) {
        return new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which)
                {
                    case 0:
                        intent.setDataAndType(fileUri, "text/*");
                        break;
                    case 1:
                        intent.setDataAndType(fileUri, "image/*");
                        break;
                    case 2:
                        intent.setDataAndType(fileUri, "video/*");
                        break;
                    case 3:
                        intent.setDataAndType(fileUri, "audio/*");
                        break;
                    default:
                        intent.setDataAndType(fileUri, "text/*");
                        break;
                }

                context.startActivity(intent);

            }
        };
    }

}
