package com.etisalatcash.gateway;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

/**
 * Keeps the app process alive so the SMS receiver and webhook delivery keep
 * working while ColorOS/Oppo battery management is aggressive. The actual
 * payment detection happens in SmsReceiver, which the system wakes directly.
 */
public class GatewayService extends Service {

    public static final String CHANNEL_SERVICE = "gateway_service";
    private static final int NOTIFICATION_ID = 1001;
    private static final String PREFS = "gateway_prefs";
    private static final String KEY_ENABLED = "service_enabled";

    public static boolean isEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(Context ctx, boolean enabled) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static void start(Context ctx) {
        setEnabled(ctx, true);
        Intent i = new Intent(ctx, GatewayService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            ctx.startForegroundService(i);
        } else {
            ctx.startService(i);
        }
    }

    public static void stop(Context ctx) {
        setEnabled(ctx, false);
        ctx.stopService(new Intent(ctx, GatewayService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForegroundWithNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundWithNotification();
        return START_STICKY;
    }

    private void startForegroundWithNotification() {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_SERVICE)
                : new Notification.Builder(this);
        Notification n = builder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.service_notification_title))
                .setContentText(getString(R.string.service_notification_text))
                .setContentIntent(pi)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_SERVICE, getString(R.string.channel_service),
                    NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
