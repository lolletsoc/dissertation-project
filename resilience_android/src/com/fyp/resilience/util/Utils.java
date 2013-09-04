
package com.fyp.resilience.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import com.fyp.resilience.PreferenceConstants;
import com.fyp.resilience.receiver.ConnectivityBroadcastReceiver;
import com.fyp.resilience.register.model.DeviceInfo;
import com.google.android.gcm.GCMRegistrar;

public final class Utils {

    /**
     * @param context - This application's {@link Context}.
     * @param intent - The action {@link Intent} to search for.
     * @return True or false depending on whether an application can handle the
     *         given URI.
     */
    public static boolean isUriAvailable(final Context context, final Intent intent) {
        return context.getPackageManager().resolveActivity(intent, 0) != null;
    }

    /**
     * @param context - This application's {@link Context}.
     * @return True or false depending on whether this device has a camera.
     */
    public static boolean hasCamera(final Context context) {
        final PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
                || pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * @param context - This application's {@link Context}.
     * @param contentUri - The {@link Uri} to query against.
     * @return The {@link Uri}'s file path.
     */
    public static String getFilePathFromUri(final Context context, final Uri contentUri) {

        /* Retrieve the scheme associated with this URI */
        final String uriScheme = contentUri.getScheme();

        /* Check if the URI is a file pointer or content */
        if (ContentResolver.SCHEME_FILE.equals(uriScheme)) {
            return contentUri.getPath();

        } else if (ContentResolver.SCHEME_CONTENT.equals(uriScheme)) {
            /*
             * If content then we must query a content resolver for the true
             * file path
             */
            final Cursor cursor = context.getContentResolver().query(contentUri, null, null, null, null);

            try {
                if (cursor.moveToFirst()) {
                    final int pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                    return cursor.getString(pathIndex);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @param context - This application's {@link Context}.
     * @return The {@link DeviceInfo} of this device.
     */
    public static DeviceInfo getDeviceInfo(final Context context) {
        final String gcmRegistrationId = GCMRegistrar.getRegistrationId(context);
        final String serverId = PreferenceManager.getDefaultSharedPreferences(context).getString(
                PreferenceConstants.SERVER_ID_KEY, "");

        return new DeviceInfo()
                .setDeviceRegistrationId(gcmRegistrationId)
                .setServerRegistrationId(serverId);
    }

    /**
     * Query this device's Connectivity status and retrieves its GCM status.
     * 
     * @param context - This application's {@link Context}.
     * @return True or false depending on whether this device can currently
     *         upload.
     */
    public static boolean canUploadToServer(final Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PreferenceConstants.WIFI_ONLY_KEY, false)) {

            return ConnectivityBroadcastReceiver.isConnected(context)
                    && !getDeviceInfo(context).getServerRegistrationId().equals("")
                    && ConnectivityBroadcastReceiver.isConnectedToWifi(context);
        } else {

            return ConnectivityBroadcastReceiver.isConnected(context)
                    && !getDeviceInfo(context).getServerRegistrationId().equals("");
        }
    }

    /**
     * @param context - This application's {@link Context}.
     * @param name - The file's name.
     * @param mode - The read/write mode of the file.
     * @return The new {@link FileOutputStream} of the file.
     * @throws FileNotFoundException
     */
    public static FileOutputStream createFile(final Context context, final String name, final int mode)
            throws FileNotFoundException {
        return context.openFileOutput(name, mode);
    }

    /**
     * @param context - This application's {@link Context}.
     * @param name - The file's name.
     * @return The found file or null.
     */
    public static File getFileFromStream(final Context context, final String name) {
        return context.getFileStreamPath(name);
    }

}
