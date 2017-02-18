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
    private String mUserInput;

    public FirmwareHostNameLoader(Context context, String userInput) {
        super(context);
        mUserInput = userInput;
    }

    @Override
    public Boolean loadInBackground() {
        setHostName();
        return true;
    }

    private void setHostName() {
        sendPostRequest();
    }

    private void sendPostRequest() {
        Log.d(TAG, "[Enter] sendPostRequest");
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String ip = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        String hash = server.getHash();
        String commandURL = "http://" + ip + "/nas/set/hostname";
        Log.d(TAG, "commandURL: "+ commandURL);
        Log.d(TAG, "mUserInput: "+ mUserInput);

        try {
            HttpPost httpPost = new HttpPost(commandURL);
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("hostname", mUserInput));
            nameValuePairs.add(new BasicNameValuePair("hash", hash));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = HttpClientManager.getClient().execute(httpPost);
            Log.d(TAG, "responseCode: "+ response.getStatusLine().getStatusCode());

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
