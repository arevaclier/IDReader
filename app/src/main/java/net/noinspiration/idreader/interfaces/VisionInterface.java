package net.noinspiration.idreader.interfaces;

/**
 * Interface used to communicate Camera Vision results
 */
public interface VisionInterface {

    /**
     * Called when the vision succeeds
     *
     * @param result The read QR code/text
     */
    void onSuccessfulVision(String result);

    /**
     * Called when the vision fails
     *
     * @param exception The exception thrown
     */
    void onFailureVision(String exception);
}
