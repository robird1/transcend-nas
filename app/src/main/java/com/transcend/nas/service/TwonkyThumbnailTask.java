package com.transcend.nas.service;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ike_lee on 2016/10/25.
 */
public class TwonkyThumbnailTask extends AsyncTask<String, String, Boolean> {
    private static final String TAG = "TwonkyThumbnailTask";
    private String mImageUrl;
    private ImageView mImageView;

    public TwonkyThumbnailTask(String imageUrl, ImageView imageView) {
        mImageUrl = imageUrl;
        mImageView = imageView;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        String twonkyUrl = doGetRequest(mImageUrl);
        Log.d(TAG, twonkyUrl);
        if(twonkyUrl != null && !twonkyUrl.equals("")) {
            mImageUrl = twonkyUrl;
            return true;
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        String url = TwonkyManager.getInstance().convertUrlByLink(mImageUrl);
        ImageLoader.getInstance().displayImage(url, mImageView);
    }

    public String doGetRequest(String url) {
        HttpURLConnection conn = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            conn = (HttpURLConnection) realUrl.openConnection();

            int responseCode = conn.getResponseCode();// 调用此方法就不必再使用conn.connect()方法
            if (responseCode == 200) {
                InputStream is = conn.getInputStream();
                result = getStringFromInputStream(is);
            } else {
                Log.d(TAG, "访问失败 " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (conn != null) {
                conn.disconnect();// 关闭连接
            }
        }

        return result;
    }

    private String getStringFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        // 模板代码 必须熟练
        byte[] buffer = new byte[1024];
        int len = -1;
        // 一定要写len=is.read(buffer)
        // 如果while((is.read(buffer))!=-1)则无法将数据写入buffer中
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        is.close();
        String state = os.toString();// 把流中的数据转换成字符串,采用的编码是utf-8(模拟器默认编码)
        os.close();
        return state;
    }
}
