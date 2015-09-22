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
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.ui.ParseLoginBuilder;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final int LOGIN_ACTIVITY_CODE = 100;
    static final int EDIT_ACTIVITY_CODE = 200;
    static final int EDIT_ACTIVITY_FRAGMENT_CODE = 65736;
    static final String USER_CHANNEL_PREFIX = "t";

    private static final int I_OWE_TAB_INDEX = 0;
    private static final int OWE_ME_TAB_INDEX = 1;

    private static final String ALARM_SCHEME = "timer:";

    private static final boolean SHOW_LOGIN_ON_ERROR = true;

    private boolean _isShowLoginOnFail = false;
    private boolean _wasSignupShowen = false;

    private int numPinned;//// TODO: 05/09/2015 remove
    private int numSaved;//// TODO: 05/09/2015 remove

    ListViewFragment iOweViewFragment;
    ListViewFragment oweMeViewFragment;
    ChartFragment oweMeChartFragment;
    ChartFragment iOweChartFragment;

    MenuItem loginMenuItem;
    MenuItem logoutMenuItem;
    MenuItem chartModeMenuItem;
    MenuItem listModeMenuItem;

    private boolean isChartMode;


//    ListViewFragment iOweViewFragmentWithTag;
//    ListViewFragmentOweMe oweMeViewFragmentWithTag;


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
//        if (iOweViewFragmentWithTag == null) {
//            iOweViewFragmentWithTag = (ListViewFragment) getSupportFragmentManager().findFragmentByTag(Debt.I_OWE_TAG);
//        }
//        if (oweMeViewFragmentWithTag == null) {
//            oweMeViewFragmentWithTag = (ListViewFragmentOweMe) getSupportFragmentManager().findFragmentByTag(Debt.OWE_ME_TAG);
//        }
        // Check if we have a real user
        if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            // Sync data to Parse
            syncDebtsToParse(!SHOW_LOGIN_ON_ERROR);// TODO: 19/09/2015 make sure it's called after on result from login, so no accidental debts are uploaded
            // Update the logged in label info
            updateLoggedInInfo();
        }
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Debt.KEY_TAB_TAG)) {
            String tabTag = intent.getStringExtra(Debt.KEY_TAB_TAG);
            ActionBar actionBar = getSupportActionBar();
            if (tabTag.equals(Debt.I_OWE_TAG)) {
                actionBar.selectTab(actionBar.getTabAt(I_OWE_TAB_INDEX));
            } else {
                actionBar.selectTab(actionBar.getTabAt(OWE_ME_TAB_INDEX));
            }
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
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.addTab(actionBar.newTab()
                    .setText(getString(R.string.i_owe_tab_title))
                    .setTabListener(new ActionBar.TabListener() {
                        @Override
                        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                            if (iOweViewFragment == null || iOweViewFragmentWithTag == null) {
//                                iOweViewFragment = new ListViewFragment();
//                                fragmentTransaction.replace(android.R.id.content, iOweViewFragment, Debt.I_OWE_TAG);
//                                iOweViewFragmentWithTag = (ListViewFragment) getSupportFragmentManager().findFragmentByTag(Debt.I_OWE_TAG);
//                            } else {
                            if (isChartMode) {
                                if (iOweChartFragment == null) {
                                    iOweChartFragment = new ChartFragment();
                                }
                                fragmentTransaction.replace(android.R.id.content, iOweChartFragment, Debt.I_OWE_TAG);
                            } else {
                                if (iOweViewFragment == null) {
                                    iOweViewFragment = new ListViewFragment();// TODO: 9/18/2015 update on login
                                }
                                fragmentTransaction.replace(android.R.id.content, iOweViewFragment, Debt.I_OWE_TAG);
                            }
//                            }
                        }

                        @Override
                        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                        }

                        @Override
                        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                            onTabSelected(tab,fragmentTransaction);
                        }
                    }));
            actionBar.addTab(actionBar.newTab()
                    .setText(getString(R.string.owe_me_tab_title))
                    .setTabListener(new ActionBar.TabListener() {
                        @Override
                        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                            if (oweMeViewFragment == null || oweMeViewFragmentWithTag == null) {
//                                oweMeViewFragment = new ListViewFragmentOweMe();
//                                fragmentTransaction.replace(android.R.id.content, oweMeViewFragment, Debt.OWE_ME_TAG);
//                                oweMeViewFragmentWithTag = (ListViewFragmentOweMe) getSupportFragmentManager().findFragmentByTag(Debt.OWE_ME_TAG);
//                            } else {
                            if (isChartMode) {
                                if (oweMeChartFragment == null) {
                                    oweMeChartFragment = new ChartFragment();
                                }
                                fragmentTransaction.replace(android.R.id.content, oweMeChartFragment, Debt.OWE_ME_TAG);
                            } else {
                                if (oweMeViewFragment == null) {
                                    oweMeViewFragment = new ListViewFragment();
                                }
                                fragmentTransaction.replace(android.R.id.content, oweMeViewFragment, Debt.OWE_ME_TAG);
                            }
//                            }
                        }

                        @Override
                        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                        }

                        @Override
                        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                            onTabSelected(tab,fragmentTransaction);
                        }
                    }));
//            actionBar.addTab(actionBar.newTab()
//                    .setText(getString(R.string.dashboard_tab_title))
//                    .setTabListener(new ActionBar.TabListener() {
//                        @Override
//                        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                            if (iOweChartFragment == null) {
//                                iOweChartFragment = new ChartFragment();
//                            }
//                            fragmentTransaction.replace(android.R.id.content, iOweChartFragment, DASHBOARD_TAB_TAG);
//                        }
//
//                        @Override
//                        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                        }
//
//                        @Override
//                        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//                        }
//                    }));// REMOVE: 22/09/2015
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
        loginMenuItem = menu.findItem(R.id.action_login);
        logoutMenuItem = menu.findItem(R.id.action_logout);
        chartModeMenuItem = menu.findItem(R.id.action_chart_mode);
        listModeMenuItem = menu.findItem(R.id.action_list_mode);

        chartModeMenuItem.setVisible(!isChartMode);
        listModeMenuItem.setVisible(isChartMode);

        loginMenuItem.setVisible(!isRealUser);
        logoutMenuItem.setVisible(isRealUser);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ActionBar actionBar = getSupportActionBar();
        switch (item.getItemId()) {
            case R.id.action_sync:
                syncDebtsToParse(SHOW_LOGIN_ON_ERROR);
                break;

            case R.id.action_chart_mode:
                isChartMode = true;
                chartModeMenuItem.setVisible(!isChartMode);
                listModeMenuItem.setVisible(isChartMode);
                actionBar.getSelectedTab().select();
                break;

            case R.id.action_list_mode:
                isChartMode = false;
                chartModeMenuItem.setVisible(!isChartMode);
                listModeMenuItem.setVisible(isChartMode);
                actionBar.getSelectedTab().select();
                break;

            case R.id.action_logout:
                logoutFromParse();
                break;

            case R.id.action_login:
                openLoginView();
                break;
            case R.id.about:
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
                String info = "\nphone : " + curr.getString("phone") + "\nuser: " + curr.getUsername() + "\nisAuth: " + isAuth + "\nisDataAvai: " + isDataAvai + "\nisNew: " + isNew + "\nisDirty: " + isDirty + (isDirtyFixed ? " (fixed)" : "") + "\nkeys: " + keys + "\ndirtyKey: " + dirtyKey + "\nnumDirty: " + numDirty + "\ntoken: " + token + "\nisLinked: " + isLinked + "\npinned: " + numPinned + "\nsaved: " + numSaved;

//                content.setText(Html.fromHtml(getString(R.string.about_body)));// UNCOMMENT: 14/09/2015
                content.setText(info);// REMOVE: 14/09/2015
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
                if (data != null && data.hasExtra(Debt.KEY_TAB_TAG)) {
//                    String tabTag = data.getStringExtra(Debt.KEY_TAB_TAG);
//                    if (tabTag.equals(Debt.I_OWE_TAG) && iOweViewFragmentWithTag != null) {
//                        iOweViewFragment.updateView();
//                    } else if (oweMeViewFragmentWithTag != null) {
//                        oweMeViewFragmentWithTag.updateView();
//                    }
                }
            } else if (requestCode == LOGIN_ACTIVITY_CODE) {
                subscribeToPush();
                // If the user is new, sync data to Parse, otherwise get the current list from Parse
                if (ParseUser.getCurrentUser().isNew()) {
                    syncDebtsToParse(SHOW_LOGIN_ON_ERROR);// FIXME: 06/09/2015 add if
                } else {
                    loadFromParse();
                }
            }
            updateLoggedInInfo();// TODO: 05/09/2015 remove?
        }

    }

    private void subscribeToPush() {
        List<String> subscribedChannels = ParseInstallation.getCurrentInstallation().getList("channels");
        String currUserChannel = USER_CHANNEL_PREFIX + ParseUser.getCurrentUser().getString("phone").replaceAll("[^0-9]+", "");
        if (subscribedChannels == null || !subscribedChannels.contains(currUserChannel)) {
            ParsePush.subscribeInBackground(currUserChannel);
        }
    }

    private void unsubscribeFromPush() {
        List<String> subscribedChannels = ParseInstallation.getCurrentInstallation().getList("channels");
        String currUserChannel = USER_CHANNEL_PREFIX + ParseUser.getCurrentUser().getString("phone").replaceAll("[^0-9]+", "");
        if (subscribedChannels != null && subscribedChannels.contains(currUserChannel)) {
            ParsePush.unsubscribeInBackground(currUserChannel);
        }
    }

    private void openLoginView() {
        ParseLoginBuilder builder = new ParseLoginBuilder(getApplicationContext());
        startActivityForResult(builder.build(), LOGIN_ACTIVITY_CODE);
    }

    private void logoutFromParse() {
        unsubscribeFromPush();
        // Log out the current user
        ParseUser.logOut();
        // Create a new anonymous user
        ParseAnonymousUtils.logIn(null);// FIXME: 02/09/2015
        // Clear the view
//        if (iOweViewFragmentWithTag != null) {
        iOweViewFragment.clearView();// FIXME: 17/09/2015
//        }
//        if (oweMeViewFragmentWithTag != null) {
//            oweMeViewFragmentWithTag.clearView();
//        }
        // Unpin all the current objects
        ParseObject.unpinAllInBackground(DebtListApplication.DEBT_GROUP_NAME);
        cancelAllAlarmsOnPinnedObjects();// TODO: 09/09/2015 not only pinned ?
        // Update the logged in label info
        updateLoggedInInfo();
    }


    private void syncDebtsToParse(final boolean isShowLoginOnFail) {
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
                                            // Let adapter know to update view
                                            if (!isFinishing()) {
                                                // REMOVE: 07/09/2015 debtListAdapter.notifyDataSetChanged();
//                                                if (debt.getTabTag().equals(Debt.I_OWE_TAG) && iOweViewFragmentWithTag != null) {
//                                                    iOweViewFragment.updateView();
//                                                } else if (oweMeViewFragmentWithTag != null) {
//                                                    oweMeViewFragmentWithTag.updateView();
//                                                }
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
//                                            if (iOweViewFragmentWithTag != null) {
//                                                iOweViewFragment.updateView();
//                                            }
//                                            if (oweMeViewFragment != null) {
//                                                oweMeViewFragment.updateView();
//                                            }
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
        alertIntent.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());

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

    private void handleParseError(ParseException e) {
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