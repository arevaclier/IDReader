package net.noinspiration.idreader.identitydocument;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseArray;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.helper.ProcessInputStream;
import net.noinspiration.idreader.interfaces.InputStreamListener;
import net.sf.scuba.smartcards.APDUWrapper;
import net.sf.scuba.smartcards.CardFileInputStream;
import net.sf.scuba.smartcards.CardServiceException;

import org.jmrtd.DefaultFileSystem;
import org.jmrtd.PassportService;
import org.jmrtd.lds.SODFile;
import org.jmrtd.lds.icao.DG11File;
import org.jmrtd.protocol.ReadBinaryAPDUSender;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import androidx.annotation.NonNull;

import static net.noinspiration.idreader.identitydocument.HelperFunctions.fromBytes;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.fromHexString;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.getBytesFromInputStream;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.hexStringToByteArray;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.toSQLDate;


/**
 * Class allowing android to read a European Driving Licence.
 * Based on ICAO document 9303, ISO 7816 and EU directive 383/2012
 * Tested only on Dutch Driving Licences
 * <p>
 * https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=uriserv:OJ.L_.2012.120.01.0001.01.ENG
 * <p>
 * The images stored in the driving licence are in FACE format (DG5 and DG6).
 * However this format is not in use anymore.
 * It is possible to get a JPEG image from it by stripping all the
 * bytes off until the marker for JPEG appears. This marker is indicated by
 * the bytes 0xFF 0xD8.
 * Here, the byte array is converted to a dg11 string. Then the beginning of the string
 * is removed (until bytes 0xFF 0xD8 are found). Finally, the dg11 string is
 * converted back into a byte array and BitmapFactory can interpret it as a JPEG.
 */
public class DrivingLicenceHelper implements InputStreamListener {

    private final static String TAG = "DrivingLicenseHelper";

    // Encoding modes
    private final static int ENC_MODE = 1;
    private final static int MAC_MODE = 2;

    // Applet ID, see section I.3.2.1 of the EU directive
    private final static byte[] AID = new byte[]{(byte) 0xA0, 0x00, 0x00, 0x04, 0x56, 0x45, 0x44, 0x4C, 0x2D, 0x30, 0x31};

    // Maximum size for BINARY READ
    private final static int MAX_BLOCK_READ_SIZE = 200;
    // Data groups present on the NFC chip
    private final static SparseArray<Byte> dataGroups;

    static {
        dataGroups = new SparseArray<>();
        dataGroups.append(1, PassportService.SFI_DG1);
        dataGroups.append(2, PassportService.SFI_DG2);
        dataGroups.append(3, PassportService.SFI_DG3);
        dataGroups.append(4, PassportService.SFI_DG4);
        dataGroups.append(5, PassportService.SFI_DG5);
        dataGroups.append(6, PassportService.SFI_DG6);
        dataGroups.append(7, PassportService.SFI_DG7);
        dataGroups.append(8, PassportService.SFI_DG8);
        dataGroups.append(9, PassportService.SFI_DG9);
        dataGroups.append(10, PassportService.SFI_DG10);
        dataGroups.append(11, PassportService.SFI_DG11);
        dataGroups.append(12, PassportService.SFI_DG12);
        dataGroups.append(13, PassportService.SFI_DG13);
        dataGroups.append(14, PassportService.SFI_DG14);
        dataGroups.append(15, PassportService.SFI_DG15);
        dataGroups.append(16, PassportService.SFI_DG16);
    }

    private final InputStreamListener progressListener;

    private DefaultFileSystem defaultFileSystem;
    private Context context;
    private String country;
    private String lastName;
    private String otherName;
    private String dob;
    private String birthPlace;
    private String nationality;
    private String gender;
    private String issueDate;
    private String expDate;
    private String issuingAuthority;
    private String administrativeNumber;
    private String docNumber;
    private int categoryNumber;
    private String bsn;
    private List<DriverLicenseCategory> categories;

    // Document signing
    private boolean authenticationSuccess = false;
    private boolean datagroupHashesSuccess = false;
    private boolean documentSignerSuccess = false;
    private boolean countrySignerSuccess = false;

    private X509Certificate dscCertificate;
    private X509Certificate cscaCertificate;

    private SparseArray<String> datagroupHashes = new SparseArray<>();
    private SparseArray<String> datagroupControl = new SparseArray<>();

    // File storage
    private byte[] dg1File = null;
    private byte[] dg5File = null;
    private byte[] dg6File = null;
    private byte[] dg11File = null;

    // Holds false if the chip has been falsified
    private boolean certified = true;

    private boolean isCheckingCertificate = false;
    private boolean readingSOD;

    public DrivingLicenceHelper(String mrz, PassportService passportService, Context context, InputStreamListener progressListener) throws GeneralSecurityException, CardServiceException, NullPointerException {

        this.context = context;
        this.progressListener = progressListener;

        // Generate necessary keys for authentication from MRZ
        Log.i(TAG, "Generating keys");
        byte[] kSeed = generateKSeed(mrz.getBytes());
        SecretKey kEnc = deriveKey(kSeed, ENC_MODE);
        SecretKey kMac = deriveKey(kSeed, MAC_MODE);

        passportService.open();

        // Select applet and get the driving licence filesystem
        Log.i(TAG, "Sending AID");
        ReadBinaryAPDUSender bSender = new ReadBinaryAPDUSender(passportService);
        bSender.sendSelectApplet(null, AID);
        defaultFileSystem = new DefaultFileSystem(bSender, false);

        // Authenticate with the keys and set the authentication layer
        Log.i(TAG, "Authenticating");
        passportService.doBAC(kEnc, kMac);
        APDUWrapper wrapper = passportService.getWrapper();
        defaultFileSystem.setWrapper(wrapper);

        authenticationSuccess = true;
    }

    /**
     * Computes a key from a seed, see ICAO 9303 for details
     *
     * @param keySeed the seed
     * @param mode    the encryption mode (ENC, MAC)
     * @return A secret key
     * @throws GeneralSecurityException If encryption doesn;t work
     */
    private static SecretKey deriveKey(byte[] keySeed, int mode)
            throws GeneralSecurityException {
        MessageDigest shaDigest = MessageDigest.getInstance("SHA1");
        shaDigest.update(keySeed);
        byte[] c = {0x00, 0x00, 0x00, (byte) mode};
        shaDigest.update(c);
        byte[] hash = shaDigest.digest();
        byte[] key = new byte[24];
        System.arraycopy(hash, 0, key, 0, 8);
        System.arraycopy(hash, 8, key, 8, 8);
        System.arraycopy(hash, 0, key, 16, 8);
        SecretKeyFactory desKeyFactory = SecretKeyFactory.getInstance("DESede");
        return desKeyFactory.generateSecret(new DESedeKeySpec(key));
    }

    /**
     * InputStream listener, takes care of setting correct percentages before sending to NFCActivity
     */
    @Override
    public void process(int percentage) {
        if (!isCheckingCertificate) {
            progressListener.process(percentage);
        } else {
            if (readingSOD) {
                // Map the reading of the SOD to 30% of the progress bar
                progressListener.process((int) (percentage * 0.3));
            }
        }
    }

    /**
     * Checks the driver licence for legitimacy by:
     * 1. Validating the country root certificate (CSCA)
     * 2. Validating the Document Signer Certificate (DSC)
     * 3. Validating the Data Group hashes
     */
    public void checkLegitimacy() throws CardServiceException, CertificateException, NoSuchAlgorithmException, IOException {
        isCheckingCertificate = true;
        readingSOD = true;
        SODFile sodFile = null;
        // Retrieve SOD
        // Select and read the file
        defaultFileSystem.selectFile(PassportService.SFI_SOD);
        CardFileInputStream is = new CardFileInputStream(MAX_BLOCK_READ_SIZE, defaultFileSystem);
        ProcessInputStream pis = new ProcessInputStream(is, is.getLength());
        pis.addListener(this);

        try {
            Log.d(TAG, "Reading SOD");
            sodFile = new SODFile(pis);
        } catch (IOException e) {
            Log.e(TAG, "Error reading SOD");
            certified = false;
            return;
        }

        X509Certificate certificate = sodFile.getDocSigningCertificate();
        if (certificate == null) {
            certified = false;
            throw new CertificateException("Certificate not present");
        }
        dscCertificate = certificate;

        readingSOD = false;

        // Get certificate present on driver licence and check the dates
        try {
            certificate.checkValidity();
        } catch (CertificateNotYetValidException | CertificateExpiredException e) {
            // Invalid dates
            Log.e(TAG, "Certificate check failed: " + e.toString());
            certified = false;
            return;
        }

        progressListener.process(32);

        checkCSCA(certificate);
        if (!certified) {
            isCheckingCertificate = false;
            return;
        }
        countrySignerSuccess = true;

        checkDSC(sodFile, certificate);
        if (!certified) {
            isCheckingCertificate = false;
            return;
        }
        documentSignerSuccess = true;

        checkDatagroupHashes(sodFile);
        if (certified)
            datagroupHashesSuccess = true;

        isCheckingCertificate = false;
    }

    /**
     * Verifies the signature of the certificate against root certificates emitted by countries
     *
     * @param certificate The certificate present on the chip
     * @throws CertificateException When reading countries certificates fails.
     */
    private void checkCSCA(@NonNull X509Certificate certificate) throws CertificateException {
        // Get certificate issuer country
        String country = certificate.getIssuerDN().toString();
        int index = country.indexOf("C=") + 2;
        country = country.substring(index, index + 2).toUpperCase();

        // We only have dutch certificates
        if (country.equals("NL")) {
            // Load certificates
            Set<X509Certificate> certificates = new HashSet<>();
            InputStream is;
            X509Certificate cert;

            is = context.getResources().openRawResource(R.raw.cscaedl_v1_prod);
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
            certificates.add(cert);

            progressListener.process(34);

            is = context.getResources().openRawResource(R.raw.cscaedl_v1_specimen);
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
            certificates.add(cert);

            progressListener.process(36);

            is = context.getResources().openRawResource(R.raw.cscaedl_v2_prod);
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
            certificates.add(cert);
            progressListener.process(38);


            is = context.getResources().openRawResource(R.raw.cscaedl_v2_specimen);
            cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
            certificates.add(cert);

            progressListener.process(40);

            // Check against certificates emitted by the country
            for (X509Certificate c : certificates) {
                try {
                    certificate.verify(c.getPublicKey());
                    certified = true;
                    cscaCertificate = c;
                    Log.d(TAG, "Root CA valid (Certificate " + country + ": " + c.getSerialNumber() + ")");
                    break;
                } catch (Exception e) {
                    Log.d(TAG, "Verification failed (Certificate " + country + ": " + c.getSerialNumber() + ")");
                    certified = false;
                }
            }

        }

        progressListener.process(45);
    }

    /**
     * Verifies the signature of the Document Signer Certificate
     *
     * @param sodFile     the SOD File of the chip
     * @param certificate the certificate on the chip
     */
    private void checkDSC(@NonNull SODFile sodFile, @NonNull X509Certificate certificate) {
        // Validate certificate signature
        try {
            Signature signature = Signature.getInstance(certificate.getSigAlgName());
            signature.initVerify(certificate.getPublicKey());
            signature.update(sodFile.getEContent());
            signature.verify(certificate.getSignature());
            Log.d(TAG, "Validated certificate using " + Security.getProviders()[0].getName());
        } catch (Exception e) {
            Log.e(TAG, "Validation failed with " + Security.getProviders()[0].getName() + ": " + e.toString());
            certified = false;
        }

        progressListener.process(50);
    }

    /**
     * Calculates the checksum of every datagroup and compares it to the value stored in SOD
     *
     * @param sodFile the SOD file
     */
    private void checkDatagroupHashes(@NonNull SODFile sodFile) throws NoSuchAlgorithmException, NullPointerException {
        Map<Integer, byte[]> hashes = sodFile.getDataGroupHashes();
        MessageDigest crypt = MessageDigest.getInstance(sodFile.getDigestAlgorithm());
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
                Log.e(TAG, "Error getting DG" + i + ": " + e.toString());
                datagroupHashes.append(i, "-");
            }
            progressListener.process(50 + (50 / keys.size()) * counter);
        }
    }

    /**
     * Reads the DG1. This contains text info (name, dob, categories, etc)
     *
     * @throws CardServiceException When communication doesn't work
     * @throws IOException          When there is an error reading
     * @throws ParseException       When a date is not parsed correctly
     */
    public void readDG1() throws CardServiceException, IOException, ParseException {
        Log.i(TAG, "Reading DG1");
        String dg1 = fromBytes(readFile(PassportService.SFI_DG1));
        parseDG1(dg1);
        String dg11 = fromBytes(readFile(PassportService.SFI_DG11));
        parseDG11(dg11);
        Log.i(TAG, "DG1 done");
    }

    /**
     * Reads the DG5. This contains the driving license holder's signature
     *
     * @return A bitmap containing the signature
     * @throws CardServiceException When communication doesn't work
     * @throws IOException          When there is an error reading
     */
    public Bitmap readDG5() throws CardServiceException, IOException {
        Log.i(TAG, "Reading DG5");
        String image = fromBytes(readFile(PassportService.SFI_DG5));
        image = image.substring(image.indexOf("FFD8"));
        byte[] photo = hexStringToByteArray(image);
        Log.i(TAG, "DG5 done");
        return BitmapFactory.decodeByteArray(photo, 0, photo.length);
    }

    /**
     * Reads the DG6. This contains the driving license holder's photo
     *
     * @return A bitmap containing the photo
     * @throws CardServiceException When communication doesn't work
     * @throws IOException          When there is an error reading
     */
    public Bitmap readDG6() throws CardServiceException, IOException {
        Log.i(TAG, "Reading DG6");
        String image = fromBytes(readFile(PassportService.SFI_DG6));
        image = image.substring(image.indexOf("FFD8"));
        byte[] photo = hexStringToByteArray(image);
        Log.i(TAG, "DG6 done");
        return BitmapFactory.decodeByteArray(photo, 0, photo.length);
    }

    /**
     * Reads a file from the driving licence
     *
     * @param file The file to read
     * @return The file as an array of bytes
     * @throws CardServiceException When communication doesn't work
     * @throws IOException          When there is an error reading
     */
    private byte[] readFile(short file) throws CardServiceException, IOException {
        // Select and read the file
        defaultFileSystem.selectFile(file);
        CardFileInputStream is = new CardFileInputStream(MAX_BLOCK_READ_SIZE, defaultFileSystem);

        // Transform the input stream into an array of bytes
        ProcessInputStream pis = new ProcessInputStream(is, is.getLength());
        pis.addListener(this);

        switch (file) {
            case PassportService.SFI_DG1:
                if (dg1File != null)
                    return dg1File;
                else {
                    dg1File = getBytesFromInputStream(pis, MAX_BLOCK_READ_SIZE);
                    Log.d(TAG, "readFile: Stored DG1");
                    return dg1File;
                }
            case PassportService.SFI_DG5:
                if (dg5File != null)
                    return dg5File;
                else {
                    dg5File = getBytesFromInputStream(pis, MAX_BLOCK_READ_SIZE);
                    Log.d(TAG, "readFile: Stored DG5");
                    return dg5File;
                }
            case PassportService.SFI_DG6:
                if (dg6File != null)
                    return dg6File;
                else {
                    dg6File = getBytesFromInputStream(pis, MAX_BLOCK_READ_SIZE);
                    Log.d(TAG, "readFile: Stored DG6");
                    return dg6File;
                }
            case PassportService.SFI_DG11:
                if (dg11File != null)
                    return dg11File;
                else {
                    dg11File = getBytesFromInputStream(pis, MAX_BLOCK_READ_SIZE);
                    Log.d(TAG, "readFile: Stored DG11");
                    return dg11File;
                }
            default:
                return getBytesFromInputStream(pis, MAX_BLOCK_READ_SIZE);
        }
    }

    /**
     * Generates the kSeed from the MRZ, see ICAO 9303 for details
     *
     * @param mrz the driving licence MRZ
     * @return The kSeed as an array of bytes
     * @throws NoSuchAlgorithmException If SHA-1 algorithm doesn't exist on the system
     */
    private byte[] generateKSeed(byte[] mrz) throws NoSuchAlgorithmException {

        // Encrypt MRZ with SHA-1
        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(mrz);
        byte[] result = crypt.digest();

        // Keep only the 16 most significant bytes
        byte[] kSeed = new byte[16];
        System.arraycopy(result, 0, kSeed, 0, 16);

        return kSeed;
    }

    /**
     * Parses a DG1 file into usable info
     *
     * @param dg1 the DG1 file as a dg11 string
     * @throws ParseException When parsing dates fails
     */
    private void parseDG1(String dg1) throws ParseException {
        // Indices (M is mandatory, aka present in the chip, O is optional)
        int countryIndex = dg1.indexOf("5F03") + 6;     // M
        int lNameIndex = dg1.indexOf("5F04") + 6;       // M
        int oNameIndex = dg1.indexOf("5F05") + 6;       // M
        int dobIndex = dg1.indexOf("5F06") + 6;         // M
        int birthPlaceIndex = dg1.indexOf("5F07") + 6;  // M
        int nationalityIndex = dg1.indexOf("5F08");     // O
        int genderIndex = dg1.indexOf("5F09");          // O
        int doiIndex = dg1.indexOf("5F0A") + 6;         // M
        int doeIndex = dg1.indexOf("5F0B") + 6;         // M
        int authorityIndex = dg1.indexOf("5F0C") + 6;   // M
        int adminNumIndex = dg1.indexOf("5F0D");        // O
        int docNumIndex = dg1.indexOf("5F0E") + 6;      // M
        int addressIndex = dg1.indexOf("5F0F");         // O
        int categoriesData = dg1.indexOf("7F63") + 6;   // M


        // Country
        Log.i(TAG, "Country");
        country = fromHexString(dg1.substring(countryIndex, countryIndex + 6));

        // Last name(s)
        Log.i(TAG, "Last name(s)");
        lastName = fromHexString(dg1.substring(lNameIndex, oNameIndex - 6));

        // Other name(s)
        Log.i(TAG, "Other name(s)");
        otherName = fromHexString(dg1.substring(oNameIndex, dobIndex - 6));

        // Date of birth
        Log.i(TAG, "Date of birth");
        dob = toSQLDate(dg1.substring(dobIndex, dobIndex + 8), true);

        // Birth place
        Log.i(TAG, "Birth place");
        // Nationality present
        if (nationalityIndex != -1) {
            birthPlace = fromHexString(dg1.substring(birthPlaceIndex, nationalityIndex));
        } else {
            // No nationality, but gender present
            if (genderIndex != -1) {
                birthPlace = fromHexString(dg1.substring(birthPlaceIndex, genderIndex));
            } else {
                // No nationality, no gender
                birthPlace = fromHexString(dg1.substring(birthPlaceIndex, doiIndex - 6));
            }
        }

        // Nationality
        if (nationalityIndex != -1) {
            Log.i(TAG, "Nationality");
            nationality = fromHexString(dg1.substring(nationalityIndex + 6, nationalityIndex + 12));
        }

        // Gender
        if (genderIndex != -1) {
            Log.i(TAG, "Gender");
            gender = fromHexString(dg1.substring(genderIndex + 6, genderIndex + 8));
        }

        // Date of issue
        Log.i(TAG, "Date of issue");
        issueDate = toSQLDate(dg1.substring(doiIndex, doiIndex + 8), true);

        // Date of expiry
        Log.i(TAG, "Date of expiry");
        expDate = toSQLDate(dg1.substring(doeIndex, doeIndex + 8), true);

        // Issuing authority
        // If there is a administrative number
        Log.i(TAG, "Issuing authority");
        if (adminNumIndex != -1) {
            issuingAuthority = fromHexString(dg1.substring(authorityIndex, adminNumIndex));
        } else {
            // No administrative number
            issuingAuthority = fromHexString(dg1.substring(authorityIndex, docNumIndex - 6));
        }

        // Administrative number
        if (adminNumIndex != -1) {
            Log.i(TAG, "Administrative number");
            administrativeNumber = fromHexString(dg1.substring(adminNumIndex + 6, docNumIndex - 6));
        }

        // Document number
        // If there is an address
        Log.i(TAG, "Document number");
        if (addressIndex != -1) {
            docNumber = fromHexString(dg1.substring(docNumIndex, addressIndex));
        } else {
            docNumber = fromHexString(dg1.substring(docNumIndex, categoriesData - 6));
        }

        // Categories
        String categoriesHex = dg1.substring(categoriesData);

        // Number of categories on DL
        Log.i(TAG, "Number of categories");
        int categoriesIndex = categoriesHex.indexOf("02") + 4;
        categoryNumber = Integer.parseInt(categoriesHex.substring(categoriesIndex, categoriesIndex + 2));
        String categoriesToParse = categoriesHex.substring(categoriesIndex + 2);

        List<Integer> indices = new ArrayList<>();

        // Get categories indices
        int index = categoriesToParse.indexOf("87");
        while (index >= 0) {
            indices.add(index);
            index = categoriesToParse.indexOf("87", index + 1);
        }

        // Remove indices where the next byte is a semicolon (3B), this indicates
        // that it is not a new category but a date ending by 87 (thanks stupid EU standards)
        for (int i = 0; i < indices.size(); i++) {
            int j = indices.get(i);
            if (categoriesToParse.substring(j, j + 4).equals("873B"))
                indices.remove(i);
        }

        // Create categories
        categories = new ArrayList<>();
        for (int i = 0; i < categoryNumber; i++) {
            String rawCategory;
            if (i == categoryNumber - 1) {
                rawCategory = categoriesToParse.substring(indices.get(i) + 4);
            } else {
                rawCategory = categoriesToParse.substring(indices.get(i) + 4, indices.get(i + 1));
            }
            categories.add(new DriverLicenseCategory(rawCategory, context));
        }
    }

    private void parseDG11(String dg11) {
        String startingByte = "80";
        String data;
        int index;
        while ((index = dg11.indexOf(startingByte)) > -1) {

            dg11 = dg11.substring(index + 2);

            int length = Integer.parseInt(dg11.substring(0, 2), 16) * 2;

            dg11 = dg11.substring(2);
            if (length > 0) {
                data = dg11.substring(0, length);
                switch(startingByte) {
                    case "80":
                        bsn = fromHexString(data);
                        break;
                    case "85":
                        lastName = fromHexString(data);
                        break;
                    case "86":
                        otherName = fromHexString(data);
                        break;
                }
            }
            dg11 = dg11.substring(length);
            int start = Integer.parseInt(startingByte) + 1;
            startingByte = String.valueOf(start);

        }
    }


    /* --------- GETTERS ------------- */

    public String getCountry() {
        return country;
    }

    public String getLastName() {
        return lastName;
    }

    public String getOtherName() {
        return otherName;
    }

    public String getDob() {
        return dob;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public String getIssueDate() {

        return issueDate;
    }

    public String getExpDate() {

        return expDate;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public String getNationality() {
        return nationality;
    }

    public String getGender() {
        return gender;
    }

    public String getAdministrativeNumber() {
        return administrativeNumber;
    }

    public String getBSN() {
        return bsn;
    }

    public List<DriverLicenseCategory> getCategories() {
        return categories;
    }

    public boolean getCertified() {
        return certified;
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