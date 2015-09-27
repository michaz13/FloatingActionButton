package com.melnykov.fab.sample;


import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.fortysevendeg.swipelistview.BaseSwipeListViewListener;
import com.fortysevendeg.swipelistview.SwipeListView;
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
//    private ListView debtListView; remo
    private LinearLayout noDebtsView;

    private View mRoot;

    ParseQueryAdapter.QueryFactory<Debt> factory;
    private SwipeListView swipeListView;

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
//        debtListView = (ListView) root.findViewById(android.R.id.list);// REMOVE: 24/09/2015
        swipeListView = (SwipeListView) root.findViewById(R.id.debts_list);
        noDebtsView = (LinearLayout) root.findViewById(R.id.no_debts_view);
        swipeListView.setEmptyView(noDebtsView);

        // Set up the adapter
        debtListAdapter = new DebtListAdapter(getActivity(), factory);


        swipeListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            swipeListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                      long id, boolean checked) {
                    mode.setTitle("Selected (" + swipeListView.getCountSelected() + ")");
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_delete:
                            swipeListView.dismissSelected();
                            mode.finish();
                            return true;
                        default:
                            return false;
                    }
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.menu_choice_items, menu);
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    swipeListView.unselectedChoiceStates();
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }
            });
        }

        swipeListView.setSwipeListViewListener(new BaseSwipeListViewListener() {
            @Override
            public void onOpened(int position, boolean toRight) {
            }

            @Override
            public void onClosed(int position, boolean fromRight) {
            }

            @Override
            public void onListChanged() {
            }

            @Override
            public void onMove(int position, float x) {
            }

            @Override
            public void onStartOpen(int position, int action, boolean right) {
                Log.d("swipe", String.format("onStartOpen %d - action %d", position, action));
            }

            @Override
            public void onStartClose(int position, boolean right) {
                Log.d("swipe", String.format("onStartClose %d", position));
            }

            @Override
            public void onClickFrontView(int position) {
                Debt debt = debtListAdapter.getItem(position);
                openEditView(debt);
            }

            @Override
            public void onClickBackView(int position) {
                Log.d("swipe", String.format("onClickBackView %d", position));
            }

            @Override
            public void onDismiss(int[] reverseSortedPositions) {
/*                for (int position : reverseSortedPositions) {
                    data.remove(position);
                }
                adapter.notifyDataSetChanged();*/
                int x = 5;
            }

        });
        // Attach the mQuery adapter to the view
        swipeListView.setAdapter(debtListAdapter);

        FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
                i.putExtra(Debt.KEY_TAB_TAG, getTag());
                startActivityForResult(i , MainActivity.EDIT_ACTIVITY_CODE);
            }
        });
        fab.attachToListView(swipeListView, new ScrollDirectionListener() {// REMOVE: 07/09/2015 listener
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
        reloadSettings();
    }

    private void reloadSettings() {
        SettingsManager settings = SettingsManager.getInstance();
        swipeListView.setSwipeMode(settings.getSwipeMode());
        swipeListView.setSwipeActionLeft(settings.getSwipeActionLeft());
        swipeListView.setSwipeActionRight(settings.getSwipeActionRight());
        swipeListView.setOffsetLeft(convertDpToPixel(settings.getSwipeOffsetLeft()));
        swipeListView.setOffsetRight(convertDpToPixel(settings.getSwipeOffsetRight()));
        swipeListView.setAnimationTime(settings.getSwipeAnimationTime());
        swipeListView.setSwipeOpenOnLongPress(settings.isSwipeOpenOnLongPress());
    }

    public int convertDpToPixel(float dp) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return (int) px;
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
