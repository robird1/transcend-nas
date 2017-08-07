package com.transcend.nas.management.upload;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.transcend.nas.management.upload.FileUploadLoader;

import org.apache.commons.io.FilenameUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by ikelee on 17/8/3.
 */
public class UriFileUploadLoader extends FileUploadLoader {

    public UriFileUploadLoader(Context context, List<Uri> uris, String dest) {
        super(context, uris, dest);
    }

    private Uri convertObject(Object src) {
        try {
            return (Uri) src;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected boolean isDirectory(Object src) {
        return false;
    }

    @Override
    protected List listDirectory(Object src) {
        return null;
    }

    @Override
    protected String parserFileName(Object src) {
        Uri uri = convertObject(src);
        Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            String name = uri.getPath();
            return FilenameUtils.getName(name);
        } else {
            try {
                if (cursor.moveToFirst()) {
                    String name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    return FilenameUtils.getName(name);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        return null;
    }

    @Override
    protected InputStream createInputStream(Object src) {
        Uri uri = convertObject(src);
        ContentResolver cr = getContext().getContentResolver();
        try {
            InputStream source = cr.openInputStream(uri);
            return source;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
