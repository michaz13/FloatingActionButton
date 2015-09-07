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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;
import com.parse.FindCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.ui.ParseLoginBuilder;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int LOGIN_ACTIVITY_CODE = 100;
    public static final int EDIT_ACTIVITY_CODE = 200;
    public static final int EDIT_ACTIVITY_FRAGMENT_CODE = 65736;
    private static final boolean SHOW_LOGIN_ON_ERROR = true;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if we have a real user
        if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            // Sync data to Parse
            syncDebtsToParse(!SHOW_LOGIN_ON_ERROR);
            // Update the logged in label info
            updateLoggedInInfo();
        }
    }

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
                            if(listIOweViewFragment == null) {
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
                            if(listOweMeViewFragment == null) {
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
        if (item.getItemId() == R.id.action_sync) {
            syncDebtsToParse(SHOW_LOGIN_ON_ERROR);
        }

        if (item.getItemId() == R.id.action_logout) {
            logoutFromParse();
        }

        if (item.getItemId() == R.id.action_login) {
            openLoginView();
        }
        if (item.getItemId() == R.id.about) {
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
                syncDebtsToParse(SHOW_LOGIN_ON_ERROR);// FIXME: 06/09/2015 add if
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
                                                listIOweViewFragment.updateView();
                                            }
                                        } else {
                                            if (!isShowLoginOnFail) {
                                                Toast.makeText(getApplicationContext(),
                                                        e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
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
            public void done(List<Debt> debts, ParseException e) {
                if (e == null) {
                    ParseObject.pinAllInBackground(debts,
                            new SaveCallback() {
                                public void done(ParseException e) {
                                    if (e == null) {
                                        if (!isFinishing()) {
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
            Toast.makeText(getApplicationContext(),
                    "Didn't show login\nisShowLoginOnFail: " + _isShowLoginOnFail + "\nwasSignupShowen: " + _wasSignupShowen,
                    Toast.LENGTH_SHORT).show();// REMOVE: 06/09/2015
        }
    }





}