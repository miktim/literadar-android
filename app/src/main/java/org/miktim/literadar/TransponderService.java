/*
 * LiteRadar Service, MIT (c) 2021-2025 miktim@mail.ru
 * Thanks to:
 *   developer.android.com
 *   stackoverflow.com
 */

package org.miktim.literadar;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.miktim.Notifier;
import org.miktim.udpsocket.UdpSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

public class TransponderService extends Service {
    static String ACTION_PAKET = "org.literadar.paket";
    static String ACTION_PAKET_EXTRA = "json";
    Settings mSettings = MainActivity.sSettings;
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
        mNotifier = new Notifier(this,R.drawable.ic_stat_service,
                "LiteRadar mode:" +
                        getResources().getStringArray(R.array.mode_array)[mSettings.getMode()]);
        mNotifier.setActivity(new Intent(this, SettingsActivity.class));
        mNotifier.setPriority(Notifier.PRIORITY_MAX);

        MainActivity.sService = this;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SettingsActivity.ACTION_EXIT);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);
//        mLocationProvider = new LocationProvider(this,mLocationHandler);
//        mLocationProvider.connect(mSettings.locations.timeout, mSettings.locations.minDistance);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return super.onStartCommand(intent, flags, startId);
    }

    public void startService() {
        mNotifier.startForeground("On air!");

// todo
//        if(settings.mode != Settings.MODE_TRACKER_ONLY) {
            try {
                InetSocketAddress sa = (InetSocketAddress) mSettings.network.getRemoteSocket(mSettings.mode);
                mUdpSocket = new UdpSocket(sa.getPort(),sa.getAddress(), mSettings.network.getLocalSocket());
                mUdpSocket.receive(mUdpSocketHandler);
            } catch (IOException e) {
                e.printStackTrace();
                stopSelf();
            }
 //       }
 //       mDoneService = false;
 //       (new Service(this)).start();
    }

    @Override
    public void onDestroy() {
        MainActivity.sService = null;
        if (mUdpSocket != null) mUdpSocket.close();
        if (mLocationProvider != null) mLocationProvider.disconnect();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        stopForeground(true);
        super.onDestroy();
    }

    class Service extends Thread {
        TransponderService mService;
        Service(TransponderService service) {
            mService = service;
        }
        @Override
        public void run() {
            while (!mDoneService) {
                synchronized (this) {
                    try {
                        wait(500);
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
}