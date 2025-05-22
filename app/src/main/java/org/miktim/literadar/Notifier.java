/**
 * Notifier class, MIT (c) 2019-2025 miktim@mail.ru
 *
 * Simple Android notifier.
 * Usage:
 *   mNotifier = new Notifier(this, R.mipmap.ic_launcher, "My title");
 *   mNotifier.notify("...some message");
 *   ...
 *   mNotifier.alert("...another message"); // notify with beep
 *   ...
 *   mNotifier.cancel();
 *
 * Usage in the Android foreground service:
 *   mNotifier = new Notifier(this, R.mipmap.ic_stat_notify, "My title");
 *   mNotifier.setActivity(MyActivity.class);
 *   mNotifier.notify("Service started");
 *   startForeground(mNotifier.getNotificationId(), mNotifier.getNotification());
 *   ...
 *   mNotifier.alert("...something is wrong"); // notify with beep
 *   ...
 *   mNotifier.notify("Service stopped");
 *   ...
 *   stopForeground(true);
 * // cancel the notification in any way
 *
 * Created: 2019-03-15
 */

 package org.miktim.literadar;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;

import java.util.Timer;
import java.util.TimerTask;

import androidx.core.app.NotificationCompat;

public class Notifier {

    public static final int PRIORITY_MIN = 0;
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_DEFAULT = 2;
    public static final int PRIORITY_MAX = 3;
    public static final String CHANNEL_NAME = "NotifierChannel";

    private static volatile int ZERO_ID = 0;
    private final int mNotificationId = ++ZERO_ID;

    private static final int ALERT_BEEP_VOLUME = 75;
    private final Context mContext;
    private final String mChannelId;
    private String mTitle = "";
    private String mMessage = "";
    private final int mSmallIcon;
    private PendingIntent mPendingIntent;
    private int mPriority = PRIORITY_DEFAULT;

//    private NotificationCompat.Builder mBuilder;
    private final NotificationManager mManager;
//    private Notification mNotification = null;

    // https://developer.android.com/guide/topics/ui/notifiers/notifications.html#Required
    public Notifier(Context context, int smallIcon, String title) {
        mContext = context;
        mChannelId = CHANNEL_NAME + mNotificationId;
        mSmallIcon = smallIcon;
        mTitle = title;

        mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    NotificationCompat.Builder getBuilder() {
        prepareChannel(mContext, mChannelId, mPriority);

        NotificationCompat.Builder builder =
                (new NotificationCompat.Builder(mContext, mChannelId))
                .setContentTitle(mTitle)
                .setContentText(mMessage)
                .setContentIntent(mPendingIntent).setOngoing(true)
                .setPriority(PRIORITY[mPriority]);
        if(mSmallIcon != 0) {
            builder.setSmallIcon(mSmallIcon);
        }
        return builder;
    }

    NotificationChannel mChannel = null;
    @TargetApi(26)
    private void prepareChannel(Context context, String id, int priority) {
        if (Build.VERSION.SDK_INT < 26) return; // 26+ Build.VERSION_CODES.O
        final int[] IMPORTANCE = new int[] {
                NotificationManager.IMPORTANCE_MIN,
                NotificationManager.IMPORTANCE_LOW,
                NotificationManager.IMPORTANCE_DEFAULT,
                NotificationManager.IMPORTANCE_MAX
        };
        String appName = context.getString(R.string.app_name);
        String description = appName + " notification channel";//context.getString(R.string.notifications_channel_description);

            if (mChannel == null) {
                mChannel = new NotificationChannel(id, appName, IMPORTANCE[priority]);
                mChannel.setDescription(description);
                mChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                mManager.createNotificationChannel(mChannel);
            }
//        }
    }

    // https://developer.android.com/guide/topics/ui/notifiers/notifications#Actions
    public PendingIntent setActivity(Class activityCls) {
        Intent intent = new Intent(mContext, activityCls);
        return setActivity(intent);
    }

    public PendingIntent setActivity(Intent activityIntent) {
        mPendingIntent = PendingIntent.getActivity(
                mContext, 0, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return mPendingIntent;
    }

    public PendingIntent setActivity(PendingIntent activityPendingIntent) {
        mPendingIntent = activityPendingIntent;
        return mPendingIntent;
    }

// https://developer.android.com/guide/topics/ui/notifiers/notifications.html#Updating
    public void notify(String text) {
        mMessage = text;
        show();
    }

    public void notifyTitle(String title) {
        mTitle = title;
        show();
    }

    Notification show() {
        Notification notification = getBuilder().build();
        try {
            mManager.notify(mNotificationId, notification);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return notification;
    }

    public void startForeground(String text) {
        mMessage = text;
        if(mContext instanceof Service) {
            ((Service)mContext).startForeground(mNotificationId, show());
        }
    }

    public void alert(String text) {
// https://stackoverflow.com/questions/29509010/how-to-play-a-short-beep-to-android-phones-loudspeaker-programmatically
// See also:
// https://stackoverflow.com/questions/11964623/audioflinger-could-not-create-track-status-12
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
    public NotificationManager getManager() {
        return mManager;
    }

    public void clearActivity() {
        mPendingIntent = null;
        show();
    }

    private static final int[] PRIORITY = new int[] {
            NotificationCompat.PRIORITY_MIN,
            NotificationCompat.PRIORITY_LOW,
            NotificationCompat.PRIORITY_DEFAULT,
            NotificationCompat.PRIORITY_MAX
    };

    public void setPriority(int notifierPriority) {
        if(notifierPriority < PRIORITY_MIN || notifierPriority > PRIORITY_MAX)
            throw new IllegalArgumentException();
        mPriority = notifierPriority;
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
