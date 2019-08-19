package com.google.app.splitwise_clone.expense;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.TextUtilsCompat;
import androidx.fragment.app.DialogFragment;

import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.groups.AddGroup;
import com.google.app.splitwise_clone.groups.Groups;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.DatePickerFragment;
import com.google.app.splitwise_clone.utils.GroupsAdapter;
import com.google.app.splitwise_clone.utils.MultiSpinner;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddExpense extends AppCompatActivity implements ListView.OnItemClickListener {

    private DatabaseReference mDatabaseReference;
    private String TAG = AddExpense.class.getSimpleName();
    ListView listView;
    Spinner spinner2;
    private static String[] groupMembers;
    private AutoCompleteTextView mDescription, mAmount;
    private Button date_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_details);

        date_btn = findViewById(R.id.date_btn);
        mDescription = findViewById(R.id.expense_description);
        mAmount = findViewById(R.id.expense_amount);

        getSupportActionBar().setTitle(getString(R.string.add_expense));
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        listView = findViewById(R.id.group_members);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        spinner2 = (Spinner) findViewById(R.id.member_spent);
        setDefaultDate();

        Query query = mDatabaseReference.child("groups/group1/members");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<String> members = new ArrayList<>();
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        members.add(i.getKey());
                    }
                    groupMembers = members.toArray(new String[0]);
                    listView.setAdapter(new ArrayAdapter<String>(AddExpense.this, android.R.layout.simple_list_item_multiple_choice, groupMembers));
                    for (int i = 0; i < members.size(); i++)
                        listView.setItemChecked(i, true);
                    listView.setOnItemClickListener(AddExpense.this);


                    //person who spends for the expense
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(AddExpense.this,
                            android.R.layout.simple_spinner_item, members);
                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner2.setAdapter(dataAdapter);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View arg1, int arg2, long arg3) {


        SparseBooleanArray sp = listView.getCheckedItemPositions();

        String str = "";
        for (int i = 0; i < sp.size(); i++) {
            str += groupMembers[sp.keyAt(i)] + ",";
        }
        Toast.makeText(this, "" + str, Toast.LENGTH_SHORT).show();

    }

    public void onButtonClicked(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "Date Picker");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_expense, menu);
        return true;
    }

    private List<String> getExpenseParticipants() {

        SparseBooleanArray sp = listView.getCheckedItemPositions();
        List<String> participants = new ArrayList<>();

        for (int i = 0; i < sp.size(); i++) {
            if (sp.valueAt(i) == true)
                participants.add(groupMembers[i]);
//            str += groupMembers[sp.keyAt(i)] + ",";
        }
        return participants;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.saveExpense:
                String spentDate = date_btn.getText().toString();
                String description = mDescription.getText().toString();
                float amount = Float.valueOf(mAmount.getText().toString());
                List<String> participants = getExpenseParticipants();
                String spender = (String) spinner2.getSelectedItem();
                Log.i(TAG, "Expense Added");
                Expense expense = new Expense(spentDate, spender, description, amount);

                for (int i = 0; i < participants.size(); i++) {

                    final String participant = participants.get(i);
                    float amountForUser = amount;
                    float amountSpentForOthers = 0.0f;
                    String amountStatus = "you spent";
                    if (TextUtils.equals(spender, participant)) {
                        amountSpentForOthers = amount / participants.size();

                    } else {
                        amountForUser = -(amount / participants.size());
                        amountStatus = "you owe";
                    }
                    final SingleBalance singleBalance = new SingleBalance(amountForUser, amountStatus);
                    final float amountSpentForOthers1 = amountSpentForOthers;
                    expense.addMember(participant, singleBalance);

                    //update the participant's total amount
                    Query query = mDatabaseReference.child("groups/group1/members/" + participant);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                float existingBalance = 0.0f;
                                for (DataSnapshot i : dataSnapshot.getChildren()) {

                                    //if key is amount
                                    if (TextUtils.equals(i.getKey(), "amount")) {
                                        existingBalance = Float.parseFloat(i.getValue().toString());
                                        float newBalance = existingBalance + singleBalance.getAmount() - amountSpentForOthers1;
                                        String newStatus = "you owe";
                                        if (newBalance > 0)
                                            newStatus = "owes you";
                                        mDatabaseReference.child("groups/group1/members/" + participant).setValue(new SingleBalance(newBalance, newStatus));
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
                //update individual expense
                mDatabaseReference.child("groups/group1/expenses").push().setValue(expense, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {
                        Toast.makeText(AddExpense.this, " added", Toast.LENGTH_LONG).show();
                    }
                });
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setDefaultDate() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        date_btn.setText(String.format("%d-%02d-%02d", year, month, day));
    }
}
