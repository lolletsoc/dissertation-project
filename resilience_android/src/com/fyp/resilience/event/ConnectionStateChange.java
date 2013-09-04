package com.fyp.resilience.event;

import com.fyp.resilience.connection.Connectable;

public final class ConnectionStateChange {

    private final Connectable mConnectable;
    
    public ConnectionStateChange(final Connectable connectable) {
        mConnectable = connectable;
    }
    
    public Connectable getConnectable() {
        return mConnectable;
    }
    
}
