package com.flopcode.getpix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class StupidDatabase implements com.flopcode.getpix.Database {
  private final Thread thread;
  private final File file;
  private final Logging logging;
  private final String logTag;
  private TreeSet<Transferred> transferred;

  public StupidDatabase(Logging logging, String logTag, File file) {
    this.logging = logging;
    this.logTag = logTag;
    this.thread = Thread.currentThread();
    this.file = file;
    read();
  }

  private void read() {
    assertThread();
    try {
      transferred = (TreeSet<Transferred>) new ObjectInputStream(new GZIPInputStream(new FileInputStream(file))).readObject();
    } catch (Exception e) {
      logging.error(logTag, "StupidDatabase could not read data", e);
      transferred = new TreeSet<>();
    }
  }

  private void assertThread() {
    if (thread != Thread.currentThread()) {
      throw new RuntimeException("called read not from correct thread. expected " + thread + ", got " + Thread.currentThread());
    }
  }

  @Override
  public void close() {
    assertThread();
    try {
      final ObjectOutputStream objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
      objectOutputStream.writeObject(transferred);
      objectOutputStream.close();
      logging.debug(logTag, "Size of db file: " + file.length());
    } catch (IOException e) {
      logging.error(logTag, "Stupid Database could not write data", e);
    }
  }

  @Override
  public void deleteAll() {
    assertThread();
    file.delete();
    read();
  }

  @Override
  public void add(Transferred t) {
    assertThread();
    transferred.add(t);
  }

  @Override
  public Iterator<Transferred> getAll() {
    assertThread();
    return transferred.iterator();
  }

  @Override
  public String toJson() {
    Iterator<Transferred> i = getAll();
    StringBuilder res = new StringBuilder();
    res.append("[");
    while (i.hasNext()) {
      Transferred next = i.next();
      res.append("{\"filename\"=\"" + next.filename + "\",\"to\"=\"" + next.to + "\"}");
      if (i.hasNext()) {
        res.append(",");
      }
    }
    res.append("]");
    return res.toString();
  }
}
