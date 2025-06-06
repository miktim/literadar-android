/*
 * LiteRadar Tracker Activity, MIT (c) 2021-2025 miktim@mail.ru
 * Thanks to:
 *   developer.android.com
 *   stackoverflow.com
 */

package org.miktim.literadar;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class TrackerActivity extends AppActivity {
    static final String TRACKER_ACTION = "org.literadar.tracker.ACTION";
    static final String TRACKER_EVENT = "org.literadar.tracker.EVENT";
    static final String TRACKER_EXTRA = "json";
    static final String ACTION_TRACKER_CLOSE = "org.literadar.CLOSE_TRACKER";

    private WebView mWebView;

    LocalBroadcastManager mLocalBroadcastManager;

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TRACKER_ACTION)) {
                String msg = intent.getStringExtra(TRACKER_EXTRA);
                mWebView.loadUrl("javascript: Tracker.webview.toTracker('" + msg + "')");
            } else if (action.equals(ACTION_TRACKER_CLOSE) ||
                    action.equals(MainActivity.ACTION_EXIT)) {
                finish();
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);

        MainActivity.sTrackerStarted = true;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.ACTION_EXIT);
        intentFilter.addAction(TRACKER_ACTION);
        intentFilter.addAction(ACTION_TRACKER_CLOSE);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mWebView = findViewById(R.id.webView);

        mWebView.getSettings().setAllowFileAccess(false);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setGeolocationEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
        String url = getIntent().getStringExtra("url"); //MainActivity.sSettings.trackerUrl;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { // Android 4.4
            WebView.setWebContentsDebuggingEnabled(true);
//            if (BuildConfig.DEBUG) {
//                url = url.replace("mode=", "mode=debug");
//            }
        }

        mWebView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript: Tracker.webview.fromTracker = function(event) {Android.fromTracker(event);};");
            }
        });

// https://developer.android.com/develop/ui/views/layout/webapps/webview#UsingJavaScript
        mWebView.addJavascriptInterface(new FromTracker(this), "Android");

        mWebView.loadUrl(url);
    }

    public class FromTracker {
        Context mContext;

        /** Instantiate the interface and set the context. */
        FromTracker(Context c) {
            mContext = c;
        }

        /** get from WebPage */
        @JavascriptInterface
        public void fromTracker(String json) {
            Intent intent = new Intent(TRACKER_EVENT);
            intent.putExtra(TRACKER_EXTRA, json);
            MainActivity.sendBroadcast(mContext, intent);

// todo error event
            Log.d("From tracker", json);
        }
    }

    @Override
    public void onBackPressed() {
            moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyWebView();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        MainActivity.sTrackerStarted = false;
    }

    // https://stackoverflow.com/questions/17418503/destroy-webview-in-android
    public void destroyWebView() {

        // Make sure you remove the WebView from its parent view before doing anything.
//        mWebContainer.removeAllViews();
        mWebView.clearHistory();

        // NOTE: clears RAM cache, if you pass true, it will also clear the disk cache.
        // Probably not a great idea to pass true if you have other WebViews still alive.
//        mWebView.clearCache(true);

        // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
//        mWebView.loadUrl("about:blank");

//        mWebView.onPause();
//        mWebView.removeAllViews();
//        mWebView.destroyDrawingCache(); // has been deprecated

        // NOTE: This pauses JavaScript execution for ALL WebViews,
        // do not use if you have other WebViews still alive.
        // If you create another WebView after calling this,
        // make sure to call mWebView.resumeTimers().
//        mWebView.pauseTimers();

        // NOTE: This can occasionally cause a segfault below API 17 (4.2)
        if (android.os.Build.VERSION.SDK_INT >= 17)
            mWebView.destroy();

        // Null out the reference so that you don't end up re-using it.
        mWebView = null;
    }

}
