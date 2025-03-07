/*
 * LiteRadar Service, MIT (c) 2021-2025 miktim@mail.ru
 * Thanks to:
 *   developer.android.com
 *   stackoverflow.com
 */

package org.miktim.literadar;

import static java.lang.String.format;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.IBinder;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.miktim.udpsocket.UdpSocket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;

public class TransponderService extends Service {
    static String ACTION_PACKET = "org.literadar.tracker.ACTION";
    static String ACTION_PACKET_EXTRA = "json";
    Settings mSettings;
    Context mContext = this;

    Packet mIncomingPacket = new Packet();
    Packet mOutgoingPacket;
    LocationProvider mLocationProvider;
    LocationProvider.Handler mLocationHandler = new LocationProvider.Handler() {
        @Override
        public void onLocationChanged(Location location) {
            if (location == null) return;
            mOutgoingPacket.updateLocation(
                    location.getTime(),
//                    System.currentTimeMillis(),
                    mSettings.locations.timeout * 2,
                    location.getLatitude(),
                    location.getLongitude(),
                    (int) location.getAccuracy());
            try {
                packetToTracker(mOutgoingPacket);
                if (mUdpSocket != null && mSettings.getMode() != Settings.MODE_TRACKER_ONLY)
                    mUdpSocket.send(mOutgoingPacket.pack());
            } catch (java.security.GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
// todo
                e.printStackTrace();
            }
        }

        @Override
        public void onOutOfLocationService() {

        }
    };

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MainActivity.ACTION_EXIT)) {
                stopSelf();
            } else if (action.equals(SettingsActivity.ACTION_RESTART)) {
                mLocationProvider.disconnect();
                if (mUdpSocket != null) {
                    mUdpSocket.close();
//                    mUdpSocket = null;
                }
                restartService();
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
            try {
                mIncomingPacket.unpack(datagramPacket.getData());
// todo favorites
                packetToTracker(mIncomingPacket);
            } catch (IOException e) {
// todo exception handling
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(UdpSocket udpSocket, Exception e) {
// todo
        }

        @Override
        public void onClose(UdpSocket udpSocket) {

        }
    };

    void packetToTracker(Packet packet) throws IOException {
        if (mSettings.showTracker) {
            String json = packet.toJSON();
            Intent intent = new Intent(ACTION_PACKET);
            intent.putExtra(ACTION_PACKET_EXTRA, json);
            sendBroadcast(mContext, intent);
        }
    }

    public TransponderService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mSettings = MainActivity.sSettings;

        mNotifier = new Notifier(this,
                R.drawable.ic_stat_service,
                notificationTitle() );
        mNotifier.setActivity(MainActivity.sSettingsIntent);
        mNotifier.setPriority(Notifier.PRIORITY_MAX);

        MainActivity.sService = this;

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.ACTION_EXIT);
        intentFilter.addAction(SettingsActivity.ACTION_RESTART);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);

        mLocationProvider = new LocationProvider(this, mLocationHandler);
    }
    String notificationTitle() {
        return  format("%s: %s",
                resString(R.string.app_name),
                getResources().getStringArray(R.array.mode_array)[mSettings.getMode()]);
    }

    String resString(int resString) {
        return (getResources().getString(resString));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotifier.startForeground(resString(R.string.on_air));
        restartService();
        return START_STICKY;//super.onStartCommand(intent, flags, startId);
    }

    public void restartService() {
        try {
            mOutgoingPacket = new Packet(mSettings.getKeyPair(), mSettings.getDisplayName(), mSettings.getIconId());
        } catch (java.security.GeneralSecurityException e) {
            MainActivity.fatalDialog(e);
            e.printStackTrace();
        }
        if (mSettings.getMode() != Settings.MODE_TRACKER_ONLY) {
            try {
                InetSocketAddress sa = (InetSocketAddress) mSettings.network.getRemoteSocket(mSettings.mode);
                mUdpSocket = new UdpSocket(sa.getPort(), sa.getAddress(), mSettings.network.getLocalSocket());
                if (mSettings.getMode() == Settings.MODE_MULTICAST_MEMBER)
                    mUdpSocket.receive(mUdpSocketHandler);
            } catch (IOException e) {
                e.printStackTrace();
                stopSelf();
            }
        }
        // todo: too small minTime = 5 sec
        mLocationProvider.connect(mSettings.locations.getTimeout() * 1000 * 2, mSettings.locations.getDistance());
        mNotifier.notifyTitle(notificationTitle());
    }

    @Override
    public void onDestroy() {
        MainActivity.sService = null;
        super.onDestroy();
        if (mUdpSocket != null) {
            mUdpSocket.close();
            mUdpSocket = null;
        }
        if (mLocationProvider != null) mLocationProvider.disconnect();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
        stopForeground(true);
    }

    void sendBroadcast(Context context, Intent intent) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
