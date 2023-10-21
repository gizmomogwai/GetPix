package com.flopcode.getpix;

import android.util.Log;

class LogcatLogging implements Logging {
    @Override
    public void error(String tag, String msg, Exception e) {
        Log.e(tag, msg, e);
    }

    @Override
    public void info(String tag, String msg, Exception e) {
        Log.i(tag, msg, e);
    }

    @Override
    public void debug(String tag, String msg) {
        Log.d(tag, msg);
    }
}
