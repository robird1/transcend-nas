package com.transcend.nas.introduce;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by ikelee on 16/8/22.
 */
public class BasicViewerPager extends ViewPager {

    public BasicViewerPager(Context context) {
        super(context);
    }

    public BasicViewerPager(Context context, AttributeSet attrs) {
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
