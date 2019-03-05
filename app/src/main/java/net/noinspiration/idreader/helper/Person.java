package net.noinspiration.idreader.helper;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a Person with an associated ID
 */
public class Person implements Parcelable {
    @SuppressWarnings("unused")
    public static final Creator<Person> CREATOR = new Creator<Person>() {
        @Override
        public Person createFromParcel(Parcel in) {
            return new Person(in);
        }

        @Override
        public Person[] newArray(int size) {
            return new Person[size];
        }
    };
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private String placeOfBirth;
    private IdentityDocument identityDocument;
    private String bsn;
    private String photo;
    private String gender;
    private String nationality;
    private String signature;

    /*
    -------------- GETTERS/SETTERS ------------------
     */

    /**
     * Constructor for a Person
     *
     * @param firstName        The first name
     * @param lastName         The last name
     * @param dateOfBirth      The date of birth
     * @param identityDocument The person's identity document
     */
    public Person(String firstName, String lastName, String dateOfBirth,
                  String placeOfBirth, String photo, String gender, String nationality,
                  String bsn, IdentityDocument identityDocument) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.placeOfBirth = placeOfBirth;
        this.photo = photo;
        this.gender = gender;
        this.nationality = nationality;
        this.bsn = bsn;
        this.identityDocument = identityDocument;
    }

    protected Person(Parcel in) {
        firstName = in.readString();
        lastName = in.readString();
        dateOfBirth = in.readString();
        placeOfBirth = in.readString();
        bsn = in.readString();
        photo = in.readString();
        gender = in.readString();
        nationality = in.readString();
        signature = in.readString();
        identityDocument = (IdentityDocument) in.readValue(IdentityDocument.class.getClassLoader());
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public String getBSN() {
        return bsn;
    }

    public IdentityDocument getIdentityDocument() {
        return identityDocument;
    }

    public String getGender() {
        return gender;
    }

    public String getPhoto() {
        return photo;
    }

    public String getNationality() {
        return nationality;
    }

    public String getSignature() {
        return signature;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    /*
    -------------- ANDROID PARCEL -----------------------
     */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(dateOfBirth);
        dest.writeString(placeOfBirth);
        dest.writeString(bsn);
        dest.writeString(photo);
        dest.writeString(gender);
        dest.writeString(nationality);
        dest.writeString(signature);
        dest.writeValue(identityDocument);
    }
}