package net.noinspiration.idreader.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.nfc.tech.IsoDep;
import android.util.Log;

import net.noinspiration.idreader.identitydocument.DrivingLicenceHelper;
import net.noinspiration.idreader.identitydocument.PassportHelper;
import net.noinspiration.idreader.interfaces.InputStreamListener;
import net.noinspiration.idreader.interfaces.NfcInterface;
import net.sf.scuba.smartcards.CardService;

import org.jmrtd.BACKey;
import org.jmrtd.PassportService;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.iso19794.FaceImageInfo;
import org.jmrtd.lds.iso19794.FaceInfo;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static net.noinspiration.idreader.helper.AppProperties.NFC_STAGE_1;
import static net.noinspiration.idreader.helper.AppProperties.NFC_STAGE_2;
import static net.noinspiration.idreader.helper.AppProperties.NFC_STAGE_3;
import static net.noinspiration.idreader.helper.AppProperties.NFC_STAGE_4;

/**
 * Class allowing the reading of an NFC chip
 * The two constructors are there for either ID card/Passport or Driving Licence
 */
public class NfcReader implements Runnable, InputStreamListener {

    // Debug tag
    private final static String TAG = "NfcReader";

    // Needed variables
    private IsoDep isoDep;
    private BACKey bacKey;
    private NfcInterface callingActivity;
    private Context appContext;
    private String mrz;

    // Person info
    private Person person;
    private IdentityDocument identityDocument;

    // Document type
    private int docType;

    // Current reading stage
    private int stage = 0;

    /**
     * Constructor for Passport/ID card
     *
     * @param isoDep          The NFC tag
     * @param bacKey          the BAC key to authenticate
     * @param callingActivity the calling activity
     * @param appContext      the application context
     * @param docType         the document type
     */
    public NfcReader(IsoDep isoDep, BACKey bacKey, NfcInterface callingActivity, Context appContext, int docType) {
        this.isoDep = isoDep;
        this.bacKey = bacKey;
        this.callingActivity = callingActivity;
        this.appContext = appContext;
        this.docType = docType;
    }

    /**
     * Constructor for Driving Licences
     *
     * @param isoDep          The NFC tag
     * @param mrz             The one-line MRZ on the Driving Licence
     * @param callingActivity The calling activity
     * @param appContext      The application context
     * @param docType         The document type
     */
    public NfcReader(IsoDep isoDep, String mrz, NfcInterface callingActivity, Context appContext, int docType) {
        this.isoDep = isoDep;
        this.mrz = mrz;
        this.callingActivity = callingActivity;
        this.appContext = appContext;
        this.docType = docType;
    }

    /**
     * Callbak for InputStream reading progress
     *
     * @param percent The current read progress in percent
     */
    @Override
    public void process(int percent) {
        switch (stage) {
            case NFC_STAGE_2:
                callingActivity.updateInformationProgress(percent);
                break;
            case NFC_STAGE_3:
                callingActivity.updatePhotoProgress(percent);
                break;
            case NFC_STAGE_4:
                callingActivity.updateCertificateProgress(percent);
                break;
        }
    }

    /**
     * The function where the reading is done
     * In a separate thread to avoid UI blocking
     */
    @Override
    public void run() {
        try {
            // Connect to the NFC tag and obtain a PassportService to interact with it
            isoDep.connect();
            CardService cs = CardService.getInstance(isoDep);
            PassportService ps = new PassportService(cs, 256, 200, true, true);

            // PASSPORT/ID CARD
            if (docType == AppProperties.ACTIVITY_PASSPORT_SCAN) {

                /* ---------------- AUTHENTICATION ------------------- */

                stage = NFC_STAGE_1;
                callingActivity.updateUI(NFC_STAGE_1);

                // Authentication
                PassportHelper passHelper = new PassportHelper(ps, bacKey, appContext, this);

                /* ----------------- PERSON DATA ----------------------- */

                stage = NFC_STAGE_2;
                callingActivity.updateUI(NFC_STAGE_2);

                passHelper.readDG1();
                // Create person and identity document objects
                identityDocument = new IdentityDocument(passHelper.getDocumentType(),
                        null, passHelper.getIssueDate(), passHelper.getExpDate(),
                        passHelper.getDocNumber(), null, passHelper.getIssuingState());

                person = new Person(passHelper.getFirstName(), passHelper.getLastName(),
                        passHelper.getDob(), passHelper.getPlaceOfBirth(),
                        passHelper.getPhoto(), passHelper.getGender(),
                        passHelper.getNationality(), null, identityDocument);



                /* ----------------- PHOTO ----------------------------- */

                stage = NFC_STAGE_3;
                callingActivity.updateUI(NFC_STAGE_3);

                passHelper.readDG2();
                person.setPhoto(passHelper.getPhoto());

                /* ----------------- CERTIFICATE VALIDATION ------------ */

                stage = NFC_STAGE_4;
                callingActivity.updateUI(NFC_STAGE_4);

                passHelper.checkLegitimacy();
                identityDocument.setLegitimate(passHelper.getCertified());
                identityDocument.setValidityBooleans(passHelper.isAuthenticationSuccess(),
                        passHelper.isDatagroupHashesSuccess(), passHelper.isDocumentSignerSuccess(),
                        passHelper.isCountrySignerSuccess());

                identityDocument.setSecurityFeatures(passHelper.getDscCertificate(),
                        passHelper.getCscaCertificate(), passHelper.getDatagroupControl(),
                        passHelper.getDatagroupHashes());

            }

            // DRIVING LICENCE
            else if (docType == AppProperties.ACTIVITY_DL_SCAN) {

                /* ----------------- AUTHENTICATION ------------------------------ */
                stage = NFC_STAGE_1;
                callingActivity.updateUI(NFC_STAGE_1);

                // Driving licence helper, does authentication
                DrivingLicenceHelper dlHelper = new DrivingLicenceHelper(mrz, ps, appContext, this);


                /* ----------------- PERSON DATA --------------------------------- */
                stage = NFC_STAGE_2;
                callingActivity.updateUI(NFC_STAGE_2);

                // Read person info (name, etc)
                dlHelper.readDG1();

                // Create person and identity document objects
                identityDocument = new IdentityDocument(AppProperties.DOCTYPE_DRIVERS_LICENCE,
                        dlHelper.getIssuingAuthority(), dlHelper.getIssueDate(),
                        dlHelper.getExpDate(), dlHelper.getDocNumber(), null, dlHelper.getCountry());
                identityDocument.setDriverLicenseCategories(dlHelper.getCategories());

                person = new Person(dlHelper.getOtherName(), dlHelper.getLastName(),
                        dlHelper.getDob(), dlHelper.getBirthPlace(), null, dlHelper.getGender(),
                        dlHelper.getNationality(), dlHelper.getBSN(), identityDocument);


                /* ------------------- PHOTO ------------------------------------- */

                stage = NFC_STAGE_3;
                callingActivity.updateUI(NFC_STAGE_3);

                // Read photo
                Bitmap picture = dlHelper.readDG6();
                if (picture != null) {
                    // Store image to file
                    String output = appContext.getCacheDir().toString() + "/passport.jpg";
                    FileOutputStream out = new FileOutputStream(output);
                    picture.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    person.setPhoto(output);
                }

                /* ----------------- CERTIFICATE VALIDATION AND SIGNATURE ----------- */

                stage = NFC_STAGE_4;
                callingActivity.updateUI(NFC_STAGE_4);
                dlHelper.checkLegitimacy();
                identityDocument.setLegitimate(dlHelper.getCertified());
                identityDocument.setValidityBooleans(dlHelper.isAuthenticationSuccess(),
                        dlHelper.isDatagroupHashesSuccess(), dlHelper.isDocumentSignerSuccess(),
                        dlHelper.isCountrySignerSuccess());
                identityDocument.setSecurityFeatures(dlHelper.getDscCertificate(),
                        dlHelper.getCscaCertificate(), dlHelper.getDatagroupControl(),
                        dlHelper.getDatagroupHashes());

                // Read photo
                Bitmap signature = dlHelper.readDG5();
                if (signature != null) {
                    // Store image to file
                    String output = appContext.getCacheDir().toString() + "/signature.jpg";
                    FileOutputStream out = new FileOutputStream(output);
                    signature.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    person.setSignature(output);
                }
            }

            /* ------------------- CALLBACK ---------------------------------------- */
            // Successfully read passport/ID
            callingActivity.onNfcResult(person);

        } catch (Exception e) {
            Log.e(TAG, e.toString());
            callingActivity.onNfcError();
        }
    }
}
