package com.google.app.splitwise_clone.expense;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
    public static String EXPENSE_EDITED = "EXPENSE_EDITED";
public static String EXPENSE_ADDED = "EXPENSE_ADDED";
public static String ACTION_CANCEL = "ACTION_CANCEL";
public static String EXPENSE_DELETED = "EXPENSE_DELETED";
    String db_users, db_balances, db_groups, db_archivedExpenses, db_expenses, db_members, db_nonMembers,
            db_totalAmount, db_owner, db_photoUrl, db_amount, db_status, db_friends, db_email, db_name, db_imageUrl;
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
    List<String> notificationRecipients;
    float totalGroupExpense = 0.0f;

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
        initDBValues();
        Bundle bundle = getIntent().getExtras();
        if (bundle.containsKey(ExpenseList.GROUP_NAME)) {

            group_name = bundle.getString(ExpenseList.GROUP_NAME);
            if (bundle.containsKey(ExpenseList.EDIT_EXPENSEID)) {
                getSupportActionBar().setTitle(getString(R.string.edit_expense_title));
                expenseId = bundle.getString(ExpenseList.EDIT_EXPENSEID);
                mExpense = bundle.getParcelable(ExpenseList.EDIT_EXPENSE);

            }
        }

        Query query = mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_members);
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
        notificationRecipients = new ArrayList<>(participants);
        if(notificationRecipients.contains(userName))
            notificationRecipients.remove(userName);

        final String description = mDescription.getText().toString();
        switch (item.getItemId()) {

            case R.id.saveExpense:

                String spentDate = date_btn.getText().toString();

                String category = AppUtils.getExpenseCategory(this, description);
                final String amountStr = mAmount.getText().toString();
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
                final float amountNot = amount;
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

                if (expenseId != null) {//update individual expense
                    mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_expenses + "/" + expenseId).setValue(expense, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            updateGroup(group_name, participants);
                            String action = getString(R.string.expense_edited);
                            String notificationMessage = String.format("%s %s for %s to %.2f in the group %s", userName, action, description, amountNot, group_name);
                            sendNotification(action, notificationMessage, notificationRecipients);
                            gotoExpenseList(EXPENSE_EDITED, String.format("%s %s", action, description));
                        }
                    });
                } else {//add expense

                    mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_expenses).push().setValue(expense, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {
                            updateGroup(group_name, participants);
                            String action = getString(R.string.expense_added);
                            String notificationMessage = String.format("%s added %.2f for %s in the group %s", userName, amountNot, description, group_name);
                            sendNotification(action, notificationMessage, notificationRecipients);
                            gotoExpenseList(EXPENSE_ADDED, String.format("%s %s", action, description));
                        }
                    });
                }
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
                                mDatabaseReference.child(db_groups + "/" + group_name + "/"+db_expenses + "/" + expenseId).removeValue();
                                updateGroup(group_name, participants);// TODO send all the group members as list
                                //get the participants from the previous activity
                                // so that only those users will be updated

                                //Send Notification
                                String action = getString(R.string.expense_deleted);
                                String notificationMessage = String.format("%s %s for %s %f", userName, action, mExpense.getDescription(), mExpense.getTotal());
                                sendNotification(action, notificationMessage, notificationRecipients);
                                gotoExpenseList(EXPENSE_DELETED, String.format("%s %s", getString(R.string.expense_deleted), description));
                            }
                        });

                alertDialogBuilder.setNegativeButton(getString(R.string.no), null);

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                break;

            case R.id.cancelUpdate:
                gotoExpenseList(ACTION_CANCEL, "");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void gotoExpenseList(String name, String value){

        final Intent intent = new Intent(AddExpense.this, ExpenseList.class);
        intent.putExtra(ExpenseList.GROUP_NAME, group_name);
        intent.putExtra(name, value);
        startActivity(intent);
        finish();
    }

    private void sendNotification(String title, String msg, List<String> participants) {

        //Send Notification
        SendNotificationLogic notification = new SendNotificationLogic(this);
        //loop through the people sharing expense
        for (int i = 0; i < participants.size(); i++) {
            if (!TextUtils.equals(userName, participants.get(i))) {
                notification.setData(participants.get(i), title, msg);
                notification.send();
            }
        }
    }


    private void setDefaultDate() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        date_btn.setText(String.format("%d-%02d-%02d", year, month + 1, day));
    }

    //update a single specified group
    public void updateGroup(final String groupName, final List<String> participants ) {

        amountSpentByMember = new HashMap<>();
        amountDueByMember = new HashMap<>();
        expenseMatrix = new HashMap<>();

        //update the participant's total amount
        //Get all the group members
        Query query = mDatabaseReference.child(db_groups + "/" + groupName);

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

                        SingleBalance oldsb = (SingleBalance) pairMbr.getValue();
                        SingleBalance sb = null;
                        try {
                            sb = (SingleBalance) oldsb.clone();
                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                        }
sb.setAmount(0.0f);

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
                    mDatabaseReference.child(db_groups + "/" + groupName + "/" + db_members).setValue(members, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            mDatabaseReference.child(db_groups + "/" + groupName + "/" + db_totalAmount).setValue(totalGroupExpense, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                    for(String participant : participants)
                                    FirebaseUtils.updateUsersAmount(getApplicationContext(), participant);
                                }
                            });
//                            finish();
                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }


@Override
    public void onBackPressed(){
        Log.i(TAG, "back pressed in Add Expense");

}

    @Override
    public void onResume() {
        super.onResume();
        mDatabaseReference = AppUtils.getDBReference();
    }

    @Override
    public void onPause() {
        super.onPause();
        AppUtils.closeDBReference(mDatabaseReference);
        Log.i(TAG, "listener cleared");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "config changed");

    }

    private void initDBValues(){

        db_users = getString(R.string.db_users);
        db_balances = getString(R.string.db_balances);
        db_groups = getString(R.string.db_groups);
        db_archivedExpenses = getString(R.string.db_archivedExpenses);
        db_expenses = getString(R.string.db_expenses);
        db_members = getString(R.string.db_members);
        db_nonMembers = getString(R.string.db_nonMembers);
        db_owner = getString(R.string.db_owner);
        db_photoUrl = getString(R.string.db_photoUrl);
        db_amount = getString(R.string.db_amount);
        db_status = getString(R.string.db_status);
        db_friends = getString(R.string.db_friends);
        db_email = getString(R.string.db_email);
        db_name = getString(R.string.db_name);
        db_imageUrl = getString(R.string.db_imageUrl);
        db_totalAmount = getString(R.string.db_totalAmount);

    }
}
