package com.flopcode.getpix;

import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class StupidDatabaseTest {
  @Test
  public void testDatabaseCreatesFile() {
    // when
    final File file = new File("./test.db");
    file.delete();
    Database db = new StupidDatabase(new NoopLogging(), "", file);
    db.close();

    // then
    assertThat(file.exists()).isTrue();
  }

  @Test
  public void testDatabaseGrowsGetsSmallerAgain() {
    // when
    final File file = new File("./test3.db");
    file.delete();
    Database db = new StupidDatabase(new NoopLogging(), "", file);
    db.close();
    long emptySize = file.length();

    // and when
    db = new StupidDatabase(new NoopLogging(), "", file);
    db.add(new Transferred("filename", "forme"));
    db.close();
    long fullSize = file.length();

    // and when
    db = new StupidDatabase(new NoopLogging(), "", file);
    db.deleteAll();
    db.close();
    long newEmptySize = file.length();

    // then
    assertThat(fullSize).isGreaterThan(emptySize);
    assertThat(newEmptySize).isEqualTo(emptySize);
  }

  @Test
  public void freeData() {
    final File file = new File("./test4.db");
    file.delete();
    Database db = new StupidDatabase(new NoopLogging(), "", file);
    db.add(new Transferred("filename1", "forme"));
    db.add(new Transferred("filename2", "forme"));
    String json = db.toJson();
    db.close();
    assertThat(json).isEqualTo("[{\"filename\"=\"filename1\",\"to\"=\"forme\"},{\"filename\"=\"filename2\",\"to\"=\"forme\"}]");
  }

  @Test
  public void checkSize() {
    final File file = new File("./test4.db");
    file.delete();
    Database db = new StupidDatabase(new NoopLogging(), "", file);
    for (int i=0; i<100000; ++i) {
      db.add(new Transferred("filename" + i, "forme"));
    }
    db.close();
    assertThat(file.length()).isEqualTo(267241);
  }
}
