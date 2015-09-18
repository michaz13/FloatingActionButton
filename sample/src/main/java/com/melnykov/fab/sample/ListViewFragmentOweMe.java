package com.melnykov.fab.sample;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
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
public class ListViewFragmentOweMe extends android.support.v4.app.Fragment {// REMOVE: 17/09/2015

    // Adapter for the Debts Parse Query
    ParseQueryAdapter<Debt> debtListAdapterOweMe;

    // For showing empty and non-empty debt views
    private ListView debtListViewOweMe;
    private LinearLayout noDebtsViewOweMe;

    private View mRootOweMe;

    ParseQueryAdapter.QueryFactory<Debt> factoryOweMe;

    public ListViewFragmentOweMe() {
        // Set up the Parse query to use in the adapter
        factoryOweMe = new ParseQueryAdapter.QueryFactory<Debt>() {
            public ParseQuery<Debt> create() {
                ParseQuery<Debt> query = Debt.getQuery();
                query.whereEqualTo(Debt.KEY_TAB_TAG, Debt.OWE_ME_TAG);
                query.orderByAscending("createdAt");
                query.fromLocalDatastore();
                return query;
            }
        };
    }

    @SuppressLint("InflateParams")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mRootOweMe != null) {
            return mRootOweMe;
        }

        View root = inflater.inflate(R.layout.fragment_listview, container, false);
        // Set up the views
        debtListViewOweMe = (ListView) root.findViewById(android.R.id.list);
        noDebtsViewOweMe = (LinearLayout) root.findViewById(R.id.no_debts_view);
        debtListViewOweMe.setEmptyView(noDebtsViewOweMe);

        // Set up the adapter
        debtListAdapterOweMe = new DebtListAdapter(getActivity(), factoryOweMe);

        // Attach the query adapter to the view
        debtListViewOweMe.setAdapter(debtListAdapterOweMe);

        debtListViewOweMe.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Debt debt = debtListAdapterOweMe.getItem(position);
                openEditView(debt);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
                i.putExtra(Debt.KEY_TAB_TAG, getTag());
                startActivityForResult(i, MainActivity.EDIT_ACTIVITY_CODE);
            }
        });
        fab.attachToListView(debtListViewOweMe, new ScrollDirectionListener() {// REMOVE: 07/09/2015 listener
            @Override
            public void onScrollDown() {
                Log.d("ListViewFragmentOweMe", "onScrollDown()");
            }

            @Override
            public void onScrollUp() {
                Log.d("ListViewFragmentOweMe", "onScrollUp()");
            }
        }, new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                Log.d("ListViewFragmentOweMe", "onScrollStateChanged()");
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                Log.d("ListViewFragmentOweMe", "onScroll()");
            }
        });

        mRootOweMe = root;
        return root;
    }


    // Helper methods: -----------------------------------------------------------------------------
    private void openEditView(Debt debt) {
        Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        startActivityForResult(i, MainActivity.EDIT_ACTIVITY_CODE);
    }

    private void updateView() {
        debtListAdapterOweMe.loadObjects();// REMOVE: 07/09/2015 ?
        debtListAdapterOweMe.notifyDataSetChanged();
    }

    void clearView() {
        debtListAdapterOweMe.clear();
    }
}
