package com.transcend.nas;

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
import android.webkit.MimeTypeMap;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.transcend.nas.connection.LoginHelper;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.service.FileRecentManager;
import com.transcend.nas.utils.MimeUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.data;
import static android.R.attr.mimeType;

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
//        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
//        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
//        Log.d(TAG, "extension: " + extension);
//        Log.d(TAG, "mimeType: " + mimeType);
//
//        intent.setDataAndType(fileUri, mimeType);

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

}
