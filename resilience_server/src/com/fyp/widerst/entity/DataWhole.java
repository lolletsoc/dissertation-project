
package com.fyp.widerst.entity;

import java.util.ArrayList;
import java.util.List;

import com.fyp.widerst.partial.DataWholePartial;
import com.google.appengine.api.blobstore.BlobKey;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Cache
@Entity
public class DataWhole {

    @Id
    private String mKey;
    
    /* Defines the number of children pieces */
    private int mNumOfPieces;
    
    /* Defines the file's original filename */
    private String mFileName;
    
    /* Defines the BlobKey of the file once completed */
    private BlobKey mBlobKey;

    /* Defines the Mime Type of the original file */
    private String mMimeType;
    
    /* Defines a list of keys relating the this Whole's Pieces */
    private List<Key<DataPiece>> mDataPieceList = new ArrayList<Key<DataPiece>>();
    
    /* Defines a list of DeviceInfo keys that have uploaded 1..* DataPieces */
    private List<Key<DeviceInfo>> mDeviceList = new ArrayList<Key<DeviceInfo>>();
    
    /* OBJECTIFY REQUIRES A NO-ARG CONSTRUCTOR */
    public DataWhole() {

    }

    public DataWhole(DataWholePartial dataWholePartial) {
        mNumOfPieces = dataWholePartial.getNumOfPieces();
        mFileName = dataWholePartial.getFileName();
        mKey = dataWholePartial.getKey();
        mMimeType = dataWholePartial.getMimeType();
    }
    
    public BlobKey getBlobKey() {
        return mBlobKey;
    }

    public String getKey() {
        return mKey;
    }

    public int getNumOfPieces() {
        return mNumOfPieces;
    }
    
    public String getMimeType() {
        return mMimeType;
    }
    
    public String getFileName() {
        return mFileName;
    }
    
    public List<Key<DeviceInfo>> getDeviceInfoKeyList() {
        return mDeviceList;
    }
    
    public void setDeviceInfoKeyList(List<Key<DeviceInfo>> deviceInfoList) {
        mDeviceList = deviceInfoList;
    }
    
    public List<Key<DataPiece>> getDataPieceKeyList() {
        return mDataPieceList;
    }
    
    public void setDataPieceKeyList(List<Key<DataPiece>> dataPieceList) {
        mDataPieceList = dataPieceList;
    }
    
    public void setMimeType(String mimeType) {
        mMimeType = mimeType;
    }
    
    public void setFileName(String filename) {
        mFileName = filename;
    }

    public void setNumOfPieces(int numOfPieces) {
        mNumOfPieces = numOfPieces;
    }
    
    public void setBlobKey(BlobKey blobKey) {
        mBlobKey = blobKey;
    }

    public DataWholePartial toPartial() {
        return new DataWholePartial(mKey, mNumOfPieces, mFileName, mMimeType);
    }

}
