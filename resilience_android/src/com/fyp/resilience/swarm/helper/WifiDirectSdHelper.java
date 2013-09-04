
package com.fyp.resilience.swarm.helper;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.DialogListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import com.fyp.resilience.receiver.WiFiDirectBroadcastReceiver;
import com.fyp.resilience.service.PieceUploadService;

public class WifiDirectSdHelper implements SwarmHelperInterface {

    private static final String TAG = WifiDirectSdHelper.class.getSimpleName();
    private static final String TXTRECORD_PORT = "port";
    private static final String SERVICE_INSTANCE = "_resilwi";
    private static final String SERVICE_REG_TYPE = "_resil._tcp";

    private final PieceUploadService mService;
    private final WifiP2pManager mP2PManager;
    private final Channel mChannel;

    private WiFiDirectBroadcastReceiver mWifiReceiver;
    private final IntentFilter mWifiIntentFilter = new IntentFilter();

    private WifiP2pDnsSdServiceRequest mServiceRequest;
    private DnsSdServiceResponseListener mDnsListener;
    private DnsSdTxtRecordListener mDnsTxtRecordListener;
    private ActionListener mServiceAddListener;
    private ActionListener mDiscoverServiceListener;
    private ActionListener mLocalServiceListener;
    private WifiP2pDnsSdServiceInfo mServiceInfo;

    public WifiDirectSdHelper(final PieceUploadService service) {
        mService = service;
        mP2PManager = (WifiP2pManager) mService.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mP2PManager.initialize(mService, mService.getMainLooper(), null);
    }

    private void initialiseDnsListener() {
        mDnsListener = new DnsSdServiceResponseListener() {

            @Override
            public void onDnsSdServiceAvailable(String instanceName,
                    String registrationType, WifiP2pDevice srcDevice) {

                // A service has been discovered. Is this our app?

                if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                    Log.d(TAG, "onBonjourServiceAvailable " + instanceName);
                }
            }
        };
    }

    private void initialiseDnsTxtRecordListener() {
        mDnsTxtRecordListener = new DnsSdTxtRecordListener() {

            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName,
                    Map<String, String> txtRecordMap, WifiP2pDevice srcDevice) {

                if (fullDomainName.contains(SERVICE_INSTANCE)) {

                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = srcDevice.deviceAddress;
                    config.groupOwnerIntent = 0;
                    config.wps.setup = WpsInfo.PBC;

                    mP2PManager.connect(mChannel, config, null);
                }

            }
        };
    }

    private void initialiseServiceAddListener() {
        mServiceAddListener = new ActionListener() {

            @Override
            public void onSuccess() {
                mP2PManager.discoverServices(mChannel, mDiscoverServiceListener);
                Log.d(TAG, "Added service discovery request");
            }

            @Override
            public void onFailure(int arg0) {
                Log.d(TAG, "Failed adding service discovery request " + arg0);
            }
        };
    }

    private void initialiseDiscoverServiceListener() {
        mDiscoverServiceListener = new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Service discovery initiated");
            }

            @Override
            public void onFailure(int arg0) {
                Log.d(TAG, "Service discovery failed " + arg0);
            }
        };
    }

    private void initialiseLocalServiceListener() {
        mLocalServiceListener = new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Added Local Service");
            }

            @Override
            public void onFailure(int error) {
                Log.d(TAG, "Failed to add a service " + error);
            }
        };
    }

    private void initialiseDummyDialogListener() {

        mP2PManager.setDialogListener(mChannel, new DialogListener() {

            @Override
            public void onShowPinRequested(String arg0) {
                Log.d(TAG, "PIN Requested");
            }

            @Override
            public void onDetached(int arg0) {
                Log.d(TAG, "onDetached has been called");
            }

            @Override
            public void onConnectionRequested(WifiP2pDevice arg0, WifiP2pConfig
                    arg1) {
                mP2PManager.connect(mChannel, arg1, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Added DialogListener");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "Failed to add DialogListener");
                    }
                });
                Log.d(TAG, "Accepting Wi-Fi Direct with Overriden DialogListener");
            }

            @Override
            public void onAttached() {
                Log.d(TAG, "onAttached has been called");
            }
        });
    }

    @Override
    public void initialise() {
        mWifiReceiver = new WiFiDirectBroadcastReceiver(mP2PManager, mChannel, mService);

        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mWifiIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        initialiseDnsListener();
        initialiseDnsTxtRecordListener();
        initialiseServiceAddListener();
        initialiseDiscoverServiceListener();
        initialiseLocalServiceListener();
        initialiseDummyDialogListener();
    }

    @Override
    public void register(final int port, final boolean discover, Context context) {
        mP2PManager.initialize(context, context.getMainLooper(), new ChannelListener() {

            @Override
            public void onChannelDisconnected() {
                Log.d(TAG, "Channel disconnected...");
            }
        });

        mService.registerReceiver(mWifiReceiver, mWifiIntentFilter);

        final Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PORT, String.valueOf(port));

        mServiceInfo = WifiP2pDnsSdServiceInfo.newInstance(SERVICE_INSTANCE, SERVICE_REG_TYPE, record);

        mP2PManager.addLocalService(mChannel, mServiceInfo, mLocalServiceListener);

        if (discover) {
            discover();
        }
    }

    @Override
    public void discover() {
        mP2PManager.setDnsSdResponseListeners(mChannel, mDnsListener, mDnsTxtRecordListener);
        mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mP2PManager.addServiceRequest(mChannel, mServiceRequest, mServiceAddListener);
    }

    @Override
    public void stopDiscovery() {
        mP2PManager.removeGroup(mChannel, null);
    }

    @Override
    public void tearDown() {
        stopDiscovery();
        mService.unregisterReceiver(mWifiReceiver);
        mP2PManager.removeLocalService(mChannel, mServiceInfo, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Local services cleared");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Local services failed to clear");
            }
        });

        mP2PManager.removeServiceRequest(mChannel, mServiceRequest, new ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Service requests cleared");
            }

            @Override
            public void onFailure(int reason) {
                Log.d(TAG, "Failed to clear service requests");
            }
        });
    }
}
