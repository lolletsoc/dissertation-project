
package com.fyp.resilience.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fyp.resilience.Constants;
import com.fyp.resilience.Flags;
import com.fyp.resilience.PreferenceConstants;
import com.fyp.resilience.R;
import com.fyp.resilience.ResilienceApplication;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.connection.Connectable;
import com.fyp.resilience.connection.ServerUploadConnectable;
import com.fyp.resilience.connection.WifiDownloadConnectable;
import com.fyp.resilience.connection.WifiUploadConnectable;
import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.ServerUploadFinished;
import com.fyp.resilience.event.WifiDownloadFinished;
import com.fyp.resilience.event.WifiUploadFinished;
import com.fyp.resilience.receiver.AbstractConnectivityBroadcastReceiver;
import com.fyp.resilience.receiver.ConnectivityBroadcastReceiver;
import com.fyp.resilience.swarm.model.SwarmClient;
import com.fyp.resilience.util.Utils;
import com.fyp.resilience.widerst.Widerst;

import de.greenrobot.event.EventBus;

/**
 * Responsible for interpreting Event postings and deciding on the pieces that
 * must be uploaded.
 */
public class PieceUploadService extends Service implements ConnectionInfoListener,
        OnSharedPreferenceChangeListener {

    private static final String TAG = PieceUploadService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 100;
    private static final String VERIFICATION_STRING = "resilience_direct_client";

    private ResilienceApplication mApplication;
    private ResilienceController mController;

    private Widerst mWiderstService;

    private ExecutorService mServerUploadExecutor;
    private ExecutorService mWifiUploadExecutor;

    private Notification.Builder mNotificationBuilder;

    private String mServerId;
    private static final int mMaximumServerConnections = 1;
    private int mMaximumWifiConnections;

    private int mCurrentServerConnections;
    private int mCurrentWifiConnections;

    private ServerSocket mSocket;
    private Thread mAcceptThread;
    private Thread mDirectRegistrationThread;

    private IntentFilter mConnIntentFilter;
    private AbstractConnectivityBroadcastReceiver mConnBroadcastReceiver;

    private long mCurrentBackOff = 0;

    private static final Handler mainHandler = new Handler();

    private final Runnable mWifiConnRunnable = new Runnable() {
        @Override
        public void run() {
            mCurrentWifiConnections++;
            updateNotification();
        }
    };

    /**
     * Responsible for listening to {@link Socket} connections sent from
     * {@link WifiUploadConnectable}s.
     */
    class ClientSocketHandler implements Runnable {
        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    Socket clientSocket = mSocket.accept();

                    Log.d(TAG, "Client has hit service!");

                    Connectable wifiConnection = new WifiDownloadConnectable(
                            PieceUploadService.this,
                            null, clientSocket);

                    final InetAddress clientAddress = clientSocket.getInetAddress();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            SwarmClient client = mController.getClientFromListWithAddress(clientAddress);
                            if (null == client) {
                                mController.addClientToList(new SwarmClient(clientAddress, Constants.CLIENT_DATA_PORT));
                            }
                        }
                    });

                    mWifiUploadExecutor.submit(wifiConnection);
                    mController.addConnection(wifiConnection);
                    mainHandler.post(mWifiConnRunnable);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * {@link Runnable} that is posted to the UI Thread.
     */
    class DirectAddToSwarmRunnable implements Runnable {

        private final InetAddress mAddress;

        public DirectAddToSwarmRunnable(final InetAddress address) {
            mAddress = address;
        }

        @Override
        public void run() {
            mController.addClientToList(new SwarmClient(mAddress, Constants.CLIENT_DATA_PORT));
            getAndPostNextWifiUploads();
        }
    }

    /**
     * Responsible for listening to {@link Socket} connections send from Wi-Fi
     * Direct Peers.
     */
    class WifiDirectClientVerification implements Runnable {

        @Override
        public void run() {
            try {
                final ServerSocket registerSocket = new ServerSocket(Constants.CLIENT_VERIFICATION_PORT);
                while (!Thread.interrupted()) {
                    Socket newConnection = registerSocket.accept();

                    Log.d(TAG, "Wi-Fi Direct client is registering");

                    final ObjectInputStream input = new ObjectInputStream(newConnection.getInputStream());
                    final Object verificationObject = input.readObject();

                    /* Ensure it is of type String */
                    if (verificationObject.getClass().equals(String.class)) {

                        final String testString = (String) verificationObject;
                        if (testString.equals(VERIFICATION_STRING)) {

                            Log.d(TAG, "Registered a client: " + newConnection.getInetAddress());

                            mainHandler.post(new DirectAddToSwarmRunnable(newConnection.getInetAddress()));
                        }
                    }

                    input.close();
                    newConnection.close();

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Responsible for posting peer verification connections to the Wi-Fi Direct
     * Group Owner.
     */
    static class WifiDirectClientRegistration implements Runnable {

        private final InetAddress mGroupOwnerAddress;

        private static final int retries = 3;

        public WifiDirectClientRegistration(final InetAddress groupOwner) {
            mGroupOwnerAddress = groupOwner;
        }

        @Override
        public void run() {
            
            int count = 0;
            while (count <= retries) {
                try {
                    
                    /* Ensure the connection has settled */
                    Thread.sleep(2000);
                    
                    final Socket newConnection = new Socket(mGroupOwnerAddress, Constants.CLIENT_VERIFICATION_PORT);

                    Log.d(TAG, "Registering with Group Owner: " + mGroupOwnerAddress);

                    final ObjectOutputStream output = new ObjectOutputStream(newConnection.getOutputStream());
                    output.writeObject(new String(VERIFICATION_STRING));

                    output.flush();
                    output.close();

                    newConnection.close();

                    break;

                } catch (Exception e) {
                    e.printStackTrace();
                    count++;
                }
            }
        }
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {

        /*
         * If connected to a Wi-Fi connection then start the Network Service
         * Discovery helper
         */
        if (mController.getSwarmList().size() > 0) {
            if (mController.hasFilesWaiting()) {
                getAndPostNextWifiUploads();
            }
        }

        /*
         * If the device currently has a connection to the Internet, then
         * attempt a post to the server
         */
        if (ConnectivityBroadcastReceiver.isConnected(this)) {
            getAndPostNextServerUploads();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Flags.DEBUG) {
            Log.i(TAG, "onCreate has been called");
        }

        /* Register this object with the EventBus */
        EventBus.getDefault().register(this);

        /* Obtain Application instances */
        mApplication = ResilienceApplication.getApplication(this);
        mController = mApplication.getResilienceController();
        mServerUploadExecutor = mApplication.getServerUploadThreadExecutorService();
        mWifiUploadExecutor = mApplication.getWifiUploadThreadExecutorService();
        mMaximumWifiConnections = mApplication.getMaximumWifiThreads();
        mWiderstService = mApplication.getWiderstService();

        /* Retrieve initial preference settings */
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mServerId = sharedPreferences.getString(PreferenceConstants.SERVER_ID_KEY, "");

        /* Initialise ConnectivityBroadcastReceiver */
        mConnIntentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mConnIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mConnBroadcastReceiver = new ConnectivityBroadcastReceiver(mController,
                (WifiManager) getSystemService(WIFI_SERVICE), this);
        registerReceiver(mConnBroadcastReceiver, mConnIntentFilter);

        try {
            mSocket = new ServerSocket(Constants.CLIENT_DATA_PORT);

            if (null != mSocket) {
                mAcceptThread = new Thread(new ClientSocketHandler(), "Wi-Fi Acceptance");
                mAcceptThread.start();

                mDirectRegistrationThread = new Thread(new WifiDirectClientVerification(), "Wi-Fi Direct Registration");
                mDirectRegistrationThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);

        mConnBroadcastReceiver.onDestroy();

        EventBus.getDefault().unregister(this);

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

        if (Flags.DEBUG) {
            Log.i(TAG, "onDestroy has been called");
        }
        mController.clearClientList();

        mAcceptThread.interrupt();
        mDirectRegistrationThread.interrupt();

        unregisterReceiver(mConnBroadcastReceiver);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    /**
     * Attempts to retrieve and upload waiting {@link DataPiece}s to the Widerst
     * server.
     */
    private void getAndPostNextServerUploads() {
        if (!Utils.canUploadToServer(this)
                || mCurrentServerConnections >= mMaximumServerConnections) {
            return;
        }

        final DataPiece dataPiece = mController.getNextDataPieceToUpload();
        if (null == dataPiece) {
            if (mController.getConnectionList().size() == 0) {
                stopForeground(true);
            }
            return;
        }

        Connectable connectable = null;
        final DataWhole dataWhole = dataPiece.getParent();
        try {

            File uploadFile = null;
            if (dataWhole.isOwned()) {
                uploadFile = dataWhole.getFile(this);
            } else {
                uploadFile = dataPiece.getFile(this);
            }

            if (null == uploadFile) {
                Log.e(TAG, "FOUND A NULL FILE!?");
                return;
            }

            connectable = new ServerUploadConnectable(this, dataWhole, dataPiece,
                    uploadFile, mWiderstService, mServerId, mCurrentBackOff);

        } catch (FileNotFoundException fe) {
            if (!dataWhole.getFile(this).exists()) {
                // TODO: what to do if file has been deleted
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (null != connectable) {
            mController.addConnection(connectable);
            mServerUploadExecutor.submit(connectable);
            mCurrentServerConnections++;

            updateNotification();
        }
    }

    /**
     * Attempts to retrieve and upload waiting {@link DataPiece}s to
     * {@link SwarmClient}s.
     */
    private void getAndPostNextWifiUploads() {

        /* Obtain all pieces that require an upload */
        final List<DataPiece> uploadPieces = mController.getAllPiecesToUpload();

        if (uploadPieces.size() == 0) {
            if (mController.getConnectionList().size() == 0) {
                stopForeground(true);
            }
            return;
        }

        for (final DataPiece piece : uploadPieces) {

            /*
             * Wi-Fi connections can be triggered by incoming connections.
             * Therefore, it is impossible to control the amount that come in
             * but is possible to limit those going out.
             */
            if (mCurrentWifiConnections >= mMaximumWifiConnections) {
                return;
            }

            List<SwarmClient> swarmList = mController.getSwarmList();

            if (swarmList.size() == 0) {
                return;
            }

            SwarmClient swarmClient = null;
            for (final SwarmClient client : swarmList) {
                if (!client.isBusy() && !client.hasPiece(piece)) {
                    swarmClient = client;
                    break;
                }
            }

            if (swarmClient != null) {

                final DataWhole dataWhole = piece.getParent();
                File uploadFile = null;
                /* If a DataWhole is owned then it WILL have a file attached */
                if (dataWhole.isOwned()) {
                    uploadFile = dataWhole.getFile(this);
                } else {
                    uploadFile = piece.getFile(this);
                }

                if (null != uploadFile) {
                    Connectable connectable = null;
                    try {
                        connectable = new WifiUploadConnectable(this, dataWhole, piece,
                                uploadFile, swarmClient);

                    } catch (FileNotFoundException fe) {
                        // TODO
                    } catch (Exception e) {
                        // TODO
                    }

                    if (null != connectable) {
                        swarmClient.setBusy(true);
                        mController.addConnection(connectable);
                        mWifiUploadExecutor.submit(connectable);
                        mCurrentWifiConnections++;
                        updateNotification();
                    }
                }
            }
        }
    }

    /**
     * @param The {@link ServerUploadFinished} event posted to the EventBus.
     */
    public void onEventMainThread(final ServerUploadFinished event) {
        mCurrentServerConnections--;
        int status = event.getConnectable().getConnectionStatus();
        if (status == Connectable.STATUS_CONNECTION_ERROR || status == Connectable.STATUS_RETRYABLE) {
            /* Calculate the backoff on failure */
            calculateBackoff();
        } else {
            /* Reset any backoff value */
            mCurrentBackOff = 0;
        }
        getAndPostNextServerUploads();
        getAndPostNextWifiUploads();
    }

    /**
     * @param The {@link WifiUploadFinished} event posted to the EventBus.
     */
    public void onEventMainThread(final WifiUploadFinished event) {
        Log.d(TAG, "A Wi-Fi upload has finished!");

        final SwarmClient swarmClient = mController.getClientFromListWithAddress(event.getConnectable().getClient()
                .getAddress());
        if (null != swarmClient) {
            swarmClient.setBusy(false);

            if (event.getConnectable().getConnectionStatus() == Connectable.STATUS_SUCCESS) {
                swarmClient.addSuccessfulAttempt();
                swarmClient.addPieceToMap(event.getConnectable().getDataPiece());
            } else {
                if (swarmClient.getFailedAttempts() >= Constants.MAX_SWARM_CLIENT_RETRIES) {
                    /* Remove the client if it */
                    mController.removeClientFromList(swarmClient);
                } else {
                    swarmClient.addFailedAttempt();
                }
            }

        }

        mCurrentWifiConnections--;
        updateNotification();
        getAndPostNextWifiUploads();
    }

    /**
     * @param The {@link WifiDownloadFinished} event posted to the EventBus.
     */
    public void onEventMainThread(final WifiDownloadFinished event) {
        if (event.getConnectable().getConnectionStatus() == Connectable.STATUS_SUCCESS) {
            final SwarmClient client = mController.getClientFromListWithAddress(event.getConnectable().getSocket()
                    .getInetAddress());
            if (null != client) {
                final DataPiece piece = event.getConnectable().getPiece();

                /*
                 * Add the piece to the client map so that we don't attempt to
                 * upload to the same client
                 */
                if (null != piece) {
                    client.addPieceToMap(piece);
                }

            }
        }
        mCurrentWifiConnections--;
        updateNotification();
        getAndPostNextServerUploads();
        getAndPostNextWifiUploads();
    }

    /**
     * Calculates the exponential back-off for a Server connection. If a
     * connection fails then the current back-off is doubled.
     */
    private void calculateBackoff() {
        if (mCurrentBackOff > 0) {
            if ((mCurrentBackOff << 1) < Constants.MAXIMUM_BACKOFF) {
                mCurrentBackOff <<= 1;
                return;
            }
        }

        mCurrentBackOff = TimeUnit.SECONDS.toMillis(2);
    }

    /**
     * Create/Update the foreground notification.
     */
    private void updateNotification() {

        /* Create the Notification if it doesn't already exist */
        if (null == mNotificationBuilder) {
            mNotificationBuilder = new Notification.Builder(this)
                    .setOngoing(true)
                    .setContentTitle("Resilience")
                    .setSmallIcon(R.drawable.ic_launcher);
        }

        /* Set Notification information */
        mNotificationBuilder.setContentText("Server Connections: " + mCurrentServerConnections
                + "\n" +
                "Wi-Fi Connections: " + mCurrentWifiConnections);
        startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
    }

    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {

        /*
         * Perform check to see who is group owner. This will determine which
         * device goes first.
         */
        if (info.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
        } else {
            new Thread(new WifiDirectClientRegistration(info.groupOwnerAddress)).start();
            mController.addClientToList(new SwarmClient(info.groupOwnerAddress, Constants.CLIENT_DATA_PORT));
            getAndPostNextWifiUploads();
            Log.d(TAG, "Connected as peer");
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {

        if (key.equals(PreferenceConstants.SERVER_ID_KEY)) {
            mServerId = sharedPreferences.getString(PreferenceConstants.SERVER_ID_KEY, "");
        }

        getAndPostNextServerUploads();
    }
}
