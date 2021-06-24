package com.flopcode.getpix;

/**
 * Created by gizmo on 25/09/16.
 */
class NoopLogging implements Logging {
  @Override
  public void error(String tag, String msg, Exception e) {
  }

  @Override
  public void info(String logTag, String s, Exception e) {
  }

  @Override
  public void debug(String tag, String msg) {
  }
}
