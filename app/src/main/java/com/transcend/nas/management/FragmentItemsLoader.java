package com.transcend.nas.management;

import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.content.Context;
import android.support.v4.util.TimeUtils;
import android.util.Log;

import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.utils.MathUtil;
import com.tutk.IOTC.P2PService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.os.Build.VERSION_CODES.M;
import static com.transcend.nas.R.string.device;
import static com.transcend.nas.R.string.login;
import static jcifs.Config.getBoolean;

/**
 * Created by steve_su on 2017/6/5.
 */

public class FragmentItemsLoader extends AsyncTaskLoader<ArrayList<FileInfo>> {

    private static final String TAG = FragmentItemsLoader.class.getSimpleName();
    static final int COUNT = 100;
    private static final String PATH = "/";
    private Bundle mArgs;
    private int mStart;
    private int mLoadedCount;
    private int mPosition;


    public FragmentItemsLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public ArrayList<FileInfo> loadInBackground() {
        Log.d(TAG, "[Enter] loadInBackground ");
        if (mArgs == null) {
            return new ArrayList<>();
        }
        int start = mArgs.getInt("start", 0);
        mStart = start;
        String fragmentType = mArgs.getString("fragment_type");
        Log.d(TAG, "fragmentType: "+ fragmentType+ " start: "+ start);

        mPosition = mArgs.getInt("position", 0);

//        int count = mArgs.getInt("count", 0);
        int type = mArgs.getInt("type", -1);
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String ip = P2PService.getInstance().getIP(server.getHostname(), P2PService.P2PProtocalType.HTTP);
        String userName = server.getUsername();
        String hash = server.getHash();

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        String content = "hash="+ hash+ "&fmt=json&start="+ start+ "&count="+ COUNT+ "&login="+ userName+ "&path=%2F&type="+ type;
        RequestBody body = RequestBody.create(mediaType, content);
        Request request = new Request.Builder()
                .url("http://"+ ip+ "/nas/get/twonky")
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("cache-control", "no-cache")
                .addHeader("postman-token", "980b2146-1fd5-baa8-1eb9-db0b39f9dcb0")
                .build();

        ArrayList<FileInfo> list = new ArrayList();
        try {
            Response response = client.newCall(request).execute();
            JSONObject jObject = new JSONObject(response.body().string());
            JSONArray jArray = jObject.getJSONArray("items");
            Log.d(TAG, "jArray.length(): "+ jArray.length());
            mLoadedCount = jArray.length();

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject object = jArray.getJSONObject(i);
                FileInfo info = new FileInfo();
                info.path = object.optString("local_path").replace("/home", "");
                info.name = object.optString("title");
                info.type = FileInfo.getType(info.path);
                long temp = object.optLong("modifiedTime");
//                Log.d(TAG, "temp time: "+ temp);
                info.time = FileInfo.getTime(temp);
                info.size = object.optLong("size");
//                Log.d(TAG, "info.path: "+ info.path);
//                Log.d(TAG, "info.name: "+ info.name);
//                Log.d(TAG, "info.type: "+ info.type);
//                Log.d(TAG, "info.time: "+ info.time);

                list.add(info);

//                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            Log.d(TAG, "[Enter] JSONException====================================================");
            Log.d(TAG, "e.printStackTrace(): "+ e.getMessage());
            e.printStackTrace();
        }

        return list;
    }

    @Override
    protected void onStartLoading() {
        Log.d(TAG, "[Enter] onStartLoading");
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        Log.d(TAG, "[Enter] onStopLoading");
        cancelLoad();
    }

    boolean isLazyLoading() {
        return mArgs.getBoolean("is_lazy_loading", false);
    }

    int nextStartIndex() {
        return mStart + mLoadedCount;
    }

    int getPosition() {
        return mPosition;
    }

}
