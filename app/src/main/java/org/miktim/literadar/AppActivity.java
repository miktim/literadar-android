package org.miktim.literadar;

import static java.lang.String.format;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.PrintStream;

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
/*
                File file = new File(context.getFilesDir(), "fatal.log");
                try (PrintStream ps = new PrintStream(file)) {
                    if(MainActivity.sFatal == null) {
                        MainActivity.sFatal = throwable;
                        throwable.printStackTrace(ps);

                        Toast.makeText(context,
                                format("Fatal: %s",
                                        throwable.getCause().getClass().getSimpleName()),
                                Toast.LENGTH_LONG).show();

                        LocalBroadcastManager.getInstance(context).sendBroadcast(
                                new Intent(MainActivity.ACTION_EXIT));
//                    MainActivity.self.finishAffinity();
                    }
//                    finish();
                    System.exit(1);
                } catch (Throwable ignore) { }
//                MainActivity.logFatal(throwable);
*/
            }
        });
    }
}
