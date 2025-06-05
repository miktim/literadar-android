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
import java.net.NetworkInterface;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class TransponderService extends Service {
    Settings mSettings;
    Context mContext = this;

    Packet mOutgoingPacket;
    LocationProvider mLocationProvider;
    LocationProvider.Handler mLocationHandler = new LocationProvider.Handler() {
        @Override
        public void onLocationChanged(Location location) {
            resetError(R.string.err_geolocation);
            location = mLocationProvider.getLastLocation();
            if (location == null) {
                return;
            }

            mOutgoingPacket.updateLocation(
                    System.currentTimeMillis(),//location.getTime(),
                    mSettings.locations.getMinTime() * 2,
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy());
            try {
                packetToTracker(mOutgoingPacket);
                if (mUdpSocket != null && mSettings.getMode() != Settings.MODE_TRACKER_ONLY) {
                    byte[] packetBytes = mOutgoingPacket.pack();
                    mUdpSocket.send(packetBytes);
                    resetError(R.string.err_network);
                }
            } catch (java.security.GeneralSecurityException e) {
// todo: fatal
                e.printStackTrace();
            } catch (IOException e) {
                notifyError(R.string.err_network);
            }
        }

        @Override
        public void onOutOfLocationService() {
            notifyError(R.string.err_geolocation);
        }
    };

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MainActivity.ACTION_EXIT)) {
                stopSelf();
            } else if (action.equals(MainActivity.ACTION_RESTART)) {
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
            resetError(R.string.err_network);
            try {
                Packet incomingPacket = new Packet();
                incomingPacket.unpack(Arrays.copyOf(datagramPacket.getData(), datagramPacket.getLength()));
                Favorites.Entry entry = mSettings.favorites.updateEntry(incomingPacket);
                incomingPacket.name = entry.getName();
                boolean isFavorite = entry.getFavorite();
                incomingPacket.iconid = isFavorite ? 1 : 0; // green : gray
                if(!isFavorite && mSettings.favorites.favoritesOnly) return;
                packetToTracker(incomingPacket);
            } catch (IOException e) {
                notifyError(R.string.err_network);
            } catch (GeneralSecurityException e) {
// todo statistics: bad packet
                e.printStackTrace();
            }
        }

        @Override
        public void onError(UdpSocket udpSocket, Exception e) {
            notifyError(R.string.err_network);
        }

        @Override
        public void onClose(UdpSocket udpSocket) {

        }
    };
    int mError;
    void notifyError(int msgId) {
        if(mError == 0) {// || mError == msgId) {
            mError = msgId;
            mNotifier.alert(getString(msgId));
        } else Notifier.beep();
    }
    void resetError(int msgId) {
        if(mError == msgId) {
            mError = 0;
            mNotifier.notify(getString(R.string.on_air));
        }
    }
    void packetToTracker(Packet packet) throws IOException {
        if (mSettings.getTrackerEnabled()) {
            String json = packet.toJSON();
            Intent intent = new Intent(TrackerActivity.TRACKER_ACTION);
            intent.putExtra(TrackerActivity.TRACKER_EXTRA, json);
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

        MainActivity.sServiceStarted = true;

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.ACTION_EXIT);
        intentFilter.addAction(MainActivity.ACTION_RESTART);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, intentFilter);

        mLocationProvider = new LocationProvider(MainActivity.mContext, mLocationHandler);
    }

    String notificationTitle() {
        return  format("%s: %s",
                getString(R.string.app_name),
                getResources().getStringArray(R.array.mode_array)[mSettings.getMode()]);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNotifier.startForeground(getString(R.string.on_air));
        restartService();
        return START_STICKY;//super.onStartCommand(intent, flags, startId);
    }

    public void restartService() {
        try {
            mOutgoingPacket = new Packet(mSettings.getKeyPair(), mSettings.getName(), mSettings.getIconId());
        } catch (java.security.GeneralSecurityException e) {
            e.printStackTrace();
            MainActivity.fatal(this,e);
        }
        int mode = mSettings.getMode();
        if (mode != Settings.MODE_TRACKER_ONLY) {
            try {
                InetSocketAddress remoteSocket = mSettings.network.getRemoteSocket(mode);
                NetworkInterface intF = mSettings.network.getNeworkInterface();
// intF is null if unavailable
// todo check interface available
                mUdpSocket = new UdpSocket(remoteSocket, intF);
                if(mUdpSocket.isMulticast()) {
                    mUdpSocket.setTimeToLive(mSettings.network.getTimeToLive());
                }
                if (mode == Settings.MODE_MULTICAST_MEMBER) {
                    mUdpSocket.receive(mUdpSocketHandler);
                }
            } catch (IOException e) {
// todo check interface available
                e.printStackTrace();
                MainActivity.fatal(this, e);
            }
        }
        mLocationProvider.connect(
                mSettings.locations.getMinTime() * 1000L,
//                0); // todo min distance
              mSettings.locations.getMinDistance());
        mNotifier.notifyTitle(notificationTitle());
    }

    @Override
    public void onDestroy() {
        MainActivity.sServiceStarted = false;
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
}
