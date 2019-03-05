package net.noinspiration.idreader.helper;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;

import net.noinspiration.idreader.identitydocument.DriverLicenseCategory;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing an Identity Document (Drivers Licence, Passport, ID card)
 */
public class IdentityDocument implements Parcelable {

    private String type;
    private String authority;
    private String dateOfIssue;
    private String dateOfExpiry;
    private String number;
    private String legitimate;
    private String issuingState;
    // Certificates
    private boolean authenticationSuccess;
    private boolean datagroupHashesSuccess;
    private boolean documentSignerSuccess;
    private boolean countrySignerSuccess;
    private X509Certificate dscCertificate;
    private X509Certificate cscaCertificate;
    private SparseArray<String> datagroupHashes;
    private SparseArray<String> datagroupControl;
    private List<DriverLicenseCategory> driverLicenseCategories;

    /**
     * Constructor
     *
     * @param type         The type of document
     * @param authority    The issuing authority
     * @param dateOfIssue  The date of issue
     * @param dateOfExpiry The date of expiry
     * @param number       The document number
     */
    public IdentityDocument(String type, String authority, String dateOfIssue, String dateOfExpiry,
                            String number, String legitimate, String issuingState) {
        this.type = type;
        this.authority = authority;
        this.dateOfIssue = dateOfIssue;
        this.dateOfExpiry = dateOfExpiry;
        this.number = number;
        this.legitimate = legitimate;
        this.issuingState = issuingState;
    }

    /*
    ---------------- GETTERS/SETTERS -----------------
     */
    public String getType() {
        return type;
    }

    public String getAuthority() {
        return authority;
    }

    public String getDateOfIssue() {
        return dateOfIssue;
    }

    public String getDateOfExpiry() {
        return dateOfExpiry;
    }

    public String getNumber() {
        return number;
    }

    public String getIssuingState() {
        return issuingState;
    }

    public String getLegitimate() {
        return legitimate;
    }

    public void setLegitimate(boolean legitimate) {
        if (legitimate)
            this.legitimate = "true";
        else
            this.legitimate = "false";
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

    public List<DriverLicenseCategory> getDriverLicenseCategories() {
        return driverLicenseCategories;
    }

    public void setDriverLicenseCategories(List<DriverLicenseCategory> driverLicenseCategories) {
        this.driverLicenseCategories = driverLicenseCategories;
    }

    public void setValidityBooleans(boolean authenticationSuccess, boolean datagroupHashesSuccess,
                                    boolean documentSignerSuccess, boolean countrySignerSuccess) {
        this.authenticationSuccess = authenticationSuccess;
        this.datagroupHashesSuccess = datagroupHashesSuccess;
        this.documentSignerSuccess = documentSignerSuccess;
        this.countrySignerSuccess = countrySignerSuccess;
    }

    public void setSecurityFeatures(X509Certificate dscCertificate, X509Certificate cscaCertificate,
                                    SparseArray<String> datagroupControl, SparseArray<String> datagroupHashes) {
        this.dscCertificate = dscCertificate;
        this.cscaCertificate = cscaCertificate;
        this.datagroupHashes = datagroupHashes;
        this.datagroupControl = datagroupControl;
    }

    protected IdentityDocument(Parcel in) {
        type = in.readString();
        authority = in.readString();
        dateOfIssue = in.readString();
        dateOfExpiry = in.readString();
        number = in.readString();
        legitimate = in.readString();
        issuingState = in.readString();
        authenticationSuccess = in.readByte() != 0x00;
        datagroupHashesSuccess = in.readByte() != 0x00;
        documentSignerSuccess = in.readByte() != 0x00;
        countrySignerSuccess = in.readByte() != 0x00;
        dscCertificate = (X509Certificate) in.readValue(X509Certificate.class.getClassLoader());
        cscaCertificate = (X509Certificate) in.readValue(X509Certificate.class.getClassLoader());
        datagroupHashes = (SparseArray) in.readValue(SparseArray.class.getClassLoader());
        datagroupControl = (SparseArray) in.readValue(SparseArray.class.getClassLoader());
        if (in.readByte() == 0x01) {
            driverLicenseCategories = new ArrayList<DriverLicenseCategory>();
            in.readList(driverLicenseCategories, DriverLicenseCategory.class.getClassLoader());
        } else {
            driverLicenseCategories = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(authority);
        dest.writeString(dateOfIssue);
        dest.writeString(dateOfExpiry);
        dest.writeString(number);
        dest.writeString(legitimate);
        dest.writeString(issuingState);
        dest.writeByte((byte) (authenticationSuccess ? 0x01 : 0x00));
        dest.writeByte((byte) (datagroupHashesSuccess ? 0x01 : 0x00));
        dest.writeByte((byte) (documentSignerSuccess ? 0x01 : 0x00));
        dest.writeByte((byte) (countrySignerSuccess ? 0x01 : 0x00));
        dest.writeValue(dscCertificate);
        dest.writeValue(cscaCertificate);
        dest.writeValue(datagroupHashes);
        dest.writeValue(datagroupControl);
        if (driverLicenseCategories == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(driverLicenseCategories);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<IdentityDocument> CREATOR = new Parcelable.Creator<IdentityDocument>() {
        @Override
        public IdentityDocument createFromParcel(Parcel in) {
            return new IdentityDocument(in);
        }

        @Override
        public IdentityDocument[] newArray(int size) {
            return new IdentityDocument[size];
        }
    };
}