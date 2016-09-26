package com.flopcode.getpix;

public interface Logging {
  void error(String tag, String msg, Exception e);
  void debug(String tag, String msg);
}
