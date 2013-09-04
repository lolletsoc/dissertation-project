package com.fyp.resilience.thread;

import java.util.concurrent.ThreadFactory;

/**
 *
 */
public class ResilienceThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(final Runnable r) {
        return new Thread(r, "resil_thread");
    }

}
