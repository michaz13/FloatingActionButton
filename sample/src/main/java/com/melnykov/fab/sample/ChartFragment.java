package com.melnykov.fab.sample;


import android.app.Fragment;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChartFragment extends android.support.v4.app.Fragment {

    private static final int DEFAULT_UPPER_INDEX = 5;
    private static final int DEFAULT_lOWER_INDEX = 0;

    private View mRoot;

    private PieChart mChart;
    private SeekBar mSeekBarXMax;
    private SeekBar mSeekBarXMin;
    private TextView tvXMax;
    private TextView tvXMin;

    MenuItem editModeMenuItem;
    Debt selectedDebt;

    private List<Debt> mData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        if (mRoot != null) {
            return mRoot;
        }

        View v = inflater.inflate(R.layout.fragment_chart, container, false);

        mChart = (PieChart) v.findViewById(R.id.pieChart1);
        mChart.setDescription("");

        mChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {
                editModeMenuItem.setVisible(false);
            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

            }
        });
        mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
                if (e == null) {
                    return;
                }
                selectedDebt = mData.get(e.getXIndex());
                editModeMenuItem.setVisible(true);
            }

            @Override
            public void onNothingSelected() {
                editModeMenuItem.setVisible(false);
            }
        });


        tvXMax = (TextView) v.findViewById(R.id.tvXMax);
        tvXMin = (TextView) v.findViewById(R.id.tvXMin);

        mSeekBarXMax = (SeekBar) v.findViewById(R.id.seekBar1);
        mSeekBarXMin = (SeekBar) v.findViewById(R.id.seekBar2);

        mSeekBarXMax.setProgress(DEFAULT_UPPER_INDEX);
        mSeekBarXMin.setProgress(DEFAULT_lOWER_INDEX);

        mSeekBarXMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvXMax.setText("" + (progress + 1));
                if (mSeekBarXMin.getProgress() > progress) { // FIXME: 22/09/2015
                    mSeekBarXMin.setProgress(progress);
                    tvXMin.setText("" + (progress + 1));
                }
                setData(mSeekBarXMin.getProgress(), progress + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBarXMin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvXMin.setText("" + (progress + 1));
                if (mSeekBarXMax.getProgress() < progress) { // FIXME: 22/09/2015
                    mSeekBarXMax.setProgress(progress);
                    tvXMax.setText("" + (progress + 1));
                }
                setData(progress, mSeekBarXMax.getProgress() + 1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Legend l = mChart.getLegend();
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART);

        mRoot = v;
        return v;
    }


    @Override
    public void onResume() {
        super.onResume();
        try { // TODO: 22/09/2015 only on data change!
            ParseQuery<Debt> mQuery = Debt.getQuery();
            mQuery.whereEqualTo(Debt.KEY_TAB_TAG, getTag());
            mQuery.whereNotEqualTo(Debt.KEY_CURRENCY_POS, Debt.NON_MONEY_DEBT_CURRENCY);
            mQuery.orderByDescending(Debt.KEY_MONEY_AMOUNT);
            mQuery.fromLocalDatastore();
            mData = mQuery.find();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int maxProgress;
        if (mData != null) {
            maxProgress = mData.size() - 1;
        } else {
            // Make sure the app does not crash. Use old value instead
            maxProgress = mSeekBarXMax.getMax() - 1;
        }
        mSeekBarXMax.setMax(maxProgress);
        mSeekBarXMin.setMax(maxProgress);
        mSeekBarXMax.setProgress(maxProgress);
        mSeekBarXMin.setProgress(0);
        tvXMax.setText("" + (mSeekBarXMax.getProgress() + 1));
        tvXMin.setText("" + (mSeekBarXMin.getProgress() + 1));
        setData(mSeekBarXMin.getProgress(), mSeekBarXMax.getProgress() + 1);
    }

//    @Override // TODO: 23/09/2015 separate menu
//    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater) {
//        getActivity().getMenuInflater().inflate(R.menu.main, menu);
//        super.onCreateOptionsMenu(menu, inflater);
//    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        editModeMenuItem = menu.findItem(R.id.action_edit_mode);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_mode:
                openEditView(selectedDebt);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setData(int fromIndex, int toIndex) {
        int totalValue = 0;

        if (fromIndex > toIndex) {
            toIndex = fromIndex + 1;
        }
        if (mData == null || mData.size() <= 0) {
            toIndex = 0;
        }
        ArrayList<Entry> yVals = new ArrayList<>();

        // note: xIndex must be unique
        for (int i = fromIndex, xIndex = 0; i < toIndex; i++, xIndex++) {
            int amount = mData.get(i).getMoneyAmount();
            yVals.add(new Entry(amount, xIndex));// TODO: 22/09/2015 make sure it's money debt
            totalValue += amount;
        }

        ArrayList<String> xVals = new ArrayList<String>();

        for (int i = fromIndex; i < toIndex; i++) {
            xVals.add(mData.get(i).getOwner());
        }

        PieDataSet dataSet = new PieDataSet(yVals, getTag() + " debts");// TODO: 22/09/2015 by tag
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // add some colors
        ArrayList<Integer> colors = new ArrayList<Integer>();

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());

        Collections.rotate(colors, -fromIndex);
        dataSet.setColors(colors);

        PieData data = new PieData(xVals, dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(10f);

        mChart.setCenterText("Total Value\n" + totalValue + "\n(all slices)");
        mChart.setData(data);

        // undo all highlights
        mChart.highlightValues(null);

        // refresh data
        mChart.invalidate();
    }

    // Helper methods: -----------------------------------------------------------------------------
    private void openEditView(Debt debt) {
        Intent i = new Intent(getActivity().getApplicationContext(), EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        startActivityForResult(i, MainActivity.EDIT_ACTIVITY_CODE);
    }
}
