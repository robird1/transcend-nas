package com.transcend.nas.management;

import android.content.Context;

import com.transcend.nas.NASPref;
import com.transcend.nas.utils.MimeTypeMapExt;
import com.transcend.nas.utils.MimeUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.util.Comparator;

/**
 * Created by silverhsu on 16/1/26.
 */
public class FileInfoSort {


    public static Comparator<FileInfo> comparator(Context context) {
        NASPref.Sort sort = NASPref.getFileSort(context);
        if (sort.equals(NASPref.Sort.DATE)) {
            return new FileInfoSort.byDate();
        }
        if (sort.equals(NASPref.Sort.NAME)) {
            return new FileInfoSort.byName();
        }
        return new FileInfoSort.byType();
    }

    public static class byType implements Comparator<FileInfo> {

        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            int result = 0;
            if (lhs.type.equals(rhs.type)) {
                result = compareByExtension(lhs, rhs);
                if (result == 0)
                    result = compareByName(lhs, rhs);
            }
            else {
                result =  compareByType(lhs, rhs);
            }
            return result;
        }

    }

    public static class byDate implements Comparator<FileInfo> {

        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            int result = 0;
            if (lhs.time.equals(rhs.time)) {
                result = compareByName(lhs, rhs);
            }
            else {
                result = compareByDate(lhs, rhs);
            }
            return result;
        }

    }

    public static class byName implements Comparator<FileInfo> {

        @Override
        public int compare(FileInfo lhs, FileInfo rhs) {
            return compareByName(lhs, rhs);
        }

    }

    private static int compareByType(FileInfo lhs, FileInfo rhs) {
        return lhs.type.compareTo(rhs.type);
    }

    private static int compareByExtension(FileInfo lhs, FileInfo rhs) {
        return FilenameUtils.getExtension(lhs.name).compareTo(FilenameUtils.getExtension(rhs.name));
    }

    private static int compareByName(FileInfo lhs, FileInfo rhs) {
        return lhs.name.compareToIgnoreCase(rhs.name);
    }

    private static int compareByDate(FileInfo lhs, FileInfo rhs) {
        return FileInfo.getDate(lhs.time).compareTo(FileInfo.getDate(rhs.time));
    }

}
