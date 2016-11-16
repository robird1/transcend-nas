package com.transcend.nas.firmware_api;

import android.util.Log;

import com.realtek.nasfun.api.HttpClientManager;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ike_lee on 2016/11/15.
 */
public class ShareFolderManager {
    private static final String TAG = ShareFolderManager.class.getSimpleName();
    private static ShareFolderManager mShareFolderManager;
    private static final Object mMute = new Object();
    private static final int defaultLifeCycle = 10;

    private Map<String, String> mPathMap;
    private int mMapLifeCycle = 0;

    public ShareFolderManager() {
        mPathMap = new HashMap<String, String>();
        mMapLifeCycle = 0;
    }

    public static ShareFolderManager getInstance() {
        synchronized (mMute) {
            if (mShareFolderManager == null)
                mShareFolderManager = new ShareFolderManager();
        }
        return mShareFolderManager;
    }

    public void updateSharedFolder() {
        if (!checkMapLifeCycle()) {
            cleanRealPathMap();
            if(getSharedList()) {
                Log.d(TAG, "shared folder map update success");
                mMapLifeCycle = defaultLifeCycle;
            }
        }
        Log.d(TAG, "shared folder map size : " + getMapSize());
    }

    public void addMap(String key, String path) {
        if (mPathMap == null)
            mPathMap = new HashMap<String, String>();
        if (!key.startsWith("/"))
            key = "/" + key + "/";
        if (!path.endsWith("/"))
            path = path + "/";
        mPathMap.put(key, path);
    }

    public int getMapSize() {
        int size = 0;
        if (mPathMap != null)
            size = mPathMap.size();
        return size;
    }

    public List<String> getAllKey() {
        List<String> list = new ArrayList<>();
        if (mPathMap != null) {
            for (String key : mPathMap.keySet())
                list.add(key);
        }

        return list;
    }

    public List<String> getAllValue() {
        List<String> list = new ArrayList<>();
        if (mPathMap != null && mPathMap.size() > 0) {
            for (String key : mPathMap.keySet())
                list.add(mPathMap.get(key));
        }

        return list;
    }

    public String getKey(String path) {
        String result = "";
        if (mPathMap != null) {
            for (String key : mPathMap.keySet()) {
                if (path.startsWith(key)) {
                    result = key;
                    break;
                }
            }
        }
        return result;
    }

    public String getValue(String path) {
        String realPath = "";
        if (mPathMap != null) {
            for (String key : mPathMap.keySet()) {
                if (path.startsWith(key)) {
                    realPath = mPathMap.get(key);
                    break;
                }
            }
        }
        return realPath;
    }

    public String getRealPath(String path) {
        String key = getKey(path);
        String value = getValue(path);
        if (key != null && !key.equals(""))
            path = path.replaceFirst(key, value);
        return path;
    }

    public boolean checkMapLifeCycle() {
        mMapLifeCycle--;
        if (mMapLifeCycle > 0)
            return true;
        else {
            mMapLifeCycle = defaultLifeCycle;
            return false;
        }
    }

    public void cleanRealPathMap() {
        if (mPathMap != null)
            mPathMap.clear();
        mMapLifeCycle = 0;
    }

    private boolean getSharedList() {
        boolean isSuccess = false;
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        String hash = server.getHash();
        DefaultHttpClient httpClient = HttpClientManager.getClient();
        String commandURL = "http://" + hostname + "/nas/get/sharelist";
        Log.d(TAG, commandURL);

        HttpResponse response = null;
        InputStream inputStream = null;
        try {
            do {
                HttpPost httpPost = new HttpPost(commandURL);
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("hash", hash));
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
                String name = null;
                String path = null;
                boolean add = false;

                do {
                    String tagName = xpp.getName();
                    if (eventType == XmlPullParser.START_TAG) {
                        curTagName = tagName;
                        if (curTagName.equals("sharelist")) {
                            isSuccess = true;
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        if (curTagName != null) {
                            text = xpp.getText();
                            if (curTagName.equals("name")) {
                                name = text;
                            } else if (curTagName.equals("path")) {
                                path = text;
                                if (name != null) {
                                    if (add) {
                                        addMap(name, path);
                                    }
                                    name = null;
                                    path = null;
                                    add = false;
                                }
                            } else if (curTagName.equals("services")) {
                                if (text.equals("smb")) {
                                    add = true;
                                }
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
                isSuccess = false;
                e.printStackTrace();
            }
        }

        return isSuccess;
    }
}
