package com.fyp.resilience.thread;

public abstract class ResilienceRunnable implements Runnable {

    @Override
    public abstract void run();
    
    protected boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }
    
}
