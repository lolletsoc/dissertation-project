
package com.fyp.resilience;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.ServerRegistrationChanged;
import com.fyp.resilience.register.Register.Devices;
import com.fyp.resilience.register.model.DeviceInfo;
import com.fyp.resilience.service.PieceUploadService;
import com.fyp.resilience.util.Utils;
import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import de.greenrobot.event.EventBus;

/**
 * An intent service which runs within its own thread. Is responsible for
 * Registration to GCM and the Widerst server. As well as receiving Messages
 * from the Widerst server via GCM.
 */
public class GCMIntentService extends GCMBaseIntentService {

    private Devices mDevicesEndpoint;
    private static final String SENDER_ID = "136104787243";
    private static final String TAG = GCMIntentService.class.getSimpleName();

    /**
     * Register the device for GCM.
     * 
     * @param context the activity's context.
     */
    public static void register(final Context context) {
        GCMRegistrar.checkDevice(context);
        GCMRegistrar.checkManifest(context);
        GCMRegistrar.register(context, SENDER_ID);
    }

    public GCMIntentService() {
        super(SENDER_ID);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDevicesEndpoint = ResilienceApplication.getApplication(getApplicationContext()).getRegistrationService()
                .devices();
    }

    /**
     * Called on registration error. This is called in the context of a Service
     * - no dialog or UI.
     * 
     * @param context the Context
     * @param errorId an error message
     */
    @Override
    public void onError(final Context context, final String errorId) {
        Log.i(TAG, "GCM has errored  " + errorId);
    }

    /**
     * Called when a cloud message has been received.
     */
    @Override
    public void onMessage(final Context context, final Intent intent) {

        final ResilienceController controller = ResilienceApplication.getApplication(this).getResilienceController();
        final String dataWholeKey = intent.getExtras().getString("whole_completed");
        final String dataPieceKey = intent.getExtras().getString("piece_request");

        if (null != dataWholeKey) {

            final DataWhole dataWhole = controller.getDataWholeById(dataWholeKey);

            Log.i(TAG, "DataWhole " + dataWholeKey + " is available on the server");

            if (null != dataWhole && (dataWhole.getUriString() == null || dataWhole.getUriString().equals(""))) {
                dataWhole.setAvailability(true);
                dataWhole.setState(DataWhole.STATE_COMPLETED);
                controller.removeDataPieces(dataWhole);
                controller.addDataWhole(dataWhole);
            }

        } else if (null != dataPieceKey) {
            
            Log.d(TAG, dataPieceKey);

            final String requestWholeKey = intent.getExtras().getString("whole_request");

            if (null != requestWholeKey) {
                final DataWhole dataWhole = controller.getDataWholeById(requestWholeKey);

                if (null != dataWhole) {

                    /* Check if the user has already downloaded the Whole */
                    if (!dataWhole.isAvailable()) {

                        List<DataPiece> pieces = dataWhole.getPieces();
                        if (null != pieces) {
                            for (DataPiece dataPiece : pieces) {
                                if (dataPiece.getKey().equals(dataPieceKey)) {
                                    dataPiece.setRetry(true);
                                    dataWhole.setState(DataWhole.STATE_NONE);
                                    controller.addDataWhole(dataWhole);
                                    if (Utils.canUploadToServer(this)) {
                                        startService(new Intent(this, PieceUploadService.class));
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when a registration token has been received.
     * 
     * @param context the Context
     */
    @Override
    public void onRegistered(final Context context, final String registration) {
        Log.i(TAG, "Device has been registered to GCM with " + registration);
        try {

            if (!GCMRegistrar.isRegisteredOnServer(context)) {
                /*
                 * If the device has NEVER been registered then inform the
                 * server of the new device
                 */
                final DeviceInfo deviceInfo = mDevicesEndpoint.insert(
                        new DeviceInfo().setDeviceRegistrationId(registration))
                        .execute();
                if (null != deviceInfo.getServerRegistrationId()) {
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putString(PreferenceConstants.SERVER_ID_KEY,
                                    deviceInfo.getServerRegistrationId())
                            .commit();

                    GCMRegistrar.setRegisteredOnServer(context, true);
                }

            } else {
                /*
                 * If the device has been registered but this is a NEW
                 * registration then update the server of a change
                 */
                final String serverId = Utils.getDeviceInfo(context).getServerRegistrationId();
                final DeviceInfo deviceInfo = new DeviceInfo()
                        .setServerRegistrationId(serverId)
                        .setDeviceRegistrationId(registration);

                mDevicesEndpoint.update(deviceInfo).execute();
            }

            EventBus.getDefault().post(new ServerRegistrationChanged());

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (Utils.canUploadToServer(this)) {
            startService(new Intent(this, PieceUploadService.class));
        }

    }

    /**
     * Called when the device has been unregistered.
     * 
     * @param context the Context
     */
    @Override
    protected void onUnregistered(final Context context, final String registrationId) {
    }
}
