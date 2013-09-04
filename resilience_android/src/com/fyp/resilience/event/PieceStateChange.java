package com.fyp.resilience.event;

import com.fyp.resilience.database.model.DataPiece;

public final class PieceStateChange {
    
    private final DataPiece mDataPiece;
    
    public PieceStateChange(final DataPiece dataPiece) {
        mDataPiece = dataPiece;
    }
    
    public DataPiece getDataPiece() {
        return mDataPiece;
    }
    
}
