package com.transcend.nas.utils;

import org.apache.commons.io.FilenameUtils;

/**
 * Created by silverhsu on 16/1/25.
 */
public class MimeUtil {

    private static final String IMAGE = "image";
    private static final String VIDEO = "video";
    private static final String AUDIO = "audio";


    public static boolean isPhoto(String path) {
        String mime = getMimeType(path);
        if (mime == null)
            return false;
        return mime.contains(IMAGE);
    }

    public static boolean isVideo(String path) {
        String mime = getMimeType(path);
        if (mime == null)
            return false;
        return mime.contains(VIDEO);
    }

    public static boolean isMusic(String path) {
        String mime = getMimeType(path);
        if (mime == null)
            return false;
        return mime.contains(AUDIO);
    }

    public static String getMimeType(String path) {
        String ext = FilenameUtils.getExtension(path);
        if (ext != null) {
            return MimeTypeMapExt.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
        }
        return null;
    }

}
