
package com.fyp.widerst.handler;

import static com.fyp.widerst.WiderstObjectifyService.ofy;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fyp.widerst.Constants;
import com.fyp.widerst.entity.DataPiece;
import com.fyp.widerst.entity.DataWhole;
import com.fyp.widerst.util.DbHelper;
import com.google.appengine.api.backends.BackendServiceFactory;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;

@SuppressWarnings("serial")
public class BlobstoreUploadHandler extends HttpServlet {

    private static final Logger log = Logger.getLogger(BlobstoreUploadHandler.class.getName());
    private final BlobstoreService blobStore = BlobstoreServiceFactory.getBlobstoreService();
    private final BlobInfoFactory blobInfoFactory = new BlobInfoFactory();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException,
            IOException {
        Map<String, List<BlobKey>> blobs = blobStore.getUploads(req);
        log.log(Level.INFO, "This many Blobs were found in the POST " + blobs.size());

        final String dwKeyParam = req.getParameter("dataWholeId");
        final String dpKeyParam = req.getParameter("dataPieceId");

        final BlobKey blobKey = blobs.values().iterator().next().get(0);
        final BlobInfo blobInfo = blobInfoFactory.loadBlobInfo(blobKey);

        Integer blobTransaction = ofy().transact(new Work<Integer>() {

            @Override
            public Integer run() {

                final DataWhole dataWhole = DbHelper.findDataWholeByKey(dwKeyParam);
                final DataPiece dataPiece = DbHelper.findDataPieceByKeyAndParent(dpKeyParam, dataWhole);

                if (null == dataPiece) {
                    return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                } else {

                    if (dataPiece.getHash().contentEquals(blobInfo.getMd5Hash())) {
                        /* Ensure the hash values match */
                        dataPiece.setBlobKey(blobKey);
                        Boolean dpTransaction = ofy().transact(new Work<Boolean>() {
                            @Override
                            public Boolean run() {
                                return ofy().save().entity(dataPiece).now() != null ? true : false;
                            }
                        });

                        if (dpTransaction) {
                            /*
                             * Check if this was the last piece to be added. If
                             * so, then we must send a request to the Backend
                             * server to perform a re-structure.
                             */

                            int blobCount = 0;
                            final Map<Key<DataPiece>, DataPiece> pieces = ofy().load().keys(dataWhole.getDataPieceKeyList());
                            
                            if (pieces.size() == dataWhole.getNumOfPieces()) {
                                for (DataPiece blobbedDatatPiece : pieces.values()) {
                                    if (null != blobbedDatatPiece.getBlobKey()) {
                                        blobCount++;
                                    }
                                }
                            }

                            if (blobCount == dataWhole.getNumOfPieces()) {
                                log.log(Level.INFO, "Transaction complete. Server has total pieces");
                                Queue fileJoinerQueue = QueueFactory.getQueue("fileJoinQueue");
                                fileJoinerQueue.add(createBackendTask(dataWhole));
                            }

                            return HttpServletResponse.SC_OK;

                        } else {
                            log.log(Level.SEVERE, "Transaction failed!");
                        }

                    } else {
                        /*
                         * If the MD5 Hashes do not match then the client must
                         * re-upload
                         */
                        return HttpServletResponse.SC_CONFLICT;

                    }
                    return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                }
            }
        });

        resp.setStatus(blobTransaction);
    }

    private TaskOptions createBackendTask(DataWhole dataWhole) {
        /* Create a TaskOptions object which can then be posted to a back end */

        TaskOptions taskOptions = null;
        taskOptions = TaskOptions.Builder
                .withUrl("/joinFile")
                .param(Constants.DATAWHOLE_KEY_PARAM, dataWhole.getKey())
                .header("Host", BackendServiceFactory.getBackendService().getBackendAddress("filejoin"))
                .method(TaskOptions.Method.POST);

        return taskOptions;

    }
}
