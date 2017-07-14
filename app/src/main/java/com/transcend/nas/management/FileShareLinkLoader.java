package com.transcend.nas.management;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.management.firmware.ShareFolderManager;
import com.transcend.nas.management.firmware.TwonkyManager;
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
 * Created by ikelee on 17/2/16.
 */
public class FileShareLinkLoader extends AsyncTaskLoader<Boolean> {

    private static final String TAG = FileShareLinkLoader.class.getSimpleName();
    private ArrayList<String> mPaths;
    private ArrayList<String> mUrls;
    private ArrayList<String> mAbsolutePaths;
    private int DEFAULT_VALID_TIME = -1;
    private String mResult = "";

    public FileShareLinkLoader(Context context, ArrayList<String> paths) {
        super(context);
        mPaths = paths;
        mUrls = new ArrayList<>();
        mAbsolutePaths = new ArrayList<>();
    }

    @Override
    public Boolean loadInBackground() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String username = server.getUsername();
        for(String path : mPaths) {
            String realPath = ShareFolderManager.getInstance().getRealPath(path);
            if (path.equals(realPath) && path.startsWith("/" + username + "/"))
                realPath = "/home" + path;

            //String url = TwonkyManager.getInstance().getFileUrl(server, realPath);
            //if(url != null && !"".equals(url)) {
            //    Log.d(TAG, "file path : " + realPath  + ", twonky url " + url);
            //    mAbsolutePaths.add(realPath);
            //    mUrls.add(url);
            //}
            getShareLink(server, DEFAULT_VALID_TIME, realPath);
        }


        return mUrls.size() > 0;
    }

    private boolean getShareLink(Server server, int validTime, String path) {
        boolean isSuccess = false;
        String username = server.getUsername();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        String hash = server.getHash();
        if (hash != null && !"".equals(hash)) {
            DefaultHttpClient httpClient = HttpClientManager.getClient();
            String commandURL = "http://" + hostname + "/nas/get/sharelink";
            HttpResponse response = null;
            InputStream inputStream = null;
            try {
                do {
                    HttpPost httpPost = new HttpPost(commandURL);
                    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                    nameValuePairs.add(new BasicNameValuePair("hash", hash));
                    nameValuePairs.add(new BasicNameValuePair("path", path));
                    nameValuePairs.add(new BasicNameValuePair("user", username));
                    if (validTime > 0)
                        nameValuePairs.add(new BasicNameValuePair("validtime", "" + validTime));
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs,"UTF-8"));
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
                            Log.d(TAG, "currentTagName:"+ curTagName);
                            if (curTagName != null)
                                isSuccess = true;
                        } else if (eventType == XmlPullParser.TEXT) {
                            if (curTagName != null) {
                                text = xpp.getText();
                                Log.d(TAG, "currentText:"+ text);
                                if (curTagName.equals("reason")) {
                                    mResult = text;
                                } else if(curTagName.equals("url")) {
                                    mUrls.add(text);
                                    Log.d(TAG, "share link: " + text);
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
        }

        return isSuccess;
    }

    public ArrayList<String> getFileShareLinks() {
        return mUrls;
    }

    public ArrayList<String> getFileAbsolutePaths() {
        return mAbsolutePaths;
    }

    public String getResult() {
        return mResult;
    }
}
