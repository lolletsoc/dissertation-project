
package com.fyp.resilience.database.model;

import java.io.File;

import android.content.Context;
import android.net.Uri;

import com.fyp.resilience.event.PieceStateChange;
import com.fyp.resilience.interfaces.Messagable;
import com.fyp.resilience.interfaces.Partialable;
import com.fyp.resilience.proto.ProtoBuffSpecification.DataPieceMessage;
import com.fyp.resilience.util.Utils;
import com.fyp.resilience.widerst.model.DataPiecePartial;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import de.greenrobot.event.EventBus;

@DatabaseTable(tableName = "data_piece")
public class DataPiece implements Partialable<DataPiecePartial>, Messagable<DataPieceMessage> {

    /**
     * A public static method used to build DataPiece objects
     * 
     * @return {@link DataPiece}
     */
    public static DataPiece getDataPiece(final int pieceNo, final DataWhole dataWhole, final long size) {
        return new DataPiece(pieceNo, dataWhole, size);
    }

    /* State constants */
    // Uploaded to Widerst
    public static final int STATE_UPLOADED_TO_SERVER = 0;

    // Uploaded to a device
    public static final int STATE_UPLOADED_TO_DEVICE = 1;

    // Currently no activity
    public static final int STATE_NONE = 2;

    // Something is currently using this piece
    public static final int STATE_IN_PROGRESS = 3;

    /* Column name constants */
    public static final String COL_STATE = "state";
    public static final String COL_PIECE_NO = "piece_number";
    public static final String COL_PIECE_KEY = "piece_key";
    public static final String COL_WHOLE_ID = "whole_id";
    public static final String COL_MD5_HASH = "md5_hash";
    public static final String COL_PIECE_URI = "piece_uri";
    public static final String COL_PIECE_SIZE = "piece_size";
    public static final String COL_RETRYABLE = "retryable";

    /* Database fields */
    @DatabaseField(columnName = COL_PIECE_KEY, id = true)
    private String mKey;

    @DatabaseField(columnName = COL_PIECE_NO)
    private int mPieceNo;

    @DatabaseField(columnName = COL_STATE)
    private int mState;

    @DatabaseField(columnName = COL_MD5_HASH)
    private String mHash;

    @DatabaseField(columnName = COL_PIECE_URI)
    private String mUri;

    @DatabaseField(columnName = COL_PIECE_SIZE)
    private long mSize;
    
    @DatabaseField(columnName = COL_RETRYABLE)
    private boolean mRetry;

    @DatabaseField(columnName = COL_WHOLE_ID, canBeNull = false, foreign = true)
    private DataWhole mDataWhole;

    /* Instance field */
    private File mFile;

    DataPiece() {
        /* Ormlite requires a constructor with no arguments */
    }

    private DataPiece(final int pieceNo, final DataWhole dataWhole, final long size) {
        mPieceNo = pieceNo;
        mState = STATE_NONE;
        mDataWhole = dataWhole;
        mKey = mDataWhole.getKey() + pieceNo;
        mHash = null;
        mSize = size;
        mRetry = false;
    }

    public String getKey() {
        return mKey;
    }

    public DataWhole getParent() {
        return mDataWhole;
    }

    public int getPieceNo() {
        return mPieceNo;
    }

    public String getHash() {
        return mHash;
    }

    public void setHash(final String hash) {
        mHash = hash;
    }

    public File getFile(final Context context) {
        if (null == mFile && null != mUri) {
            mFile = new File(Utils.getFilePathFromUri(context, Uri.parse(mUri)));
        }

        return mFile;
    }

    public String getUri() {
        return mUri;
    }

    public long getSize() {
        return mSize;
    }
    
    public void setRetry(final boolean retry) {
        mRetry = retry;
    }
    
    public boolean requiresRetry() {
        return mRetry;
    }
    
    public void setParent(final DataWhole dataWhole) {
        mDataWhole = dataWhole;
    }

    public void setUri(String uri) {
        mUri = uri;
    }

    /**
     * @return
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * @param state
     */
    public synchronized void setState(final int state) {
        mState = state;
        postEvent(new PieceStateChange(this));
    }

    /**
     * @param event
     */
    private void postEvent(final Object event) {
        EventBus.getDefault().post(event);
    }

    @Override
    public DataPiecePartial toPartial() {
        
        return new DataPiecePartial()
                .setKey(mKey)
                .setPieceNo(mPieceNo)
                .setWholeParent(mDataWhole.toPartial())
                .setHash(mHash)
                .setRetry(mRetry);
    }

    @Override
    public DataPieceMessage toMessage() {
        return DataPieceMessage.newBuilder()
                .setMd5Hash(mHash)
                .setPieceNo(mPieceNo)
                .setPieceSize(mSize)
                .build();
    }

    @Override
    public String toString() {
        return mKey;
    }
    
    @Override
    public boolean equals(Object o) {
        return mKey != null && mKey.contentEquals(((DataPiece) o).getKey());
    }
    
    @Override
    public int hashCode() {
        return mKey.hashCode();
    }
}
