package com.transcend.nas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.connection.LoginHelper;
import com.transcend.nas.connection.LoginLoader;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.firmwareupdate.FirmwareUpdateService;
import com.transcend.nas.service.FileRecentManager;
import com.transcend.nas.utils.MimeUtil;
import com.tutk.IOTC.P2PService;
import com.tutk.IOTC.P2PTunnelAPIs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static android.support.v7.app.AlertDialog.Builder;

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
            NASPref.setFBUserName(context, "");
        }

        NASPref.setHostname(context, "");
        NASPref.setPassword(context, "");
        NASPref.setUUID(context, "");
        NASPref.setMacAddress(context, "");
        NASPref.setDeviceName(context, "");
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
        FileRecentManager.getInstance().deleteAction();
        LoginHelper loginHelper = new LoginHelper(context);
        LoginHelper.LoginInfo account = new LoginHelper.LoginInfo();
        account.email = NASPref.getCloudUsername(context);
        account.uuid = NASPref.getCloudUUID(context);
        loginHelper.deleteAccount(account);
        //loginHelper.onDestroy();
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

    public static List<File> getStoragePath(Context mContext) {
        //Log.d(TAG, "[Enter] getStoragePath()");
        List<File> stgList = new ArrayList<File>();
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        try {

            String[] paths = (String[]) mStorageManager.getClass().getMethod("getVolumePaths").invoke(mStorageManager);
            for (String p: paths) {
                String status = (String) mStorageManager.getClass().getMethod("getVolumeState", String.class).invoke(mStorageManager, p);
                if (Environment.MEDIA_MOUNTED.equals(status)) {
                    stgList.add(new File(p));
                }
            }
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        //for (File sd : stgList) {
        //    Log.d(TAG, "sd.getAbsolutePath(): "+ sd.getAbsolutePath());
        //}
        return stgList;
    }

    public static boolean isSDCardPath(Context context, String path) {
        String location = getSDLocation(context);
        return location != null && path.contains(location);
    }

    public static String getSDLocation(Context context) {
        List<File> stgList = NASUtils.getStoragePath(context);
        for (File sd : stgList) {
            if ((!sd.getAbsolutePath().contains(NASApp.ROOT_STG)) && (!sd.getAbsolutePath().toLowerCase().contains("usb"))) {
                //Log.d(TAG, "sd.getAbsolutePath(): "+ sd.getAbsolutePath());
                return sd.getAbsolutePath();
            }
        }
        return null;
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public static void showAppChooser(final Context context, final Uri fileUri) {
        Log.d(TAG, "[Enter] showAppChooser");
        final Intent intent = getIntentUri(context, fileUri);

        try {
            Log.d(TAG, "decoded path: "+ fileUri.getPath());
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "[Enter] ActivityNotFoundException");
            new AlertDialog.Builder(context).setTitle(R.string.open_with).setItems(getItemList(context),
                    getClickListener(context, fileUri, intent)).create().show();
        }
    }

    public static Intent getIntentUri(Context context, Uri uri) {
        context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        String type = MimeUtil.getMimeType(uri.getPath());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        intent.setDataAndType(uri, type);
        return intent;
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

    public static void shareLocalFile(Context context, ArrayList<FileInfo> files) {
        if (files != null && files.size() > 0) {
            boolean onlyImage = true;
            ArrayList<Uri> imageUris = new ArrayList<Uri>();
            for (FileInfo file : files) {
                Uri uri = Uri.fromFile(new File(file.path));
                imageUris.add(uri);
                if (!file.type.equals(FileInfo.TYPE.PHOTO))
                    onlyImage = false;
            }

            Intent shareIntent = new Intent();
            shareIntent.setType(onlyImage ? "image/*" : "*/*");

            if (imageUris.size() == 1) {
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUris.get(0));
            } else {
                shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris);
            }

            context.startActivity(Intent.createChooser(shareIntent, context.getResources().getText(R.string.share)));
            Log.w(TAG, "doShare: " + files.size() + " item(s)");
        }
    }

    public static String encodeString(String origPath){
        String encodePath = "";
        String[] paths = origPath.replaceFirst("/", "").split("/");
        int length = paths.length;
        try {
            String newPath = "";
            for (int i = 0; i < length; i++) {
                if(paths[i].contains(" ")) {
                    String[] tmp = paths[i].split(" ");
                    String result = "";
                    for(int j = 0 ; j < tmp.length ; j++) {
                        if(j == 0)
                            result = URLEncoder.encode(tmp[j], "UTF-8");
                        else
                            result += " " + URLEncoder.encode(tmp[j], "UTF-8");
                    }
                    newPath = newPath + "/" + result;
                } else {
                    newPath = newPath + "/" + URLEncoder.encode(paths[i], "UTF-8");
                }
            }
            encodePath = newPath.replaceAll(" ", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if(encodePath != null && !"".equals(encodePath))
            return encodePath;
        else
            return origPath;
    }

    public static void showFirmwareNotify(final Activity activity) {
        Builder builder = new Builder(activity);
        builder.setTitle(activity.getString(R.string.dialog_firmware_title_notify));
        builder.setMessage(R.string.dialog_firmware_message_notify);
        builder.setNegativeButton(R.string.dialog_firmware_button_remind, null);
        builder.setPositiveButton(R.string.dialog_firmware_button_update, null);
        builder.setCancelable(true);
        final android.support.v7.app.AlertDialog dialog = builder.show();
        Button posBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        posBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUpdateConfirm(activity);
                dialog.dismiss();

            }
        });
        Button negBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NASPref.setFirmwareNotify(activity, false);
                dialog.dismiss();
            }
        });
    }

    private static void showUpdateConfirm(final Activity activity) {
        Builder builder = new Builder(activity);
        builder.setTitle(activity.getString(R.string.dialog_firmware_title_notify));
        builder.setMessage(R.string.dialog_firmware_message_confirm);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.confirm, null);
        final android.support.v7.app.AlertDialog dialog = builder.show();
        Button posBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        posBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.startService(new Intent(activity, FirmwareUpdateService.class));
                dialog.dismiss();

            }
        });
    }

    public static String startP2PService(Context context) {
        String errorMsg;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            P2PService.getInstance().stopP2PConnect();
            int result = P2PService.getInstance().startP2PConnect(getUUID(context));
            if (result >= 0) {
                return "";
            }

            if (result == P2PTunnelAPIs.TUNNEL_ER_INITIALIZED)
                errorMsg = "Remote Access initial fail";
            else if (result == P2PTunnelAPIs.TUNNEL_ER_CONNECT)
                errorMsg = "Sorry, we can't find the device";
            else if (result == P2PTunnelAPIs.TUNNEL_ER_UID_UNLICENSE)
                errorMsg = "Sorry, this UID is illegal";
            else
                errorMsg = context.getString(R.string.network_error);
        }
        else {
            errorMsg = context.getString(R.string.network_error);
        }
        return errorMsg;

    }

    private static String getUUID(Context context) {
        String uuid = NASPref.getUUID(context);
        if (uuid == null || "".equals(uuid)) {
            uuid = NASPref.getCloudUUID(context);
//            if (uuid == null || "".equals(uuid)) {
//                return false;
//            }
        }
        return uuid;
    }

    private static String getIP() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
    }

    public static boolean isAdmin() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        return NASPref.defaultUserName.equals(server.getUsername());
    }

}
