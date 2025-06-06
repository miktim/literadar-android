/*
 * LiteRadar LocationProvider, MIT (c) 2021-2025 miktim@mail.ru
 *  Constructor:
 *    LocationProvider(Context context, LocationProvider.Handler handler);
 *  Interface:
 *    LocationProvider.Handler {
 *       void onLocationChanged(Location location);
 *          - location can be null
 *       void onOutOfService(); }
 *  Methods:
 *    void connect(long minTimeMillis, float minDistanceMeters);
 *    void disconnect();
 *    boolean isOutOfService();
 *    location getLastLocation();
 *      - returns last detected location or null
 *
 * Thanks to:
 *   developer.android.com
 *   stackoverflow.com
 */

package org.miktim.literadar;

import static java.lang.String.format;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationListenerCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LocationProvider {

    public interface Handler {
        void onLocationChanged(Location location);

        void onOutOfLocationService();
    }

    Context mContext;
    Handler mHandler;
    LocationManager mLocationManager;
    List<ProviderListener> mListeners = new ArrayList<>();
    int mEnabledProviders = 0;

    private Location mLastLocation = null;
    private Location mLocation = new Location("");
    long mLastTime = 0;
    private Timer mTimer;

    public LocationProvider(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;

        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        List<String> names = mLocationManager.getProviders(false);
        mEnabledProviders = 0;
        for (String providerName : names) {
            if (providerName.equals(LocationManager.PASSIVE_PROVIDER)) continue;
            mListeners.add(new ProviderListener(providerName));
            if (mLocationManager.isProviderEnabled(providerName))
                mEnabledProviders++;
        }
    }

    void renewLocation() {
        //       synchronized(mLocation) { // todo
        mLocation = new Location("");
        mLocation.setAccuracy(Float.MAX_VALUE);
        mLastTime = System.currentTimeMillis();
        //       }
    }

    @SuppressLint("MissingPermission")
    public Location getLastLocation() {
        return mLastLocation;

//        if (mLastLocation != null) return mLastLocation;
//        return mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
    }

    public boolean isOutOfService() {
        if(mListeners == null) return true;
        for (ProviderListener listener : mListeners) {
            if(listener.isEnabled()) return false;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public void connect (long minTime, float minDistance) {

        disconnect();

        for (ProviderListener listener : mListeners) {
            listener.connectListener(minTime, minDistance);//todo
        }
        renewLocation();
        mTimer = new Timer();
        TimerTask timerTask = new TimerTask() {public void run() { updateLocation(); }};
        mTimer.scheduleAtFixedRate(timerTask, minTime, minTime);
    }

    public void disconnect() {
        if (mLocationManager == null || mListeners == null) return; // not connected
        for (ProviderListener listener : mListeners) {
            listener.disconnectListener();
        }

//        mLocationManager = null;
        if (mTimer != null)
            try { mTimer.cancel(); } catch (Exception ignore) {}
    }

    private void updateLocation() {
        if (!mLocation.getProvider().isEmpty()) {
//            synchronized(mLocation) {
                mLastLocation = mLocation;
//            }
            renewLocation();
            mHandler.onLocationChanged(mLastLocation);
            Log.d("LocationProvider",
                    format("%s %TT", mLastLocation.getProvider(),new Date(mLastLocation.getTime())));
        } else {
            if (isOutOfService()) {
                 mHandler.onOutOfLocationService();
            } else {
                mHandler.onLocationChanged(null);
            }
        }
    }

    private class ProviderListener implements LocationListenerCompat {
        String mProviderName;
        Boolean mConnected = false;

        ProviderListener(String providerName){
            this.mProviderName = providerName;
        }

        boolean isConnected() {
            return mConnected;
        }

        @Override
        public void onLocationChanged(Location location) {
 //           synchronized (mLocation) {
                if (location != null
 //                   && location.getTime() >= mLastTime //todo
                    && location.getAccuracy() < mLocation.getAccuracy() )
                    mLocation = location;
 //           }
        }
        @Override
        public void onStatusChanged(String s, int i, android.os.Bundle b) {
        }
        @Override
        public void onProviderEnabled(String s) {
            mEnabledProviders++;
        }

        @Override
        public void onProviderDisabled(String s) {
            mEnabledProviders--;
        }

        boolean isEnabled() {
            return mLocationManager.isProviderEnabled(mProviderName);
        }

        void connectListener(long minTime, float minDistance) {
            try {
                mLocationManager.requestLocationUpdates(mProviderName, minTime, minDistance, this);
                mConnected = true;
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        void disconnectListener() {
            if(isConnected()) {
                mLocationManager.removeUpdates( this);
                mConnected = false;
            }
        }
    }
}
