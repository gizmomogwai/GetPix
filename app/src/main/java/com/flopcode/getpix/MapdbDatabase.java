package com.flopcode.getpix;

/*
public class MapdbDatabase implements Database {
  private final File filesDir;
  private DB mapDb;
  private KeySet<Transferred> map;

  public MapdbDatabase(File filesDir) {
    this.filesDir = filesDir;
    createDb();
  }

  private void createDb() {
    mapDb = DBMaker.fileDB(getMapdbFile()).transactionEnable().make();
    map = mapDb.hashSet("entries", new Serializer<Transferred>() {
      @Override
      public void serialize(@NotNull DataOutput2 out, @NotNull Transferred value) throws IOException {
        out.writeUTF(value.filename);
        out.writeUTF(value.to);
      }

      @Override
      public Transferred deserialize(@NotNull DataInput2 input, int available) throws IOException {
        return new Transferred(input.readUTF(), input.readUTF());
      }

      @Override
      public int compare(Transferred first, Transferred second) {
        return first.virtualFilename().compareTo(second.virtualFilename());
      }

    }).create();
  }

  private File getMapdbFile() {
    return new File(filesDir, "transferred.mapdb");
  }

  @Override
  public void close() {
    mapDb.close();
  }

  @Override
  public void deleteAll() {
    mapDb.close();
    getMapdbFile().delete();
    createDb();
  }

  @Override
  public void add(String filename, String suffix) {
    map.add(new Transferred(filename, suffix));
    mapDb.commit();
  }

  @Override
  public Iterator<Transferred> getAll() {
    return map.iterator();
  }
}
*/
