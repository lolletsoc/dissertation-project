
package com.fyp.resilience.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.net.Uri;

import com.fyp.resilience.Constants;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.util.Utils;

public class ServerDownloadConnectable extends Connectable {

    public ServerDownloadConnectable(final Context context, final DataWhole dataWhole) {
        super(context, dataWhole);
        mConnectionType = Connectable.CONNECTION_TYPE_SERVER_DOWNLOAD;
    }

    @Override
    public void run() {
        super.run();

        mDataWhole.setState(DataWhole.STATE_DOWNLOADING);

        mConnectionStatus = runTask();

        runPostTask();
    }

    @Override
    protected int runTask() {
        InputStream urlStream = null;
        OutputStream fileStream = null;

        try {
            final URL url = new URL(Constants.WIDERST_DOWNLOAD_URL + mDataWhole.getKey());
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            final int response = conn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {

                Context context = mContext.get();

                if (null == context) {
                    return Connectable.STATUS_CONNECTION_ERROR;
                }

                /* Retrieve the length of the file */
                final int fileLength = conn.getContentLength();

                final byte[] buffer = new byte[4096];
                urlStream = conn.getInputStream();
                fileStream = Utils.createFile(context, mDataWhole.getFileName(),
                        Context.MODE_WORLD_READABLE);

                int length;
                long downloaded = 0;
                while ((length = urlStream.read(buffer)) != -1) {
                    fileStream.write(buffer, 0, length);

                    if (fileLength > 0) {
                        downloaded += length;
                        mProgress = (int) (((float) downloaded / fileLength) * 100);
                        notifyOfProgressChange();
                    }
                }

                mDataWhole.setUriString(Uri.fromFile(
                        Utils.getFileFromStream(context, mDataWhole.getFileName())).toString());
                return Connectable.STATUS_SUCCESS;
            }

            return Connectable.STATUS_CONNECTION_ERROR;

        } catch (IOException io) {
            io.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != urlStream) {
                    urlStream.close();
                }
                if (null != fileStream) {
                    fileStream.flush();
                    fileStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Connectable.STATUS_CONNECTION_ERROR;
    }

    @Override
    protected void runPostTask() {
        final Context context = mContext.get();
        if (null != context) {

            final ResilienceController controller = ResilienceController.getInstance(context);
            if (mConnectionStatus == Connectable.STATUS_SUCCESS) {

                controller.removeDataPieces(mDataWhole);
                mDataWhole.setAvailability(true);
                mDataWhole.setState(DataWhole.STATE_COMPLETED);
                controller.addDataWhole(mDataWhole);

            } else {
                mDataWhole.setState(DataWhole.STATE_COMPLETED);
            }

            controller.removeConnection(this);
        }
    }
}
