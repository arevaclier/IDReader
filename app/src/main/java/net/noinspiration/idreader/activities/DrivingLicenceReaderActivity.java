package net.noinspiration.idreader.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.camera.BarcodeProcessor;
import net.noinspiration.idreader.helper.AppProperties;
import net.noinspiration.idreader.interfaces.VisionInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class DrivingLicenceReaderActivity extends AppCompatActivity implements VisionInterface {
    private static final String TAG = "DLReaderActivity";

    private BarcodeProcessor processor = new BarcodeProcessor(this);

    //Camera
    private CameraView cameraView;
    private int cameraCounter = 0;

    // Instances
    private boolean intentStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving_licence_reader);

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
        Log.i(TAG, "onSuccessfulVision: Read MRZ");
        if (result.startsWith(AppProperties.DRIVING_LICENCE_MRZ_VALIDATION)) {
            if (!intentStarted) {
                stopCamera();
                intentStarted = true;
                Intent intent = new Intent(this, NFCActivity.class);
                intent.putExtra("mrz", result);
                intent.putExtra("activity", AppProperties.ACTIVITY_DL_SCAN);
                startActivity(intent);
                finish();
            }
        } else {
            Toast toast = Toast.makeText(this, getString(R.string.dl_qr_error), Toast.LENGTH_SHORT);
            toast.show();
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
