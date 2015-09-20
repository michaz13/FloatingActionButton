package com.melnykov.fab.sample;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.google.gson.Gson;
import com.parse.ParseException;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SendCallback;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Locale;


public class EditDebtActivity extends AppCompatActivity {

    static final String ALARM_SCHEME = "timer:";
    private static final int FLAG_FORCE_BACK_TO_MAIN = 0x00040000;
    private static final int FLAG_SET_ALARM = 0X00020000;

    private Button saveButton;
    private Button deleteButton;
    private Button remindButton;
    private CheckBox remindCheckBox;
    private EditText debtTitleText;
    private EditText debtOwnerText;
    private EditText debtPhoneText;
    private EditText debtDescText;
    private SearchView searchView;

    private Debt debt;
    private String debtId;
    private String debtTabTag;
    private boolean isFromPush;
    private boolean isNew;
    private boolean isModified;
    private Debt beforeChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_debt);

        fetchExtras();
        setActionBarTitle();
        initViewHolders();
        try {
            prepareDebtForEditing();
        } catch (ParseException e) {
            e.printStackTrace();// REMOVE: 19/09/2015
            Toast.makeText(EditDebtActivity.this, "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra(Debt.KEY_TAB_TAG, debtTabTag);
            startActivity(intent);
        }

        if(isNew){
            debtTitleText.requestFocus();
        }

        saveButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!validateDebtDetails()) {
                    return;
                }
                setDebtFieldsAfterEditing();
                if (debt.getPhone() != null && (isNew || isModified)) {
                    showPushDialog();// TODO: 17/09/2015 check for change
                } else {
                    saveDebt(FLAG_SET_ALARM | FLAG_FORCE_BACK_TO_MAIN);
                }
            }
        });

        deleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendPushResponse(debt.getOtherUuid(), Debt.STATUS_RETURNED);// TODO: 16/09/2015 move to "done" marking
                cancelAlarm(debt);
                // The debt will be deleted eventually but will immediately be excluded from query results.
                debt.deleteEventually();// FIXME: 17/09/2015
/*                debt.deleteInBackground(new DeleteCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            System.err.println("Not deleted: " + e.getMessage());
                        }
                    }
                });*/// FIXME: 17/09/2015
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
                Date initDate;
                Date currDate = debt.getDueDate();
                if (currDate != null) {
                    initDate = currDate;
                } else {
                    initDate = new Date();
                }
                new SlideDateTimePicker.Builder(getSupportFragmentManager())
                        .setListener(listener)
                        .setInitialDate(initDate)
                        .setIndicatorColor(Color.RED)
                        .build()
                        .show();
            }
        });
    }

    private void saveDebt(final int flags) {
        debt.pinInBackground(DebtListApplication.DEBT_GROUP_NAME,
                new SaveCallback() {

                    @Override
                    public void done(ParseException e) {
                        if (isFinishing()) {
                            return;
                        }
                        if (e == null) {
                            if (isFromPush) {
                                sendPushResponse(debt.getOtherUuid(), Debt.STATUS_CONFIRMED);
                            }
                            wrapUp(flags);
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Error saving: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void setDebtFieldsAfterEditing() {
        debt.setTitle(debtTitleText.getText().toString());
        debt.setOwner(debtOwnerText.getText().toString());
        debt.setPhone(debtPhoneText.getText().toString(), getUserCountry(EditDebtActivity.this));
        debt.setDescription(debtDescText.getText().toString());
        if (!remindCheckBox.isChecked()) {
            // In case the date was already set by the dialog
            debt.setDueDate(null);
            cancelAlarm(debt);
        }
        debt.setDraft(true);
        debt.setStatus(Debt.STATUS_CREATED);
        ParseUser currUser = ParseUser.getCurrentUser();
        debt.setAuthor(currUser);
        debt.setAuthorName(currUser.getString("name"));
        debt.setAuthorPhone(currUser.getString("phone"));
        if (debt.equals(beforeChange)) {
            isModified = false;
        } else {
            isModified = true;// TODO: 19/09/2015
        }
    }

    private boolean validateDebtDetails() {
        if (debtTitleText.getText().toString().trim().equals("")) {
            debtTitleText.setError(getString(R.string.no_title_error));
            debtTitleText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(debtTitleText, InputMethodManager.SHOW_IMPLICIT);
            return false;
        }
        if (debtOwnerText.getText().toString().trim().equals("")) {
            debtOwnerText.setError(getString(R.string.no_owner_error));
            debtOwnerText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(debtOwnerText, InputMethodManager.SHOW_IMPLICIT);
            return false;
        }
        return true;
    }

    private void setActionBarTitle() {//
        ActionBar actionBar = getSupportActionBar();
        if (debtTabTag.equals(Debt.I_OWE_TAG)) {
            actionBar.setTitle(getString(R.string.i_owe_tab_title));
        } else {
            actionBar.setTitle(getString(R.string.owe_me_tab_title));
        }
    }

    /**
     * Synchronize the status of the other end
     *
     * @param otherUuid of the debt on the destination side
     * @param status    to deliver to the other end
     */
    private void sendPushResponse(String otherUuid, final int status) {
        if (otherUuid == null) {
            return;
        }
        ParsePush push = new ParsePush();
        push.setChannel(MainActivity.USER_CHANNEL_PREFIX + debt.getPhone().replaceAll("[^0-9]+", ""));
        Gson gson = new Gson(); // Or use new GsonBuilder().create();
        // TODO: 14/09/2015 use proxy (add image, date): https://gist.github.com/janakagamini/f5c63ea27bee8b7b7581
        push.setMessage(status + "+" + debt.getUuidString() + "+" + otherUuid/*gson.toJson(o)*/);///**/);
        push.sendInBackground(new SendCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    debt.setStatus(status);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Push not sent: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();// REMOVE: 15/09/2015

                }
            }
        });
    }

    private void wrapUp(int flags) {
        if ((flags & FLAG_SET_ALARM) != 0 && debt.getDueDate() != null) {
            setAlarm(debt);
        }
        setResult(Activity.RESULT_OK);
        finish();
        if ((flags & FLAG_FORCE_BACK_TO_MAIN) != 0) {
            returnToMain(debt); // in case the activity was not started for a result
        }
    }

    private void showPushDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(EditDebtActivity.this);
        String message;
        String linkWord;
        if (debt.getTabTag().equals(Debt.I_OWE_TAG)) {
            linkWord = "your";
        } else {
            linkWord = "his";
        }
        if (isNew) {
            message = "Tell " + debt.getOwner() + " about " + linkWord + " debt";
        } else {
            message = "Tell " + debt.getOwner() + " about changed details";
        }
        builder.setMessage(message);
        builder.setPositiveButton("Notification", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                sendPushToOwner();
                saveDebt(FLAG_SET_ALARM | FLAG_FORCE_BACK_TO_MAIN);
            }
        });
        builder.setNeutralButton("Phone call", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + debt.getPhone().substring(1)));
                startActivity(dial);
                saveDebt(FLAG_SET_ALARM);
            }
        });
        builder.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                saveDebt(FLAG_SET_ALARM | FLAG_FORCE_BACK_TO_MAIN);
            }
        });
        builder.show();
    }

    private void prepareDebtForEditing() throws ParseException {
        if (isFromPush) {
            isNew = false;
            cloneDebtFromPush();
        } else if (debtId != null) {
            isNew = false;
            loadExistingDebt();
        } else {
            isNew = true;
            debt = new Debt();
            debt.setUuidString();
            debt.setTabTag(debtTabTag);
        }
        beforeChange = debt.createClone();
    }

    private TextView.OnEditorActionListener onEditorActionListener = new EditText.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                TextView next = findNextEmptyView(v);
                if (next != null) {
                    requestViewFocus(next);
                } else {
                    clearViewFocus(v);
                }
                return true;
            }
            return false;
        }
    };

    private void initViewHolders() {
        debtTitleText = (EditText) findViewById(R.id.debt_title);
        debtTitleText.setOnEditorActionListener(onEditorActionListener);
        debtOwnerText = (EditText) findViewById(R.id.debt_owner);
        debtOwnerText.setOnEditorActionListener(onEditorActionListener);
        debtPhoneText = (EditText) findViewById(R.id.debt_phone);
        debtPhoneText.setOnEditorActionListener(onEditorActionListener);
        debtDescText = (EditText) findViewById(R.id.debt_desc);
        saveButton = (Button) findViewById(R.id.save_button);
        deleteButton = (Button) findViewById(R.id.delete_button);
        remindButton = (Button) findViewById(R.id.remind_button);
        remindCheckBox = (CheckBox) findViewById(R.id.remind_checkbox);
    }

    private void requestViewFocus(TextView v) {
        v.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    private void clearViewFocus(TextView v) {
        v.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }


    private void fetchExtras() {
        debtId = getIntent().getStringExtra(Debt.KEY_UUID);
        debtTabTag = getIntent().getStringExtra(Debt.KEY_TAB_TAG);
        isFromPush = getIntent().getBooleanExtra("fromPush", false);
    }

    private void sendPushToOwner() {
        // TODO: 14/09/2015 send only if data was changed
        ParsePush push = new ParsePush();
        push.setChannel(MainActivity.USER_CHANNEL_PREFIX + debt.getPhone().replaceAll("[^0-9]+", ""));
        Gson gson = new Gson(); // Or use new GsonBuilder().create();
        // TODO: 14/09/2015 use proxy (add image, date): https://gist.github.com/janakagamini/f5c63ea27bee8b7b7581
        push.setMessage(debt.getUuidString()/*gson.toJson(o)*/);///**/);
        push.sendInBackground(new SendCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    debt.setStatus(Debt.STATUS_PENDING);
                    // TODO: 16/09/2015 save
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Push not sent: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();// REMOVE: 15/09/2015

                }
            }
        });
    }

    private void loadExistingDebt() throws ParseException {
        ParseQuery<Debt> query = Debt.getQuery();
        query.fromLocalDatastore();
        query.whereEqualTo(Debt.KEY_UUID, debtId);
        Debt object = query.getFirst();
        debt = object;
        debtTitleText.setText(debt.getTitle());
        debtOwnerText.setText(debt.getOwner());
        debtPhoneText.setText(debt.getPhone());
        debtDescText.setText(debt.getDescription());
        Date dueDate = debt.getDueDate();
        if (dueDate != null) {
            remindButton.setText(android.text.format.DateFormat.format("MM/dd/yy h:mmaa", dueDate.getTime()));
            remindCheckBox.setChecked(true);
        }
        deleteButton.setVisibility(View.VISIBLE);
    }

    private void cloneDebtFromPush() throws ParseException {
        debt = new Debt();
        debt.setUuidString();

        ParseQuery<Debt> query = Debt.getQuery();
        query.whereEqualTo(Debt.KEY_UUID, debtId);

        Debt other = query.getFirst();
        debt.setOtherUuid(debtId);
        debt.setTabTag(reverseTag(other.getTabTag()));
        debtTitleText.setText(other.getTitle());
        debtOwnerText.setText(other.getAuthorName());
        debtPhoneText.setText(other.getAuthorPhone());
        debtDescText.setText(other.getDescription());
        Date dueDate = other.getDueDate();
        if (dueDate != null) {
            debt.setDueDate(dueDate);
            remindButton.setText(android.text.format.DateFormat.format("MM/dd/yy h:mmaa", dueDate.getTime()));
            remindCheckBox.setChecked(true);
        }
        saveButton.setText(R.string.add_debt);
        deleteButton.setText(R.string.ignore);
        deleteButton.setVisibility(View.VISIBLE);
    }

    private String reverseTag(String tabTag) {
        if (tabTag.equals(Debt.I_OWE_TAG)) {
            tabTag = Debt.OWE_ME_TAG;
        } else {
            tabTag = Debt.I_OWE_TAG;
        }
        return tabTag;
    }

    private void returnToMain(Debt debt) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//        intent.putExtra(Debt.KEY_TAB_TAG, debt.getUuidString());// REMOVE: 16/09/2015
        intent.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        startActivity(intent);
    }
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() ==  android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item);
    }*/// REMOVE: 08/09/2015

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_debt, menu);
        setupSearch(menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setupSearch(Menu menu) {
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        setSearchTextColors();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        setSearchIcons();
    }

    private void setSearchTextColors() {
        LinearLayout linearLayout1 = (LinearLayout) searchView.getChildAt(0);
        LinearLayout linearLayout2 = (LinearLayout) linearLayout1.getChildAt(2);
        LinearLayout linearLayout3 = (LinearLayout) linearLayout2.getChildAt(1);
        AutoCompleteTextView autoComplete = (AutoCompleteTextView) linearLayout3.getChildAt(0);
        //Set the input text color
        autoComplete.setTextColor(Color.WHITE);
        // set the hint text color
        autoComplete.setHintTextColor(Color.WHITE);
        autoComplete.setNextFocusDownId(R.id.debt_title);
        autoComplete.setOnEditorActionListener(onEditorActionListener);
    }

    private TextView findNextEmptyView(TextView view) {
        int nextId = view.getNextFocusDownId();
        while (nextId != View.NO_ID) {
            TextView next = (TextView) findViewById(nextId);
            if (next.getText().toString().trim().equals("")) {
                return next;
            }
            nextId = next.getNextFocusDownId();
        }
        return null;
    }


    private void setSearchIcons() {
        try {
            Field searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            ImageView closeBtn = (ImageView) searchField.get(searchView);
            closeBtn.setImageResource(R.drawable.ic_close_white_24dp);

            searchField = SearchView.class.getDeclaredField("mVoiceButton");
            searchField.setAccessible(true);
            ImageView voiceBtn = (ImageView) searchField.get(searchView);
            voiceBtn.setImageResource(R.drawable.ic_keyboard_voice_white_24dp);

            searchField = SearchView.class.getDeclaredField("mSearchButton");
            searchField.setAccessible(true);
            ImageView searchButton = (ImageView) searchField.get(searchView);
            searchButton.setImageResource(R.drawable.ic_search_white_24dp);

            // Accessing the SearchAutoComplete
            int queryTextViewId = getResources().getIdentifier("android:id/search_src_text", null, null);
            View autoComplete = searchView.findViewById(queryTextViewId);

            Class<?> clazz = Class.forName("android.widget.SearchView$SearchAutoComplete");

            SpannableStringBuilder stopHint = new SpannableStringBuilder("   ");
            stopHint.append(getString(R.string.findContact));

            // Add the icon as an spannable
            Drawable searchIcon = getResources().getDrawable(R.drawable.ic_search_white_24dp);
            Method textSizeMethod = clazz.getMethod("getTextSize");
            Float rawTextSize = (Float) textSizeMethod.invoke(autoComplete);
            int textSize = (int) (rawTextSize * 1.25);
            searchIcon.setBounds(0, 0, textSize, textSize);
            stopHint.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set the new hint text
            Method setHintMethod = clazz.getMethod("setHint", CharSequence.class);
            setHintMethod.invoke(autoComplete, stopHint);

        } catch (NoSuchFieldException e) {
            Log.e("SearchView", e.getMessage(), e);
        } catch (IllegalAccessException e) {
            Log.e("SearchView", e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        int x = 3;
        if (ContactsContract.Intents.SEARCH_SUGGESTION_CLICKED.equals(intent.getAction())) {
            //handles suggestion clicked query
            String displayName = getDisplayNameForContact(intent);
            debtOwnerText.setText(displayName);
            String phone = getPhoneNumber(displayName);
            debtPhoneText.setText(phone);
            debtTitleText.requestFocus();
            debtTitleText.setNextFocusDownId(R.id.debt_desc);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(debtTitleText, InputMethodManager.SHOW_IMPLICIT);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) { // REMOVE: 14/09/2015
            // handles a search query
            String query = intent.getStringExtra(SearchManager.QUERY);
//            debtOwnerText.setText("should search for query: '" + query + "'...");
            debtTitleText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(debtTitleText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Get ISO 3166-1 alpha-2 country code for this device (or null if not available)
     *
     * @param context Context reference to get the TelephonyManager instance from
     * @return country code or null
     */
    private static String getUserCountry(Context context) {
        try {
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String simCountry = tm.getSimCountryIso();
            if (simCountry != null && simCountry.length() == 2) { // SIM country code is available
                return simCountry.toUpperCase(Locale.US);
            } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
                String networkCountry = tm.getNetworkCountryIso();
                if (networkCountry != null && networkCountry.length() == 2) { // network country code is available
                    return networkCountry.toUpperCase(Locale.US);
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    private String getDisplayNameForContact(Intent intent) {
        Cursor phoneCursor = getContentResolver().query(intent.getData(), null, null, null, null);
        phoneCursor.moveToFirst();
        int idDisplayName = phoneCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        String name = phoneCursor.getString(idDisplayName);
        phoneCursor.close();
        return name;
    }

    private String getPhoneNumber(String name) {
        String ret = null;
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like'" + name + "'";
        String[] projection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, null, null);
        if (c.moveToFirst()) {
            ret = c.getString(0);
        }
        c.close();
        if (ret == null) {
            ret = "Unsaved";
        }
        return ret;
    }

    /**
     * Sets a new notification alarm.
     *
     * @param debt with valid dueDate
     */
    private void setAlarm(Debt debt) {
        long timeInMillis = debt.getDueDate().getTime();
        Intent alertIntent = new Intent(this, DueDateAlarm.class);
        String schemeSpecificPart = debt.getUuidString();
        int alarmId = schemeSpecificPart.hashCode();

        alertIntent.putExtra(Debt.KEY_TITLE, debt.getTitle());
        alertIntent.putExtra(Debt.KEY_OWNER, debt.getOwner());
        alertIntent.putExtra(Debt.KEY_PHONE, debt.getPhone());
        alertIntent.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());

        alertIntent.setData(Uri.parse(ALARM_SCHEME + schemeSpecificPart));

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, timeInMillis, PendingIntent.getBroadcast(
                this, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Toast.makeText(this, "Reminder  " + alarmId + " at "
                        + android.text.format.DateFormat.format("MM/dd/yy h:mmaa", timeInMillis),
                Toast.LENGTH_LONG).show();// REMOVE: 07/09/2015
    }

    /**
     * Cancels notification alarm if exists.
     *
     * @param debt to cancel
     */
    private void cancelAlarm(Debt debt) {
        Intent alertIntent = new Intent(this, DueDateAlarm.class);
        String schemeSpecificPart = debt.getUuidString();
        int alarmId = schemeSpecificPart.hashCode();

        alertIntent.setData(Uri.parse(ALARM_SCHEME + schemeSpecificPart));

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(PendingIntent.getBroadcast(this, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Toast.makeText(this, "REMOVED Reminder " + alarmId, Toast.LENGTH_LONG).show(); // REMOVE: 07/09/2015
    }
}
