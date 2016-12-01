package com.transcend.nas.management;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.transcend.nas.BuildConfig;
import com.transcend.nas.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by steve_su on 2016/11/22.
 */

public class FeedbackActivity extends AppCompatActivity {

    private static final String TAG = FeedbackActivity.class.getSimpleName();
    private static final String CATEGORY_LIST_URL = "http://www.transcend-info.com/Service/SMSService.svc/web/GetSrvCategoryList";
    private static final String FEEDBACK_URL = "http://www.transcend-info.com/Service/SMSService.svc/web/ServiceMailCaseAdd";
//    private static final String FEEDBACK_URL = "http://10.13.5.10/Service/SMSService.svc/web/ServiceMailCaseAdd";
    private static final int ID_GET_CATEGORY_LIST = 0;
    private static final int ID_SEND_FEEDBACK = 1;
    private static final String KEY_SERVICE_TYPE = "service_type";
    private static final String KEY_SERVICE_CATEGORY = "service_category";
    private static final String PRODUCT_NAME = "StoreJet Cloud";
    private TextInputLayout mInputLayoutName;
    private TextInputLayout mInputLayoutEmail;
    private TextInputLayout mInputLayoutMessage;
    private EditText mEditTextName;
    private EditText mEditTextEmail;
    private EditText mEditTextMessage;
    private static final String REGION = "Taiwan";
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case ID_GET_CATEGORY_LIST:

                    Bundle listData = msg.getData();
                    if (listData != null) {
                        Log.d(TAG, "listData != null");
                        String srvType = listData.getString(KEY_SERVICE_TYPE);
                        String srvCategory = listData.getString(KEY_SERVICE_CATEGORY);
                        String platformInfo = "App v" + BuildConfig.VERSION_NAME + " OS version: " + android.os.Build.VERSION.SDK_INT +
                                " device name: " + getDeviceName();

                        final String jsonData = "{\"DataModel\":{\"CustName\":\"" + mEditTextName.getText().toString() + "\"" +
                                ",\"CustEmail\":\"" + mEditTextEmail.getText().toString() + "\"" +
                                ",\"Region\":\"" + REGION + "\"" +
                                ",\"ISOCode\":\"TW\"" +
                                ",\"Request\":\"" + platformInfo + "\"" +
                                ",\"SrvType\":\"" + srvType + "\"" +
                                ",\"SrvCategory\":\"" + srvCategory + "\"" +
                                ",\"ProductName \":\"" + PRODUCT_NAME + "\"" +
                                ",\"LocalProb\":\"" + mEditTextMessage.getText().toString() + "\"}}";

                        sendRequest(configurePostRequest(FEEDBACK_URL), jsonData, ID_SEND_FEEDBACK);

                    }

                    break;
                case ID_SEND_FEEDBACK:
                    Toast.makeText(FeedbackActivity.this, R.string.thank_you, Toast.LENGTH_SHORT).show();
                    FeedbackActivity.this.finish();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        initToolbar();
        mInputLayoutName = (TextInputLayout) findViewById(R.id.input_layout_name);
        mInputLayoutEmail = (TextInputLayout) findViewById(R.id.input_layout_email);
        mInputLayoutMessage = (TextInputLayout) findViewById(R.id.input_layout_message);
        mEditTextName = (EditText) findViewById(R.id.input_name);
        mEditTextEmail = (EditText) findViewById(R.id.input_email);
        mEditTextMessage = (EditText) findViewById(R.id.input_message);
        mEditTextName.addTextChangedListener(new MyTextWatcher(mEditTextName));
        mEditTextEmail.addTextChangedListener(new MyTextWatcher(mEditTextEmail));
        mEditTextMessage.addTextChangedListener(new MyTextWatcher(mEditTextMessage));
    }

    public void onClickBackImage(View view)
    {
        super.onBackPressed();
    }

    public void onClickSendButton(View view)
    {
        if (isInputValid()) {

            Button sendBtn = (Button) findViewById(R.id.btn_send);
            sendBtn.setEnabled(false);

            sendRequest(configurePostRequest(CATEGORY_LIST_URL), null, ID_GET_CATEGORY_LIST);
        }
    }

    private boolean isInputValid()
    {
        return isValidName() && isValidEmail() && isValidMessage();
    }

    private boolean isValidName() {
        if (mEditTextName.getText().toString().trim().isEmpty()) {
            mInputLayoutName.setError(getString(R.string.invalid_name));
            requestFocus(mEditTextName);
            return false;
        } else {
            mInputLayoutName.setErrorEnabled(false);
            return true;
        }
    }

    private boolean isValidEmail() {
        String email = mEditTextEmail.getText().toString().trim();
        boolean isValid = !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();

        if (!isValid) {
            mInputLayoutEmail.setError(getString(R.string.invalid_email));
            requestFocus(mEditTextEmail);
            return false;
        } else {
            mInputLayoutEmail.setErrorEnabled(false);
            return true;
        }
    }

    private boolean isValidMessage() {
        String message = mEditTextMessage.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            mInputLayoutMessage.setError(getString(R.string.invalid_message));
            requestFocus(mEditTextMessage);
            return false;
        } else {
            mInputLayoutMessage.setErrorEnabled(false);
            return true;
        }
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private HttpURLConnection configurePostRequest(String requestUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(requestUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("content-type", "application/json");
            conn.setDoOutput(true);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(10000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return conn;
        }
    }

    private void sendRequest(final HttpURLConnection connection, final String jsonData, final int messageId)
    {
        if (connection == null) {
            return;
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                Message msg = new Message();
                try {
                    OutputStream out = connection.getOutputStream();
                    if (jsonData != null) {
                        out.write(jsonData.getBytes());
                    }
                    out.flush();
                    out.close();

                    int responseCode = connection.getResponseCode();

                    Log.d(TAG, "param: " + jsonData);
                    Log.d(TAG, "responseCode: " + responseCode);
                    if (responseCode == HttpsURLConnection.HTTP_OK) {

                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuffer sb = new StringBuffer("");
                        String line="";

                        while((line = in.readLine()) != null) {

                            sb.append(line);
                            break;
                        }

                        Log.d(TAG, "get category result: " + line);
                        in.close();

                        if (messageId == ID_GET_CATEGORY_LIST) {
                            Bundle data = new Bundle();
                            data.putString(KEY_SERVICE_TYPE, String.valueOf(15));
                            data.putString(KEY_SERVICE_CATEGORY, String.valueOf(384));
                            msg.setData(data);
                        }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                msg.what = messageId;
                mHandler.sendMessage(msg);
            }
        }).start();

    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) this.findViewById(R.id.feedback_toolbar);
        toolbar.setTitle("");
//        toolbar.setNavigationIcon(R.drawable.ic_navigation_arrow_white_24dp);
        setSupportActionBar(toolbar);
        TextView title = (TextView) findViewById(R.id.feedback_toolbar_title);
        title.setText(R.string.app_name);
    }

    private String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    private class MyTextWatcher implements TextWatcher {

        private View view;

        private MyTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.input_name:
                    isValidName();
                    break;
                case R.id.input_email:
                    isValidEmail();
                    break;
                case R.id.input_message:
                    isValidMessage();
                    break;
            }
        }
    }
}
