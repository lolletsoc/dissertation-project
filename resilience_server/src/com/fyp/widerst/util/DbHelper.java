
package com.fyp.widerst.util;

import static com.fyp.widerst.WiderstObjectifyService.ofy;

import java.util.List;

import com.fyp.widerst.entity.DataPiece;
import com.fyp.widerst.entity.DataWhole;
import com.fyp.widerst.entity.DeviceInfo;
import com.googlecode.objectify.Key;

public final class DbHelper {

    /**
     * 
     * @param {@link String} object which defines the unique key of the {@link DataWhole} to search for.
     * @return {@link DataWhole} instance or null if not found
     */
    public static DataWhole findDataWholeByKey(final String key) {
        return ofy().load().type(DataWhole.class).id(key).get();
    }

    /**
     * 
     * @param {@link String} object that defines the unique key of the {@link DataPiece} to search for.
     * @param {@link DataWhole} parent object
     * @return {@link DataPiece} instance or null if not found
     */
    public static DataPiece findDataPieceByKeyAndParent(final String key, final DataWhole parent) {

        /* Define object Keys */
        final Key<DataWhole> parentKey = Key.create(DataWhole.class, parent.getKey());
        final Key<DataPiece> pieceKey = Key.create(parentKey, DataPiece.class, key);

        /* Search for DataPiece based on both its parent and its ID */
        return ofy().load().type(DataPiece.class).ancestor(parent).filterKey(pieceKey).first().get();
    }
    
    /**
     * 
     * @param {@link DataWhole} parent object
     * @return {@link List} of {@link DataPiece} objects found
     */
    public static List<DataPiece> findDataPiecesByParent(final DataWhole parent) {
        return ofy().load().type(DataPiece.class).ancestor(parent).list();
    }
    
    /**
     * 
     * @param key
     * @return
     */
    public static DeviceInfo findDeviceInfoByKey(final long key) {
        return ofy().load().type(DeviceInfo.class).id(key).get();
    }

}
