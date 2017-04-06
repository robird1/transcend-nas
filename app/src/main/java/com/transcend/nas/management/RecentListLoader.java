package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.transcend.nas.service.FileRecentInfo;
import com.transcend.nas.service.FileRecentManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by ikelee on 17/3/22.
 */
public class RecentListLoader extends AsyncTaskLoader {
    private static final String TAG = RecentListLoader.class.getSimpleName();
    private String[] WEEK_REVERSE = new String[]{"Saturday", "Friday", "Thursday", "Wednesday", "Tuesday", "Monday", "Sunday"};

    private ArrayList<FileRecentInfo> mFileList;
    private ArrayList<String> mFileDayIdList;
    private int mUserID;
    private String mPath;

    public RecentListLoader(Context context, String path) {
        this(context, -1, path);
    }

    public RecentListLoader(Context context, int userID, String path) {
        super(context);
        mFileList = new ArrayList<FileRecentInfo>();
        mFileDayIdList = new ArrayList<>();
        mUserID = userID;
        mPath = path;
    }

    @Override
    public Boolean loadInBackground() {
        try {
            return updateFileList();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean updateFileList() {
        ArrayList<FileRecentInfo> files;
        if (mUserID >= 0) {
            //files = FileRecentManager.getInstance().getAction(mUserID, mPath);
            files = FileRecentManager.getInstance().getAction(mUserID, null);
        } else {
            //files = FileRecentManager.getInstance().getAction(mPath);
            files = FileRecentManager.getInstance().getAction();
        }
        Log.w(TAG, "mFileList origin size: " + files.size());

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        String year = cal.get(Calendar.YEAR) + "-";
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        int dayCount = 0;
        int dayWeekTotal = WEEK_REVERSE.length;
        String day = dateFormat.format(cal.getTime());
        for (FileRecentInfo file : files) {
            if (file.actionTime != null && !"".equals(file.actionTime)) {
                String time = file.actionTime.split(" ")[0];
                while (dayCount < dayWeekTotal) {
                    if (time.startsWith(day)) {
                        switch (dayCount) {
                            case 0:
                                time = "Today";
                                break;
                            case 1:
                                time = "Yesterday";
                                break;
                            default:
                                time = WEEK_REVERSE[(dayWeekTotal - dayOfWeek + dayCount) % dayWeekTotal];
                                break;
                        }
                        break;
                    } else {
                        cal.add(Calendar.DATE, -1);
                        day = dateFormat.format(cal.getTime());
                        dayCount++;
                    }
                }

                if (time.startsWith(year)) {
                    time = time.replaceFirst(year, "");
                }

                mFileDayIdList.add(time);
                mFileList.add(file);
            }
        }

        Log.w(TAG, "mFileList size: " + mFileList.size());
        return true;
    }

    public String getPath() {
        return mPath;
    }

    public ArrayList<FileRecentInfo> getFileList() {
        return mFileList;
    }

    public ArrayList<String> getFileDayIDList(){
        return mFileDayIdList;
    }

}
