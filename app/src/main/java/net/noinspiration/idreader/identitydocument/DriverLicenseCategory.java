package net.noinspiration.idreader.identitydocument;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.ParseException;

import static net.noinspiration.idreader.identitydocument.HelperFunctions.fromHexString;
import static net.noinspiration.idreader.identitydocument.HelperFunctions.toLocaleDate;

/**
 * Class representing a category on the European Driving Licence model (ISO 7816)
 * See https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=uriserv:OJ.L_.2012.120.01.0001.01.ENG section I.6.2
 */
public class DriverLicenseCategory implements Parcelable {
    private final static String TAG = "DriverLicenseCategory";
    private String category;
    private String issueDate;
    private String expDate;
    private String code;
    private String sign;
    private String value;

    public DriverLicenseCategory(String hex, Context context) throws ParseException {

        // Parse category
        String[] split = new String[]{
                "", "", "", "", "", ""
        };

        // Sections of the categories are split on ";" (3B in hex)
        String[] cat = hex.split("3B");
        System.arraycopy(cat, 0, split, 0, cat.length);

        category = fromHexString(split[0]);
        issueDate = toLocaleDate(split[1], context, "ddMMyyyy");
        expDate = toLocaleDate(split[2], context, "ddMMyyyy");
        code = fromHexString(split[3]);
        sign = fromHexString(split[4]);
        value = fromHexString(split[5]);

        Log.i(TAG, "Category: " + category);
    }

    /* ---------- GETTERS --------------- */

    public String getCategory() {
        return category;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public String getExpDate() {
        return expDate;
    }

    public String getExtra() {
        String toReturn = "";
        if (!code.equals("")) {
            if (!value.equals("") || !sign.equals(""))
                toReturn += code + ".";
            else
                toReturn += code;
        }
        if (!sign.equals("")) {
            if (!value.equals(""))
                toReturn += sign + ".";
            else
                toReturn += sign;
        }
        if (!value.equals("")) {
            toReturn += value;
        }

        return toReturn;
    }


    protected DriverLicenseCategory(Parcel in) {
        category = in.readString();
        issueDate = in.readString();
        expDate = in.readString();
        code = in.readString();
        sign = in.readString();
        value = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(category);
        dest.writeString(issueDate);
        dest.writeString(expDate);
        dest.writeString(code);
        dest.writeString(sign);
        dest.writeString(value);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<DriverLicenseCategory> CREATOR = new Parcelable.Creator<DriverLicenseCategory>() {
        @Override
        public DriverLicenseCategory createFromParcel(Parcel in) {
            return new DriverLicenseCategory(in);
        }

        @Override
        public DriverLicenseCategory[] newArray(int size) {
            return new DriverLicenseCategory[size];
        }
    };
}