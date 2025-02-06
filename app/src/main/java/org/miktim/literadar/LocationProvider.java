package org.miktim.literadar;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class LocationProvider {

    public interface Handler {
        void onLocationChanged(Location location);
        void onOutOfLocationService();
    }

    public LocationProvider(Context context, Handler handler){
        mHandler = handler;
        mSatProvider = new Provider(context, LocationManager.GPS_PROVIDER);
        mNetProvider = new Provider(context, LocationManager.NETWORK_PROVIDER);
    }

    public void connect (long minTime, float minDistance) {
        mDistance = minDistance;
        mTimeout = minTime;
        disconnect();
        mSatProvider.connect(mTimeout, mDistance);
        mNetProvider.connect(mTimeout, mDistance);

        mTimer = new Timer();
        TimerTask timerTask = new TimerTask() {public void run() { updateLocation(); }};
        mTimer.scheduleAtFixedRate(timerTask, mTimeout, mTimeout);
    }

    public void disconnect() {
        mSatProvider.disconnect();
        mNetProvider.disconnect();
        if (mTimer != null)
            try { mTimer.cancel(); } catch (Exception ignore) {}
    }

    private void updateLocation() {
        try {
//        if (!mSatProvider.isReachable() && !mNetProvider.isReachable())
//            mHandler.onOutOfLocationService();
            mLastLocation = mSatProvider.getProviderLocation();
            if (mLastLocation == null) {
                mLastLocation = mNetProvider.getProviderLocation();
            }
            if (mLastLocation == null) {
                debug("NULL location " + String.format("%tT", System.currentTimeMillis()));
                mHandler.onOutOfLocationService();
            } else {
                debug(mLastLocation.getProvider() + " " + String.format("%tT %tT", mLastLocation.getTime(), System.currentTimeMillis()));
                mHandler.onLocationChanged(mLastLocation);
            }
        } catch (Throwable ignore) {
// todo ??
//            t.printStackTrace();
        }
    }

    private final Handler mHandler;
    private long mTimeout; // milliseconds
    private float mDistance; // meters
    private Location mLastLocation = null;
    private Timer mTimer;

    private final Provider mSatProvider;
    private final Provider mNetProvider;

private void debug(String msg) { Log.d("LocationProvider", msg); }

    private static class Provider implements LocationListener {
        String name;
        boolean permitted = true;
        boolean connected = false;
        boolean reachable = false;
        LocationManager manager;
        Location location = new Location("Null");
        long time;

private void debug(String msg) { Log.d("LocationProvider " + name, msg); }

        Provider(Context context, String providerName){
            name = providerName;
            manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        @Override
        public void onLocationChanged(Location location) {
debug("onLocationChanged");
            if (location != null && location.getTime() > this.location.getTime())
                this.location = location;
        }
        @Override
        public void onStatusChanged(String s, int i, android.os.Bundle b) {
debug("onStatusChanged: " + s);
        }
        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }

        boolean isPermitted() {
            return permitted;
        }
        boolean isConnected() {
            return connected;
        }
        boolean isEnabled() {
            return isPermitted() && manager.isProviderEnabled(name);
        }
        boolean isReachable() {
debug((reachable ? "Reachable" : "UnReachable"));
            return reachable && isEnabled();
        }

        void connect(long minTime, float minDistance) {
            time = minTime;
            if (isPermitted()) {
//                if (isConnected()) disconnect(); //??
                try {
                    manager.requestLocationUpdates(name, time-100, minDistance, this);
//                    location.setTime(System.currentTimeMillis());
                    time = System.currentTimeMillis();
                    connected = true;
                } catch (SecurityException e) {
                    e.printStackTrace();
                    connected = false;
                    permitted = false;
                }
            }
        }

        void disconnect() {
            manager.removeUpdates(this);
            connected = false;
            reachable = false;
        }

        Location getProviderLocation() {
            Location newLocation;
            if (isEnabled() && isConnected()) {
                try {
                    newLocation = manager.getLastKnownLocation(name);
                    if (newLocation != null
                            && newLocation.getTime() > time) {
                        reachable = true;
                        time = newLocation.getTime();
debug("New Location "+String.format("%tT",newLocation.getTime()));
                        return location;
                    } else {
debug("Null or old location "+String.format("%tT",System.currentTimeMillis()));
                        reachable = false;
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
            return null;
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
