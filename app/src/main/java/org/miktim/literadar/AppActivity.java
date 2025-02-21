package org.miktim.literadar;

import android.app.Activity;
import android.os.Bundle;

public class AppActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                MainActivity.showStackTrace(throwable);
                finish();
            }
        });
    }
}
