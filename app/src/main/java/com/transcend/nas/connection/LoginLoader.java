package com.transcend.nas.connection;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASPref;
import com.tutk.IOTC.P2PService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by silverhsu on 16/1/5.
 */
public class LoginLoader extends AsyncTaskLoader<Boolean> {

    private Server mServer;

    public LoginLoader(Context context, Bundle args) {
        super(context);
        String hostname = args.getString("hostname");
        String username = args.getString("username");
        String password = args.getString("password");
        mServer = new Server(hostname, username, password);
    }

    @Override
    public Boolean loadInBackground() {
        boolean success = mServer.connect();
        if (success) {
            updateServerManager();
            updateLoginPreference();
        }
        return success;
    }


    private void updateServerManager() {
        ServerManager.INSTANCE.saveServer(mServer);
        ServerManager.INSTANCE.setCurrentServer(mServer);
    }

    private void updateLoginPreference() {
        NASPref.setLoginHostname(getContext(), mServer.getHostname());
        NASPref.setLoginUsername(getContext(), mServer.getUsername());
        NASPref.setLoginPassword(getContext(), mServer.getPassword());
    }
}
