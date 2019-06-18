package net.noinspiration.idreader.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.helper.AppProperties;
import net.noinspiration.idreader.helper.BACKeyHelper;
import net.noinspiration.idreader.helper.NfcReader;
import net.noinspiration.idreader.helper.Person;
import net.noinspiration.idreader.interfaces.NfcInterface;

import org.jmrtd.BACKey;

import java.util.Arrays;

import static net.noinspiration.idreader.helper.AppProperties.NFC_STAGE_1;
import static net.noinspiration.idreader.helper.AppProperties.NFC_STAGE_2;
import static net.noinspiration.idreader.helper.AppProperties.NFC_STAGE_3;
import static net.noinspiration.idreader.helper.AppProperties.NFC_STAGE_4;

public class NFCActivity extends AppCompatActivity implements NfcInterface {

    private static final String TAG = "NFCActivity";
    private int callingActivity;

    private String mrz;
    private BACKey bacKey;

    private NfcAdapter nfcAdapter;

    // The error message field in case the scanning goes wrong
    private TextView errorMessage;

    // Various activity layouts, see layout file
    private TextView documentText;
    private TextView infoText;
    private TextView photoText;
    private TextView certificateText;
    private ProgressBar infoBar;
    private ProgressBar photoBar;
    private ProgressBar certificateBar;

    private String docType;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);
        callingActivity = getIntent().getIntExtra("activity", -1);

        switch (callingActivity) {
            case AppProperties.ACTIVITY_DL_SCAN:
                mrz = getIntent().getStringExtra("mrz");
                break;
            case AppProperties.ACTIVITY_PASSPORT_SCAN:
                BACKeyHelper bacKeyHelper = getIntent().getParcelableExtra("backey");
                bacKey = bacKeyHelper.getBacKey();
                docType = getIntent().getStringExtra("doctype");
                break;
            default:
                onBackPressed();
        }
        // Get various UI parts
        errorMessage = findViewById(R.id.errorMessage);

        documentText = findViewById(R.id.documentText);
        infoText = findViewById(R.id.infoText);
        photoText = findViewById(R.id.photoText);
        certificateText = findViewById(R.id.certificateText);
        infoBar = findViewById(R.id.infoBar);
        photoBar = findViewById(R.id.photoBar);
        certificateBar = findViewById(R.id.certificateBar);

        resetUI();

        AppProperties.activityName = this.getClass().getSimpleName();
    }

    @Override
    public void onBackPressed() {
        Intent intent;
        switch (callingActivity) {
            case AppProperties.ACTIVITY_DL_SCAN:
                intent = new Intent(this, DrivingLicenceReaderActivity.class);
                break;
            case AppProperties.ACTIVITY_PASSPORT_SCAN:
                intent = new Intent(this, PassportReaderActivity.class);
                intent.putExtra("doctype", docType);
                break;
            default:
                intent = new Intent(this, StartActivity.class);
        }
        startActivity(intent);
        finish();
    }

    @Override
    public void onNfcResult(Person person) {
        Intent intent = new Intent(this, PersonActivity.class);
        intent.putExtra("activity", callingActivity);
        intent.putExtra("person", person);
        switch(callingActivity) {
            case AppProperties.ACTIVITY_DL_SCAN:
                intent.putExtra("mrz", mrz);
                break;
            case AppProperties.ACTIVITY_PASSPORT_SCAN:
                intent.putExtra("backey", new BACKeyHelper(bacKey));
                intent.putExtra("doctype", docType);
                break;
        }
        startActivity(intent);
        finish();
    }

    @Override
    public void onNfcError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resetUI();
                errorMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onFalsifiedDocument() {

    }

    /* ------------------------ UI FUNCTIONS ------------------------- */

    /**
     * Callback from NFC reader to update the UI on the UI thread
     *
     * @param stage Which stage the NFC reader is at
     */
    @Override
    public void updateUI(final int stage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (stage) {
                    case NFC_STAGE_1:
                        documentText.setText(R.string.document_found);
                        break;
                    case NFC_STAGE_2:
                        infoText.setVisibility(View.VISIBLE);
                        infoBar.setVisibility(View.VISIBLE);
                        break;
                    case NFC_STAGE_3:
                        photoText.setVisibility(View.VISIBLE);
                        photoBar.setVisibility(View.VISIBLE);
                        break;
                    case NFC_STAGE_4:
                        certificateText.setVisibility(View.VISIBLE);
                        certificateBar.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });
    }

    @Override
    public void updatePhotoProgress(final int percentage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                photoBar.setProgress(percentage);
            }
        });
    }

    @Override
    public void updateInformationProgress(final int percentage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoBar.setProgress(percentage);
            }
        });
    }

    @Override
    public void updateCertificateProgress(final int percentage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                certificateBar.setProgress(percentage);
            }
        });
    }

    /**
     * Resets the UI to defaults
     */
    private void resetUI() {
        // Reset text
        documentText.setText(R.string.searching_document);

        // Reset progress bars
        infoBar.setProgress(0);
        photoBar.setProgress(0);
        certificateBar.setProgress(0);

        // Set visibility
        errorMessage.setVisibility(View.INVISIBLE);
        infoText.setVisibility(View.INVISIBLE);
        photoText.setVisibility(View.INVISIBLE);
        certificateText.setVisibility(View.INVISIBLE);
        infoBar.setVisibility(View.INVISIBLE);
        photoBar.setVisibility(View.INVISIBLE);
        certificateBar.setVisibility(View.INVISIBLE);

        documentText.setVisibility(View.VISIBLE);
    }


    /*------------------------- NFC Chip functions ---------------------------------- */

    /**
     * Called when no NFC chip is present in the phone or the NFC chip is not capable of decoding official documents
     */
    private void onNoAvailableNFC() {
        Toast toast = Toast.makeText(this, R.string.no_nfc, Toast.LENGTH_LONG);
        toast.show();
    }

    private void activateNFC() {
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setTitle(R.string.nfc_popup_title);
        alertbox.setMessage(getString(R.string.nfc_popup_message));
        alertbox.setPositiveButton(R.string.nfc_popup_accept_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                startActivity(intent);
            }
        });
        AlertDialog dialog = alertbox.create();
        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * On activity resume, look for NFC tag
     */
    @Override
    protected void onResume() {
        super.onResume();

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {

            if (!nfcAdapter.isEnabled())
                activateNFC();

            Intent intent = new Intent(getApplicationContext(), this.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            String[][] filter = new String[][]{new String[]{AppProperties.NFC_TECH}};
            nfcAdapter.enableForegroundDispatch(this, pi, null, filter);

        } else {
            onNoAvailableNFC();
        }
    }

    /**
     * On activity pause, stop looking for NFC tag
     */
    @Override
    protected void onPause() {
        super.onPause();

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        } else {
            onNoAvailableNFC();
        }
    }

    /**
     * When a new NFC tag is found
     *
     * @param intent the new intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        // If the intent is a NFC related intent
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {

            resetUI();

            // Get NFC chip possibilities
            Tag tag = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
            Log.i(TAG, "Scanning tag");

            // Check if the NFC chip is capable of reading passport chips
            if (Arrays.asList(tag.getTechList()).contains(AppProperties.NFC_TECH)) {

                // Start reading in a separate thread
                IsoDep isoDep = IsoDep.get(tag);
                isoDep.setTimeout(5000);
                NfcReader reader = null;

                if (callingActivity == AppProperties.ACTIVITY_PASSPORT_SCAN) {
                    reader = new NfcReader(isoDep, bacKey, this, this, callingActivity);
                } else if (callingActivity == AppProperties.ACTIVITY_DL_SCAN) {
                    String cutMRZ = mrz.substring(1, mrz.length() - 1);
                    reader = new NfcReader(isoDep, cutMRZ, this, this, callingActivity);
                }

                Thread nfcThread = new Thread(reader);
                nfcThread.start();
            } else {
                onNoAvailableNFC();
            }
        }
    }
}
