/*
 * LiteRadar Main Activity, MIT (c) 2021-2025 miktim@mail.ru
 * Thanks to:
 *   developer.android.com
 *   stackoverflow.com
 */

package org.miktim.literadar;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import static org.miktim.literadar.Settings.SETTINGS_FILENAME;

import static java.lang.System.exit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;

public class MainActivity extends AppCompatActivity {
    static String ACTION_CLOSE = "org.literadar.close";
    static final String ACTION_EXIT = "org.literadar.exit";
    static MainActivity self;
    static Settings sSettings;
    static TransponderService sService;//?
    static TrackerActivity sTracker;
    static int FINE_LOCATION_GRANTED = 256;
    static Intent sTrackerIntent;
    static Intent sSettingsIntent;

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_EXIT)) {
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
        setContentView(R.layout.activity_main);
//        this.getTheme().applyStyle(R.style.AppTheme_Invisible, true);
//        ActivityCompat.recreate(this);

        self = this;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_EXIT);
        intentFilter.addAction(SettingsActivity.ACTION_RESTART);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);

        sTrackerIntent = new Intent(this, TrackerActivity.class);
// https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK
        sTrackerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sSettingsIntent = new Intent(this, SettingsActivity.class);
        sSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION,FINE_LOCATION_GRANTED);
    }
    @Override
    public void finish () {
        closeTrackerActivity();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.finish();
    }
    @Override
    protected void onDestroy () {
        super.onDestroy();
        exit(0);
    }

    void permissionsGranted() {
        if(sSettings == null) {
            try {
                sSettings = new Settings();
                loadSettings(this);
// todo map not showing samsung
                startTrackerActivity();
                startActivity(sSettingsIntent);
                startService(new Intent(this, TransponderService.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void startTrackerActivity() {
        if(sSettings.showTracker && sTracker == null)
            startActivity(sTrackerIntent);
        else if(!sSettings.showTracker)
            closeTrackerActivity();
    }
    void closeTrackerActivity(){
        sendBroadcast(this, new Intent(ACTION_CLOSE));
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

// accessed over MainActivity.self
    void fatalDialog(Context context, Exception e) {
        showDialog(context,
                "FATAL: " + e.toString(),
                e.getMessage(),
               "Exit LiteRadar" );
        sendBroadcast(this,new Intent(ACTION_EXIT));
        finish();
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

    String resString(int resId) {
        return (getResources().getString(resId));
    }

    static void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}