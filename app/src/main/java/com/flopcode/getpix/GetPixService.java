package com.flopcode.getpix;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.flopcode.getpix.MainActivity.LOG_TAG;
import static fi.iki.elonen.NanoHTTPD.Method.POST;
import static fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR;
import static fi.iki.elonen.NanoHTTPD.Response.Status.NOT_FOUND;
import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;
import static fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT;

public class GetPixService extends Service {
  public static final int PORT = 4567;
  public static final String INDEX = "/index";
  public static final String FILES = "/files/";
  public static final String JSON_MIME_TYPE = "application/json";
  private static final int GET_PIX_NOTIFICATION = 17;
  private NanoHTTPD httpServer;
  private HandlerThread serviceThread;
  private Handler handler;
  private Database database;

  private static void copy(InputStream inputStream, ByteArrayOutputStream body) throws Exception {
    int read = inputStream.read();
    while (read != -1) {
      body.write(read);
      read = inputStream.read();
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    Log.e(LOG_TAG, "GetPixService.onCreate");
    super.onCreate();
    serviceThread = new HandlerThread("GetPixServiceThread");
    serviceThread.start();
    handler = new Handler(serviceThread.getLooper());
    startServer();
  }

  @Override
  public void onDestroy() {
    Log.e(LOG_TAG, "GetPixService.onDestroy");
    removeNotificationIcon();

    final DataTransfer<String> dt = new DataTransfer<>(1);
    try {
      handler.post(new Runnable() {
        @Override
        public void run() {
          try {
            httpServer.closeAllConnections();
            database.close();
            serviceThread.quitSafely();
            dt.setData("ok");
          } catch (Exception e) {
            dt.setData("nok");
          }
        }
      });
      dt.await();
    } catch (InterruptedException e) {
      Log.e(LOG_TAG, "got interrupted", e);
      Thread.interrupted();
    }
    super.onDestroy();
  }

  private void removeNotificationIcon() {
    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    nm.cancel(GET_PIX_NOTIFICATION);
  }

  private List<String> collectFiles() {
    File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    List<String> res = new ArrayList<>();
    return collect(res, root);
  }

  private List<String> collect(List<String> res, File root) {
    File[] files = root.listFiles();
    for (File f : files) {
      if (f.isFile()) {
        res.add(f.getAbsolutePath());
      } else {
        collect(res, f);
      }
    }
    return res;
  }

  private Collection<? extends String> databaseFiles() {
    final DataTransfer<ArrayList<String>> dt = new DataTransfer<>(1);
    try {
      handler.post(new Runnable() {
        public void run() {
          ArrayList<String> res = new ArrayList<>();
          Iterator<Transferred> i = database.getAll();
          while (i.hasNext()) {
            Transferred t = i.next();
            Log.d(LOG_TAG, "from database: " + t.virtualFilename());
            res.add(t.virtualFilename());
          }
          dt.setData(res);
        }
      });
      dt.await();
      return dt.getData();
    } catch (InterruptedException e) {
      Log.e(LOG_TAG, "got interrupted");
      Thread.interrupted();
    }
    return new ArrayList<>();
  }

  private void startServer() {
    final DataTransfer<String> dt = new DataTransfer<>(1);
    try {
      handler.post(new Runnable() {
        @Override
        public void run() {
          try {
            database = new StupidDatabase(new LogcatLogging(), LOG_TAG, new File(getFilesDir(), "stupid.java-objects"));
            httpServer = new WebServer();
            httpServer.start(SOCKET_READ_TIMEOUT, false);
            dt.setData("ok");
            installNotificationIcon();
          } catch (Exception e) {
            Log.e(LOG_TAG, "could not handle request", e);
            dt.exception(e);
          }
        }
      });
      dt.await();
    } catch (InterruptedException e) {
      Log.e(LOG_TAG, "got interrupted", e);
      Thread.interrupted();
    }
  }

  private void installNotificationIcon() {
    Intent i = new Intent(this, StopGetPixService.class);
    PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

    final InboxStyle inboxStyle = new InboxStyle();
    inboxStyle.setBigContentTitle("GetPix Server running:");
    inboxStyle.setSummaryText("Press to stop server");
    for (String s : MainActivity.getNetworkAddresses()) {
      inboxStyle.addLine(s);
    }

    NotificationCompat.Builder builder = new NotificationCompat
      .Builder(this)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentIntent(pi)
      .setDeleteIntent(pi)
      .setStyle(inboxStyle);

    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    notificationManager.notify(GET_PIX_NOTIFICATION, builder.build());
  }

  private class WebServer extends NanoHTTPD {
    public WebServer() {
      super(GetPixService.PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {
      try {
        if (session.getMethod() == Method.GET) {
          String path = session.getUri();
          if (path.equals(INDEX)) {
            return getIndex();
          } else if (path.startsWith(FILES)) {
            return getFiles(path);
          } else {
            newFixedLengthResponse(NOT_FOUND, JSON_MIME_TYPE, "path '" + path + "' not known");
          }
        } else if (session.getMethod() == POST) {
          String path = session.getUri();
          if (path.startsWith(FILES)) {
            return postFiles(session);
          }
        }
        return newFixedLengthResponse(NOT_FOUND, "text/json", "could not work with " + session);
      } catch (Exception e) {
        Log.e(LOG_TAG, e.getMessage(), e);
        return newFixedLengthResponse(INTERNAL_ERROR, "text", e.getMessage());
      }
    }

    private Response postFiles(IHTTPSession session) throws IOException, ResponseException {
      Map<String, String> postData = new HashMap<>();
      session.parseBody(postData);
      final String filename = session.getParameters().get("filename").get(0);
      final String suffix = session.getParameters().get("suffix").get(0);
      handler.post(new Runnable() {
        @Override
        public void run() {
          database.add(new Transferred(filename, suffix));
        }
      });
      return newFixedLengthResponse(OK, "text", "OK");
    }

    private Response getFiles(String path) throws FileNotFoundException {
      String filename = path.substring(FILES.length());
      Log.d(LOG_TAG, FILES + filename);
      return newFixedLengthResponse(OK, "application/binary", new FileInputStream(filename), new File(filename).length());
    }

    private Response getIndex() {
      Log.d(LOG_TAG, INDEX);
      final List<String> files = collectFiles();
      files.addAll(databaseFiles());
      JSONArray array = new JSONArray();
      for (String s : files) {
        array.put(s);
      }
      return newFixedLengthResponse(OK, JSON_MIME_TYPE, array.toString());
    }
  }
}
