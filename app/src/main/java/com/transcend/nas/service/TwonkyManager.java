package com.transcend.nas.service;

import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASPref;
import com.transcend.nas.common.FileFactory;
import com.transcend.nas.management.FileInfo;
import com.tutk.IOTC.P2PService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TwonkyManager {
    private static final String TAG = "TwonkyManager";
    private static final Object mMute = new Object();
    private static TwonkyManager mTwonkyManager;

    private String KEYWORD_FOLDER = "TWONKY_FOLDER";
    private HashMap<String, String> mImageMap;
    private HashMap<String, String> mFolderMap;
    private Set<String> mCacheMap;
    //TODO : check the size

    public TwonkyManager() {
        mImageMap = new HashMap<>();
        mFolderMap = new HashMap<>();
        mCacheMap = new HashSet<>();
    }

    public static TwonkyManager getInstance() {
        synchronized (mMute) {
            if (mTwonkyManager == null)
                mTwonkyManager = new TwonkyManager();
        }
        return mTwonkyManager;
    }

    public void cleanTwonkyMap() {
        if (mCacheMap != null)
            mCacheMap.clear();
        if (mFolderMap != null)
            mFolderMap.clear();
        if (mImageMap != null)
            mImageMap.clear();
    }

    public boolean startTwonkyParser(String path, int start, int count) {
        String key = FileFactory.getInstance().getRealPathKeyFromMap(path);
        String realPath = FileFactory.getInstance().getRealPathFromMap(path);
        if (key != null && !key.equals(""))
            path = path.replaceFirst(key, realPath);
        path = path.replaceFirst("/home/", "/");
        Log.d(TAG, "twonky origin path : " + path);

        if (mCacheMap.contains(path)) {
            Log.d(TAG, "twonky cache contain : " + path);
            return true;
        }

        String[] paths = path.split("/");
        int length = paths.length;
        Log.d(TAG, "twonky origin path length : " + length);
        if (length == 0) {
            //root folder, get twonky server
            String server = parserTwonkyServer();
            if (server != null && !server.equals("")) {
                //get twonky category "Photo"
                String photos = parserTwonkyCategory(server, "Photos", "?start=" + start + "&fmt=json");
                if (photos != null && !photos.equals("")) {
                    //get twonky category "By Folder"
                    String byFolder = parserTwonkyCategory(photos, "By Folder", "?start=" + start + "&fmt=json");
                    Log.d(TAG, "twonky byFolder " + byFolder);
                    if (byFolder != null && !byFolder.equals("")) {
                        mFolderMap.put(KEYWORD_FOLDER, byFolder);
                        mCacheMap.add(path);
                        return true;
                    }
                }
            }
        } else if (0 < length && length <= 2) {
            return parserTwonkyFolder(path, start, count, getFolderUrlFromMap(false, KEYWORD_FOLDER));
        } else {
            //other folder
            String currentUrl = "";
            String tmp = "/" + paths[1] + "/";
            if(!mCacheMap.contains(tmp))
                parserTwonkyFolder(tmp, start, count, getFolderUrlFromMap(false, KEYWORD_FOLDER));

            //loop check the folder
            for (int i = 2; i < length; i++) {
                tmp += paths[i] + "/";
                Log.d(TAG, "twonky folder path : " + tmp);

                String nextUrl = getFolderUrlFromMap(false, tmp);
                if (nextUrl == null || nextUrl.equals("")) {
                    parserTwonkyFolder(tmp.replaceFirst(paths[i]+"/" , ""), start, count, currentUrl);
                    nextUrl = getFolderUrlFromMap(false, tmp);
                }

                Log.d(TAG, "twonky folder url : " + nextUrl);

                if (nextUrl != null && !"".equals(nextUrl)) {
                    currentUrl = nextUrl;
                } else {
                    return false;
                }
            }

            return parserTwonkyFolder(path, start, count, getFolderUrlFromMap(false, path));
        }

        return false;
    }

    public String getUrlFromMap(boolean convertLink, FileInfo.TYPE type, String path) {
        if (type.equals(FileInfo.TYPE.DIR))
            return getFolderUrlFromMap(convertLink, path);
        else if (type.equals(FileInfo.TYPE.PHOTO))
            return getImageUrlFromMap(convertLink, path);
        else
            return null;
    }

    private String getFolderUrlFromMap(boolean convertLink, String path) {
        String result = "";
        if (mFolderMap != null) {
            result = mFolderMap.get(path);
            if (convertLink && result != null && !result.equals(""))
                result = convertUrlByLink(result);
        }
        return result;
    }

    private String getImageUrlFromMap(boolean convertLink, String path) {
        String key = FileFactory.getInstance().getRealPathKeyFromMap(path);
        String realPath = FileFactory.getInstance().getRealPathFromMap(path);
        if (key != null && !key.equals(""))
            path = path.replaceFirst(key, realPath);
        path = path.replaceFirst("/home/", "/");

        String result = "";
        if (mImageMap != null) {
            result = mImageMap.get(path);
            if (convertLink && result != null && !result.equals(""))
                result = convertUrlByLink(result);
        }
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

    public String doTwonkyRescan(boolean force) {
        //TODO: add lifecycle check
        if (!force && mCacheMap.contains(NASApp.ROOT_SMB))
            return null;

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
        String newUrl = convertUrlByLink(url) + param;
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

    public boolean parserTwonkyFolder(String path, int start, int count, String url) {
        if (url != null && !url.equals("")) {
            String newUrl = convertUrlByLink(url) + "?start=" + start + "&count=" + count + "&fmt=json";
            Log.d(TAG, newUrl);
            String result = doGetRequest(newUrl);
            String target = "";
            try {
                JSONObject obj = new JSONObject(result);
                String folderChildCount = obj.optString("childCount");
                JSONArray items = new JSONArray(obj.optString("item"));
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    String title = item.optString("title");
                    String metaString = item.optString("meta");
                    if (metaString != null && !metaString.equals("")) {
                        JSONObject meta = new JSONObject(metaString);
                        String childCount = meta.optString("childCount");
                        if (childCount != null && !childCount.equals("")) {
                            //folder item, record it to folder hash map
                            JSONObject enclosure = new JSONObject(item.optString("enclosure"));
                            target = enclosure.optString("url");
                            mFolderMap.put(path + title + "/", target);
                            //Log.d(TAG, "key: " + path + title + "/, value: " + target);
                        } else {
                            //image item, record it to image hash map
                            String extension = meta.optString("pv:extension");
                            JSONArray res = new JSONArray(meta.optString("res"));
                            for (int j = 0; j < res.length(); j++) {
                                JSONObject tmp = res.optJSONObject(j);
                                target = tmp.optString("value");
                                if (target.contains("?"))
                                    target = target.substring(0, target.indexOf('?'));
                                mImageMap.put(path + title, target);
                                //Log.d(TAG, "key: " + path + title + ", value: " + target);
                            }
                        }
                    }
                }
                Log.d(TAG, "twonky folder size : " + mFolderMap.size());
                Log.d(TAG, "twonky image size : " + mImageMap.size());
                int nextStart = start + count;
                if (Integer.parseInt(folderChildCount) > nextStart)
                    return parserTwonkyFolder(path, nextStart, count, url);
                else {
                    mCacheMap.add(path);
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();

            }
        }

        return false;
    }

    public String convertUrlByLink(String url) {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String hostname = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.TWONKY);
        String[] splits = url.split("/");
        String newUrl = "";
        boolean add = false;

        for (String split : splits) {
            if (add) {
                newUrl = newUrl + "/" + split;
            } else {
                if (split.contains(":9000")) {
                    newUrl = "http://" + hostname;
                    add = true;
                }
            }
        }
        return newUrl;
    }
}
