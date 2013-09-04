
package com.fyp.resilience.view;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fyp.resilience.R;
import com.fyp.resilience.ResilienceController;
import com.fyp.resilience.connection.Connectable;
import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.fyp.resilience.event.ConnectionsModified;

import de.greenrobot.event.EventBus;

public class FileView extends RelativeLayout {

    private DataWhole mDataWhole;

    public FileView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private TextView getFileName() {
        return (TextView) findViewById(R.id.file_view_item_name);
    }

    private TextView getFileSize() {
        return (TextView) findViewById(R.id.file_view_item_size);
    }

    private TextView getFilePieces() {
        return (TextView) findViewById(R.id.file_view_item_pieces);
    }

    private TextView getFileConnections() {
        return (TextView) findViewById(R.id.file_view_item_connections);
    }

    private PieceProgressIndicator getPieceIndicator() {
        return (PieceProgressIndicator) findViewById(R.id.progress_view_pieces);
    }

    private TextView getWholeStatus() {
        return (TextView) findViewById(R.id.file_view_whole_status);
    }

    public DataWhole getDataWhole() {
        return mDataWhole;
    }

    public void setDataWhole(DataWhole dataWhole) {

        mDataWhole = dataWhole;

        /*  */
        File wholeFile = mDataWhole.getFile(getContext());

        if (null != wholeFile) {
            getFileName().setText(wholeFile.getName());
            getFileSize().setText(
                    String.format("%.2f", (wholeFile.length() / (float) 1024 / 1024)) + "MB");
            getFilePieces().setText(mDataWhole.getNoPieces() + "/" + mDataWhole.getNoPieces() + " pieces of data");
        } else {
            getFileName().setText("Key: " + dataWhole.getKey());
            getFileSize().setText("UNKNOWN");
            if (mDataWhole.isAvailable()) {
                getFilePieces().setText("NONE");
            } else {
                List<DataPiece> pieces = mDataWhole.getPieces();
                getFilePieces().setText((pieces != null ? pieces.size() : 0) + "/" + mDataWhole.getNoPieces() + " pieces of data");
            }
        }

        List<Connectable> connections = ResilienceController.getInstance(getContext())
                .getConnectionList();

        int count = 0;
        for (Connectable connectable : connections) {
            if (connectable.getDataWhole() == mDataWhole) {
                count++;
            }
        }
        getFileConnections().setText(count + " connections active");

        if (null != mDataWhole.getUriString() && !mDataWhole.getUriString().equals("")) {
            getWholeStatus().setText("AVAILABLE ON DEVICE");
        
        } else if (mDataWhole.getState() == DataWhole.STATE_DOWNLOADING) {
            getWholeStatus().setText("DOWNLOADING");
            
        } else if (mDataWhole.isAvailable()) {
            getWholeStatus().setText("AVAILABLE ON SERVER");

        } else {
            getWholeStatus().setText("NOT AVAILABLE");

        }

        getPieceIndicator().setDataWhole(mDataWhole);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this, ConnectionsModified.class);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(ConnectionsModified event) {
        if (null != event.getDataWhole() && event.getDataWhole() == mDataWhole) {
            setDataWhole(event.getDataWhole());
            requestLayout();
        }
    }
}
