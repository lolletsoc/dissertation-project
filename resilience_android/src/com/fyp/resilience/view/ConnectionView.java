
package com.fyp.resilience.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fyp.resilience.R;
import com.fyp.resilience.connection.Connectable;
import com.fyp.resilience.event.ConnectionProgressChange;
import com.fyp.resilience.event.ConnectionStateChange;

import de.greenrobot.event.EventBus;

public class ConnectionView extends RelativeLayout {

    private Connectable mConnectable;

    public ConnectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private TextView getConnectionDescription() {
        return (TextView) findViewById(R.id.connection_view_item_type);
    }

    private ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.connection_view_item_progress);
    }

    private TextView getConnectionState() {
        return (TextView) findViewById(R.id.connection_view_item_state);
    }

    public void setConnectable(Connectable connectable) {

        mConnectable = connectable;
        getProgressBar().setProgress(connectable.getProgress());
        switch (mConnectable.getConnectionType()) {
            case Connectable.CONNECTION_TYPE_SERVER_UPLOAD:
                getConnectionDescription().setText("Uploading to Server");
                break;
                
            case Connectable.CONNECTION_TYPE_SERVER_DOWNLOAD:
                getConnectionDescription().setText("Downloading from Server");
                break;
                
            case Connectable.CONNECTION_TYPE_WIFI_UPLOAD:
                getConnectionDescription().setText("Wi-Fi Upload");
                break;

            case Connectable.CONNECTION_TYPE_WIFI_DOWNLOAD:
                getConnectionDescription().setText("Wi-Fi Download");
                break;
        }

        switch (mConnectable.getConnectionStatus()) {
            case Connectable.STATUS_IN_PROGRESS:
                getConnectionState().setText("Working...");
                break;

            case Connectable.STATUS_WAITING:
                getConnectionState().setText("Waiting...");
                break;

            case Connectable.STATUS_HASHING:
                getConnectionState().setText("Hashing...");
                break;

            case Connectable.STATUS_BACKING_OFF:
                getConnectionState().setText("Backing off...");
                break;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this, ConnectionProgressChange.class, ConnectionStateChange.class);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(ConnectionProgressChange event) {
        if (event.getConnectable() == mConnectable) {
            getProgressBar().setProgress(event.getConnectable().getProgress());
            requestLayout();
        }
    }

    public void onEventMainThread(ConnectionStateChange event) {
        if (event.getConnectable() == mConnectable) {
            setConnectable(event.getConnectable());
            requestLayout();
        }
    }
}
