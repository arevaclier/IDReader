package net.noinspiration.idreader.helper;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.jnbis.WsqDecoder;
import org.openJpeg.OpenJPEGJavaDecoder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageHelper {

    private static OpenJPEGJavaDecoder decoder = new OpenJPEGJavaDecoder();
    private static String TAG = "ImageHelper";

    /**
     * Creates a bitmap from a DG2 passport information
     *
     * @param context     Application context
     * @param mimeType    MIME type of the read information
     * @param inputStream InputStream of the DG2
     * @return a bitmap containing the passport/ID photo
     * @throws IOException When decoding the file doesn't work
     */
    public static Bitmap decodeImage(Context context, String mimeType, InputStream inputStream) throws IOException {
        String inputFile = context.getCacheDir().toString() + "/temp.jp2";
        String outputFile = context.getCacheDir().toString() + "/decoded.bmp";
        // Check MIME type
        mimeType = mimeType.toLowerCase();
        // JPEG2000 or JP2 (they're the same)
        if (mimeType.equals(AppProperties.IMAGE_JPEG2000) || mimeType.equals(AppProperties.IMAGE_JP2)) {
            Log.i(TAG, "Decoding JPEG2000");

            // Create file from input stream
            Log.i(TAG, "Creating JP2 file");
            OutputStream output = new FileOutputStream(new File(context.getCacheDir(), "temp.jp2"));
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                Log.i(TAG, "creating image");
            }
            output.close();

            // Decode JP2
            Log.i(TAG, "Decoding JP2 file");
            String[] parameters = new String[4];
            parameters[0] = "-i";
            parameters[1] = inputFile;
            parameters[2] = "-o";
            parameters[3] = outputFile;

            decoder.decodeJ2KtoImage(parameters);
            Log.i(TAG, "Successfully decoded JPEG2000");

            // Create bitmap
            Log.i(TAG, "Creating bitmap");
            Bitmap bitmap = BitmapFactory.decodeFile(outputFile);
            File file = new File(outputFile);
            file.delete();
            return bitmap;
        }
        // WSQ
        else if (mimeType.equals(AppProperties.IMAGE_WSQ)) {
            Log.i(TAG, "Decoding WSQ");
            WsqDecoder wsqDecoder = new WsqDecoder();
            org.jnbis.Bitmap bitmap = wsqDecoder.decode(inputStream);
            byte[] byteData = bitmap.getPixels();
            int[] intData = new int[byteData.length];
            for (int j = 0; j < byteData.length; j++) {
                intData[j] = 0xFF000000 | ((byteData[j] & 0xFF) << 16) | ((byteData[j] & 0xFF) << 8) | (byteData[j] & 0xFF);
            }
            return Bitmap.createBitmap(intData, 0, bitmap.getWidth(), bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        }
        // Other type
        else {
            Log.e(TAG, "Image type not known");
            Log.e(TAG, mimeType);

            // Try to decode the image
            try {
                return BitmapFactory.decodeStream(inputStream);
            }

            // If the image can't be decoded, return null
            catch (Exception e) {
                return null;
            }
        }
    }
}
