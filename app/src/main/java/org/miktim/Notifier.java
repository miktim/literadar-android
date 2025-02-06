/**
 * Notifier class
 *
 * Simple Android notifier.
 * Usage:
 *   mNotifier = new Notifier(this, R.mipmap.ic_launcher, "My title");
 *   mNotifier.notify("...some message");
 *   ...
 *   mNotifier.notify("...another message");
 *   ...
 *   mNotifier.cancel();
 *
 * Usage in the Android foreground service:
 *   mNotifier = new Notifier(this, R.mipmap.ic_stat_notify, "My title");
 *   mNotifier.setActivity(MyActivity.class);
 * // WARNING: the corresponding IMPORTANCE_ used but not tested
 *   mNotifier.setPriority(Notifier.PRIORITY_LOW);
 *   mNotifier.notify("Service started");
 *   startForeground(mNotifier.getNotificationId(), mNotifier.getNotification());
 *   ...
 *   mNotifier.alert("...something is wrong");
 *   ...
 *   mNotifier.clearActivity();
 *   mNotifier.notify("Service stopped");
 *   stopForeground(false);
 * // cancel the notification in any way
 *
 * Author:  miktim@mail.ru
 * Created: 2019-03-15
 * Updated:
 *   2019-04-02 release ToneGenerator (alert method)
 *
 * License: MIT
 */

 package org.miktim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;

import java.util.Timer;
import java.util.TimerTask;

import androidx.core.app.NotificationCompat;

public class Notifier {

    public static final int PRIORITY_MIN = 0;
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_DEFAULT = 2;
    public static final int PRIORITY_MAX = 3;
    public static final String CHANNEL_NAME = "NotifierChannel";

    private static int ZERO_ID = 0;
    private int mNotificationId = ++ZERO_ID;

    private static final int ALERT_BEEP_VOLUME = 30;
    private Context mContext;
    private String mChannelId;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mManager;
    private Notification mNotification = null;

    // https://developer.android.com/guide/topics/ui/notifiers/notifications.html#Required
    public Notifier(Context context, int smallIcon, String title) {
        mContext = context;
        mChannelId = CHANNEL_NAME + mNotificationId;
        mBuilder = new NotificationCompat.Builder(context, mChannelId)
                .setContentTitle(title);
        if(smallIcon != 0) {
            mBuilder.setSmallIcon(smallIcon);
        }

        mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    // https://developer.android.com/guide/topics/ui/notifiers/notifications#Actions
    public PendingIntent setActivity(Class activity) {
        Intent intent = new Intent(mContext, activity);
        return setActivity(intent);
    }

    public PendingIntent setActivity(Intent activityIntent) {
        PendingIntent notifyPendingIntent =
                PendingIntent.getActivity(
                        mContext, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT+PendingIntent.FLAG_IMMUTABLE);
        mBuilder.setContentIntent(notifyPendingIntent).setOngoing(true) ;
        return notifyPendingIntent;
    }


// https://developer.android.com/guide/topics/ui/notifiers/notifications.html#Updating
    public void notify(String text) {
        mBuilder.setContentText(text);
        mNotification = mBuilder.build();
        try {
            mManager.notify(mNotificationId, mNotification);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void startForeground(String text) {
        notify(text);
        if(mContext instanceof Service) {
            ((Service)mContext).startForeground(mNotificationId, mNotification);
        }
    }

    public void alert(String text) {
// https://stackoverflow.com/questions/29509010/how-to-play-a-short-beep-to-android-phones-loudspeaker-programmatically
// See also:
// https://stackoverflow.com/questions/11964623/audioflinger-could-not-create-track-status-12
//        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 75);
//        toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT,100);
        playTone(AudioManager.STREAM_NOTIFICATION, ALERT_BEEP_VOLUME
                ,ToneGenerator.TONE_CDMA_ABBR_ALERT,100);
        notify(text);
    }

    // https://developer.android.com/training/notify-user/build-notification.html#Removing
    public void cancel() {
        mManager.cancel(mNotificationId);
    }

    public String getChannelId() { return mChannelId; }
    public int getNotificationId() { return mNotificationId; }
    public NotificationCompat.Builder getBuilder() { return mBuilder; }
    public NotificationManager getManager() {
        return mManager;
    }

// returns NULL or LAST SHOWN notification
    public Notification getNotification() { return mNotification;  }

    public void clearActivity() {
// (Activity).mContext.finish();
        mBuilder.setContentIntent(null);
    }

// https://android-developers.googleblog.com/2018/12/effective-foreground-services-on-android_11.html
    private static final int[] PRIORITY = new int[] {
            NotificationCompat.PRIORITY_MIN,
            NotificationCompat.PRIORITY_LOW,
            NotificationCompat.PRIORITY_DEFAULT,
            NotificationCompat.PRIORITY_MAX
    };

    public void setPriority(int notifierPriority) {
        mBuilder.setPriority(PRIORITY[notifierPriority]);
// https://developer.android.com/training/notify-user/channels#CreateChannel
// Create the NotificationChannel, but only on API 26+ (Android 8.0) because
// the NotificationChannel class is new and not in the support library
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
// NOT TESTED!!!
           final int[] IMPORTANCE = new int[] {
                    NotificationManager.IMPORTANCE_MIN,
                    NotificationManager.IMPORTANCE_LOW,
                    NotificationManager.IMPORTANCE_DEFAULT,
                    NotificationManager.IMPORTANCE_MAX
            };
// "If you create a new channel with this same id, the deleted channel will be un-deleted
// with all of the same settings it had before it was deleted."
            if (mManager.getNotificationChannel(mChannelId) != null) {
                mManager.deleteNotificationChannel(mChannelId);
                mChannelId = CHANNEL_NAME + (++ZERO_ID);
                mBuilder.setChannelId(mChannelId); // ????
            }
            NotificationChannel channel = new NotificationChannel(
                    mChannelId, CHANNEL_NAME, IMPORTANCE[notifierPriority]);
//            channel.setDescription("Channel description");
// Register the channel with the system; you can't change the importance
// or other notification behaviors after this
            mManager.createNotificationChannel(channel);
        }
    }

    public static void beep() {
        beep( ALERT_BEEP_VOLUME, 100, ToneGenerator.TONE_CDMA_ABBR_ALERT);
    }

    public static void beep(int volume, int duration, int tone) {
        playTone(AudioManager.STREAM_NOTIFICATION, volume, tone, duration);
    }

    private static class releaseToneGen extends TimerTask {
        private ToneGenerator toneGen;
        releaseToneGen(ToneGenerator toneGen) { this.toneGen = toneGen; }
        public void run() { toneGen.release(); }
    }
    private static void playTone(int streamType, int volume, int toneType, int durationMs) {
        try {
            ToneGenerator toneGen = new ToneGenerator(streamType ,volume);
            (new Timer()).schedule((new releaseToneGen (toneGen)), durationMs);
            toneGen.startTone(toneType, durationMs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
