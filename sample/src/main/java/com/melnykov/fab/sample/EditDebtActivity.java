package com.melnykov.fab.sample;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.Calendar;
import java.util.Date;


public class EditDebtActivity extends AppCompatActivity {

    private Button saveButton;
    private Button deleteButton;
    private Button remindButton;
    private CheckBox remindCheckBox;
    private EditText debtTitleText;
    private EditText debtOwnerText;
    private EditText debtDescText;

    private Debt debt;
    private String debtId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_debt);

        // Fetch the debtId from the Extra data
        if (getIntent().hasExtra("ID")) {
            debtId = getIntent().getExtras().getString("ID");
        }

        debtTitleText = (EditText) findViewById(R.id.debt_title);
        debtOwnerText = (EditText) findViewById(R.id.debt_owner);
        debtDescText = (EditText) findViewById(R.id.debt_desc);
        saveButton = (Button) findViewById(R.id.saveButton);
        deleteButton = (Button) findViewById(R.id.deleteButton);
        remindButton = (Button) findViewById(R.id.remindButton);
        remindCheckBox = (CheckBox) findViewById(R.id.remindCheckBox);

        // TODO: 07/09/2015 transfer Debt serializable ?
        if (debtId == null) {
            debt = new Debt();
            debt.setUuidString();
        } else {
            ParseQuery<Debt> query = Debt.getQuery();
            query.fromLocalDatastore();
            query.whereEqualTo("uuid", debtId);
            query.getFirstInBackground(new GetCallback<Debt>() {

                @Override
                public void done(Debt object, ParseException e) {
                    if (!isFinishing()) {
                        debt = object;
                        debtTitleText.setText(debt.getTitle());
                        debtOwnerText.setText(debt.getOwner());
                        debtDescText.setText(debt.getDescription());
                        Date dueDate = debt.getDueDate();
                        if (dueDate != null) {
                            remindButton.setText(android.text.format.DateFormat.format("MM/dd/yy h:mmaa", dueDate.getTime()));
                            remindCheckBox.setChecked(true);
                        }
                        deleteButton.setVisibility(View.VISIBLE);
                    }
                }

            });

        }

        saveButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                debt.setTitle(debtTitleText.getText().toString());
                debt.setOwner(debtOwnerText.getText().toString());
                debt.setDescription(debtDescText.getText().toString());
                if (!remindCheckBox.isChecked()) {
                    // In case the date was already set by the dialog
                    debt.setDueDate(null);
                    cancelAlarm(debt);
                }
                debt.setDraft(true);
                debt.setAuthor(ParseUser.getCurrentUser());
                debt.pinInBackground(DebtListApplication.DEBT_GROUP_NAME,
                        new SaveCallback() {

                            @Override
                            public void done(ParseException e) {
                                if (isFinishing()) {
                                    return;
                                }
                                if (e == null) {
                                    if (debt.getDueDate() != null) {
                                        setAlarm(debt);
                                    }
                                    setResult(Activity.RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            "Error saving: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }

                        });
            }

        });

        deleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // The debt will be deleted eventually but will
                // immediately be excluded from query results.
                cancelAlarm(debt);
                debt.deleteEventually();
                setResult(Activity.RESULT_OK);
                finish();
            }

        });

        remindButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                SlideDateTimeListener listener = new SlideDateTimeListener() {

                    @SuppressWarnings("deprecation")
                    @Override
                    public void onDateTimeSet(Date date) {
                        date.setSeconds(0);
                        remindButton.setText(android.text.format.DateFormat.format("MM/dd/yy h:mmaa", date.getTime()));
                        remindCheckBox.setChecked(true);
                        debt.setDueDate(date);
                    }

                    @Override
                    public void onDateTimeCancel() {

                    }
                };
                new SlideDateTimePicker.Builder(getSupportFragmentManager())
                        .setListener(listener)
                        .setInitialDate(new Date())
                        .build()
                        .show();
            }
        });
    }

    /**
     * Sets a new notification alarm.
     *
     * @param debt with valid dueDate
     */
    private void setAlarm(Debt debt) {
        long timeInMillis = debt.getDueDate().getTime();
        Intent alertIntent = new Intent(this, DueDateAlarm.class);
        String uuid = debt.getUuidString();
        int alarmId = uuid.hashCode();

        alertIntent.putExtra("title", debt.getTitle());
        alertIntent.putExtra("owner", debt.getOwner());

        alertIntent.setData(Uri.parse("timer:" + uuid));

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, timeInMillis, PendingIntent.getBroadcast(
                this, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Toast.makeText(
                this,
                "Reminder  " + alarmId + " at "
                        + android.text.format.DateFormat.format(
                        "MM/dd/yy h:mmaa",
                        timeInMillis),
                Toast.LENGTH_LONG).show();// REMOVE: 07/09/2015
    }

    /**
     * Cancels notification alarm if exists.
     *
     * @param debt to cancel
     */
    private void cancelAlarm(Debt debt) {
        Intent alertIntent = new Intent(this, DueDateAlarm.class);
        int alarmId = debt.getUuidString().hashCode();

        alertIntent.setData(Uri.parse("timer:" + alarmId));

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(PendingIntent.getBroadcast(this, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Toast.makeText(this, "REMOVED Reminder " + alarmId, Toast.LENGTH_LONG).show(); // REMOVE: 07/09/2015
    }
}
