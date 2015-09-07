package com.melnykov.fab.sample;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.ParseAnonymousUtils;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;

/**
 * Created by Michael on 07/09/2015.
 */
public class DebtListAdapter extends ParseQueryAdapter<Debt> {

    private class ViewHolder {
        TextView debtTitle;
    }

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
