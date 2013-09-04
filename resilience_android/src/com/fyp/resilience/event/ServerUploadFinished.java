package com.fyp.resilience.event;

import com.fyp.resilience.connection.Connectable;

public final class ServerUploadFinished {

    private final Connectable mConnectable;
    
    public ServerUploadFinished(final Connectable connectable) {
        mConnectable = connectable;
    }
    
    public Connectable getConnectable() {
        return mConnectable;
    }
    
}
