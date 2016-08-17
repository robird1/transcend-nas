package com.transcend.nas.connection;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.R;
import com.transcend.nas.settings.DiskFactory;
import com.transcend.nas.settings.DiskStructDevice;
import com.tutk.IOTC.P2PService;
import com.tutk.IOTC.P2PTunnelAPIs;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ikelee on 16/7/22.
 */
public class P2PStatusTask extends AsyncTask<String, String, Boolean> {

    private static final String TAG = P2PStatusTask.class.getSimpleName();
    private Context mContext;
    private String mUID = "";
    private TextView mTextView;

    public P2PStatusTask(Context context, String strUID, TextView textView) {
        mContext = context;
        mUID = strUID;
        mTextView = textView;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(result) {
            mTextView.setText("On");
            mTextView.setTextColor(Color.GREEN);
        }
        else{
            mTextView.setText("Off");
            mTextView.setTextColor(ContextCompat.getColor(mContext, R.color.textColorSecondary));
        }
        Log.d(TAG, "onPostExecute, " + mUID + " : " + result);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        Log.d(TAG, "doInBackground, " + mUID);
        if (mUID.length() < 20) {
            Log.d(TAG, "P2P UID is short < 20");
            return false;
        }

        P2PTunnelAPIs commApis = new P2PTunnelAPIs(null);
        int init = commApis.P2PTunnelAgentInitialize(4);
        Log.d(TAG, "P2PTunnel m_nInit=" + init);
        String username = "Tutk.com", password = "P2P Platform";
        if (username.length() < 64) {
            for (int i = 0; username.length() < 64; i++) {
                username += "\0";
            }
        }
        if (password.length() < 64) {
            for (int i = 0; password.length() < 64; i++) {
                password += "\0";
            }
        }

        byte[] baAuthData = (username + password).getBytes();
        int[] pnErrFromDeviceCB = new int[1];
        int start = commApis.P2PTunnelAgent_Connect(mUID, baAuthData, baAuthData.length, pnErrFromDeviceCB);
        Log.d(TAG, "P2PTunnelAgent_Connect(.) UID=" + mUID + ", start=" + start);

        if(start >= 0)
            commApis.P2PTunnelAgent_Disconnect(start);
        commApis.P2PTunnelAgentDeInitialize();
        return start >= 0;
    }
}
