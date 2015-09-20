package com.melnykov.fab.sample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

/**
 * Accepts alarms on the due date of the debt
 */
public class DueDateAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String uuid = intent.getData().getSchemeSpecificPart();
        String title = intent.getStringExtra(Debt.KEY_TITLE);
        String owner = intent.getStringExtra(Debt.KEY_OWNER);
        String phone = intent.getStringExtra(Debt.KEY_PHONE);
        String tabTag = intent.getStringExtra(Debt.KEY_TAB_TAG);

        String firstPart;
        String preposition;
        if (tabTag.equals(Debt.OWE_ME_TAG)) {
            firstPart = "Get your ";
            preposition = " from ";
        }
        else {
            firstPart = "Return ";
            preposition = " to ";
        }

        // Raise the notification about the debt
        createNotification(context, firstPart + title, preposition + owner, title, uuid, owner, phone, tabTag);
    }

    /**
     * Creates and shows notification to the user.
     *  @param context app context for the intent
     * @param title   short content
     * @param text    few more details
     * @param alert   shows on the top bar for one second
     * @param uuid    must be unique
     * @param tabTag
     */
    private void createNotification(Context context, String title, String text, String alert, String uuid, String owner, String phone, String tabTag) {
        Intent intent = new Intent(context, EditDebtActivity.class);
//        intent.setFlags(/*Intent.FLAG_ACTIVITY_REORDER_TO_FRONT*/ /*Intent.FLAG_ACTIVITY_SINGLE_TOP | */Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int alarmId = uuid.hashCode();
        intent.putExtra(Debt.KEY_UUID, uuid);
        intent.putExtra(Debt.KEY_TAB_TAG, tabTag);

        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0, intent
                , PendingIntent.FLAG_UPDATE_CURRENT);
        Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
        PendingIntent notificationCallIntent = PendingIntent.getActivity(context, 0, dial
                , PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(context)
                .setContentTitle(title)
                .setTicker(alert)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(notificationIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .addAction(0, "Call " + owner, notificationCallIntent) //todo contact's phone
                .setAutoCancel(true)
                .build();
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(alarmId, notification); //todo check int cast, make unique
    }
}
