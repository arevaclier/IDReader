package net.noinspiration.idreader.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.camera.TextProcessor;
import net.noinspiration.idreader.helper.AppProperties;
import net.noinspiration.idreader.helper.BACKeyHelper;
import net.noinspiration.idreader.interfaces.VisionInterface;

import org.jmrtd.BACKey;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class PassportReaderActivity extends AppCompatActivity implements VisionInterface {

    private static final String TAG = "PassportReaderActivity";

    private TextProcessor processor = new TextProcessor(this);

    //Camera
    private CameraView cameraView;
    private int cameraCounter = 0;

    // Instances
    private boolean intentStarted = false;

    //Intent extras
    private String docType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passport_reader);

        ImageView mrzOverlay = findViewById(R.id.mrzOverlay);

        // Change overlay depending on type of document
        docType = getIntent().getStringExtra("doctype");
        switch (docType) {
            case AppProperties.DOCTYPE_IDCARD:
                mrzOverlay.setImageDrawable(getDrawable(R.drawable.mrz_overlay_idcard));
                break;
            case AppProperties.DOCTYPE_PASSPORT:
                mrzOverlay.setImageDrawable(getDrawable(R.drawable.mrz_overlay_passport));
                break;
            default:
                mrzOverlay.setImageDrawable(getDrawable(R.drawable.mrz_overlay_passport));
        }

        AppProperties.activityName = this.getClass().getSimpleName();

        //Camera
        cameraView = findViewById(R.id.cameraView);
        startCamera();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        startCamera();
    }

    @Override
    public void onPause() {
        stopCamera();
        super.onPause();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        startCamera();
    }

    @Override
    public void onStop() {
        stopCamera();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        stopCamera();
        super.onDestroy();
    }

    @Override
    public void onSuccessfulVision(String result) {
        Log.i(TAG, "onSuccessfulVision: Read BAC");
        BACKeyHelper bacKeyHelper = null;
        try {
            String info[] = result.split("/");
            BACKey bacKey = new BACKey(info[0], info[1], info[2]);
            bacKeyHelper = new BACKeyHelper(bacKey);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "onSuccessfulVision: Incorrect BAC Key");
        }

        if (!intentStarted && bacKeyHelper != null && bacKeyHelper.getBacKey() != null) {
            intentStarted = true;
            stopCamera();
            Intent intent = new Intent(this, NFCActivity.class);
            intent.putExtra("backey", bacKeyHelper);
            intent.putExtra("activity", AppProperties.ACTIVITY_PASSPORT_SCAN);
            intent.putExtra("doctype", docType);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onFailureVision(String exception) {
        Log.e(TAG, "onFailureVision: TextRecognition failed");
        Log.e(TAG, "onFailureVision: " + exception);
    }

    private void stopCamera() {
        cameraView.clearFrameProcessors();
        cameraView.clearCameraListeners();
        cameraView.close();
    }

    private void startCamera() {
        cameraCounter = 0;
        cameraView.open();

        // Remove sounds, set focus
        cameraView.setPlaySounds(false);
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER);
        cameraView.addFrameProcessor(processor);

        // Camera errors
        cameraView.addCameraListener(new CameraListener() {
            @Override
            public void onCameraError(@NonNull CameraException exception) {
                cameraCounter++;
                if (cameraCounter >= 3) {
                    Log.e(TAG, "Camera error " + exception.getMessage());
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.error_camera, Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    cameraView.open();
                }
            }
        });
    }

}
