
package com.fyp.resilience.receiver;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.fyp.resilience.Constants;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.service.PieceUploadService;
import com.fyp.resilience.swarm.helper.NsdHelper;
import com.fyp.resilience.swarm.helper.SwarmHelperInterface;
import com.fyp.resilience.swarm.helper.WifiDirectSdHelper;
import com.fyp.resilience.util.Utils;

public class ConnectivityBroadcastReceiver extends AbstractConnectivityBroadcastReceiver {

    private static final String TAG = ConnectivityBroadcastReceiver.class.getSimpleName();

    private SwarmHelperInterface mNsdHelper;
    private SwarmHelperInterface mWifiDirectHelper;

    private final ResilienceController mController;
    private final WifiManager mWifiMgr;
    private final PieceUploadService mService;

    private boolean mNsdDiscovering;
    private boolean mWifiDiscovering;

    public ConnectivityBroadcastReceiver(final ResilienceController controller, final WifiManager wifiMgr,
            final PieceUploadService service) {
        mController = controller;
        mWifiMgr = wifiMgr;
        mService = service;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (Utils.canUploadToServer(context)) {
                context.startService(new Intent(context, PieceUploadService.class));
            }

            if (ConnectivityBroadcastReceiver.isConnectedToWifi(context)) {
                if (!mNsdDiscovering) {
                    Log.d(TAG, "Starting NSDHelper");
                    mNsdHelper = new NsdHelper(mController, mWifiMgr);
                    mNsdHelper.initialise();
                    mNsdHelper.register(Constants.CLIENT_DATA_PORT, true, context);
                    mNsdDiscovering = true;
                }
            } else {
                /*
                 * If the Wi-Fi has been disconnected then clear the client list
                 * as it is no longer valid
                 */
                ResilienceController.getInstance(context).clearClientList();

                /* Check to ensure it hasn't already been destroyed */
                if (null != mNsdHelper) {
                    Log.d(TAG, "Destroying NSDHelper");
                    mNsdDiscovering = false;
                    mNsdHelper.tearDown();
                    mNsdHelper = null;
                }
            }
        } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            if (isWifiOnline(context)) {
                if (!mWifiDiscovering) {
                    Log.d(TAG, "Starting WifiDirectHelper");
                    mWifiDirectHelper = new WifiDirectSdHelper(mService);
                    mWifiDirectHelper.initialise();
                    mWifiDirectHelper.register(Constants.CLIENT_DATA_PORT, true, context);
                    mWifiDiscovering = true;
                }
            } else {
                if (null != mWifiDirectHelper) {
                    Log.d(TAG, "Destroying WifiDirectHelper");
                    mWifiDiscovering = false;
                    mWifiDirectHelper.tearDown();
                    mWifiDirectHelper = null;
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        mNsdDiscovering = false;
        mNsdHelper.tearDown();
        mNsdHelper = null;

        mWifiDiscovering = false;
        mWifiDirectHelper.tearDown();
        mWifiDirectHelper = null;
    }

    /**
     * Indicates whether the device is currently connected to the Internet.
     * 
     * @param {@link Context}
     * @return A boolean indicating whether the device is connected to the
     *         Internet.
     */
    public static boolean isConnected(final Context context) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        return null != netInfo && netInfo.isConnected();
    }

    /**
     * @param context
     * @return
     */
    public static boolean isWifiOnline(final Context context) {
        final WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiMgr.isWifiEnabled();
    }

    /**
     * @param context
     * @return
     */
    public static boolean isConnectedToWifi(final Context context) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo wifiInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return (null != wifiInfo && wifiInfo.isConnected());
    }
}
