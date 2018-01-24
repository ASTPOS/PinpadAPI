package com.astpos.ASTPinpad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.astpos.ASTPinpad.util.Constants;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Iskren Iliev on 11/14/17.
 */

public class PinpadActivity extends Activity {

    /**
     * TAG for printing logs
     */
    protected final static String TAG = "ASTPOS";

    // pop up message
    private AstDialogFragment dialogFragment;

    //layout vars
    private LinearLayout mainLayout;
    private TextView editPing;
    private EditText editSaleAmount, editTipAmount,
            editHREF, editTransactionId, editTotalAmount,
            editAuthCode, editAccount, editResponseMsg;
    private Spinner spinnerTransName, spinnerDebitCredit;
    private ImageView signImage;
    private Button btnNoTip, btn10Tip, btn15Tip, btn20Tip,
                    btnProcessPmt, btnDone;
    private LinearLayout scrollViewLayout;
    private ProgressBar progressBar;


    // Shared Preferences
    private android.content.SharedPreferences preferences;
    private android.content.SharedPreferences.Editor preferencesEditor;

    // variables for asyncTask
    private final String USER_AGENT = "Mozilla/5.0";

    // vars for pinpadActivity ///
    private static PinpadTransaction pinpadTransaction;
    private int transactionId = 0;
    private String transactionName = "";

    //extras
    private String saleAmountStr;
    private String tipAmount;
    private Boolean fromSign;
    private String processorType;


    // Input Format filter for decimal numbers up to 2 digits after decimal point
    private final DecimalFormat decimalFormatter = new DecimalFormat("#.00");

    // map that stores all the information from transaction response
    protected static Map<String, String> responsePars;

    // async task reference for Post request
    RequestAsyncTask urlConnectionTask = null;
    PingAsyncTask pingTask = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "PinpadActivity onCreate");


        this.preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.preferencesEditor = preferences.edit();

        //process and load variables if any extras are send with the intent
        this.getExtras();

        setContentView(R.layout.activity_pinpad);

        //initialize fragment for any popup msgs that may appear
        dialogFragment = new AstDialogFragment();

        setLayoutPref();
        setListeners();

        // create the object of PinpadTransaction class.
        String ip = preferences.getString(Constants.PINPAD_IP, "");
        if(fromSign){
            Log.i(TAG, "Coming from Sign activity");
        } else if(processorType.equals(Constants.NAB)) {
            pinpadTransaction = new NabPinpadTransaction(ip);
        } else if(processorType.equals(Constants.PAX)) {
            pinpadTransaction = new PaxPinpadTransaction(ip);
        } else {
            Log.i(TAG, "Wrong processorType name. Please use PAX or NAB!");
        }

        //set transaction
        this.transactionId = spinnerTransName.getSelectedItemPosition();

        /////////////////////////////////////////////////////////
        // !!! in order to access response information use !!! //
        // !!! pinpadTransaction.getResponseMap();      !!! //
        /////////////////////////////////////////////////////////
        this.displayResponse();


        //to get imagepath from SignatureActivity and set it on ImageView
        String image_path = getIntent().getStringExtra(Constants.IMAGE_PATH);
        if(image_path == null) {
         Log.i(TAG, "image path is null");
        } else {
            Bitmap bitmap = BitmapFactory.decodeFile(image_path);
            signImage.setImageBitmap(bitmap);
        }


        //for testing
//        this.sendEmailOnBackground();
    }


    private void getExtras(){
        //get extras
        Intent intent = getIntent();
        tipAmount = preferences.getString(Constants.TIP_AMOUNT, null);
        processorType = intent.getStringExtra(Constants.PROCESSOR_TYPE);
        fromSign = intent.getBooleanExtra(Constants.FROM_SIGN, false);

        if(this.fromSign) {
            saleAmountStr = preferences.getString(Constants.TRANS_AMOUNT, null);
        } else {
            saleAmountStr = intent.getStringExtra(Constants.TRANS_AMOUNT);
            if(saleAmountStr != null) {
                this.preferencesEditor.putString(Constants.TRANS_AMOUNT, saleAmountStr);
            }
        }

        Log.i(TAG, "==== PxPinpadActivity onCreate ===== ");
        Log.i(TAG, "==== Extras = Amount   : " + saleAmountStr);
        Log.i(TAG, "==== Extras = Tip      : " + tipAmount);
        Log.i(TAG, "==== Extras = From Sign: " + fromSign);
        Log.i(TAG, "==== Extras = Processor: " + processorType);
        Log.i(TAG, "==================================== ");
    }



    private void setLayoutPref(){
//        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // initialize layout variables
//        checkDebit = (CheckBox)findViewById(R.id.checkDebit);
//        checkCredit = (CheckBox)findViewById(R.id.checkCredit);
//        checkReboot = (CheckBox)findViewById(R.id.checkReboot);

        mainLayout = (LinearLayout)findViewById(R.id.linearLayoutPinpadMain);

        scrollViewLayout = (LinearLayout)findViewById(R.id.linearScrollView);

        progressBar = (ProgressBar)findViewById(R.id.indeterminateBar);

        editPing = (TextView)findViewById(R.id.ping_text);
        pingTask = new PingAsyncTask();
        pingTask.execute(preferences.getString(Constants.PINPAD_IP, ""));

        editSaleAmount = (EditText)findViewById(R.id.saleAmountId);
        editTipAmount = (EditText)findViewById(R.id.tipAmountId);
        editTotalAmount = (EditText)findViewById(R.id.totalAmountId);
        editHREF = (EditText)findViewById(R.id.tipAmountId);
        editTransactionId = (EditText)findViewById(R.id.transactionId);
        editAuthCode = (EditText)findViewById(R.id.authCodeId);
        editAccount = (EditText)findViewById(R.id.accountId);
        editResponseMsg = (EditText)findViewById(R.id.responseId);

        spinnerTransName = (Spinner)findViewById(R.id.transaction_name_id);
        spinnerDebitCredit = (Spinner)findViewById(R.id.transaction_type_id);

        signImage = (ImageView) findViewById(R.id.imageView1);


/* ************* set default values ************** */
        editSaleAmount.setText(saleAmountStr);
        editSaleAmount.setEnabled(false);

        editTipAmount.requestFocus();
        editTipAmount.setText(tipAmount);
        editTipAmount.setSelection(editTipAmount.getText().length());

//        editTipAmount.setFilters(new InputFilter[] {new DecimalDigitInputFilter(4, 2)});
        if(fromSign) {
            editTipAmount.setEnabled(false);
            this.hideKeyboard(fromSign);
            editAuthCode.setVisibility(View.VISIBLE);
            editAccount.setVisibility(View.VISIBLE);
            editResponseMsg.setVisibility(View.VISIBLE);
            findViewById(R.id.authCodeTxt).setVisibility(View.VISIBLE);
            findViewById(R.id.accountTxt).setVisibility(View.VISIBLE);
            findViewById(R.id.responseTxt).setVisibility(View.VISIBLE);

            findViewById(R.id.processButton).setVisibility(View.GONE);
            findViewById(R.id.tipNoTip).setEnabled(false);
            findViewById(R.id.tipNoTip).setEnabled(false);
            findViewById(R.id.tip10Procent).setEnabled(false);
            findViewById(R.id.tip15Procent).setEnabled(false);
            findViewById(R.id.tip20Procent).setEnabled(false);


            String approvedMsg;
            if(processorType.equals(Constants.NAB)){
                approvedMsg = Constants.AUTH_RESP_TEXT;
            } else{
                approvedMsg = Constants.HOST_RESP_MSG;
            }

            String dialogMsg = ""; //responsePars.get(approvedMsg); // this is too long

            if(pinpadTransaction.isChip()){
                dialogMsg = dialogMsg + "\nPlease remove the card!";
            }
            Log.d(TAG, "dialogMsg: " + dialogMsg);

            if(pinpadTransaction.isSuccessful()){
                createDialog(dialogMsg, Constants.APPROVED_TRANS, "approvedTransMsg");
            }

        } else {
            findViewById(R.id.doneButton).setVisibility(View.GONE);
            editAuthCode.setVisibility(View.GONE);
            editAccount.setVisibility(View.GONE);
            editResponseMsg.setVisibility(View.GONE);
            findViewById(R.id.authCodeTxt).setVisibility(View.GONE);
            findViewById(R.id.accountTxt).setVisibility(View.GONE);
            findViewById(R.id.responseTxt).setVisibility(View.GONE);
        }


        this.calculateTotal();


        spinnerTransName.setSelection(preferences.getInt(Constants.TRANSACTION_ID, 0));
        if(preferences.getBoolean(Constants.IS_DEBIT, false)) {
            spinnerDebitCredit.setSelection(1);
        } else {
            spinnerDebitCredit.setSelection(0);
        }
/* ************* set default values end ************** */

        // waits until ping has been completed
        progressBar.setVisibility(View.VISIBLE);
        mainLayout.setAlpha(0.3f);
        this.hideKeyboard(true);
    }


    /**
     * Disables any touch events on the view when the progress bar is visible
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        if(progressBar.getVisibility() == View.VISIBLE){
            return true;//consume
        } else {
            return super.dispatchTouchEvent(ev);
        }
    }


    /**
     * sets all required listeners of element that are used in this view
     */
    private void setListeners(){
        //here implement spinner for drop down.
        spinnerTransName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                saveTransaction(position);
                ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                Log.i("Trans Type position", ""+position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });


        spinnerDebitCredit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
                Log.i("Credit Debit position", ""+position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });


        editTipAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //move cursor to the end of the string
                editTipAmount.setSelection(editTipAmount.getText().length());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//             Log.i(TAG, "CHAR: " + s + " start: " + start + " before : " + before +  " Count: " + count);
                //move cursor to the end of the string
                editTipAmount.setSelection(editTipAmount.getText().length());

                //save old amount
                String oldAmount = s.subSequence(0,start).toString() + s.subSequence(start+count, s.length());
//                Log.d(TAG, "old tip amount: " + oldAmount);

                // Unregister self before setText
                editTipAmount.removeTextChangedListener(this);

                if((s.length() > 0 ) ||
                // if(!s.toString().matches( "^\\$((\\d{1,3})*|(\\d+))(\\.\\d{2})?$" ))
                        (!s.toString().matches( "^((\\d{1,3})?|(\\d+))(\\.\\d{2})?$" )) )
                {
                    String userInput= ""+s.toString().replaceAll("[^\\d]", "");
                    StringBuilder cashAmountBuilder = new StringBuilder(userInput);

                    while (cashAmountBuilder.length() > 3 && cashAmountBuilder.charAt(0) == '0') {
                        cashAmountBuilder.deleteCharAt(0);
                    }
                    while (cashAmountBuilder.length() < 3) {
                        cashAmountBuilder.insert(0, '0');
                    }
                    cashAmountBuilder.insert(cashAmountBuilder.length()-2, '.');

                    editTipAmount.setText(cashAmountBuilder.toString());
                    // keeps the cursor always to the right
                    Selection.setSelection(editTipAmount.getText(), cashAmountBuilder.toString().length());
                }


                // check if amounts are correct
                if(!calculateTotal()) {
                    editTipAmount.setText(oldAmount);
                }

                // Re-register self after setText
                editTipAmount.addTextChangedListener(this);
            }

            @Override
            public void afterTextChanged(Editable s) {
                //move cursor to the end of the string
                editTipAmount.setSelection(editTipAmount.getText().length());
            }
        });


        scrollViewLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(fromSign);
            }
        });
    }



    /**
     * calculates the Total amount including the sale and selected tip amount
     * and it updates the current view
     */
    private boolean calculateTotal() {
        saleAmountStr = editSaleAmount.getText().length() < 1 ? "0" : editSaleAmount.getText().toString();
        tipAmount = editTipAmount.getText().length() < 1 ? "0" : editTipAmount.getText().toString();

        double saleAmountDbl = this.convertStrToDble(saleAmountStr);
        double tipAmountDbl = this.convertStrToDble(tipAmount);

        int tresholdFactor = preferences.getInt(Constants.TIP_THRESHOLD, 0) < 2 ? 2 : preferences.getInt(Constants.TIP_THRESHOLD, 0);
        double totalAmt;
        if(saleAmountDbl*tresholdFactor < tipAmountDbl) {
            //do error
//            Log.d(TAG, "tip too large");
            createDialog(getString(R.string.large_tip), Constants.TIP_LARGE_ERR, "largeTipErrorMsg");

            return false;
        } else {
            totalAmt = saleAmountDbl + tipAmountDbl;
        }


        String totalAmtString = decimalFormatter.format(totalAmt);
        preferencesEditor.putString(Constants.TOTAL_AMOUNT, totalAmtString);

        editTotalAmount.setText(totalAmtString);
        editTotalAmount.setEnabled(false);
        Log.i(TAG, "sale: " + saleAmountStr + " + tip: " + tipAmount + " = " + totalAmtString);

        return true;
    }


    private double convertStrToDble(String str) {
        double dble;
        try {
            dble = Double.valueOf(str);
        } catch (Exception e) {
            Log.e(TAG, "Invalid Value: " + e);
            dble = 0;
        }
        return dble;
    }


    /**
     * if comming back from Signature page, it will save proper information
     * in order to be printed on the final receipt view
     */
    private void displayResponse(){
//        Log.i(TAG, "this pars: " + this.responsePars);

        String authCode;
        String account;
        String authResponse;
        if(processorType.equals(Constants.NAB)) {
            authCode = Constants.AUTH_CODE;
            account = Constants.AUTH_ACCOUNT_NBR;
            authResponse = Constants.AUTH_RESP_TEXT;
        } else {
            authCode = Constants.AUTH_CODE_PAX;
            account = Constants.AUTH_ACCOUNT_PAX;
            authResponse = Constants.HOST_RESP_MSG;
        }

        if(!fromSign){
            responsePars = null;
        } else if(responsePars != null) {
            Log.i(TAG, "Response auth: " + responsePars.get(authCode));
            Log.i(TAG, "Response acct: " + responsePars.get(account));
            Log.i(TAG, "Response msg:  " + responsePars.get(authResponse));


            preferencesEditor.putString(Constants.AUTH_CODE, responsePars.get(authCode));
            preferencesEditor.putString(Constants.AUTH_ACCOUNT_NBR, responsePars.get(account));
            preferencesEditor.putString(Constants.AUTH_RESP_TEXT, responsePars.get(authResponse));
            preferencesEditor.apply();

            editAuthCode.setText(responsePars.get(authCode));
            editAccount.setText(responsePars.get(account));
            editResponseMsg.setText(responsePars.get(authResponse));
        } else {
            Log.i(TAG, "Response pars is null!");
        }
    }


    @Override
    protected void onResume(){
        super.onResume();
//        Log.d(TAG, "PinpadActivity  resumed");

    }

    @Override
    protected void onPause(){
        super.onPause();
//        Log.d(TAG, "PinpadActivity paused");
    }

    @Override
    protected void onStop(){
        super.onStop();
        Log.i(TAG, "PinpadActivity stop");

        if(fromSign){
            this.resetParameters();
        } else {
            preferencesEditor.apply();
        }
    }

    @Override
    protected void onDestroy(){
        if(this.urlConnectionTask != null) {
            this.urlConnectionTask.cancel(true);
        }
        if(this.pingTask != null) {
            this.pingTask.cancel(true);
        }

        super.onDestroy();
//        Log.d(TAG, "PinpadActivity destroyed");
    }

    @Override
    public void onBackPressed() {
        this.resetParameters();

        super.onBackPressed();
    }

    /**
     * This method used for save the position and name corresponding to
     * the selected spinner for transaction name.
     * @param position
     */
    public void saveTransaction(int position){
        this.transactionId = position;
        this.transactionName = spinnerTransName.getSelectedItem().toString();
    }


    /**
     * This method is called after clicking exit button
     * @param view
     */
    public void onGoBackButton(View view) {
        finish();
    }


    /**
     * send information to printer method
     * @param view
     */
    public void onPrintButton(View view) {
//        Log.d(TAG, "printing button pressed...");
        PrintProcessor printProcessor = new PrintProcessor("192.168.1.49");
    }


    public void onSignButton(View view) {
//        Log.d(TAG, "getting signature...");
        this.openSignatureActivity();
    }


    /**
     * When sale is done and reset all parameters
     * @param view
     */
    public void onDoneButton(View view) {
//        Log.d(TAG, "done button pressed");
        this.resetParameters();

//        Intent intent = new Intent(this, HomeMainActivity.class);
//        startActivity(intent);

        finish();
    }


    /**
     * resets all preferences related to this sale
     */
    private void resetParameters(){
        preferencesEditor.remove(Constants.TRANS_AMOUNT);
        preferencesEditor.remove(Constants.TIP_AMOUNT);
        preferencesEditor.remove(Constants.FROM_SIGN);

        preferencesEditor.apply();
    }


    /**
     * opens Signature activity to collect customer signature
     */
    private void openSignatureActivity(){
        Intent i = new Intent(PinpadActivity.this, SignatureActivity.class);
        startActivity(i);
        finish();
    }


    /**
     *
     * This method is called after clicking the button.
     * hides KB and adds TIP amount to preferences
     * @param view
     */
    public void onProcessPaymentButton(View view) throws Exception {
        this.hideKeyboard(fromSign);
        preferencesEditor.putString(Constants.TIP_AMOUNT, editTipAmount.getText().toString());

        // retrieve selected transaction
        switch(transactionId) {
            case Constants.INIT:
//                pinpadTransaction.setToInit();
                break;
            case Constants.SALE:
                if(processorType.equals(Constants.NAB)){
                    if (preferences.getBoolean(Constants.IS_DEBIT, false)) {
                        pinpadTransaction.setToSaleDebit(this.getAmount());
                    } else if (preferences.getBoolean(Constants.IS_CREDIT, false)) {
                        pinpadTransaction.setToSaleCredit(this.getAmount());
                    } else if (preferences.getBoolean(Constants.DO_REBOOT, false)) {
                        //TODO no action
                    }
                } else {
//                    Log.d(TAG, "PAX set to sale");
                    pinpadTransaction.setToSale(this.getAmount());
                }
                break;
            case Constants.AUTH:
                pinpadTransaction.setToPreAuth(this.getAmount());
                break;
            case Constants.ADJ_TIP:
//                pinpadTransaction.setToAdjustTip(this.getAmount(),
//                        this.editTransactionId.getText().toString());
                break;
            case Constants.ADJ_REF:
//                pinpadTransaction.setToAdjustByRef(this.getAmount(),
//                        this.editHREF.getText().toString());
                break;
            default:
                break;
        }

        if(processorType.equals(Constants.PAX)){
            // choose Debit/Credit/Reboot based on CheckBox
            if (preferences.getBoolean(Constants.IS_DEBIT, false)) {
                pinpadTransaction.setDebit();
            } else if (preferences.getBoolean(Constants.IS_CREDIT, false)) {
                pinpadTransaction.setCredit();
            } else if (preferences.getBoolean(Constants.DO_REBOOT, false)) {
                pinpadTransaction.setReboot();
            }
        }

        // starts asynchronous task to process transaction
        if(processorType.equals(Constants.NAB)){
//            Log.d(TAG, "url connection post task");
            urlConnectionTask = new PostAsyncTask(pinpadTransaction.getPostData());
        } else {
//            Log.d(TAG, "url connection get task");
            urlConnectionTask = new AsyncTaskForGet();
        }
        Log.i(TAG, "URL string: "+pinpadTransaction.getUrlString());
        urlConnectionTask.execute(pinpadTransaction.getUrlString());
    }


    /**
     * hides keyboard on current page
     */
    private void hideKeyboard(boolean yes) {
        View view = this.getCurrentFocus();

        if(yes){
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } else if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Collect all amounts and formats them as per PAX appliances
     */
    private String getAmount(){
        String amountSale;
        String amountTip;
        try {
            amountSale = editSaleAmount.getText().toString().trim();
        } catch(Exception e) {
            Log.e(TAG, "Tip amount error: " + e);
            amountSale = "0.00";
        }
        try {
            amountTip = editTipAmount.getText().toString().trim().isEmpty() ? "0.00" : editTipAmount.getText().toString().trim();
        } catch(Exception e) {
            Log.e(TAG, "Tip amount error: " + e);
            amountTip = "0.00";
        }

        DecimalFormat decimalFormat = new DecimalFormat("0.00");

        double amt = (Double.valueOf(amountSale) + Double.valueOf(amountTip));
        String amount = decimalFormat.format(amt);
//        Log.d(TAG, "Unformatted amount: " + amount);

        String formattedAmount;
        if(processorType.equals(Constants.PAX)){
            String[] parts = amount.split("\\.");
            formattedAmount = (parts.length < 2) ? parts[0] : parts[0].concat(parts[1].substring(0,2));
        } else {
            formattedAmount = amount;
        }
        Log.i(TAG, "Formatted Amount: " + formattedAmount);

        return formattedAmount;
    }


    /**
     * Calculates the tip based on selected percentage or no tip
     * @param v
     */
    public void onTipButton(View v) {
        this.hideKeyboard(fromSign);
        double tip = 0.00;
        double sale = this.convertStrToDble(saleAmountStr);

        Log.i(TAG, "Button id: " + v.getId());
        switch(v.getId()) {
            case R.id.tipNoTip:
                tip = 0.00;
                break;
            case R.id.tip10Procent:
                tip = sale * 10 / 100;
                break;
            case R.id.tip15Procent:
                tip = sale * 15 / 100;
                break;
            case R.id.tip20Procent:
                tip = sale * 20 / 100;
                break;
            default:
                break;
        }

//        Log.i(TAG, "Sale: " + sale + " Tip: " + tip);
        editTipAmount.setText(decimalFormatter.format(tip));
        preferencesEditor.putString(Constants.TIP_AMOUNT, editTipAmount.getText().toString());
    }


    public void closeActivity(){
        finish();
    }

    public void closeActivityWithDelay(int miliseconds){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                finish();
            }
        }, miliseconds);
    }


    private String customerEmail;
    public void setEmailAddress(String emailAddress) {
        customerEmail = emailAddress;
//        this.sendEmailViaIntent();
        this.sendEmailOnBackground();
    }

    public void getEmailAddress() {

        createDialog("Please Enter Email: ", Constants.COLLECT_EMAIL, "collectEmail");
    }


    /**
     * Sending an email using an intent and user to choose the email app
     */
    public void sendEmailViaIntent(){
        String emailBody = "Details for your transaction at " + getString(R.string.app_name) + ":\n" +
                            "\n==============================\n" +
                            "Auth Code  : " + preferences.getString(Constants.AUTH_CODE, "") + "\n" +
                            "Account    : " + preferences.getString(Constants.AUTH_ACCOUNT_NBR, "") + "\n" +
                            "Response   : " + preferences.getString(Constants.AUTH_RESP_TEXT, "") + "\n" +
                            "\n==============================\n" +
                            "Amount : " + preferences.getString(Constants.TRANS_AMOUNT, "") + "\n" +
                            "Tip    : " + preferences.getString(Constants.TIP_AMOUNT, "") + "\n" +
                            "Total  : " + preferences.getString(Constants.TOTAL_AMOUNT, "") + "\n"
                ;
        String emailSubject = getString(R.string.app_name)+" Invoice";
        Log.d(TAG, "Email Subject" + emailSubject + "\nEmail Body: \n" + emailBody);

        //prep email
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL  , new String[]{customerEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name)+" Invoice"); //Subject
        intent.putExtra(Intent.EXTRA_TEXT   , emailBody); //Body
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //To return back to parent app after Back

        String imagePath = preferences.getString(Constants.USER_EMAIL, ""); //find file's path
//        Log.d(TAG, "image path: "+ imagePath);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imagePath)); //Attach file using its path

        //send email
        try {
            startActivity(Intent.createChooser(intent, "Sending email..."));
        } catch (android.content.ActivityNotFoundException ex) {
//            Toast.makeText(PinpadActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Send an email using custom MailSender and MailProvider ONLY with existing GMAIL account
     */
    public void sendEmailOnBackground() {
        String emailBody = "Details for your transaction at " + getString(R.string.app_name) + ":\n" +
                "\n==============================\n" +
                "Auth Code  : " + preferences.getString(Constants.AUTH_CODE, "") + "\n" +
                "Account    : " + preferences.getString(Constants.AUTH_ACCOUNT_NBR, "") + "\n" +
                "Response   : " + preferences.getString(Constants.AUTH_RESP_TEXT, "") + "\n" +
                "\n==============================\n" +
                "Amount : " + preferences.getString(Constants.TRANS_AMOUNT, "") + "\n" +
                "Tip    : " + preferences.getString(Constants.TIP_AMOUNT, "") + "\n" +
                "Total  : " + preferences.getString(Constants.TOTAL_AMOUNT, "") + "\n"
                ;
        String emailSubject = getString(R.string.app_name)+" Invoice";
        Log.d(TAG, "Email Subject" + emailSubject + "\nEmail Body: \n" + emailBody);

        String companyEmail = preferences.getString(Constants.COMPANY_EMAIL_ID, "");
        String companyPass = preferences.getString(Constants.COMPANY_EMAIL_PASS, "");
//        Log.d(TAG, "Email: " + companyEmail + " Pass: " + companyPass);

//        customerEmail = "iskren@astpos.com";
//        Log.d(TAG, "Customer Email: " + customerEmail);

        EmailAsyncTask emailTask = new EmailAsyncTask();
        emailTask.execute(companyEmail, companyPass, emailSubject, emailBody, customerEmail);
    }




    /**
     * Builds a dialog fragment and displays it in the current activity
     * @param errorMessage to be displayed in the dialog
     * @param errorType chosen from Constants triggers different dialog
     * @param errorId standard required char ID for the Dialog
     */
    public void createDialog(String errorMessage, int errorType, String errorId){
        Bundle args = new Bundle();
        args.putString(Constants.ERROR_MSG, errorMessage);
        args.putInt(Constants.ERROR_TYPE, errorType);
        if((dialogFragment.getDialog() != null) && (dialogFragment.getDialog().isShowing())) {
            dialogFragment.dismiss();
            dialogFragment = new AstDialogFragment();
        }
        dialogFragment.setArguments(args);
        dialogFragment.setCancelable(false);
        dialogFragment.show(getFragmentManager(), errorId);
    }



/** ******************************* ASYNC TASKS ************************************************ */
private class EmailAsyncTask extends AsyncTask<String, Void, Boolean> {
    @Override
    protected Boolean doInBackground(String[] params) {
        String companyEmail = params[0];
        String emailPass = params[1];
        String emailSubject = params[2];
        String emailBody = params[3];
        String userEmail = params[4];

        try {
            MailSender sender = new MailSender(companyEmail, emailPass);
            sender.sendMail(emailSubject, //email subject
                    emailBody, //email body
                    companyEmail, //from email account
                    userEmail  //to email account
            );
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean emailIsSent) {
        //process message
        if(emailIsSent){
            Log.d(TAG, "Email sent");
        } else {
            Log.d(TAG, "Email cannot be sent");
        }

        progressBar.setVisibility(View.GONE);
        mainLayout.setAlpha(1.0f);

//            Log.i(Constants.TAG, "onPostExecute Ping: " + pingIsOK);
    }
}



private class PingAsyncTask extends AsyncTask<String, Void, Boolean> {
    @Override
    protected Boolean doInBackground(String[] params) {
        // do above Server call here
//            Log.i(TAG, "ping IP: "+ params[0]);
        if(SettingsActivity.isPingable(params[0])) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean pingIsOK) {
        //process message
        if(pingIsOK){
            editPing.setText("Pinpad Response OK");
            editPing.setBackgroundResource(R.drawable.button_green);
            editPing.setTextColor(Color.GREEN);
        } else {
            editPing.setText("Pinpad unreachable");
            editPing.setBackgroundResource(R.drawable.button_red);
            editPing.setTextColor(Color.WHITE);

            createDialog(getString(R.string.check_connection), Constants.CONNECTION_ERR, "connectionErrorMsg");
        }

        progressBar.setVisibility(View.GONE);
        mainLayout.setAlpha(1.0f);

//            Log.i(Constants.TAG, "onPostExecute Ping: " + pingIsOK);
    }
}



    /**
     * Wrapper class for URL connection Async tasks in the class
     */
    abstract class RequestAsyncTask extends AsyncTask<String, Void, String>{

        protected FrameLayout frameLayout;
    }


    /**
     * process http GET request as an Async and prints the respond out
     */
    class PostAsyncTask extends RequestAsyncTask{

        private Exception exception;
        private String postData;


        public PostAsyncTask(String postData) {
            this.postData = postData;

            this.frameLayout = (FrameLayout)findViewById(R.id.loadingPanelLayout);
        }

        @Override
        protected void onPreExecute() {
            frameLayout.setVisibility(View.VISIBLE);
        }

        protected String doInBackground(String... urls) {
            HttpURLConnection con = null;
            try {
                URL url = new URL(urls[0]);
//                Log.i(TAG, "URL for post: " + url);

                con = (HttpURLConnection)url.openConnection();
                //add request header
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                con.setConnectTimeout(Constants.TIMEOUT_INTERVAL);
//                con.setDoInput(true);
                con.setDoOutput(true);

                if (this.postData != null) {
//                    Log.i(TAG, "Post Data: " + postData);
                    DataOutputStream writer = new DataOutputStream(con.getOutputStream());
                    writer.writeBytes(postData);
                    writer.flush();
                    writer.close();
                }

                int responseCode = con.getResponseCode();
                Log.d(TAG, "doInBackground Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // temp parameters
                String responseString = response.toString();
//                Log.d(TAG, "doInBackground Response: " + responseString);

                return responseString;
            } catch (Exception e) {
                this.exception = e;
                Log.d(TAG, "Error: " + this.exception);
                createDialog(exception.getMessage(), Constants.TIMEOUT_ERR, "exceptionErrorMsg");

                return null;
            } finally {
                con.disconnect();
            }
        }

        protected void onPostExecute(String string) {
//            Log.d(TAG, "onPostExec Response: " + string);
            pinpadTransaction.processResponse(string);
            responsePars = pinpadTransaction.getResponseMap();
            pinpadTransaction.printMap(responsePars);

            //in case activity is exited prior async process to finish
            if(isFinishing()) {
                frameLayout.setVisibility(View.GONE);
                return;
            }

            if(pinpadTransaction.isSuccessful()){
                openSignatureActivity();
                finish();
            } else if(pinpadTransaction.isCanceled()){
                Log.d(TAG, "canceled error ");
                createDialog(responsePars.get(Constants.AUTH_RESP_TEXT), Constants.CANCELED_ERR, "canceledErrorMsg");
            } else if(!((dialogFragment.getDialog() != null)
                    && (dialogFragment.getDialog().isShowing()))) {
                Log.d(TAG, "declined error ");
                createDialog(responsePars.get(Constants.AUTH_RESP_TEXT), Constants.DECLINED_ERR, "declinedErrorMsg");
            }

            frameLayout.setVisibility(View.GONE);
        }
    }


    /**
     * process http GET request as an Async and prints the respond out
     */
    class AsyncTaskForGet extends RequestAsyncTask{

        private Exception exception;
//        private FrameLayout frameLayout;


        public AsyncTaskForGet() {
            frameLayout = (FrameLayout)findViewById(R.id.loadingPanelLayout);
        }

        @Override
        protected void onPreExecute() {
            frameLayout.setVisibility(View.VISIBLE);
        }

        protected String doInBackground(String... urls) {
            HttpURLConnection con = null;
            try {
                URL url = new URL(urls[0]);

                con = (HttpURLConnection)url.openConnection();

                //add request header
                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", USER_AGENT);
                con.setConnectTimeout(5000);

                int responseCode = con.getResponseCode();

                Log.d(TAG, "doInBackground Response Code : " + responseCode);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // temp parameters
                String responseString = response.toString();
//                Log.d(TAG, "Response: " + str);

                return responseString;
            } catch (Exception e) {
                this.exception = e;
                Log.d(TAG, "Error: " + this.exception);
                createDialog(exception.getMessage(), Constants.TIMEOUT_ERR, "exceptionErrorMsg");

                return null;
            } finally {
                con.disconnect();
            }
        }

        protected void onPostExecute(String string) {
//            Log.d(TAG, "onPostExec Response: " + string);
            pinpadTransaction.processResponse(string);
            responsePars = pinpadTransaction.getResponseMap();
            pinpadTransaction.printMap(responsePars);


            //in case activity is exited prior async process to finish
            if(isFinishing()) {
                frameLayout.setVisibility(View.GONE);
                return;
            }

            if(pinpadTransaction.isSuccessful()){
                openSignatureActivity();
                finish();
            }
            else if(pinpadTransaction.isCanceled()){
                Log.d(TAG, "canceled error ");
                createDialog(responsePars.get(Constants.RESP_MSG), Constants.CANCELED_ERR, "canceledErrorMsg");

            } else if(!((dialogFragment.getDialog() != null)
                    && (dialogFragment.getDialog().isShowing()))) {
                Log.d(TAG, "declined error ");
                createDialog(responsePars.get(Constants.RESP_MSG), Constants.DECLINED_ERR, "declinedErrorMsg");

            }

            frameLayout.setVisibility(View.GONE);
        }
    }


}
