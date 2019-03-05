package net.noinspiration.idreader.helper;

import android.os.Parcel;
import android.os.Parcelable;

import org.jmrtd.BACKey;

/**
 * The only point of this class is to implement Parcelable for a BAC Key
 */
public class BACKeyHelper implements Parcelable {

    BACKey bacKey;

    /**
     * Constructor
     *
     * @param bacKey a BAC key
     */
    public BACKeyHelper(BACKey bacKey) {
        this.bacKey = bacKey;
    }

    public BACKey getBacKey() {
        return bacKey;
    }

    /*
    ------------ ANDROID PARCEL -----------------
     */
    protected BACKeyHelper(Parcel in) {
        bacKey = (BACKey) in.readValue(BACKey.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(bacKey);
    }

    @SuppressWarnings("unused")
    public static final Creator<BACKeyHelper> CREATOR = new Creator<BACKeyHelper>() {
        @Override
        public BACKeyHelper createFromParcel(Parcel in) {
            return new BACKeyHelper(in);
        }

        @Override
        public BACKeyHelper[] newArray(int size) {
            return new BACKeyHelper[size];
        }
    };
}