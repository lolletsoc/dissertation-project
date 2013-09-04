
package com.fyp.resilience.event;

import com.fyp.resilience.connection.WifiDownloadConnectable;

public final class WifiDownloadFinished {

    private final WifiDownloadConnectable mConnectable;

    public WifiDownloadFinished(final WifiDownloadConnectable connectable) {
        mConnectable = connectable;
    }

    public WifiDownloadConnectable getConnectable() {
        return mConnectable;
    }

}
