
package com.fyp.widerst.partial;


public class DataPiecePartial {

    /*  */
    private DataWholePartial mWholeParent;
    
    /*  */
    private String mDeviceId;
    
    /*  */
    private int mPieceNo;
    
    /*  */
    private String mKey;
    
    /*  */
    private String mHash;
    
    /*  */
    private boolean mRetry;

    public DataPiecePartial() {
    }

    public DataPiecePartial(String key, String hash, int pieceNo, boolean retry, DataWholePartial wholeParent) {
        mKey = key;
        mPieceNo = pieceNo;
        mWholeParent = wholeParent;
        mHash = hash;
        mRetry = retry;
    }

    public int getPieceNo() {
        return mPieceNo;
    }

    public void setPieceNo(int pPieceNo) {
        mPieceNo = pPieceNo;
    }
    
    public String getHash() {
        return mHash;
    }
    
    public boolean isRetry() {
        return mRetry;
    }
    
    public void setRetry(boolean retry) {
        mRetry = retry;
    }
    
    public void setHash(String hash) {
        mHash = hash;
    }
    
    public void setKey(String pKey){
        mKey = pKey;
    }
    
    public String getKey() {
        return mKey;
    }

    public DataWholePartial getWholeParent() {
        return mWholeParent;
    }

    public void setWholeParent(DataWholePartial wholeParent) {
        mWholeParent = wholeParent;
    }
    
    public String getDeviceId() {
        return mDeviceId;
    }
    
    public void setDeviceId(String deviceId) {
        mDeviceId = deviceId;
    }
}
