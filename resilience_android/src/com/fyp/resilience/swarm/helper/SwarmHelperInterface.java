
package com.fyp.resilience.swarm.helper;

import android.content.Context;

public interface SwarmHelperInterface {

    public void initialise();

    public void register(int port, boolean discover, Context context);

    public void discover();

    public void stopDiscovery();

    public void tearDown();

}
