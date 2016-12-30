package com.transcend.nas.management;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.settings.BaseDrawerActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by steve_su on 2016/12/26.
 */

public class ExternalStorageLollipop extends AbstractExternalStorage {
    private static final String TAG = ExternalStorageLollipop.class.getSimpleName();
    public static final String PREF_DEFAULT_URISD = "PREF_DEFAULT_URISD";
    public static final int REQUEST_CODE = ExternalStorageLollipop.class.hashCode() & 0xFFFF;
    private Context mContext;

    public ExternalStorageLollipop(Context context) {
        super(context);
        mContext = getContext();
    }

    @Override
    protected void onNavigationItemSelected(BaseDrawerActivity activity, int itemId) {
        Log.d(TAG, "[Enter] onNavigationItemSelected()");

        if (isFirstSelectSDCard() == true) {
            requestPermissionDialog(activity);
        } else {
//            String[] temp = locationUri.split("@@@@");
//            Log.d(TAG, "locationUri: "+ locationUri);
//            Log.d(TAG, "temp.length: "+ temp.length);
//
//            if (temp.length == 2) {
//                Uri treeUri = Uri.parse(temp[1]);
//                    Uri treeUri = Uri.parse(locationUri);
            Uri treeUri = Uri.parse(getSDLocationUri());
            Log.d(TAG, "treeUri: "+ treeUri);

            Log.d(TAG, "getSDLocation(): "+ getSDLocation());
            NASApp.ROOT_SD = getSDLocation();
            activity.startFileManageActivity(itemId);

//            }
        }

    }

    @Override
    protected void onActivityResult(FileManageActivity activity, Intent data) {
        Log.d(TAG, "[Enter] onActivityResult()");

        Uri uriTree = data.getData();
        Log.d(TAG, "uriTree.toString(): " + uriTree.toString());
        if (!uriTree.toString().contains("primary")) {

            if (isRootFolder(uriTree)) {
                if (Build.VERSION.SDK_INT >= 19) {
                    Log.d(TAG, "[Enter] Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION");
                    mContext.getContentResolver().takePersistableUriPermission(uriTree,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    DocumentFile pickedDir = DocumentFile.fromTreeUri(mContext, uriTree);//OTG root path
                    //jerry
                    boolean isSDFile = isSDFile(pickedDir);
                    Log.d(TAG, "isSDFile: " + isSDFile);
                    if (isSDFile) {
                        boolean storedResult = storeSDLocationUri(uriTree);
                        Log.d(TAG, "storedResult: " + storedResult);

                        NASApp.sdDir = pickedDir;
                        Log.d(TAG, "NASApp.sdDir: " + NASApp.sdDir);
//                    mRoot = NASApp.ROOT_SD;
//                    Log.d(TAG, "mRoot: " + mRoot);

                        activity.doLoad(getSDLocation());
                    }
//                            else {
//                                toast(R.string.toast_plz_select_sd);
//                                requestPermissionDialog();
//                            }

                }
            }
            else {
                Toast.makeText(mContext, "Selected folder is not the root folder of SD card", Toast.LENGTH_LONG).show();
                requestPermissionDialog(activity);
            }
        }
        else {
            Toast.makeText(mContext, "Selected folder is not the folder of SD card", Toast.LENGTH_LONG).show();
            requestPermissionDialog(activity);
        }

        activity.toggleDrawerCheckedItem();

    }

    /**
     * example of splitURI:  (root folder: /BE8D-1108)         content://com.android.externalstorage.documents/tree/BE8D-1108%3A
     * example of splitURI:  (subfolder: /BE8D-1108/Android)   content://com.android.externalstorage.documents/tree/BE8D-1108%3AAndroid
     *
     * @param uriTree
     * @return
     */
    private boolean isRootFolder(Uri uriTree) {
        String[] splitURI = new String[0];
        if (uriTree != null) {
            splitURI = uriTree.toString().split("%");
        }

        for (int i = 0; i < splitURI.length; i++) {
            Log.d(TAG, "splitURI[i]: " + splitURI[i]);
        }

        return (splitURI.length == 2) && (splitURI[1].length() <= 2);
    }

    private boolean isSDFile(DocumentFile sdDir) {
        Log.d(TAG, "[Enter] isSDFile()");
        List<File> stgList = NASUtils.getStoragePath(mContext);
        boolean isSDFile = false;
        if (stgList.size() > 1) {//has sd card
            for (File sd : stgList) {
                if ((!sd.getAbsolutePath().contains(NASApp.ROOT_STG)) && (!sd.getAbsolutePath().toLowerCase().contains("usb"))) {
                    try {
                        ArrayList<String> tmpFile = getSDCardFileName(getSDLocation());
                        Log.d(TAG, "getSDLocation(): "+ getSDLocation());

                        DocumentFile[] tmpDFile = sdDir.listFiles();
                        isSDFile = doFileNameCompare(tmpDFile, tmpFile);
                    }catch (Exception e){
                        isSDFile = false;
                    }

                    break;
                }
            }
        }
        return isSDFile;
    }

    public static ArrayList<String> getSDCardFileName(String mPath) {
        ArrayList<String> sdName = new ArrayList<String>();
        File dir = new File(mPath);

        File files[] = dir.listFiles();
        for (File file : files) {
            if (file.isHidden())
                continue;
            String name = file.getName();
            sdName.add(name);
        }
        return sdName;
    }

    private boolean doFileNameCompare(DocumentFile[] tmpDFile, ArrayList<String> tmpFile) {
        int fileCount = 0;
        for (int fi = 0; fi < tmpFile.size(); fi++) {
            String name = tmpFile.get(fi);
            for (int df = 0; df < tmpDFile.length; df++) {
                if (name.equals(tmpDFile[df].getName())) {
                    fileCount++;
                    break;
                }
            }
        }
        if (fileCount == tmpFile.size()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isFirstSelectSDCard() {
        boolean isfirstSelected = true;
        String uri = getSDLocationUri();
        if (uri != null) {
            isfirstSelected = false;
        }
        return isfirstSelected;
    }

    private String getSDLocationUri() {
        return (String) PreferenceManager.getDefaultSharedPreferences(mContext).getAll().get(PREF_DEFAULT_URISD);
    }

    private boolean storeSDLocationUri(Uri uriTree) {
        return PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString(PREF_DEFAULT_URISD, uriTree.toString()).commit();
    }

    private void requestPermissionDialog(final BaseDrawerActivity activity) {
        Log.d(TAG, "[Enter] requestPermissionDialog()");
        new AlertDialog.Builder(mContext).setTitle(mContext.getResources().getString(R.string.app_name))
                .setMessage("Please select root folder to grant read/write permission")
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        activity.startActivityForResult(intent, REQUEST_CODE);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.toggleDrawerCheckedItem();
                    }
                }).show();
    }

}
