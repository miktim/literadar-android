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
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;

public class MainActivity extends AppActivity {
    static final String ACTION_CLOSE = "org.literadar.close";
    static final String ACTION_EXIT = "org.literadar.exit";

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
            }
        }
    };
    LocalBroadcastManager mLocalBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
// hide main activity
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
        checkPermission(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
        }, FINE_LOCATION_GRANTED);

    }

    @Override
    public void finish() {
        super.finish();
        closeTrackerActivity();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
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
                loadSettings(this);
                // todo ???map not showing samsung
                startTrackerActivity();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(new Intent(this, TransponderService.class));
                } else {
                    startService(new Intent(this, TransponderService.class));
                }
                startActivity(sSettingsIntent);
            } catch (Exception e) {
                fatalDialog(e);
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

    public void checkPermission(String[] permissions, int requestCode) {
        // Checking if permission is not granted
        if (!checkPermissions(permissions)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(MainActivity.this, permissions, requestCode);
            } else {
                finish();
            }
        } else {
            permissionsGranted();
        }
    }

    public boolean checkPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_LOCATION_GRANTED && grantResults[0] != PERMISSION_DENIED) {
            permissionsGranted();
        } else {
            finish();
        }
    }

    public static class DialogAction {
        DialogAction() {
        }
        public void execute(int id) {
        }
    }

    static void showDialog(Context context, String title, String message, String okText, DialogAction okAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
// Add the buttons.
        builder.setNegativeButton(okText, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                okAction.execute(id);
            }
        });
/*
        builder.setPositiveButton(R.string.cancelLbl, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancels the dialog.
            }
        });
*/
        builder.setMessage(message).setTitle(title);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    static DialogAction sFatalAction = new DialogAction() {
        @Override
        public void execute(int i) {
            sendBroadcast(self, new Intent(ACTION_EXIT));
        }
    };

    public static void fatalDialog(Throwable t) {
        showDialog(self,
                "FATAL: " + t.toString(),
                t.getMessage(),
                "Exit LiteRadar", sFatalAction);
    }

    void loadSettings(Context context) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, ParseException {
        File file = new File(context.getFilesDir(), SETTINGS_FILENAME);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                sSettings.load(fis);
            }
        }
    }

    String resString(int resId) {
        return (getResources().getString(resId));
    }

    static void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void exitBtnClicked(View v) {
        finish();
    }
}