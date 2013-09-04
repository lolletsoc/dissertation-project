
package com.fyp.resilience.swarm.model;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.ServiceInfo;

import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.event.ClientModified;

import de.greenrobot.event.EventBus;

public class SwarmClient {

    private InetAddress mAddress;
    private int mPort;

    private int mSuccessAttempts;
    private int mFailedAttempts;

    private boolean mWifiDirect;
    private String mMacAddress;

    private boolean mBusy;
    private Map<String, DataPiece> mPieceMap;

    public SwarmClient(final InetAddress address, final int port) {
        mAddress = address;
        mPort = port;
        mBusy = false;
        mWifiDirect = false;
    }

    @SuppressWarnings("deprecation")
    public SwarmClient(final ServiceInfo serviceInfo) {
        mAddress = serviceInfo.getInetAddress();
        mPort = serviceInfo.getPort();
        mWifiDirect = false;
    }

    public InetAddress getAddress() {
        return mAddress;
    }
    
    public void setAddress(final InetAddress address) {
        mAddress = address;
    }

    public int getPort() {
        return mPort;
    }

    public String getMac() {
        return mMacAddress;
    }

    public void addSuccessfulAttempt() {
        mSuccessAttempts++;
        mFailedAttempts = 0;
        EventBus.getDefault().post(new ClientModified(this));
    }

    public void addFailedAttempt() {
        mFailedAttempts++;
        mSuccessAttempts = 0;
        EventBus.getDefault().post(new ClientModified(this));
    }

    public int getSuccessfulAttempts() {
        return mSuccessAttempts;
    }

    public int getFailedAttempts() {
        return mFailedAttempts;
    }

    public boolean isWifiDirect() {
        return mWifiDirect;
    }

    public boolean isBusy() {
        return mBusy;
    }

    public void setBusy(boolean busy) {
        mBusy = busy;
    }

    public void addPieceToMap(final DataPiece dataPiece) {
        if (null == mPieceMap) {
            mPieceMap = new HashMap<String, DataPiece>();
        }
        mPieceMap.put(dataPiece.getKey(), dataPiece);
    }

    public void removeFromMap(final DataPiece dataPiece) {
        if (null != mPieceMap) {
            mPieceMap.remove(dataPiece.getKey());
        }
    }

    public boolean hasPiece(final DataPiece dataPiece) {
        if (null != mPieceMap) {
            return mPieceMap.containsKey(dataPiece.getKey());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return mAddress.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return null != mAddress && mAddress.equals(((SwarmClient) o).getAddress());
    }
}
