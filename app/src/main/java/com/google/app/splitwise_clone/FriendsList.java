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
import com.google.app.splitwise_clone.model.Balance;
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

import org.w3c.dom.Text;

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
    List<String> groupMember = new ArrayList<>();
    Map<String, Float> amountSpentByMember = null;
    Map<String, Float> amountDueByMember = null;
    Map<String, Map<String, Float>> expenseMatrix = null;
    private String TAG = "Friendslist_Page";
    private RecyclerView friends_rv;
    private FriendsAdapter mFriendsAdapter;
    Map<String, Expense> expenses;
    private Map<String, SingleBalance> members;
    private Map<String, Float> splitDues = new HashMap<>();
    Map<String, Float> balances;
    private Group group;
    float totalGroupExpense = 0.2f;
    Float amountSpentByUser = 0.2f;

    public void updateUsersAmount() {

        amountSpentByMember = new HashMap<>();
        amountDueByMember = new HashMap<>();
        expenseMatrix = new HashMap<>();
        friends = new ArrayList<>();
        members = new HashMap<>();

        //update the participant's total amount
        final DatabaseReference mDatabaseReference;
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

        Query query = mDatabaseReference.child("groups/").orderByChild("members/" + userName + "/name").equalTo(userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    Balance balance = new Balance();
                    Map<String, Map<String, Float>> amountGroup = new HashMap<>();

                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        Group group = i.getValue(Group.class);
                        String groupName = i.getKey();
                        Map<String, SingleBalance> sb = group.getMembers();
                        SingleBalance sb1 = sb.get(userName);
                        Float amountSpentForGroup = sb1.getAmount();
                        Map<String, Float> dues = sb1.getSplitDues();

                        Iterator it = dues.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            String expenseParticipant = (String) pair.getKey();
                            Float amount = (Float) pair.getValue();
                            //update user Balance
                            Map<String, Float> eachGroup = new HashMap<>();
                            eachGroup.put(groupName, amount);
                            if (amountGroup.containsKey(expenseParticipant)) {
                                amountGroup.get(expenseParticipant).put(groupName, amount);
                            } else
                                amountGroup.put(expenseParticipant, eachGroup);

                        }
                        amountSpentByUser += amountSpentForGroup; //TODO put it in the APP BAR
                    }
                    balance.setAmount(amountSpentByUser);
                    amountGroup.remove(userName);
                    balance.setGroups(amountGroup);
                    mFriendsAdapter = new FriendsAdapter(amountGroup, FriendsList.this);
                    friends_rv.setAdapter(mFriendsAdapter);
                    mDatabaseReference.child("users/" + userName + "/balances/").setValue(balance);
                    Log.i(TAG, "total calculation");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }


    //update a single specified group
    public void updateGroup(final String groupName) {

        amountSpentByMember = new HashMap<>();
        amountDueByMember = new HashMap<>();
        expenseMatrix = new HashMap<>();

        //update the participant's total amount
        final DatabaseReference mDatabaseReference;
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();

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
                        splitDues.put(grouMbr, 0.0f);
                    }

                    //build the expense matrix for all the members
                    itMbr = members.entrySet().iterator();
                    while (itMbr.hasNext()) {
                        Map.Entry pairMbr = (Map.Entry) itMbr.next();
                        String grouMbr = (String) pairMbr.getKey();
                        groupMember.add(grouMbr);
                        SingleBalance sb = new SingleBalance(0.0f, "amount owed", grouMbr);
                        sb.setSplitDues(new HashMap<String, Float>(splitDues));
                        members.put(grouMbr, (SingleBalance) sb);
                    }

//loop through all the expense
                    Iterator it1 = expenses.entrySet().iterator();
                    while (it1.hasNext()) {
                        Map.Entry pairExp = (Map.Entry) it1.next();
                        Expense expense = (Expense) pairExp.getValue();
                        String spender = expense.getMemberSpent();
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

                                members.get(spender).setSplitDues(new HashMap<String, Float>(lenderSplit));
                                members.get(name).setSplitDues(new HashMap<String, Float>(borrowerSplit));

                            }
                            it.remove(); // avoids a ConcurrentModificationException
                        }

                        //total expense in the group
                        totalGroupExpense += expense.getTotal();

                        //add the amount spent by the members
                        float amountSpentByMember = members.get(spender).getAmount() + expense.getTotal();
                        members.get(spender).setAmount(amountSpentByMember);
                        members.get(spender).setStatus("you owe");
                        if (amountSpentByMember > 0)
                            members.get(spender).setStatus("others owe you");
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
                startActivity(intent);

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

    @Override
    public void onResume() {
        super.onResume();
        updateGroup("group1");
        updateGroup("group2");
        updateUsersAmount();
    }

}