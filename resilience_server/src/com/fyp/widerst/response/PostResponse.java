
package com.fyp.widerst.response;

public class PostResponse {

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_BUSY = 1;
    public static final int STATUS_FAILED = 2;
    public static final int STATUS_NOT_REQUIRED = 3;
    public static final int STATUS_REGISTRATION_ERROR = 4;
    public static final int STATUS_WHOLE_COMPLETE = 5;

    private int mStatus;
    private String mBlobstoreUrl;

    /* Used for responses that have failed */
    public PostResponse(int pSuccess) {
        mStatus = pSuccess;
    }

    public PostResponse(String pBlobstoreUrl, int pSuccess) {
        mBlobstoreUrl = pBlobstoreUrl;
        mStatus = pSuccess;
    }

    public int getSuccess() {
        return mStatus;
    }

    public String getPostUrl() {
        return mBlobstoreUrl;
    }
}
