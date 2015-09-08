package com.melnykov.fab.sample;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SendCallback;
import com.parse.ui.ParseLoginBuilder;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int LOGIN_ACTIVITY_CODE = 100;
    public static final int EDIT_ACTIVITY_CODE = 200;
    public static final int EDIT_ACTIVITY_FRAGMENT_CODE = 65736;

    private static final String ALARM_SCHEME = "timer:";

    private static final boolean SHOW_LOGIN_ON_ERROR = true;
    private static final boolean SUBSCRIBE_TO_PARSE_CHANNEL = true;

    private boolean _isShowLoginOnFail = false;
    private boolean _wasSignupShowen = false;

    private int numPinned;//// TODO: 05/09/2015 remove
    private int numSaved;//// TODO: 05/09/2015 remove
    ListViewFragment listIOweViewFragment;
    ListViewFragment listOweMeViewFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();
/*        System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ on create");
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Debt.KEY_UUID)) {
            String uuid = intent.getStringExtra(Debt.KEY_UUID);
            openEditView(uuid);
        }*/// REMOVE: 08/09/2015
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we have a real user
        if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            // Sync data to Parse
            syncDebtsToParse(!SHOW_LOGIN_ON_ERROR, !SUBSCRIBE_TO_PARSE_CHANNEL);
            // Update the logged in label info
            updateLoggedInInfo();
        }
    }

/*    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.hasExtra(Debt.KEY_UUID)) {
            String uuid = intent.getStringExtra(Debt.KEY_UUID);
            intent.removeExtra(Debt.KEY_UUID);
            openEditView(uuid);
        }
    }*/// REMOVE: 08/09/2015

    @SuppressWarnings("deprecation")
    private void initActionBar() {
        listIOweViewFragment = new ListViewFragment();
        listOweMeViewFragment = new ListViewFragment();
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.addTab(actionBar.newTab()
                    .setText("I owe :(")
                    .setTabListener(new ActionBar.TabListener() {
                        @Override
                        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                            if (listIOweViewFragment == null) {
                                listIOweViewFragment = new ListViewFragment();
                            }
                            fragmentTransaction.replace(android.R.id.content, listIOweViewFragment);
                        }

                        @Override
                        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                        }

                        @Override
                        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                        }
                    }));
            actionBar.addTab(actionBar.newTab()
                    .setText("Owe me :)")
                    .setTabListener(new ActionBar.TabListener() {
                        @Override
                        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                            if (listOweMeViewFragment == null) {
                                listOweMeViewFragment = new ListViewFragment();
                            }
                            fragmentTransaction.replace(android.R.id.content, listOweMeViewFragment);
                        }

                        @Override
                        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                        }

                        @Override
                        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                        }
                    }));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean isRealUser = !ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser());
        menu.findItem(R.id.action_login).setVisible(!isRealUser);
        menu.findItem(R.id.action_logout).setVisible(isRealUser);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sync:
                syncDebtsToParse(SHOW_LOGIN_ON_ERROR, !SUBSCRIBE_TO_PARSE_CHANNEL);
                break;

            case R.id.action_logout:
                logoutFromParse();
                break;

            case R.id.action_login:
                openLoginView();
                break;
            case R.id.about:
                ParsePush push = new ParsePush();
                push.setChannel(ParseUser.getCurrentUser().getEmail());
                push.setMessage("The Giants just scored! It's now 2-2 against the Mets.");
                push.sendInBackground(new SendCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e==null){

                        }
                    }
                });
                TextView content = (TextView) getLayoutInflater().inflate(R.layout.about_view, null);
                content.setMovementMethod(LinkMovementMethod.getInstance());

                // REMOVE: 07/09/2015 info
                ParseUser curr = ParseUser.getCurrentUser();
                String token = curr.getSessionToken();
                boolean isAuth = curr.isAuthenticated();
                boolean isDataAvai = curr.isDataAvailable();
                boolean isNew = curr.isNew();
                boolean isDirty = curr.isDirty();
                boolean isDirtyFixed = false;
                boolean isLinked = ParseAnonymousUtils.isLinked(curr);
                countSavedAndPinnedObjects();
                String dirtyKey = null;
                String keys = Arrays.toString(curr.keySet().toArray());
                int numDirty = 0;
                if (isDirty) {
                    for (String key : curr.keySet()) {
                        if (curr.isDirty(key)) {
                            numDirty++;
                            dirtyKey = key;
                        }
                    }
                    // TODO: 05/09/2015 fix dirty
                    curr = ParseUser.getCurrentUser();
                    isDirty = curr.isDirty();
                    if (!isDirty) {
                        isDirtyFixed = true;
                    }
                }
                String info = "\nuser: " + curr.getUsername() + "\nisAuth: " + isAuth + "\nisDataAvai: " + isDataAvai + "\nisNew: " + isNew + "\nisDirty: " + isDirty + (isDirtyFixed ? " (fixed)" : "") + "\nkeys: " + keys + "\ndirtyKey: " + dirtyKey + "\nnumDirty: " + numDirty + "\ntoken: " + token + "\nisLinked: " + isLinked + "\npinned: " + numPinned + "\nsaved: " + numSaved;

                content.setText(info/*Html.fromHtml(getString(R.string.about_body))*/);// FIXME: 07/09/2015 about text
                new AlertDialog.Builder(this)
                        .setTitle(R.string.about)
                        .setView(content)
                        .setInverseBackgroundForced(true)// FIXME: 06/09/2015
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void countSavedAndPinnedObjects() {
        ParseQuery<Debt> query = Debt.getQuery();
        query.fromPin(DebtListApplication.DEBT_GROUP_NAME);
        query.findInBackground(new FindCallback<Debt>() {
            public void done(List<Debt> debts, ParseException e) {
                if (debts != null) {
                    numPinned = debts.size();
                } else {
                    numPinned = -1;
                }
                if (e != null) {
                    numPinned = -2;
                }
            }
        });
        query = Debt.getQuery();
        query.whereEqualTo("author", ParseUser.getCurrentUser());
        query.findInBackground(new FindCallback<Debt>() {
            public void done(List<Debt> debts, ParseException e) {
                if (debts != null) {
                    numSaved = debts.size();
                } else {
                    numSaved = -1;
                }
                if (e != null) {
                    numSaved = -2;
                }
            }
        });
    }

    private void cancelAllAlarmsOnPinnedObjects() {
        ParseQuery<Debt> query = Debt.getQuery();
        query.fromPin(DebtListApplication.DEBT_GROUP_NAME);
        query.findInBackground(new FindCallback<Debt>() {
            public void done(List<Debt> debts, ParseException e) {
                if (debts != null) {
                    for (final Debt debt : debts) {
                        Date dueDate = debt.getDueDate();
                        if (dueDate != null) {
                            cancelAlarm(debt);
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // An OK result means the pinned dataset changed or
        // log in was successful
        if (resultCode == RESULT_OK) {
            if (requestCode == EDIT_ACTIVITY_CODE || requestCode == EDIT_ACTIVITY_FRAGMENT_CODE) {
                // Coming back from the edit view, update the view
                // REMOVE: 07/09/2015 debtListAdapter.loadObjects();
                listIOweViewFragment.updateView();
            } else if (requestCode == LOGIN_ACTIVITY_CODE) {
                // If the user is new, sync data to Parse,
                // else get the current list from Parse
                syncDebtsToParse(SHOW_LOGIN_ON_ERROR, SUBSCRIBE_TO_PARSE_CHANNEL);// FIXME: 06/09/2015 add if
                if (ParseUser.getCurrentUser().isNew()) {
                } else {
                    loadFromParse();
                }
            }
            updateLoggedInInfo();// TODO: 05/09/2015 remove?
        }

    }

    private void openLoginView() {
        ParseLoginBuilder builder = new ParseLoginBuilder(getApplicationContext());
        startActivityForResult(builder.build(), LOGIN_ACTIVITY_CODE);
    }

    private void logoutFromParse() {
        // Log out the current user
        ParseUser.logOut();
        // Create a new anonymous user
        ParseAnonymousUtils.logIn(null);// FIXME: 02/09/2015
        // Clear the view
        // REMOVE: 07/09/2015 debtListAdapter.clear();
        listIOweViewFragment.clearView();
        // Unpin all the current objects
        ParseObject.unpinAllInBackground(DebtListApplication.DEBT_GROUP_NAME);
        cancelAllAlarmsOnPinnedObjects();
        // Update the logged in label info
        updateLoggedInInfo();
    }

    private void syncDebtsToParse(final boolean isShowLoginOnFail, final boolean isSubscribeToChannel) {
        // We could use saveEventually here, but we want to have some UI
        // around whether or not the draft has been saved to Parse
        _wasSignupShowen = false;// FIXME: 06/09/2015 ?
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if ((ni != null) && (ni.isConnected())) {
            if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
                // If we have a network connection and a current logged in user, sync the debts
                // In this app, local changes should overwrite content on the server.
                ParseQuery<Debt> query = Debt.getQuery();
                query.fromPin(DebtListApplication.DEBT_GROUP_NAME);
                query.whereEqualTo("isDraft", true);
                query.findInBackground(new FindCallback<Debt>() {
                    public void done(List<Debt> debts, ParseException e) {
                        if (e == null) {
                            for (final Debt debt : debts) {
                                // Set is draft flag to false before
                                // syncing to Parse
                                debt.setDraft(false);
                                debt.saveInBackground(new SaveCallback() {// FIXME: 04/09/2015

                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            if (isSubscribeToChannel) {
                                                ParsePush.subscribeInBackground(ParseUser.getCurrentUser().getEmail());
                                            }
                                            // Let adapter know to update view
                                            if (!isFinishing()) {
                                                // REMOVE: 07/09/2015 debtListAdapter.notifyDataSetChanged();
                                                listIOweViewFragment.updateView();
                                            }
                                        } else {
                                            if (!isShowLoginOnFail) {
/*                                                Toast.makeText(getApplicationContext(),
                                                        e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();*/// REMOVE: 09/09/2015
                                            }
                                            // Reset the is draft flag locally to true
                                            debt.setDraft(true);
                                            // Save flag field as late as possible - to deal with
                                            // asynchronous callback
                                            _isShowLoginOnFail = isShowLoginOnFail;
                                            handleParseError(e);// FIXME: 05/09/2015
                                        }
                                    }

                                });
                            }
                        } else {
                            Log.i("DebtListActivity",
                                    "syncDebtsToParse: Error finding pinned debts: "
                                            + e.getMessage());
                        }
                    }
                });
            } else {
                // If we have a network connection but no logged in user, direct
                // the person to log in or sign up.
                openLoginView();
            }
        } else {
            // If there is no connection, let the user know the sync didn't
            // happen
            Toast.makeText(
                    getApplicationContext(),
                    "Your device appears to be offline. Some debts may not have been synced to Parse.",
                    Toast.LENGTH_LONG).show();
        }

    }

    private void loadFromParse() {
        ParseQuery<Debt> query = Debt.getQuery();
        query.whereEqualTo("author", ParseUser.getCurrentUser());
        query.findInBackground(new FindCallback<Debt>() {
            public void done(final List<Debt> debts, ParseException e) {
                if (e == null) {
                    ParseObject.pinAllInBackground(debts,
                            new SaveCallback() {
                                public void done(ParseException e) {
                                    if (e == null) {
                                        if (!isFinishing()) {
                                            for (final Debt debt : debts) {
                                                Date dueDate = debt.getDueDate();
                                                if (dueDate != null) {
                                                    setAlarm(debt);
                                                } else {
                                                    cancelAlarm(debt);
                                                }
                                            }
                                            // REMOVE: 07/09/2015 debtListAdapter.loadObjects();
                                            listIOweViewFragment.updateView();
                                        }
                                    } else {
                                        Log.i("DebtListActivity",
                                                "Error pinning debts: "
                                                        + e.getMessage());
                                    }
                                }
                            });
                } else {
                    Log.i("DebtListActivity",
                            "loadFromParse: Error finding pinned debts: "
                                    + e.getMessage());
                }
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
        String schemeSpecificPart = debt.getUuidString();
        int alarmId = schemeSpecificPart.hashCode();

        alertIntent.putExtra(Debt.KEY_TITLE, debt.getTitle());
        alertIntent.putExtra(Debt.KEY_OWNER, debt.getOwner());

        alertIntent.setData(Uri.parse(ALARM_SCHEME + schemeSpecificPart));

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
        String schemeSpecificPart = debt.getUuidString();
        int alarmId = schemeSpecificPart.hashCode();

        alertIntent.setData(Uri.parse(ALARM_SCHEME + schemeSpecificPart));

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(PendingIntent.getBroadcast(this, alarmId, alertIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Toast.makeText(this, "REMOVED Reminder " + alarmId, Toast.LENGTH_LONG).show(); // REMOVE: 07/09/2015
    }


    private void updateLoggedInInfo() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }
        if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            ParseUser currentUser = ParseUser.getCurrentUser();
            getSupportActionBar().setTitle(getString(R.string.logged_in,
                    currentUser.getString("name")));
        } else {
            getSupportActionBar().setTitle(getResources().getString(R.string.not_logged_in));
        }
    }

    public void handleParseError(ParseException e) {
        handleInvalidSessionToken();// TODO: 05/09/2015

        /*        switch (e.getCode()) {
            case ParseException.INVALID_SESSION_TOKEN:
                handleInvalidSessionToken();
                break;

            // Other Parse API errors
        }*/
    }

    private void handleInvalidSessionToken() {
        //--------------------------------------
        // Option 1: Show a message asking the user to log out and log back in.// REMOVE: 06/09/2015
        //--------------------------------------
        // If the user needs to finish what they were doing, they have the opportunity to do so.
        //
        // new AlertDialog.Builder(getActivity())
        //   .setMessage("Session is no longer valid, please log out and log in again.")
        //   .setCancelable(false).setPositiveButton("OK", ...).create().show();

        //--------------------------------------
        // Option #2: Show login screen so user can re-authenticate.
        //--------------------------------------
        // You may want this if the logout button could be inaccessible in the UI.
        //
        // startActivityForResult(new ParseLoginBuilder(getActivity()).build(), 0);
        if (_isShowLoginOnFail && !_wasSignupShowen) {
            // only in case the user initiated the sync - no demanding login
            _wasSignupShowen = true;
            openLoginView();
        } else {
/*            Toast.makeText(getApplicationContext(),
                    "Didn't show login\nisShowLoginOnFail: " + _isShowLoginOnFail + "\nwasSignupShowen: " + _wasSignupShowen,
                    Toast.LENGTH_SHORT).show();// REMOVE: 06/09/2015*/
        }
    }


}