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
import android.text.format.DateFormat;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.google.gson.Gson;
import com.parse.FindCallback;
import com.parse.ParseAnonymousUtils;
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
import java.util.List;
import java.util.Locale;

/**
 * Choosing debt details. Used both for new and for existing cc
 */
public class EditDebtActivity extends AppCompatActivity {

    static final String ALARM_SCHEME = "timer:";
    private static final int FLAG_FORCE_BACK_TO_MAIN = 0x00040000;
    private static final int FLAG_SET_ALARM = 0X00020000;

    private Button remindButton;
    private CheckBox remindCheckBox;
    private CheckBox pushCheckBox;
    private EditText debtTitleText;
    private EditText debtOwnerText;
    private EditText debtPhoneText;
    private EditText debtDescText;
    private MenuItem searchViewMenuItem;
    private SearchView searchView;
    private ImageView closeBtn;
    private Spinner spinner1;

    private Debt debt;
    private String debtId;
    private String debtTabTag;
    private boolean isFromPush;
    private boolean isNew;
    private boolean isModified;
    private Debt beforeChange;

    private int currencyPos;
    private MenuItem deleteMenuItem;


    //**********************************************************************************************
    //**************************************** Lifecycle methods: **********************************
    //**********************************************************************************************
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

        if (isNew) {
            debtTitleText.requestFocus();
        }

        remindButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                SlideDateTimeListener listener = new SlideDateTimeListener() {

                    @SuppressWarnings("deprecation")
                    @Override
                    public void onDateTimeSet(Date date) {
                        date.setSeconds(0);
                        remindButton.setText(DateFormat.format("MM/dd/yy h:mmaa", date.getTime()));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_debt, menu);
        setupSearch(menu);
        deleteMenuItem = menu.findItem(R.id.action_delete);
        if (isNew) {
            deleteMenuItem.setVisible(false);
        } else {
            deleteMenuItem.setVisible(true);
        }
        if (isFromPush) {
            deleteMenuItem.setIcon(R.drawable.ic_cancel_white_36dp);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra(Debt.KEY_TAB_TAG, debtTabTag);
                startActivity(intent);
                break;
            case R.id.action_delete:// TODO: 24/09/2015 confirm dialog
                sendPushResponse(debt.getOtherUuid(), Debt.STATUS_RETURNED);// TODO: 16/09/2015 move to "done" marking
                cancelAlarm(debt);
                // The debt will be deleted eventually but will immediately be excluded from mQuery results.
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
                break;
            case R.id.action_done:
                if (!validateDebtDetails()) {
                    break;
                }
                setDebtFieldsAfterEditing();
                if (debt.getPhone() != null && (isNew || isModified)) {
                    if (pushCheckBox.isChecked()) {// TODO: 24/09/2015 settings
                        sendPushToOwner();
                    }
                    showActionsDialog();
                } else {
                    saveDebt(FLAG_SET_ALARM | FLAG_FORCE_BACK_TO_MAIN);
                }
                break;
            default:
                break;
        }
        return false;
    }

    /**
     * Extracts the extras from the <code>Intent</code>.
     */
    private void fetchExtras() {
        debtId = getIntent().getStringExtra(Debt.KEY_UUID);
        debtTabTag = getIntent().getStringExtra(Debt.KEY_TAB_TAG);
        isFromPush = getIntent().getBooleanExtra("fromPush", false);
    }

    /**
     * Set the alarm and finish the activity.
     *
     * @param flags FLAG_SET_ALARM for setting the alarm if needed, and FLAG_FORCE_BACK_TO_MAIN for calling {@link MainActivity}.
     */
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

    /**
     * Opens {@link MainActivity}.
     *
     * @param debt for the intent's extras.
     */
    private void returnToMain(Debt debt) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        startActivity(intent);
    }


    //**********************************************************************************************
    //**************************************** Debt operation methods: *****************************
    //**********************************************************************************************

    /**
     * Pins the <code>Debt</code> in local database and ? todo
     *
     * @param flags the <code>flags</code> parameter for the {@link #wrapUp}.
     */
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

    /**
     * Updates the debt's details from the text fields.
     */
    private void setDebtFieldsAfterEditing() {
        debt.setTitle(debtTitleText.getText().toString());
        debt.setCurrencyPos(currencyPos);
        debt.setMoneyAmountByTitle();
        setTitleFormattedAsMoneyAmount(currencyPos);
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

    /**
     * In case the debt is money, sets the title to: [money amount] [currency symbol].
     * Assumes the money amount was already set by {@link Debt#getMoneyAmount()}.
     *
     * @param currencyPos the position of the currency symbol in the <code>Spinner</code>.
     */
    private void setTitleFormattedAsMoneyAmount(int currencyPos) {
        if (currencyPos == Debt.NON_MONEY_DEBT_CURRENCY) {
            return;
        }
        String currency = spinner1.getItemAtPosition(currencyPos).toString();
        int amount = debt.getMoneyAmount();
        if (amount >= 0) {
            debt.setTitle(amount + " " + currency);
        } else {
            debt.setCurrencyPos(Debt.NON_MONEY_DEBT_CURRENCY);
        }
    }

    /**
     * Make sure all required fields are entered.
     *
     * @return <code>true</code> iff all fields are valid.
     */
    private boolean validateDebtDetails() {
        if (debtTitleText.getText().toString().trim().equals("")) {
            debtTitleText.setError(getString(R.string.no_title_error));
            requestViewFocus(debtTitleText);
            return false;
        }
        if (debtOwnerText.getText().toString().trim().equals("")) {
            debtOwnerText.setError(getString(R.string.no_owner_error));
            requestViewFocus(debtTitleText);
            return false;
        }
        return true;
    }

    /**
     * Loads the current <code>Debt</code> from parse.
     *
     * @throws ParseException
     */
    private void loadExistingDebt() throws ParseException {
        ParseQuery<Debt> query = Debt.getQuery();
        query.fromLocalDatastore();
        query.whereEqualTo(Debt.KEY_UUID, debtId);
        Debt object = query.getFirst();
        debt = object;
        debtTitleText.setText(debt.getTitle());
        spinner1.setSelection(debt.getCurrencyPos());
        debtOwnerText.setText(debt.getOwner());
        debtPhoneText.setText(debt.getPhone());
        debtDescText.setText(debt.getDescription());
        Date dueDate = debt.getDueDate();
        if (dueDate != null) {
            remindButton.setText(android.text.format.DateFormat.format("MM/dd/yy h:mmaa", dueDate.getTime()));
            remindCheckBox.setChecked(true);
        }
    }

    /**
     * Creates a copy of the current <code>Debt</code> with reversed tag.
     *
     * @throws ParseException
     */
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
        // REMOVE: 23/09/2015
        ParseUser author = other.getAuthor();
        System.out.println("Other id *******************************************************************" + author.getObjectId());
    }

    /**
     * Load the <code>Debt</code> from Parse or create a new one.
     *
     * @throws ParseException
     */
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


    //**********************************************************************************************
    //**************************************** Communication methods: ******************************
    //**********************************************************************************************

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
        String phone = debt.getPhone();
        if (phone == null) {
            return;
        }
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

    /**
     * Show a confirmation push notification dialog, with an option to call the owner.
     */
    private void showActionsDialog() {
        int title;
        if (isNew) {
            title = R.string.contact_actions_dialog_title_new_debt;
        } else {
            title = R.string.contact_actions_dialog_title_modified_debt;
        }
        int array;
        if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            array = R.array.contact_actions_array_logged_in;
        } else {
            array = R.array.contact_actions_array_logged_out;
        }
        (new AlertDialog.Builder(EditDebtActivity.this))
                .setTitle(title)
                .setItems(array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        switch (whichButton) {
                            case DebtListAdapter.ACTION_CHAT:
                                openConversationByPhone();
                                break;
                            case DebtListAdapter.ACTION_CALL:
                                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + debt.getPhone()));
                                startActivity(dial);
                                break;
                            case DebtListAdapter.ACTION_SMS:
                                Intent sms = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", debt.getPhone(), null));
                                startActivity(sms);
                                break;

                        }
                        saveDebt(FLAG_SET_ALARM);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        saveDebt(FLAG_SET_ALARM | FLAG_FORCE_BACK_TO_MAIN);
                    }
                })
                .show();
    }

    public void openConversationByPhone() {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("phone", debt.getAuthorPhone());
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> user, com.parse.ParseException e) {
                if (e == null) {
                    Intent intent = new Intent(getApplicationContext(), MessagingActivity.class);
                    intent.putExtra("RECIPIENT_ID", user.get(0).getObjectId());
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Error finding that user",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Sends the owner a push notification about the debt.
     */
    private void sendPushToOwner() {
        // TODO: 14/09/2015 send only if data was changed
        ParsePush push = new ParsePush();
        String phone = debt.getPhone();
        if (phone == null) {
            return;
        }
        push.setChannel(MainActivity.USER_CHANNEL_PREFIX + phone.replaceAll("[^0-9]+", ""));
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


    //**********************************************************************************************
    //**************************************** Auxiliary methods: **********************************
    //**********************************************************************************************

    /**
     * Set the window's title according to the current tab tag.
     */
    private void setActionBarTitle() {//
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
/*        if (debtTabTag.equals(Debt.I_OWE_TAG)) {
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.i_owe_tab_title));
            }
        } else {
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.owe_me_tab_title));
            }
        }*/// REMOVE: 24/09/2015
    }

    /**
     * Next button listener that focuses on next empty <code>TextView</code>.
     */
    private TextView.OnEditorActionListener onEditorActionListener = new EditText.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                focusOnNextEmptyOrDone(v);
                return true;
            }
            return false;
        }
    };

    private void focusOnNextEmptyOrDone(TextView v) {
        TextView next = findNextEmptyView(v);
        if (next != null) {
            requestViewFocus(next);
        } else {
            clearViewFocus(v);
        }
    }

    /**
     * Retrieve the <code>View</code>s by their ids.
     */
    private void initViewHolders() {
        debtTitleText = (EditText) findViewById(R.id.debt_title);
        debtTitleText.setOnEditorActionListener(onEditorActionListener);
        debtOwnerText = (EditText) findViewById(R.id.debt_owner);
        debtOwnerText.setOnEditorActionListener(onEditorActionListener);
        debtPhoneText = (EditText) findViewById(R.id.debt_phone);
        debtPhoneText.setOnEditorActionListener(onEditorActionListener);
        debtDescText = (EditText) findViewById(R.id.debt_desc);
        remindButton = (Button) findViewById(R.id.remind_button);
        remindCheckBox = (CheckBox) findViewById(R.id.remind_checkbox);
        pushCheckBox = (CheckBox) findViewById(R.id.push_checkbox);
        spinner1 = (Spinner) findViewById(R.id.spinner1);
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currencyPos = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    /**
     * Request the focus on the given <code>TextView</code> the show the keyboard.
     *
     * @param v the out of focus <code>TextView</code>.
     */
    private void requestViewFocus(TextView v) {
        v.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * Clear the focus on the given <code>TextView</code> the hide the keyboard.
     *
     * @param v the <code>TextView</code> in focus.
     */
    private void clearViewFocus(TextView v) {
        v.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    /**
     * Switches to the opposite tag.
     *
     * @param tabTag the tag to reverse.
     * @return the opposite tag.
     */
    private String reverseTag(String tabTag) {
        if (tabTag.equals(Debt.I_OWE_TAG)) {
            tabTag = Debt.OWE_ME_TAG;
        } else {
            tabTag = Debt.I_OWE_TAG;
        }
        return tabTag;
    }

    /**
     * Get ISO 3166-1 alpha-2 country code for this device (or <code>null</code> if not available).
     *
     * @param context <code>Context</code> reference to get the <code>TelephonyManager</code> instance from.
     * @return country code or <code>null</code>.
     */
    private static String getUserCountry(Context context) {// TODO: 21/09/2015 test on tablet
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
            } else {
//                Locale.Builder builder = new Locale.Builder();// TODO: 22/09/2015 for tablet / no network no sim
//                builder.setRegion(Locale.getDefault().getCountry());
//                Locale locale=builder.build();
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Returns the next empty <code>TextView</code>.
     *
     * @param view the <code>TextView</code> to start the search from (not including).
     * @return next empty field, or <code>null</code> if all fields are filled.
     */
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


    //**********************************************************************************************
    //**************************************** Contacts search methods: ****************************
    //**********************************************************************************************

    /**
     * Prepares the <code>SearchView</code>.
     *
     * @param menu the <code>Menu</code> from the parameter given to {@link #onCreateOptionsMenu}
     */
    private void setupSearch(Menu menu) {
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchViewMenuItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchViewMenuItem.getActionView();
        setSearchTextColors();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        setSearchIcons();
    }

    /**
     * Changes the hint and text colors of the <code>SearchView</code>.
     */
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
        autoComplete.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    closeSearchView();
                    focusOnNextEmptyOrDone(v);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Changes the icons of the <code>SearchView</code>, using Java's reflection.
     */
    private void setSearchIcons() {
        try {
            Field searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            closeBtn = (ImageView) searchField.get(searchView);
            closeBtn.setImageResource(R.drawable.ic_close_white_36dp);

            searchField = SearchView.class.getDeclaredField("mVoiceButton");
            searchField.setAccessible(true);
            ImageView voiceBtn = (ImageView) searchField.get(searchView);
            voiceBtn.setImageResource(R.drawable.ic_keyboard_voice_white_36dp);

            searchField = SearchView.class.getDeclaredField("mSearchButton");
            searchField.setAccessible(true);
            ImageView searchButton = (ImageView) searchField.get(searchView);
            searchButton.setImageResource(R.drawable.ic_search_white_36dp);

            // Accessing the SearchAutoComplete
            int queryTextViewId = getResources().getIdentifier("android:id/search_src_text", null, null);
            View autoComplete = searchView.findViewById(queryTextViewId);

            Class<?> clazz = Class.forName("android.widget.SearchView$SearchAutoComplete");

            SpannableStringBuilder stopHint = new SpannableStringBuilder("   ");
            stopHint.append(getString(R.string.findContact));

            // Add the icon as an spannable
            Drawable searchIcon = getResources().getDrawable(R.drawable.ic_search_white_36dp);
            Method textSizeMethod = clazz.getMethod("getTextSize");
            Float rawTextSize = (Float) textSizeMethod.invoke(autoComplete);
            int textSize = (int) (rawTextSize * 1.25);
            if (searchIcon != null) {
                searchIcon.setBounds(0, 0, textSize, textSize);
            }
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
        // Handle the search intent
        if (ContactsContract.Intents.SEARCH_SUGGESTION_CLICKED.equals(intent.getAction())) {
            // Suggestion clicked mQuery
            String displayName = getDisplayNameForContact(intent);
            debtOwnerText.setText(displayName);
            String phone = getPhoneNumber(displayName);
            debtPhoneText.setText(phone);
            closeSearchView();
            focusOnNextEmptyOrDone(debtTitleText);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) { // REMOVE: 14/09/2015
            // Other query
            closeSearchView();
            focusOnNextEmptyOrDone(debtTitleText);
        }
    }

    private void closeSearchView() {
        closeBtn.performClick();
        closeBtn.performClick();
    }

    /**
     * Get contact's display name by the search mQuery.
     *
     * @param intent the <code>Intent</code> the started the search.
     * @return contact's display name.
     */
    private String getDisplayNameForContact(Intent intent) {
        Cursor phoneCursor = getContentResolver().query(intent.getData(), null, null, null, null);
        phoneCursor.moveToFirst();
        int idDisplayName = phoneCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        String name = phoneCursor.getString(idDisplayName);
        phoneCursor.close();
        return name;
    }

    /**
     * Extract the phone number by display name.
     *
     * @param name contacts display name.
     * @return the phone number of the contact.
     */
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


    //**********************************************************************************************
    //**************************************** Alarm methods: **************************************
    //**********************************************************************************************

    /**
     * Sets a new notification alarm.
     *
     * @param debt with valid dueDate.
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
     * @param debt to cancel.
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
