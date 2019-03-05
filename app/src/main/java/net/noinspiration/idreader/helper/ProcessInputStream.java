package net.noinspiration.idreader.helper;

import net.noinspiration.idreader.interfaces.InputStreamListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ProcessInputStream extends InputStream {
    private static final String TAG = "PIS";
    private InputStream in;
    private int length, sumRead;
    private java.util.List<InputStreamListener> listeners;
    private double percent;

    public ProcessInputStream(InputStream inputStream, int length) throws IOException {
        this.in = inputStream;
        listeners = new ArrayList<>();
        sumRead = 0;
        this.length = length;
    }


    @Override
    public int read(byte[] b) throws IOException {
        int readCount = in.read(b);
        evaluatePercent(readCount);
        return readCount;
    }


    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int readCount = in.read(b, off, len);
        evaluatePercent(readCount);
        return readCount;
    }

    @Override
    public long skip(long n) throws IOException {
        long skip = in.skip(n);
        evaluatePercent(skip);
        return skip;
    }

    @Override
    public int read() throws IOException {
        int read = in.read();
        if (read != -1) {
            evaluatePercent(1);
        }
        return read;
    }

    public ProcessInputStream addListener(InputStreamListener listener) {
        this.listeners.add(listener);
        return this;
    }

    private void evaluatePercent(long readCount) {
        if (readCount != -1) {
            sumRead += readCount;
            percent = sumRead * 1.0 / length;
            percent *= 100;
        }
        notifyListener();
    }

    private void notifyListener() {
        for (InputStreamListener listener : listeners) {
            listener.process((int) percent);
        }
    }
}
