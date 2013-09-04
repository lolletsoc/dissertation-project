
package com.fyp.resilience.view;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.PieceStateChange;

import de.greenrobot.event.EventBus;

public class PieceProgressIndicator extends View {

    /* Android colours for rectangle painting */
    private final int GREEN = getResources().getColor(android.R.color.holo_green_light);
    private final int YELLOW = getResources().getColor(android.R.color.holo_orange_light);
    private final int RED = getResources().getColor(android.R.color.holo_red_light);

    private List<DataPiece> mPieceList;
    private DataWhole mDataWhole;

    private Paint mPaint;

    public PieceProgressIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
    }

    @Override
    protected void onAttachedToWindow() {
        EventBus.getDefault().register(this, PieceStateChange.class);
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        EventBus.getDefault().unregister(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (null != mDataWhole) {

            /*
             * No need to calculate rectangles due to the device already being
             * present on the server
             */
            if (mDataWhole.isAvailable()
                    || (mDataWhole.isAvailable() && mDataWhole.getUriString() != null && !mDataWhole.getUriString().equals(""))) {
                mPaint.setColor(GREEN);
                canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mPaint);
            }
            else if (null != mPieceList) {

                /* Calculate the width of each piece rectangle */
                float pieceWidth = (float) canvas.getWidth() / mPieceList.size();

                /* Changes based on the predefined piece width */
                float leftX = 0;

                for (DataPiece dataPiece : mPieceList) {

                    int state = dataPiece.getState();
                    switch (state) {
                        case DataPiece.STATE_UPLOADED_TO_SERVER:
                            mPaint.setColor(GREEN);
                            break;

                        case DataPiece.STATE_UPLOADED_TO_DEVICE:
                            mPaint.setColor(YELLOW);
                            break;

                        default:
                            mPaint.setColor(RED);
                            break;
                    }

                    canvas.drawRect(leftX, 0, leftX + pieceWidth, canvas.getHeight(), mPaint);
                    leftX += pieceWidth;
                }
            }
        }
    }

    /**
     * Sets the DataWhole and its pieces against this view
     * 
     * @param {@link DataWhole} that this view represents
     */
    public void setDataWhole(DataWhole dataWhole) {
        mDataWhole = dataWhole;
        mPieceList = dataWhole.getPieces();
    }

    /**
     * Is called by the EventBus when a PieceStateChange event is fired.
     * 
     * @param {@link PieceStateChange}
     */
    public void onEventMainThread(PieceStateChange event) {
        if (event.getDataPiece().getParent() == mDataWhole) {
            invalidate();
        }
    }
}
