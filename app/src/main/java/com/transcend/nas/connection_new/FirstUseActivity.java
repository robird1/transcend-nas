package com.transcend.nas.connection_new;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.transcend.nas.R;

/**
 * Created by steve_su on 2016/11/16.
 */

public class FirstUseActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_use);
        TextView privacyBtn = (TextView) findViewById(R.id.btn_privacy_policy);

        privacyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FirstUseActivity.this, PrivacyPolicyActivity.class));
            }
        });

        Button licenseBtn = (Button) findViewById(R.id.btn_view_license_agreement);
        licenseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(FirstUseActivity.this, LicenseAgreementActivity.class));
                FirstUseActivity.this.finish();
            }
        });
    }

}
