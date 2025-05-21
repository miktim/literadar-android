/*
 * LiteRadar Main Activity, MIT (c) 2021-2025 miktim@mail.ru
 * - static fields and methods;
 * - check app permissions;
 * - start/stop service and tracker.
 *
 * Thanks to:
 *   developer.android.com
 *   stackoverflow.com
 */

package org.miktim.literadar;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import static org.miktim.literadar.Settings.SETTINGS_FILENAME;

import static java.lang.String.format;
import static java.lang.System.exit;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

public class MainActivity extends AppActivity {
    static final String ACTION_EXIT = "org.literadar.EXIT";
    static final String ACTION_RESTART = "org.literadar.RESTART";
    static final String SETTINGS_FILENAME = "settings.json";

    static Context mContext;

    static Settings sSettings;
    static boolean sServiceStarted = false;
    static boolean sTrackerStarted = false;

    static final int PERMISSION_GRANTED = 256;
    static Intent sTrackerIntent;
    static Intent sSettingsIntent;
    static Intent sServiceIntent;

    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_EXIT)) {
                finish();
            } else if (action.equals(ACTION_RESTART)) {
                startTrackerActivity();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
// hide main activity
//        this.getTheme().applyStyle(R.style.AppTheme_Invisible, true);
//        ActivityCompat.recreate(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mContext = getApplicationContext();

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_EXIT);
        intentFilter.addAction(ACTION_RESTART);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);

        sTrackerIntent = new Intent(this, TrackerActivity.class);
// https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK
        sTrackerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        sSettingsIntent = new Intent(this, SettingsActivity.class);
        sSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//
        sServiceIntent = new Intent(this, TransponderService.class);
        checkPermission(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
                Manifest.permission.POST_NOTIFICATIONS
        }, PERMISSION_GRANTED);
        wifiAcquire();

    }

    @Override
    public void finish() {
        super.finish();
        closeTrackerActivity();
    }

    @Override
    protected void onDestroy() {
        wifiRelease();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
        exit(0);
    }

    void permissionsGranted() {
        if (sSettings == null) {
            try {
                sSettings = new Settings(); // todo
                sSettings = loadSettings(getBaseContext());
            } catch (Throwable t) {
                toastError(getBaseContext(),getString(R.string.err_settings_title));
//                finish();
//                self.uncaughtException(Thread.currentThread(), t);
            }
// todo ???map not showing samsung
            startTrackerActivity();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(sServiceIntent);
            } else {
                startService(sServiceIntent);
            }
            startActivity(sSettingsIntent);
        }
    }

    void startTrackerActivity() {
        if (sSettings.showTracker && !sTrackerStarted) {
            sTrackerIntent.putExtra("url", sSettings.getTrackerURL());
            startActivity(sTrackerIntent);
        } else if (!sSettings.showTracker)
            closeTrackerActivity();
    }

    void closeTrackerActivity() {
        sendBroadcast(this, new Intent(TrackerActivity.ACTION_TRACKER_CLOSE));
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
        if (requestCode == PERMISSION_GRANTED && grantResults[0] != PERMISSION_DENIED) {
            permissionsGranted();
        } else {
            finish();
        }
    }

// WiFi
    static String sWifiTAG = "TAG";
    WifiManager.WifiLock mWifiLock;
    WifiManager.MulticastLock mCastLock;
    void wifiAcquire() {
// https://www.b4x.com/android/forum/threads/solved-android-blocking-receiving-udp-broadcast.99519/
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService( Context.WIFI_SERVICE );
        if(wifi != null){
            mWifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF,sWifiTAG);
            mWifiLock.acquire();
            mCastLock = wifi.createMulticastLock(sWifiTAG);
            mCastLock.acquire();
        }
    }
    void wifiRelease() {
        if (mWifiLock != null) mWifiLock.release();
        if(mCastLock != null) mCastLock.release();
    }

    public static class DialogAction {
        public DialogAction() {
        }
        public void execute(int id) {
        }
    }

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

    public static void okDialog(Context context, String title, String msg) {
        showDialog(context, title, msg, "Ok", new DialogAction());
    }

    public static void toastError(Context context, String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();
        Notifier.beep();
    }

    public static void toastInfo(Context context, String text) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();
    }

    Settings loadSettings(Context context) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Settings settings = new Settings();
        File file = new File(context.getFilesDir(), SETTINGS_FILENAME);
        if (file.exists()) {
            try (FileInputStream fis = context.openFileInput(SETTINGS_FILENAME)) {
//            try (FileInputStream fis = new FileInputStream(file)) {
                settings.load(fis);
            } catch (IOException | ParseException e) {
                toastError(context, getString(R.string.err_settings_title));
/*
                MainActivity.okDialog(mContext,
                        getString(R.string.err_settings_title),
                        getString(R.string.err_settings_msg)
                        );
*/
            }
        }
        return settings;
    }

    static void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void exitBtnClicked(View v) {
        finish();
    }

    static Throwable sFatal = null;
    static void fatal(Context context, Throwable throwable) {
        synchronized (sFatal) {
            if(sFatal != null) return;
            sFatal = throwable;
        }
        File file = new File(context.getFilesDir(), "fatal.log");
        try (PrintStream ps = new PrintStream(file)) {
            throwable.printStackTrace(ps);
            Throwable cause = throwable.getCause();
            if(cause == null) cause = throwable;
            Toast.makeText(context,
                    format("LiteRadar FATAL: %s", cause.getClass().getSimpleName()),
                    Toast.LENGTH_LONG).show();
            sleep(3000);
            sendBroadcast(context, new Intent(ACTION_EXIT));
            exit(1);
        } catch (Throwable ignore) {}
    }
}