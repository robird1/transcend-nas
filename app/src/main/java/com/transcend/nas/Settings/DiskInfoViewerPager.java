package com.transcend.nas.settings;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by silverhsu on 16/2/23.
 */
public class DiskInfoViewerPager extends ViewPager {

    public DiskInfoViewerPager(Context context) {
        super(context);
    }

    public DiskInfoViewerPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // FIX PINCH-TO-ZOOM ISSUE ON android.support.v4.view.ViewPager
        try {
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // FIX PINCH-TO-ZOOM ISSUE ON android.support.v4.view.ViewPager
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return false;
    }

}
