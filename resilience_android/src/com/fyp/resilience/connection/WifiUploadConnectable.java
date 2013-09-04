
package com.fyp.resilience.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;

import android.content.Context;
import android.util.Log;

import com.fyp.resilience.Flags;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.WifiUploadFinished;
import com.fyp.resilience.proto.ProtoBuffSpecification.PieceUploadReply;
import com.fyp.resilience.proto.ProtoBuffSpecification.PieceUploadRequest;
import com.fyp.resilience.proto.ProtoBuffSpecification.PieceUploadReply.Result;
import com.fyp.resilience.swarm.model.SwarmClient;
import com.fyp.resilience.util.ConnectionUtils;

import de.greenrobot.event.EventBus;

public class WifiUploadConnectable extends UploadConnectable {

    private static final String TAG = WifiUploadConnectable.class.getSimpleName();

    private final SwarmClient mClient;

    public WifiUploadConnectable(final Context context, final DataWhole dataWhole, final DataPiece dataPiece,
            final File file, final SwarmClient client)
            throws FileNotFoundException, IOException, NullPointerException {
        super(context, dataWhole, dataPiece, file);
        mClient = client;
        mConnectionType = Connectable.CONNECTION_TYPE_WIFI_UPLOAD;
    }

    /**
     * @return
     */
    public SwarmClient getClient() {
        return mClient;
    }

    @Override
    protected int runTask() {

        mDataPiece.setState(STATUS_IN_PROGRESS);

        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;
        Socket socket = null;

        try {
            socket = new Socket(mClient.getAddress(), mClient.getPort());
            socket.setSoTimeout(10000);

            PieceUploadRequest uploadRequest = PieceUploadRequest.newBuilder()
                    .setDataPieceMessage(mDataPiece.toMessage())
                    .setDataWholeMessage(mDataWhole.toMessage())
                    .build();

            outputStream = new DataOutputStream(socket.getOutputStream());
            ConnectionUtils.writeByteArrayToStreamWithLengthPrefix(uploadRequest.toByteArray(),
                    outputStream);

            inputStream = new DataInputStream(socket.getInputStream());
            /* Parse the size of the message */
            int length_of_message = inputStream.readInt();

            if (Flags.DEBUG) {
                Log.d(TAG, "Length of message: " + length_of_message);
            }

            byte[] message = new byte[length_of_message];
            inputStream.read(message);
            PieceUploadReply uploadReply = PieceUploadReply.parseFrom(message);

            Result result = uploadReply.getResult();
            if (result != Result.SUCCESS) {
                Log.d(TAG, "Client returned message: " + result.toString());
                return Connectable.STATUS_SUCCESS;
            }

            if (Flags.DEBUG) {
                Log.d(TAG, "Result: " + result);
            }

            do {
                try {
                    final byte[] buff = new byte[4096];
                    double totalRead = 0;
                    int lengthRead = 0;
                    int previousProgress = 0;

                    while ((lengthRead = mFile.read(buff)) != -1) {
                        outputStream.write(buff, 0, lengthRead);
                        totalRead += lengthRead;
                        mProgress = (int) ((totalRead / mPieceSize) * 100);
                        if (mProgress != previousProgress) {
                            notifyOfProgressChange();
                        }
                        previousProgress = mProgress;
                    }

                    /* Ensure the Stream is flushed */
                    outputStream.flush();

                    length_of_message = inputStream.readInt();
                    message = new byte[length_of_message];
                    inputStream.read(message);
                    uploadReply = PieceUploadReply.parseFrom(message);

                    result = uploadReply.getResult();

                } catch (Exception e) {
                    e.printStackTrace();
                    return Connectable.STATUS_CONNECTION_ERROR;
                }

            } while (result != Result.SUCCESS);

            return Connectable.STATUS_SUCCESS;

        } catch (IOException e) {
            e.printStackTrace();
            return Connectable.STATUS_CONNECTION_ERROR;
        } catch (Exception e) {
            e.printStackTrace();
            return Connectable.STATUS_CONNECTION_ERROR;
        } finally {
            try {

                if (null != socket) {
                    socket.close();
                }

                if (null != outputStream) {
                    outputStream.close();
                }

                if (null != inputStream) {
                    inputStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void runPostTask() {
        final Context context = mContext.get();
        if (null != context) {
            if (mConnectionStatus == Connectable.STATUS_SUCCESS) {
                mDataPiece.setState(DataPiece.STATE_UPLOADED_TO_DEVICE);
            } else {
                mDataPiece.setState(DataPiece.STATE_NONE);
            }
            ResilienceController.getInstance(context).removeConnection(this);
        }
    }

    @Override
    protected void notifyOfCompletion() {
        EventBus.getDefault().post(new WifiUploadFinished(this));
    }
}
