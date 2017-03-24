package com.transcend.nas.introduce;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.transcend.nas.NASPref;
import com.transcend.nas.NASUtils;
import com.transcend.nas.R;

/**
 * Created by steve_su on 2016/11/16.
 */

public class LicenseAgreementActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_agreement);
        TextView content = (TextView) findViewById(R.id.content);

        CharSequence info = Html.fromHtml(NASUtils.readFromAssets(this, "NASAPPEULA.txt"));
        content.setText(info);

        Button agreeBtn = (Button) findViewById(R.id.button_agree);
        agreeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickAgreeButton();
            }
        });
    }

    public void onClickAgreeButton() {
        Intent i = new Intent(this, IntroduceActivity.class);
        startActivity(i);
        NASPref.setIsLicenseAgreed(this, true);
        finish();
    }

}
