package com.melnykov.fab.sample;


import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
public class ListViewFragmentIOwe extends android.support.v4.app.Fragment {

    // Adapter for the Debts Parse Query
    ParseQueryAdapter<Debt> debtListAdapterIOwe;

    // For showing empty and non-empty debt views
    private ListView debtListViewIOwe;
    private LinearLayout noDebtsViewIOwe;

    private View mRootIOwe;

    ParseQueryAdapter.QueryFactory<Debt> factoryIOwe;

    public ListViewFragmentIOwe() {
        // Set up the Parse query to use in the adapter
        factoryIOwe = new ParseQueryAdapter.QueryFactory<Debt>() {
            public ParseQuery<Debt> create() {
                ParseQuery<Debt> query = Debt.getQuery();
                query.whereEqualTo(Debt.KEY_TAB_TAG, getTag());
                query.orderByAscending("createdAt");
                query.fromLocalDatastore();
                return query;
            }
        };
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootIOwe != null) {
            return mRootIOwe;
        }

        View root = inflater.inflate(R.layout.fragment_listview, container, false);
        // Set up the views
        debtListViewIOwe = (ListView) root.findViewById(android.R.id.list);
        noDebtsViewIOwe = (LinearLayout) root.findViewById(R.id.no_debts_view);
        debtListViewIOwe.setEmptyView(noDebtsViewIOwe);

        // Set up the adapter
        debtListAdapterIOwe = new DebtListAdapter(getActivity(), factoryIOwe);

        // Attach the query adapter to the view
        debtListViewIOwe.setAdapter(debtListAdapterIOwe);

        debtListViewIOwe.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Debt debt = debtListAdapterIOwe.getItem(position);
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
        fab.attachToListView(debtListViewIOwe, new ScrollDirectionListener() {// REMOVE: 07/09/2015 listener
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

        mRootIOwe = root;
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
        debtListAdapterIOwe.loadObjects();// REMOVE: 07/09/2015 ?
        debtListAdapterIOwe.notifyDataSetChanged();
    }

    void clearView() {
        debtListAdapterIOwe.clear();
    }
}
