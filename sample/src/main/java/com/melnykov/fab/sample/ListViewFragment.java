package com.melnykov.fab.sample;


import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.melnykov.fab.FloatingActionButton;
import com.melnykov.fab.ScrollDirectionListener;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class ListViewFragment extends android.support.v4.app.Fragment {

    // Adapter for the Debts Parse Query
    ParseQueryAdapter<Debt> debtListAdapter;

    // For showing empty and non-empty debt views
    private ListView debtListView;
    private LinearLayout noDebtsView;

    private View mRoot;

    ParseQueryAdapter.QueryFactory<Debt> factory;

    public ListViewFragment() {
        // Set up the Parse mQuery to use in the adapter
        factory = new ParseQueryAdapter.QueryFactory<Debt>() {
            public ParseQuery<Debt> create() {
                ParseQuery<Debt> query = Debt.getQuery();
                query.whereEqualTo(Debt.KEY_TAB_TAG, getTag());
                query.orderByAscending("createdAt");
                query.fromLocalDatastore();
                return query;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRoot != null) {
            return mRoot;
        }

        View root = inflater.inflate(R.layout.fragment_listview, container, false);
        // Set up the views
        debtListView = (ListView) root.findViewById(android.R.id.list);
        noDebtsView = (LinearLayout) root.findViewById(R.id.no_debts_view);
        debtListView.setEmptyView(noDebtsView);

        // Set up the adapter
        debtListAdapter = new DebtListAdapter(getActivity(), factory);

        // Attach the mQuery adapter to the view
        debtListView.setAdapter(debtListAdapter);

        debtListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Debt debt = debtListAdapter.getItem(position);
                openEditView(debt);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
                i.putExtra(Debt.KEY_TAB_TAG, getTag());
                startActivityForResult(i , MainActivity.EDIT_ACTIVITY_CODE);
            }
        });
        fab.attachToListView(debtListView, new ScrollDirectionListener() {// REMOVE: 07/09/2015 listener
            @Override
            public void onScrollDown() {

            }

            @Override
            public void onScrollUp() {

            }
        }, new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        mRoot = root;
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    // Helper methods: -----------------------------------------------------------------------------
    private void openEditView(Debt debt) {
        Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        startActivityForResult(i, MainActivity.EDIT_ACTIVITY_CODE);
    }

    private void updateView() {
        debtListAdapter.loadObjects();// REMOVE: 07/09/2015 ?
        debtListAdapter.notifyDataSetChanged();
    }

    void clearView() {
        debtListAdapter.clear();
    }
}
