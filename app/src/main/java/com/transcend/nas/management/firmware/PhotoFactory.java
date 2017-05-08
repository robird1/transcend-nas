package com.transcend.nas.management.firmware;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.realtek.nasfun.api.Server;
import com.realtek.nasfun.api.ServerManager;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.management.FileInfo;

/**
 * Created by ike_lee on 2017/4/24.
 */
public class PhotoFactory {

    private static final String TAG = PhotoFactory.class.getSimpleName();
    private static PhotoFactory mPhotoFactory;
    private static final Object mMute = new Object();

    public PhotoFactory() {

    }

    public static PhotoFactory getInstance() {
        synchronized (mMute) {
            if (mPhotoFactory == null)
                mPhotoFactory = new PhotoFactory();
        }
        return mPhotoFactory;
    }

    public void displayPhoto(Context context, boolean thumbnail, String path, ImageView view) {
        displayPhoto(context, thumbnail, path, view, null);
    }

    public void displayPhoto(Context context, boolean thumbnail, String path, ImageView view, final ProgressBar progressBar) {
        if (path.startsWith(NASApp.ROOT_STG) || NASUtils.isSDCardPath(context, path)) {
            displayLocalPhoto(context, thumbnail, path, view);
        } else {
            displayRemotePhoto(context, thumbnail, path, view, progressBar);
        }
    }

    private void displayLocalPhoto(Context context, boolean thumbnail, String path, ImageView view) {
        String url = getPhotoPath(context, thumbnail, path);
        FileInfo.TYPE type = FileInfo.getType(path);
        switch (type) {
            case PHOTO:
                DisplayImageOptions options = new DisplayImageOptions.Builder()
                        .bitmapConfig(Bitmap.Config.RGB_565)
                        .cacheInMemory(true)
                        .cacheOnDisk(false)
                        .build();

                if (!url.startsWith("content://")) {
                    url = Uri.decode(url);
                }
                ImageLoader.getInstance().displayImage(url, view, options);
                break;
            case VIDEO:
                //TODO : load video thumbnail
                //MICRO_KIND, size: 96 x 96 thumbnail
                //Bitmap bmThumbnail = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MICRO_KIND);
                //view.setImageBitmap(bmThumbnail);
                ImageLoader.getInstance().cancelDisplayTask(view);
                break;
            case MUSIC:
                //TODO : load music thumbnail
                ImageLoader.getInstance().cancelDisplayTask(view);
                break;
            default:
                break;
        }
    }

    private void displayRemotePhoto(Context context, boolean thumbnail, String path, ImageView view, final ProgressBar progressBar) {
        String url = getPhotoPath(context, thumbnail, path);
        if (progressBar != null) {
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .cacheInMemory(true)
                    .cacheOnDisk(true)
                    .build();
            SimpleImageLoadingListener simpleImageLoadingListener = new SimpleImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    if (progressBar != null) {
                        progressBar.setProgress(0);
                        progressBar.setMax(100);
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    if (progressBar != null)
                        progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    if (progressBar != null)
                        progressBar.setVisibility(View.GONE);
                }
            };

            ImageLoadingProgressListener imageLoadingProgressListener = new ImageLoadingProgressListener() {
                @Override
                public void onProgressUpdate(String imageUri, View view, final int current, final int total) {
                    if (progressBar != null)
                        progressBar.setProgress(Math.round(100.0f * current / total));
                }
            };
            ImageLoader.getInstance().displayImage(url, view, options, simpleImageLoadingListener, imageLoadingProgressListener);
        } else {
            ImageLoader.getInstance().displayImage(url, view);
        }
    }

    public String getPhotoPath(Context context, boolean thumbnail, String path) {
        return getPhotoPath(context, false, thumbnail, path);
    }

    public String getPhotoPath(Context context, boolean forceLocal, boolean thumbnail, String path) {
        String url = "";
        if (path.startsWith(NASApp.ROOT_STG) || NASUtils.isSDCardPath(context, path)) {
            url = "file://" + path;
        } else {
            //try twonky image
            url = getTwonkyUrl(forceLocal, thumbnail, path);

            //try webdav image when twonky image empty
            if (null == url || "".equals(url)) {
                url = getWebDavUrl(forceLocal, thumbnail, path);
            }

            Log.d(TAG, "path : " + path + ", url : " + url);
        }
        return url;
    }

    private String getTwonkyUrl(boolean forceLocal, boolean thumbnail, String path) {
        Server server = ServerManager.INSTANCE.getCurrentServer();
        String username = server.getUsername();

        String realPath = ShareFolderManager.getInstance().getRealPath(path);
        if (path.equals(realPath) && path.startsWith("/" + username + "/"))
            realPath = "/home" + path;
        return TwonkyManager.getInstance().getPhotoUrl(server, forceLocal, thumbnail, realPath);
    }

    private String getWebDavUrl(boolean forceLocal, boolean thumbnail, String path) {
        String url = WebDavFactory.createUri(forceLocal, path).toString();
        if (thumbnail)
            url += "&thumbnail";
        else
            url += "&webview";
        return url;
    }
}
