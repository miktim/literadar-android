package org.miktim.literadar;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public class AppActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
// Thread.defaultExceptionHandler.uncaughtException
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Context context = getApplicationContext();
                MainActivity.fatal(context, throwable);
            }
        });
    }
}
