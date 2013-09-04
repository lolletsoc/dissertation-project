
package com.fyp.resilience.event;

import com.fyp.resilience.connection.WifiUploadConnectable;

public final class WifiUploadFinished {

    private final WifiUploadConnectable mConnectable;

    public WifiUploadFinished(final WifiUploadConnectable connectable) {
        mConnectable = connectable;
    }

    public WifiUploadConnectable getConnectable() {
        return mConnectable;
    }
}
