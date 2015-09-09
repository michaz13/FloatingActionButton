package com.melnykov.fab.sample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v7.app.NotificationCompat;

import com.google.gson.Gson;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by michaz13 on 09/09/2015.
 */
public class MyReceiver extends ParsePushBroadcastReceiver {

/*    @Override
    public void onPushOpen(Context context, Intent intent) {
        Intent i = new Intent(context, EditDebtActivity.class);
        i.putExtras(intent.getExtras());
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }*/// TODO: 09/09/2015 remove

    @Override
    public void onPushReceive(Context context, Intent intent) {
        JSONObject data;
        String strData;
        EditDebtActivity.MyObj debt=null;
        Gson gson = new Gson(); // Or use new GsonBuilder().create();
        try {
            data = new JSONObject(intent.getStringExtra(MyReceiver.KEY_PUSH_DATA));
            strData = data.getString("alert");
            debt = gson.fromJson(strData, EditDebtActivity.MyObj.class); // deserializes json into target2
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return;
/*        if(debt == null){
            return;
        }
        String alert = "Friend's debt created";

        if (debt.getTabTag().equals(Debt.OWE_ME_TAG)) {
            createNotification(context, "You owe "+ debt.getOwner(), debt.getTitle(), alert, debt.getUuidString());
        }
        else {
            createNotification(context, debt.getOwner() + " owes you", debt.getTitle(), alert, debt.getUuidString());
        }*/
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
    public void createNotification(Context context, String title, String text, String alert, String uuid) {
        Intent intent = new Intent(context, EditDebtActivity.class);
//        intent.setFlags(/*Intent.FLAG_ACTIVITY_REORDER_TO_FRONT*/ /*Intent.FLAG_ACTIVITY_SINGLE_TOP | */Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int alarmId = uuid.hashCode();
        intent.putExtra(Debt.KEY_UUID, uuid);
        PendingIntent notificationIntent = PendingIntent.getActivity(context, 0, intent
                , PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(context)
                .setContentTitle(title)
                .setTicker(alert)
                .setContentText(text)
                .setSmallIcon(R.drawable.app_logo)
                .setContentIntent(notificationIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .addAction(0, "Call ...", notificationIntent) //todo contact's phone
                .setAutoCancel(true)
                .build();
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(alarmId, notification); //todo check int cast, make unique
    }
}
