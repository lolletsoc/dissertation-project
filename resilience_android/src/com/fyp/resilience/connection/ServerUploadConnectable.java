
package com.fyp.resilience.connection;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import android.content.Context;
import android.util.Log;

import com.fyp.resilience.Flags;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.ServerUploadFinished;
import com.fyp.resilience.widerst.Widerst;
import com.fyp.resilience.widerst.model.DataPiecePartial;
import com.fyp.resilience.widerst.model.PostResponse;
import com.google.api.client.http.HttpMethods;

import de.greenrobot.event.EventBus;

public class ServerUploadConnectable extends UploadConnectable {

    private static final String TAG = ServerUploadConnectable.class.getSimpleName();
    private static final String MULTI_PART_BOUNDARY = "********";
    private static final String DOUBLE_HYPHEN = "--";
    public static final String CRLF = "\r\n";
    private static final int BUFFER_SIZE = 4096;

    /**
     * A workaround to counter issue 9172
     * https://code.google.com/p/googleappengine/issues/detail?id=9172
     */
    private static final HostnameVerifier workaroundVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * @param url - The URL to attempt a connection with.
     * @return A {@link HttpsURLConnection} "linked" with the specified URL.
     */
    private static HttpsURLConnection createMultiPartHttpUrlConnectionFromUrl(final String url) {

        /* If the URL is null then immediately return */
        if (null == url) {
            return null;
        }

        final URL postUrl;
        try {
            postUrl = new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }

        final HttpsURLConnection httpConn;
        try {
            httpConn = (HttpsURLConnection) postUrl.openConnection();
            httpConn.setDoOutput(true);
            httpConn.setUseCaches(false);
            httpConn.setRequestMethod(HttpMethods.POST);
            httpConn.setConnectTimeout(20000);
            httpConn.setReadTimeout(20000);
            httpConn.setRequestProperty("Connection", "Keep-Alive");
            httpConn.setRequestProperty("Cache-Control", "no-cache");
            httpConn.setRequestProperty("Content-Type", "multipart/form-data;boundary="
                    + MULTI_PART_BOUNDARY);
            httpConn.setHostnameVerifier(workaroundVerifier);

        } catch (IOException e) {
            return null;
        }

        return httpConn;
    }

    /**
     * A function to create the multi-part form pre-file upload header. This
     * header constists of the piece & whole IDs as well as the multi-part
     * boundaries.
     * 
     * @param dataPiece - The {@link DataPiece} this connection relates to.
     * @return A {@link StringBuilder} object containing the pre-file header.
     */
    private static StringBuilder createBlobstorePreFileStreamHeader(final DataPiece dataPiece) {

        final StringBuilder headerString = new StringBuilder();
        headerString.append(DOUBLE_HYPHEN + MULTI_PART_BOUNDARY + CRLF);
        headerString.append("Content-Disposition: form-data; name=\"dataPieceId\"");
        headerString.append(CRLF + CRLF);
        headerString.append(dataPiece.getKey());
        headerString.append(CRLF);

        headerString.append(DOUBLE_HYPHEN + MULTI_PART_BOUNDARY + CRLF);
        headerString.append("Content-Disposition: form-data; name=\"dataWholeId\"");
        headerString.append(CRLF + CRLF);
        headerString.append(dataPiece.getParent().getKey());
        headerString.append(CRLF);

        headerString.append(DOUBLE_HYPHEN + MULTI_PART_BOUNDARY + CRLF);
        headerString.append("Content-Disposition: form-data; name=\""
                + dataPiece.getKey() + "\"; filename=\"" + dataPiece.getKey()
                + "\"" + CRLF);

        headerString
                .append("Content-Type: application/octet-stream" + CRLF);
        headerString.append(CRLF);

        return headerString;
    }

    /**
     * A function to create the terminator string of the multi-part request.
     * 
     * @return A {@link StringBuilder} object containing the post-file
     *         terminator.
     */
    private static StringBuilder createBlobstorePostFileStreamTerminator() {
        final StringBuilder requestEndString = new StringBuilder();
        requestEndString.append(CRLF);
        requestEndString.append(DOUBLE_HYPHEN + MULTI_PART_BOUNDARY + DOUBLE_HYPHEN
                + CRLF);

        return requestEndString;
    }

    private final Widerst mService;
    private final long mBackOff;
    private final String mServerId;

    /**
     * @param context - The {@link Context} of the app.
     * @param dataWhole - The {@link DataWhole} parent of the given
     *            {@link DataPiece}.
     * @param dataPiece - The {@link DataPiece} relating to this connection.
     * @param file - The {@link File} of the given {@link DataWhole}.
     * @param service - The instance of the {@link Widerst} service to
     *            communicate with.
     * @param serverId - The current Server ID of this device.
     * @param backoff - The backoff time that the server is currently under.
     * @throws Exception
     */
    public ServerUploadConnectable(final Context context, final DataWhole dataWhole, final DataPiece dataPiece,
            final File file, final Widerst service, final String serverId, final long backoff) throws Exception {

        super(context, dataWhole, dataPiece, file);

        mConnectionType = Connectable.CONNECTION_TYPE_SERVER_UPLOAD;
        mServerId = serverId;
        mService = service;
        mBackOff = backoff;
    }

    private static final int BLOBSTORE_SUCCESS = HttpURLConnection.HTTP_OK;

    @Override
    public int runTask() {

        if (mBackOff > 0) {
            try {
                mConnectionStatus = Connectable.STATUS_BACKING_OFF;
                notifyOfStateChange();

                Thread.sleep(mBackOff);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        mConnectionStatus = Connectable.STATUS_IN_PROGRESS;
        notifyOfStateChange();

        HttpURLConnection httpConn = null;

        try {
            /*
             * Post the DataPiece to the AppEngine backend and wait for a
             * response
             */
            PostResponse postResponse;
            try {
                final DataPiecePartial dataPiecePartial = mDataPiece.toPartial().setDeviceId(
                        mServerId);
                postResponse = mService.pieces().insert(dataPiecePartial).execute();

                /*
                 * Set progress once we've made a connection to ensure a retry
                 * state isn't overwritten
                 */
                mDataPiece.setState(DataPiece.STATE_IN_PROGRESS);

            } catch (EOFException e) {
                return Connectable.STATUS_CONNECTION_ERROR;
            }
            final int postResult = postResponse.getSuccess();

            /* postResult determines the success of the post */
            if (postResult == PostResponse.STATUS_SUCCESS) {

                DataOutputStream requestStream = null;

                try {
                    /*
                     * Instantiate HttpConnection and URL to communicate with
                     * the Blobstore
                     */
                    httpConn = createMultiPartHttpUrlConnectionFromUrl(postResponse.getPostUrl());

                    /* Build the multi-part file requests */
                    final StringBuilder requestHeader = createBlobstorePreFileStreamHeader(mDataPiece);
                    final StringBuilder requestTerminator = createBlobstorePostFileStreamTerminator();

                    httpConn.setFixedLengthStreamingMode(
                            (int) mPieceSize
                                    + requestHeader.toString().length()
                                    + requestTerminator.toString().length());

                    /*
                     * Create a DataOutputStream to construct the multi-part
                     * request
                     */
                    requestStream = new DataOutputStream(
                            httpConn.getOutputStream());

                    /* WRITE HEADER TO THE STREAM */
                    requestStream.writeBytes(requestHeader.toString());

                    /*
                     * Reference previously instantiated RandomAccessFile (field
                     * referencing is SIGNIFICANTLY quicker than getter
                     * functions within Android
                     */

                    final byte[] buff = new byte[BUFFER_SIZE];
                    double totalRead = 0;
                    int lengthRead = 0;
                    int previousProgress = 0;

                    while ((lengthRead = mFile.read(buff)) != -1) {
                        requestStream.write(buff, 0, lengthRead);
                        totalRead += lengthRead;
                        mProgress = (int) ((totalRead / mPieceSize) * 100);
                        if (mProgress != previousProgress) {
                            notifyOfProgressChange();
                        }
                        previousProgress = mProgress;
                    }

                    /* WRITE THE TERMINATOR TO THE STREAM */
                    requestStream.writeBytes(requestTerminator.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    return Connectable.STATUS_RETRYABLE;
                } finally {
                    if (null != requestStream) {
                        requestStream.flush();
                        requestStream.close();
                    }
                    mFile.close();
                }

                /* Notify the DataPiece of a change to its state */
                if (httpConn.getResponseCode() == BLOBSTORE_SUCCESS) {
                    return Connectable.STATUS_SUCCESS;
                } else {
                    return Connectable.STATUS_RETRYABLE;
                }

            } else if (postResult == PostResponse.STATUS_NOT_REQUIRED) {
                return Connectable.STATUS_NOT_REQUIRED;

            } else if (postResult == PostResponse.STATUS_WHOLE_COMPLETE) {
                return Connectable.STATUS_NONE_REQUIRED;

            } else if (postResult == PostResponse.STATUS_FAILED) {
                return Connectable.STATUS_CONNECTION_ERROR;

            } else if (postResult == PostResponse.STATUS_BUSY) {
                return Connectable.STATUS_CONNECTION_ERROR;

            } else if (postResult == PostResponse.STATUS_REGISTRATION_ERROR) {
                return Connectable.STATUS_REGISTRATION_ERROR;

            }

        } catch (IOException e) {
            /*
             * TODO: If this is called then something has gone VERY wrong. The
             * file SHOULD exist due to being checked within Connectable.
             */
            if (Flags.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            /* Disconnect the connection to ensure the GC can reclaim */
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }

        return Connectable.STATUS_CONNECTION_ERROR;

    }

    @Override
    protected void runPostTask() {
        final Context context = mContext.get();
        if (null != context) {
            final ResilienceController controller = ResilienceController.getInstance(context);
            switch (mConnectionStatus) {
                case Connectable.STATUS_SUCCESS:
                case Connectable.STATUS_NOT_REQUIRED:
                    mDataPiece.setState(DataPiece.STATE_UPLOADED_TO_SERVER);
                    mDataPiece.setRetry(false);
                    Log.d(TAG, mDataPiece.getKey() + "Piece uploaded");
                    break;

                case Connectable.STATUS_NONE_REQUIRED:
                    mDataWhole.setState(DataWhole.STATE_COMPLETED);
                    mDataWhole.setAvailability(true);
                    controller.removeDataPieces(mDataWhole);
                    controller.addDataWhole(mDataWhole);

                    Log.d(TAG, mDataPiece.getKey() + "Piece none required");
                    break;

                case Connectable.STATUS_RETRYABLE:
                    mDataPiece.setRetry(true);
                    Log.d(TAG, mDataPiece.getKey() + "Piece requires a try");
                    break;

                default:
                    mDataPiece.setState(DataPiece.STATE_NONE);
                    Log.d(TAG, mDataPiece.getKey() + "Piece errored with " + mConnectionStatus);
                    break;
            }
            controller.removeConnection(this);
        }
    }

    @Override
    protected void notifyOfCompletion() {
        EventBus.getDefault().post(new ServerUploadFinished(this));
    }
}
