
package com.fyp.resilience.connection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;

import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.stream.PiecedRandomAccessFile;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public abstract class UploadConnectable extends Connectable {

    protected final DataPiece mDataPiece;
    protected final PiecedRandomAccessFile mFile;

    protected final long mPieceSize;
    private final long mStartByte;

    /**
     * @param dataPiece
     * @param file
     * @param service
     * @throws IOException
     */
    public UploadConnectable(final Context context, final DataWhole dataWhole, final DataPiece dataPiece, final File file)
            throws FileNotFoundException, IOException, NullPointerException {

        super(context, dataWhole);

        if (null == dataPiece) {
            throw new NullPointerException("DataPiece cannot be NULL");
        }

        if (null == file) {
            throw new NullPointerException("File cannot be NULL");
        }

        mDataPiece = dataPiece;
        long standardSize = dataWhole.getPieces().get(0).getSize();

        /* If the DataPiece has a URI then it is a foreign piece */
        if (mDataWhole.isOwned()) {

            if (mDataPiece.getPieceNo() > 1) {
                mStartByte = ((mDataPiece.getPieceNo() - 1) * standardSize);
            } else {
                mStartByte = 0;
            }

        } else {

            mStartByte = 0;

        }

        mPieceSize = mDataPiece.getSize();
        mFile = new PiecedRandomAccessFile(mStartByte, mPieceSize, file);
        mConnectionStatus = STATUS_WAITING;
    }

    @Override
    public void run() {

        /* Check if the hash has failed. Return if it has */
        if (!performHashIfRequired()) {
            mConnectionStatus = STATUS_HASH_ERROR;
            notifyOfCompletion();
            return;
        }

        mConnectionStatus = STATUS_IN_PROGRESS;
        notifyOfStateChange();
        
        mConnectionStatus = runTask();
        runPostTask();

        notifyOfCompletion();
    }

    private boolean performHashIfRequired() {

        String pieceHash = mDataPiece.getHash();

        if (null != pieceHash) {
            return true;
        }

        mConnectionStatus = STATUS_HASHING;
        notifyOfStateChange();

        final Hasher md5Hasher = Hashing.md5().newHasher();
        try {

            int length;
            final byte[] buffer = new byte[4096];
            while ((length = mFile.read(buffer)) != -1) {
                md5Hasher.putBytes(buffer, 0, length);
            }

            pieceHash = md5Hasher.hash().toString();
            mDataPiece.setHash(pieceHash);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != mFile) {
                    mFile.resetToStart();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null == pieceHash ? false : true;
    }

    /**
     * Abstract method enforced by the Runnable interface
     */
    @Override
    protected abstract int runTask();

    @Override
    protected abstract void runPostTask();

    protected abstract void notifyOfCompletion();

    public DataPiece getDataPiece() {
        return mDataPiece;
    }
}
