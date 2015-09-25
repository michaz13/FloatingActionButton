package com.melnykov.fab.sample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;

import java.util.List;

/**
 * Adapter between the {@link com.fortysevendeg.swipelistview.SwipeListView} and the user's debts.
 */
class DebtListAdapter extends ParseQueryAdapter<Debt> {

    private class ViewHolder {
        TextView debtTitle;
        public ImageView debtImage;
        public TextView debtDescription;
        public Button actionEdit;
        public Button actionMessage;
        public Button actionCall;
    }

    private final Context mContext;

    DebtListAdapter(Context context, QueryFactory<Debt> queryFactory) {
        super(context, queryFactory);
        mContext = context;
    }

    @Override
    public View getItemView(final Debt debt, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
            holder = new ViewHolder();
            holder.debtImage = (ImageView) view.findViewById(R.id.example_row_iv_image);
            holder.debtTitle = (TextView) view
                    .findViewById(R.id.debt_title);
            holder.debtDescription = (TextView) view.findViewById(R.id.example_row_tv_description);
            holder.actionEdit = (Button) view.findViewById(R.id.action_edit);
            holder.actionMessage = (Button) view.findViewById(R.id.action_message);
            holder.actionCall = (Button) view.findViewById(R.id.action_call);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        TextView debtTitle = holder.debtTitle;
        TextView debtDescription = holder.debtDescription;
        ImageView debtImage = holder.debtImage;

        holder.actionEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditView(debt);
            }
        });

        if(debt.getCurrencyPos()!=Debt.NON_MONEY_DEBT_CURRENCY){
            debtImage.setImageResource(R.drawable.dollar);
        }else{
            debtImage.setImageResource(R.drawable.box_closed_icon);// TODO: 25/09/2015 image / location
        }

        holder.actionMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openConversationByPhone(debt);
            }
        });

        holder.actionCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + debt.getPhone()));
                mContext.startActivity(dial);
            }
        });
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

//String extra = "\n"+debt.getUuidString()+"<-"+debt.getOtherUuid(); // REMOVE: 24/09/2015
        debtTitle.setText(debt.getTitle());
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

        debtDescription.setText(debt.getOwner());
        return view;
    }

    public void openConversationByPhone(Debt debt) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo("phone", debt.getAuthorPhone());
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> user, com.parse.ParseException e) {
                if (e == null) {
                    Intent intent = new Intent(mContext, MessagingActivity.class);
                    intent.putExtra("RECIPIENT_ID", user.get(0).getObjectId());
                    mContext.startActivity(intent);
                } else {
                    Toast.makeText(mContext,
                            "Error finding that user",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Helper methods: -----------------------------------------------------------------------------
    private void openEditView(Debt debt) {
        Intent i = new Intent(mContext, EditDebtActivity.class);
        i.putExtra(Debt.KEY_UUID, debt.getUuidString());
        i.putExtra(Debt.KEY_TAB_TAG, debt.getTabTag());
        mContext.startActivity(i);
    }

}
