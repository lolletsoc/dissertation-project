
package com.fyp.widerst.entity;

import com.fyp.widerst.partial.DataPiecePartial;
import com.fyp.widerst.partial.DataWholePartial;
import com.google.appengine.api.blobstore.BlobKey;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class DataPiece {

    @Id
    private String mKey;
    
    /* Indicates this piece's number */
    private int mPieceNo;
    
    /* Indicates the hash assigned against this piece */
    private String mHash;

    @Parent
    private Key<DataWhole> mWholeParent;
    
    /* The unique Blobkey assigned against this DataPiece */
    private BlobKey mBlobKey;

    /* OBJECTIFY REQUIRES A NO-ARG CONSTRUCTOR */
    public DataPiece() {
        
    }
    
    public DataPiece(Key<DataWhole> wholeParent, DataPiecePartial dpPartial) {
        this.mPieceNo = dpPartial.getPieceNo();
        this.mKey = dpPartial.getKey();
        this.mHash = dpPartial.getHash();
        this.mWholeParent = wholeParent;
    }

    public String getKey() {
        return mKey;
    }

    public int getPieceNo() {
        return mPieceNo;
    }

    public String getHash() {
        return mHash;
    }

    public void setHash(String hash) {
        mHash = hash;
    }

    public void setPieceNo(int pPieceNo) {
        this.mPieceNo = pPieceNo;
    }

    public Key<DataWhole> getWholeParent() {
        return mWholeParent;
    }

    public void setWholeParent(Key<DataWhole> pWholeParent) {
        this.mWholeParent = pWholeParent;
    }

    public BlobKey getBlobKey() {
        return mBlobKey;
    }

    public void setBlobKey(BlobKey blobKey) {
        this.mBlobKey = blobKey;
    }

    public DataPiecePartial toPartial(DataWholePartial dataWholePartial) {
        DataPiecePartial partial = new DataPiecePartial(mKey, mHash, mPieceNo, false, dataWholePartial);
        if (mKey != null) {
            partial.setKey(mKey);
        }
        return partial;
    }
}
