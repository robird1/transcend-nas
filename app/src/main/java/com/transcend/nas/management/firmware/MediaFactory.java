package com.transcend.nas.management.firmware;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.common.images.WebImage;
import com.transcend.nas.NASApp;
import com.transcend.nas.NASUtils;
import com.transcend.nas.management.externalstorage.ExternalStorageController;
import com.transcend.nas.utils.MimeUtil;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created by silverhsu on 16/1/25.
 */
public class MediaFactory {
    private static final String TAG = MediaFactory.class.getSimpleName();
    private static final List<String> CHROMECAST_MEDIA_SUPPORT_FORMAT = Arrays.asList("aac", "mp3", "mp4", "wav", "webm");

    public static void open(Activity act, String path) {
        Uri uri = createUri(act, path);
        String name = FilenameUtils.getName(path);
        String type = MimeUtil.getMimeType(path);
        openIn(act, uri, type, name);
    }

    public static Uri createUri(Context context, String path) {
        return createUri(context, false, path);
    }

    public static Uri createUri(Context context, boolean forceLocal, String path) {
        Uri uri;
        if (path.startsWith(NASApp.ROOT_STG)) {
            uri = Uri.fromFile(new File(path));
        } else if (NASUtils.isSDCardPath(context, path)) {
            uri = new ExternalStorageController(context).getSDFileUri(path);
        } else {
            uri = WebDavFactory.createUri(forceLocal, path);
        }
        return uri;
    }

    public static MediaInfo createChromeCastMediaInfo(Context context, int mediaType, String path) {
        MediaInfo info = null;
        String ext = FilenameUtils.getExtension(path);
        if (ext != null && CHROMECAST_MEDIA_SUPPORT_FORMAT.contains(ext.toLowerCase())) {
            String uri = createUri(context, true, path).toString();
            String type = MimeUtil.getMimeType(path);
            MediaMetadata metadata = new MediaMetadata(mediaType);
            metadata.putString(MediaMetadata.KEY_TITLE, FilenameUtils.getName(path));
            metadata.addImage(new WebImage(Uri.parse(PhotoFactory.getInstance().getPhotoPath(context, true, true, path))));

            info = new MediaInfo.Builder(uri)
                    .setContentType(type)
                    .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                    .setMetadata(metadata)
                    .build();
        }

        return info;
    }

    private static void openIn(Activity act, Uri uri, String type, String title) {
        Intent intent = NASUtils.getIntentUri(act, uri);
        intent.putExtra("title", title);
        try {
            act.startActivityForResult(intent, 0);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(act, "No suitable app", Toast.LENGTH_SHORT).show();
        }
    }
}
