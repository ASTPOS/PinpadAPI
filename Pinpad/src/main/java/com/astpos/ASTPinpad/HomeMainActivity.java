package com.astpos.ASTPinpad;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.astpos.ASTPinpad.util.Constants;


/**
 * Created by Iskren Iliev on 11/14/17.
 */

public class HomeMainActivity extends Activity {

    /**
     * TAG for printing logs
     */
    private final static String TAG = "ASTPOS";

    /**
     * custom toast variables
     */
    static Toast toast;
    static Handler handler;
    /* Custom Toast that minimizes the time msg is shown for*/
    protected final static void showToast(final String text) {
        toast.setText(text);
        toast.show();

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.cancel();
            }
        }, 800);
    }

    // Shared Preferences
    private android.content.SharedPreferences preferences;
    private android.content.SharedPreferences.Editor preferencesEditor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homemain);

        setLayoutPref();

        //create custom toast (shows only for 0.5 sec)
        toast = Toast.makeText(this, "my toast", Toast.LENGTH_SHORT);


        //isTestAccount=testModeUrl.isChecked();

        // create the object of VelocityProcessor class.
        //velocityProcessor=new VelocityProcessor(VelocityConstants.Identytoken,VelocityConstants.appProfileId,VelocityConstants.merchantProfileId,VelocityConstants.workflowId,VelocityConstants.isTestAccount,sessionToken);

//        // create an object of PaxHttpInit class
//        paxHttpInit = new PaxHttpInit();
//        try {
//            paxHttpInit.main(new String[]{"192.168.1.119", "192.168.1.49"});
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        this.preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.preferencesEditor = preferences.edit();

//        preferencesEditor.remove(Constants.TRANS_AMOUNT);
//        preferencesEditor.remove(Constants.TIP_AMOUNT);
//        preferencesEditor.remove(Constants.FROM_SIGN);
        preferencesEditor.remove(Constants.AUTH_CODE);
        preferencesEditor.remove(Constants.AUTH_ACCOUNT_NBR);
        preferencesEditor.remove(Constants.AUTH_RESP_TEXT);

        preferencesEditor.apply();

    }


    public void usePinpadButton(View view){
        Log.i(TAG, "use Pinpad button clicked");
        showToast("Loading transaction...");

        Intent i;
        if(this.preferences.getBoolean(Constants.USE_NAB, false)) {
            Log.i(TAG, "Loading NAB activity");

            i = new Intent(this, PinpadActivity.class);
            i.putExtra(Constants.TRANS_AMOUNT, "1.00");
            i.putExtra(Constants.FROM_SIGN, false);
            i.putExtra(Constants.PROCESSOR_TYPE, Constants.NAB);
            startActivity(i);
        } else if(this.preferences.getBoolean(Constants.USE_PAX, false)) {
            Log.i(TAG, "Loading PAX activity");

            i = new Intent(this, PinpadActivity.class);
            i.putExtra(Constants.TRANS_AMOUNT, "1.00");
            i.putExtra(Constants.FROM_SIGN, false);
            i.putExtra(Constants.PROCESSOR_TYPE, Constants.PAX);
            startActivity(i);
        } else {
            //do not start activity
        }

    }


    public void useSettingsButton(View view){
        Log.i(TAG, "use Settings button clicked");
        showToast("Loading settings...");
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
    }

    private void setLayoutPref(){
        //TODO
        // if needed to preset any variables
    }

}
