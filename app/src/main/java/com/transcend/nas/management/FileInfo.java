package com.transcend.nas.management;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.transcend.nas.NASUtils;
import com.transcend.nas.management.externalstorage.ExternalStorageController;
import com.transcend.nas.utils.MimeUtil;

import java.io.File;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by silverhsu on 16/1/25.
 */
public class FileInfo implements Serializable {

    public enum TYPE {
        DIR,
        PHOTO,
        VIDEO,
        MUSIC,
        FILE
    }

    public String path;
    public String name;
    public String time;
    public TYPE type;
    public Long size;
    private String lastModifiedTime;
    public boolean checked;

    public FileInfo() {

    }

    public static TYPE getType(String path) {
        if (MimeUtil.isPhoto(path))
            return TYPE.PHOTO;
        if (MimeUtil.isVideo(path))
            return TYPE.VIDEO;
        if (MimeUtil.isMusic(path))
            return TYPE.MUSIC;
        return TYPE.FILE;
    }

    public static String getTime(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return format.format(new Date(time));
    }

    public static Date getDate(String time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        try {
            return format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return new Date();
    }

    public void setLastModifiedTime(String time)
    {
        lastModifiedTime = time;
    }

    public String getLastModifiedTime()
    {
        return lastModifiedTime;
    }

    public boolean isLocalFile() {
        if (path != null) {
            return path.startsWith(Environment.getExternalStorageDirectory().getPath());
        }
        return false;
    }

    public void openLocalFile(Context context) {
        Uri fileUri = Uri.fromFile(new File(path));
        NASUtils.showAppChooser(context, fileUri);
    }

    public void openSDCardFile(Context context) {
        Uri uri = new ExternalStorageController(context).getSDFileUri(path);
        if (uri != null) {
            NASUtils.showAppChooser(context, uri);
        }
    }

}
