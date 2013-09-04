
package com.fyp.resilience.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.fyp.resilience.R;
import com.fyp.resilience.event.ClientModified;
import com.fyp.resilience.swarm.model.SwarmClient;

import de.greenrobot.event.EventBus;

public class ClientView extends RelativeLayout {

    private SwarmClient mClient;

    public ClientView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private TextView getClientAddress() {
        return (TextView) findViewById(R.id.client_view_client_address);
    }

    private TextView getClientSuccess() {
        return (TextView) findViewById(R.id.client_view_successes);
    }

    private TextView getClientFailures() {
        return (TextView) findViewById(R.id.client_view_failures);
    }

    public void setClient(SwarmClient client) {
        mClient = client;
        
        getClientAddress().setText(mClient.getAddress().getHostAddress() + ":" + mClient.getPort());
        getClientSuccess().setText("Success: " + mClient.getSuccessfulAttempts() + "");
        getClientFailures().setText("Failure: " + mClient.getFailedAttempts() + "");
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this, ClientModified.class);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }
    
    public void onEventMainThread(ClientModified event) {
        if (event.getClient() == mClient) {
            setClient(mClient);
            invalidate();
        }
    }
}
