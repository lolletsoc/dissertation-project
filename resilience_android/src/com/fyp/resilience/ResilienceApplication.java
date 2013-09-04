
package com.fyp.resilience;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.fyp.resilience.register.Register;
import com.fyp.resilience.service.PieceUploadService;
import com.fyp.resilience.thread.ResilienceThreadFactory;
import com.fyp.resilience.widerst.Widerst;
import com.google.android.gcm.GCMRegistrar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.gson.GsonFactory;

public class ResilienceApplication extends Application {

    private static final float EXECUTOR_POOL_SIZE_PER_CORE = 1.0f;

    /* Controllers */
    private ResilienceController mResilController;

    /* Executors and other threaded services */
    private ExecutorService mDatabaseSingleExecutor;
    private ExecutorService mUploadSingleExecutor;
    private ExecutorService mWifiUploadMultiExecutor;

    private int mMaximumWifiUploads;

    private Widerst mWiderstService;
    private Register mRegistrationService;

    /**
     * @param {@link Context}
     * @return {@link ResilienceApplication}
     */
    public static ResilienceApplication getApplication(Context context) {
        return (ResilienceApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mResilController = new ResilienceController(this);

        mMaximumWifiUploads = Math.round(Runtime.getRuntime().availableProcessors()
                * EXECUTOR_POOL_SIZE_PER_CORE);

        startService(new Intent(this, PieceUploadService.class));

        if (!GCMRegistrar.isRegisteredOnServer(this)) {
            GCMIntentService.register(getApplicationContext());
        }
    }

    /**
     * @return Returns a unique instance of {@link ResilienceController}
     */
    public ResilienceController getResilienceController() {
        return mResilController;
    }

    /**
     * @return {@link Widerst}
     */
    public Widerst getWiderstService() {
        if (null == mWiderstService) {
            mWiderstService = new Widerst.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), null)
                    .build();
        }
        return mWiderstService;
    }

    /**
     * @return {@link Register}
     */
    public Register getRegistrationService() {
        if (null == mRegistrationService) {
            mRegistrationService = new Register.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), null)
                    .build();
        }
        return mRegistrationService;
    }

    /**
     * @return
     */
    public int getMaximumWifiThreads() {
        return mMaximumWifiUploads;
    }

    /**
     * @return Returns a single threaded {@link ExecutorService} used to
     *         communicate with the SQLite Database.
     */
    public ExecutorService getDatabaseThreadExecutorService() {
        /* Ensure the Executor is neither null nor shutdown */
        if (null == mDatabaseSingleExecutor || mDatabaseSingleExecutor.isShutdown()) {
            mDatabaseSingleExecutor = Executors
                    .newSingleThreadExecutor(new ResilienceThreadFactory());
        }
        return mDatabaseSingleExecutor;
    }

    /**
     * @return Returns a single-threaded {@link ExecutorService} used to upload
     *         data to the Widerst server.
     */
    public ExecutorService getServerUploadThreadExecutorService() {
        /* Ensure the Executor is neither null nor shutdown */
        if (null == mUploadSingleExecutor || mUploadSingleExecutor.isShutdown()) {
            mUploadSingleExecutor = Executors
                    .newSingleThreadExecutor(new ResilienceThreadFactory());
        }
        return mUploadSingleExecutor;
    }

    /**
     * @return Returns a multi-threaded {@link ExecutorService} used to upload
     *         data to Wi-Fi devices.
     */
    public ExecutorService getWifiUploadThreadExecutorService() {
        if (null == mWifiUploadMultiExecutor || mWifiUploadMultiExecutor.isShutdown()) {
            mWifiUploadMultiExecutor = Executors.newFixedThreadPool(mMaximumWifiUploads, new ResilienceThreadFactory());
        }
        return mWifiUploadMultiExecutor;
    }
}
