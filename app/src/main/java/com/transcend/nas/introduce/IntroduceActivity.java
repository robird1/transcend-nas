package com.transcend.nas.introduce;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.transcend.nas.NASPref;
import com.transcend.nas.R;
import com.transcend.nas.common.GoogleAnalysisFactory;
import com.transcend.nas.common.StyleFactory;
import com.transcend.nas.AutoLinkActivity;

import java.util.ArrayList;
import java.util.List;

public class IntroduceActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = IntroduceActivity.class.hashCode() & 0xFFFF;
    private static final String TAG = IntroduceActivity.class.getSimpleName();

    private BasicViewerPager mBasicViewerPager;
    private BasicPagerAdapter mBasicPagerAdapter;
    private RadioGroup mIndicator;
    private Button mStart;
    private TextView mNext;
    private final int mTotal = 5;
    private int mCurrent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduce);
        GoogleAnalysisFactory.getInstance(this).sendScreen(GoogleAnalysisFactory.VIEW.INTRODUCE);

        initView();
        initViewContent();
        initViewClick();
    }

    private void initView() {
        mBasicViewerPager = (BasicViewerPager) findViewById(R.id.introduce_view_pager);
        mIndicator = (RadioGroup) findViewById(R.id.introduce_indicator);
        mStart = (Button) findViewById(R.id.introduce_started_button);
        StyleFactory.set_lt_gray_button_touch_effect(this, mStart);
        mNext = (TextView) findViewById(R.id.introduce_next_text);
        //ImageView background = (ImageView) findViewById(R.id.introduce_bg);
        //Point p = new Point();
        //getWindowManager().getDefaultDisplay().getSize(p);
        //background.setImageBitmap(StyleFactory.createBitmapFromResource(this, R.drawable.bg_wizard, p.x, p.y));
    }

    private void initViewClick(){
        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCurrent == mTotal - 1)
                    startAutoLinkActivity();
                else
                    mBasicViewerPager.setCurrentItem(mCurrent + 1, false);
            }
        });

        mBasicViewerPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setIndicator(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void initViewContent(){
        String[] titles = new String[]{getString(R.string.welcome_to_storejet_cloud), getString(R.string.appwizard1_remote_access),
                getString(R.string.auto_backup),getString(R.string.appwizard3_media_streaming)};
        String[] infos = new String[]{"",getString(R.string.appwizard1_freely_access),
                getString(R.string.appwizard2_storejet_cloud_can_automatically_back_u), getString(R.string.appwizard3_storejet_cloud_airplay_casting_your_vid)};
        int[] images = new int[] {R.drawable.ic_logo_storejetcloud_big, R.drawable.guide_image1_small,
                R.drawable.guide_image2_small, R.drawable.guide_image3_small};
        List<View> views = new ArrayList<>();
        LayoutInflater mInflater = getLayoutInflater().from(this);
        for(int i = 0; i < mTotal;i++){
            //add introduce page
            View view;
            if(i == 0) {
                view = mInflater.inflate(R.layout.viewer_welcome, null);
            } else if(i == mTotal - 1) {
                view = mInflater.inflate(R.layout.activity_initial, null);
                LinearLayout startLayout = (LinearLayout) view.findViewById(R.id.initial_started_layout);
                startLayout.setVisibility(View.GONE);
            } else {
                view = mInflater.inflate(R.layout.viewer_introduce, null);
                TextView title = (TextView) view.findViewById(R.id.introduce_title);
                title.setText(titles[i]);
                TextView info = (TextView) view.findViewById(R.id.introduce_info);
                info.setText(infos[i]);
                ImageView image = (ImageView) view.findViewById(R.id.introduce_image);
                image.setImageResource(images[i]);
            }
            views.add(view);
        }

        mBasicPagerAdapter = new BasicPagerAdapter();
        mBasicPagerAdapter.setContentList(views);
        mBasicViewerPager.setAdapter(mBasicPagerAdapter);

        setIndicator(0);
    }

    private void setIndicator(int index) {
        mCurrent = index;
        //mIndicator.setVisibility(index == mTotal - 1 ? View.INVISIBLE : View.VISIBLE);
        //mNext.setVisibility(index == mTotal - 1 ? View.INVISIBLE : View.VISIBLE);
        mStart.setText(index == mTotal - 1 ? getString(R.string.wizard_start) : getString(R.string.next));
        for(int i = 0; i < mTotal; i++) {
            RadioButton button = (RadioButton) mIndicator.getChildAt(i);
            button.setChecked(i == index);
        }
    }

    public void startAutoLinkActivity() {
        NASPref.setIntroduce(this, true);
        Intent intent = new Intent();
        intent.setClass(IntroduceActivity.this, AutoLinkActivity.class);
        startActivity(intent);
        finish();
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
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
