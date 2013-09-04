
package com.fyp.resilience.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.fyp.resilience.Flags;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.WifiDownloadFinished;
import com.fyp.resilience.proto.ProtoBuffSpecification.DataPieceMessage;
import com.fyp.resilience.proto.ProtoBuffSpecification.DataWholeMessage;
import com.fyp.resilience.proto.ProtoBuffSpecification.PieceUploadReply;
import com.fyp.resilience.proto.ProtoBuffSpecification.PieceUploadReply.Result;
import com.fyp.resilience.proto.ProtoBuffSpecification.PieceUploadRequest;
import com.fyp.resilience.util.ConnectionUtils;
import com.fyp.resilience.util.Utils;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;

import de.greenrobot.event.EventBus;

public class WifiDownloadConnectable extends Connectable {

    private static final int MAXIMUM_PROTO_BUFF_MESSAGE_SIZE = 1048576;
    private static final String TAG = WifiDownloadConnectable.class.getSimpleName();

    private final Socket mClientSocket;
    private DataPiece mDataPiece;

    public WifiDownloadConnectable(final Context context, final DataWhole dataWhole, final Socket socket)
            throws IOException {
        super(context, dataWhole);
        mClientSocket = socket;
        mConnectionType = Connectable.CONNECTION_TYPE_WIFI_DOWNLOAD;
    }

    @Override
    public void run() {
        super.run();

        mConnectionStatus = runTask();

        runPostTask();

        notifyOfCompletion();
    }

    @Override
    protected int runTask() {

        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;
        OutputStream fileStream = null;
        InputStream limitStream = null;

        DataWhole dataWhole = null;
        DataPiece dataPiece = null;

        DataWholeMessage wholeMessage = null;
        DataPieceMessage pieceMessage = null;
        
        Context context;
        
        PieceUploadReply uploadReply;

        String hash;

        try {
            mClientSocket.setSoTimeout(10000);
            outputStream = new DataOutputStream(mClientSocket.getOutputStream());
            inputStream = new DataInputStream(mClientSocket.getInputStream());
            Result result = null;

            /* Parse the size of the message */
            int length_of_message = inputStream.readInt();

            if (Flags.DEBUG) {
                Log.d(TAG, "Length of the message: " + length_of_message);
            }

            if (length_of_message == 0 || length_of_message > MAXIMUM_PROTO_BUFF_MESSAGE_SIZE) {
                return Connectable.STATUS_CONNECTION_ERROR;
            }

            final byte[] message = new byte[length_of_message];
            inputStream.read(message);
            PieceUploadRequest uploadRequest = PieceUploadRequest.parseFrom(message);
            if (uploadRequest == null) {
                return Connectable.STATUS_CONNECTION_ERROR;
            }

            wholeMessage = uploadRequest.getDataWholeMessage();
            pieceMessage = uploadRequest.getDataPieceMessage();

            if (Flags.DEBUG) {
                Log.d(TAG, wholeMessage.toString());
                Log.d(TAG, pieceMessage.toString());
            }

            context = mContext.get();

            if (context == null) {
                return Connectable.STATUS_CONNECTION_ERROR;
            }

            dataWhole = ResilienceController.getInstance(context).getDataWholeById(
                    wholeMessage.getKey());
            dataPiece = null;

            if (dataWhole != null) {

                if (dataWhole.isAvailable()) {

                }

                ArrayList<DataPiece> pieceList = new ArrayList<DataPiece>(dataWhole.getPieces());
                for (DataPiece piece : pieceList) {
                    if (piece.getPieceNo() == pieceMessage.getPieceNo()) {
                        dataPiece = piece;
                    }
                }
            }

            if (dataPiece == null) {
                result = Result.SUCCESS;
            } else {
                result = Result.NOT_REQUIRED;
            }

            uploadReply = PieceUploadReply.newBuilder()
                    .setResult(result)
                    .build();

            ConnectionUtils.writeByteArrayToStreamWithLengthPrefix(uploadReply.toByteArray(),
                    outputStream);

            outputStream.flush();

            if (dataPiece != null) {
                return Connectable.STATUS_NOT_REQUIRED;
            }

            boolean hashResult;
            /* If reached then a piece is required */
            context = mContext.get();

            if (context == null) {
                return Connectable.STATUS_CONNECTION_ERROR;
            }

            /* If the DataWhole has never been acquired then we must create it */
            if (dataWhole == null) {
                dataWhole = DataWhole.getUnownedDataWhole(context, wholeMessage);
            }

            Log.d(TAG, dataWhole.toString());

            dataPiece = DataPiece.getDataPiece(pieceMessage.getPieceNo(), dataWhole,
                    pieceMessage.getPieceSize());
            mDataWhole = dataWhole;
            mDataPiece = dataPiece;

            Log.d(TAG, dataPiece.toString());

            do {

                /* Calculate MD5 Hash whilst receiving data */
                final Hasher md5Hasher = Hashing.md5().newHasher();
                final byte[] buffer = new byte[4096];

                /*
                 * Don't append the data. This ensures that any retries will
                 * automatically overwrite any data
                 */
                fileStream = Utils.createFile(context, dataPiece.getKey(), Context.MODE_PRIVATE);
                limitStream = ByteStreams.limit(inputStream, pieceMessage.getPieceSize());

                int length;
                long downloaded = 0;
                while ((length = limitStream.read(buffer)) != -1) {
                    md5Hasher.putBytes(buffer, 0, length);
                    fileStream.write(buffer, 0, length);
                    downloaded += length;
                    mProgress = (int) (((float) downloaded / pieceMessage.getPieceSize()) * 100);
                    notifyOfProgressChange();
                }

                hash = md5Hasher.hash().toString();
                hashResult = pieceMessage.getMd5Hash().equals(hash);
                fileStream.flush();
                fileStream.close();

                if (hashResult) {
                    result = Result.SUCCESS;
                    Log.d(TAG, "Hash success!");
                } else {
                    result = Result.ERROR;
                    Log.d(TAG, "Hash failed!");
                }

                uploadReply = PieceUploadReply.newBuilder()
                        .setResult(result)
                        .build();

                ConnectionUtils.writeByteArrayToStreamWithLengthPrefix(uploadReply.toByteArray(),
                        outputStream);

                outputStream.flush();

            } while (!hashResult);

            context = mContext.get();
            if (context == null) {
                return Connectable.STATUS_CONNECTION_ERROR;
            }

            final File file = Utils.getFileFromStream(context, dataPiece.getKey());

            if (null == file) {
                return Connectable.STATUS_CONNECTION_ERROR;
            }

            /* If this has been hit then the hash has been successful */
            dataPiece.setHash(hash);
            dataPiece.setUri(Uri.fromFile(file).toString());

            return Connectable.STATUS_SUCCESS;

        } catch (IOException e) {
            e.printStackTrace();
            return Connectable.STATUS_CONNECTION_ERROR;
        } catch (Exception e) {
            e.printStackTrace();
            return Connectable.STATUS_CONNECTION_ERROR;
        } finally {
            try {
                if (null != mClientSocket) {
                    mClientSocket.close();
                }

                if (null != outputStream) {
                    outputStream.close();
                }

                if (null != inputStream) {
                    inputStream.close();
                }

                if (null != fileStream) {
                    fileStream.flush();
                    fileStream.close();
                }

                if (null != limitStream) {
                    limitStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Socket getSocket() {
        return mClientSocket;
    }

    public DataPiece getPiece() {
        return mDataPiece;
    }

    protected void notifyOfCompletion() {
        EventBus.getDefault().post(new WifiDownloadFinished(this));
    }

    @Override
    protected void runPostTask() {
        final Context context = mContext.get();
        if (null != context) {
            if (mConnectionStatus == Connectable.STATUS_SUCCESS) {
                mDataPiece.setState(DataPiece.STATE_NONE);
                mDataWhole.addPiece(mDataPiece);
                ResilienceController.getInstance(context).addDataWhole(mDataWhole);
            }
            ResilienceController.getInstance(context).removeConnection(this);
        }
    }
}
