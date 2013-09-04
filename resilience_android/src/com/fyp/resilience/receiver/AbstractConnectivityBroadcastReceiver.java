package com.fyp.resilience.receiver;

import android.content.BroadcastReceiver;

public abstract class AbstractConnectivityBroadcastReceiver extends BroadcastReceiver {

    public abstract void onDestroy();
    
}