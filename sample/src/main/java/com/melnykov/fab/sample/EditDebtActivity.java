package com.melnykov.fab.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;


public class EditDebtActivity extends Activity {

    private Button saveButton;
    private Button deleteButton;
    private EditText debtText;
    private Debt debt;
    private String debtId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_debt);

        // Fetch the debtId from the Extra data
        if (getIntent().hasExtra("ID")) {
            debtId = getIntent().getExtras().getString("ID");
        }

        debtText = (EditText) findViewById(R.id.debt_text);
        saveButton = (Button) findViewById(R.id.saveButton);
        deleteButton = (Button) findViewById(R.id.deleteButton);

        if (debtId == null) {
            debt = new Debt();
            debt.setUuidString();
        } else {
            ParseQuery<Debt> query = Debt.getQuery();
            query.fromLocalDatastore();
            query.whereEqualTo("uuid", debtId);
            query.getFirstInBackground(new GetCallback<Debt>() {

                @Override
                public void done(Debt object, ParseException e) {
                    if (!isFinishing()) {
                        debt = object;
                        debtText.setText(debt.getTitle());
                        deleteButton.setVisibility(View.VISIBLE);
                    }
                }

            });

        }

        saveButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                debt.setTitle(debtText.getText().toString());
                debt.setDraft(true);
                debt.setAuthor(ParseUser.getCurrentUser());
                debt.pinInBackground(DebtListApplication.DEBT_GROUP_NAME,
                        new SaveCallback() {

                            @Override
                            public void done(ParseException e) {
                                if (isFinishing()) {
                                    return;
                                }
                                if (e == null) {
                                    setResult(Activity.RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            "Error saving: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }

                        });
            }

        });

        deleteButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // The debt will be deleted eventually but will
                // immediately be excluded from query results.
                debt.deleteEventually();
                setResult(Activity.RESULT_OK);
                finish();
            }

        });

    }

}
