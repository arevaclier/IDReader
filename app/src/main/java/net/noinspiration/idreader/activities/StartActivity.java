package net.noinspiration.idreader.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.helper.AppProperties;
import net.noinspiration.idreader.helper.CertificateReader;
import net.noinspiration.idreader.interfaces.CertificateInterface;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class StartActivity extends AppCompatActivity implements CertificateInterface {

    private static final String TAG = "StartActivity";

    private Button idButton;
    private Button passportButton;
    private Button dlButton;

    private ProgressBar certificatePBar;
    private TextView certificateText;

    private Button permissionButton;
    private TextView permissionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        AppProperties.activityName = this.getClass().getSimpleName();

        // Create security provider (for ID scanning)
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        // Retrieve UI elements
        idButton = findViewById(R.id.id_button);
        passportButton = findViewById(R.id.passport_button);
        dlButton = findViewById(R.id.dl_button);
        certificatePBar = findViewById(R.id.certificate_progressbar);
        certificateText = findViewById(R.id.certificate_progess_text);
        permissionButton = findViewById(R.id.permission_button);
        permissionText = findViewById(R.id.permission_text);

        // Load certificates from masterlist
        try {
            FileInputStream fis = new FileInputStream(getFilesDir() + "/" + AppProperties.CERTIFICATE_FILE_NAME);
            ObjectInputStream oos = new ObjectInputStream(fis);
            AppProperties.certificates = (Map<String, Set<X509Certificate>>) oos.readObject();
            oos.close();
            fis.close();
            setReady();
        } catch (FileNotFoundException e) {
            CertificateReader certificateReader = new CertificateReader(this, this);
            Thread t = new Thread(certificateReader);
            t.start();
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "onCreate: " + e.toString());
        }

        checkPermission();

    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        }
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            permissionText.setVisibility(View.VISIBLE);
            permissionButton.setVisibility(View.VISIBLE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    AppProperties.PERMISSION_REQUEST_CAMERA);
        }
    }

    private void setReady() {
        certificateText.setVisibility(View.GONE);
        certificatePBar.setVisibility(View.GONE);

        idButton.setClickable(true);
        passportButton.setClickable(true);
        dlButton.setClickable(true);
    }

    @Override
    public void notifyRead() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setReady();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case AppProperties.PERMISSION_REQUEST_CAMERA:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionButton.setVisibility(View.GONE);
                    permissionText.setVisibility(View.GONE);
                } else {
                    requestPermission();
                }
        }
    }

    public void onGrantButtonClicked(View v) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                AppProperties.PERMISSION_REQUEST_CAMERA);
    }

    public void onDLButtonClicked(View v) {
        Intent intent = new Intent(this, DrivingLicenceReaderActivity.class);
        startActivity(intent);
        finish();
    }

    public void onIDButtonClicked(View v) {
        Intent intent = new Intent(this, PassportReaderActivity.class);
        startActivity(intent);
        finish();
    }
}
