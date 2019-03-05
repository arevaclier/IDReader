package net.noinspiration.idreader.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.noinspiration.idreader.R;
import net.noinspiration.idreader.activities.PersonActivity;
import net.noinspiration.idreader.helper.Person;

import java.text.ParseException;

import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;

import static net.noinspiration.idreader.identitydocument.HelperFunctions.getBytesFromInputStream;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.toLocaleDate;


public class IdentityFragment extends Fragment {

    private final static String TAG = "IdentityFragment";

    private ImageView photo;
    private TextView lastname;
    private TextView firstname;
    private TextView dateOfBirth;
    private TextView gender;
    private TextView nationality;
    private TextView placeOfBirth;
    private TextView bsn;
    private ImageView signature;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        PersonActivity callingActivity = (PersonActivity) getActivity();
        Person person = callingActivity.getPerson();

        photo = getView().findViewById(R.id.photo);
        lastname = getView().findViewById(R.id.lastnameText);
        firstname = getView().findViewById(R.id.firstnameText);
        dateOfBirth = getView().findViewById(R.id.dateOfBirthText);
        gender = getView().findViewById(R.id.genderText);
        nationality = getView().findViewById(R.id.nationalityText);
        placeOfBirth = getView().findViewById(R.id.placeOfBirthText);
        bsn = getView().findViewById(R.id.bsnText);
        signature = getView().findViewById(R.id.signaturePhoto);

        if (person.getPhoto() != null) {
            Bitmap bmp = BitmapFactory.decodeFile(person.getPhoto());
            photo.setImageBitmap(bmp);
        } else {
            Group group = getView().findViewById(R.id.photoGroup);
            group.setVisibility(View.GONE);
        }

        if (person.getLastName() != null) {
            lastname.setText(person.getLastName());
        } else {
            Group group = getView().findViewById(R.id.lastnameGroup);
            group.setVisibility(View.GONE);
        }

        if (person.getFirstName() != null) {
            firstname.setText(person.getFirstName());
        } else {
            Group group = getView().findViewById(R.id.firstnameGroup);
            group.setVisibility(View.GONE);
        }

        if (person.getDateOfBirth() != null) {
            try {
                dateOfBirth.setText(toLocaleDate(person.getDateOfBirth(), getActivity(), "yyyyMMdd"));
            } catch (ParseException e) {
                Log.e(TAG, "onCreateView - error while parsing date of birth: " + e.toString());
            }
        } else {
            Group group = getView().findViewById(R.id.dateOfBirthGroup);
            group.setVisibility(View.GONE);
        }

        if (person.getGender() != null) {
            gender.setText(person.getGender());
        } else {
            Group group = getView().findViewById(R.id.genderGroup);
            group.setVisibility(View.GONE);
        }

        if (person.getPlaceOfBirth() != null) {
            placeOfBirth.setText(person.getPlaceOfBirth());
        } else {
            Group group = getView().findViewById(R.id.placeOfBirthGroup);
            group.setVisibility(View.GONE);
        }
        if (person.getNationality() != null) {
            nationality.setText(person.getNationality());
        } else {
            Group group = getView().findViewById(R.id.nationalityGroup);
            group.setVisibility(View.GONE);
        }

        if (person.getBSN() != null) {
            bsn.setText(person.getBSN());
        } else {
            Group group = getView().findViewById(R.id.bsnGroup);
            group.setVisibility(View.GONE);
        }

        if (person.getSignature() != null) {
            Bitmap bmp = BitmapFactory.decodeFile(person.getSignature());
            signature.setImageBitmap(bmp);
        } else {
            Group group = getView().findViewById(R.id.signatureGroup);
            group.setVisibility(View.GONE);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_identity, container, false);
    }
}
