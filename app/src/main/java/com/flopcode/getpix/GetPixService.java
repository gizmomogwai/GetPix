package com.flopcode.getpix;

import static com.flopcode.getpix.MainActivity.LOG_TAG;
import static fi.iki.elonen.NanoHTTPD.Method.POST;
import static fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR;
import static fi.iki.elonen.NanoHTTPD.Response.Status.NOT_FOUND;
import static fi.iki.elonen.NanoHTTPD.Response.Status.OK;
import static fi.iki.elonen.NanoHTTPD.SOCKET_READ_TIMEOUT;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdManager.RegistrationListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

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
    private RegistrationListener listener;
    private Bonjour bonjour;

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
        bonjour = new Bonjour(getApplicationContext());
        startServer();
        installNotification();
    }

    @Override
    public void onDestroy() {
        Log.e(LOG_TAG, "GetPixService.onDestroy");
        removeNotificationIcon();
        bonjour.removeService();
        final DataTransfer<String> dt = new DataTransfer<>(1);
        try {
            handler.post(() -> {
                try {
                    httpServer.closeAllConnections();
                    database.close();
                    serviceThread.quitSafely();
                    dt.setData("ok");
                } catch (Exception e) {
                    dt.setData("nok");
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
            handler.post(() -> {
                ArrayList<String> res = new ArrayList<>();
                Iterator<Transferred> i = database.getAll();
                while (i.hasNext()) {
                    Transferred t = i.next();
                    Log.d(LOG_TAG, "from database: " + t.virtualFilename());
                    res.add(t.virtualFilename());
                }
                dt.setData(res);
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
            handler.post(() -> {
                try {
                    database = new StupidDatabase(new LogcatLogging(), LOG_TAG, new File(getFilesDir(), "stupid.java-objects"));
                    httpServer = new WebServer();
                    httpServer.start(SOCKET_READ_TIMEOUT, false);
                    dt.setData("ok");
                    String name = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("bonjour_name", "gizmo");
                    bonjour.installService(PORT, name);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "could not handle request", e);
                    dt.exception(e);
                }
            });
            dt.await();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "got interrupted", e);
            Thread.interrupted();
        }
    }

    class IN extends AsyncTask<Void, Void, NotificationCompat.Builder> {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected NotificationCompat.Builder doInBackground(Void... voids) {
            NotificationChannel channel = new NotificationChannel("getpixservice", "getpixservice", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Intent i = new Intent(GetPixService.this, StopGetPixService.class);

            int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pi = PendingIntent.getActivity(GetPixService.this, 0, i, pendingIntentFlags);

            final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle("GetPix Server running:");
            inboxStyle.setSummaryText("Press to stop server");
            for (String s : MainActivity.getNetworkAddresses()) {
                inboxStyle.addLine(s);
            }

            return new NotificationCompat.Builder(GetPixService.this, "getpixservice").setSmallIcon(R.drawable.ic_notification).setContentIntent(pi).setDeleteIntent(pi).setStyle(inboxStyle);
        }

        @Override
        protected void onPostExecute(NotificationCompat.Builder builder) {
            GetPixService.this.startForeground(GET_PIX_NOTIFICATION, builder.build());
        }
    }

    private void installNotification() {
        new IN().execute();
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
            handler.post(() -> database.add(new Transferred(filename, suffix)));
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
            // files.addAll(databaseFiles()); TODO ????
            JSONArray array = new JSONArray();
            for (String s : files) {
                array.put(s);
            }
            return newFixedLengthResponse(OK, JSON_MIME_TYPE, array.toString());
        }
    }
}
