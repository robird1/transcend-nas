package com.transcend.nas.utils;

import java.text.DecimalFormat;

/**
 * Created by silverhsu on 16/2/15.
 */
public class MathUtil {

    public static final long KB = 1000;
    public static final long MB = KB * KB;
    public static final long GB = MB * KB;
    public static final long TB = GB * KB;

    public static String getBytes(long number) {
        long[] dividers = { TB, GB, MB, KB, 1 };
        String[] units = { "TB", "GB", "MB", "KB", "B" };
        for (int i = 0; i < dividers.length; i++) {
            if (number >= dividers[i]) {
                double value = (double) number / (double) dividers[i];
                DecimalFormat df = new DecimalFormat("#,##0.#");
                return String.format("%s%s", df.format(value), units[i]);
            }
        }
        return "0B";
    }

}
