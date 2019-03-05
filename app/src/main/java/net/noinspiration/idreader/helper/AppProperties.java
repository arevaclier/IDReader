package net.noinspiration.idreader.helper;

import android.annotation.SuppressLint;
import android.content.Context;

import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class AppProperties {

    // Permission request codes
    public static final int PERMISSION_REQUEST_CAMERA = 0;

    // Constants
    public final static String DRIVING_LICENCE_MRZ_VALIDATION = "D1";

    // Activities
    public final static int ACTIVITY_DL_SCAN = 0;
    public final static int ACTIVITY_PASSPORT_SCAN = 1;

    // Document types
    public final static String DOCTYPE_DRIVERS_LICENCE = "DrivingLicence";
    public final static String DOCTYPE_PASSPORT = "Passport";
    public final static String DOCTYPE_IDCARD = "IDcard";

    // NFC
    public final static String NFC_TECH = "android.nfc.tech.IsoDep";

    //NFC UI updates
    public final static int NFC_STAGE_1 = 0;
    public final static int NFC_STAGE_2 = 1;
    public final static int NFC_STAGE_3 = 2;
    public final static int NFC_STAGE_4 = 3;

    // Image types
    public final static String IMAGE_JP2 = "image/jp2";
    public final static String IMAGE_JPEG2000 = "image/jpeg2000";
    public final static String IMAGE_WSQ = "image/x-wsq";

    // Frames to process per second for barcode and text detection
    public final static int CAMERA_FPS = 1;
    public static String activityName = "";

    // Country certificate for Root CA validation
    public static Map<String, Set<X509Certificate>> certificates;
    public static final String CERTIFICATE_FILE_NAME = "certificates.cer";

    /* ---------------- FUNCTIONS ---------------- */

    public static String subtractYears(String date, int years) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Date d = sdf.parse(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(d);
        calendar.add(Calendar.YEAR, -years);
        calendar.add(Calendar.DAY_OF_MONTH, 1);

        return sdf.format(calendar.getTime());
    }

    /**
     * Computes the byte array of an hexadecimal string
     *
     * @param s the hexadecimal string to convert
     * @return A representation of @s as a byte array
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
