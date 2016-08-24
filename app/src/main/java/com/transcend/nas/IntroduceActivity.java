package com.transcend.nas;

import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.transcend.nas.common.StyleFactory;
import com.transcend.nas.connection.AutoLinkActivity;

import java.util.ArrayList;
import java.util.List;

public class IntroduceActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = IntroduceActivity.class.hashCode() & 0xFFFF;
    private static final String TAG = IntroduceActivity.class.getSimpleName();

    private BasicViewerPager mBasicViewerPager;
    private BasicPagerAdapter mBasicPagerAdapter;
    private RadioGroup mIndicator;
    private Button mStart;
    private final int mTotal = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduce);
        initView();
        initViewContent();
        initViewClick();
    }

    private void initView() {
        mBasicViewerPager = (BasicViewerPager) findViewById(R.id.introduce_view_pager);
        mIndicator = (RadioGroup) findViewById(R.id.introduce_indicator);
        mStart = (Button) findViewById(R.id.introduce_started_button);
        StyleFactory.set_white_button_touch_effect(this, mStart);
        ImageView background = (ImageView) findViewById(R.id.introduce_bg);
        Point p = new Point();
        getWindowManager().getDefaultDisplay().getSize(p);
        background.setImageBitmap(StyleFactory.createBitmapFromResource(this, R.drawable.bg_wizard, p.x, p.y));
    }

    private void initViewClick(){
        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAutoLinkActivity();
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
        String[] titles = new String[]{"Title1","Title2","Title3","Title4"};
        String[] infos = new String[]{"Info1","Info2","Info3","Info4"};
        List<View> views = new ArrayList<>();
        LayoutInflater mInflater = getLayoutInflater().from(this);
        for(int i = 0; i < mTotal;i++){
            //add introduce page
            View view = mInflater.inflate(R.layout.viewer_introduce, null);
            TextView title = (TextView) view.findViewById(R.id.introduce_title);
            title.setText(titles[i]);
            TextView info = (TextView) view.findViewById(R.id.introduce_info);
            info.setText(infos[i]);
            ImageView image = (ImageView) view.findViewById(R.id.introduce_image);
            views.add(view);

            //add indicator
            RadioButton button = new RadioButton(this);
            button.setClickable(false);
            mIndicator.addView(button);
        }

        mBasicPagerAdapter = new BasicPagerAdapter();
        mBasicPagerAdapter.setContentList(views);
        mBasicViewerPager.setAdapter(mBasicPagerAdapter);

        setIndicator(0);
    }

    private void setIndicator(int index){
        mStart.setVisibility(index == mTotal - 1 ? View.VISIBLE : View.INVISIBLE);
        mIndicator.setVisibility(index == mTotal - 1 ? View.INVISIBLE : View.VISIBLE);
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
}
