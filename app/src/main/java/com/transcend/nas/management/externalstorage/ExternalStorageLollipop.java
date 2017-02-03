package com.transcend.nas.management.externalstorage;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.transcend.nas.DrawerMenuActivity;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.viewer.photo.ViewerPager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.transcend.nas.NASUtils.getSDLocation;
import static com.transcend.nas.NASUtils.isSDCardPath;

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
    protected void onNavigationItemSelected(DrawerMenuActivity activity, int itemId) {
        if (isWritePermissionNotGranted() == true) {
            Toast.makeText(mContext, R.string.dialog_request_write_permission, Toast.LENGTH_LONG).show();
            requestPermissionDialog(activity);
        } else {
            Uri treeUri = Uri.parse(getSDLocationUri());
            Log.d(TAG, "treeUri: "+ treeUri);
            Log.d(TAG, "getSDLocation(): "+ getSDLocation(mContext));
            NASApp.ROOT_SD = getSDLocation(mContext);
            activity.startFileManageActivity(itemId);
        }
    }

    @Override
    protected void onActivityResult(FileManageActivity activity, Intent data) {
        if (activity == null || data == null) {
            return;
        }

        boolean isValid = checkSelectedFolder(data);
        if (isValid) {
            activity.doLoad(getSDLocation(mContext));
        } else {
            Toast.makeText(mContext, R.string.dialog_grant_permission_failed, Toast.LENGTH_LONG).show();
            requestPermissionDialog(activity);
        }
    }

    @Override
    public boolean isWritePermissionNotGranted() {
        String uri = getSDLocationUri();
        if (uri != null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isWritePermissionRequired(String... path) {
        for (String p : path) {
            if (isSDCardPath(mContext, p))
                return true;
        }
        return false;
    }

    @Override
    public Uri getSDFileUri(String path) {
        DocumentFile file = getSDFileLocation(path);
        if (file != null) {
            return file.getUri();
        }
        return null;
    }

    @TargetApi(19)
    public boolean checkSelectedFolder(Intent data) {
        boolean isValid = false;
        Uri uriTree = data.getData();
        Log.d(TAG, "uriTree.toString(): " + uriTree.toString());
        if (!uriTree.toString().contains("primary")) {
            if (isRootFolder(uriTree)) {
                mContext.getContentResolver().takePersistableUriPermission(uriTree,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                DocumentFile pickedDir = DocumentFile.fromTreeUri(mContext, uriTree);
                //jerry
                boolean isSDFile = isSDFile(pickedDir);
                Log.d(TAG, "isSDFile: " + isSDFile);
                if (isSDFile) {
                    storeSDLocationUri(uriTree);
                    NASApp.sdDir = pickedDir;
                    Log.d(TAG, "NASApp.sdDir: " + NASApp.sdDir);

                    isValid = true;
                }
            }
        }
        return isValid;
    }

    public DocumentFile getDestination(String destPath) {
        File file = new File(destPath);
        if (!file.exists()) {
            return null;
        }
        DocumentFile pickedDir;
        if (NASUtils.isSDCardPath(mContext, destPath)) {
            pickedDir = getSDFileLocation(destPath);
        } else {
            pickedDir = DocumentFile.fromFile(file);
        }
        return pickedDir;
    }

    public DocumentFile getSDFileLocation(String destPath) {
        DocumentFile sdFile = getRootFolderSD();
        if (destPath.equals(NASApp.ROOT_SD)) {//root path
            return sdFile;
        } else {
            String[] splitPath = destPath.split("/");
            if (splitPath.length > 3) {
                for (int index = 3; index < splitPath.length; index++) {
                    sdFile = sdFile.findFile(splitPath[index]);
                }
                return sdFile;

            } else {
                return sdFile;
            }
        }
    }

    @TargetApi(19)
    private DocumentFile getRootFolderSD() {
        String uriTree = (String) PreferenceManager.getDefaultSharedPreferences(mContext).getAll().get(PREF_DEFAULT_URISD);
        mContext.getContentResolver().takePersistableUriPermission(Uri.parse(uriTree),
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        return DocumentFile.fromTreeUri(mContext, Uri.parse(uriTree));
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

        return (splitURI.length == 2) && (splitURI[1].length() <= 2);
    }

    private boolean isSDFile(DocumentFile sdDir) {
        List<File> stgList = NASUtils.getStoragePath(mContext);
        boolean isSDFile = false;
        if (stgList.size() > 1) {//has sd card
            for (File sd : stgList) {
                if ((!sd.getAbsolutePath().contains(NASApp.ROOT_STG)) && (!sd.getAbsolutePath().toLowerCase().contains("usb"))) {
                    try {
                        ArrayList<String> tmpFile = getSDCardFileName(getSDLocation(mContext));
                        DocumentFile[] tmpDFile = sdDir.listFiles();
                        isSDFile = doFileNameCompare(tmpDFile, tmpFile);
                    } catch (Exception e) {
                        isSDFile = false;
                    }

                    break;
                }
            }
        }
        return isSDFile;
    }

    private ArrayList<String> getSDCardFileName(String mPath) {
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

    private String getSDLocationUri() {
        return (String) PreferenceManager.getDefaultSharedPreferences(mContext).getAll().get(PREF_DEFAULT_URISD);
    }

    private boolean storeSDLocationUri(Uri uriTree) {
        return PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString(PREF_DEFAULT_URISD, uriTree.toString()).commit();
    }

    private void requestPermissionDialog(final DrawerMenuActivity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(mContext.getResources().getString(R.string.sdcard));
        builder.setIcon(R.drawable.ic_drawer_sdcard);
        builder.setView(R.layout.dialog_connect_sd);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(R.string.confirm, null);
        builder.setCancelable(false);
        final AlertDialog dialog = builder.show();
        Button posBtn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        posBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                activity.startActivityForResult(intent, REQUEST_CODE);
                dialog.dismiss();
            }
        });
        posBtn.setTextSize(18);
        Button negBtn = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        negBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.toggleDrawerCheckedItem();
                dialog.dismiss();
            }
        });
        negBtn.setTextSize(18);

        ViewerPager viewerPager = (ViewerPager) dialog.findViewById(R.id.viewer_pager_sd);
        viewerPager.setAdapter(new ViewerPagerAdapterSD(mContext));
        viewerPager.setCurrentItem(0);

    }

}
