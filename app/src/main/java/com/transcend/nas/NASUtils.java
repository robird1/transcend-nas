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
import android.os.storage.StorageManager;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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

    public static List<File> getStoragePath(Context mContext) {
        List<File> stgList = new ArrayList<File>();
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = null;
            Method getPath = null;
            Method isRemovable = null;
            try {
                getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
                getPath = storageVolumeClazz.getMethod("getPath");
                isRemovable = storageVolumeClazz.getMethod("isRemovable");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            Method getSubSystem = null;
            try {
                getSubSystem = storageVolumeClazz.getMethod("getSubSystem");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                String subSystem = "";
                if (getSubSystem != null) {
                    subSystem = (String) getSubSystem.invoke(storageVolumeElement);
                }
                if (!subSystem.contains("usb")) {
                    if (!path.toLowerCase().contains("private")) {
                        stgList.add(new File(path));
                    }

                }

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return stgList;
    }

    public static boolean isSDCardPath(Context context, String path) {
//        List<File> stgList = NASUtils.getStoragePath(context);
//        if (stgList.size() > 1) {
//            for (File sd : stgList) {
//                if ((!sd.getAbsolutePath().contains(NASApp.ROOT_STG)) && (!sd.getAbsolutePath().toLowerCase().contains("usb"))) {
//                    Log.d(TAG, "current path: "+ path);
//                    Log.d(TAG, "sd.getAbsolutePath(): "+ sd.getAbsolutePath());
//                    return path.contains(sd.getAbsolutePath());
//                }
//            }
//        }
        String location = getSDLocation(context);
        return location != null && path.contains(location);
    }

    public static String getSDLocation(Context context) {
        List<File> stgList = NASUtils.getStoragePath(context);
        if (stgList.size() > 1) {
            for (File sd : stgList) {
                if ((!sd.getAbsolutePath().contains(NASApp.ROOT_STG)) && (!sd.getAbsolutePath().toLowerCase().contains("usb"))) {
                    Log.d(TAG, "sd.getAbsolutePath(): "+ sd.getAbsolutePath());
                    return sd.getAbsolutePath();
                }
            }
        }

        return null;
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
