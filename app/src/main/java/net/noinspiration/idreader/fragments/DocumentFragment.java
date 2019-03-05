package net.noinspiration.idreader.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.activities.PersonActivity;
import net.noinspiration.idreader.helper.AppProperties;
import net.noinspiration.idreader.helper.IdentityDocument;
import net.noinspiration.idreader.helper.Person;

import java.text.ParseException;

import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;

import static net.noinspiration.idreader.identitydocument.HelperFunctions.toLocaleDate;


public class DocumentFragment extends Fragment {

    private static final String TAG = "DocumentFragment";

    private TextView type;
    private TextView number;
    private TextView expiryDate;
    private TextView issueDate;
    private TextView issuingState;
    private TextView authority;
    private TextView legitimate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        PersonActivity callingActivity = (PersonActivity) getActivity();
        Person person = callingActivity.getPerson();
        IdentityDocument document = person.getIdentityDocument();

        type = getView().findViewById(R.id.documentTypeText);
        number = getView().findViewById(R.id.documentNumberText);
        expiryDate = getView().findViewById(R.id.expiryDateText);
        issueDate = getView().findViewById(R.id.issueDateText);
        issuingState = getView().findViewById(R.id.issuingStateText);
        authority = getView().findViewById(R.id.authorityText);
        legitimate = getView().findViewById(R.id.legitimateText);

        if (document.getType() != null) {
            switch (document.getType()) {
                case AppProperties.DOCTYPE_DRIVERS_LICENCE:
                    type.setText(getString(R.string.driving_licence));
                    break;
                case AppProperties.DOCTYPE_IDCARD:
                    type.setText(getString(R.string.id_card));
                    break;
                case AppProperties.DOCTYPE_PASSPORT:
                    type.setText(R.string.passport);
                    break;
            }
        } else {
            Group group = getView().findViewById(R.id.documentTypeGroup);
            group.setVisibility(View.GONE);
        }

        if (document.getNumber() != null) {
            number.setText(document.getNumber());
        } else {
            Group group = getView().findViewById(R.id.documentNumberGroup);
            group.setVisibility(View.GONE);
        }

        if (document.getDateOfExpiry() != null) {
            try {
                expiryDate.setText(toLocaleDate(document.getDateOfExpiry(), getContext(), "yyyyMMdd"));
            } catch (ParseException e) {
                Log.e(TAG, "onViewCreated - expiry date fail: " + e.toString());
            }
        } else {
            Group group = getView().findViewById(R.id.expiryDateGroup);
            group.setVisibility(View.GONE);
        }

        if (document.getDateOfIssue() != null) {
            try {
                issueDate.setText(toLocaleDate(document.getDateOfIssue(), getContext(), "yyyyMMdd"));
            } catch (ParseException e) {
                Log.e(TAG, "onViewCreated - issue date fail: " + e.toString());
            }
        } else {
            Group group = getView().findViewById(R.id.issueDateGroup);
            group.setVisibility(View.GONE);
        }

        if (document.getIssuingState() != null) {
            issuingState.setText(document.getIssuingState());
        } else {
            Group group = getView().findViewById(R.id.issuingStateGroup);
            group.setVisibility(View.GONE);
        }

        if (document.getAuthority() != null) {
            authority.setText(document.getAuthority());
        } else {
            Group group = getView().findViewById(R.id.authorityGroup);
            group.setVisibility(View.GONE);
        }

        if (document.getLegitimate() != null) {
            boolean certified = Boolean.parseBoolean(document.getLegitimate());
            if (certified)
                legitimate.setText(getString(R.string.yes));
            else
                legitimate.setText(getString(R.string.no));
        } else {
            Group group = getView().findViewById(R.id.legitimateGroup);
            group.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_document, container, false);
    }
}
