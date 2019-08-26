package com.google.app.splitwise_clone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.app.splitwise_clone.groups.GroupsList;
import com.google.app.splitwise_clone.model.Expense;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.User;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.app.splitwise_clone.utils.FriendsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FriendsList extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseAuth.IdTokenListener mIdTokenListener;
    private String userName = "anonymous";
    List<String> friends = new ArrayList<>();
    Map<String, Float> amountSpentByMember = new HashMap<>();
    Map<String, Float> amountDueByMember = new HashMap<>();
    Map<String, Map<String, Float>> expenseMatrix = new HashMap<>();
    private String TAG = "Friendslist_Page";
    private RecyclerView friends_rv;
    private FriendsAdapter mFriendsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);

        userName = FirebaseUtils.getUserName();

        getSupportActionBar().setTitle(userName);
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        friends_rv = (RecyclerView) findViewById(R.id.friends_rv);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        friends_rv.setLayoutManager(layoutManager);
//        updateDB("group1");
        }

        private void populateFriendsList(){
            //Get the amount owed in all the groups by the user
            Query query = mDatabaseReference.child("users").orderByChild(userName).limitToLast(1);//TODO parameterize name
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                int id = 0;
                    if (dataSnapshot.exists()) {
                        Map<String, SingleBalance> allbalances = null;
                        for (DataSnapshot i : dataSnapshot.getChildren()) {

                            Log.i(TAG, i.getKey());
                            User f = i.getValue(User.class);
//                        Map<String, SingleBalance> balances= f.getAllBalance();
//                        id = f.getId();
                            allbalances = f.getBalances();
//                        SingleBalance sb1 = new SingleBalance(1212, "test");
//                        f.addToBalance("new_fri", sb1);
//                        f.addToGroup("G3");
//                        mDatabaseReference.child("friends/" + id).setValue(f);
                        }

                        mFriendsAdapter = new FriendsAdapter(friends, expenseMatrix, FriendsList.this);
                        friends_rv.setAdapter(mFriendsAdapter);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.friends_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent;
        switch (item.getItemId()) {

            case R.id.saveFriend:
                intent = new Intent(FriendsList.this, AddFriend.class);
//                intent.putExtra("friendId", id);
                startActivity(intent);

//                Query query = mDatabaseReference.child("users").orderByChild("mukesh").limitToLast(1);
//                query.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                        int id = 0;
//                        if (dataSnapshot.exists()) {
//                            for (DataSnapshot i : dataSnapshot.getChildren()) {
//                                Log.i(TAG, i.getKey());
//                                User f = i.getValue(User.class);
//                                id = f.getId();
//                                //Add a user to a group
////                        f.addToGroup("G1");
////                        f.addToGroup("G2");
////                        mDatabaseReference.child("friends/" + id).setValue(f);
////                        f.removeFromGroup("G2");
////                        mDatabaseReference.child("friends/" + id).setValue(f);
//                            }
//                        }
//                        Intent intent = new Intent(FriendsList.this, GroupsList.class);
//                        intent.putExtra("friendId", id);
//                        startActivity(intent);
//
//                    }
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError databaseError) {
//                    }
//                });

                break;

            case R.id.gotoGroups:
                intent = new Intent(FriendsList.this, GroupsList.class);
                startActivity(intent);
                break;

                //Sign Out
            case R.id.signout:
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();
                SharedPreferences prefs = getSharedPreferences(MainActivity.SPLIT_PREFS, 0);
                prefs.edit().remove(MainActivity.USERNAME_KEY).commit();
                prefs.edit().remove(MainActivity.PASSWORD_KEY).commit();
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }


    public void updateDB(final String group_name){

        //update the participant's total amount
        final DatabaseReference mDatabaseReference;
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        Query query = mDatabaseReference.child("users/" + userName + "/friends");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    Map<String, Float> indExp = new HashMap<>();
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        String friendName = i.getKey();
                        friends.add(friendName);
                        indExp.put(friendName, 0.0f);
                    }
                    expenseMatrix.put(userName, indExp);//add all the friends to the expense matrix
//                groupMembers = members.toArray(new String[0]);
                }

                //get All the expenses that are not settled

//        Query query = mDatabaseReference.child("groups/" + group_name + "/expenses");
                Query query = mDatabaseReference.child("groups/");
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
//                        List<DataSnapshot> expenseSnapshotList = new ArrayList<>();

                            float totalGroupExpense = 0.2f;
                            for (DataSnapshot i : dataSnapshot.getChildren()) {
                                Group group = i.getValue(Group.class);
                                Map<String, Expense> expenses = group.getExpenses();
//                            expenseSnapshotList.add(i);
                            Iterator itEx = expenses.entrySet().iterator();
                            while(itEx.hasNext()) {
                                Map.Entry pairEx = (Map.Entry) itEx.next();
                                Expense expense = (Expense) pairEx.getValue();


//                                Expense expense = i.getValue(Expense.class);

//                            String date = expense.getDateSpent();
//                            final String expense_id = i.getKey();
                                String spender = expense.getMemberSpent();
                                Map<String, SingleBalance> splitExpense = expense.getSplitExpense();

                                //amount due by individuals
                                Iterator it = splitExpense.entrySet().iterator();
                                while (it.hasNext()) {
                                    Map.Entry pair = (Map.Entry) it.next();
                                    String name = (String) pair.getKey();
                                    SingleBalance balance = (SingleBalance) pair.getValue();
                                    float amount = balance.getAmount();

                                    if (amountDueByMember.containsKey(name)) {
                                        amountDueByMember.put(name, amountDueByMember.get(name) + amount);
                                    } else amountDueByMember.put(name, amount);


                                    if (TextUtils.equals(spender, name)) {

                                    }
//                                    matrix
//                                    m1	-	m2-100	m3+15	m4-200
//                                    m2	-	m1+100	m3-200	m4+50
//                                    m3	-	m1+100	m2+40	m4+98
//                                    m4	-	m1+100	m2-90	m3-20
//
                                    else {//Expense Matrix
                                        Map<String, Float> indExp = new HashMap<>();
                                        if (expenseMatrix.containsKey(spender)) {
                                            indExp = expenseMatrix.get(spender);
                                            float newAmount = amount;
                                            if (indExp.containsKey(name)) {
                                                newAmount = indExp.get(name) + amount;
                                            } else {

                                            }
                                            indExp.put(name, newAmount);
                                            expenseMatrix.put(spender, indExp);
//                                            } else {
//                                            indExp.put(name, amount);
//                                            expenseMatrix.put(spender, indExp);
                                        }
                                    }
                                    it.remove(); // avoids a ConcurrentModificationException
                                }

                                //total amount by the group members
                                if (amountSpentByMember.containsKey(spender))
                                    amountSpentByMember.put(spender, amountSpentByMember.get(spender) + expense.getTotal());
                                else
                                    amountSpentByMember.put(spender, expense.getTotal());

                                //total expense in the group
                                totalGroupExpense += expense.getTotal();
                            }
                            }
                            Log.i(TAG, "" + amountSpentByMember.size());
                            Log.i(TAG, " group expense " + totalGroupExpense);
                            Log.i(TAG, "individual amount due " + amountDueByMember.size());

                            mFriendsAdapter = new FriendsAdapter(friends, expenseMatrix, FriendsList.this);
                            friends_rv.setAdapter(mFriendsAdapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });

    }
    @Override
    public void onResume() {
        super.onResume();
        updateDB("group1");
    }

}