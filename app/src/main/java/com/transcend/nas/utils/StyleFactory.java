package com.transcend.nas.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;

/**
 * Created by ike_lee on 2016/5/23.
 */
public class StyleFactory {
    private static Handler handler;
    private static Runnable runnable;

    public static void set_button_Drawable_center(final Context context,final Button button,final int imageID,final int spacing)
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
                    button.setPadding(padding_w,0,0,0);
                    button.setCompoundDrawablePadding(-padding_w);
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }
}
