package com.fyp.resilience.event;

import com.fyp.resilience.swarm.model.SwarmClient;

public final class ClientModified {
    
    private final SwarmClient mClient;
    
    public ClientModified(final SwarmClient client) {
        mClient = client;
    }
    
    public SwarmClient getClient() {
        return mClient;
    }
}
