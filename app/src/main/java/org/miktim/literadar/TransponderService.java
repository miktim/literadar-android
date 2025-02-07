/*
 * LiteRadar Service, MIT (c) 2021-2025 miktim@mail.ru
 * Thanks to:
 *   developer.android.com
 *   stackoverflow.com
 */

package org.miktim.literadar;

import static org.miktim.literadar.MainActivity.sSettings;

import static java.lang.String.format;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.miktim.Notifier;
import org.miktim.udpsocket.UdpSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.List;

public class TransponderService extends Service {
    static String ACTION_PAKET = "org.literadar.paket";
    static String ACTION_PAKET_EXTRA = "json";
    Settings mSettings = sSettings;
    boolean mDoneService = false;

    LocationProvider mLocationProvider;
    LocationProvider.Handler mLocationHandler = new LocationProvider.Handler() {
        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onOutOfLocationService() {

        }
    };

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(SettingsActivity.ACTION_EXIT)) {
                mDoneService = true;
                stopSelf();
            }
        }
    };
    LocalBroadcastManager mLocalBroadcastManager;
    Notifier mNotifier;
    UdpSocket mUdpSocket;
    UdpSocket.Handler mUdpSocketHandler = new UdpSocket.Handler() {
        @Override
        public void onStart(UdpSocket udpSocket) {

        }

        @Override
        public void onPacket(UdpSocket udpSocket, DatagramPacket datagramPacket) {

        }

        @Override
        public void onError(UdpSocket udpSocket, Exception e) {

        }

        @Override
        public void onClose(UdpSocket udpSocket) {

        }
    };

    public TransponderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mNotifier = new Notifier(this, R.drawable.ic_stat_service, format("%s:%s",
                resString(R.string.app_name),
                getResources().getStringArray(R.array.mode_array)[mSettings.getMode()]));
        mNotifier.setActivity(MainActivity.sSettingsIntent);
        mNotifier.setPriority(Notifier.PRIORITY_MAX);

        MainActivity.sService = this;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SettingsActivity.ACTION_EXIT);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
        mLocationProvider = new LocationProvider(this,mLocationHandler);
        mLocationProvider.connect(mSettings.locations.timeout*1000, mSettings.locations.minDistance);
    }

    String resString(int resString) {
        return (getResources().getString(resString));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return super.onStartCommand(intent, flags, startId);
    }

    public void startService() {
        mNotifier.startForeground(resString(R.string.on_air));

// todo
//        if(settings.mode != Settings.MODE_TRACKER_ONLY) {
        try {
            InetSocketAddress sa = (InetSocketAddress) mSettings.network.getRemoteSocket(mSettings.mode);
            mUdpSocket = new UdpSocket(sa.getPort(), sa.getAddress(), mSettings.network.getLocalSocket());
            mUdpSocket.receive(mUdpSocketHandler);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        MainActivity.sService = null;
        if (mUdpSocket != null) {
            mUdpSocket.close();
            mUdpSocket = null;
        }
        if (mLocationProvider != null) mLocationProvider.disconnect();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        stopForeground(true);
        super.onDestroy();
    }

    void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
/*
    class Service {
        Context mContext;
        LocationManager mLocationManager;
        LocationListener mLocationListener = new LocationListener();
        List<String> mAllProviders;
        List<LocationProvider> mLocationProviders;

        Service(Context context) {
            mContext = context;
            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            mAllProviders = mLocationManager.getAllProviders();
            for(String provider : mAllProviders) {
                mLocationProviders.add(new LocationProvider(mContext, new Provider(mContext,provider)));
            }
        }

        public class Provider implements LocationListener {
            String mProviderName;
            LocationManager mLocationManager;

            Provider(Context context, String providerName){
                mProviderName = providerName;
                mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            }
            void connect(long minTime, float minDistance) {
//                time = minTime;
//                if (isPermitted()) {
//                if (isConnected()) disconnect(); //??
                    try {
                        mLocationManager.requestLocationUpdates(minTime, minDistance, this);
//                    location.setTime(System.currentTimeMillis());
//                        time = System.currentTimeMillis();
                    } catch (SecurityException e) {
                        e.printStackTrace();
//                        connected = false;
//                        permitted = false;
                    }
                }

            void disconnect() {
                mLocationManager.removeUpdates(this);
//                connected = false;
//                reachable = false;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onLocationChanged(@NonNull Location location) {

            }

            @Override
            public void onLocationChanged(@NonNull List<Location> locations) {

            }

            @Override
            public void onFlushComplete(int requestCode) {

            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {

            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {

            }
        }
    }

 */
}