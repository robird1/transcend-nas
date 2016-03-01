package com.transcend.nas.viewer;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by silverhsu on 16/2/23.
 */
public class ViewerPager extends ViewPager {

    public ViewerPager(Context context) {
        super(context);
    }

    public ViewerPager(Context context, AttributeSet attrs) {
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
