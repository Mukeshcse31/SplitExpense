package com.google.app.splitwise_clone.expense;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.app.splitwise_clone.MainActivity;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.ExpenseAdapter;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExpenseList extends AppCompatActivity implements ExpenseAdapter.OnClickListener {

    private DatabaseReference mDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mPhotosStorageReference;
    private String TAG = ExpenseList.class.getSimpleName();
    LinkedHashMap<String, Expense> expenseSnapshotMap, categorizedExpenseMap, archivedExpenseSnapshotMap;
    private ExpenseAdapter mExpenseAdapter;
    private RecyclerView expenses_rv;
    private FloatingActionButton mFloatingActionButton;
    private AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;
    private CollapsingToolbarLayout mCollapsingToolbarLayout;
    private String group_name;
    private ImageView groupImage, settleup_image;
    private String userName = "";
    private TextView groupName_tv, user_balance, user_summary, noExpenses_tv, settleup_tv;
    public static String GROUP_NAME = "group_name";
    public static String EDIT_EXPENSE = "edit_expense";
    public static String EDIT_EXPENSEID = "edit_expenseID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);
        //this line shows back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mAppBarLayout = findViewById(R.id.app_bar_layout_exp);
        mCollapsingToolbarLayout = findViewById(R.id.collap_toolbar_exp);

        mDatabaseReference = AppUtils.getDBReference();
        groupName_tv = findViewById(R.id.groupName_tv);
        settleup_tv = findViewById(R.id.settleup_tv);
        noExpenses_tv = findViewById(R.id.noExpenses_tv);
        settleup_image = findViewById(R.id.settleup_image);

        user_balance = findViewById(R.id.user_balance);
        user_summary = findViewById(R.id.user_summary);
        expenses_rv = findViewById(R.id.expenses_rv);
        mFloatingActionButton = findViewById(R.id.add_expense_fab);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        expenses_rv.setLayoutManager(layoutManager);
        groupImage = findViewById(R.id.groupImage);
        mFirebaseStorage = AppUtils.getDBStorage();
        userName = FirebaseUtils.getUserName();

        Intent intent = getIntent();
        if (intent.hasExtra("group_name")) {
            group_name = intent.getStringExtra("group_name");
//            getSupportActionBar().setTitle(group_name);
            groupName_tv.setText(group_name);
        }

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ExpenseList.this, AddExpense.class);
                intent.putExtra(GROUP_NAME, group_name);
                startActivity(intent);
            }
        });
        loadGroupImage(group_name);
    }

    //populate all the archived expenses
    public void populateSettledUpExpenses(View view) {
        settleup_tv.setVisibility(View.GONE);
        settleup_image.setVisibility(View.GONE);
        expenses_rv.setVisibility(View.VISIBLE);
        archivedExpenseSnapshotMap = AppUtils.reverseExpense(archivedExpenseSnapshotMap);
        mExpenseAdapter = new ExpenseAdapter(archivedExpenseSnapshotMap, ExpenseList.this, false);
        expenses_rv.setAdapter(mExpenseAdapter);
    }

    public void settleUpExpenses() {

//        add all the expenses to archivedExpenses for later use
        Iterator it = expenseSnapshotMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            Expense expense = (Expense) pair.getValue();
            mDatabaseReference.child("groups/" + group_name + "/archivedExpenses/").push().setValue(expense);
        }
        Task deleteTask = mDatabaseReference.child("groups/" + group_name + "/expenses/").setValue(null);
        deleteTask.addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                resetMembersBalances();
                populateExpenseList();
            }
        });

    }


    @Override
    public void gotoExpenseDetails(String expenseId) {

        Expense expense = expenseSnapshotMap.get(expenseId);
        Intent intent = new Intent(ExpenseList.this, AddExpense.class);
        intent.putExtra(GROUP_NAME, group_name);
        intent.putExtra(EDIT_EXPENSEID, expenseId);
        intent.putExtra(EDIT_EXPENSE, expense);
        startActivity(intent);
        Log.i(TAG, String.format("clicked the expense %s", expenseId));

    }

    private void resetMembersBalances() {

        //Get all the group members
        Query query = mDatabaseReference.child("groups/" + group_name + "/members");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    Map<String, SingleBalance> groupMembers = (Map<String, SingleBalance>) dataSnapshot.getValue();
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        String groupMemberName = i.getKey();
                        SingleBalance sb = new SingleBalance(groupMemberName);
                        groupMembers.put(groupMemberName, sb);
                    }
                    mDatabaseReference.child("groups/" + group_name + "/members/").setValue(groupMembers);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void populateExpenseList() {
        Query query = mDatabaseReference.child("groups/" + group_name + "/expenses").orderByChild("dateSpent");
//.orderByChild("active").equalTo("Yes") is not required as settle up is implemented
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                expenseSnapshotMap = new LinkedHashMap<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        expenseSnapshotMap.put(i.getKey(), i.getValue(Expense.class));
                    }
                }
                //TODO display either no expense or settled up
                if (expenseSnapshotMap.size() == 0) {
                    settleup_tv.setVisibility(View.VISIBLE);
                    settleup_image.setVisibility(View.VISIBLE);
                    expenses_rv.setVisibility(View.GONE);
                    getArchivedExpense();
                } else {
                    settleup_tv.setVisibility(View.GONE);
                    settleup_image.setVisibility(View.INVISIBLE);
                    noExpenses_tv.setVisibility(View.GONE);
                    expenses_rv.setVisibility(View.VISIBLE);
                    expenseSnapshotMap = AppUtils.reverseExpense(expenseSnapshotMap);
                    mExpenseAdapter = new ExpenseAdapter(expenseSnapshotMap, ExpenseList.this, true);
                    expenses_rv.setAdapter(mExpenseAdapter);
                    getExpenseByCategory();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getExpenseByCategory() {
        Query query = mDatabaseReference.child("groups/" + group_name + "/expenses").orderByChild("category");
//.orderByChild("active").equalTo("Yes") is not required as settle up is implemented
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                categorizedExpenseMap = new LinkedHashMap<>();//to maintain the order of elements
                if (dataSnapshot.exists()) {
                    String prev_Category = "";
                    for (DataSnapshot i : dataSnapshot.getChildren()) {

                        Expense expense = i.getValue(Expense.class);
                        String category = expense.getCategory();

                        if(TextUtils.isEmpty(prev_Category)) prev_Category = category;// to handle the reverse order

                        categorizedExpenseMap.put(i.getKey(), i.getValue(Expense.class));
                        if (!TextUtils.equals(prev_Category, category)) {
                            //add a dummy object for category
                            categorizedExpenseMap.put(category, null);
                            prev_Category = category;
                        }

                    }
                    Log.i(TAG, categorizedExpenseMap.size() + "");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getArchivedExpense() {
        Query query = mDatabaseReference.child("groups/" + group_name + "/archivedExpenses");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                archivedExpenseSnapshotMap = new LinkedHashMap<>();
                if (dataSnapshot.exists()) {
                    String prev_Category = "";
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        Expense expense = i.getValue(Expense.class);
                        String category = expense.getCategory();

                        if (!TextUtils.equals(prev_Category, category)) {
                            //add a dummy object for category
                            archivedExpenseSnapshotMap.put(category, null);
                            prev_Category = category;
                        }
                        archivedExpenseSnapshotMap.put(i.getKey(), i.getValue(Expense.class));
                    }
                }

                if (archivedExpenseSnapshotMap.size() == 0) {
                    noExpenses_tv.setVisibility(View.VISIBLE);
                    settleup_tv.setVisibility(View.GONE);
                } else {

                    settleup_tv.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        String group_nameKey = getResources().getString(R.string.group_name);
        if (savedInstanceState.containsKey(group_nameKey)) {
            group_name = savedInstanceState.getString(group_nameKey);
            Log.v("savedInstanceState", "Inside of onRestoreInstanceState " + group_name);
        }
        super.onRestoreInstanceState(savedInstanceState);

    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {

        bundle.putString(getResources().getString(R.string.group_name), group_name);
        super.onSaveInstanceState(bundle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.expense_list_menu, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        populateAppBar();
        populateExpenseList();
    }

    private void populateAppBar() {

    //get the user's splitDues
        Query query = mDatabaseReference.child("groups/" + group_name + "/members/" + userName + "/splitDues");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String summary = "";
                    Map<String, Float> dues = (Map<String, Float>) dataSnapshot.getValue();
                    Iterator it = dues.entrySet().iterator();
                    while (it.hasNext()) {

                        Map.Entry pair = (Map.Entry) it.next();
                        String friendName = (String) pair.getKey();
                        String status = "spent";
                        float amount = Float.parseFloat(String.valueOf(pair.getValue()));
                        if (amount > 0) status = "owes you";
                        summary += String.format("%s %s $%.2f \n", friendName, status, Math.abs(amount));
                    }
                    user_summary.setText(summary);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        //get the user's splitDues
        Query query1 = mDatabaseReference.child("groups/" + group_name + "/members/" + userName + "/amount");
        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    float balanceAmount = Float.parseFloat(String.valueOf(dataSnapshot.getValue()));
                    user_balance.setText(String.format("Amount %s spent $%.2f ", balanceAmount > 0 ? "you" : "others", balanceAmount));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void loadGroupImage(String group_name) {

        Query query = mDatabaseReference.child("groups/" + group_name + "/photoUrl");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    String imagePath = (String) dataSnapshot.getValue();
                    if (TextUtils.isEmpty(imagePath)) return;

//                    https://firebase.google.com/docs/storage/android/download-files
                    mPhotosStorageReference = mFirebaseStorage.getReference();
                    StorageReference islandRef = mPhotosStorageReference.child(imagePath);

                    final long ONE_MEGABYTE = 1024 * 1024;
                    islandRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            // Data for "images/island.jpg" is returns, use this as needed
                            Glide.with(ExpenseList.this)
                                    .load(bytes)
                                    .asBitmap()
                                    .into(groupImage);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle any errors
                            Log.i(TAG, exception.toString());
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
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;
        switch (item.getItemId()) {

            case R.id.orderbyCategory:
                Log.i(TAG, "order by category");
                if (categorizedExpenseMap.size() > 0) {
                    categorizedExpenseMap = AppUtils.reverseExpense(categorizedExpenseMap);
                    mExpenseAdapter = new ExpenseAdapter(categorizedExpenseMap, ExpenseList.this, true);
                    expenses_rv.setAdapter(mExpenseAdapter);
                }
                break;

            case R.id.orderbyDate:
                populateExpenseList();
                Log.i(TAG, "order by date");
                break;

            case R.id.settle_up:
                if (expenseSnapshotMap.size() == 0)
                    showAlertForNoExpense();
                else
                    settleUpExpenses();
                break;

            case R.id.export:
                if (expenseSnapshotMap.size() == 0)
                    showAlertForNoExpense();
                else {
                    ExportUtility.exportExpenses(this, group_name);
                }
                break;

            //Sign Out
            case R.id.signout:
                AppUtils.signOut(this);

                intent = new Intent(ExpenseList.this, MainActivity.class);
                startActivity(intent);
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void showAlertForNoExpense() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.no_expense));
        builder.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}