package org.miktim.literadar;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.spec.InvalidKeySpecException;

public class MainActivity extends AppCompatActivity {
    static Settings sSettings;
    static TransponderService sService;//?
    static String ACTION_CLOSE = "org.literadar.close";
    static TrackerActivity sTracker;
    static int FINE_LOCATION_GRANTED = 256;
    Intent mTrackerIntent;

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(SettingsActivity.ACTION_EXIT)) {
                stopTrackerActivity();
                finish();
            } else if (action.equals(SettingsActivity.ACTION_RESTART)){
                startTrackerActivity();
            }
        }
    };
    LocalBroadcastManager mLocalBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
// hide main activity
//        setContentView(R.layout.activity_main);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SettingsActivity.ACTION_EXIT);
        intentFilter.addAction(SettingsActivity.ACTION_RESTART);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);

        mTrackerIntent = new Intent(this, TrackerActivity.class);
        mTrackerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

//todo requestPermissions FINE_LOCATION
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION,FINE_LOCATION_GRANTED);
    }

    void permissionsGranted() {
        if(sSettings == null) {
// todo must be load settings
            sSettings = new Settings();
            try {
                sSettings.create();
            } catch (Exception e) {
                e.printStackTrace();
            }
//todo            startService(new Intent(this, TransponderService.class));
            startTrackerActivity();
            startActivity(new Intent(this, SettingsActivity.class));
        }
    }

    public void checkPermission(String permission, int requestCode)
    {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
            } else {
                finish();
            }
        }
        else {
            permissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_LOCATION_GRANTED && grantResults[0] != PERMISSION_DENIED){
            permissionsGranted();
        }    else {
            finish();
        }

    }
    void startTrackerActivity() {
        if(sSettings.showTracker && sTracker == null)
            startActivity(mTrackerIntent);
        else if(!sSettings.showTracker && sTracker != null)
            stopTrackerActivity();
    }
    void stopTrackerActivity() {
        sendBroadcast(this, new Intent(ACTION_CLOSE));
    }

    void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void finish () {
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.finish();
//        System.exit(0);
    }
}