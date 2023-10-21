package com.flopcode.getpix;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class StopGetPixService extends Activity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    stopService(MainActivity.getPixServiceIntent(this));
    finish();
  }
}
