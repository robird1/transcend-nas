package com.transcend.nas.settings;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by steve_su on 2017/2/15.
 */

public class FirmwareHostNameLoader extends AsyncTaskLoader<Boolean> {
    private static String TAG = FirmwareHostNameLoader.class.getSimpleName();
    private String mHostName;

    public FirmwareHostNameLoader(Context context, String hostName) {
        super(context);
        mHostName = hostName;
    }

    @Override
    public Boolean loadInBackground() {
        int responseCode = setHostName();
        return true;
    }

    public String getHostName() {
        return mHostName;
    }

    private int setHostName() {
        return sendPostRequest();
    }

    private int sendPostRequest() {
        Log.d(TAG, "[Enter] sendPostRequest");
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        String hash = server.getHash();
        String commandURL = "http://" + hostname + "/nas/set/hostname";
        Log.d(TAG, "commandURL: "+ commandURL);
        Log.d(TAG, "mHostName: "+ mHostName);

        int responseCode = -1;
        try {
            HttpPost httpPost = new HttpPost(commandURL);
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("hostname", mHostName));
            nameValuePairs.add(new BasicNameValuePair("hash", hash));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = HttpClientManager.getClient().execute(httpPost);
            responseCode = response.getStatusLine().getStatusCode();
            Log.d(TAG, "responseCode: "+ responseCode);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseCode;
    }

}
