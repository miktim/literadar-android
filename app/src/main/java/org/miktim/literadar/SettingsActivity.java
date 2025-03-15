/*
 * LiteRadar Settings Activity, MIT (c) 2021-2025 miktim@mail.ru
 * Thanks to:
 *   developer.android.com
 *   stackoverflow.com
 */
package org.miktim.literadar;

import static org.miktim.literadar.MainActivity.okDialog;
import static org.miktim.literadar.Settings.MODE_UNICAST_CLIENT;
import static org.miktim.literadar.Settings.SETTINGS_FILENAME;

import static java.lang.String.format;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;

public class SettingsActivity extends AppActivity
        implements AdapterView.OnItemSelectedListener {
    Settings mSettings;
    String[] mInterfaceArray;
    CheckBox mTrackerChk;
    EditText mAddressEdt;
    Spinner mInterfaceSpn;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MainActivity.ACTION_EXIT)) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        registerReceiver(mBroadcastReceiver, new IntentFilter(MainActivity.ACTION_EXIT));

        mSettings = MainActivity.sSettings;
        mTrackerChk = findViewById(R.id.trackerChk);
        mAddressEdt = findViewById(R.id.addressEdt);
        mInterfaceSpn = findViewById(R.id.interfaceSpn);
        fillLayout();

//        throw new NullPointerException(); // test
    }

    @Override
    public void finish() {
        unregisterReceiver(mBroadcastReceiver);
        super.finish();
    }

    Settings loadSettings(Context context) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Settings settings = null;
        File file = new File(context.getFilesDir(), SETTINGS_FILENAME);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                settings = new Settings();
                settings.load(fis);
            } catch (IOException | ParseException e) {
                okDialog(getApplicationContext(),
                        resString(R.string.err_settings_title),
                        resString(R.string.err_settings_msg)
                );
            }
        }
        return settings;
    }

    void fillLayout() {
        String version = "";
        try {
            version = this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignore) { }
        ((TextView) findViewById(R.id.titleTxt)).setText(
                format(resString(R.string.titleFmt),resString(R.string.app_name), version));
        ((TextView) findViewById(R.id.keyTxt)).setText(
                format(resString(R.string.keyFmt), mSettings.algorithm));
        ((TextView) findViewById(R.id.keyDateTxt)).setText
                (format(resString(R.string.key_dateFmt),
                        new SimpleDateFormat("yyyy-MM-dd").format(mSettings.getKeyTimeStamp())));

        ((TextView) findViewById(R.id.nameEdt)).setText(mSettings.name);
        String iName = mSettings.network.interfaceName;
        mInterfaceArray = getInterfaceList(iName);
        initDropdownList(mInterfaceSpn, mInterfaceArray,
                getListIndex(mInterfaceArray, iName));
        initDropdownList(findViewById(R.id.modeSpn), getResources().getStringArray(R.array.mode_array), mSettings.mode);
        modeDependedSettings(mSettings.mode); // sets address, tracker enabled
    }

    int getListIndex(String[] iList, String itemName) {
// get listIndex by itemName
        for (int i = 0; itemName != null && i < iList.length; i++)
            if (iList[i].startsWith(itemName)) return i;
        return 0;
    }

    void initDropdownList(Spinner s, String[] items, int pos) {
// https://stackoverflow.com/questions/13377361/how-to-create-a-drop-down-list
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, R.layout.spinner_item, items);
// set the spinners adapter to the previously created one.
        s.setAdapter(adapter);
        s.setOnItemSelectedListener(this);
        s.setSelection(pos);
    }

    void modeDependedSettings(int mode) {
        mSettings.setMode(mode);
        String address = mSettings.network.getAddress(mode);
        switch (mode) {
            case Settings.MODE_TRACKER_ONLY: {
                mTrackerChk.setChecked(true);
                mTrackerChk.setEnabled(false);
                mInterfaceSpn.setEnabled(false);
                mAddressEdt.setEnabled(false);
                address = "";
                break;
            }
            case Settings.MODE_MULTICAST_MEMBER: {
                mTrackerChk.setChecked(mSettings.getTrackerEnabled());
                mTrackerChk.setEnabled(true);
                mInterfaceSpn.setEnabled(true);
                mAddressEdt.setEnabled(false);
                break;
            }
            case Settings.MODE_UNICAST_CLIENT: {
                mTrackerChk.setChecked(mSettings.getTrackerEnabled());
                mTrackerChk.setEnabled(true);
                mInterfaceSpn.setEnabled(true);
                mAddressEdt.setEnabled(true);
                break;
            }
        }
        mAddressEdt.setText(address);
    }

    String[] getInterfaceList(String iName) {
        ArrayList<String> niList = new ArrayList<>();
        niList.add(resString(R.string.all_interfaces));
        try {
            Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
            while (niEnum.hasMoreElements()) {
                NetworkInterface ni = niEnum.nextElement();
                String niDn = ni.getDisplayName();
                InetAddress ia = mSettings.network.getInet4Address(ni);
                if ((ia != null && !ia.isLoopbackAddress()) || niDn.equals(iName)) {
                    niList.add(niDn +
                            (ia != null ? ia.toString() : resString(R.string.int_unavailable)));
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return niList.toArray((new String[0]));
    }

    String resString(int resString) {
        return (getResources().getString(resString));
    }

    public void clipTagClicked(View v) {
// todo
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Spinner spinner = (Spinner) parent;
        int spinnerId = parent.getId();
        if(spinnerId == R.id.modeSpn) {
            modeDependedSettings(position);
            mSettings.mode = position;
            if(position == Settings.MODE_TRACKER_ONLY) mSettings.showTracker = true;
        } else if(spinnerId == R.id.interfaceSpn) {
            try {
                mSettings.network.setInterface(
                        spinner.getSelectedItem().toString().split("/")[0]);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void trackerChkClicked(View v) {
        mSettings.setTrackerEnabled(mTrackerChk.isChecked());
    }

    boolean fillSettings() {
        String name = ((TextView) findViewById(R.id.nameEdt)).getText().toString();
        mSettings.name = name.substring(0, Math.min(name.length(), 16));
        String address = mAddressEdt.getText().toString();
        try {
            if (mSettings.getMode() == MODE_UNICAST_CLIENT)
                mSettings.network.setRemoteAddress(
                        address.isEmpty() ? null : address);
        } catch (Exception e) {
            okDialog(this,
                    resString(R.string.err_wrong_address),
                    resString(R.string.required_address));
            mAddressEdt.requestFocus();
            return false;
        }
        return true;
    }

    void saveSettings(Context context) {
        File file = new File(context.getFilesDir(), SETTINGS_FILENAME);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            mSettings.save(fos);
//            MainActivity.sSettings = mSettings;
        } catch (Exception e) {
            okDialog(getApplicationContext(),
                    resString(R.string.err_settings_title),
                    resString(R.string.err_settings_msg));
        }
    }

    public void cancelBtnClicked(View v) {
        finish();
    }

    public void exitBtnClicked(View v) {
        exitLiteRadar();
    }

    void exitLiteRadar() {
        sendBroadcast(this, new Intent(MainActivity.ACTION_EXIT));
        finish();
    }

    public void restartBtnClicked(View v) {
        if (fillSettings()) { // returns false on illegal params
            saveSettings(this);
            sendBroadcast(this, new Intent(MainActivity.ACTION_RESTART));
            finish();
        }
    }

    void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}