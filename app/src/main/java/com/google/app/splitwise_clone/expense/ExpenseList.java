package com.google.app.splitwise_clone.expense;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.app.splitwise_clone.SignIn;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.SummaryActivity;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.ExpenseListViewType;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExpenseList extends AppCompatActivity implements ExpenseAdapter.OnClickListener {

    private DatabaseReference mDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    ChildEventListener firebaseListener;
    private StorageReference mPhotosStorageReference;
    private String TAG = ExpenseList.class.getSimpleName();
    LinkedHashMap<String, Expense> expenseSnapshotMap, categorizedExpenseMap, archivedExpenseSnapshotMap;
    private ExpenseAdapter mExpenseAdapter;
    CollapsingToolbarLayout collapsingToolbarLayout;
    private RecyclerView expenses_rv;
    private FloatingActionButton mFloatingActionButton;
    private AppBarLayout app_bar_layout_exp;
    private Toolbar my_toolbar;
    private CollapsingToolbarLayout collap_toolbar_exp;
    private String group_name;
    private static Menu mMenu;
    private ImageView settleup_image;
    private ImageView groupImage;
    private String userName = "", snackBarMsg = "";
    private TextView user_balance, user_summary, noExpenses_tv, settleup_tv;
    public static String GROUP_NAME = "group_name";
    public static String EDIT_EXPENSE = "edit_expense";
    public static String EDIT_EXPENSEID = "edit_expenseID";
    public static final String EXPENSE_MAP = "expenseSnapshotMap";
    public static final String ARCHIVEDEXPENSE_MAP = "archivedExpenseSnapshotMap";
    public static final String CATEGORIZEDEXPENSE_MAP = "categorizedExpenseMap";
    public static String VIEW_TYPE = ExpenseListViewType.DATE.getValue();
    public static String VIEW_TYPE_KEY = "viewType";

    String db_users, db_balances, db_groups, db_archivedExpenses, db_expenses, db_members, db_nonMembers,
            db_totalAmount, db_dateSpent, db_splitDues, db_images, db_category, db_owner, db_photoUrl, db_amount, db_status, db_friends, db_email, db_name, db_imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collap_toolbar_exp);
        collapsingToolbarLayout.setTitleEnabled(false);

        invalidateOptionsMenu();
        my_toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        my_toolbar.setTitle("");//don't know why, setting the title here only works to update later
        setSupportActionBar(my_toolbar);

        //this line shows back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        app_bar_layout_exp = findViewById(R.id.app_bar_layout_exp);
        collap_toolbar_exp = findViewById(R.id.collap_toolbar_exp);

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
        if (intent.hasExtra(GROUP_NAME)) {
            group_name = intent.getStringExtra(GROUP_NAME);
//            collapsingToolbarLayout.setTitle(group_name);
            my_toolbar.setTitle(group_name);
        }

        if (intent.hasExtra(AddExpense.EXPENSE_EDITED)) {
            snackBarMsg = intent.getStringExtra(AddExpense.EXPENSE_EDITED);
        }
        if (intent.hasExtra(AddExpense.EXPENSE_ADDED)) {
            snackBarMsg = intent.getStringExtra(AddExpense.EXPENSE_ADDED);
        }
        if (intent.hasExtra(AddExpense.EXPENSE_DELETED)) {
            snackBarMsg = intent.getStringExtra(AddExpense.EXPENSE_DELETED);
        }

        initDBValues();
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ExpenseList.this, AddExpense.class);
                intent.putExtra(GROUP_NAME, group_name);
                startActivity(intent);
                finish();
            }
        });
        loadGroupImage(group_name);

        AppUtils.showSnackBar(this, findViewById(android.R.id.content), snackBarMsg);
    }

    //populate all the archived expenses
    public void populateSettledUpExpenses(View view) {
        getArchivedExpense();
    }

    private void showSettledUpExpenses() {

        VIEW_TYPE = ExpenseListViewType.ARCHIVE.getValue();
        settleup_tv.setVisibility(View.GONE);
        settleup_image.setVisibility(View.GONE);
        expenses_rv.setVisibility(View.VISIBLE);
        mExpenseAdapter = new ExpenseAdapter(archivedExpenseSnapshotMap, ExpenseList.this, false);
        expenses_rv.setAdapter(mExpenseAdapter);

    }

    public void settleUpExpenses() {

//        add all the expenses to archivedExpenses for later use
        Iterator it = expenseSnapshotMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            Expense expense = (Expense) pair.getValue();
            mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_archivedExpenses).push().setValue(expense);
        }
        Task deleteTask = mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_expenses).setValue(null);
        deleteTask.addOnSuccessListener(new OnSuccessListener() {
            @Override
            public void onSuccess(Object o) {
                resetMembersBalances();
                getExpenseList();

            }
        });
        AppUtils.hideOption(mMenu, new int[]{R.id.orderbyCategory, R.id.orderbyDate, R.id.settle_up, R.id.export});
        AppUtils.showMenuOption(mMenu, new int[]{R.id.archivedExp});

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
        //reset them to init values
        Query query = mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_members);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    Map<String, SingleBalance> groupMembers = (Map<String, SingleBalance>) dataSnapshot.getValue();
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        String groupMemberName = i.getKey();
                        SingleBalance sb = i.getValue(SingleBalance.class);
                        Map<String, Float> splitDues = sb.getSplitDues();

                        Map<String, Float> new_splitDues = new HashMap<>();


                        Iterator it = splitDues.entrySet().iterator();

                        //set 0 for all the group members
                        while (it.hasNext()) {

                            Map.Entry pair = (Map.Entry) it.next();
                            String grpMember = (String) pair.getKey();
                            new_splitDues.put(grpMember, 0.0f);
                        }

                        SingleBalance sb1 = null;
                        try {
                            sb1 = (SingleBalance) sb.clone();
                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                        }
                        sb1.setSplitDues(new_splitDues);
                        groupMembers.put(groupMemberName, sb1);
                    }
                    mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_members).setValue(groupMembers, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                            populateAppBar();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

//reset the total Amount
        mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_totalAmount).setValue(0.0);
    }

    private void getExpenseList() {

        VIEW_TYPE = ExpenseListViewType.DATE.getValue();
        //sparseArray is not used as it doesn't keep the order of insertion
        Query query = mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_expenses).orderByChild(db_dateSpent);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                expenseSnapshotMap = new LinkedHashMap<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        expenseSnapshotMap.put(i.getKey(), i.getValue(Expense.class));
                    }
                }

                if (expenseSnapshotMap.size() == 0) {
                    expenses_rv.setVisibility(View.GONE);

                    //check if there is any archived expense
                    Query query = mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_archivedExpenses).limitToFirst(1);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                expenses_rv.setVisibility(View.INVISIBLE);
                                settleup_tv.setVisibility(View.VISIBLE);
                                settleup_image.setVisibility(View.VISIBLE);
                                AppUtils.showMenuOption(mMenu, new int[]{R.id.archivedExp});
                            } else {

                                noExpenses_tv.setVisibility(View.VISIBLE);
                                AppUtils.hideOption(mMenu, new int[]{R.id.orderbyCategory, R.id.orderbyDate, R.id.settle_up, R.id.export});
                                //menu is not created here, so called after this too
                            }
                            //placed here so it takes a while to reach this and onCreateItemsOption is already called
                            AppUtils.hideOption(mMenu, new int[]{R.id.orderbyCategory, R.id.orderbyDate, R.id.settle_up, R.id.export});

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });


                } else {
                    settleup_tv.setVisibility(View.GONE);
                    settleup_image.setVisibility(View.INVISIBLE);
                    noExpenses_tv.setVisibility(View.GONE);
                    expenses_rv.setVisibility(View.VISIBLE);
                    expenseSnapshotMap = AppUtils.reverseExpense(expenseSnapshotMap);
                    mExpenseAdapter = new ExpenseAdapter(expenseSnapshotMap, ExpenseList.this, true);
                    expenses_rv.setAdapter(mExpenseAdapter);
                    AppUtils.showMenuOption(mMenu, new int[]{R.id.orderbyCategory, R.id.settle_up, R.id.export, R.id.archivedExp});
                    AppUtils.hideOption(mMenu, new int[]{R.id.orderbyDate});
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getExpenseByCategory() {

        VIEW_TYPE = ExpenseListViewType.CATEGORY.getValue();

        Query query = mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_expenses).orderByChild(db_category);
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

                        if (!TextUtils.equals(prev_Category, category)) {
                            //add a dummy object for category
                            categorizedExpenseMap.put(category, null);
                            prev_Category = category;
                        }
                        categorizedExpenseMap.put(i.getKey(), i.getValue(Expense.class));
                    }
                }
                if (categorizedExpenseMap.size() > 0) {

//                    categorizedExpenseMap = AppUtils.reverseExpense(categorizedExpenseMap);
                    mExpenseAdapter = new ExpenseAdapter(categorizedExpenseMap, ExpenseList.this, true);
                    expenses_rv.setAdapter(mExpenseAdapter);

                    expenses_rv.setVisibility(View.VISIBLE);
                    noExpenses_tv.setVisibility(View.GONE);

                } else {
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        AppUtils.hideOption(mMenu, new int[]{R.id.orderbyCategory});
        AppUtils.showMenuOption(mMenu, new int[]{R.id.orderbyDate, R.id.settle_up, R.id.export, R.id.archivedExp});
    }

    private void getArchivedExpense() {
        Query query = mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_archivedExpenses).orderByChild(db_category);

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
                    expenses_rv.setVisibility(View.GONE);
                    noExpenses_tv.setVisibility(View.VISIBLE);
                    noExpenses_tv.setText(getString(R.string.no_arch_expense));
                    settleup_tv.setVisibility(View.GONE);
                } else {
                    showSettledUpExpenses();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        AppUtils.hideOption(mMenu, new int[]{R.id.orderbyCategory, R.id.settle_up, R.id.export, R.id.archivedExp});
        AppUtils.showMenuOption(mMenu, new int[]{R.id.orderbyDate});
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        String group_nameKey = getResources().getString(R.string.group_name);

        //TODO expenseSnapShot and archivedSnapShot
        if (bundle.containsKey(group_nameKey)) {
            group_name = bundle.getString(group_nameKey);
            Log.v("savedInstanceState", "Inside of onRestoreInstanceState " + group_name);
        }

        if (bundle.containsKey(VIEW_TYPE_KEY))
            VIEW_TYPE = bundle.getString(VIEW_TYPE_KEY);

        if (bundle.containsKey(EXPENSE_MAP))
            expenseSnapshotMap = (LinkedHashMap<String, Expense>) bundle.getSerializable(EXPENSE_MAP);

        if (bundle.containsKey(CATEGORIZEDEXPENSE_MAP))
            categorizedExpenseMap = (LinkedHashMap<String, Expense>) bundle.getSerializable(CATEGORIZEDEXPENSE_MAP);

        if (bundle.containsKey(ARCHIVEDEXPENSE_MAP))
            archivedExpenseSnapshotMap = (LinkedHashMap<String, Expense>) bundle.getSerializable(ARCHIVEDEXPENSE_MAP);

        super.onRestoreInstanceState(bundle);

    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {

        bundle.putString(getResources().getString(R.string.group_name), group_name);
        bundle.putString(VIEW_TYPE_KEY, VIEW_TYPE);
        bundle.putSerializable(EXPENSE_MAP, expenseSnapshotMap);
        bundle.putSerializable(CATEGORIZEDEXPENSE_MAP, categorizedExpenseMap);
        bundle.putSerializable(ARCHIVEDEXPENSE_MAP, archivedExpenseSnapshotMap);

        super.onSaveInstanceState(bundle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu = menu;
        getMenuInflater().inflate(R.menu.menu_expense_list, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        mDatabaseReference = AppUtils.getDBReference();

        //TODO retrieve from Saved instance state
        populateAppBar();
        getExpenseList();
//        setExpenseAdapter();
        startListener();

    }

    private void setExpenseAdapter() {

        if (VIEW_TYPE == ExpenseListViewType.DATE.getValue()) {

            if (expenseSnapshotMap == null) getExpenseList();
            else mExpenseAdapter = new ExpenseAdapter(expenseSnapshotMap, ExpenseList.this, true);

        } else if (VIEW_TYPE == ExpenseListViewType.CATEGORY.getValue()) {
            mExpenseAdapter = new ExpenseAdapter(categorizedExpenseMap, ExpenseList.this, true);
            expenses_rv.setAdapter(mExpenseAdapter);
        } else if (VIEW_TYPE == ExpenseListViewType.ARCHIVE.getValue()) {
            mExpenseAdapter = new ExpenseAdapter(archivedExpenseSnapshotMap, ExpenseList.this, false);
            expenses_rv.setAdapter(mExpenseAdapter);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        removeListener();
        AppUtils.closeDBReference(mDatabaseReference);
    }

    private void populateAppBar() {

        //get the user's splitDues
        Query query = mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_members + "/" + userName + "/" + db_splitDues);
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
                        if (!TextUtils.equals(userName, friendName)) {
                            String status = getString(R.string.spent_you);
                            float amount = Float.parseFloat(String.valueOf(pair.getValue()));
                            if (amount > 0) status = getString(R.string.owes_you);
                            summary += String.format("%s %s $%.2f \n", friendName, status, Math.abs(amount));
                        }
                    }
                    user_summary.setText(summary);
                } else
                    user_summary.setText("");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        //get the user's splitDues
        Query query1 = mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_members + "/" + userName + "/" + db_amount);
        query1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    float balanceAmount = Float.parseFloat(String.valueOf(dataSnapshot.getValue()));
                    String label = getString(R.string.amount_others_spent);
                    if (balanceAmount > 0) label = getString(R.string.amount_you_spent);
                    user_balance.setText(String.format("%s $%.2f ", label, Math.abs(balanceAmount)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void loadGroupImage(final String group_name) {

        //TODO reuse this with userImage load in SummaryActivity
//                    https://firebase.google.com/docs/storage/android/download-files
        mPhotosStorageReference = mFirebaseStorage.getReference();
        StorageReference islandRef = mPhotosStorageReference.child(db_images + "/" + db_groups + "/" + group_name);

        final long ONE_MEGABYTE = 1024 * 1024;
        islandRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for "images/island.jpg" is returns, use this as needed
                Glide.with(ExpenseList.this)
                        .load(bytes)
                        .asBitmap()
                        .placeholder(R.drawable.group)
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;
        switch (item.getItemId()) {

            case R.id.orderbyCategory:
                Log.i(TAG, "order by category");
                getExpenseByCategory();

                break;

            case R.id.orderbyDate:
                getExpenseList();
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

            case R.id.archivedExp:
                getArchivedExpense();
                break;

            //Sign Out
            case R.id.signout:
                FirebaseUtils.signOut(this);
                intent = new Intent(ExpenseList.this, SignIn.class);
                startActivity(intent);
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ExpenseList.this, SummaryActivity.class);
        startActivity(intent);
        finish();
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

    private void populateExpenseAdapter() {

        if (VIEW_TYPE == ExpenseListViewType.DATE.getValue())
            getExpenseList();
        else if (VIEW_TYPE == ExpenseListViewType.CATEGORY.getValue())
            getExpenseByCategory();
        else if (VIEW_TYPE == ExpenseListViewType.ARCHIVE.getValue())
            getArchivedExpense();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(TAG, "config changed");

    }

    private void startListener() {

        firebaseListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                getExpenseList();
//                populateExpenseAdapter();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                populateExpenseAdapter();
                getExpenseList();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                getExpenseList();
//                populateExpenseAdapter();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_expenses).addChildEventListener(firebaseListener);
    }

    private void removeListener() {
        mDatabaseReference.child(db_groups + "/" + group_name + "/" + db_expenses).removeEventListener(firebaseListener);
    }


    private void initDBValues() {
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
        db_dateSpent = getString(R.string.db_dateSpent);
        db_category = getString(R.string.db_category);
        db_splitDues = getString(R.string.db_splitDues);
        db_images = getString(R.string.db_images);

    }
}