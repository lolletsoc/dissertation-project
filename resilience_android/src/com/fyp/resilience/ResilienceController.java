
package com.fyp.resilience;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;

import com.fyp.resilience.connection.Connectable;
import com.fyp.resilience.database.ResilienceDbManager;
import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.ClientListChanged;
import com.fyp.resilience.event.ConnectionsModified;
import com.fyp.resilience.event.WholeModified;
import com.fyp.resilience.swarm.model.SwarmClient;

import de.greenrobot.event.EventBus;

/**
 * Controls each aspect of the {@link DataWhole} cache, {@link Connectable}
 * list, and {@link SwarmClient} list.
 */
public class ResilienceController {

    private final List<Connectable> mConnectionList;
    private final Map<String, DataWhole> mCacheDataWholeList;
    private final List<SwarmClient> mSwarmList;

    private final Context mContext;

    /**
     * Must ensure that the controller returned is that which is related to this
     * Context. Failure to do so may lead to undesirable errors.
     * 
     * @param context {@link Context}
     * @return {@link ResilienceController}
     */
    public static ResilienceController getInstance(final Context context) {
        return ResilienceApplication.getApplication(context).getResilienceController();
    }

    /**
     * Upon construction, the object constructs a new cache array list, which is
     * then filled from the DB.
     * 
     * @param context {@link Context}
     */
    ResilienceController(final Context context) {

        mContext = context;

        mCacheDataWholeList = new HashMap<String, DataWhole>();
        mConnectionList = new ArrayList<Connectable>();
        mSwarmList = new ArrayList<SwarmClient>();

        List<DataWhole> wholeCache = ResilienceDbManager.getDataWholes(mContext);
        if (null != wholeCache) {
            for (final DataWhole dataWhole : wholeCache) {
                mCacheDataWholeList.put(dataWhole.getKey(), dataWhole);
            }
        }
    }

    /**
     * @return A boolean indicating whether any {@link DataPiece}s require
     *         upload.
     */
    public boolean hasFilesWaiting() {
        synchronized (mCacheDataWholeList) {
            for (final DataWhole dataWhole : mCacheDataWholeList.values()) {
                if (!dataWhole.isAvailable() && null != dataWhole.getPieces()) {
                    for (DataPiece dataPiece : dataWhole.getPieces()) {
                        if (dataPiece.getState() == DataPiece.STATE_NONE || dataPiece.requiresRetry()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /**
     * Returns a new ArrayList that contains all elements within the cache.
     * 
     * @return {@link List}
     */
    public List<DataWhole> getDataWholeCache() {
        synchronized (mCacheDataWholeList) {
            return new ArrayList<DataWhole>(mCacheDataWholeList.values());
        }
    }

    /**
     * Returns a new {@link List} containing the Swarm Clients.
     * 
     * @return
     */
    public List<SwarmClient> getSwarmList() {
        return new ArrayList<SwarmClient>(mSwarmList);
    }

    /**
     * Gets a {@link SwarmClient} from the Swarm list with the provided
     * {@link InetAddress}.
     * 
     * @param address {@link InetAddress}
     * @return {@link SwarmClient}
     */
    public SwarmClient getClientFromListWithAddress(final InetAddress address) {
        for (final SwarmClient client : mSwarmList) {
            if (client.getAddress() != null && client.getAddress().equals(address)) {
                return client;
            }
        }
        return null;
    }

    /**
     * Adds a {@link SwarmClient} to the Swarm list providing it hasn't already.
     * 
     * @param client - The {@link SwarmClient} to be added.
     */
    public void addClientToList(final SwarmClient client) {
        if (!mSwarmList.contains(client)) {
            mSwarmList.add(client);
            postToEventBus(new ClientListChanged());
        }
    }

    /**
     * Removes the provided {@link SwarmClient} from the Swarm list.
     * 
     * @param client - The {@link SwarmClient} to be removed.
     */
    public void removeClientFromList(final SwarmClient client) {
        mSwarmList.remove(client);
        postToEventBus(new ClientListChanged());
    }

    /**
     * Clears the Swarm list of all entities.
     */
    public void clearClientList() {
        mSwarmList.clear();
        postToEventBus(new ClientListChanged());
    }

    /**
     * @return A new {@link List} of {@link Connectable}s.
     */
    public List<Connectable> getConnectionList() {
        synchronized (mConnectionList) {
            return new ArrayList<Connectable>(mConnectionList);
        }
    }

    /**
     * Adds a {@link Connectable} to the Connection list.
     * 
     * @param connection - The {@link Connectable} to be added to the Connection
     *            list.
     */
    public void addConnection(final Connectable connection) {
        if (null != connection) {
            synchronized (mConnectionList) {
                if (!mConnectionList.contains(connection)) {
                    mConnectionList.add(connection);
                }
                postToEventBus(new ConnectionsModified(connection.getDataWhole()));
            }
        }
    }

    /**
     * Removes a {@link Connectable} from the Connection list. Posts a
     * {@link ConnectionsModified} to the EventBus.
     * 
     * @param connection - The {@link Connectable} to be removed
     */
    public void removeConnection(final Connectable connection) {
        if (null != connection) {
            synchronized (mConnectionList) {
                mConnectionList.remove(connection);

                if (null != connection.getDataWhole()) {
                    ResilienceDbManager.persistDataWhole(mContext, connection.getDataWhole());
                }
                postToEventBus(new ConnectionsModified(connection.getDataWhole()));
            }
        }
    }

    /**
     * @param dataWhole - The {@link DataWhole} to be added
     * @return A boolean to dictate whether the DataWhole was persisted or not
     */
    public void addDataWhole(final DataWhole dataWhole) {
        if (null != dataWhole) {
            synchronized (mCacheDataWholeList) {
                if (!mCacheDataWholeList.containsKey(dataWhole.getKey())) {
                    mCacheDataWholeList.put(dataWhole.getKey(), dataWhole);
                }
                ResilienceDbManager.persistDataWhole(mContext, dataWhole);
                postToEventBus(new WholeModified());
            }
        }
    }

    /**
     * @param dataWhole - The {@link DataWhole} to be removed
     * @return A boolean to dictate whether the DataWhole was persisted or not
     */
    public void removeDataWhole(final DataWhole dataWhole) {
        if (null != dataWhole) {
            synchronized (mCacheDataWholeList) {
                mCacheDataWholeList.remove(dataWhole.getKey());
                ResilienceDbManager.removeDataWhole(mContext, dataWhole);
                postToEventBus(new WholeModified());
            }
        }
    }

    /**
     * Removes {@link DataPiece}s from the specified {@link DataWhole}
     * 
     * @param dataWhole - {@link DataWhole} whose {@link DataPiece}s should be
     *            removed.
     */
    public void removeDataPieces(final DataWhole dataWhole) {
        if (null != dataWhole) {
            synchronized (mCacheDataWholeList) {

                final List<DataPiece> pieces = dataWhole.getPieces();
                if (null != pieces) {
                    for (final DataPiece dataPiece : pieces) {
                        File pieceFile = dataPiece.getFile(mContext);
                        if (null != pieceFile) {
                            final boolean result = dataPiece.getFile(mContext).delete();
                            Log.d("PIECE_DELETION", "Piece deleted: " + result);
                        }
                    }
                }

                dataWhole.setPieces(null);
                ResilienceDbManager.removeDataPieces(mContext, pieces);
            }
        }
    }

    /**
     * Searches through the {@link DataWhole} cache and searches for a
     * {@link DataPiece} that requires upload.
     * 
     * @return The first {@link DataPiece} that is available for upload.
     */
    public DataPiece getNextDataPieceToUpload() {
        synchronized (mCacheDataWholeList) {
            for (final DataWhole dataWhole : mCacheDataWholeList.values()) {

                if ((dataWhole.getState() == DataWhole.STATE_IN_PROGRESS
                        || dataWhole.getState() == DataWhole.STATE_NONE)
                        && !dataWhole.isAvailable()
                        && null != dataWhole.getPieces()) {

                    for (final DataPiece dataPiece : dataWhole.getPieces()) {

                        final int state = dataPiece.getState();
                        if (state == DataPiece.STATE_NONE ||
                                state == DataPiece.STATE_UPLOADED_TO_DEVICE ||
                                dataPiece.requiresRetry()) {

                            return dataPiece;
                        }
                    }
                }
            }
            /* Indicates that there is NOTHING waiting to be uploaded */
            return null;
        }
    }

    /**
     * Searches through the {@link DataWhole} cache and searches for all
     * {@link DataPiece}s that require upload.
     * 
     * @return The first {@link DataPiece} that is available for upload or null.
     */
    public List<DataPiece> getAllPiecesToUpload() {
        synchronized (mCacheDataWholeList) {
            final List<DataPiece> uploadPieces = new ArrayList<DataPiece>();
            for (final DataWhole dataWhole : mCacheDataWholeList.values()) {

                if ((dataWhole.getState() == DataWhole.STATE_IN_PROGRESS
                        || dataWhole.getState() == DataWhole.STATE_NONE)
                        && !dataWhole.isAvailable()
                        && null != dataWhole.getPieces()) {

                    for (final DataPiece dataPiece : dataWhole.getPieces()) {
                        if (dataPiece.getState() == DataPiece.STATE_NONE) {
                            uploadPieces.add(dataPiece);
                        }
                    }
                }
            }
            return uploadPieces;
        }
    }

    /**
     * Searches through the cache for a {@link DataWhole} with the specified ID.
     * 
     * @param key - The ID to be searched.
     * @return The {@link DataWhole} or null.
     */
    public DataWhole getDataWholeById(final String key) {
        synchronized (mCacheDataWholeList) {
            final DataWhole dataWhole = mCacheDataWholeList.get(key);
            return dataWhole != null ? dataWhole : null;
        }
    }

    /**
     * A helper method to post an event to the EventBus
     * 
     * @param event - An event object
     */
    private void postToEventBus(final Object event) {
        EventBus.getDefault().post(event);
    }
}
