
package com.fyp.resilience.database.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.fyp.resilience.Flags;
import com.fyp.resilience.event.WholeModified;
import com.fyp.resilience.interfaces.Messagable;
import com.fyp.resilience.interfaces.Partialable;
import com.fyp.resilience.proto.ProtoBuffSpecification.DataWholeMessage;
import com.fyp.resilience.util.Utils;
import com.fyp.resilience.widerst.model.DataWholePartial;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import de.greenrobot.event.EventBus;

@DatabaseTable(tableName = "data_whole")
public class DataWhole implements Partialable<DataWholePartial>, Messagable<DataWholeMessage> {

    private static final String TAG = DataWhole.class.getSimpleName();

    /**
     * @param {@link Context}
     * @param {@link Uri}
     * @return {@link DataWhole}
     */
    public static DataWhole getOwnedDataWhole(final Context context, final Uri uri, long pieceSize) {

        final String fileUri = Utils.getFilePathFromUri(context, uri);

        if (null == fileUri) {
            return null;
        }

        /* Create a temporary File to obtain its length in bytes */
        final File dataFile = new File(fileUri);

        if (Flags.DEBUG) {
            Log.d(TAG, "PieceSize is " + pieceSize);
        }

        /* Calculate the pieces based on the Fixed PIECE_SIZE */
        final int pieces = (int) Math.ceil((double) dataFile.length() / (double) pieceSize);

        /* Calculate the size of the last piece */
        final long lastPieceSize = dataFile.length() - ((pieces - 1) * pieceSize);

        if (Flags.DEBUG) {
            Log.d(TAG, "Last PieceSize is " + lastPieceSize);
        }

        /* Create the server ID */
        final String serverId = Utils.getDeviceInfo(context).getServerRegistrationId();
        final Calendar currTime = Calendar.getInstance();

        final DataWhole dataWhole = new DataWhole(
                serverId + currTime.getTimeInMillis(),
                uri.toString(),
                pieces,
                dataFile.getName(),
                context.getContentResolver().getType(uri),
                new Date(),
                true);

        final List<DataPiece> pieceList = new ArrayList<DataPiece>();
        for (int i = 1; i <= pieces; i++) {

            if (i == pieces) {
                pieceSize = lastPieceSize;
            }

            pieceList.add(DataPiece.getDataPiece(i, dataWhole, pieceSize));
        }

        dataWhole.mPieces = pieceList;

        return dataWhole;
    }

    /**
     * @param context
     * @param wholeMessage
     * @return
     */
    public static DataWhole getUnownedDataWhole(final Context context, final DataWholeMessage wholeMessage) {

        return new DataWhole(
                wholeMessage.getKey(),
                null,
                wholeMessage.getNoPieces(),
                wholeMessage.getFileName(),
                null,
                new Date(),
                false);
    }

    /* State constants */
    public static final int STATE_COMPLETED = 0;
    public static final int STATE_IN_PROGRESS = 1;
    public static final int STATE_DOWNLOADING = 2;
    public static final int STATE_NONE = 3;

    /* Column name constants */
    static final String COL_KEY = "key";
    static final String COL_URI = "uri";
    static final String COL_NO_PIECES = "piece_number";
    static final String COL_FILE_NAME = "file_name";
    static final String COL_ADDED_ON = "added_on";
    static final String COL_OVERALL_STATE = "state";
    static final String COL_MIME_TYPE = "mime_type";
    static final String COL_AVAILABLE = "available";
    static final String COL_OWNED = "owned";

    @DatabaseField(columnName = COL_KEY, id = true)
    private String mKey;

    @DatabaseField(columnName = COL_URI)
    private String mUri;

    @DatabaseField(columnName = COL_NO_PIECES)
    private int mNoPieces;

    @DatabaseField(columnName = COL_FILE_NAME)
    private String mFileName;

    @DatabaseField(columnName = COL_ADDED_ON)
    private Date mTimeAdded;

    @DatabaseField(columnName = COL_OVERALL_STATE)
    private int mState;

    @DatabaseField(columnName = COL_MIME_TYPE)
    private String mMimeType;

    @DatabaseField(columnName = COL_OWNED)
    private boolean mOwned;

    @DatabaseField(columnName = COL_AVAILABLE)
    private boolean mAvailable;

    private List<DataPiece> mPieces;

    /* Instance-specific fields */
    private File mFile;

    DataWhole() {
        /* Ormlite requires a constructor with no arguments */
    }

    private DataWhole(final String key, final String uri, final int noPieces, final String fileName,
            final String mimeType,
            final Date date, final boolean owned) {
        mKey = key;
        mUri = uri;
        mNoPieces = noPieces;
        mFileName = fileName;
        mMimeType = mimeType;
        mTimeAdded = date;
        mState = STATE_NONE;
        mAvailable = false;
        mOwned = owned;
        mPieces = new ArrayList<DataPiece>();
    }

    public String getKey() {
        return mKey;
    }

    public boolean isAvailable() {
        return mAvailable;
    }

    public boolean isOwned() {
        return mOwned;
    }

    public String getUriString() {
        return mUri;
    }

    public void setUriString(final String uri) {
        mUri = uri;
    }

    public int getNoPieces() {
        return mNoPieces;
    }

    public List<DataPiece> getPieces() {
        if (null != mPieces) {
            synchronized (mPieces) {
                return new ArrayList<DataPiece>(mPieces);
            }
        }
        return null;
    }

    public synchronized void addPiece(final DataPiece dataPiece) {
        if (null == mPieces) {
            mPieces = new ArrayList<DataPiece>();
        }
        mPieces.add(dataPiece);
    }

    public void setPieces(final List<DataPiece> pieces) {
        if (pieces != null) {
            for (DataPiece dataPiece : pieces) {
                dataPiece.setParent(this);
                if (dataPiece.getState() == DataPiece.STATE_IN_PROGRESS) {
                    dataPiece.setState(DataPiece.STATE_NONE);
                }
            }
        }
        mPieces = pieces;
    }

    public Date getTimeAdded() {
        return mTimeAdded;
    }

    public String getFileName() {
        return mFileName;
    }

    public File getFile(final Context context) {
        if (null == mFile && null != mUri) {
            String filePath = Utils.getFilePathFromUri(context, Uri.parse(mUri));
            if (null != filePath) {
                mFile = new File(filePath);
            }
        }

        if (null != mFile) {
            return mFile.exists() ? mFile : null;
        }

        return null;
    }

    public int getState() {
        return mState;
    }

    public void setState(final int state) {
        mState = state;
        postEvent(new WholeModified());
    }

    public void setAvailability(final boolean available) {
        mAvailable = available;
    }

    private void postEvent(final Object event) {
        EventBus.getDefault().post(event);
    }

    @Override
    public String toString() {
        return mKey;
    }

    @Override
    public int hashCode() {
        return mKey.hashCode();
    }

    @Override
    public boolean equals(final Object object) {
        return mKey != null && mKey.contentEquals(((DataWhole) object).getKey());
    }

    @Override
    public DataWholePartial toPartial() {
        return new DataWholePartial()
                .setFileName(mFileName)
                .setMimeType(mMimeType)
                .setKey(mKey)
                .setNumOfPieces(mNoPieces);
    }

    @Override
    public DataWholeMessage toMessage() {
        return DataWholeMessage.newBuilder()
                .setKey(mKey)
                .setNoPieces(mNoPieces)
                .setFileName(mFileName)
                .build();
    }
}
