package net.noinspiration.idreader.identitydocument;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

import net.noinspiration.idreader.helper.AppProperties;
import net.noinspiration.idreader.helper.ImageHelper;
import net.noinspiration.idreader.helper.ProcessInputStream;
import net.noinspiration.idreader.interfaces.InputStreamListener;
import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardServiceException;

import org.jmrtd.BACKey;
import org.jmrtd.PassportService;
import org.jmrtd.lds.SODFile;
import org.jmrtd.lds.icao.DG11File;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.DG7File;
import org.jmrtd.lds.icao.MRZInfo;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;

import static net.noinspiration.idreader.helper.AppProperties.subtractYears;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.capitalize;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.fromBytes;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.getBytesFromInputStream;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.toLocaleDate;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.toSQLDate;

/**
 * Class allowing the reading of a passport/ID card compliant with the ICAO 9303 document
 * Tracks progress in real time based on the implementation of @ProcessInputStream
 * The photo stored in identity document is based on a couple different formats that Android
 * cannot read but that can either be interpreted by OpenJPEG or converted into a JPG format by
 * removing the header of the file until the bytes FF D8 are met.
 */
public class PassportHelper implements InputStreamListener {

    // Debug name
    private final static String TAG = "PassportHelper";

    // Data groups present on the NFC chip
    private final static SparseArray<Short> dataGroups;
    // Maximum size for BINARY READ
    private final static int MAX_BLOCK_READ_SIZE = 200;

    static {
        dataGroups = new SparseArray<>();
        dataGroups.append(1, PassportService.EF_DG1);
        dataGroups.append(2, PassportService.EF_DG2);
        dataGroups.append(3, PassportService.EF_DG3);
        dataGroups.append(4, PassportService.EF_DG4);
        dataGroups.append(5, PassportService.EF_DG5);
        dataGroups.append(6, PassportService.EF_DG6);
        dataGroups.append(7, PassportService.EF_DG7);
        dataGroups.append(8, PassportService.EF_DG8);
        dataGroups.append(9, PassportService.EF_DG9);
        dataGroups.append(10, PassportService.EF_DG10);
        dataGroups.append(11, PassportService.EF_DG11);
        dataGroups.append(12, PassportService.EF_DG12);
        dataGroups.append(13, PassportService.EF_DG13);
        dataGroups.append(14, PassportService.EF_DG14);
        dataGroups.append(15, PassportService.EF_DG15);
        dataGroups.append(16, PassportService.EF_DG16);
    }

    // Listener callback for progress
    private final InputStreamListener progressListener;

    // The object used to access an identity document
    private final PassportService passportService;

    // Document holder information
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String dateOfIssue;
    private String dateOfExpiry;
    private String documentNumber;
    private String documentType;
    private String issuingState;
    private String gender;
    private String nationality;
    private String placeOfBirth;
    private String photo;

    // Document signing
    private boolean authenticationSuccess = false;
    private boolean datagroupHashesSuccess = false;
    private boolean documentSignerSuccess = false;
    private boolean countrySignerSuccess = false;

    private X509Certificate dscCertificate;
    private X509Certificate cscaCertificate;

    private SparseArray<String> datagroupHashes = new SparseArray<>();
    private SparseArray<String> datagroupControl = new SparseArray<>();

    // Context of the application for UI updates
    private Context context;

    // Holds files that may be read more than once
    private DG1File dg1 = null;
    private DG2File dg2 = null;
    private DG11File dg11 = null;
    private SODFile sod = null;

    // Is the document legitimate
    private boolean certified;

    // UI related booleans
    private boolean isCheckingCertificate;
    private boolean readingSOD;

    /**
     * Constructor
     *
     * @param passportService  the JMRTD object to access an identity document
     * @param bacKey           the Basic Access Control key to authenticate
     * @param context          the application context (for UI updates)
     * @param progressListener the listener that tracks progress
     * @throws CardServiceException When authentication or reading doesn't work
     */
    public PassportHelper(PassportService passportService, BACKey bacKey, Context context, InputStreamListener progressListener) throws CardServiceException {
        this.passportService = passportService;
        this.context = context;
        this.progressListener = progressListener;

        // Perform authentication
        passportService.sendSelectApplet(false);
        passportService.doBAC(bacKey);

        authenticationSuccess = true;
    }

    /**
     * Function from the InputStreamListener that calls @NFCActivity to update the UI
     *
     * @param percent the progress of the current reading stage
     */
    @Override
    public void process(int percent) {
        if (!isCheckingCertificate)
            progressListener.process(percent);
        else {
            if (readingSOD) {
                // Map the reading of the SOD to 30% of the progress bar
                progressListener.process((int) (percent * 0.3));
            }
        }
    }

    /**
     * Reads the DG1 datagroup. Contains the document's holder information
     *
     * @throws CardServiceException When something goes wrong with the chip reading
     * @throws IOException          When extracting the data group fails
     * @throws ParseException       When parsing dates or names fails
     * @throws SignatureException
     */
    public void readDG1() throws CardServiceException, IOException, ParseException, SignatureException {
        // Read DG1
        readFile(PassportService.EF_DG1);

        // Extract MRZ info from DG1
        MRZInfo info = dg1.getMRZInfo();

        // Extract first and last name and format them
        firstName = capitalize(info.getSecondaryIdentifier().toLowerCase());
        lastName = capitalize(info.getPrimaryIdentifier().toLowerCase());
        firstName = firstName.replace("<", " ").trim();
        lastName = lastName.replace("<", " ").trim();

        // Extract dates and format them to SQL format
        dateOfBirth = toSQLDate(info.getDateOfBirth(), false);
        dateOfExpiry = toSQLDate(info.getDateOfExpiry(), false);
        dateOfIssue = subtractYears(dateOfExpiry, 10);

        // Get document number, issuing state and document type (passport or ID card)
        documentNumber = info.getDocumentNumber();
        issuingState = info.getIssuingState();

        // Get extra information
        nationality = info.getNationality();
        gender = capitalize(info.getGender().name().toLowerCase());

        if (info.getDocumentType() == MRZInfo.DOC_TYPE_ID1) {
            documentType = AppProperties.DOCTYPE_IDCARD;
        } else {
            documentType = AppProperties.DOCTYPE_PASSPORT;
        }

        // Try to extract DG11 if present (additional holder information)
        try {
            readFile(PassportService.EF_DG11);
            Log.d(TAG, "Extracting DG11 information");

            // Get the full name from DG11 and format it
            String name = dg11.getNameOfHolder();
            String names[] = name.split("<<");
            String lastNames = names[0];
            String firstNames = names[1];
            lastName = capitalize(lastNames.replace("<", " ").toLowerCase());
            firstName = capitalize(firstNames.replace("<", " ").toLowerCase());
            List<String> birthPlace = dg11.getPlaceOfBirth();
            if (birthPlace != null) {
                placeOfBirth = capitalize(birthPlace.get(0).toLowerCase());
            }

        } catch (CardServiceException e) {
            Log.d(TAG, "DG11 not present");
        }
    }

    /**
     * Reads DG2 datagroup (holder's photo)
     *
     * @throws CardServiceException When something goes wrong with chip reading
     * @throws IOException          When extracting the data group fails
     * @throws SignatureException
     */
    public void readDG2() throws CardServiceException, IOException, SignatureException {
        // Read DG2
        readFile(PassportService.EF_DG2);

        // Extract face information
        List<FaceImageInfo> faceImageInfos = new ArrayList<>();
        List<FaceInfo> faceInfos = dg2.getFaceInfos();
        for (FaceInfo faceInfo : faceInfos) {
            faceImageInfos.addAll(faceInfo.getFaceImageInfos());
        }

        // If there is a picture encoded in the passport
        if (!faceImageInfos.isEmpty()) {
            // Fetch picture
            FaceImageInfo faceImageInfo = faceImageInfos.iterator().next();
            int imageLength = faceImageInfo.getImageLength();
            DataInputStream dataInputStream = new DataInputStream(faceImageInfo.getImageInputStream());
            byte[] buffer = new byte[imageLength];
            dataInputStream.readFully(buffer, 0, imageLength);
            InputStream inputStream = new ByteArrayInputStream(buffer, 0, imageLength);
            Bitmap picture = ImageHelper.decodeImage(context, faceImageInfo.getMimeType(), inputStream);

            // If the photo is extracted correctly
            if (picture != null) {
                // Store image to file
                String output = context.getCacheDir().toString() + "/passport.jpg";
                FileOutputStream out = new FileOutputStream(output);
                picture.compress(Bitmap.CompressFormat.JPEG, 100, out);
                photo = output;
            }
        }
    }

    /**
     * Checks the document's legitimacy against:
     * 1. The dates present on the document's embedded certificate
     * 2. The document's country root CA (CSCA)
     * 3. The document's certificate signature (DSC)
     * 4. The hashes of each data group
     */
    public void checkLegitimacy() {
        isCheckingCertificate = true;
        readingSOD = true;

        // Try extracting the SOD
        try {
            readFile(PassportService.EF_SOD);
        } catch (Exception e) {
            // In case it fails, return
            certified = false;
            Log.e(TAG, "Error reading SOD " + e.toString());
            return;
        }

        // Extract the embedded docuement's certificate
        X509Certificate certificate = sod.getDocSigningCertificate();
        if (certificate == null) {
            Log.e(TAG, "Certificate non existent");
            certified = false;
            return;
        }

        dscCertificate = certificate;

        readingSOD = false;

        // Verify validity
        try {
            certificate.checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
            certified = false;
            Log.e(TAG, "Certificate dates invalid");
            isCheckingCertificate = false;
            return;
        }

        progressListener.process(32);

        // Check against country's root CA (CSCA)
        checkCSCA(certificate);
        if (!certified) {
            isCheckingCertificate = false;
            return;
        }
        countrySignerSuccess = true;

        // Check certificate signature (DSC)
        checkDSC(certificate);
        if (!certified) {
            isCheckingCertificate = false;
            return;
        }
        documentSignerSuccess = true;

        // Check data group hashes
        try {
            checkDatagroupHashes();
        } catch (Exception e) {
            certified = false;
        }
        if (certified)
            datagroupHashesSuccess = true;

        isCheckingCertificate = false;
    }

    /**
     * Validates a document's certificate against the country's root certificate (public key validation)
     *
     * @param certificate The certificate to check
     */
    private void checkCSCA(@NonNull X509Certificate certificate) {
        // Get certificate issuer country
        String country = certificate.getIssuerDN().toString();
        int index = country.indexOf("C=") + 2;
        country = country.substring(index, index + 2).toUpperCase();

        // Only check against certificate emitted by the document's country
        if (AppProperties.certificates.containsKey(country)) {
            // Get country certificates
            Set<X509Certificate> countryCertificates = AppProperties.certificates.get(country);
            for (X509Certificate c : countryCertificates) {
                // Check the certificate
                try {
                    // If valid
                    certificate.verify(c.getPublicKey());
                    certified = true;
                    cscaCertificate = c;
                    Log.d(TAG, "Root CA valid (Certificate " + country + ": " + c.getSerialNumber() + ")");
                    break;
                } catch (Exception e) {
                    // If invalid
                    Log.d(TAG, "Verification failed (Certificate " + country + ": " + c.getSerialNumber() + ")");
                    certified = false;
                }
            }
        } else {
            certified = false;
        }

        progressListener.process(45);
    }

    /**
     * Check a certificate's signature
     *
     * @param certificate The certificate to check
     */
    private void checkDSC(@NonNull X509Certificate certificate) {
        try {
            Signature signature = Signature.getInstance(certificate.getSigAlgName());
            signature.initVerify(certificate.getPublicKey());
            signature.update(sod.getEContent());
            signature.verify(certificate.getSignature());
            Log.d(TAG, "DSC validation succeeded");
        } catch (Exception e) {
            certified = false;
            Log.e(TAG, "DSC validation failed");
        }

        progressListener.process(50);
    }

    /**
     * Compute data group hashes
     *
     * @throws NoSuchAlgorithmException When unable to hash using the algorithm embedded in the certificate
     * @throws NullPointerException     When reading goes wrong
     */
    private void checkDatagroupHashes() throws NoSuchAlgorithmException, NullPointerException {
        // Get data groups signatures and a digest for the algorithm used to hash them
        Map<Integer, byte[]> hashes = sod.getDataGroupHashes();
        MessageDigest crypt = MessageDigest.getInstance(sod.getDigestAlgorithm());

        byte[] hash, control;
        int counter = 0;
        Set<Integer> keys = hashes.keySet();
        for (int i : keys) {
            counter++;
            try {
                control = hashes.get(i);
                datagroupControl.append(i, fromBytes(control));

                byte[] file = readFile(dataGroups.get(i));
                hash = crypt.digest(file);
                datagroupHashes.append(i, fromBytes(hash));

                if (!Arrays.equals(hash, control)) {
                    Log.e(TAG, "Wrong hash for DG" + i);
                    certified = false;
                    return;
                } else
                    Log.d(TAG, "Valid DG" + i + " hash");

            } catch (Exception e) {
                Log.e(TAG, "Error getting DG" + i + " hash");
                datagroupHashes.append(i, "-");
            }
            progressListener.process(50 + (50 / keys.size()) * counter);
        }

    }

    /**
     * Reads a file present on the document
     *
     * @param file The file to read
     * @return a byte array containing the file
     * @throws CardServiceException When accessing the document fails
     * @throws IOException          When reading the file fails
     * @throws SignatureException
     */
    private byte[] readFile(short file) throws CardServiceException, IOException, SignatureException {

        byte[] buffer;

        CardFileInputStream cis = passportService.getInputStream(file);
        ProcessInputStream pis = new ProcessInputStream(cis, cis.getLength());
        pis.addListener(this);

        // Store files for optimization
        switch (file) {
            case PassportService.EF_DG1:
                if (dg1 != null)
                    return dg1.getEncoded();
                else {
                    dg1 = new DG1File(pis);
                    Log.d(TAG, "Stored DG1");
                    return dg1.getEncoded();
                }
            case PassportService.EF_DG2:
                if (dg2 != null)
                    return dg2.getEncoded();
                else {
                    dg2 = new DG2File(pis);
                    Log.d(TAG, "Stored DG2");
                    return dg2.getEncoded();
                }
            case PassportService.EF_DG11:
                if (dg11 != null)
                    return dg11.getEncoded();
                else {
                    dg11 = new DG11File(pis);
                    Log.d(TAG, "Stored DG11");
                    return dg1.getEncoded();
                }
            case PassportService.EF_SOD:
                if (sod != null)
                    return sod.getEContent();
                else {
                    sod = new SODFile(pis);
                    return sod.getEContent();
                }
            default:
                buffer = getBytesFromInputStream(pis, MAX_BLOCK_READ_SIZE);
        }
        return buffer;
    }

    /* ------------------------ GETTERS --------------------- */

    public String getDocumentType() {
        return documentType;
    }

    public String getIssueDate() {
        return dateOfIssue;
    }

    public String getExpDate() {
        return dateOfExpiry;
    }

    public String getDocNumber() {
        return documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getGender() {
        return gender;
    }

    public String getNationality() {
        return nationality;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public String getDob() {
        return dateOfBirth;
    }

    public String getPhoto() {
        return photo;
    }

    public boolean getCertified() {
        return certified;
    }

    public String getIssuingState() {
        return issuingState;
    }

    public boolean isAuthenticationSuccess() {
        return authenticationSuccess;
    }

    public boolean isDatagroupHashesSuccess() {
        return datagroupHashesSuccess;
    }

    public boolean isDocumentSignerSuccess() {
        return documentSignerSuccess;
    }

    public boolean isCountrySignerSuccess() {
        return countrySignerSuccess;
    }

    public X509Certificate getDscCertificate() {
        return dscCertificate;
    }

    public X509Certificate getCscaCertificate() {
        return cscaCertificate;
    }

    public SparseArray<String> getDatagroupHashes() {
        return datagroupHashes;
    }

    public SparseArray<String> getDatagroupControl() {
        return datagroupControl;
    }
}
