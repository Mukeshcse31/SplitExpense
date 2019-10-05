package com.google.app.splitwise_clone;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.app.splitwise_clone.model.Balance;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.app.splitwise_clone.utils.FriendsAdapter;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FriendsFragment extends Fragment implements FriendsAdapter.OnClickListener {

    private DatabaseReference mDatabaseReference;
    private String userName = "anonymous";
    ChildEventListener firebaseListener;
    private String balanceSummaryTxt;
    private onFriendClickListener mOnFriendClickListener;
    Map<String, Float> amountSpentByMember = null;
    Map<String, Float> amountDueByMember = null;
    Map<String, Map<String, Float>> expenseMatrix = null;
    List<String> friends = new ArrayList<>();
    private String TAG = "Friendslist_Page";
    private RecyclerView friends_rv;
    private FriendsAdapter mFriendsAdapter;
    float amountSpentByUser = 0.0f, balanceAmount = 0.0f;

    public FriendsFragment() {
        // Required empty public constructor
        Log.i(TAG, "fragment con");

        userName = FirebaseUtils.getUserName();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friends, container, false);

        setHasOptionsMenu(true);

        friends_rv = rootView.findViewById(R.id.friends_rv);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        friends_rv.setLayoutManager(layoutManager);
        return rootView;
    }

    // Override onAttach to make sure that the container activity has implemented the callback
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // This makes sure that the host activity has implemented the callback interface
        // If not, it throws an exception
        try {
            mOnFriendClickListener = (onFriendClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnImageClickListener");
        }
    }


    public void updateUsersAmount() {

        amountSpentByMember = new HashMap<>();
        amountDueByMember = new HashMap<>();
        expenseMatrix = new HashMap<>();
        friends = new ArrayList<>();

        //update the participant's total amount
        Query query = mDatabaseReference.child("groups/").orderByChild("members/" + userName + "/name").equalTo(userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists()) {
                    balanceAmount = 0f;
                    amountSpentByUser = 0.0f;
                    Balance balance = new Balance();
                    Map<String, Map<String, Float>> amountGroup = new HashMap<>();

                    //loop through the groups
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
                            balanceAmount += amount;

                            //group-wise balance
                            Map<String, Float> eachGroup = new HashMap<>();
                            eachGroup.put(groupName, amount);
                            if (amountGroup.containsKey(expenseParticipant)) {
                                amountGroup.get(expenseParticipant).put(groupName, amount);
                            } else
                                amountGroup.put(expenseParticipant, eachGroup);

                        }
                        amountSpentByUser += amountSpentForGroup;
                    }
                    balance.setAmount(amountSpentByUser);
                    amountGroup.remove(userName);
                    balance.setGroups(amountGroup);

                    balanceSummaryTxt = String.format("%s $%.2f\n%s $%.2f",
                            getString(R.string.amount_spent_by_you), Math.abs(amountSpentByUser), getString(R.string.others_owe), Math.abs(balanceAmount));
                    mOnFriendClickListener.updateUserSummary(balanceSummaryTxt);
                    mFriendsAdapter = new FriendsAdapter(amountGroup, FriendsFragment.this);
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

    public interface onFriendClickListener {
        void gotoGroupsList();

        void updateUserSummary(String summary);
    }


    @Override
    public void gotoGroup() {
        mOnFriendClickListener.gotoGroupsList();
    }

    private void startFriendsListener() {

        firebaseListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                Log.i(TAG, "child added");
                updateUsersAmount();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                Log.i(TAG, "child Changed");
                updateUsersAmount();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
//                Log.i(TAG, "child removed");
                updateUsersAmount();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mDatabaseReference.child("users/" + userName + "/balances/groups").addChildEventListener(firebaseListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


    @Override
    public void onResume() {
        super.onResume();
        mDatabaseReference = AppUtils.getDBReference();
        startFriendsListener();
    }

    private void removeListener() {
        mDatabaseReference.child("users/" + userName + "/balances/groups").removeEventListener(firebaseListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        removeListener();
        AppUtils.closeDBReference(mDatabaseReference);
        Log.i(TAG, "listener cleared");
    }
}
