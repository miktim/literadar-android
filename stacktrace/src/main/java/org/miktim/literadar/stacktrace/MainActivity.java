package org.miktim.literadar.stacktrace;

import static java.lang.System.exit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends Activity {
    public static String ACTION_LOG = "org.miktim.SEND_LOG";
//    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver;
    static TextView mLogView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); // make a dialog without a titlebar
        setFinishOnTouchOutside(false); // prevent users from dismissing the dialog by tapping outside
        setContentView(R.layout.activity_main);

        mLogView = (TextView) findViewById(R.id.stackTraceTx);
        //mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastReceiver = new AppReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_LOG);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
        exit(0);
    }

    public void exitBtnClicked(View v) {
        finish();
    }

}