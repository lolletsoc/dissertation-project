package com.fyp.resilience.event;

import com.fyp.resilience.connection.Connectable;

public final class ConnectionProgressChange {

    private final Connectable mConnectable;
    
    public ConnectionProgressChange(final Connectable connectable) {
        mConnectable = connectable;
    }
    
    public Connectable getConnectable() {
        return mConnectable;
    }
}
