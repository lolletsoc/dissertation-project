
package com.fyp.resilience.connection;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.content.Context;

import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.ConnectionProgressChange;
import com.fyp.resilience.event.ConnectionStateChange;

import de.greenrobot.event.EventBus;

/**
 * An Abstract class used for Connectables. Each Connectable class inherits from
 * this and provides a base for Views i.e. Progress changes and state changes.
 */
public abstract class Connectable implements Runnable {

    /* Constants to specify the type of connection this Connectable relates to */
    public static final int CONNECTION_TYPE_WIFI_DOWNLOAD = 1;
    public static final int CONNECTION_TYPE_WIFI_UPLOAD = 2;
    public static final int CONNECTION_TYPE_SERVER_UPLOAD = 3;
    public static final int CONNECTION_TYPE_SERVER_DOWNLOAD = 4;

    /* Constants to specify the connections up-to-date status */
    public static final int STATUS_IN_PROGRESS = 4;
    public static final int STATUS_WAITING = 5;
    public static final int STATUS_SUCCESS = 6;
    public static final int STATUS_NOT_REQUIRED = 7;
    public static final int STATUS_NONE_REQUIRED = 8;
    public static final int STATUS_HASHING = 9;
    public static final int STATUS_CONNECTION_ERROR = 10;
    public static final int STATUS_HASH_ERROR = 11;
    public static final int STATUS_REGISTRATION_ERROR = 12;
    public static final int STATUS_RETRYABLE = 13;
    public static final int STATUS_BACKING_OFF = 14;

    protected int mConnectionType;
    protected DataWhole mDataWhole;
    protected final WeakReference<Context> mContext;

    /* Guarantees atomic access */
    protected volatile int mProgress;
    protected volatile int mConnectionStatus;

    /**
     * @param dataPiece
     * @param file
     * @param service
     * @throws IOException
     */
    public Connectable(final Context context, final DataWhole dataWhole) {
        mContext = new WeakReference<Context>(context);
        mDataWhole = dataWhole;
        mConnectionStatus = STATUS_WAITING;
    }

    @Override
    public void run() {
        mConnectionStatus = STATUS_IN_PROGRESS;
        notifyOfStateChange();
    }

    protected abstract int runTask();

    protected abstract void runPostTask();

    /**
     * Posts a {@link ConnectionProgressChange} to the EventBus.
     */
    protected void notifyOfProgressChange() {
        postEvent(new ConnectionProgressChange(this));
    }

    /**
     * Posts a {@link ConnectionStateChange} to the EventBus.
     */
    protected void notifyOfStateChange() {
        postEvent(new ConnectionStateChange(this));
    }

    /**
     * @return
     */
    public int getConnectionStatus() {
        return mConnectionStatus;
    }

    /**
     * @return This objects connection constant.
     */
    public int getConnectionType() {
        return mConnectionType;
    }

    /**
     * @return This objects {@link DataWhole}
     */
    public DataWhole getDataWhole() {
        return mDataWhole;
    }

    /**
     * WARNING - THIS FUNCTION ACCESSES A VOLATILE VARIABLE
     * 
     * @return An integer indicating this connectable's progress.
     */
    public int getProgress() {
        return mProgress;
    }

    /**
     * Helper function to post Event objects.
     * 
     * @param event
     */
    private void postEvent(Object event) {
        EventBus.getDefault().post(event);
    }
}
