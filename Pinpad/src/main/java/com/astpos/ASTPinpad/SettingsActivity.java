package com.astpos.ASTPinpad;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.astpos.ASTPinpad.util.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.astpos.ASTPinpad.util.Constants.TAG;

/**
 * Created by Iskren Iliev on 11/17/17.
 */


public class SettingsActivity extends Activity {

    private LinearLayout mainLayout;
    private Button saveButton, cancelButton, pingButton;
    private CheckBox checkBox;
    private Spinner spinnerTransName, spinnerDebitCredit, spinnerProcessorType;
    private EditText editMerchantId, editPinpadIp, editPingData, editTipThreshold,
            editCompanyEmail, editCompanyPass;
    private ProgressBar progressBar;

    // Shared Preferences
    private SharedPreferences preferences;
    private SharedPreferences.Editor preferencesEditor;


    private String transactionName = "";
    private int transactionId = 0;
    private boolean isDebit = false;
    private boolean isCredit = false;
    private boolean useNab = false;
    private boolean usePax = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //init shared preferences
        this.preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.preferencesEditor = this.preferences.edit();
        this.printSharedPreferences();

        this.setLayout();
        this.setListeners();

    }


    private void setLayout(){
        setContentView(R.layout.activity_settings);

        mainLayout = (LinearLayout)findViewById(R.id.linearLayoutSettingsMain);

        progressBar = (ProgressBar)findViewById(R.id.indeterminateBar);

        saveButton = (Button)findViewById(R.id.save_button);
        saveButton.setOnClickListener(onSaveButtonClick);

        cancelButton = (Button)findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(onCancelButtonClick);

        pingButton = (Button)findViewById(R.id.ping_button);
        pingButton.setOnClickListener(onPingButtonClick);

        checkBox = (CheckBox) findViewById(R.id.checkbox);
        //disable button if checkbox is not checked else enable button
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ( isChecked ) {
                    saveButton.setEnabled(true);
                }
                else {
                    saveButton.setEnabled(false);
                }
            }
        });

        editMerchantId = (EditText)findViewById(R.id.merchant_id_data);
        editPinpadIp = (EditText)findViewById(R.id.pinpad_ip_data);
        editPingData = (EditText)findViewById(R.id.ping_data);
        editTipThreshold = (EditText)findViewById(R.id.tip_threshold);
        editCompanyEmail = (EditText)findViewById(R.id.company_email);
        editCompanyPass = (EditText)findViewById(R.id.company_pass);

        spinnerDebitCredit = (Spinner)findViewById(R.id.transaction_type);
        spinnerTransName = (Spinner)findViewById(R.id.transaction_name);
        spinnerProcessorType = (Spinner)findViewById(R.id.processor_type);


        if(!preferences.getString(Constants.MERCHANT_ID, "").isEmpty()) {
            editMerchantId.setText(preferences.getString(Constants.MERCHANT_ID, ""));
        }
        if(!preferences.getString(Constants.PINPAD_IP, "").isEmpty()) {
            editPinpadIp.setText(preferences.getString(Constants.PINPAD_IP, ""));
        }
        if(preferences.getInt(Constants.TIP_THRESHOLD, 0) > 0) {
            Log.i(TAG, "is bigger than 0: " + String.valueOf(preferences.getInt(Constants.TIP_THRESHOLD, 0)));
            editTipThreshold.setText(String.valueOf(preferences.getInt(Constants.TIP_THRESHOLD, 0)));
        }
        if(!preferences.getString(Constants.COMPANY_EMAIL_ID, "").isEmpty()) {
            editCompanyEmail.setText(preferences.getString(Constants.COMPANY_EMAIL_ID, ""));
        }
        if(!preferences.getString(Constants.COMPANY_EMAIL_PASS, "").isEmpty()) {
            editCompanyPass.setText(preferences.getString(Constants.COMPANY_EMAIL_PASS, ""));
        }


        // preset if any selection previously or CREDIT by default
        if(preferences.getBoolean(Constants.IS_DEBIT, false)) {
            spinnerDebitCredit.setSelection(1);
        } else {
            spinnerDebitCredit.setSelection(0);
        }

        // preset if any selection for processor or NAB by default
        if(preferences.getBoolean(Constants.USE_NAB, false)) {
            spinnerProcessorType.setSelection(0);
        } else {
            spinnerProcessorType.setSelection(1);
        }

        // preset with previously chosen transaction or INIT/0 by default
        spinnerTransName.setSelection(preferences.getInt(Constants.TRANSACTION_ID, 0));
    }


    private void setListeners(){
        //here implement spinner for transaction name drop down.
        spinnerTransName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                saveTransactionName(position);
                Log.i(Constants.TAG, "position: "+position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });


        //here implement Debit/Credit spinner for drop down.
        spinnerDebitCredit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                saveTransactionType(position);
                Log.i(Constants.TAG, "position: "+position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });


        //here implement Processor spinner for drop down.
        spinnerProcessorType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                saveProcessorType(position);
                Log.i(Constants.TAG, "position: "+position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });


        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard();
            }
        });
    }

    /**
     * This method used for save the position and name corresponding to
     * the selected spinner for transaction name.
     * @param position
     */
    public void saveTransactionType(int position){
        if(position == 0) {
            isCredit = true;
            isDebit = false;
//            this.preferencesEditor.putBoolean(IS_DEBIT, true);
        } else if(position == 1) {
            isCredit = false;
            isDebit = true;
//            this.preferencesEditor.putBoolean(IS_CREDIT, true);
        } else {
            isCredit = false;
            isDebit = false;
            Log.i(Constants.TAG, "wrong trans type - check spinnerDebitCredit!");
        }
    }


    /**
     * This method used for save the position and name corresponding to
     * the selected spinner for processor type.
     * @param position
     */
    public void saveProcessorType(int position){
        if(position == 0) {
            useNab = true;
            usePax = false;
        } else if(position == 1) {
            useNab = false;
            usePax = true;
        } else {
            useNab = false;
            usePax = false;
            Log.i(Constants.TAG, "Wrong processor type - check spinnerProcessorType!");
        }
    }



    /**
     * This method used for save the position and name corresponding to
     * the selected spinner for transaction name.
     * @param position
     */
    public void saveTransactionName(int position){
        String name = spinnerTransName.getSelectedItem().toString();
        this.transactionName = name;
        this.transactionId = position;
    }


    /**
     * Saves all user preferences (user defaults) from this activity
     */
    public void savePreferences() {
        this.preferencesEditor.putBoolean(Constants.IS_DEBIT, this.isDebit);
        this.preferencesEditor.putBoolean(Constants.IS_CREDIT, this.isCredit);

        this.preferencesEditor.putBoolean(Constants.USE_NAB, this.useNab);
        this.preferencesEditor.putBoolean(Constants.USE_PAX, this.usePax);

        this.preferencesEditor.putString(Constants.TRANSACTION_NAME, transactionName);
        this.preferencesEditor.putString(Constants.MERCHANT_ID, editMerchantId.getText().toString());
        this.preferencesEditor.putString(Constants.PINPAD_IP, editPinpadIp.getText().toString());
        this.preferencesEditor.putString(Constants.COMPANY_EMAIL_ID, editCompanyEmail.getText().toString());
        this.preferencesEditor.putString(Constants.COMPANY_EMAIL_PASS, editCompanyPass.getText().toString());

        String tipThreshold;
        if(editTipThreshold.getText().toString().isEmpty()) {
            tipThreshold = "0";
        } else {
            tipThreshold = editTipThreshold.getText().toString();
        }
        this.preferencesEditor.putInt(Constants.TIP_THRESHOLD, Integer.valueOf(tipThreshold));
        this.preferencesEditor.putInt(Constants.TRANSACTION_ID, this.transactionId);

        this.preferencesEditor.apply();
    }



    Button.OnClickListener onSaveButtonClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Save button clicked");
            if (checkBox.isChecked()){
                savePreferences();
//                printSharedPreferences();
                // not opening new activity but going back to parent activity
                finish();
//                Intent i = new Intent(SettingsActivity.this, HomeMainActivity.class);
//                startActivity(i);
            }
            else {
                saveButton.setEnabled(false);
            }
        }
    };


    Button.OnClickListener onCancelButtonClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "Cancel button clicked");
//            printSharedPreferences();
            // not opening new activity but going back to parent activity
            finish();

        }
    };

    Button.OnClickListener onPingButtonClick = new Button.OnClickListener() {
        @Override
        public  void onClick(View v) {
            hideKeyboard();
            Log.d(TAG, "Ping button clicked");
            editPingData.setText(R.string.ping_result);
            editPingData.setBackgroundColor(Color.LTGRAY);
            editPingData.setTextColor(Color.BLACK);
            mainLayout.setAlpha(0.3f);
            progressBar.setVisibility(View.VISIBLE);

            PingAsyncTask pingTask = new PingAsyncTask();
            pingTask.execute(editPinpadIp.getText().toString());
        }
    };


    private class PingAsyncTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String[] params) {
            // do above Server call here
//            Log.i(TAG, "ping IP: "+ params[0]);
            if(isPingable(params[0])) {
                return true;//;
            } else {
                return false;//;
            }
        }

        @Override
        protected void onPostExecute(Boolean pingIsOK) {
            //process message
            if(pingIsOK){
                editPingData.setText("Response OK");
                editPingData.setBackgroundColor(Color.GREEN);
                editPingData.setTextColor(Color.WHITE);
            } else {
                editPingData.setText("Unreachable");
                editPingData.setBackgroundColor(Color.RED);
                editPingData.setTextColor(Color.WHITE);
            }

            progressBar.setVisibility(View.GONE);
            mainLayout.setAlpha(1.0f);
            Log.i(TAG, "onPostExecute Ping: " + pingIsOK);
        }
    }


    public static boolean isPingable(String ipAddress) {
        InetAddress in;
        in = null;
        try {
            in = InetAddress.getByName(ipAddress);
            Log.d(TAG, "pinging " + ipAddress);

        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }

        try {
            if (in.isReachable(Constants.TIMEOUT_INTERVAL)) { //Response OK
                return true;
            } else { //Time out
                return false;
            }
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Settings activity destroyed");
//        printSharedPreferences();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Settings activity stopped");
        printSharedPreferences();
    }


    /**
     * hides keyboard on current page
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    private void printSharedPreferences() {
        Log.i(TAG, "============ PREFERENCES ===========");
        Log.i(TAG, Constants.USE_NAB + ": " + preferences.getBoolean(Constants.USE_NAB, false));
        Log.i(TAG, Constants.USE_PAX + ": " + preferences.getBoolean(Constants.USE_PAX, false));
        Log.i(TAG, Constants.MERCHANT_ID + ": " + preferences.getString(Constants.MERCHANT_ID, ""));
        Log.i(TAG, Constants.PINPAD_IP + ": " + preferences.getString(Constants.PINPAD_IP, ""));
        Log.i(TAG, Constants.IS_DEBIT + ": " + preferences.getBoolean(Constants.IS_DEBIT, false));
        Log.i(TAG, Constants.IS_CREDIT + ": " + preferences.getBoolean(Constants.IS_CREDIT, false));
        Log.i(TAG, Constants.TRANSACTION_NAME + ": " + preferences.getString(Constants.TRANSACTION_NAME, ""));
        Log.i(TAG, Constants.TRANSACTION_ID + ": " + preferences.getInt(Constants.TRANSACTION_ID, 0));
        Log.i(TAG, Constants.TIP_THRESHOLD + ": " + preferences.getInt(Constants.TIP_THRESHOLD, 0));
        Log.i(TAG, "====================================");
    }
}

