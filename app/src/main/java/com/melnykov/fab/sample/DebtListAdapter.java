package com.melnykov.fab.sample;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jjobes.slidedatetimepicker.SlideDateTimeListener;
import com.github.jjobes.slidedatetimepicker.SlideDateTimePicker;
import com.parse.FindCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;

import java.util.Date;
import java.util.List;

/**
 * Adapter between the {@link com.fortysevendeg.swipelistview.SwipeListView} and the user's debts.
 */
class DebtListAdapter extends ParseQueryAdapter<Debt> {

    static final int ACTION_CALL = 0;
    static final int ACTION_SMS = 1;
    static final int ACTION_CHAT = 2;

    private class ViewHolder {
        TextView debtTitle;
        public ImageView debtImage;
        public TextView debtDescription;
        public Button action1;
        public Button action2;
        public Button action3;
    }

    public static class CustomMenuAdapter extends ArrayAdapter<CharSequence> {// REMOVE: 27/09/2015

        boolean disableOptionA = true;

        private CustomMenuAdapter(Context context, int textViewResId, CharSequence[] strings, boolean disableOptionA) {
            super(context, textViewResId, strings);
            this.disableOptionA = disableOptionA;
        }

        public static CustomMenuAdapter createFromResource(Context context, int textArrayResId, int textViewResId,
                                                           boolean disableOptionA) {

            Resources resources = context.getResources();
            CharSequence[] strings = resources.getTextArray(textArrayResId);

            return new CustomMenuAdapter(context, textViewResId, strings, disableOptionA);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            switch (position) {
                case ACTION_CHAT:
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
//                            openConversationByPhone(new Debt());
                            int x = 7;
                        }
                    });
                    break;
            }
            view.setEnabled(isEnabled(position));
            return view;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            if (position == 0) {
                if (disableOptionA) {
                    return false;
                }
            }
            return true;
        }
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
            holder.action1 = (Button) view.findViewById(R.id.action_edit);
            holder.action2 = (Button) view.findViewById(R.id.action_message);
            holder.action3 = (Button) view.findViewById(R.id.action_call);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        TextView debtTitle = holder.debtTitle;
        TextView debtDescription = holder.debtDescription;
        ImageView debtImage = holder.debtImage;

        if (debt.getCurrencyPos() != Debt.NON_MONEY_DEBT_CURRENCY) {
            debtImage.setImageResource(R.drawable.dollar);
        } else {
            debtImage.setImageResource(R.drawable.box_closed_icon);// TODO: 25/09/2015 image / location
        }

        // Action 1:
        holder.action1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditView(debt);
            }
        });

        // Action 2:
        if (debt.getPhone() != null) {
            holder.action2.setText(R.string.action2_text_with_phone);
            holder.action2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showActionsDialog(debt);
                }
            });
        } else {
            holder.action2.setText(R.string.action2_text_no_phone);
            holder.action2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: 9/26/2015
                }
            });
        }
//        holder.action3.setText(mContext.getString(R.string.action3_text, debt.getDueDate()));
        holder.action3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SlideDateTimeListener listener = new SlideDateTimeListener() {

                    @SuppressWarnings("deprecation")
                    @Override
                    public void onDateTimeSet(Date date) {
                        date.setSeconds(0);
                        debt.setDueDate(date);
                    }

                    @Override
                    public void onDateTimeCancel() {

                    }
                };
                Date initDate;
                Date currDate = debt.getDueDate();
                if (currDate != null) {
                    initDate = currDate;
                } else {
                    initDate = new Date();
                }
                new SlideDateTimePicker.Builder(((AppCompatActivity) mContext).getSupportFragmentManager())
                        .setListener(listener)
                        .setInitialDate(initDate)
                        .setIndicatorColor(Color.RED)
                        .build()
                        .show();
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
            if (debt.getStatus() == Debt.STATUS_CREATED) {
                debtTitle.setTextColor(Color.BLACK);
            } else if (debt.getStatus() == Debt.STATUS_PENDING) {
                debtTitle.setTextColor(Color.GREEN);
            } else if (debt.getStatus() == Debt.STATUS_CONFIRMED) {
                debtTitle.setTextColor(Color.BLUE);
            } else if (debt.getStatus() == Debt.STATUS_RETURNED) {
                debtTitle.setTextColor(Color.MAGENTA);
            } else {
                debtTitle.setTextColor(Color.YELLOW);
            }

        }

        debtDescription.setText(debt.getOwner());
        return view;
    }


    /**
     * Show a confirmation push notification dialog, with an option to call the owner.
     */
    private void showActionsDialog(final Debt debt) {
        int array;
        if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            array = R.array.contact_actions_array_logged_in;
        } else {
            array = R.array.contact_actions_array_logged_out;
        }
        (new AlertDialog.Builder(mContext))
                .setTitle(R.string.contact_actions_dialog_title_action)
                .setItems(array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        switch (whichButton) {
                            case ACTION_CHAT:
                                openConversationByPhone(debt);
                                break;
                            case ACTION_CALL:
                                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + debt.getPhone()));
                                mContext.startActivity(dial);
                                break;
                            case ACTION_SMS:
                                Intent sms = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", debt.getPhone(), null));
                                mContext.startActivity(sms);
                                break;

                        }
                    }
                })
                .show();
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
