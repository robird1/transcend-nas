package com.transcend.nas.management;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.transcend.nas.R;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by steve_su on 2016/11/22.
 */

public class FeedbackActivity extends AppCompatActivity {

    private static final String TAG = FeedbackActivity.class.getSimpleName();
    private static final String FEEDBACK_URL = "http://www.transcend-info.com/Service/SMSService.svc/web/ServiceMailCaseAdd";
    private TextInputLayout mInputLayoutName;
    private TextInputLayout mInputLayoutEmail;
    private TextInputLayout mInputLayoutMessage;
    private EditText mEditTextName;
    private EditText mEditTextEmail;
    private EditText mEditTextMessage;
    private static final String REGION = "Taiwan";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
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
            final String jsonData = "{\"DataModel\":{\"CustName\":\"" + mEditTextName.getText().toString() + "\"" +
                    ",\"CustEmail\":\"" + mEditTextEmail.getText().toString() + "\"" +
                    ",\"Region\":\"" + REGION + "\"" +
                    ",\"ISOCode\":\"TW\"" +
                    ",\"LocalProb\":\"" + mEditTextMessage.getText().toString() + "\"}}";

            sendRequest(configureRequest(), jsonData);
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

    private HttpURLConnection configureRequest() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(FEEDBACK_URL);
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

    private void sendRequest(final HttpURLConnection connection, final String jsonData)
    {
        if (connection == null) {
            return;
        }

        final Handler handler = new Handler()
        {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Toast.makeText(FeedbackActivity.this, R.string.thank_you, Toast.LENGTH_SHORT).show();
                FeedbackActivity.this.finish();
            }
        };

        Button sendBtn = (Button) findViewById(R.id.btn_send);
        sendBtn.setEnabled(false);

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    OutputStream out = connection.getOutputStream();
                    out.write(jsonData.getBytes());
                    out.flush();
                    out.close();

                    int responseCode = connection.getResponseCode();

                    Log.d(TAG, "param: " + jsonData);
                    Log.d(TAG, "responseCode: " + responseCode);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                handler.sendMessage(new Message());
            }
        }).start();

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
