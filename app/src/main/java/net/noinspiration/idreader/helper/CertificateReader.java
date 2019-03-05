package net.noinspiration.idreader.helper;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.interfaces.CertificateInterface;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static net.noinspiration.idreader.helper.AppProperties.hexStringToByteArray;


/**
 * Class allowing reading of the ICAO PKD Masterlist to get CSCAs
 * The read is done in a separate thread to allow the user to continue using the application in the
 * meantime
 */
public class CertificateReader implements Runnable {

    private static final String TAG = "CertificateReader";
    private final Context context;
    private final CertificateInterface callingActivity;
    private HashMap<String, Set<X509Certificate>> certificates = new HashMap<>();

    /**
     * Constructor, class needs @context to open masterlist
     *
     * @param context The application context
     */
    public CertificateReader(Context context, CertificateInterface callingActivity) {
        this.context = context;
        this.callingActivity = callingActivity;
    }

    @Override
    public void run() {
        try {
            // Get the masterlist
            InputStream is = context.getResources().openRawResource(R.raw.masterlist);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            // Retrieve the masterlists of the countries represented in the file in base64
            StringBuilder stringb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringb.append(line);
            }
            String s = stringb.toString();
            ArrayList<String> base64lists = new ArrayList<>();
            while (s.contains("CscaMasterListData::")) {
                int startIndex = s.indexOf("CscaMasterListData::") + 21;
                s = s.substring(startIndex);
                int endIndex = s.indexOf("dn:");
                String cut;
                if (endIndex == -1)
                    cut = s.replaceAll("\\s+", "");
                else
                    cut = s.substring(0, endIndex).replaceAll("\\s+", "");
                base64lists.add(cut);
            }
            Log.d(TAG, "Done reading master list");

            Set<X509Certificate> certificatesToAdd = new HashSet<>();

            // For each base64 masterlist, decrypt it and extract the CSCA certificates
            for (String i : base64lists) {
                byte[] decoded = Base64.decode(i, Base64.DEFAULT);
                ASN1InputStream input = new ASN1InputStream(new ByteArrayInputStream(decoded));
                String contents;

                if (input.available() > 0) {
                    ASN1Primitive obj = input.readObject();
                    contents = obj.toString();

                    // The string containing the certificates is stored at object ID 2.23.136.1.1.2
                    if (contents.contains("2.23.136.1.1.2")) {
                        contents = contents.substring(contents.indexOf("2.23.136.1.1.2"));
                        contents = contents.substring(contents.indexOf("#") + 1);
                        if (contents.contains("3183"))
                            contents = contents.substring(contents.indexOf("3183") + 10, contents.indexOf("]"));
                        else
                            contents = contents.substring(contents.indexOf("3182") + 10, contents.indexOf("]"));

                        // Turn the hex string into a byte array that CertificateFactory can interpret
                        byte[] encoded = hexStringToByteArray(contents);
                        Collection<X509Certificate> collection = (Collection<X509Certificate>) CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(encoded));
                        certificatesToAdd.addAll(collection);
                    }
                }
                input.close();
            }

            Log.d(TAG, "Done reading certificates");

            Set<X509Certificate> countryCerts = null;
            String lastCountry = "";

            // Sort the certificates by country
            for (X509Certificate c : certificatesToAdd) {
                String issuer = c.getIssuerDN().getName();
                int index = issuer.indexOf("C=") + 2;
                String country = issuer.substring(index, index + 2).toUpperCase();
                try {
                    c.checkValidity();
                    if (certificates.containsKey(country)) {
                        Set<X509Certificate> set;
                        // Country already present in the map
                        if (lastCountry.equals(country)) {
                            set = countryCerts;
                        } else {
                            set = certificates.get(country);
                        }
                        set.add(c);
                    } else {
                        // Country not present in the map
                        Set<X509Certificate> set = new HashSet<>();
                        countryCerts = set;
                        set.add(c);
                        certificates.put(country, set);
                    }
                } catch (CertificateExpiredException e) {
                    // Remove expired certificates
                    Log.d(TAG, "Certificate " + country + ":" + c.getSerialNumber() + " is expired");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "readCertificates: " + e.toString());
        }

        Log.d(TAG, "Done sorting certificates");

        // Store certificates in a file so that no reading is necessary next time
        try {
            FileOutputStream fos = context.openFileOutput(AppProperties.CERTIFICATE_FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(certificates);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AppProperties.certificates = certificates;
        callingActivity.notifyRead();
    }
}
