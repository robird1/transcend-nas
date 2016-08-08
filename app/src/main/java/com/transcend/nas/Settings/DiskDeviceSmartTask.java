package com.transcend.nas.settings;

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
import com.transcend.nas.utils.FileFactory;
import com.tutk.IOTC.P2PService;

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
public class DiskDeviceSmartTask extends AsyncTask<String, String, Boolean> {

    private static final String TAG = DiskDeviceSmartTask.class.getSimpleName();
    private Context mContext;
    private DiskStructDevice mDevice;
    List<DiskStructDevice> mDevices;
    private String mResult = "";
    private TextView mTextView;

    public DiskDeviceSmartTask(Context context, List<DiskStructDevice> devices, DiskStructDevice device, TextView textView) {
        mContext = context;
        mDevice = device;
        mDevices = devices;
        mTextView = textView;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        DiskFactory.getInstance().setDeviceSmartResult(mResult, mDevice);
        DiskFactory.getInstance().setDeviceSmartText(mContext, mTextView, mDevice);
        Log.d(TAG, "onPostExecute : " + result + ", " + mDevice.infos.get("path") + " : " + mDevice.smart);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        return getSMARTInfo(mDevice.infos.get("path"));
    }

    private boolean getSMARTInfo(String path) {
        Log.d(TAG, "getSmartInfo : " + path);
        boolean isSuccess = false;

        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = server.getHostname();
        String hash = server.getHash();
        String p2pIP = P2PService.getInstance().getP2PIP();
        if (hostname.contains(p2pIP)) {
            hostname = p2pIP + ":" + P2PService.getInstance().getP2PPort(P2PService.P2PProtocalType.HTTP);
        }
        DefaultHttpClient httpClient = HttpClientManager.getClient();
        String commandURL = "http://" + hostname + "/nas/smart/all";
        HttpResponse response = null;
        InputStream inputStream = null;
        try {
            do {
                HttpPost httpPost = new HttpPost(commandURL);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("hash", hash));
                nameValuePairs.add(new BasicNameValuePair("device", path));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                response = httpClient.execute(httpPost);
                if (response == null) {
                    Log.e(TAG, "response is null");
                    break;
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    Log.e(TAG, "response entity is null");
                    break;
                }
                inputStream = entity.getContent();
                String inputEncoding = EntityUtils.getContentCharSet(entity);
                if (inputEncoding == null) {
                    inputEncoding = HTTP.DEFAULT_CONTENT_CHARSET;
                }

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(inputStream, inputEncoding);
                int eventType = xpp.getEventType();
                String curTagName = null;
                String text = null;

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null) {
                            text = xpp.getText();
                            if (curTagName.equals("all")) {
                                isSuccess = true;
                                mResult = text;
                            } else if (curTagName.equals("reason")){
                                isSuccess = true;
                                mResult = text;
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        curTagName = null;
                    }
                    eventType = xpp.next();
                } while (eventType != XmlPullParser.END_DOCUMENT);
            } while (false);

        } catch (XmlPullParserException e) {
            Log.d(TAG, "XML Parser error");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "Fail to connect to server");
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "catch IllegalArgumentException");
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return isSuccess;
    }
}
