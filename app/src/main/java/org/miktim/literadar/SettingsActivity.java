package org.miktim.literadar;

import static org.miktim.literadar.Settings.SETTINGS_FILENAME;

import static java.lang.String.format;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

//import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;

public class SettingsActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener  {
    Settings mSettings;
    static final String ACTION_EXIT = "org.literadar.exit";
    static final String ACTION_RESTART = "org.literadar.restart";
    String[] mInterfaceArray;
//    String[] mModeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_settings);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mSettings = MainActivity.sSettings;
        fillLayout();

    }

    void setEditText(View v, String s) {
        ((EditText) v).setText(s);
        ((EditText) v).setHint(s);
    }

    void fillLayout() {
        ((TextView) findViewById(R.id.titleTxt)).setText(resString(R.string.app_name));
        ((TextView) findViewById(R.id.keyTxt)).setText(resString(R.string.keyLbl));
// todo key timestamp
        ((TextView) findViewById(R.id.keyDateTxt)).setText(resString(R.string.key_dateLbl)
        + new SimpleDateFormat("yyyy-MM-dd").format(mSettings.getKeyTimeStamp()));
// todo replace tag with clip button
        String iName = mSettings.network.interfaceName;
        mInterfaceArray = getInterfaceList(iName);
        initDropdownList(findViewById(R.id.interfaceSpn), mInterfaceArray,
                getListIndex(mInterfaceArray, iName));
        initDropdownList(findViewById(R.id.modeSpn), getResources().getStringArray(R.array.mode_array), mSettings.mode);
        ((CheckBox)findViewById(R.id.trackerChk)).setChecked(mSettings.showTracker);
        ((EditText) findViewById(R.id.addressEdt)).setText(mSettings.network.getAddress(mSettings.getMode()));

    }

    int getListIndex(String[] iList, String itemName) {
        for (int i = 0; itemName != null && i < iList.length; i++)
            if (iList[i].startsWith(itemName)) return i;
        return 0;
    }
/* todo
    String getSelectedInterfaceName(Spinner spinner) {
        int i = spinner.getSelectedItemPosition();
        if (spinner.getId() == R.id.interfaceLst) {
            if (i == 0) return "";
            return mInterfacesList[i].split("/")[0];
        }
        return i;
    }
*/
    void initDropdownList(Spinner s, String[] items, int pos) {
// https://stackoverflow.com/questions/13377361/how-to-create-a-drop-down-list
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, R.layout.spinner_item, items);
// set the spinners adapter to the previously created one.
        s.setAdapter(adapter);
        s.setOnItemSelectedListener(this);
        s.setSelection(pos);
    }

    void fillSettings(){

    }

    String[] getInterfaceList(String iName) {
        ArrayList<String> niList = new ArrayList<>();
        niList.add(resString(R.string.all_interfaces));
        try {
            Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
            while ( niEnum.hasMoreElements() ) {
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
        return(getResources().getString(resString));
    }

    void loadSettings(Context context) {
        File file = new File(context.getFilesDir(), SETTINGS_FILENAME);
        try(FileInputStream fis = new FileInputStream(file) ) {
            if (!file.exists()) {
                mSettings.create();
//                saveSettings(context);
            } else {
                mSettings.load(fis);
            }
        } catch (Exception e) {
//
            finish();
        }
    }
    void saveSettings(Context context) {
        File file = new File(context.getFilesDir(), SETTINGS_FILENAME);
        try(FileOutputStream fos = new FileOutputStream(file)) {
            mSettings.save(fos);
        } catch (Exception e) {
//            e.printStackTrace();
            finish();
        }
    }
    public void clipTagClicked(View v) {

    }
    public void trackerChkClicked(View v) {

    }
    public void cancelBtnClicked(View v) {
        finish();
    }
    public void exitBtnClicked(View v) {
        sendBroadcast(this, new Intent(ACTION_EXIT));
        finish();
    }
    public void restartBtnClicked(View v) {
        fillSettings();
        saveSettings(this);
        sendBroadcast(this, new Intent(ACTION_RESTART));
        finish();
    }
    void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}