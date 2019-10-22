package com.bluberry.adclient;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends FilterInputStream {
    private int totalBytes = 0;

    public CountingInputStream(InputStream in) {
        super(in);
    }

    public int getTotalBytesRead() {

        return totalBytes;
    }

    @Override
    public int read() throws IOException {
        int byteValue = super.read();
        if (byteValue != -1)
            totalBytes++;
        return byteValue;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int bytesRead = super.read(b);
        if (bytesRead != -1)
            totalBytes += bytesRead;
        return bytesRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        if (bytesRead != -1)
            totalBytes += bytesRead;
        return bytesRead;
    }

}