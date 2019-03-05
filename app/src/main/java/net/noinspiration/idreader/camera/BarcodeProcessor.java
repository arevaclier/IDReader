package net.noinspiration.idreader.camera;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.otaliastudios.cameraview.Frame;
import com.otaliastudios.cameraview.FrameProcessor;
import com.otaliastudios.cameraview.Size;

import net.noinspiration.idreader.helper.AppProperties;
import net.noinspiration.idreader.interfaces.VisionInterface;

import java.util.List;

import androidx.annotation.NonNull;

//import android.util.Log;

/**
 * This class uses the Firebase ML Kit to read QR codes. It can be easily adapted to read other types
 * of barcodes if necessary
 */
public class BarcodeProcessor implements FrameProcessor {

    private final static String TAG = "BarcodeProcessor";
    private long storedTime;

    // Options for the Barcode Detector. Set to QR codes only
    private FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build();
    // Firebase barcode detector
    private FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
    // The calling activity. Used to provide callbacks
    private VisionInterface callingActivity;


    /**
     * Constructor
     *
     * @param callingActivity the calling activity
     */
    public BarcodeProcessor(VisionInterface callingActivity) {
        this.callingActivity = callingActivity;
        storedTime = 0;
        Log.d(TAG, "Called Barcode");
    }

    /**
     * The processing thread, called for each frame sent by the camera
     * Only processes 4 frames per second to not overload the CPU. Can be changed in AppProperties
     *
     * @param frame the camera frame
     */
    @Override
    public void process(@NonNull Frame frame) {
        // Only analyze @{CAMERA_FPS} frames per second if they come from the correct activities
        if ((System.currentTimeMillis() - storedTime) < 1000 / AppProperties.CAMERA_FPS ||
                !AppProperties.activityName.equals("DrivingLicenceReaderActivity")) {
            return;
        } else {
            storedTime = System.currentTimeMillis();


            // Get frame size
            Size size = frame.getSize();

            // Create metadata
            FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                    .setWidth(size.getWidth())
                    .setHeight(size.getHeight())
                    .setFormat(frame.getFormat())
                    .setRotation(calculateRotation(frame.getRotation()))
                    .build();

            // Create firebase image
            FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(frame.getData(), metadata);

            // Process image
            detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                @Override
                public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                    // If the image is processed correctly
                    for (FirebaseVisionBarcode barcode : firebaseVisionBarcodes) {
                        // Extract potential QR-codes and send back to calling activity
                        String rawValue = barcode.getRawValue();
                        callingActivity.onSuccessfulVision(rawValue);
                    }
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // In case of error
                            Log.e("Detector", e.toString());
                        }
                    });
        }
    }

    /**
     * Calculates the rotation to apply to the firebase image based on the camera rotation and position
     *
     * @param rotation the rotation of the frame
     * @return A firebase integer representing the rotation to apply to the frame
     */
    private int calculateRotation(int rotation) {
        int result;
        switch (rotation) {
            case 0:
                result = FirebaseVisionImageMetadata.ROTATION_0;
                break;
            case 90:
                result = FirebaseVisionImageMetadata.ROTATION_90;
                break;
            case 180:
                result = FirebaseVisionImageMetadata.ROTATION_180;
                break;
            case 270:
                result = FirebaseVisionImageMetadata.ROTATION_270;
                break;
            default:
                result = FirebaseVisionImageMetadata.ROTATION_0;
        }
        return result;
    }
}
