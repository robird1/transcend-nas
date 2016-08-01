package com.transcend.nas.settings;

import android.content.Context;
import java.util.Comparator;

/**
 * Created by ikelee on 16/8/1.
 */
public class DiskInfoSort {

    public static Comparator<DiskStructDevice> comparator() {
        return new DiskInfoSort.byName();
    }

    public static class byName implements Comparator<DiskStructDevice> {
        @Override
        public int compare(DiskStructDevice lhs, DiskStructDevice rhs) {
            return compareByName(lhs, rhs);
        }
    }

    private static int compareByName(DiskStructDevice lhs, DiskStructDevice rhs) {
        return lhs.infos.get("path").compareToIgnoreCase(rhs.infos.get("path"));
    }

}
