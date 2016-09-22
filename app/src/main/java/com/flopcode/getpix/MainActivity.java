package com.flopcode.getpix;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.TextView;
import com.google.common.base.Joiner;
import fi.iki.elonen.NanoHTTPD;
import io.realm.Realm;
import io.realm.Realm.Transaction;
import io.realm.RealmConfiguration;
import io.realm.RealmQuery;
import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static fi.iki.elonen.NanoHTTPD.Method.POST;
import static fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR;
import static fi.iki.elonen.NanoHTTPD.Response.Status.NOT_FOUND;
import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;
import static fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT;

public class MainActivity extends AppCompatActivity {

  private static final int MY_REQUEST_PERMISSON_CODE = 42;
  private static final String LOG_TAG = "GetPix";
  public static final String INDEX = "/index";
  public static final String FILES = "/files";
  public static final String JSON_MIME_TYPE = "application/json";
  private FloatingActionButton fab;
  private Realm realm;
  private Exception exception;
  private TextView ips;

  private class GetNetworkAddresses extends AsyncTask<Void, Void, String> {

    @Override
    protected String doInBackground(Void... voids) {
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
      return Joiner.on("\n").join(res);
    }

    @Override
    protected void onPostExecute(String s) {
      ips.setText(s);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    RealmConfiguration realmConfig = new RealmConfiguration.Builder(this).build();
    Realm.setDefaultConfiguration(realmConfig);
    realm = Realm.getDefaultInstance();

    setContentView(R.layout.activity_main);
    ips = (TextView) findViewById(R.id.ips);
    new GetNetworkAddresses().execute();

    fab = (FloatingActionButton) findViewById(R.id.fab);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
          .setAction("Action", null).show();
      }
    });

    requestPermissions();
  }

  private void requestPermissions() {
    int readRes = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);
    int writeRes = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
    Log.e(LOG_TAG, "read external: " + readRes);
    Log.e(LOG_TAG, "write external: " + writeRes);
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
    try {
      NanoHTTPD httpServer = new NanoHTTPD(4567) {
        @Override
        public Response serve(IHTTPSession session) {
          try {
            if (session.getMethod() == Method.GET) {
              String path = session.getUri();
              if (path.equals(INDEX)) {
                final List<String> files = collectFiles();
                final DataTransfer<Collection<? extends String>> dt = new DataTransfer<>(1);
                runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    dt.setData(realmFiles());
                  }
                });
                dt.await();
                files.addAll(dt.getData());
                JSONArray array = new JSONArray();
                for (String s : files) {
                  array.put(s);
                }
                return newFixedLengthResponse(OK, JSON_MIME_TYPE, array.toString());
              } else if (path.startsWith(FILES + "/")) {
                String filename = path.substring(FILES.length());
                Log.e(LOG_TAG, "transferring file: " + filename);
                return newFixedLengthResponse(OK, "application/binary", new FileInputStream(filename), new File(filename).length());
              } else {
                newFixedLengthResponse(NOT_FOUND, JSON_MIME_TYPE, "path '" + path + "' not known");
              }
            } else if (session.getMethod() == POST) {

              String path = session.getUri();
              if (path.startsWith(FILES + "/")) {
                Map<String, String> postData = new HashMap<>();
                session.parseBody(postData);
                final String filename = session.getParameters().get("filename").get(0);
                final String suffix = session.getParameters().get("suffix").get(0);
                final DataTransfer<Object> dt = new DataTransfer<>(1);
                runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    try {
                      realm.executeTransaction(new Transaction() {
                        @Override
                        public void execute(Realm realm) {
                          Transferred transferred = realm.createObject(Transferred.class);
                          transferred.filename = filename;
                          transferred.to = suffix;
                        }
                      });
                      dt.setData("OK");
                    } catch (Exception e) {
                      exception = e;
                      dt.exception(e);
                    }
                  }
                });
                dt.await();
                return newFixedLengthResponse(OK, "text", "" + dt.getData());
              }
            }
            return newFixedLengthResponse(NOT_FOUND, "text/json", "could now work with " + session);
          } catch (Exception e) {
            return newFixedLengthResponse(INTERNAL_ERROR, "text", e.getMessage());
          }
        }

        private void copy(InputStream inputStream, ByteArrayOutputStream body) throws Exception {
          int read = inputStream.read();
          while (read != -1) {
            body.write(read);
            read = inputStream.read();
          }
        }
      };
      httpServer.start(SOCKET_READ_TIMEOUT, false);
    } catch (Exception e) {
      Log.e(LOG_TAG, "could not handle request", e);
      // Snackbar.make(fab, "Could not start httpd server: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
    }
    File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    Log.e(LOG_TAG, "pictures directory: " + file);
    File[] h = file.listFiles();
    Log.e(LOG_TAG, "pictures directory files: " + h);
    if (h != null) {
      for (File f : file.listFiles()) {
        Log.e(LOG_TAG, "  " + f);
      }
    }

  }

  private Collection<? extends String> realmFiles() {
    ArrayList<String> res = new ArrayList<>();
    RealmQuery<Transferred> h = realm.where(Transferred.class);
    Iterator<Transferred> i = h.findAll().iterator();
    while (i.hasNext()) {
      Transferred t = i.next();
      Log.e(LOG_TAG, "from realm: " + t.virtualFilename());
      res.add(t.virtualFilename());
    }
    return res;
  }

  private List<String> collectFiles() {
    File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    List<String> res = new ArrayList<>();
    return collect(res, root);
  }

  private List<String> collect(List<String> res, File root) {
    File[] files = root.listFiles();
    for (File f : files) {
      Log.e(LOG_TAG, "absolute: " + f.getAbsolutePath());
      Log.e(LOG_TAG, "name: " + f.getName());
      if (f.isFile()) {
        res.add(f.getAbsolutePath());
      } else {
        collect(res, f);
      }
    }
    return res;
  }
}
