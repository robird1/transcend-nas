package com.transcend.nas.common;

import android.app.ActivityManager;
import android.content.Context;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.transcend.nas.NASApp;

import java.io.File;

/**
 * Created by ike_lee on 2016/8/18.
 */
public class AnimFactory {
    private static final String TAG = AnimFactory.class.getSimpleName();
    private static AnimFactory mAnimFactory;
    private static final Object mMute = new Object();

    public AnimFactory() {
    }

    public static AnimFactory getInstance() {
        synchronized (mMute) {
            if (mAnimFactory == null)
                mAnimFactory = new AnimFactory();
        }
        return mAnimFactory;
    }

    public Animation getBlinkAnimation(){
        Animation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setDuration(1000); //You can manage the blinking time with this parameter
        anim.setStartOffset(100);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        return anim;
    }
}
