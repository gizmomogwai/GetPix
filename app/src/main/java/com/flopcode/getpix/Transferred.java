package com.flopcode.getpix;

import io.realm.RealmObject;

public class Transferred extends RealmObject {
    public String filename;
    public String to;

    public String virtualFilename() {
        return filename + "." + to;
    }
}
