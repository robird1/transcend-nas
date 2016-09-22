package com.transcend.nas.service;

import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.tutk.IOTC.P2PService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

public class TwonkyManager {
    private static final String TAG = "TwonkyManager";
    private static final Object mMute = new Object();
    private static TwonkyManager mTwonkyManager;

    private String KEYWORD_FOLDER = "TWONKY_FOLDER";
    private String mFolderUrl = "";
    private HashMap<String, String> mImageMap;
    private HashMap<String, String> mFolderMap;
    private final int mSize = 50;
    //TODO : check the size

    public TwonkyManager() {
        mImageMap = new HashMap<>();
        mFolderMap = new HashMap<>();
    }

    public static TwonkyManager getInstance() {
        synchronized (mMute) {
            if (mTwonkyManager == null)
                mTwonkyManager = new TwonkyManager();
        }
        return mTwonkyManager;
    }

    public boolean startTwonkyParser(String path){
        return startTwonkyParser(path, 0, mSize);
    }

    public boolean startTwonkyParser(String path, int startIndex, int size) {
        String[] paths = path.split("/");
        int length = paths.length;
        if (length == 0) {
            //get twonky server
            String server = parserTwonkyServer();
            Log.d(TAG, "twonky server " + server);
            if (server != null && !server.equals("")) {
                //get twonky category "Photo"
                String photos = parserTwonkyCategory(server, "Photos", "?start="+startIndex+"&fmt=json");
                Log.d(TAG, "twonky photos " + photos);
                if (photos != null && !photos.equals("")) {
                    //get twonky category "By Folder"
                    String byFolder = parserTwonkyCategory(photos, "By Folder", "?start="+startIndex+"&fmt=json");
                    Log.d(TAG, "twonky byFolder " + byFolder);
                    if (byFolder != null && !byFolder.equals("")) {
                        mFolderMap.put(KEYWORD_FOLDER, byFolder);
                        return true;
                    }
                }
            }
        } else if (0 < length && length <= 2) {
            return parserTwonkyFolder(getFolderUrlFromMap(KEYWORD_FOLDER), "?start="+startIndex+"&count="+size+"&fmt=json");
        } else {
            return parserTwonkyFolder(getFolderUrlFromMap(paths[length-1]), "?start="+startIndex+"&count="+size+"&fmt=json");
        }

        return false;
    }

    public String getFolderUrlFromMap(String path){
        String result = "";
        if(mFolderMap != null)
            result = mFolderMap.get(path);
        return result;
    }

    public String getImageUrlFromMap(String path){
        String result = "";
        if(mImageMap != null)
            result = mImageMap.get(path);
        return result;
    }

    private String doGetRequest(String url) {
        HttpURLConnection conn = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            conn = (HttpURLConnection) realUrl.openConnection();
            conn.setRequestProperty("content-type", "application/json");
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);

            int responseCode = conn.getResponseCode();
            //Log.i(TAG, "url " + url);
            //Log.i(TAG, "responseCode: " + responseCode);
            if (responseCode == 200 || responseCode == 201) {
                InputStream is = conn.getInputStream();
                result = getStringFromInputStream(is);
            } else {
                Log.i(TAG, "error " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return result;
    }

    private String getStringFromInputStream(InputStream is)
            throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        is.close();
        String state = os.toString();
        os.close();
        return state;
    }

    public String doTwonkyRescan() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.TWONKY);
        String value = "http://" + hostname + "/rpc/rescan";
        HttpURLConnection conn = null;
        String result = "";
        try {
            URL realUrl = new URL(value);
            conn = (HttpURLConnection) realUrl.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);

            int responseCode = conn.getResponseCode();
            Log.i(TAG, "url " + value);
            Log.i(TAG, "responseCode: " + responseCode);
            if (responseCode == 200 || responseCode == 201) {
                InputStream is = conn.getInputStream();
                result = getStringFromInputStream(is);
            } else {
                Log.i(TAG, "error " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return result;
    }

    public String parserTwonkyServer() {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.TWONKY);
        String value = "http://" + hostname + "/nmc/rss/server?start=0&fmt=json";
        String result = doGetRequest(value);

        String target = "";
        try {
            JSONObject obj = new JSONObject(result);
            JSONArray items = new JSONArray(obj.optString("item"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                JSONObject serverJson = new JSONObject(item.optString("server"));
                String[] baseURLs = serverJson.optString("baseURL").split("/");
                String baseURL = "";
                for (String base : baseURLs) {
                    if (base.contains(":9000")) {
                        baseURL = base;
                        break;
                    }
                }

                JSONObject enclosureJson = new JSONObject(item.optString("enclosure"));
                String url = enclosureJson.optString("url");
                if (!baseURL.equals("") && url.contains(baseURL)) {
                    target = url;
                    break;
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return target;
    }

    public String parserTwonkyCategory(String url, String keyword, String param) {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.TWONKY);
        String[] splits = url.split("/");
        String newUrl = "";
        boolean start = false;

        for (String split : splits) {
            if (start) {
                newUrl = newUrl + "/" + split;
            } else {
                if (split.contains(":9000")) {
                    newUrl = "http://" + hostname;
                    start = true;
                }
            }
        }
        newUrl += param;
        String result = doGetRequest(newUrl);

        String target = "";
        try {
            JSONObject obj = new JSONObject(result);
            JSONArray items = new JSONArray(obj.optString("item"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                String title = item.optString("title");
                if (title.equals(keyword)) {
                    JSONObject enclosureJson = new JSONObject(item.optString("enclosure"));
                    target = enclosureJson.optString("url");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return target;
    }

    public boolean parserTwonkyFolder(String url, String param) {
        if(url != null && !url.equals("")) {
            Server server = ServerManager.INSTANCE.getCurrentServer();
            String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.TWONKY);
            String[] splits = url.split("/");
            String newUrl = "";
            boolean start = false;

            for (String split : splits) {
                if (start) {
                    newUrl = newUrl + "/" + split;
                } else {
                    if (split.contains(":9000")) {
                        newUrl = "http://" + hostname;
                        start = true;
                    }
                }
            }
            newUrl += param;
            Log.d(TAG, newUrl);
            String result = doGetRequest(newUrl);
            String target = "";
            try {
                JSONObject obj = new JSONObject(result);
                JSONArray items = new JSONArray(obj.optString("item"));
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    String title = item.optString("title");
                    String metaString = item.optString("meta");
                    if(metaString != null && !metaString.equals("")) {
                        JSONObject meta = new JSONObject(metaString);
                        String childCount = meta.optString("childCount");
                        if (childCount != null && !childCount.equals("")) {
                            //folder item, record it to folder hash map
                            JSONObject enclosure = new JSONObject(item.optString("enclosure"));
                            target = enclosure.optString("url");
                            mFolderMap.put(title, target);
                        } else {
                            //image item, record it to image hash map
                            JSONArray res = new JSONArray(meta.optString("res"));
                            for (int j = 0; j < res.length(); j++) {
                                JSONObject tmp = res.optJSONObject(j);
                                String size = tmp.optString("size");
                                target = tmp.optString("value");
                                if (target.contains("?"))
                                    target = target.substring(0, target.indexOf('?'));
                                mImageMap.put(title, target);
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "twonky folder size : " + mFolderMap.size());
            Log.d(TAG, "twonky image size : " + mImageMap.size());
            return true;
        }

        return false;

    }
}
