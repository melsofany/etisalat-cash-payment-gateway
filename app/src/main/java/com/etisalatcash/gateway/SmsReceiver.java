package com.etisalatcash.gateway;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;

import java.util.Locale;

public class SmsReceiver extends BroadcastReceiver {

    public static final String CHANNEL_PAYMENTS = "payments";
    public static final String ACTION_REFRESH = "com.etisalatcash.gateway.REFRESH";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) return;

        Object[] pdus = (Object[]) extras.get("pdus");
        if (pdus == null || pdus.length == 0) return;
        String format = extras.getString("format");

        String sender = null;
        StringBuilder body = new StringBuilder();
        for (Object pdu : pdus) {
            SmsMessage msg = format != null
                    ? SmsMessage.createFromPdu((byte[]) pdu, format)
                    : SmsMessage.createFromPdu((byte[]) pdu);
            if (msg == null) continue;
            if (sender == null) sender = msg.getDisplayOriginatingAddress();
            String part = msg.getMessageBody();
            if (part != null) body.append(part);
        }

        final Transaction t = SmsParser.parse(sender, body.toString());
        if (t == null) return;

        TransactionStore.save(context, t);
        showPaymentNotification(context, t);

        final Context appCtx = context.getApplicationContext();
        final PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                WebhookSender.post(TransactionStore.getWebhookUrl(appCtx), t);
            } finally {
                pending.finish();
            }
        }).start();

        context.sendBroadcast(new Intent(ACTION_REFRESH).setPackage(context.getPackageName()));
    }

    static void showPaymentNotification(Context context, Transaction t) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_PAYMENTS, context.getString(R.string.channel_payments),
                    NotificationManager.IMPORTANCE_HIGH));
        }
        String amount = String.format(Locale.US, "%.2f", t.amount);
        String text = "مبلغ " + amount + " جنيه"
                + (t.fromPhone != null ? " من " + t.fromPhone : "")
                + (t.reference != null ? " — مرجع " + t.reference : "");

        PendingIntent pi = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_PAYMENTS)
                : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("تم استلام دفعة جديدة 💰")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi);
        nm.notify((int) (t.time % Integer.MAX_VALUE), builder.build());
    }
}
