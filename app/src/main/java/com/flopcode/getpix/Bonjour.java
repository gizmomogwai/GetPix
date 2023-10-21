package com.flopcode.getpix;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import static com.flopcode.getpix.MainActivity.LOG_TAG;

public class Bonjour {
  NsdManager nsd;
  private final RegistrationListener registrationListener;

  public Bonjour(Context applicationContext) {
    nsd = (NsdManager) applicationContext.getSystemService(Context.NSD_SERVICE);
    registrationListener = new RegistrationListener() {
      @Override
      public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
        Log.e(LOG_TAG, "onRegistrationFailed");
      }

      @Override
      public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
        Log.e(LOG_TAG, "onUnregistrationFailed");
      }

      @Override
      public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
        Log.d(LOG_TAG, "onServiceRegistered");
        Log.d(LOG_TAG, "  registered as " + nsdServiceInfo.getServiceName());
      }

      @Override
      public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
        Log.d(LOG_TAG, "onServiceUnregistered");
      }
    };
  }

  public void installService(int port, String name) {
    NsdServiceInfo info = new NsdServiceInfo();
    info.setPort(port);
    info.setServiceName(name);
    info.setServiceType("_getpix._tcp");
    nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener);
  }


  public void removeService() {
    nsd.unregisterService(registrationListener);
  }
}
