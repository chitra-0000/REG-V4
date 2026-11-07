package com.bnpp.regliss.config.thread;

// [MISSING FROM SCREENSHOTS: lines 3-6 of the original file were not visible in the photos]

public class ThreadScopeListener implements ServletRequestListener {

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        ThreadScopeContextHolder.currentThreadScopeAttributes().clear();
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        //this method is empty because the only functionality we need is the requestDestroyed implementation
    }

 }

