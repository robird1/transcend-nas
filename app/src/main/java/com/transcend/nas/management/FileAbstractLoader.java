package com.transcend.nas.management;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.transcend.nas.common.CustomNotificationManager;
import com.transcend.nas.utils.MathUtil;

/**
 * Created by ikelee on 17/8/9.
 */
public abstract class FileAbstractLoader extends AsyncTaskLoader<Boolean> {
    private static final String TAG = FileAbstractLoader.class.getSimpleName();

    private Activity mActivity;
    private String mType = "";
    protected int mNotificationID = 0;
    private String[] mLoadingString = {".", "..", "..."};
    private NotificationCompat.Builder mBuilder;

    protected int mTotal = 0;
    protected int mCurrent = 0;

    public FileAbstractLoader(Context context) {
        super(context);
        mActivity = (Activity) context;
    }

    @Override
    public Boolean loadInBackground() {
        return true;
    }

    protected void setType(String type) {
        mType = type;
    }

    public String getType() {
        return mType;
    }

    protected void updateProgress(String name, int count, int total){
        updateProgress(name, count, total, true);
    }

    protected void updateProgress(String name, int count, int total, boolean showProgress) {
        if(isLoadInBackgroundCanceled()) {
            return;
        }

        Log.w(TAG, mNotificationID + " progress: " + count + "/" + total + ", " + name);
        if (mBuilder == null) {
            mBuilder = CustomNotificationManager.createProgressBuilder(getContext(), mActivity, mNotificationID);
        }

        if(showProgress) {
            int max = 100;
            int progress = (total > 100) ? count / (total / 100) : 0;
            boolean indeterminate = (total == 0);

            String stat = String.format("%s / %s", MathUtil.getBytes(count), MathUtil.getBytes(total));
            String text = String.format("%s - %s", getType(), stat);
            String info = String.format("%d%%", progress);

            mBuilder.setContentText(text);
            mBuilder.setContentInfo(info);
            mBuilder.setProgress(max, progress, indeterminate);
        } else {
            String loading = mLoadingString[mCurrent%mLoadingString.length];
            mBuilder.setContentText(String.format("%s%s", getType(), loading));
        }

        String title = mTotal > 1 ? String.format("(%s/%s) " + name, mCurrent, mTotal) : name;
        mBuilder.setContentTitle(title);

        NotificationManager ntfMgr = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        ntfMgr.notify(mNotificationID, mBuilder.build());
    }

    protected void updateResult(String result, String destination) {
        if(isLoadInBackgroundCanceled()) {
            return;
        }

        CustomNotificationManager.updateResult(getContext(), mNotificationID, getType(), result, destination);
    }
}
