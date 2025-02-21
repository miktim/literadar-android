package org.miktim.literadar.stacktrace;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AppReceiver extends BroadcastReceiver {
    public AppReceiver() {
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        MainActivity.mLogView.setText(intent.getStringExtra("stackTrace"));
    }
}
