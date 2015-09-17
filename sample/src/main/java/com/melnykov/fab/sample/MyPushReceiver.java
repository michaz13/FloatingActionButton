package com.melnykov.fab.sample;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParsePushBroadcastReceiver;
import com.parse.ParseQuery;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Creates notification for the received debt, which allows the user to add/ignore it
 */
public class MyPushReceiver extends ParsePushBroadcastReceiver {
    private Debt debt;
    private String debtId;
    private String debtOtherId;
    private int debtStatus;
    private boolean isResponsePush = false;

/*    @Override
    public void onPushOpen(Context context, Intent intent) {
        Intent i = new Intent(context, EditDebtActivity.class);
        i.putExtras(intent.getExtras());
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }*/// TODO: 09/09/2015 remove

    @Override
    public void onPushReceive(final Context context, Intent intent) {
        JSONObject data;
        String alert = null;
        try {
            data = new JSONObject(intent.getStringExtra(MyPushReceiver.KEY_PUSH_DATA));
            if (data != null) {
                alert = data.getString("alert");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (alert == null) {
            return;
        }
        String[] alertParts=alert.split("\\+");
        if (alertParts.length == 3) {
            isResponsePush = true;
            debtStatus = Integer.parseInt(alertParts[0]);
            debtOtherId = alertParts[1];
            debtId = alertParts[2];
        } else {
            isResponsePush = false;
            debtId = alert;
        }
        ParseQuery<Debt> query = Debt.getQuery();
        query.whereEqualTo(Debt.KEY_UUID, debtId);
        query.getFirstInBackground(new GetCallback<Debt>() {

            @Override
            public void done(Debt object, ParseException e) {
                if (e != null) {
                    System.err.println("onPushReceive fail: " + e.getMessage());
                    return;
                }
                debt = object;
                String alert = "Friend's debt created";
                if (isResponsePush) {
                    alert = "confirmed";
                    debt.setStatus(debtStatus);// TODO: 16/09/2015 save
                    debt.setOtherUuid(debtOtherId);// TODO: 16/09/2015 save
                    if (debtStatus == Debt.STATUS_RETURNED) {
                        cancelAlarm(context, debt);
                    }
                }
                if (debt.getTabTag().equals(Debt.OWE_ME_TAG)) { // reversed logic
                    createNotification(context, "You owe " + debt.getAuthorName(), debt.getTitle(), alert, debt.getUuidString(), debt);
                } else {
                    createNotification(context, debt.getAuthorName() + " owes you", debt.getTitle(), alert, debt.getUuidString(), debt);
                }
            }

        });
    }

    /**
     * Creates and shows notification to the user.
     *
     * @param context app context for the intent
     * @param title   short content
     * @param text    few more details
     * @param alert   shows on the top bar for one second
     * @param uuid    must be unique
     */
    public void createNotification(Context context, String title, String text, String alert, String uuid, Debt debt) {
        Intent intent = new Intent(context, EditDebtActivity.class);
//        intent.setFlags(/*Intent.FLAG_ACTIVITY_REORDER_TO_FRONT*/ /*Intent.FLAG_ACTIVITY_SINGLE_TOP | */Intent.FLAG_ACTIVITY_CLEAR_TOP);// REMOVE: 14/09/2015
        int alarmId = uuid.hashCode();
        intent.putExtra(Debt.KEY_UUID, uuid);
        intent.putExtra("fromPush", true);
        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0, intent
                , PendingIntent.FLAG_UPDATE_CURRENT);

        Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + debt.getAuthorPhone()));
        PendingIntent notificationCallIntent = PendingIntent.getActivity(context, 0, dial
                , PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(context)
                .setContentTitle(title)
                .setTicker(alert)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(notificationIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .addAction(0, "Call " + debt.getAuthorName(), notificationCallIntent) // FIXME: 14/09/2015 dep
                .setAutoCancel(true)
                .build();
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(alarmId, notification); // make sure alarmId is unique
    }

    /**
     * Cancels notification alarm if exists.
     *
     * @param debt to cancel
     */
    private void cancelAlarm(Context context, Debt debt) {
        Intent alertIntent = new Intent(context, DueDateAlarm.class);
        String schemeSpecificPart = debt.getUuidString();
        int alarmId = schemeSpecificPart.hashCode();

        alertIntent.setData(Uri.parse(EditDebtActivity.ALARM_SCHEME + schemeSpecificPart));

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(PendingIntent.getBroadcast(context, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

    }
}
