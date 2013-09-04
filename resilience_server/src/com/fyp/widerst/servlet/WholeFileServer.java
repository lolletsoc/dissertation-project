package com.fyp.widerst.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fyp.widerst.Constants;
import com.fyp.widerst.entity.DataWhole;
import com.fyp.widerst.util.DbHelper;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

@SuppressWarnings("serial")
public class WholeFileServer extends HttpServlet {

    private final BlobstoreService blobStoreService = BlobstoreServiceFactory.getBlobstoreService();
    private final BlobInfoFactory blobInfoFactory = new BlobInfoFactory();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        String dwKey = req.getParameter(Constants.DATAWHOLE_KEY_PARAM);
        
        if (null == dwKey) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        DataWhole dataWhole = DbHelper.findDataWholeByKey(req.getParameter(Constants.DATAWHOLE_KEY_PARAM));
        
        /* If a DataWhole couldn't be found then reply with a BAD_REQUEST status */
        if (null == dataWhole) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        final BlobKey blobKey = dataWhole.getBlobKey();
        
        /* If the DataWhole does not have a BlobKey then reply with a NO_CONTENT status */
        if (null == blobKey) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        
        final BlobInfo blobInfo = blobInfoFactory.loadBlobInfo(blobKey);
        resp.addHeader("Content-Length", Long.toString(blobInfo.getSize()));
        
        /* Server the Blob to the client */
        blobStoreService.serve(blobKey, resp);
    }
    
}
