package com.transcend.nas.common;

import android.app.ActivityManager;
import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;

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

    public AnimationSet getTextAnimation(){
        AnimationSet set = new AnimationSet(true);

        Animation blink = getBlinkAnimation();
        set.addAnimation(blink);

        Animation scale = getScaleAnimation();
        set.addAnimation(scale);

        return set;
    }

    public Animation getBlinkAnimation() {
        Animation anim = new AlphaAnimation(1.0f, 0.0f);
        anim.setDuration(1000); //You can manage the blinking time with this parameter
        anim.setStartOffset(200);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        return anim;
    }

    public Animation getAlphaAnimation() {
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(1000);
        return anim;
    }

    public Animation getScaleAnimation() {
        Animation anim = new ScaleAnimation(0.5f, 1f, 0.5f, 1f);
        anim.setDuration(1000); //You can manage the blinking time with this parameter
        anim.setStartOffset(100);
        return anim;
    }


    public void setRollBackAnimation(View view){
        ViewCompat.animate(view)
                .rotation(0F)
                .withLayer()
                .setDuration(720L)
                .setInterpolator(new OvershootInterpolator(1F))
                .start();
    }

    public void setRollAnimation(View view){
        view.setRotation(0);
        ViewCompat.animate(view)
                .rotation(360F)
                .withLayer()
                .setDuration(720L)
                .setInterpolator(new OvershootInterpolator(1F))
                .start();
    }
}
