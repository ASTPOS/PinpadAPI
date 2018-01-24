package com.astpos.ASTPinpad;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.astpos.ASTPinpad.util.Constants;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.astpos.ASTPinpad.util.Constants.USER_EMAIL;

/**
 * Created by Iskren Iliev on 11/17/17.
 */
public class SignatureActivity extends Activity {

    private Button buttonClear, buttonGetSign, buttonCancel;
    private File file;
    private LinearLayout mContent;
    private View view;
    private Signature  mSignature;
    private Bitmap bitmap;

    // Shared Preferences
    private android.content.SharedPreferences preferences;
    private android.content.SharedPreferences.Editor preferencesEditor;

    // Creating Separate Directory for saving Generated Images
    String DIRECTORY;
    String pic_name ;
    String StoredPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);


        DIRECTORY = Environment.getExternalStorageDirectory().getPath() + "/UserSignature/"; //getCacheDir() + "/UserSigniture/";
        pic_name = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        StoredPath = DIRECTORY + pic_name + ".png";

        this.preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.preferencesEditor = preferences.edit();
        preferencesEditor.putString(USER_EMAIL, StoredPath);
        preferencesEditor.apply();

        mContent = (LinearLayout) findViewById(R.id.canvasLayout);
        mSignature = new Signature (getApplicationContext(), null);
        mSignature.setBackgroundColor(Color.WHITE);
        // Dynamically generating Layout through java code
        mContent.addView(mSignature, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        buttonClear = (Button) findViewById(R.id.clear);
        buttonGetSign = (Button) findViewById(R.id.getsign);
        buttonGetSign.setEnabled(false);
        buttonCancel = (Button) findViewById(R.id.cancel);
        buttonCancel.setVisibility(View.GONE);
        view = mContent;
        buttonGetSign.setOnClickListener(onButtonClick);
        buttonClear.setOnClickListener(onButtonClick);
        buttonCancel.setOnClickListener(onButtonClick);

        // Method to create Directory, if the Directory doesn't exists
        file = new File(DIRECTORY);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    Button.OnClickListener onButtonClick = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == buttonClear) {
                Toast.makeText(getApplicationContext(), "Cleared!", Toast.LENGTH_SHORT).show();
                mSignature.clear();
                buttonGetSign.setEnabled(false);
            } else if (v == buttonGetSign) {
                //Log.v(Constants.TAG, "Panel Saved");
                if (Build.VERSION.SDK_INT >= 23) {
                    if(!isStoragePermissionGranted()) {
                        return;
                    }
                }
                    view.setDrawingCacheEnabled(true);
                    mSignature.save(view, StoredPath);
                    Toast.makeText(getApplicationContext(), "Accepted!", Toast.LENGTH_SHORT).show();
                    // Calling the same class
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        recreate();
                    }
                finish();

            } else if(v == buttonCancel){
                Log.v(Constants.TAG, "Panel Canceled");
                finish();
            }
        }
    };


    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getApplicationContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            view.setDrawingCacheEnabled(true);
            mSignature.save(view, StoredPath);
            Toast.makeText(getApplicationContext(), "Successfully Saved", Toast.LENGTH_SHORT).show();
            // Calling the same class
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                recreate();
            }
        }
        else
        {
            Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onResume(){
        super.onResume();
//        Log.d(TAG, "SignatureActivity  resumed");
    }

    @Override
    protected void onPause(){
        super.onPause();
//        Log.d(TAG, "SignatureActivity paused");
    }

    @Override
    protected void onStop(){
        super.onStop();
//        Log.i(Constants.TAG, "SignatureActivity stop");
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
//        Log.d(TAG, "SignatureActivity destroyed");
    }

    @Override
    public void onBackPressed() {
        //do nothing
//        super.onBackPressed();
    }


    public class Signature  extends View {

        private static final float STROKE_WIDTH = 5f;
        private static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;
        private Paint paint = new Paint();
        private Path path = new Path();

        private float lastTouchX;
        private float lastTouchY;
        private final RectF dirtyRect = new RectF();

        // Shared Preferences

        private final android.content.SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        public Signature(Context context, AttributeSet attrs) {
            super(context, attrs);
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(STROKE_WIDTH);
        }

        public void save(View v, String storedPath) {
            Log.d(Constants.TAG, "Store image path: " + storedPath);
            //Log.d(Constants.TAG, "Width: " + v.getWidth());
            //Log.d(Constants.TAG, "Height: " + v.getHeight());
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(mContent.getWidth(), mContent.getHeight(), Bitmap.Config.RGB_565);
            }
            Canvas canvas = new Canvas(bitmap);
            try {
                // Output the file
                FileOutputStream mFileOutStream = new FileOutputStream(storedPath);
                v.draw(canvas);

                // Convert the output file to Image such as .png
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, mFileOutStream);

                Intent intent;
                intent = new Intent(SignatureActivity.this, PinpadActivity.class);
                intent.putExtra(Constants.IMAGE_PATH, storedPath);
                intent.putExtra(Constants.FROM_SIGN, true);
                if(this.preferences.getBoolean(Constants.USE_NAB, false)) {
                    intent.putExtra(Constants.PROCESSOR_TYPE, Constants.NAB);
                } else if(this.preferences.getBoolean(Constants.USE_PAX, false)){
                    intent.putExtra(Constants.PROCESSOR_TYPE, Constants.PAX);
                } else {
                    intent = new Intent(SignatureActivity.this, HomeMainActivity.class);
                }
                startActivity(intent);

                finish();
                mFileOutStream.flush();
                mFileOutStream.close();

            } catch (Exception e) {
                Log.v(Constants.TAG, e.toString());
            }

        }

        public void clear() {
            path.reset();
            invalidate();
            buttonGetSign.setEnabled(false);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();
            buttonGetSign.setEnabled(true);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    return true;

                case MotionEvent.ACTION_MOVE:

                case MotionEvent.ACTION_UP:

                    resetDirtyRect(eventX, eventY);
                    int historySize = event.getHistorySize();
                    for (int i = 0; i < historySize; i++) {
                        float historicalX = event.getHistoricalX(i);
                        float historicalY = event.getHistoricalY(i);
                        expandDirtyRect(historicalX, historicalY);
                        path.lineTo(historicalX, historicalY);
                    }
                    path.lineTo(eventX, eventY);
                    break;

                default:
                    debug("Ignored touch event: " + event.toString());
                    return false;
            }

            invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

            lastTouchX = eventX;
            lastTouchY = eventY;

            return true;
        }

        private void debug(String string) {
            Log.v(Constants.TAG, string);
        }

        private void expandDirtyRect(float historicalX, float historicalY) {
            if (historicalX < dirtyRect.left) {
                dirtyRect.left = historicalX;
            } else if (historicalX > dirtyRect.right) {
                dirtyRect.right = historicalX;
            }

            if (historicalY < dirtyRect.top) {
                dirtyRect.top = historicalY;
            } else if (historicalY > dirtyRect.bottom) {
                dirtyRect.bottom = historicalY;
            }
        }

        private void resetDirtyRect(float eventX, float eventY) {
            dirtyRect.left = Math.min(lastTouchX, eventX);
            dirtyRect.right = Math.max(lastTouchX, eventX);
            dirtyRect.top = Math.min(lastTouchY, eventY);
            dirtyRect.bottom = Math.max(lastTouchY, eventY);
        }
    }
}