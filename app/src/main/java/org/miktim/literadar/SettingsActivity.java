/*
 * LiteRadar Settings Activity, MIT (c) 2021-2025 miktim@mail.ru
 * Thanks to:
 *   developer.android.com
 *   stackoverflow.com
 */
package org.miktim.literadar;

import static org.miktim.literadar.MainActivity.okDialog;
import static org.miktim.literadar.Settings.MODE_UNICAST_CLIENT;
import static java.lang.String.format;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.tabs.TabLayout;

import org.miktim.Base64;

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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

public class SettingsActivity extends AppActivity
        implements AdapterView.OnItemSelectedListener {
    Settings mSettings;
    String[] mInterfaceArray;
    CheckBox mTrackerChk;
    EditText mAddressEdt;
    Spinner mInterfaceSpn;
    TabLayout mTabLayout;

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
        mTabLayout = findViewById(R.id.sectionTabs);
        mTabLayout.addOnTabSelectedListener (mTabListener);
        mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        mSettings = MainActivity.sSettings;
        mTrackerChk = findViewById(R.id.trackerChk);
        mAddressEdt = findViewById(R.id.addressEdt);
        mInterfaceSpn = findViewById(R.id.interfaceSpn);
        fillGeneralLayout();
        fillFavoritesLayout();

//        throw new NullPointerException(); // test
    }

    @Override
    public void finish() {
        unregisterReceiver(mBroadcastReceiver);
        mTabLayout.removeOnTabSelectedListener(mTabListener);
        super.finish();
    }

    Settings loadSettings(Context context) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Settings settings = null;
        File file = new File(context.getFilesDir(), MainActivity.SETTINGS_FILENAME);
        if (file.exists()) {
            try (FileInputStream fis = context.openFileInput(MainActivity.SETTINGS_FILENAME)) {
//                try (FileInputStream fis = new FileInputStream(file)) {
                settings = new Settings();
                settings.load(fis);
            } catch (IOException | ParseException e) {
                okDialog(getApplicationContext(),
                        getString(R.string.err_settings_title),
                        getString(R.string.err_settings_msg)
                );
            }
        }
        return settings;
    }

    void fillGeneralLayout() {
        String version = "";
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = format("%s.%s",pInfo.versionName, pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException ignore) { }
        ((TextView) findViewById(R.id.titleTxt)).setText(
                format(getString(R.string.titleFmt),getString(R.string.app_name), version));
        ((TextView) findViewById(R.id.keyTxt)).setText(
                format(getString(R.string.keyFmt), mSettings.algorithm));
        ((TextView) findViewById(R.id.keyDateTxt)).setText(
                format(getString(R.string.key_dateFmt), mSettings.getKeyTimeStamp()));

        ((TextView) findViewById(R.id.nameEdt)).setText(mSettings.name);
        String iName = mSettings.network.interfaceName;
        mInterfaceArray = getInterfaceList(iName);
        initDropdownList(mInterfaceSpn, mInterfaceArray,
                getListIndex(mInterfaceArray, iName));
        initDropdownList(findViewById(R.id.modeSpn), getResources().getStringArray(R.array.mode_array), mSettings.mode);
        modeDependedSettings(mSettings.mode); // sets address, tracker enabled

        // Geolocation
        ((TextView) findViewById(R.id.minTimeEdt)).setText(
                String.valueOf(mSettings.locations.getMinTime()));

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
        niList.add(getString(R.string.all_interfaces));
        try {
            Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
            while (niEnum.hasMoreElements()) {
                NetworkInterface ni = niEnum.nextElement();
                String niDn = ni.getDisplayName();
                InetAddress ia = Network.getInet4Address(ni);
                if ((ia != null && !ia.isLoopbackAddress()) || niDn.equals(iName)) {
                    niList.add(niDn +
                            (ia != null ? ia.toString() : getString(R.string.int_unavailable)));
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return niList.toArray((new String[0]));
    }

    public void onBtnCopyTagClick(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        try {
            ClipData clip = ClipData.newPlainText("Tag", mSettings.getTag());
            clipboard.setPrimaryClip(clip);
            MainActivity.toastInfo(this, getString(R.string.toastClipTag));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public void onBtnPasteTagClick(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String pasteTag;
        try {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            pasteTag = item.getText().toString();
            Base64.decode(pasteTag);
        } catch (Exception e) {
            Notifier.beep(); // TODO toast
            return;
        }
        mFavoritesTable.updateRow(pasteTag);
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

    public void onChkTrackerShowClick(View v) {
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
            mAddressEdt.requestFocus();
            MainActivity.toastError(this,getString(R.string.err_wrong_address));
/*
            okDialog(this,
                    getString(R.string.err_wrong_address),
                    getString(R.string.required_address));
*/
            return false;
        }

        // Geolocation
        mSettings.locations.minTime = checkNumberView(R.id.minTimeEdt, 1);
//        mSettings.locations.minDistance = checkNumberView(R.id.minDistanceEdt, 0);
//        mSettings.locations.timeout = checkNumberView(R.id.timeoutEdt,mSettings.locations.minTime * 2);
        fillFavoritesSettings();
        return true;
    }
    int checkNumberView(int viewId, int minValue) {
        TextView view = ((TextView) findViewById(viewId));
        String s = view.getText().toString();
        if(s.isEmpty() || Integer.parseInt(s) < minValue) {
            view.setText(String.valueOf(minValue));
            return minValue;
        }
        return Integer.parseInt(s);
    }

// Settings sections

    void setViewVisibility(int viewId, boolean visible) {
        setViewVisibility(findViewById(viewId), visible);
    }
    void setViewVisibility(View view, boolean visible) {
        if (visible) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

// Settings sections
    int[] mSectionIds = new int[] {
            R.id.sectionGeneral,
            R.id.sectionFavorites,
            0 // exit literadar
    };
    TabLayout.OnTabSelectedListener mTabListener = new TabLayout.OnTabSelectedListener() {

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            int tabPos = tab.getPosition();
            int sectionRid = mSectionIds[tabPos];
            if(sectionRid == 0) {
                exitLiteRadar();
                return;
            }
            setViewVisibility(sectionRid,true);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            int tabPos = tab.getPosition();
            setViewVisibility(mSectionIds[tabPos], false);
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
        }
    };

// Favorites table layout
     class FavoritesTable {
        Context context;
        HashMap<String, TableRow> rowHashMap = new HashMap<>();
        TableLayout table;
        boolean favoritesOnly;

        FavoritesTable(Context context) {
            this.context = context;
            table = findViewById(R.id.favoritesTbl);
            favoritesOnly = mSettings.favorites.favoritesOnly;
            ((CheckBox)findViewById(R.id.favoritesOnlyChk)).setChecked(favoritesOnly);
            fill();
        }
        TableLayout table() {
            return table;
        }
        int rows() {
            return table.getChildCount();
        }
        TableRow row(int i) {
            return (TableRow)table.getChildAt(i);
        }
        CheckBox getFavorite(TableRow row) {
            return (CheckBox)row.getChildAt(0);
        }
        EditText getName(TableRow row) {
            return (EditText)row.getChildAt(1);
        }
        TextView getExpired(TableRow row) {
            return (TextView)row.getChildAt(2);
        }
        EditText getTag(TableRow row) {
            return (EditText)row.getChildAt(3);
        }
        void fill() {
            Favorites.Entry[] entries = mSettings.favorites.listEntries();
            for(Favorites.Entry entry : entries) {
                addRow(entry);
            }
        }
        void refresh() {
            for(int i = 1; i < rows(); i++)
                setRowVisibility(row(i));
        }
        void setFavoritesOnly(boolean yes) {
            favoritesOnly = yes;
            refresh();
        }
        void setRowState(TableRow row) {
            boolean favorite = getFavorite(row).isChecked();
            getName(row).setEnabled(favorite);
            setRowVisibility(row);
        }
        void setRowVisibility(TableRow row) {
            setViewVisibility(row, !favoritesOnly || getFavorite(row).isChecked());
        }
        void updateRow(String tag) {
            Favorites.Entry entry = mSettings.favorites.getEntry(tag);
            if(entry != null) {
                entry.setFavorite(true);
            } else {
                entry = new Favorites.Entry(tag, Settings.defaultName(tag));
                mSettings.favorites.updateEntry(entry);
            }

            TableRow row = addRow(entry);
            Rect rectangle=new Rect();
            row.getDrawingRect(rectangle);
            row.requestRectangleOnScreen(rectangle);
//            row.requestRectangleOnScreen(new Rect(0, 0, row.getWidth(), row.getHeight()));
        }
        TableRow addRow(Favorites.Entry entry) {
            TableRow row = rowHashMap.get(entry.getId());
            if(row == null) {
// https://stackoverflow.com/questions/38012381/duplicate-a-view-programmatically-from-an-already-existing-view
                row = (TableRow)LayoutInflater.from(context).inflate(
                    R.layout.favoriterow_item, null);
                table.addView(row);
                rowHashMap.put(entry.getId(), row);
            }
            getFavorite(row).setChecked(entry.getFavorite());
            getName(row).setText(entry.getName());
            getExpired(row).setText(entry.getExpiryDate() > 0 ?
                     format("%tR %1$tF", entry.getExpiryDate()) : "");
            getTag(row).setText(entry.getId());
            setRowState(row);
            return row;
        }
        void fillSettings() {
            mSettings.favorites.favoritesOnly = favoritesOnly;
            for( int i = 1; i < rows(); i++) {
                TableRow row = row(i);
//                if(!getFavorite(row).isChecked()) continue;
                Favorites.Entry entry = new Favorites.Entry(
                        getTag(row).getText().toString(),
                        getName(row).getText().toString());
                entry.setFavorite(getFavorite(row).isChecked());
                mSettings.favorites.updateEntry(entry);
            }
        }
    }

    FavoritesTable mFavoritesTable;
    void fillFavoritesLayout() {
        mFavoritesTable = new FavoritesTable(this);
    }
    void fillFavoritesSettings() {
        mFavoritesTable.fillSettings();
    }
    public void onChkFavoritesOnlyClick(View view) {
        mFavoritesTable.setFavoritesOnly(((CheckBox)view).isChecked());
    }
    public void onChkRowFavoriteClick(View view) {
        mFavoritesTable.setRowState((TableRow) view.getParent());
    }

    void saveSettings(Context context) {
//        File file = new File(context.getFilesDir(), MainActivity.SETTINGS_FILENAME);
        try (FileOutputStream fos = context.openFileOutput(
                MainActivity.SETTINGS_FILENAME, Context.MODE_PRIVATE)) {
//        try (FileOutputStream fos = new FileOutputStream(file)) {
            mSettings.save(fos);
        } catch (Exception e) {
            MainActivity.toastError(this,getString(R.string.err_settings_title));
/*
            okDialog(getApplicationContext(),
                    getString(R.string.err_settings_title),
                    getString(R.string.err_settings_msg));
*/
        }
    }

    public void onBtnCancelClick(View v) {
        finish();
    }

    public void onBtnExitClick(View v) {
        exitLiteRadar();
    }

    void exitLiteRadar() {
        sendBroadcast(this, new Intent(MainActivity.ACTION_EXIT));
        finish();
    }

    public void onBtnRestartClick(View v) {
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