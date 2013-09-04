
package com.fyp.resilience.stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

import com.fyp.resilience.Flags;

public class PiecedRandomAccessFile extends RandomAccessFile {

    private static final String TAG = PiecedRandomAccessFile.class.getSimpleName();

    private final long mStartByte;
    private final long mPieceLength;
    private long mBytesRead;

    public PiecedRandomAccessFile(final long startByte, final long pieceLength,
            final File file) throws FileNotFoundException, IOException {

        super(file, "r");
        mStartByte = startByte;
        mPieceLength = pieceLength;
        mBytesRead = 0;

        /*
         * Throw exceptions start byte and piece length are larger than file's
         * length
         */
        if ((mStartByte + mPieceLength) > file.length()) {
            throw new IOException("Start byte + piece length is greater than file's length");
        }

        if (Flags.DEBUG) {
            Log.i(TAG, "Starting: " + mStartByte + " Ending: " + (mStartByte + mPieceLength));
        }

        super.seek(mStartByte);
    }

    public void resetToStart() throws IOException {
        mBytesRead = 0;
        super.seek(mStartByte);
    }

    @Override
    public int read() throws IOException {
        int byteReturn = super.read();
        mBytesRead++;
        return byteReturn;
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int count) throws IOException {
        if ((mPieceLength - mBytesRead) >= buffer.length) {
            return super.read(buffer, offset, count);
        } else {
            if (mBytesRead != mPieceLength) {
                return super.read(buffer, offset, (int) (mPieceLength - mBytesRead));
            } else {
                return -1;
            }
        }
    }

    @Override
    public int read(final byte[] buffer) throws IOException {
        return updateBytesRead(read(buffer, 0, buffer.length));
    }

    private int updateBytesRead(final int bytesRead) {
        if (bytesRead > 0) {
            mBytesRead += bytesRead;
        }
        return bytesRead;
    }
}
