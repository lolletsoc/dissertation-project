
package com.fyp.widerst.util;

import static com.fyp.widerst.WiderstObjectifyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fyp.widerst.Constants;
import com.fyp.widerst.endpoint.DeviceInfoEndpoint;
import com.fyp.widerst.entity.DataPiece;
import com.fyp.widerst.entity.DataWhole;
import com.fyp.widerst.entity.DeviceInfo;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

public final class GcmHelper {

    /* May as well used code already used within the Endpoint */
    private static final DeviceInfoEndpoint deviceEndpoint = new DeviceInfoEndpoint();
    
    private static final Logger logger = Logger.getLogger(GcmHelper.class.getSimpleName());
    
    private static final String WHOLE_COMPLETION_KEY = "whole_completed";
    private static final String PIECE_REQUEST_KEY = "piece_request";
    private static final String WHOLE_REQUEST_KEY = "whole_request";

    /**
     * An entirely "fire and forget" function used to inform clients of a
     * DataWholes completion. It will attempt a retry of 5 times but will not
     * attempt any further validation apart from any Canonical IDs sent back
     * from GCM. These IDs must be updated to reflect GCM's DB.
     * 
     * @param {@link DataWhole} to inform clients about
     * @param {@link List} of {@link DeviceInfo} objects that contain device
     *        registration IDs
     */
    public static void sendWholeCompletionNotification(final DataWhole dataWhole) {

       if (null != dataWhole) {
            
            logger.log(Level.INFO, "Sending GCM completion message for DataWhole " + dataWhole.getKey());
            
            Sender sender = new Sender(Constants.GCM_API_KEY);
            Message message = buildCompletionMessage(dataWhole);

            final Collection<DeviceInfo> devices = ofy().load().keys(dataWhole.getDeviceInfoKeyList()).values();
 
            final List<String> deviceRegistrations = new ArrayList<String>(devices.size());
            for (DeviceInfo device : devices) {
                deviceRegistrations.add(device.toString());
            }

            try {
                MulticastResult multicastResult = sender.send(message, deviceRegistrations, 5);

                /* An error has occurred */
                if (deviceRegistrations.size() != multicastResult.getSuccess()) {

                    Iterator<DeviceInfo> deviceIter = devices.iterator();
                    
                    for (Result result : multicastResult.getResults()) {

                        DeviceInfo deviceInfo = deviceIter.next();
                        
                        if (null != result.getMessageId()) {

                            if (null != result.getCanonicalRegistrationId()) {

                                /*
                                 * Update the DeviceInfo with the new ID and
                                 * post to DB
                                 */
                                deviceInfo.setDeviceRegistrationId(result.getCanonicalRegistrationId());
                                deviceEndpoint.updateDeviceInfo(deviceInfo);
                                logger.log(Level.INFO, "Updated device with new GCM ID: " + deviceInfo);
                            }

                        } else {
                            logger.log(Level.WARNING, "GCM returned result " + result.getErrorCodeName());
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendPieceRequest(final DataPiece dataPiece, final DataWhole dataWhole) {
        
       if (null != dataPiece && null != dataWhole) {
            
            logger.log(Level.INFO, "Sending GCM completion message for DataWhole " + dataPiece.getKey());
            
            Sender sender = new Sender(Constants.GCM_API_KEY);
            Message message = buildRequestMessage(dataWhole, dataPiece);

            final Collection<DeviceInfo> devices = ofy().load().keys(dataWhole.getDeviceInfoKeyList()).values();
 
            final List<String> deviceRegistrations = new ArrayList<String>(devices.size());
            for (DeviceInfo device : devices) {
                deviceRegistrations.add(device.toString());
                logger.log(Level.INFO, "Sending request to Device " + device.getServerRegistrationId());
            }

            try {
                MulticastResult multicastResult = sender.send(message, deviceRegistrations, 5);

                /* An error has occurred */
                if (deviceRegistrations.size() != multicastResult.getSuccess()) {

                    Iterator<DeviceInfo> deviceIter = devices.iterator();
                    
                    for (Result result : multicastResult.getResults()) {

                        DeviceInfo deviceInfo = deviceIter.next();
                        
                        if (null != result.getMessageId()) {

                            if (null != result.getCanonicalRegistrationId()) {

                                /*
                                 * Update the DeviceInfo with the new ID and
                                 * post to DB
                                 */
                                deviceInfo.setDeviceRegistrationId(result.getCanonicalRegistrationId());
                                deviceEndpoint.updateDeviceInfo(deviceInfo);
                                logger.log(Level.INFO, "Updated device with new GCM ID: " + deviceInfo);
                            }

                        } else {
                            logger.log(Level.WARNING, "GCM returned result " + result.getErrorCodeName());
                        }
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Message buildCompletionMessage(final DataWhole dataWhole) {
        return new Message.Builder()
                .addData(WHOLE_COMPLETION_KEY, dataWhole.getKey())
                .collapseKey(dataWhole.getKey())
                .build();
    }

    private static Message buildRequestMessage(final DataWhole dataWhole, final DataPiece dataPiece) {
        return new Message.Builder()
        .addData(PIECE_REQUEST_KEY, dataPiece.getKey())
        .addData(WHOLE_REQUEST_KEY, dataWhole.getKey())
        .collapseKey(dataPiece.getKey())
        .build();
    }

}
