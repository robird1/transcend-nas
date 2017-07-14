package com.transcend.nas.management.fileaction;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.transcend.nas.LoaderID;
import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.management.FileInfo;
import com.transcend.nas.management.FileManageActivity;
import com.transcend.nas.management.LocalFileUploadLoader;
import com.transcend.nas.management.SmbFileDeleteLoader;
import com.transcend.nas.management.SmbFileListLoader;
import com.transcend.nas.management.SmbFileRenameLoader;
import com.transcend.nas.management.firmware.ConfigNTPServerLoader;
import com.transcend.nas.management.firmware.ConfigTimeZoneLoader;
import com.transcend.nas.management.firmware.EventNotifyLoader;
import com.transcend.nas.management.firmware.NTPServerLoader;
import com.transcend.nas.management.firmware.TimeZoneLoader;
import com.transcend.nas.management.firmware.TwonkyManager;
import com.transcend.nas.settings.FirmwareVersionLoader;
import com.transcend.nas.tutk.TutkLinkNasLoader;
import com.transcend.nas.tutk.TutkLogoutLoader;
import com.transcend.nas.viewer.document.OpenWithUploadHandler;

import java.util.ArrayList;

/**
 * Created by ike_lee on 2016/12/21.
 */
public class CustomActionManager extends AbstractActionManager {
    private static final String TAG = CustomActionManager.class.getSimpleName();

    private Context mContext;
    private LoaderManager.LoaderCallbacks mCallbacks;
    private RelativeLayout mProgressLayout;

    private int mPreviousLoaderID = -1;
    private Bundle mPreviousLoaderArgs = null;
    private int[] RETRY_CMD = new int[]{LoaderID.SMB_FILE_LIST, LoaderID.SMB_FILE_RENAME, LoaderID.SMB_FILE_DELETE,
            LoaderID.SMB_NEW_FOLDER, LoaderID.EVENT_NOTIFY, LoaderID.SMB_FILE_CHECK};

    private OpenWithUploadHandler mOpenWithUploadHandler;

    public CustomActionManager(Context context, LoaderManager.LoaderCallbacks callbacks) {
        this(context, callbacks, null);
    }

    public CustomActionManager(Context context, LoaderManager.LoaderCallbacks callbacks, RelativeLayout progressLayout) {
        mContext = context;
        mCallbacks = callbacks;
        mProgressLayout = progressLayout;
    }

    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        setRecordCommand(id, args);
        ArrayList<String> paths;
        String path, name;
        switch (id) {
            case LoaderID.LOCAL_FILE_UPLOAD_OPEN_WITH:
                mProgressLayout.setVisibility(View.VISIBLE);
                paths = args.getStringArrayList("paths");
                path = args.getString("path");
                return new LocalFileUploadLoader(mContext, paths, path, true);
            case LoaderID.SMB_FILE_DELETE_AFTER_UPLOAD:
                mProgressLayout.setVisibility(View.VISIBLE);
                paths = args.getStringArrayList("paths");
                return new SmbFileDeleteLoader(mContext, paths, true);
            case LoaderID.SMB_FILE_RENAME:
                mProgressLayout.setVisibility(View.VISIBLE);
                path = args.getString("path");
                name = args.getString("name");
                return new SmbFileRenameLoader(mContext, path, name);
            case LoaderID.EVENT_NOTIFY:
                mProgressLayout.setVisibility(View.VISIBLE);
                return new EventNotifyLoader(mContext, args);
            case LoaderID.TUTK_NAS_LINK:
                mProgressLayout.setVisibility(View.VISIBLE);
                return new TutkLinkNasLoader(mContext, args);
            case LoaderID.TUTK_LOGOUT:
                mProgressLayout.setVisibility(View.VISIBLE);
                return new TutkLogoutLoader(mContext);
            case LoaderID.FIRMWARE_VERSION:
                mProgressLayout.setVisibility(View.VISIBLE);
                return new FirmwareVersionLoader(mContext);
            case LoaderID.NTP_SERVER:
                mProgressLayout.setVisibility(View.VISIBLE);
                return new NTPServerLoader(mContext);
            case LoaderID.NTP_SERVER_CONFIG:
                mProgressLayout.setVisibility(View.VISIBLE);
                return new ConfigNTPServerLoader(mContext);
            case LoaderID.TIME_ZONE:
                mProgressLayout.setVisibility(View.VISIBLE);
                return new TimeZoneLoader(mContext);
            case LoaderID.TIME_ZONE_CONFIG:
                mProgressLayout.setVisibility(View.VISIBLE);
                return new ConfigTimeZoneLoader(mContext);
            default:
                return null;
        }
    }

    public boolean onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if(success) {
            if (loader instanceof TutkLinkNasLoader) {
                return doRecordCommand();
            } else if ((loader instanceof LocalFileUploadLoader)) {
                LocalFileUploadLoader uploadLoader = ((LocalFileUploadLoader) loader);
                if (uploadLoader.isOpenWithUpload()) {
                    String fileName = uploadLoader.getUniqueFileName();
                    mOpenWithUploadHandler.setTempFilePath(mOpenWithUploadHandler.getRemoteFileDirPath().concat(fileName));
                    ArrayList pathList = new ArrayList();
                    pathList.add(mOpenWithUploadHandler.getSelectedFile().path);
                    Bundle args = new Bundle();
                    args.putStringArrayList("paths", pathList);
                    ((Activity) mContext).getLoaderManager().restartLoader(LoaderID.SMB_FILE_DELETE_AFTER_UPLOAD, args, mCallbacks).forceLoad();
                    return true;
                }
            } else if ((loader instanceof SmbFileDeleteLoader)) {
                SmbFileDeleteLoader deleteLoader = ((SmbFileDeleteLoader) loader);
                if (deleteLoader.isDeleteAfterUpload()) {
                    Bundle args = new Bundle();
                    args.putString("path", mOpenWithUploadHandler.getTempFilePath());
                    args.putString("name", mOpenWithUploadHandler.getSelectedFile().name);
                    ((Activity) mContext).getLoaderManager().restartLoader(LoaderID.SMB_FILE_RENAME, args, mCallbacks).forceLoad();
                    return true;
                }
            } else if (loader instanceof EventNotifyLoader) {
                TwonkyManager.getInstance().initTwonky();
                SmbFileActionService service = new SmbFileActionService();
                int id = service.getLoaderID(FileActionService.FileAction.LIST);
                if(id > 0) {
                    Bundle args = ((EventNotifyLoader) loader).getBundleArgs();
                    args.putInt("actionType", id);
                    Loader tmp = ((Activity) mContext).getLoaderManager().restartLoader(id, args, mCallbacks);
                    if(tmp != null) {
                        tmp.forceLoad();
                        return true;
                    }
                }
            } else if (loader instanceof FirmwareVersionLoader) {
                String isUpgrade = ((FirmwareVersionLoader) loader).getIsUpgrade();
                if ("yes".equals(isUpgrade)) {
                    NASUtils.showFirmwareNotify(((Activity) mContext));
                }
                return true;
            } else if (loader instanceof TimeZoneLoader) {
                String timeZone = ((TimeZoneLoader) loader).getValue();
                if (!isTimeZoneValid(timeZone)) {
                    configTimeZone();
                } else {
                    checkNTPServer();
                }
                return true;

            } else if (loader instanceof ConfigTimeZoneLoader) {
                mProgressLayout.setVisibility(View.INVISIBLE);
                String result = ((ConfigTimeZoneLoader) loader).getValue();
                if ("update time zone".equals(result)) {
                    checkNTPServer();
                } else {
                }
                return true;

            } else if (loader instanceof NTPServerLoader) {
                mProgressLayout.setVisibility(View.INVISIBLE);
                String server = ((NTPServerLoader) loader).getValue();
                if ("time.windows.com".equals(server)) {
                    configNTPServer();
                }
                return true;
            } else if (loader instanceof ConfigNTPServerLoader) {
                String result = ((ConfigNTPServerLoader) loader).getValue();

                NASUtils.reLogin(mContext);
                mProgressLayout.setVisibility(View.INVISIBLE);
                return true;
            }
        } else {
            // force the execution of checking NTP server
            if (loader instanceof TimeZoneLoader) {
                checkNTPServer();
            }
        }

        return false;
    }

    public void onLoaderReset(Loader<Boolean> loader) {
    }

    public void setProgressLayout(RelativeLayout progressLayout) {
        mProgressLayout = progressLayout;
    }

    public boolean doNasHashKeyTimeOutCheck(String path) {
        Long lastTime = Long.parseLong(NASPref.getSessionVerifiedTime(mContext));
        Long currTime = System.currentTimeMillis();
        if (currTime - lastTime >= 180000) {
            Log.d(TAG, "doEventNotify");
            Bundle args = new Bundle();
            args.putString("path", path);
            ((Activity) mContext).getLoaderManager().restartLoader(LoaderID.EVENT_NOTIFY, args, mCallbacks).forceLoad();
            return true;
        }

        return false;
    }

    public boolean doNasTUTKLink(Loader<Boolean> loader) {
        if(loader instanceof TutkLinkNasLoader)
            return false;

        Bundle args = new Bundle();
        String uuid = NASPref.getUUID(mContext);
        if (uuid == null || "".equals(uuid)) {
            uuid = NASPref.getCloudUUID(mContext);
            if (uuid == null || "".equals(uuid)) {
                return false;
            }
        }

        args.putString("hostname", uuid);
        ((Activity) mContext).getLoaderManager().restartLoader(LoaderID.TUTK_NAS_LINK, args, mCallbacks).forceLoad();
        return true;
    }

    public void doOpenWithUpload(FileManageActivity activity, FileInfo fileInfo, String downloadFilePath, SmbFileListLoader loader){
        mOpenWithUploadHandler = new OpenWithUploadHandler(activity, fileInfo, downloadFilePath, loader);
        mOpenWithUploadHandler.showDialog();
    }


    public int getRecordCommandID() {
        return mPreviousLoaderID;
    }

    public Bundle getRecordCommandArg() {
        return mPreviousLoaderArgs;
    }

    public boolean setRecordCommand(int id, Bundle args) {
        if (id == LoaderID.TUTK_NAS_LINK) {
            return false;
        }

        boolean record = false;
        for (int cmd : RETRY_CMD) {
            if (id == cmd) {
                if (args != null) {
                    boolean retry = args.getBoolean("retry" , false);
                    if(!retry) {
                        mPreviousLoaderID = id;
                        mPreviousLoaderArgs = args;
                        record = true;
                    }
                }
                break;
            }
        }

        if (!record) {
            cleanRecordCommand();
        }

        return record;
    }

    public boolean doRecordCommand() {
        int id = getRecordCommandID();
        Bundle previous = getRecordCommandArg();
        Log.d(TAG, "doRecordCommand : " + id);
        if (id > 0 && previous != null) {
            previous.putBoolean("retry", true);
            ((Activity) mContext).getLoaderManager().restartLoader(id, previous, mCallbacks).forceLoad();
            return true;
        }
        return false;
    }

    public void cleanRecordCommand() {
        mPreviousLoaderID = -1;
        mPreviousLoaderArgs = null;
    }

    private boolean isTimeZoneValid(String timeZone) {
        return timeZone.length() > 2;
    }

    public void checkFirmwareVersion() {
        Bundle args = new Bundle();
        ((Activity) mContext).getLoaderManager().restartLoader(LoaderID.FIRMWARE_VERSION, args, mCallbacks).forceLoad();
    }

    public void checkTimeZone() {
        Bundle args = new Bundle();
        ((Activity) mContext).getLoaderManager().restartLoader(LoaderID.TIME_ZONE, args, mCallbacks).forceLoad();
    }

    private void configTimeZone() {
        Bundle args = new Bundle();
        ((Activity) mContext).getLoaderManager().restartLoader(LoaderID.TIME_ZONE_CONFIG, args, mCallbacks).forceLoad();
    }

    private void checkNTPServer() {
        Bundle args = new Bundle();
        ((Activity) mContext).getLoaderManager().restartLoader(LoaderID.NTP_SERVER, args, mCallbacks).forceLoad();
    }

    private void configNTPServer() {
        Bundle args = new Bundle();
        ((Activity) mContext).getLoaderManager().restartLoader(LoaderID.NTP_SERVER_CONFIG, args, mCallbacks).forceLoad();
    }



}
