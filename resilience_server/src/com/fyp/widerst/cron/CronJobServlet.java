
package com.fyp.widerst.cron;

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

import com.fyp.widerst.entity.DataPiece;
import com.fyp.widerst.entity.DataWhole;
import com.fyp.widerst.util.GcmHelper;
import com.googlecode.objectify.Key;

@SuppressWarnings("serial")
public class CronJobServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(GcmHelper.class.getSimpleName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        try {

        List<DataWhole> wholes = ofy().load().type(DataWhole.class).list();

        for (DataWhole dataWhole : wholes) {
            //TODO: Add time check
            /* Only check those that have enough pieces */
            if (dataWhole.getDataPieceKeyList().size() > 0
                    && (dataWhole.getDataPieceKeyList().size() == dataWhole.getNumOfPieces())) {
                Map<Key<DataPiece>, DataPiece> pieces = ofy().load().keys(dataWhole.getDataPieceKeyList());
                for (DataPiece dataPiece : pieces.values()) {
                    if (dataPiece.getBlobKey() == null || dataPiece.getBlobKey().equals("")) {
                        logger.log(Level.INFO, "Sending request for Piece " + dataPiece.getKey());
                        GcmHelper.sendPieceRequest(dataPiece, dataWhole);
                    }
                }
            }
        }
        
        resp.setStatus(HttpServletResponse.SC_OK);
        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
