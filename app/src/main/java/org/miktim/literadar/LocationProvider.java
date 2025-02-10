package org.miktim.literadar;

import static java.lang.String.format;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

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

    private long mMinTime; // milliseconds
    private float mMinDistance; // meters
    private Location mLastLocation = null;
    private Location mLocation = new Location("");
    long mLastTime = 0;
    private Timer mTimer;

    public LocationProvider(Context context, Handler handler){
        mContext = context;
        mHandler = handler;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        List<String> names = mLocationManager.getProviders(true);
        for(String providerName : names) {
            if(providerName.equals(LocationManager.PASSIVE_PROVIDER)) continue;
            mListeners.add(new ProviderListener(context, providerName));
        }
    }
    void renewLocation(){
        mLocation = new Location("");
        mLocation.setAccuracy(Float.MAX_VALUE);
        mLastTime = System.currentTimeMillis();
    }

    public void connect (long minTime, float minDistance) {
        mMinDistance = minDistance;
        mMinTime = minTime;
        disconnect();
        for (ProviderListener listener : mListeners) {
            listener.connectListener(minTime, 0);// todo minDistance);
        }
        renewLocation();
        mTimer = new Timer();
        TimerTask timerTask = new TimerTask() {public void run() { updateLocation(); }};
        mTimer.scheduleAtFixedRate(timerTask, mMinTime, mMinTime);
    }

    public void disconnect() {
        for (ProviderListener listener : mListeners) {
            listener.disconnectListener();
        }
        if (mTimer != null)
            try { mTimer.cancel(); } catch (Exception ignore) {}
    }

    private void updateLocation() {
        if (!mLocation.getProvider().isEmpty()) {
            mLastLocation = mLocation;
            Log.d("LocationProvider",
                    format("%s %TT", mLocation.getProvider(),new Date(mLastLocation.getTime())));
        }
        mHandler.onLocationChanged(mLastLocation);
        renewLocation();
    }

    private class ProviderListener implements LocationListener {
        String mProviderName;
        Boolean mConnected = false;

        ProviderListener(Context context, String providerName){
            this.mProviderName = providerName;
        }

        boolean isConnected() {
            return mConnected;
        }

        @Override
        public void onLocationChanged(Location location) {
            synchronized (mLocation) {
                if (location != null
                    && location.getTime() > mLastTime
                    && location.getAccuracy() < mLocation.getAccuracy() )
                    mLocation = location;
            }
        }
        @Override
        public void onStatusChanged(String s, int i, android.os.Bundle b) {
        }
        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }

        boolean isEnabled() {
            return mLocationManager.isProviderEnabled(mProviderName);
        }
        boolean isReachable() {
// todo
//            return mLocationManager.isReachable(providerName);
            return true;
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
                mLocationManager.removeUpdates((LocationListener) this);
                mConnected = false;
            }
        }
    }
/*

    private void setLastKnownLocation() throws SecurityException, NullPointerException {
        Location location;
        location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null && location.getTime() > mLocation.getTime()) {
debug("GPS");
            mLocation = location;
        } else {
            location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null && location.getTime() > mLocation.getTime()) {
debug("NETWORK: "+String.format("%tT",location.getTime()));
                mLocation = location;
            }
        }

        if (location == null) ((Listener) mContext).onOutOfLocationService();

        float distanceToFence = mLocation.distanceTo(mGpsFence);
        if (distanceToFence > RADIUS_GPS_OFF) {
            mGpsFence = mLocation;
            if (!mGpsOn) {
debug("GPSOn distanceToFence: " + distanceToFence + " radius: " + RADIUS_GPS_OFF);
                mGpsOn = true;
                updateManager(mTime, mDistance);
            }
        } else if (mGpsOn
                && (mLocation.getTime() - mGpsFence.getTime()) > DELAY_GPS_OFF
                && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
debug("GPSOff distanceToFence: " + distanceToFence + " radius: " + RADIUS_GPS_OFF);
            mGpsOn = false;
            updateManager(mTime, mDistance);
        }
    }

    public void delay(long millis) {
        long endTime = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    wait(endTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }
        }
    }
*/
}
