
package com.fyp.widerst.partial;

public class DataWholePartial {

    private String mKey;
    private int mNumOfPieces;
    private String mFileName;
    private String mMimeType;

    public DataWholePartial() {
    }

    public DataWholePartial(String key, int numOfPieces, String fileName, String mimeType) {
        mKey = key;
        mNumOfPieces = numOfPieces;
        mFileName = fileName;
        mMimeType = mimeType;
    }

    public String getFileName() {
        return mFileName;
    }
    
    public String getKey() {
        return mKey;
    }
    
    public String getMimeType() {
        return mMimeType;
    }
    
    public void setKey(String key) {
        mKey = key;
    }

    public int getNumOfPieces() {
        return mNumOfPieces;
    }
    
    public void setFileName(String filename) {
        mFileName = filename;
    }
    
    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }

    public void setNumOfPieces(int numOfPieces) {
        mNumOfPieces = numOfPieces;
    }

}
