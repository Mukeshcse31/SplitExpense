package com.google.app.splitwise_clone.expense;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpenseList extends AppCompatActivity implements ExpenseAdapter.OnClickListener {

    private DatabaseReference mDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
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
    private ImageView groupImage, settleup_image;
    private String userName = "", snackBarMsg = "";
    private TextView user_balance, user_summary, noExpenses_tv, settleup_tv;
    public static String GROUP_NAME = "group_name";
    public static String EDIT_EXPENSE = "edit_expense";
    public static String EDIT_EXPENSEID = "edit_expenseID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_list);

        my_toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(my_toolbar);
        //this line shows back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        app_bar_layout_exp = findViewById(R.id.app_bar_layout_exp);
        collap_toolbar_exp = findViewById(R.id.collap_toolbar_exp);

        mDatabaseReference = AppUtils.getDBReference();
//        groupName_tv = findViewById(R.id.groupName_tv);
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

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collap_toolbar_exp);

//        collapsingToolbarLayout.setContentScrimColor(Color.GREEN);


        Intent intent = getIntent();
        if (intent.hasExtra(GROUP_NAME)) {
            group_name = intent.getStringExtra(GROUP_NAME);
            collapsingToolbarLayout.setTitle(group_name);
        }

        if(intent.hasExtra(AddExpense.EXPENSE_EDITED)){
            snackBarMsg = intent.getStringExtra(AddExpense.EXPENSE_EDITED);
        }
        if(intent.hasExtra(AddExpense.EXPENSE_ADDED)){
            snackBarMsg = intent.getStringExtra(AddExpense.EXPENSE_ADDED);
        }
        if(intent.hasExtra(AddExpense.EXPENSE_DELETED)){
            snackBarMsg = intent.getStringExtra(AddExpense.EXPENSE_DELETED);
        }


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

        AppBarLayout mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout_exp);
        mAppBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            //            https://www.journaldev.com/13927/android-collapsingtoolbarlayout-example
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    isShow = true;
//                    showOption(R.id.orderbyCategory);
                    Log.i(TAG, "collapsed");
                } else if (isShow) {
                    isShow = false;
//                    hideOption(R.id.orderbyCategory);
                    Log.i(TAG, "expanded");
                }
            }
        });

//        expenses_rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
//                super.onScrollStateChanged(recyclerView, newState);
//                turnOnToolbarScrolling();
//            }
//
//            @Override
//            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
//                turnOnToolbarScrolling();
//            }
//        });
        showSnackBar(snackBarMsg);
    }


    public void showSnackBar(String message) {
//        https://www.techotopia.com/index.php/Working_with_the_Floating_Action_Button_and_Snackbar
        if(!TextUtils.isEmpty(message))
        Snackbar.make(mFloatingActionButton, message, Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.close), null).show();
    }

    private void hideOption(int[] ids) {
        for (int id : ids) {
            MenuItem item = mMenu.findItem(id);
            item.setVisible(false);
        }
    }

    private void showOption(int[] ids) {

        for (int id : ids) {
            MenuItem item = mMenu.findItem(id);
            item.setVisible(true);
        }
    }

    //populate all the archived expenses
    public void populateSettledUpExpenses(View view) {
        getArchivedExpense();
//showSettledUpExpenses();
    }

    private void showSettledUpExpenses() {
        settleup_tv.setVisibility(View.GONE);
        settleup_image.setVisibility(View.GONE);
        expenses_rv.setVisibility(View.VISIBLE);
//        archivedExpenseSnapshotMap = AppUtils.reverseExpense(archivedExpenseSnapshotMap);
        mExpenseAdapter = new ExpenseAdapter(archivedExpenseSnapshotMap, ExpenseList.this, false);
        expenses_rv.setAdapter(mExpenseAdapter);

        updateToolbarBehaviour(archivedExpenseSnapshotMap.size());
        hideOption(new int[]{R.id.orderbyCategory, R.id.settle_up, R.id.export, R.id.archivedExp});
        showOption(new int[]{R.id.orderbyDate});
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
        hideOption(new int[]{R.id.orderbyCategory, R.id.orderbyDate, R.id.settle_up, R.id.export});
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
                    mDatabaseReference.child("groups/" + group_name + "/members/").setValue(groupMembers, new DatabaseReference.CompletionListener() {
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
        mDatabaseReference.child("groups/" + group_name + "/totalAmount/").setValue(0.0);
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
                    //check if there is any archived expense
                    Query query = mDatabaseReference.child("groups/" + group_name + "/archivedExpenses").limitToFirst(1);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {

                                settleup_tv.setVisibility(View.VISIBLE);
                                settleup_image.setVisibility(View.VISIBLE);
                                expenses_rv.setVisibility(View.GONE);
                            } else
                                noExpenses_tv.setVisibility(View.VISIBLE);

                            //placed here so it takes a while to reach this and onCreateItemsOption is already called
                            hideOption(new int[]{R.id.orderbyCategory, R.id.orderbyDate, R.id.settle_up, R.id.export, R.id.archivedExp});
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
                    updateToolbarBehaviour(expenseSnapshotMap.size());
//                    getExpenseByCategory();
                    showOption(new int[]{R.id.orderbyCategory, R.id.settle_up, R.id.export, R.id.archivedExp});

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void updateToolbarBehaviour(final int rvLength){

//expenses_rv.postDelayed(new Runnable() {
//    @Override
//    public void run() {
//        if (((LinearLayoutManager)expenses_rv.getLayoutManager()).findLastCompletelyVisibleItemPosition() == rvLength - 1) {
//            turnOffToolbarScrolling();
//        } else {
//            turnOnToolbarScrolling();
//        }
//    }
//}, 1000);

    }

    private void getExpenseByCategory() {
        Query query = mDatabaseReference.child("groups/" + group_name + "/expenses").orderByChild("category");
//.orderByChild("active").equalTo("Yes") is not required as settle up is implemented
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                categorizedExpenseMap = new LinkedHashMap<>();//to maintain the order of elements
                if (dataSnapshot.exists()) {

                    int catCount = 0;
                    String catAry[] = new String[6];
                    for (DataSnapshot i : dataSnapshot.getChildren()) {

                        Expense expense = i.getValue(Expense.class);
                        String cur_Category = expense.getCategory();
                        categorizedExpenseMap.put(i.getKey(), i.getValue(Expense.class));

                        if (catCount == 0) {
                            catAry[0] = cur_Category;
                            catCount++;
                        } else {
                            if (!TextUtils.equals(catAry[catCount - 1], cur_Category)) {
                                categorizedExpenseMap.put(catAry[catCount - 1], null);
                                catAry[catCount] = cur_Category;
                                catCount++;
                            }
                        }
                    }
                    //just add for the last item
                    categorizedExpenseMap.put(catAry[catCount - 1], null);
                    Log.i(TAG, categorizedExpenseMap.size() + "");

//                    if (categorizedExpenseMap.size() > 0) {
                    categorizedExpenseMap = AppUtils.reverseExpense(categorizedExpenseMap);
                    mExpenseAdapter = new ExpenseAdapter(categorizedExpenseMap, ExpenseList.this, true);
                    updateToolbarBehaviour(categorizedExpenseMap.size());

                    expenses_rv.setVisibility(View.VISIBLE);
                    expenses_rv.setAdapter(mExpenseAdapter);
                    hideOption(new int[]{R.id.orderbyCategory});
                    showOption(new int[]{R.id.orderbyDate, R.id.settle_up, R.id.export, R.id.archivedExp});
                    noExpenses_tv.setVisibility(View.GONE);
//                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getArchivedExpense() {
        Query query = mDatabaseReference.child("groups/" + group_name + "/archivedExpenses").orderByChild("category");

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
//                    settleup_tv.setVisibility(View.VISIBLE);
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
        mMenu = menu;
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
                        if (!TextUtils.equals(userName, friendName)) {
                            String status = "spent";
                            float amount = Float.parseFloat(String.valueOf(pair.getValue()));
                            if (amount > 0) status = "owes you";
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
                getExpenseByCategory();

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

            case R.id.archivedExp:
                getArchivedExpense();
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

    @Override
    public void onBackPressed() {
        finish();
    }

    public void turnOffToolbarScrolling() {

        //turn off scrolling
//        Toolbar.LayoutParams toolbarLayoutParams = (Toolbar.LayoutParams) my_toolbar.getLayoutParams();
//
//        toolbarLayoutParams.setScrollFlags(0);
//        my_toolbar.setLayoutParams(toolbarLayoutParams);

        CoordinatorLayout.LayoutParams appBarLayoutParams = (CoordinatorLayout.LayoutParams) app_bar_layout_exp.getLayoutParams();
        appBarLayoutParams.setBehavior(null);
        app_bar_layout_exp.setLayoutParams(appBarLayoutParams);
    }

//    https://stackoverflow.com/questions/32404979/dont-collapse-toolbar-when-recyclerview-fits-the-screen
    public void turnOnToolbarScrolling() {
        //turn on scrolling
//        AppBarLayout.LayoutParams toolbarLayoutParams = (AppBarLayout.LayoutParams) my_toolbar.getLayoutParams();
//        toolbarLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
//        my_toolbar.setLayoutParams(toolbarLayoutParams);

        CoordinatorLayout.LayoutParams appBarLayoutParams = (CoordinatorLayout.LayoutParams) app_bar_layout_exp.getLayoutParams();
        appBarLayoutParams.setBehavior(new AppBarLayout.Behavior());
        app_bar_layout_exp.setLayoutParams(appBarLayoutParams);
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