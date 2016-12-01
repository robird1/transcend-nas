package com.transcend.nas.common;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.transcend.nas.R;

import org.w3c.dom.Text;

import static com.transcend.nas.introduce.IntroduceActivity.calculateInSampleSize;

/**
 * Created by ike_lee on 2016/5/23.
 */
public class StyleFactory {
    private static Handler handler;
    private static Runnable runnable;

    private enum TouchColor {
        WHITE, BLUE, RED
    }

    public static void set_button_Drawable_left(final Context context, final Button button, final int imageID, final int spacing) {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (button.getMeasuredWidth() == 0) {
                    handler.postDelayed(runnable, 0);
                } else {
                    Drawable drawable = context.getResources().getDrawable(imageID);
                    int width = button.getMeasuredWidth();
                    int height = button.getMeasuredHeight();

                    int txt_width = (int) (button.getTextSize() * button.getText().length() / 2);
                    int txt_height = (int) (button.getLineCount() * button.getLineHeight());

                    int img_width = drawable.getIntrinsicWidth();
                    int img_height = drawable.getIntrinsicHeight();
                    int content_height = txt_height + img_height + spacing;
                    int content_width = txt_width + img_width + spacing;
                    int padding_w = width / 2 - content_width / 2;
                    int padding_h = height / 2 - content_height / 2;

                    button.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                    button.setPadding(padding_w, 0, 0, 0);
                    button.setCompoundDrawablePadding(-padding_w);
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    public static void set_button_Drawable_right(final Context context, final Button button, final int imageID, final int spacing) {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (button.getMeasuredWidth() == 0) {
                    handler.postDelayed(runnable, 0);
                } else {
                    Drawable drawable = context.getResources().getDrawable(imageID);
                    int width = button.getMeasuredWidth();
                    int height = button.getMeasuredHeight();

                    int txt_width = (int) (button.getTextSize() * button.getText().length() / 2);
                    int txt_height = (int) (button.getLineCount() * button.getLineHeight());

                    int img_width = drawable.getIntrinsicWidth();
                    int img_height = drawable.getIntrinsicHeight();
                    int content_height = txt_height + img_height + spacing;
                    int content_width = txt_width + img_width + spacing;
                    int padding_w = width / 2 - content_width / 2;
                    int padding_h = height / 2 - content_height / 2;

                    button.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
                    button.setPadding(padding_w, 0, padding_w - spacing, 0);
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    public static void set_gray_button_touch_effect(final Context context, final Button button) {
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

    public static void set_white_button_touch_effect(Context context, Button button) {
        set_button_touch_effect(context, button, TouchColor.WHITE);
    }

    public static void set_blue_button_touch_effect(Context context, Button button) {
        set_button_touch_effect(context, button, TouchColor.BLUE);
    }

    public static void set_red_button_touch_effect(Context context, Button button) {
        //button.setTextColor(Color.BLACK);
        set_button_touch_effect(context, button, TouchColor.RED);
    }

    public static void set_button_touch_effect(final Context context, final Button button, final TouchColor color) {
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        button.setBackgroundResource(R.drawable.button_press);
                        break;
                    case MotionEvent.ACTION_UP:
                        switch (color) {
                            case RED:
                                button.setBackgroundResource(R.drawable.button_normal_red);
                                break;
                            case BLUE:
                                button.setBackgroundResource(R.drawable.button_normal_blue);
                                break;
                            default:
                                button.setBackgroundResource(R.drawable.button_normal);
                                break;
                        }
                        break;
                }
                return false;
            }
        });
    }

    public static void set_lt_gray_button_touch_effect(final Context context, final Button button) {
        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        button.setBackgroundResource(R.drawable.button_gray_press);
                        break;
                    case MotionEvent.ACTION_UP:
                        button.setBackgroundResource(R.drawable.button_gray);
                        break;
                }
                return false;
            }
        });
    }

    public static void set_blue_text_touch_effect(final Context context, final TextView textView) {
        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        textView.setTextColor(ContextCompat.getColor(context, R.color.textColorSecondary));
                        break;
                    case MotionEvent.ACTION_UP:
                        textView.setTextColor(ContextCompat.getColor(context, R.color.colorAccentDialog));
                        break;
                }
                return false;
            }
        });
    }

    public static Bitmap createBitmapFromResource(Context context, int drawableId, int reqWidth, int reqHeight) {
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), drawableId);
        int oldwidth = bmp.getWidth();
        int oldheight = bmp.getHeight();
        float scaleWidth = reqWidth / (float) oldwidth;
        float scaleHeight = reqHeight / (float) oldheight;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bmp, 0, 0, oldwidth, oldheight, matrix, true);
        return resizedBitmap;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                         int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
}
