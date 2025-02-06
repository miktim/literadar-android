/*
 * LiteRadar Main Activity, MIT (c) 2021-2025 miktim@mail.ru
 * Thanks to:
 *   developer.android.com
 *   stackoverflow.com
 */

package org.miktim.literadar;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import static org.miktim.literadar.Settings.SETTINGS_FILENAME;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;

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

        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION,FINE_LOCATION_GRANTED);
    }
    @Override
    public void finish () {
        closeTrackerActivity();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.finish();
//        System.exit(0);
    }

    void permissionsGranted() {
        if(sSettings == null) {
            try {
                sSettings = new Settings();
                loadSettings(this);
                startService(new Intent(this, TransponderService.class));
                startTrackerActivity();
                startActivity(new Intent(this, SettingsActivity.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    static void showDialog(Context context, String title, String message, String okText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
// Add the buttons.
        builder.setPositiveButton(okText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User taps OK button.
            }
        });
/*
        builder.setNegativeButton(R.string.cancelLbl, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancels the dialog.
            }
        });
*/
        builder.setMessage(message).setTitle(title);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    void loadSettings(Context context) {
        File file = new File(context.getFilesDir(), SETTINGS_FILENAME);
        try (FileInputStream fis = new FileInputStream(file)) {
            if (file.exists()) {
                sSettings.load(fis);
            }
        } catch (Exception e) {
            MainActivity.showDialog(this,
                    resString(R.string.err_io),
                    e.getLocalizedMessage(),
                    resString(R.string.exitLbl));
            e.printStackTrace();
            finish();
        }
    }

    String resString(int resString) {
        return (getResources().getString(resString));
    }

    void startTrackerActivity() {
        if(sSettings.showTracker)// && sTracker == null)
            startActivity(mTrackerIntent);
        else //if(!sSettings.showTracker)
            closeTrackerActivity();
    }
    void closeTrackerActivity(){
        sendBroadcast(this, new Intent(ACTION_CLOSE));
    }

    void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}