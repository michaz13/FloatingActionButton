package com.melnykov.fab.sample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;

/**
 * Accepts alarms on the due date of the debt.
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
        } else {
            firstPart = "Return ";
            preposition = " to ";
        }

        // Raise a notification about the debt
        createNotification(context, firstPart + title, preposition + owner, title, uuid, owner, phone, tabTag);
    }

    /**
     * Creates and shows notification to the user.
     *
     * @param context app <code>Context</code> for the intent.
     * @param title   short content.
     * @param text    few more details.
     * @param alert   shows on the top bar for one second.
     * @param uuid    must be unique.
     * @param tabTag  should not be <code>null</code>.
     */
    private void createNotification(Context context, String title, String text, String alert, String uuid, String owner, String phone, String tabTag) {
        int alarmId = uuid.hashCode();

        Intent intent = new Intent(context, EditDebtActivity.class);
//        intent.setFlags(/*Intent.FLAG_ACTIVITY_REORDER_TO_FRONT*/ /*Intent.FLAG_ACTIVITY_SINGLE_TOP | */Intent.FLAG_ACTIVITY_CLEAR_TOP);// REMOVE: 21/09/2015
        intent.putExtra(Debt.KEY_UUID, uuid);
        intent.putExtra(Debt.KEY_TAB_TAG, tabTag);

        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0, intent
                , PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(title)
                .setTicker(alert)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(notificationIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true);
        if (phone != null) {
            // Create dialing action
            String dialTitle = "Call " + owner;
            int dialIcon = R.drawable.ic_call_black_24dp;
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
            PendingIntent notificationCallIntent = PendingIntent.getActivity(context, 0, dialIntent
                    , PendingIntent.FLAG_UPDATE_CURRENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.addAction(new Notification.Action.Builder(
                        Icon.createWithResource(context,dialIcon),
                        dialTitle,
                        notificationCallIntent)
                        .build());
            } else {
                //noinspection deprecation
                builder.addAction(dialIcon, dialTitle, notificationCallIntent);
            }
        }
        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(alarmId, notification);
    }

}
