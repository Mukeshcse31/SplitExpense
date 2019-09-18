package com.google.app.splitwise_clone.expense;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.ExpenseAdapter;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ExpenseList extends AppCompatActivity implements ExpenseAdapter.OnClickListener {

    private DatabaseReference mDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mPhotosStorageReference;
    private String TAG = ExpenseList.class.getSimpleName();
    List<DataSnapshot> expenseSnapshotList, archivedExpenseSnapshotList;
    private ExpenseAdapter mExpenseAdapter;
    private RecyclerView expenses_rv;
    private FloatingActionButton mFloatingActionButton;
    private Button settleup_bn, export_bn;
    private String group_name;
    private ImageView groupImage;
    private String userName = "";
    private TextView groupName_tv, user_balance, user_summary, noExpenses_tv, settleup_tv;
    public static String GROUP_NAME = "group_name";
    public static String EDIT_EXPENSE = "edit_expense";
    public static String EDIT_EXPENSEID = "edit_expenseID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);
        mDatabaseReference = AppUtils.getDBReference();

        groupName_tv = findViewById(R.id.groupName_tv);
        settleup_tv = findViewById(R.id.settleup_tv);
        noExpenses_tv = findViewById(R.id.noExpenses_tv);

        user_balance = findViewById(R.id.user_balance);
        user_summary = findViewById(R.id.user_summary);
        expenses_rv = findViewById(R.id.expenses_rv);
        mFloatingActionButton = findViewById(R.id.add_expense_fab);
        settleup_bn = findViewById(R.id.settleup_bn);
        export_bn = findViewById(R.id.export_bn);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        expenses_rv.setLayoutManager(layoutManager);
        groupImage = findViewById(R.id.groupImage);
        mFirebaseStorage = AppUtils.getDBStorage();
        userName = FirebaseUtils.getUserName();

        Intent intent = getIntent();
        if (intent.hasExtra("group_name")) {
            group_name = intent.getStringExtra("group_name");
            getSupportActionBar().hide();
            groupName_tv.setText(group_name);

//            Toast.makeText(this, "Expense list - " + group_name, Toast.LENGTH_LONG).show();
//            populateExpenseList(); called in onResume method
        }

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ExpenseList.this, AddExpense.class);
                intent.putExtra(GROUP_NAME, group_name);
                startActivity(intent);
            }
        });

        settleup_bn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                settleUpExpenses();
            }
        });
        loadGroupImage(group_name);

        settleup_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                populateSettledUpExpenses();
            }
        });
    }

    //populate all the archived expenses
    public void populateSettledUpExpenses() {
        settleup_tv.setVisibility(View.GONE);
        expenses_rv.setVisibility(View.VISIBLE);
        mExpenseAdapter = new ExpenseAdapter(archivedExpenseSnapshotList, ExpenseList.this);
        expenses_rv.setAdapter(mExpenseAdapter);
    }

    public void settleUpExpenses() {

//        add all the expenses to archivedExpenses for later use
        for (DataSnapshot d : expenseSnapshotList) {
            Expense expense = d.getValue(Expense.class);
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
    public void gotoExpenseDetails(String expenseId, int index) {

        Expense expense = expenseSnapshotList.get(index).getValue(Expense.class);
        Intent intent = new Intent(ExpenseList.this, AddExpense.class);
        intent.putExtra(GROUP_NAME, group_name);
        intent.putExtra(EDIT_EXPENSEID, expenseId);
        intent.putExtra(EDIT_EXPENSE, expense);
        startActivity(intent);
        Log.i(TAG, String.format("clicked the expense %d", index));

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
        Query query = mDatabaseReference.child("groups/" + group_name + "/expenses");
//.orderByChild("active").equalTo("Yes") is not required as settle up is implemented
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                expenseSnapshotList = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        expenseSnapshotList.add(i);
                    }
                }
                //TODO display either no expense or settled up
                if (expenseSnapshotList.size() == 0) {
                    settleup_tv.setVisibility(View.VISIBLE);
                    expenses_rv.setVisibility(View.GONE);
                    getArchivedExpense();
                    settleup_bn.setClickable(false);
                    export_bn.setClickable(false);

                } else {
                    settleup_tv.setVisibility(View.GONE);
                    noExpenses_tv.setVisibility(View.GONE);
                    expenses_rv.setVisibility(View.VISIBLE);
                    mExpenseAdapter = new ExpenseAdapter(expenseSnapshotList, ExpenseList.this);
                    expenses_rv.setAdapter(mExpenseAdapter);
                    settleup_bn.setClickable(true);
                    export_bn.setClickable(true);
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
                archivedExpenseSnapshotList = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        archivedExpenseSnapshotList.add(i);
                    }
                }

                if (archivedExpenseSnapshotList.size() == 0) {
                    noExpenses_tv.setVisibility(View.VISIBLE);
                    settleup_tv.setVisibility(View.GONE);
                } else
                    settleup_tv.setVisibility(View.VISIBLE);
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
//                                float balanceAmount = (float) sb.getAmount();
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

    public void exportExpenses(View view) {
ExportUtility.exportExpenses(this, group_name);
    }
}