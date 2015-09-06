package com.melnykov.fab.sample;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ObservableScrollView;
import com.melnykov.fab.ScrollDirectionListener;
import com.parse.FindCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.ui.ParseLoginBuilder;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActionBar();
    }



    @SuppressWarnings("deprecation")
    private void initActionBar() {
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.addTab(actionBar.newTab()
                    .setText("I owe :(")
                    .setTabListener(new ActionBar.TabListener() {
                        @Override
                        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                            fragmentTransaction.replace(android.R.id.content, new ListViewFragment());
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
                            fragmentTransaction.replace(android.R.id.content, new RecyclerViewFragment());
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about) {
            TextView content = (TextView) getLayoutInflater().inflate(R.layout.about_view, null);
            content.setMovementMethod(LinkMovementMethod.getInstance());
            content.setText(Html.fromHtml(getString(R.string.about_body)));
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

    public static class ListViewFragment extends Fragment {

        private static final int LOGIN_ACTIVITY_CODE = 100;
        private static final int EDIT_ACTIVITY_CODE = 200;
        private static final boolean SHOW_LOGIN_ON_FAIL = true;

        private boolean _isShowLoginOnFail = false;
        private boolean _wasSignupShowen = false;

        private int numPinned;//// TODO: 05/09/2015 remove
        private int numSaved;//// TODO: 05/09/2015 remove

        // Adapter for the Debts Parse Query
        ParseQueryAdapter<Debt> debtListAdapter;

        // For showing empty and non-empty debt views
        private ListView debtListView;
        private LinearLayout noDebtsView;

        private TextView loggedInInfoView;

        @SuppressLint("InflateParams")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_listview, container, false);
            // Set up the views
            debtListView = (ListView) root.findViewById(android.R.id.list);
            noDebtsView = (LinearLayout) root.findViewById(R.id.no_debts_view);
            debtListView.setEmptyView(noDebtsView);
            loggedInInfoView = (TextView) root.findViewById(R.id.loggedin_info);

            // Set up the Parse query to use in the adapter
            ParseQueryAdapter.QueryFactory<Debt> factory = new ParseQueryAdapter.QueryFactory<Debt>() {
                public ParseQuery<Debt> create() {
                    ParseQuery<Debt> query = Debt.getQuery();
                    query.orderByDescending("createdAt");
                    query.fromLocalDatastore();
                    return query;
                }
            };
            // Set up the adapter
            debtListAdapter = new DebtListAdapter(getActivity(), factory);

            // Attach the query adapter to the view
            debtListView.setAdapter(debtListAdapter);

            debtListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Debt debt = debtListAdapter.getItem(position);
                    openEditView(debt);
                }
            });
            updateLoggedInInfo();// TODO: 05/09/2015 remove
            FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.fab);
            fab.attachToListView(debtListView, new ScrollDirectionListener() {
                @Override
                public void onScrollDown() {
                    Log.d("ListViewFragment", "onScrollDown()");
                }

                @Override
                public void onScrollUp() {
                    Log.d("ListViewFragment", "onScrollUp()");
                }
            }, new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                    Log.d("ListViewFragment", "onScrollStateChanged()");
                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    Log.d("ListViewFragment", "onScroll()");
                }
            });

            return root;
        }

        private void openEditView(Debt debt) {
            Intent i = new Intent(getActivity(), EditDebtActivity.class);
            i.putExtra("ID", debt.getUuidString());
            startActivityForResult(i, EDIT_ACTIVITY_CODE);
        }


        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            // An OK result means the pinned dataset changed or
            // log in was successful
            if (resultCode == RESULT_OK) {
                if (requestCode == EDIT_ACTIVITY_CODE) {
                    // Coming back from the edit view, update the view
                    debtListAdapter.loadObjects();
                } else if (requestCode == LOGIN_ACTIVITY_CODE) {
                    // If the user is new, sync data to Parse,
                    // else get the current list from Parse
                    syncDebtsToParse(SHOW_LOGIN_ON_FAIL);// FIXME: 06/09/2015 add if
                    if (ParseUser.getCurrentUser().isNew()) {
                    } else {
                        loadFromParse();
                    }
                }
                updateLoggedInInfo();// TODO: 05/09/2015 remove?
            }

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
        }

        private void openLoginView() {
            ParseLoginBuilder builder = new ParseLoginBuilder(getActivity().getApplicationContext());
            ParseUser currentUser = ParseUser.getCurrentUser();
            if (!ParseAnonymousUtils.isLinked(currentUser)) {// FIXME: 05/09/2015
            }
            startActivityForResult(builder.build(), LOGIN_ACTIVITY_CODE);
        }

        private void logoutFromParse() {
            // Log out the current user
            ParseUser.logOut();
            // Create a new anonymous user
            ParseAnonymousUtils.logIn(null);// FIXME: 02/09/2015
            // Clear the view
            debtListAdapter.clear();
            // Unpin all the current objects
            ParseObject.unpinAllInBackground(DebtListApplication.DEBT_GROUP_NAME);
            // Update the logged in label info
            updateLoggedInInfo();
        }

        private void syncDebtsToParse(final boolean isShowLoginOnFail) {
            // We could use saveEventually here, but we want to have some UI
            // around whether or not the draft has been saved to Parse
            _wasSignupShowen = false;// FIXME: 06/09/2015 ?
            ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
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
                                                if (!getActivity().isFinishing()) {
                                                    debtListAdapter.notifyDataSetChanged();
                                                }
                                            } else {
                                                if (!isShowLoginOnFail) {
                                                    Toast.makeText(getActivity().getApplicationContext(),
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
                        getActivity().getApplicationContext(),
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
                                            if (!getActivity().isFinishing()) {
                                                debtListAdapter.loadObjects();
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

            // TODO: 05/09/2015 remove info
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
            if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
                ParseUser currentUser = ParseUser.getCurrentUser();
                loggedInInfoView.setText(getString(R.string.logged_in,
                        currentUser.getString("name")) + info);
            } else {
                loggedInInfoView.setText(getString(R.string.not_logged_in) + info);// TODO: 04/09/2015 remove info
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
                Toast.makeText(getActivity().getApplicationContext(),
                        "Didn't show login\nisShowLoginOnFail: "+_isShowLoginOnFail+"\nwasSignupShowen: "+ _wasSignupShowen,
                        Toast.LENGTH_SHORT).show();// REMOVE: 06/09/2015
            }
        }
    }

    public static class RecyclerViewFragment extends Fragment {// REMOVE: 06/09/2015
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_recyclerview, container, false);

            RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.recycler_view);
            recyclerView.setHasFixedSize(true);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

            RecyclerViewAdapter adapter = new RecyclerViewAdapter(getActivity(), getResources()
                    .getStringArray(R.array.countries));
            recyclerView.setAdapter(adapter);

            FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.fab);
            fab.attachToRecyclerView(recyclerView);

            return root;
        }
    }

    public static class ScrollViewFragment extends Fragment {// REMOVE: 06/09/2015
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_scrollview, container, false);

            ObservableScrollView scrollView = (ObservableScrollView) root.findViewById(R.id.scroll_view);
            LinearLayout list = (LinearLayout) root.findViewById(R.id.list);

            String[] countries = getResources().getStringArray(R.array.countries);
            for (String country : countries) {
                TextView textView = (TextView) inflater.inflate(R.layout.list_item, container, false);
                String[] values = country.split(",");
                String countryName = values[0];
                int flagResId = getResources().getIdentifier(values[1], "drawable", getActivity().getPackageName());
                textView.setText(countryName);
                textView.setCompoundDrawablesWithIntrinsicBounds(flagResId, 0, 0, 0);

                list.addView(textView);
            }

            FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.fab);
            fab.attachToScrollView(scrollView);

            return root;
        }
    }






    private static class DebtListAdapter extends ParseQueryAdapter<Debt> {
        private final Context mContext;

        public DebtListAdapter(Context context, QueryFactory<Debt> queryFactory) {
            super(context, queryFactory);
            mContext = context;
        }

        @Override
        public View getItemView(Debt debt, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view == null) {
                view = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
                holder = new ViewHolder();
                holder.debtTitle = (TextView) view
                        .findViewById(R.id.debt_title);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            TextView debtTitle = holder.debtTitle;

            // TODO: 05/09/2015 remove info
            ParseUser author = debt.getAuthor();
            String token = author.getSessionToken();
            boolean isAuth = author.isAuthenticated();
            boolean isDataAvai = author.isDataAvailable();
            boolean isNew = author.isNew();
            boolean isDirty = author.isDirty();
            boolean isLinked = ParseAnonymousUtils.isLinked(author);
//            String info = "\nauthor: "+author.getUsername()+"\nisAuth: "+isAuth+"\nisDataAvai: "+isDataAvai+"\nisNew: "+isNew+"\nisDirty: "+isDirty+"\ntoken: "+token+"\nisLinked: "+isLinked;


            debtTitle.setText(debt.getTitle());
            if (debt.isDraft()) {
                debtTitle.setTypeface(null, Typeface.ITALIC);
                debtTitle.setTextColor(Color.RED);// TODO: 02/09/2015 GRAY

            } else {
                debtTitle.setTypeface(null, Typeface.NORMAL);
                debtTitle.setTextColor(Color.BLACK);
            }
            return view;
        }
    }

    private static class ViewHolder {
        TextView debtTitle;
    }
}