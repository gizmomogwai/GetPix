package com.flopcode.getpix;

import java.io.Serializable;

public class Transferred implements Serializable, Comparable<Transferred> {
  public final String filename;
  public final String to;

  public Transferred(String filename, String to) {
    this.filename = filename;
    this.to = to;
  }

  public String virtualFilename() {
    return filename + "." + to;
  }

  @Override
  public int compareTo(Transferred o) {
    return virtualFilename().compareTo(o.virtualFilename());
  }
}
