package com.flopcode.getpix;

import java.util.Iterator;

public interface Database {
    void close();

    void deleteAll();

    void add(Transferred t);

    Iterator<Transferred> getAll();

    String toJson();
}
