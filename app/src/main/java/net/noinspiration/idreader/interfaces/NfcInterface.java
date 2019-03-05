package net.noinspiration.idreader.interfaces;

import net.noinspiration.idreader.helper.Person;

/**
 * Interface used to communicate NFC events
 */
public interface NfcInterface {
    /**
     * Callback when NFC reading succeeds
     *
     * @param person The read document as a Person object
     */
    void onNfcResult(Person person);

    /**
     * Called when the NFC reading fails
     */
    void onNfcError();

    /**
     * Called when the app detects a potentially falsified document
     */
    void onFalsifiedDocument();

    /**
     * Called to update the UI every time a reading stage finished
     *
     * @param stage The finished stage
     */
    void updateUI(int stage);

    void updatePhotoProgress(int percentage);

    void updateInformationProgress(int percentage);

    void updateCertificateProgress(int percentage);
}
