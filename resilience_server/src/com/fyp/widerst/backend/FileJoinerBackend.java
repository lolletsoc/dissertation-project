
package com.fyp.widerst.backend;

import static com.fyp.widerst.WiderstObjectifyService.ofy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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
import com.fyp.widerst.util.GcmHelper;
import com.google.appengine.api.LifecycleManager;
import com.google.appengine.api.LifecycleManager.ShutdownHook;
import com.google.appengine.api.blobstore.BlobstoreInputStream;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;

@SuppressWarnings("serial")
public class FileJoinerBackend extends HttpServlet {

    private static final Logger logger = Logger.getLogger(FileJoinerBackend.class.getName());

    /* Requires Instance-level access due to Transaction inner class */
    private DataWhole dataWhole = null;
    private Map<Key<DataPiece>, DataPiece> dataPieces;

    @Override
    /**
     * 
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        setLifecycleManager();

        /* Query the DataWhole that requires re-structuring */
        final String dwKeyString = req.getParameter(Constants.DATAWHOLE_KEY_PARAM);

        if (null == dwKeyString) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.log(Level.WARNING, "DataWhole Key is NULL!");
            return;
        }

        logger.log(Level.WARNING, "DataWhole Key is " + dwKeyString);

        Boolean transaction = ofy().transact(new Work<Boolean>() {
            @Override
            public Boolean run() {
                dataWhole = ofy().load().type(DataWhole.class).id(dwKeyString).get();
                if (null != dataWhole) {
                    if (null == dataWhole.getBlobKey()) {
                        if (dataWhole.getDataPieceKeyList().size() == dataWhole.getNumOfPieces()) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
                logger.log(Level.SEVERE, "DataWhole NOT found!");
                return false;
            }
        });

        /* Check if an interrupt has been called */
        if (Thread.interrupted()) {
            return;
        }

        if (transaction) {
            if (structureFileAndPostToDb()) {
                GcmHelper.sendWholeCompletionNotification(dataWhole);
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * @return
     * @throws IOException
     */
    private boolean structureFileAndPostToDb() throws IOException {

        /* Define the File API specific instances */
        BlobstoreService blobStoreService = BlobstoreServiceFactory.getBlobstoreService();
        FileService fileService = FileServiceFactory.getFileService();

        /* */
        final AppEngineFile blobFile = fileService.createNewBlobFile(dataWhole.getMimeType(), dataWhole.getFileName());

        /* */
        final FileWriteChannel writeChannel = fileService.openWriteChannel(blobFile, true);

        /* */
        Boolean transaction = ofy().transact(new Work<Boolean>() {
            @Override
            public Boolean run() {
                dataPieces = ofy().load().keys(dataWhole.getDataPieceKeyList());
                return dataPieces.size() > 0 ? true : false;
            }
        });

        if (transaction) {

            int length;
            byte[] buffer = new byte[1024000];

            for (int i = 1; i <= dataPieces.size(); i++) {

                Key<DataPiece> pieceKey = Key.create(Key.create(DataWhole.class, dataWhole.getKey()), DataPiece.class,
                        dataWhole.getKey() + i);

                DataPiece dataPiece = dataPieces.get(pieceKey);

                logger.log(Level.INFO, "DataPiece Blobkey is " + dataPiece.getBlobKey());
                BlobstoreInputStream blobStream = new BlobstoreInputStream(dataPiece.getBlobKey());

                while ((length = blobStream.read(buffer)) != -1) {
                    writeChannel.write(ByteBuffer.wrap(buffer, 0, length));
                }

                blobStream.close();
                blobStoreService.delete(dataPiece.getBlobKey());
            }

            writeChannel.closeFinally();

            dataWhole.setBlobKey(fileService.getBlobKey(blobFile));

            logger.log(Level.INFO, "New Blobkey is " + fileService.getBlobKey(blobFile));
            
            /* Remove the pieces once it has been successfully restructured */
            for (DataPiece piece : dataPieces.values()) {
                blobStoreService.delete(piece.getBlobKey());
            }

            Boolean dwTransaction = ofy().transact(new Work<Boolean>() {
                @Override
                public Boolean run() {
                    /* Persist the modified DataWhole */
                    dataWhole.setDataPieceKeyList(new ArrayList<Key<DataPiece>>());
                    ofy().save().entity(dataWhole).now();

                    /* Remove the DataWhole's children */
                    ofy().delete().entities(dataPieces.values()).now();

                    return true;
                }
            });

            return dwTransaction;
        }

        return false;

    }

    /**
     * 
     */
    private void setLifecycleManager() {
        /* Specify the lifecycle manager to be used */
        LifecycleManager.getInstance().setShutdownHook(new ShutdownHook() {
            @Override
            public void shutdown() {
                /*
                 * Calls interrupt on all threads within this instance.
                 * Therefore, periodic checks must be make to ensure this
                 * instance hasn't been told to shutdown.
                 */
                LifecycleManager.getInstance().interruptAllRequests();
            }
        });
    }
}
