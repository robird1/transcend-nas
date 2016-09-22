package com.transcend.nas.service;

import android.os.AsyncTask;

/**
 * Created by ike_lee on 2016/9/20.
 */
public class TwonkyTask extends AsyncTask<String, String, Boolean> {
    private static final String TAG = "TwonkyTask";
    private TwonkyTaskCallback mListener;
    private String mResult;

    public TwonkyTask() {

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        //get twonky server
        String server = TwonkyManager.getInstance().parserTwonkyServer();
        if(server != null && !server.equals("")) {
            //get twonky category "Photos"
            String photos = TwonkyManager.getInstance().parserTwonkyCategory(server, "Photos", "?start=0&fmt=json");
            if(photos != null && !photos.equals("")) {
                //get twonky category "By Folder"
                String byFolder = TwonkyManager.getInstance().parserTwonkyCategory(photos, "By Folder", "?start=0&fmt=json");
                if(byFolder != null && !byFolder.equals("")) {
                    mResult = byFolder;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (mListener != null) {
            mListener.onTwonkyTaskFinished(result, mResult);
        }
    }

    public void addListener(TwonkyTaskCallback listener) {
        mListener = listener;
    }

    public interface TwonkyTaskCallback {
        public void onTwonkyTaskFinished(boolean success, String url);
    }
}
