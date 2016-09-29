package com.flopcode.getpix;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.support.design.widget.Snackbar.LENGTH_LONG;
import static android.text.TextUtils.join;

public class MainActivity extends AppCompatActivity {

  private static final int MY_REQUEST_PERMISSON_CODE = 42;
  public static final String LOG_TAG = "GetPix";


  private class GetNetworkAddresses extends AsyncTask<Void, Void, String> {
    private final TextView ips;

    public GetNetworkAddresses(TextView ips) {
      this.ips = ips;
    }

    @Override
    protected String doInBackground(Void... voids) {
      return join("\n", getNetworkAddresses());
    }

    @Override
    protected void onPostExecute(String s) {
      ips.setText(s + "\n\nREST-API:\n" + endpointsToString());
    }

  }

  public static List<String> getNetworkAddresses() {
    List<String> res = new ArrayList<>();
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface networkInterface = interfaces.nextElement();
        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress inetAddress = addresses.nextElement();
          if (!inetAddress.isLoopbackAddress()) {
            res.add(networkInterface.getDisplayName() + ": " + inetAddress.getHostName() + ", " + inetAddress.getHostAddress());
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return res;
  }

  private String endpointsToString() {
    return "GET " + GetPixService.INDEX + " - getting json of all files available\n"
      + "GET " + GetPixService.FILES + "{} - getting the raw data of one file\n"
      + "POST filename={} suffix={} " + GetPixService.FILES + " - marking one file as done\n";
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    new GetNetworkAddresses((TextView) findViewById(R.id.ips)).execute();

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        final Intent intent = getPixServiceIntent(MainActivity.this);
        intent.setAction("delete");
        Snackbar.make(view, "Reset copy information data", LENGTH_LONG)
          .setAction("Reset", new OnClickListener() {
            @Override
            public void onClick(View view) {
              startService(intent);
            }
          })
          .show();
      }
    });
    requestPermissions();
  }

  @OnClick(R.id.start)
  public void onStart(Button b) {
    Log.e(LOG_TAG, "onStart");
    startService(getPixServiceIntent(this));
  }

  public static Intent getPixServiceIntent(Context c) {
    return new Intent(c, GetPixService.class);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  private void requestPermissions() {
    int readRes = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);
    int writeRes = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
    if ((readRes != PERMISSION_GRANTED) || (writeRes != PERMISSION_GRANTED)) {
      ActivityCompat.requestPermissions(this, new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, MY_REQUEST_PERMISSON_CODE);
    } else {
      showPictureFolder();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == MY_REQUEST_PERMISSON_CODE) {
      Log.e(LOG_TAG, permissions.toString());

      showPictureFolder();
    }
  }

  private void showPictureFolder() {
  }


}
