
package com.fyp.widerst.endpoint;

import static com.fyp.widerst.WiderstObjectifyService.ofy;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.fyp.widerst.Constants;
import com.fyp.widerst.entity.DataPiece;
import com.fyp.widerst.entity.DataWhole;
import com.fyp.widerst.entity.DeviceInfo;
import com.fyp.widerst.partial.DataPiecePartial;
import com.fyp.widerst.partial.DataWholePartial;
import com.fyp.widerst.response.PostResponse;
import com.fyp.widerst.util.DbHelper;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;

/**
 * @author Liam Costello
 */
@Api(name = "widerst", description = "Endpoint for DataPieces to be posted")
public class DataPieceEndpoint {

    private static final Logger logger = Logger.getLogger(DataPieceEndpoint.class.getName());

    /**
     * This receives a {@link DataPiecePartial} which is then translated into a
     * DataPiece Entity. The method checks to see if a key was provided, if so
     * then it assigns the {@link DataPiece} to the correct {@link DataWhole}.
     * If not, then it creates a new {@link DataWhole} to which the
     * {@link DataPiece} is assigned.
     * 
     * @param a {@link DataPiecePartial} that contains a
     *            {@link DataWholePartial}
     * @return {@link PostResponse}
     */
    @ApiMethod(name = "pieces.insert", httpMethod = HttpMethod.POST, path = "pieces")
    public PostResponse insertDataPiece(final DataPiecePartial dataPiecePartial) {
        BlobstoreService blobStore = BlobstoreServiceFactory
                .getBlobstoreService();

        final String stringKey = dataPiecePartial.getWholeParent().getKey();

        final Long deviceId;
        try {
            deviceId = Long.parseLong(dataPiecePartial.getDeviceId());
        } catch (NumberFormatException nmf) {
            return new PostResponse(PostResponse.STATUS_FAILED);
        }

        /*
         * If the stringKey is NOT null then we must query the Datastore for its
         * existence
         */

        logger.log(Level.INFO, "Retry is: " + dataPiecePartial.isRetry());

        if (!dataPiecePartial.isRetry()) {

            Integer dpTransaction = null;
            if (null != stringKey) {

                dpTransaction = ofy().transact(new Work<Integer>() {
                    @Override
                    public Integer run() {

                        Key<DeviceInfo> deviceKey = null;
                        if (null == deviceId) {
                            return PostResponse.STATUS_REGISTRATION_ERROR;
                        } else {
                            deviceKey = Key.create(DeviceInfo.class, deviceId);
                            DeviceInfo deviceInfo = ofy().load().key(deviceKey).get();
                            if (null == deviceInfo) {
                                return PostResponse.STATUS_REGISTRATION_ERROR;
                            }
                        }

                        DataPiece dataPiece;
                        DataWhole dataWhole = DbHelper.findDataWholeByKey(stringKey);

                        if (null == dataWhole) {
                            dataWhole = new DataWhole(dataPiecePartial.getWholeParent());
                            dataPiece = new DataPiece(Key.create(dataWhole), dataPiecePartial);
                            dataWhole.getDataPieceKeyList().add(Key.create(dataPiece));
                            dataWhole.getDeviceInfoKeyList().add(deviceKey);
                        } else {
                            /* Piece belongs to a known DataWhole */

                            if (null != dataWhole.getBlobKey()) {
                                /*
                                 * Check to see if the server has already
                                 * received all required pieces
                                 */
                                logger.log(Level.INFO, "DataWhole " + dataWhole.getKey()
                                        + " is already complete.");
                                return PostResponse.STATUS_WHOLE_COMPLETE;
                            }

                            Key<DataPiece> dpKey = Key.create(Key.create(dataWhole),
                                    DataPiece.class, dataPiecePartial.getKey());
                            
                            if (dataWhole.getDataPieceKeyList().contains(dpKey)) {
                                return PostResponse.STATUS_NOT_REQUIRED;
                            }

                            dataPiece = new DataPiece(Key.create(dataWhole), dataPiecePartial);
                            dataWhole.getDataPieceKeyList().add(Key.create(dataPiece));
                            dataWhole.getDeviceInfoKeyList().add(deviceKey);

                        }

                        ofy().save().entities(dataPiece, dataWhole).now();
                        return PostResponse.STATUS_SUCCESS;
                    }
                });

            }

            PostResponse response;
            if (dpTransaction == PostResponse.STATUS_SUCCESS) {
                response = new PostResponse(blobStore.createUploadUrl(Constants.POSTBACK_URL),
                        dpTransaction);
            } else {
                response = new PostResponse(dpTransaction);
            }

            return response;
        }

        /* This piece is a retry and we must allow an upload to take place */
        return new PostResponse(blobStore.createUploadUrl(Constants.POSTBACK_URL),
                PostResponse.STATUS_SUCCESS);

    }
}
