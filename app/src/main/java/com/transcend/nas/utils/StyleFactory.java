package com.transcend.nas.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.transcend.nas.R;

/**
 * Created by ike_lee on 2016/5/23.
 */
public class StyleFactory {
    private static Handler handler;
    private static Runnable runnable;

    public static void set_button_Drawable_left(final Context context,final Button button, final int imageID,final int spacing)
    {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if(button.getMeasuredWidth() == 0)
                {
                    handler.postDelayed(runnable, 0);
                }else{
                    Drawable drawable=context.getResources().getDrawable(imageID);
                    int width=button.getMeasuredWidth();
                    int height=button.getMeasuredHeight();

                    int txt_width=(int)(button.getTextSize()*button.getText().length()/2);
                    int txt_height=(int)(button.getLineCount()*button.getLineHeight());

                    int img_width=drawable.getIntrinsicWidth();
                    int img_height=drawable.getIntrinsicHeight();
                    int content_height=txt_height+img_height+spacing;
                    int content_width=txt_width+img_width+spacing;
                    int padding_w=width/2-content_width/2;
                    int padding_h=height/2-content_height/2;

                    button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                    button.setPadding(padding_w, 0, 0, 0);
                    button.setCompoundDrawablePadding(-padding_w);
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    public static void set_button_Drawable_right(final Context context,final Button button, final int imageID,final int spacing)
    {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if(button.getMeasuredWidth() == 0)
                {
                    handler.postDelayed(runnable, 0);
                }else{
                    Drawable drawable=context.getResources().getDrawable(imageID);
                    int width=button.getMeasuredWidth();
                    int height=button.getMeasuredHeight();

                    int txt_width=(int)(button.getTextSize()*button.getText().length()/2);
                    int txt_height=(int)(button.getLineCount()*button.getLineHeight());

                    int img_width=drawable.getIntrinsicWidth();
                    int img_height=drawable.getIntrinsicHeight();
                    int content_height=txt_height+img_height+spacing;
                    int content_width=txt_width+img_width+spacing;
                    int padding_w=width/2-content_width/2;
                    int padding_h=height/2-content_height/2;

                    button.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
                    button.setPadding(padding_w, 0, padding_w - spacing, 0);
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    public static void set_gray_button_touch_effect(final Context context, final Button button){
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        button.setBackgroundColor(ContextCompat.getColor(context, R.color.textColorPrimary));
                        break;
                    case MotionEvent.ACTION_UP:
                        button.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccentDialogButton));
                        break;
                }
                return false;
            }
        });
    }

    public static void set_white_button_touch_effect(final Context context, final Button button){
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        button.setBackgroundResource(R.drawable.button_press);
                        break;
                    case MotionEvent.ACTION_UP:
                        button.setBackgroundResource(R.drawable.button_normal);
                        break;
                }
                return false;
            }
        });
    }
}
