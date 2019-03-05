package net.noinspiration.idreader.camera;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.otaliastudios.cameraview.Frame;
import com.otaliastudios.cameraview.FrameProcessor;
import com.otaliastudios.cameraview.Size;

import net.noinspiration.idreader.helper.AppProperties;
import net.noinspiration.idreader.interfaces.VisionInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

/**
 * Passport/ID card MRZ reading class using Firebase ML Kit (Text recognition)
 */
public class TextProcessor implements FrameProcessor {

    // Values for check digits
    private final static Map<String, Integer> checkValues;

    static {
        checkValues = new HashMap<>();
        checkValues.put("0", 0);
        checkValues.put("1", 1);
        checkValues.put("2", 2);
        checkValues.put("3", 3);
        checkValues.put("4", 4);
        checkValues.put("5", 5);
        checkValues.put("6", 6);
        checkValues.put("7", 7);
        checkValues.put("8", 8);
        checkValues.put("9", 9);
        checkValues.put("A", 10);
        checkValues.put("B", 11);
        checkValues.put("C", 12);
        checkValues.put("D", 13);
        checkValues.put("E", 14);
        checkValues.put("F", 15);
        checkValues.put("G", 16);
        checkValues.put("H", 17);
        checkValues.put("I", 18);
        checkValues.put("J", 19);
        checkValues.put("K", 20);
        checkValues.put("L", 21);
        checkValues.put("M", 22);
        checkValues.put("N", 23);
        checkValues.put("O", 24);
        checkValues.put("P", 25);
        checkValues.put("Q", 26);
        checkValues.put("R", 27);
        checkValues.put("S", 28);
        checkValues.put("T", 29);
        checkValues.put("U", 30);
        checkValues.put("V", 31);
        checkValues.put("W", 32);
        checkValues.put("X", 33);
        checkValues.put("Y", 34);
        checkValues.put("Z", 35);
    }

    private final String TAG = "TextProcessor";

    private VisionInterface callingActivity;
    // Keeps track of a running instance of the OCR.
    private long currentTime;
    // TD2 Documents regex (Passports)
    private String regexTD2Row2 = "[A-Z0-9<]{9}\\d[A-Z<]{3}\\d{7}[MF<]\\d{7}";
    // TD1 Documents regex (ID cards)
    private String regexTD1Row1 = "I[A-Z<]{4}[A-Z0-9<]{9}\\d";
    private String regexTD1Row2 = "\\d{7}[MF<]\\d{7}";
    // Firebase object for text recognition
    private FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();

    public TextProcessor(VisionInterface callingActivity) {
        this.callingActivity = callingActivity;
        currentTime = System.currentTimeMillis();
        Log.d(TAG, "Called Text");
    }

    /**
     * Called when a new frame is captured by the camera
     * Processes 4 frames per second to not overload the CPU. Can be changed in AppProperties
     *
     * @param frame The frame to analyze
     */
    @Override
    public void process(@NonNull Frame frame) {
        // If the size of the frame is unknown, or the text processor is already running, we drop the frame
        // This improves performance and RAM use tremendously

        if ((System.currentTimeMillis() - currentTime) < 1000 / AppProperties.CAMERA_FPS || !AppProperties.activityName.equals("PassportReaderActivity")) {
            return;
        }
        currentTime = System.currentTimeMillis();
        Size size = frame.getSize();

        // Create metadata
        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setWidth(size.getWidth())
                .setHeight(size.getHeight())
                .setFormat(frame.getFormat())
                .setRotation(calculateRotation(frame.getRotation()))
                .build();

        // Create a Firebase image from the frame
        FirebaseVisionImage image = FirebaseVisionImage.fromByteArray(frame.getData(), metadata);

        // Process the image
        detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {
                        // On successful detection, concatenate all found text, and remove lowercase characters and whitespace
                        StringBuilder text = new StringBuilder();
                        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
                            text.append(block.getText());
                        }
                        String finalText = text.toString();
                        finalText = finalText.replaceAll("\\p{javaLowerCase}|\\s", "");
                        if (checkText(finalText, regexTD2Row2)) {
                            // Passport
                            String row = getText(finalText, regexTD2Row2);
                            if (row != null) {
                                try {
                                    // Try to obtain the necessary information to do BAC for a passport
                                    String docNumber = row.substring(0, 9).replaceAll("<", "");
                                    int checkDigitDocNumber = Integer.parseInt(row.substring(9, 10));

                                    String dob = row.substring(13, 19);
                                    int checkDigitDob = Integer.parseInt(row.substring(19, 20));

                                    String expiryDate = row.substring(21, 27);
                                    int checkDigitDoe = Integer.parseInt(row.substring(27, 28));

                                    // If each of the three variables matches with its check digit, send the result back with a "/" in between each information
                                    if (checkDigit(docNumber, checkDigitDocNumber) && checkDigit(dob, checkDigitDob) && checkDigit(expiryDate, checkDigitDoe)) {
                                        String toReturn = String.format("%s/%s/%s", docNumber, dob, expiryDate);
                                        callingActivity.onSuccessfulVision(toReturn);
                                    }

                                    // Replace "O" by "0" as the reader can be confused
                                    docNumber = docNumber.replaceAll("O", "0");
                                    if (checkDigit(docNumber, checkDigitDocNumber) && checkDigit(dob, checkDigitDob) && checkDigit(expiryDate, checkDigitDoe)) {
                                        String toReturn = String.format("%s/%s/%s", docNumber, dob, expiryDate);
                                        callingActivity.onSuccessfulVision(toReturn);
                                    }
                                } catch (Exception e) {
                                    // Mostly NumberFormatException, which indicates one of the check digits wasn't read properly, hence a wrong format
                                    Log.i(TAG, "Wrong format TD2");
                                }


                            }
                        } else if (checkText(finalText, regexTD1Row1) && checkText(finalText, regexTD1Row2)) {
                            // ID card
                            String row1 = getText(finalText, regexTD1Row1);
                            String row2 = getText(finalText, regexTD1Row2);
                            if (row1 != null && row2 != null) {
                                try {
                                    // Try to obtain the necessary information to do BAC for an ID card

                                    String docNumber = row1.substring(5, 14).replaceAll("<", "");
                                    int checkDigitDocNumber = Integer.parseInt(row1.substring(14, 15));

                                    String dob = row2.substring(0, 6);
                                    int checkDigitDob = Integer.parseInt(row2.substring(6, 7));

                                    String expiryDate = row2.substring(8, 14);
                                    int checkDigitDoe = Integer.parseInt(row2.substring(14, 15));

                                    // If each of the three variables matches with its check digit, send the result back with a "/" in between each information
                                    if (checkDigit(docNumber, checkDigitDocNumber) && checkDigit(dob, checkDigitDob) && checkDigit(expiryDate, checkDigitDoe)) {
                                        String toReturn = String.format("%s/%s/%s", docNumber, dob, expiryDate);
                                        callingActivity.onSuccessfulVision(toReturn);
                                    }
                                } catch (Exception e) {
                                    // Mostly NumberFormatException, which indicates one of the check digits wasn't read properly, hence a wrong format
                                    Log.e(TAG, "Wrong format TD1");
                                }
                            }

                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Detector", e.toString());
                    }
                });
    }

    /**
     * Checks a value against its check digit
     *
     * @param string     The string to check
     * @param checkDigit The associated check digit
     * @return True if @string matches @checkDigit, false otherwise
     * <p>
     * Check digits are calculated the following way:
     * 1. Multiply each digit by the correct weighing factor following a repetitive weighing of 7-3-1
     * 2. Add the products of each multiplication
     * 3. Take the remainder of the sum modulo 10
     * 4. The remainder should be equal to the check digit
     */
    private boolean checkDigit(String string, int checkDigit) {
        int sum = 0;
        for (int i = 0; i < string.length(); i++) {

            // For each digit, check the value in the table
            String toGet = String.valueOf(string.charAt(i));
            int digit = checkValues.get(toGet);

            // Weighing factor
            switch (i % 3) {
                case 0:
                    sum += digit * 7;
                    break;
                case 1:
                    sum += digit * 3;
                    break;
                case 2:
                    sum += digit;
                    break;
            }
        }

        // Modulo
        sum = sum % 10;
        return sum == checkDigit;
    }

    /**
     * Calculate the correct image rotation for firebase based on the camera sensor
     *
     * @param rotation The rotation to apply to the image so that it is the same as what the user sees
     * @return The correct firebase rotation factor for the image
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

    /**
     * Checks if a substring appears in text using a regex
     *
     * @param input the text to check
     * @param regex the regex to match against
     * @return Whether the text matches the regex
     */
    private boolean checkText(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.find();
    }

    /**
     * Returns text matching a regex if it exists
     *
     * @param input the text to check
     * @param regex the regex to check against
     * @return a substring of @input matching @regex
     */
    private String getText(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find())
            return input.substring(matcher.start(), matcher.end());
        else return null;
    }
}