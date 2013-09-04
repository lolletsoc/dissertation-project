
package com.fyp.resilience.event;

import com.fyp.resilience.database.model.DataWhole;

public final class ConnectionsModified {
    
    private final DataWhole mDataWhole;
    
    public ConnectionsModified(final DataWhole dataWhole) {
        mDataWhole = dataWhole;
    }
    
    public DataWhole getDataWhole() {
        return mDataWhole;
    }

}
