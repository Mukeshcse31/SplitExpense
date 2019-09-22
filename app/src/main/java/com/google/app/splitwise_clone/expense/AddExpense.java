package com.google.app.splitwise_clone.expense;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.notification.SendNotificationLogic;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.DatePickerFragment;
import com.google.app.splitwise_clone.utils.DigitsInputFilter;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AddExpense extends AppCompatActivity implements ListView.OnItemClickListener {

    private DatabaseReference mDatabaseReference;
    private String TAG = AddExpense.class.getSimpleName();
    private Toolbar mToolbar;
    String userName;
    ListView listView;
    Spinner spinner2;
    private static String[] groupMembers;
    private AutoCompleteTextView mDescription, mAmount;
    private Button date_btn;
    private Expense mExpense;
    private String group_name, expenseId = null;
    List<String> groupMember = new ArrayList<>();
    Map<String, Float> amountSpentByMember = null;
    Map<String, Float> amountDueByMember = null;
    Map<String, Map<String, Float>> expenseMatrix = null;
    private Map<String, SingleBalance> members;
    Map<String, Expense> expenses;
    private Group group;
    float totalGroupExpense = 0.2f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_details);
        userName = FirebaseUtils.getUserName();
        date_btn = findViewById(R.id.date_btn);
        mDescription = findViewById(R.id.expense_description);
        mAmount = findViewById(R.id.expense_amount);
        //https://stackoverflow.com/questions/17423483/how-to-limit-edittext-length-to-7-integers-and-2-decimal-places/21802109
        mAmount.setFilters(new InputFilter[]{new DigitsInputFilter(10, 2, 99999999.99)});

        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getString(R.string.add_expense));
        mDatabaseReference = AppUtils.getDBReference();

        listView = findViewById(R.id.group_members);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        spinner2 = findViewById(R.id.member_spent);
        setDefaultDate();

        Bundle bundle = getIntent().getExtras();
        if (bundle.containsKey(ExpenseList.GROUP_NAME)) {

            group_name = bundle.getString(ExpenseList.GROUP_NAME);
            if (bundle.containsKey(ExpenseList.EDIT_EXPENSEID)) {

                expenseId = bundle.getString(ExpenseList.EDIT_EXPENSEID);
                mExpense = bundle.getParcelable(ExpenseList.EDIT_EXPENSE);

            }
        }


        Query query = mDatabaseReference.child("groups/" + group_name + "/members").orderByChild("active").equalTo("Yes");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<String> members = new ArrayList<>();
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        members.add(i.getKey());
                    }

                    //put the userName at the first index
                    members.remove(userName);
                    members.add(0, userName);

                    //set group members
                    groupMembers = members.toArray(new String[0]);
                    listView.setAdapter(new ArrayAdapter<>(AddExpense.this, R.layout.expense_participants_item, groupMembers));
                    for (int i = 0; i < members.size(); i++)
                        listView.setItemChecked(i, true);
                    listView.setOnItemClickListener(AddExpense.this);


                    //expense payer
                    ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(AddExpense.this,
                            R.layout.spinner_item, members);
                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner2.setAdapter(dataAdapter);
                    spinner2.setSelection(0, true);

                }
                populateExpense();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void populateExpense() {

        //set the expense details to edit
        if (mExpense != null) {
            date_btn.setText(mExpense.getDateSpent());
            mDescription.setText(mExpense.getDescription());
            mAmount.setText("" + mExpense.getTotal());

//        https://stackoverflow.com/questions/13151699/set-text-on-spinner
//set the payer
            for (int i = 0; i < spinner2.getAdapter().getCount(); i++) {
                if (spinner2.getAdapter().getItem(i).toString().contains(mExpense.getPayer())) {
                    spinner2.setSelection(i);
                    break;
                }
            }

            //set the members who share the expense
            Map<String, SingleBalance> splitExpense = mExpense.getSplitExpense();
            for (int i = 0; i < groupMembers.length; i++)
                if (splitExpense.containsKey(groupMembers[i]))
                    listView.setItemChecked(i, true);
                else
                    listView.setItemChecked(i, false);
        }
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

        getMenuInflater().inflate(R.menu.mnu_add_expense, menu);
        MenuItem deleteMenu = menu.findItem(R.id.deleteExpense);
        MenuItem cancelMenu = menu.findItem(R.id.deleteExpense);

        if (mExpense == null) {
            deleteMenu.setVisible(false);
            cancelMenu.setVisible(false);
        }
        return true;
    }

    private List<String> getExpenseParticipants() {

        SparseBooleanArray sp = listView.getCheckedItemPositions();
        List<String> participants = new ArrayList<>();

        for (int i = 0; i < sp.size(); i++) {
            if (sp.valueAt(i) == true)
                participants.add(groupMembers[i]);
        }
        return participants;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final List<String> participants = getExpenseParticipants();
        switch (item.getItemId()) {

            case R.id.saveExpense:

                String spentDate = date_btn.getText().toString();
                String description = mDescription.getText().toString();
                String category = AppUtils.getExpenseCategory(this, description);
                String amountStr = mAmount.getText().toString();
                float amount = 0.0f;

                //check if amount or description is empty
                if (TextUtils.isEmpty(description)) {
                    mDescription.setError(getString(R.string.desc_error));
                    break;
                }

                if (TextUtils.isEmpty(amountStr)) {
                    mAmount.setError(getString(R.string.amount_error));
                    break;
                }
                amount = Float.valueOf(amountStr);

                String spender = (String) spinner2.getSelectedItem();
                Log.i(TAG, "Expense Added");
                Expense expense = new Expense(spentDate, spender, description, amount);
                expense.setCategory(category);

                for (int i = 0; i < participants.size(); i++) {

                    final String participant = participants.get(i);
                    float amountForUser = amount;
                    String amountStatus = getString(R.string.you_lent);
                    if (TextUtils.equals(spender, participant)) {
                        amountForUser = amount - (amount / participants.size());

                    } else {
                        amountForUser = -(amount / participants.size());
                        amountStatus = getString(R.string.you_borrowed);
                    }
                    SingleBalance singleBalance = new SingleBalance(amountForUser, amountStatus, participant);
//                    final float amountSpentForOthers1 = amountForUser;
                    expense.addMember(participant, singleBalance);
                }

                String notificationMessage = "";
                String notificationTitle = "";
                if (expenseId != null) {//update individual expense
                    mDatabaseReference.child("groups/" + group_name + "/expenses/" + expenseId).setValue(expense, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            updateGroup(group_name);
                        }
                    });
                    notificationTitle = getString(R.string.expense_edited);
                    notificationMessage = String.format("%s edited the expense for %s to %.2f in the group %s", userName, description, amount, group_name);
                } else {//add expense

                    mDatabaseReference.child("groups/" + group_name + "/expenses").push().setValue(expense, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {
                            updateGroup(group_name);
                            Toast.makeText(AddExpense.this, " added", Toast.LENGTH_LONG).show();
                        }
                    });
                    notificationTitle = getString(R.string.expense_added);
                    notificationMessage = String.format("%s added %.2f for %s in the group %s", userName, amount, description, group_name);
                }

                //Send Notification
                SendNotificationLogic notification = new SendNotificationLogic(this);
                //loop through the people sharing expense
                for (int i = 0; i < participants.size(); i++) {
                    if (!TextUtils.equals(userName, participants.get(i))) {
                        notification.setData(participants.get(i), notificationTitle, notificationMessage);
                        notification.send();
                    }
                }

                finish();
                break;

            case R.id.deleteExpense:

                //code to delete the expense detail
                //https://www.tutorialspoint.com/android/android_alert_dialoges.htm
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setMessage(getString(R.string.warning_message));
                alertDialogBuilder.setPositiveButton(getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                mDatabaseReference.child("groups/" + group_name + "/expenses/" + expenseId).removeValue();
                                updateGroup(group_name);

                                //Send Notification
                                SendNotificationLogic notification = new SendNotificationLogic(AddExpense.this);
                                //loop through the people sharing expense

                                for (int i = 0; i < participants.size(); i++) {
                                    if (!TextUtils.equals(userName, participants.get(i))) {
                                        notification.setData(participants.get(i), getString(R.string.expense_deleted),
                                                String.format("%s deleted the expense for %s %f", userName, mExpense.getDescription(), mExpense.getTotal()));
                                        notification.send();
                                    }
                                }

                                finish();

                            }
                        });

                alertDialogBuilder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                break;

            case R.id.cancelUpdate:
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
        date_btn.setText(String.format("%d-%02d-%02d", year, month + 1, day));
    }

    //update a single specified group
    public void updateGroup(final String groupName) {

        amountSpentByMember = new HashMap<>();
        amountDueByMember = new HashMap<>();
        expenseMatrix = new HashMap<>();

        //update the participant's total amount
        //Get all the group members
        Query query = mDatabaseReference.child("groups/" + groupName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    Map<String, Float> splitDues = new HashMap<>();
                    group = dataSnapshot.getValue(Group.class);
                    expenses = group.getExpenses();
                    members = group.getMembers();

                    //Get all the group members
                    Iterator itMbr = members.entrySet().iterator();
                    while (itMbr.hasNext()) {
                        Map.Entry pairMbr = (Map.Entry) itMbr.next();
                        String grouMbr = (String) pairMbr.getKey();
                        splitDues.put(grouMbr, 0.0f);//this is not added when a group is added newly
                    }

                    //build the expense matrix for all the members
                    itMbr = members.entrySet().iterator();
                    while (itMbr.hasNext()) {
                        Map.Entry pairMbr = (Map.Entry) itMbr.next();
                        String grouMbr = (String) pairMbr.getKey();
                        groupMember.add(grouMbr);
                        SingleBalance sb = new SingleBalance(0.0f, "amount owed", grouMbr);
                        sb.setSplitDues(new HashMap<>(splitDues));
                        members.put(grouMbr, sb);
                    }

//loop through all the expense
                    Iterator it1 = expenses.entrySet().iterator();
                    while (it1.hasNext()) {
                        Map.Entry pairExp = (Map.Entry) it1.next();
                        Expense expense = (Expense) pairExp.getValue();
                        String spender = expense.getPayer();
                        Map<String, SingleBalance> splitExpense = expense.getSplitExpense();

                        //amount due by individuals
                        Iterator it = splitExpense.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            String name = (String) pair.getKey();
                            SingleBalance balance = (SingleBalance) pair.getValue();
                            float amount = balance.getAmount();

                            if (TextUtils.equals(spender, name)) {
//don't do anything, get the amount due with other members below
                                //this is calculated by amountSpentByMember
                            }
//                                    matrix
//                                    m1	-	m2-100	m3+15	m4-200
//                                    m2	-	m1+100	m3-200	m4+50
//                                    m3	-	m1+100	m2+40	m4+98
//                                    m4	-	m1+100	m2-90	m3-20
//
                            else {//Expense Matrix
                                Map<String, Float> borrowerSplit = members.get(name).getSplitDues();
                                Map<String, Float> lenderSplit = members.get(spender).getSplitDues();
                                lenderSplit.put(name, lenderSplit.get(name) - amount); //as the amount is in debt
                                borrowerSplit.put(spender, borrowerSplit.get(spender) + amount);

                                members.get(spender).setSplitDues(new HashMap<>(lenderSplit));
                                members.get(name).setSplitDues(new HashMap<>(borrowerSplit));

                            }
                            it.remove(); // avoids a ConcurrentModificationException
                        }

                        //total expense in the group
                        totalGroupExpense += expense.getTotal();

                        //add the amount spent by the members
                        float amountSpentByMember = members.get(spender).getAmount() + expense.getTotal();
                        members.get(spender).setAmount(amountSpentByMember);
                        members.get(spender).setStatus(getString(R.string.you_borrowed));
                        if (amountSpentByMember > 0)
                            members.get(spender).setStatus(getString(R.string.you_lent));
                    }

                    //write group total and members into DB
                    mDatabaseReference.child("groups/" + groupName + "/members/").setValue(members);
                    mDatabaseReference.child("groups/" + groupName + "/totalAmount/").setValue(totalGroupExpense);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }


}
