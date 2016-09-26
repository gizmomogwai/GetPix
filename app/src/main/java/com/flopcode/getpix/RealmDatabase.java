package com.flopcode.getpix;

/*
import io.realm.Realm;
import io.realm.Realm.Transaction;
import io.realm.RealmConfiguration;
import io.realm.RealmQuery;

import java.util.Iterator;

public class RealmDatabase implements Database {
  private final Realm realm;

  public RealmDatabase(Context c) {
    RealmConfiguration realmConfig = new RealmConfiguration.Builder(c).build();
    Realm.setDefaultConfiguration(realmConfig);
    realm = Realm.getDefaultInstance();
  }

  @Override
  public void add(final Transferred t) {
    realm.executeTransaction(new Transaction() {
      @Override
      public void execute(Realm realm) {
        RealmTransferred transferred = realm.createObject(RealmTransferred.class);
        transferred.filename = t.filename;
        transferred.to = t.to;
      }
    });
  }

  @Override
  public Iterator<Transferred> getAll() {
    RealmQuery<RealmTransferred> h = realm.where(RealmTransferred.class);
    final Iterator<RealmTransferred> i = h.findAll().iterator();
    return new Iterator<Transferred>() {
      @Override
      public boolean hasNext() {
        return i.hasNext();
      }

      @Override
      public Transferred next() {
        final RealmTransferred next = i.next();
        return new Transferred(next.filename, next.to);
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
  public void deleteAll() {
    realm.executeTransaction(new Transaction() {
      @Override
      public void execute(Realm realm) {
        realm.deleteAll();
      }
    });
  }


  @Override
  public void close() {
    realm.close();
  }
}
*/
