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
    static final String ACTION_CLOSE = "org.literadar.close";
    static final String ACTION_EXIT = "org.literadar.exit";
//    static final String ACTION_FATAL = "org.literadar.fatal";
    static MainActivity self;
    static Settings sSettings;
    static TransponderService sService;//?
    static TrackerActivity sTracker;
    static final int FINE_LOCATION_GRANTED = 256;
    static Intent sTrackerIntent;
    static Intent sSettingsIntent;

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_EXIT)) {
                finish();
            } else if (action.equals(SettingsActivity.ACTION_RESTART)) {
                startTrackerActivity();
//            } else if (action.equals(ACTION_FATAL)) {
//                fatalDialog(intent.getStringExtra("title"),intent.getStringExtra("message"));
            }
        }
    };
    LocalBroadcastManager mLocalBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
// hide main activity
//        this.getTheme().applyStyle(R.style.AppTheme_Invisible, true);
//        ActivityCompat.recreate(this);

        self = this;

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_EXIT);
        intentFilter.addAction(SettingsActivity.ACTION_RESTART);
//        intentFilter.addAction(ACTION_FATAL);

        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);

        sTrackerIntent = new Intent(this, TrackerActivity.class);
// https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK
        sTrackerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        sSettingsIntent = new Intent(this, SettingsActivity.class);
        sSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, FINE_LOCATION_GRANTED);
    }

    @Override
    public void finish() {
        closeTrackerActivity();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exit(0);
    }

    void permissionsGranted() {
        if (sSettings == null) {
            try {
                sSettings = new Settings();
                if(loadSettings(this)) {
// todo map not showing samsung
                    startTrackerActivity();
                    startActivity(sSettingsIntent);
                    startService(new Intent(this, TransponderService.class));
                }
            } catch (Exception e) {
                fatalDialog(self, e);
            }
        }
    }

    void startTrackerActivity() {
        if (sSettings.showTracker && sTracker == null) {
            sTrackerIntent.putExtra("url", sSettings.getTrackerURL());
            startActivity(sTrackerIntent);
        } else if (!sSettings.showTracker)
            closeTrackerActivity();
    }

    void closeTrackerActivity() {
        sendBroadcast(this, new Intent(ACTION_CLOSE));
    }

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            } else {
                finish();
            }
        } else {
            permissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_LOCATION_GRANTED && grantResults[0] != PERMISSION_DENIED) {
            permissionsGranted();
        } else {
            finish();
        }
    }

    public static abstract class DialogAction {
        DialogAction() {
        }

        public void execute(int id) {
        }
    }

    DialogAction mFatalAction = new DialogAction() {
        @Override
        public void execute(int i) {
            sendBroadcast(self, new Intent(ACTION_EXIT));
            finish();
        }
    };

    static void showDialog(Context context, String title, String message, String okText, DialogAction okAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
// Add the buttons.
        builder.setPositiveButton(okText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                okAction.execute(id);
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
    public void fatalDialog(Context context, Exception e) {
        showDialog(self,
                "FATAL: " + e.toString(),
                e.getMessage(),
                "Exit LiteRadar", mFatalAction);
    }

    boolean loadSettings(Context context) {
        File file = new File(context.getFilesDir(), SETTINGS_FILENAME);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                sSettings.load(fis);
            } catch (Exception e) {
                fatalDialog(self, e);
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    String resString(int resId) {
        return (getResources().getString(resId));
    }

    static void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}