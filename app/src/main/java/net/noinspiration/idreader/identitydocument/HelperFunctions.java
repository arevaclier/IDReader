package net.noinspiration.idreader.identitydocument;

import android.annotation.SuppressLint;
import android.content.Context;

import net.noinspiration.idreader.helper.ProcessInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;

public class HelperFunctions {
    /**
     * Returns a string converted from a hexadecimal string
     *
     * @param hex the hexadecimal string to convert
     * @return A string containing human readable text
     */
    public static String fromHexString(String hex) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            str.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        return str.toString();
    }

    /**
     * Transforms a DL date to a date formatted in locale format
     *
     * @param date the date to parse
     * @return The date in locale format
     * @throws ParseException When an error happens while parsing
     */
    public static String toLocaleDate(String date, Context context, String pattern) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        Date d = sdf.parse(date);
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        return dateFormat.format(d);
    }

    @SuppressLint("SimpleDateFormat")
    public static String toSQLDate(String date, boolean isDriverLicence) throws ParseException {
        SimpleDateFormat sdf;
        if(isDriverLicence)
            sdf = new SimpleDateFormat("ddMMyyyy");
        else
            sdf = new SimpleDateFormat("yyMMdd");
        Date d = sdf.parse(date);
        SimpleDateFormat spf = new SimpleDateFormat("yyyyMMdd");
        return spf.format(d);
    }


    /**
     * Converts a hexadecimal string to a byte array
     *
     * @param s the string to convert
     * @return A byte array
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

    public static String fromBytes(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02X", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    /**
     * Reads a ProcessInputStream into an array of bytes
     * Allows the app to keep track of the read progress
     *
     * @param is the input stream
     * @return a byte array
     * @throws IOException When reading fails
     */
    public static byte[] getBytesFromInputStream(ProcessInputStream is, int blockSize) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[blockSize];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    public static String capitalize(String string) {
        String[] words = string.split(" ");
        StringBuilder toReturn = new StringBuilder();
        for (String i : words) {
            i = i.substring(0, 1).toUpperCase() + i.substring(1);
            toReturn.append(" ").append(i);
        }

        return new String(toReturn);
    }
}
