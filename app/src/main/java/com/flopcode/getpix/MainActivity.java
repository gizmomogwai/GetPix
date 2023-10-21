package com.flopcode.getpix;

import static android.Manifest.permission.ACCESS_MEDIA_LOCATION;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.text.TextUtils.join;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.flopcode.getpix.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MY_REQUEST_PERMISSON_CODE = 42;
    public static final String LOG_TAG = "GetPix";

    private ActivityMainBinding binding;

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

        Log.e(LOG_TAG, "Activity started");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        new GetNetworkAddresses(binding.ips).execute();
        setSupportActionBar(binding.toolbar);
        binding.fab.setOnClickListener(view -> {
            final Intent intent = getPixServiceIntent(MainActivity.this);
            intent.setAction("delete");
            Snackbar.make(view, "Reset copy information data", LENGTH_LONG)
                    .setAction("Reset", view1 -> startService(intent))
                    .show();
        });
        binding.start.setOnClickListener(view -> {
            Log.e(LOG_TAG, "onStart");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(getPixServiceIntent(MainActivity.this));
            }
        });
        requestPermissions();
    }

    public static Intent getPixServiceIntent(Context c) {
        final Intent intent = new Intent(c, GetPixService.class);
        String name = PreferenceManager.getDefaultSharedPreferences(c).getString("name", "gizmo");
        intent.putExtra("name", name);
        return intent;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void requestPermissions() {
        int readRes = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);
        int writeRes = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE);
        int readMediaImagesRes = ContextCompat.checkSelfPermission(this, READ_MEDIA_IMAGES);
        int readMediaVideoRes = ContextCompat.checkSelfPermission(this, READ_MEDIA_VIDEO);
        int accessMediaLocationRes = ContextCompat.checkSelfPermission(this, ACCESS_MEDIA_LOCATION);
        if ((readRes != PERMISSION_GRANTED)
                || (writeRes != PERMISSION_GRANTED)
                || (readMediaImagesRes != PERMISSION_GRANTED)
                || (readMediaVideoRes != PERMISSION_GRANTED)
                || (accessMediaLocationRes != PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(this, new String[]{
                    READ_MEDIA_IMAGES,
                    READ_MEDIA_VIDEO,
                    READ_EXTERNAL_STORAGE,
                    WRITE_EXTERNAL_STORAGE,
                    ACCESS_MEDIA_LOCATION,
            }, MY_REQUEST_PERMISSON_CODE);
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
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, PreferencesActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_REQUEST_PERMISSON_CODE) {
            for (int i = 0; i < permissions.length; ++i) {
                Log.e(LOG_TAG, permissions[i] + ": " + (grantResults[i] == PERMISSION_GRANTED ? "granted" : "denied"));
            }
            showPictureFolder();
        }
    }

    private void showPictureFolder() {
    }
}
