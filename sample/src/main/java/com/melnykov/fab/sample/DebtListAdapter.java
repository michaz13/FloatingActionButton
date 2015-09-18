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
class DebtListAdapter extends ParseQueryAdapter<Debt> {

    private class ViewHolder {
        TextView debtTitle;
    }

    private final Context mContext;

    DebtListAdapter(Context context, QueryFactory<Debt> queryFactory) {
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
/*
        ParseUser author = debt.getAuthor();
        if(author!=null) {
            String token = author.getSessionToken();
            boolean isAuth = author.isAuthenticated();
            boolean isDataAvai = author.isDataAvailable();
            boolean isNew = author.isNew();
            boolean isDirty = author.isDirty();
            boolean isLinked = ParseAnonymousUtils.isLinked(author);
        }
//            String info = "\nauthor: "+author.getUsername()+"\nisAuth: "+isAuth+"\nisDataAvai: "+isDataAvai+"\nisNew: "+isNew+"\nisDirty: "+isDirty+"\ntoken: "+token+"\nisLinked: "+isLinked;
*/

String extra = "\n"+debt.getUuidString()+"<-"+debt.getOtherUuid();
        debtTitle.setText(debt.getTitle()+extra);
        if (debt.isDraft()) {
            debtTitle.setTypeface(null, Typeface.ITALIC);
            debtTitle.setTextColor(Color.RED);// TODO: 02/09/2015 GRAY

        } else {
            debtTitle.setTypeface(null, Typeface.NORMAL);
            if(debt.getStatus()==Debt.STATUS_CREATED){
                debtTitle.setTextColor(Color.BLACK);
            }
            else if(debt.getStatus()==Debt.STATUS_PENDING){
                debtTitle.setTextColor(Color.GREEN);
            }
            else if(debt.getStatus()==Debt.STATUS_CONFIRMED){
                debtTitle.setTextColor(Color.BLUE);
            }
            else if(debt.getStatus()==Debt.STATUS_RETURNED){
                debtTitle.setTextColor(Color.MAGENTA);
            }
            else{
                debtTitle.setTextColor(Color.YELLOW);
            }

        }
        return view;
    }
}
