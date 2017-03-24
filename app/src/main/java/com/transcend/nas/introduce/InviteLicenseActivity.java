package com.transcend.nas.introduce;

import com.transcend.nas.NASPref;

/**
 * Created by steve_su on 2017/3/7.
 */

public class InviteLicenseActivity extends LicenseAgreementActivity {

    @Override
    public void onBackPressed() {
        this.setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onClickAgreeButton() {
        NASPref.setIsLicenseAgreed(this, true);
        this.setResult(RESULT_OK);
        finish();
    }

}
