package net.noinspiration.idreader.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.activities.PersonActivity;
import net.noinspiration.idreader.helper.AppProperties;
import net.noinspiration.idreader.helper.IdentityDocument;
import net.noinspiration.idreader.helper.Person;

import java.security.cert.X509Certificate;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import static net.noinspiration.idreader.identitydocument.HelperFunctions.fromBytes;


public class CertificateFragment extends Fragment {
    private int dgNumber = 5;

    // Validity section
    private TextView accessControl;
    private TextView chipAuthentication;
    private TextView dgHashes;
    private TextView documentSigner;
    private TextView countrySigner;

    // CSCA Section
    private TextView cscaSN;
    private TextView cscaAlgorithm;
    private TextView cscaThumbprint;
    private TextView cscaIssuer;
    private TextView cscaSubject;
    private TextView cscaValidFrom;
    private TextView cscaValidTo;

    // DSC Section
    private TextView dscSN;
    private TextView dscAlgorithm;
    private TextView dscThumbprint;
    private TextView dscIssuer;
    private TextView dscSubject;
    private TextView dscValidFrom;
    private TextView dscValidTo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_certificate, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PersonActivity activity = (PersonActivity) getActivity();
        Person person = activity.getPerson();
        IdentityDocument document = person.getIdentityDocument();

        /* --------------- FIND VIEW ------------------- */

        // Validity section
        accessControl = view.findViewById(R.id.accessControlText);
        chipAuthentication = view.findViewById(R.id.chipAuthenticationText);
        dgHashes = view.findViewById(R.id.dgHashesText);
        documentSigner = view.findViewById(R.id.documentSignerText);
        countrySigner = view.findViewById(R.id.countrySignerText);

        // CSCA section
        cscaSN = view.findViewById(R.id.cscaSNText);
        cscaAlgorithm = view.findViewById(R.id.cscaAlgorithmText);
        cscaThumbprint = view.findViewById(R.id.cscaThumbprintText);
        cscaIssuer = view.findViewById(R.id.cscaIssuerText);
        cscaSubject = view.findViewById(R.id.cscaSubjectText);
        cscaValidFrom = view.findViewById(R.id.cscaValidFromText);
        cscaValidTo = view.findViewById(R.id.cscaValidToText);

        // DSC section
        dscSN = view.findViewById(R.id.dscSNText);
        dscAlgorithm = view.findViewById(R.id.dscAlgorithmText);
        dscThumbprint = view.findViewById(R.id.dscThumbprintText);
        dscIssuer = view.findViewById(R.id.dscIssuerText);
        dscSubject = view.findViewById(R.id.dscSubjectText);
        dscValidFrom = view.findViewById(R.id.dscValidFromText);
        dscValidTo = view.findViewById(R.id.dscValidToText);

        /* ---------------- FILL VIEW -------------------*/

        // Validity section
        if (!document.getType().equals(AppProperties.DOCTYPE_DRIVERS_LICENCE)) {
            accessControl.setText(R.string.bac);
        } else {
            accessControl.setText(R.string.bap);
        }

        if (document.isAuthenticationSuccess()) {
            chipAuthentication.setText(R.string.success);
        } else {
            chipAuthentication.setText(R.string.fail);
        }

        if (document.isDatagroupHashesSuccess()) {
            dgHashes.setText(R.string.success);
        } else {
            dgHashes.setText(R.string.fail);
        }

        if (document.isDocumentSignerSuccess()) {
            documentSigner.setText(R.string.success);
        } else {
            documentSigner.setText(R.string.fail);
        }

        if (document.isCountrySignerSuccess()) {
            countrySigner.setText(R.string.success);
        } else {
            countrySigner.setText(R.string.fail);
        }

        // CSCA section
        X509Certificate cscaCertificate = document.getCscaCertificate();

        cscaSN.setText(cscaCertificate.getSerialNumber().toString());
        cscaAlgorithm.setText(cscaCertificate.getSigAlgName());
        cscaThumbprint.setText(fromBytes(cscaCertificate.getSignature()));
        cscaIssuer.setText(cscaCertificate.getIssuerDN().toString());
        cscaSubject.setText(cscaCertificate.getSubjectDN().toString());
        cscaValidFrom.setText(cscaCertificate.getNotBefore().toString());
        cscaValidTo.setText(cscaCertificate.getNotAfter().toString());

        // DSC section
        X509Certificate dscCertificate = document.getDscCertificate();

        dscSN.setText(dscCertificate.getSerialNumber().toString());
        dscAlgorithm.setText(dscCertificate.getSigAlgName());
        dscThumbprint.setText(fromBytes(dscCertificate.getSignature()));
        dscIssuer.setText(dscCertificate.getIssuerDN().toString());
        dscSubject.setText(dscCertificate.getSubjectDN().toString());
        dscValidFrom.setText(dscCertificate.getNotBefore().toString());
        dscValidTo.setText(dscCertificate.getNotAfter().toString());


        // Data groups
        LinearLayout linearLayout = view.findViewById(R.id.datagroupList);
        SparseArray<String> datagroupHashes = document.getDatagroupHashes();
        SparseArray<String> datagroupControl = document.getDatagroupControl();

        for (int i = 0; i < datagroupHashes.size(); i++) {
            int key = datagroupHashes.keyAt(i);
            View v = createDatagroupView(view, key);
            linearLayout.addView(v);

            TextView control = v.findViewById(R.id.catIssueDateText);
            TextView hash = v.findViewById(R.id.catExpDateText);
            TextView status = v.findViewById(R.id.catExtraText);

            String hexHash = datagroupHashes.get(key);
            String hexControl = datagroupControl.get(key);

            hash.setText(hexHash);
            control.setText(hexControl);

            if(!hexHash.equals("-")) {
                if(hexControl.equals(hexHash)) {
                    status.setText(getString(R.string.match));
                } else {
                    status.setTextColor(ContextCompat.getColor(view.getContext(), R.color.colorError));
                    status.setText(getString(R.string.not_valid));
                }
            } else {
                status.setText(getString(R.string.na));
            }

        }

    }

    private View createDatagroupView(View view, int id) {
        View v;
        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.include_dg, null);

        TextView dgTitle = v.findViewById(R.id.categoryTitle);
        dgTitle.setText(getString(R.string.dg, id));
        return v;
    }
}
