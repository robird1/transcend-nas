package com.transcend.nas.management.action;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.util.Log;
import android.widget.RelativeLayout;

import com.transcend.nas.LoaderID;
import com.transcend.nas.NASPref;
import com.transcend.nas.management.firmware.EventNotifyLoader;
import com.transcend.nas.service.LanCheckManager;
import com.transcend.nas.tutk.TutkLinkNasLoader;
import com.transcend.nas.tutk.TutkLogoutLoader;

/**
 * Created by ike_lee on 2017/08/07.
 */
public class ConnectManager extends AbstractActionManager {
    private static String TAG = ConnectManager.class.getSimpleName();
    private ConnectActionListener mListener;

    public interface ConnectActionListener {
        boolean finishNasHashKeyTimeOutCheck();

        boolean finishNasTUTKLink();

        boolean finishNasTUTKLogout();
    }

    private int mPreviousLoaderID;
    private Bundle mPreviousLoaderArg;
    private int[] RETRY_CMD = new int[]{LoaderID.SMB_FILE_LIST, LoaderID.SMB_FILE_RENAME, LoaderID.SMB_FILE_DELETE,
            LoaderID.SMB_NEW_FOLDER, LoaderID.EVENT_NOTIFY, LoaderID.SMB_FILE_CHECK};

    public ConnectManager(Context context, LoaderManager.LoaderCallbacks callbacks) {
        this(context, callbacks, null);
    }

    public ConnectManager(Context context, LoaderManager.LoaderCallbacks callbacks, RelativeLayout progressLayout) {
        super(context, callbacks, progressLayout);
    }

    public void setListener(ConnectActionListener listener) {
        mListener = listener;
    }

    public Loader<Boolean> onCreateLoader(int id, Bundle args) {
        setRecordCommand(id, args);
        switch (id) {
            case LoaderID.EVENT_NOTIFY:
                showProgress();
                return new EventNotifyLoader(getContext(), args);
            case LoaderID.TUTK_NAS_LINK:
                showProgress();
                return new TutkLinkNasLoader(getContext(), args);
            case LoaderID.TUTK_LOGOUT:
                showProgress();
                return new TutkLogoutLoader(getContext());
            default:
                return null;
        }
    }

    public boolean onLoadFinished(Loader<Boolean> loader, Boolean success) {
        if (mListener != null && success) {
            if (loader instanceof EventNotifyLoader) {
                if (mListener.finishNasHashKeyTimeOutCheck())
                    return true;

                Bundle args = ((EventNotifyLoader) loader).getBundleArgs();
                if (args != null) {
                    int id = args.getInt("actionType", -1);
                    if (id > 0)
                        return createLoader(id, args);
                }
            } else if (loader instanceof TutkLinkNasLoader) {
                if (mListener.finishNasTUTKLink())
                    return true;

                return doRecordCommand();
            } else if (loader instanceof TutkLogoutLoader) {
                return mListener.finishNasTUTKLogout();
            }
        } else {
            if (!LanCheckManager.getInstance().getLanConnect() && getRecordCommandID() > 0)
                return doNasTUTKLink(loader);

            LanCheckManager.getInstance().startLanCheck();
        }

        return false;
    }

    public void onLoaderReset(Loader<Boolean> loader) {
    }


    public int getRecordCommandID() {
        return mPreviousLoaderID;
    }

    public Bundle getRecordCommandArg() {
        return mPreviousLoaderArg;
    }

    public boolean setRecordCommand(int id, Bundle args) {
        if (id == LoaderID.TUTK_NAS_LINK)
            return false;

        boolean record = false;
        boolean retry = false;
        for (int cmd : RETRY_CMD) {
            if (cmd == id) {
                if (args != null) {
                    retry = args.getBoolean("retry", false);
                    if (!retry) {
                        mPreviousLoaderID = id;
                        mPreviousLoaderArg = args;
                        record = true;
                    }
                }
            }
        }

        Log.d(TAG, "RecordCommand : " + id + ", record : " + record + ", retry : " + retry);
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
            return createLoader(id, previous);
        }
        return false;
    }

    public void cleanRecordCommand() {
        mPreviousLoaderID = -1;
        mPreviousLoaderArg = null;
    }


    public boolean doNasHashKeyTimeOutCheck(Bundle args) {
        Long lastTime = Long.parseLong(NASPref.getSessionVerifiedTime(getContext()));
        Long currTime = System.currentTimeMillis();
        if (currTime - lastTime >= 180000) {
            return createLoader(LoaderID.EVENT_NOTIFY, args);
        }

        return false;
    }

    public boolean doNasTUTKLink(Loader<Boolean> loader) {
        if (loader instanceof TutkLinkNasLoader)
            return false;

        Bundle args = new Bundle();
        String uuid = NASPref.getUUID(getContext());
        if (uuid == null || "".equals(uuid)) {
            uuid = NASPref.getCloudUUID(getContext());
            if (uuid == null || "".equals(uuid)) {
                return false;
            }
        }

        args.putString("hostname", uuid);
        return createLoader(LoaderID.TUTK_NAS_LINK, args);
    }

}
