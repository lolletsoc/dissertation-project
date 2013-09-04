
package com.fyp.resilience.swarm.helper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.swarm.model.SwarmClient;
import com.fyp.resilience.thread.ResilienceThreadFactory;

public class NsdHelper implements SwarmHelperInterface {

    private static final String TAG = NsdHelper.class.getSimpleName();

    private final ExecutorService mNsdExecutorService = Executors
            .newSingleThreadExecutor(new ResilienceThreadFactory());

    private final ResilienceController mController;

    private static final String MULTICAST_LOCK_NAME = "nsd_lock";
    private static final String SERVICE_TYPE = "_resil._tcp.local.";

    private final WifiManager mWifiManager;
    private MulticastLock mLock;
    private JmDNS mJmDNS;
    private ServiceInfo mServiceInfo;
    private InetAddress mInetAddress;

    private final ServiceListener mServiceListener = new ServiceListener() {

        @SuppressWarnings("deprecation")
        @Override
        public void serviceResolved(final ServiceEvent serviceEvent) {

            /*
             * Very easy to receive junk resolutions. Ensure that we're not
             * talking to ourself!
             */
            if (serviceEvent.getType().equals(SERVICE_TYPE) &&
                    !serviceEvent.getName().equals(mServiceInfo.getName()) &&
                    !mInetAddress.equals(serviceEvent.getInfo().getInetAddress())) {

                mController.addClientToList(new SwarmClient(serviceEvent.getInfo()));
                Log.d(TAG, "A service has been resolved");
            }
        }

        @Override
        public void serviceRemoved(ServiceEvent serviceEvent) {

            final SwarmClient client = new SwarmClient(serviceEvent.getInfo());
            Log.d(TAG, "A service has been removed");
            mController.removeClientFromList(client);
        }

        @Override
        public void serviceAdded(final ServiceEvent serviceEvent) {
            if (serviceEvent.getType().equals(SERVICE_TYPE) &&
                    !serviceEvent.getName().equals(mServiceInfo.getName())) {
                Log.d(TAG, "A service has been added");
                mJmDNS.requestServiceInfo(serviceEvent.getType(), serviceEvent.getName(), 1);
            }
        }
    };

    public NsdHelper(final ResilienceController controller, final WifiManager wifiMgr) {
        mController = controller;
        mWifiManager = wifiMgr;
    }

    @Override
    public void initialise() {

        /*
         * Must be performed on a thread otherwise a
         * NetworkOnMainThreadException will be raised
         */
        mNsdExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                final int intAddr = mWifiManager.getConnectionInfo().getIpAddress();

                if (intAddr == 0) {
                    return;
                }

                /* Elegant way of calculating an IP address from an integer */
                final byte[] byteaddr = new byte[] {
                        (byte) (intAddr & 0xff),
                        (byte) (intAddr >> 8 & 0xff),
                        (byte) (intAddr >> 16 & 0xff),
                        (byte) (intAddr >> 24 & 0xff)
                };

                try {
                    mInetAddress = InetAddress.getByAddress(byteaddr);

                    Log.d(TAG, "Network address registered with: " + mInetAddress.getHostAddress());

                    mLock = mWifiManager.createMulticastLock(MULTICAST_LOCK_NAME);
                    mLock.setReferenceCounted(true);
                    mLock.acquire();
                    try {
                        mJmDNS = JmDNS.create(mInetAddress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    @Override
    public void register(final int port, final boolean discover, Context context) {
        mNsdExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                mServiceInfo = ServiceInfo.create(SERVICE_TYPE, "resilservice", port,
                        "Resilience service for Data Upload");
                try {
                    mJmDNS.registerService(mServiceInfo);
                    Log.d(TAG, "Registered");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        if (discover) {
            discover();
        }

    }

    @Override
    public void discover() {
        mNsdExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                mJmDNS.addServiceListener(SERVICE_TYPE, mServiceListener);
                Log.d(TAG, "Discovering");
            }
        });
    }

    @Override
    public void stopDiscovery() {
        Log.d(TAG, "Stopped Discovering!");
        mJmDNS.removeServiceListener(SERVICE_TYPE, mServiceListener);
    }

    @Override
    public void tearDown() {
        mNsdExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Tearing down!");
                mJmDNS.removeServiceListener(SERVICE_TYPE, mServiceListener);
                try {
                    mJmDNS.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mLock.release();
            }
        });
    }

}
