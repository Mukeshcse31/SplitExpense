package com.google.app.splitwise_clone.friends;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Balance;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
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
    ProgressBar progressBar;
    private String balanceSummaryTxt;
    private onFriendClickListener mOnFriendClickListener;
    Map<String, Float> amountSpentByMember = null;
    Map<String, Float> amountDueByMember = null;
    Map<String, Map<String, Float>> expenseMatrix = null;
    List<String> friends = new ArrayList<>();
    private String TAG = "Friendslist_Page";
    private RecyclerView friends_rv;
    private TextView noExpense_tv;
    private FriendsAdapter mFriendsAdapter;
    float amountSpentByUser = 0.0f, balanceAmount = 0.0f;
    String db_users, db_balances, db_groups, db_archivedExpenses, db_expenses, db_members, db_nonMembers,
            db_totalAmount, db_dateSpent, db_splitDues, db_images, db_category, db_owner, db_photoUrl, db_amount, db_status, db_friends, db_email, db_name, db_imageUrl;
    private AdView mAdView;

    public FriendsFragment() {
        // Required empty public constructor
        Log.i(TAG, "fragment con");

        userName = FirebaseUtils.getUserName();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friends, container, false);

        setHasOptionsMenu(true);

        noExpense_tv = rootView.findViewById(R.id.noExpense_tv);
        progressBar = rootView.findViewById(R.id.progressBar);
        friends_rv = rootView.findViewById(R.id.friends_rv);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        friends_rv.setLayoutManager(layoutManager);

        MobileAds.initialize(getContext(), new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = rootView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

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
            initDBValues(context);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnImageClickListener");
        }
    }


    public void updateUsersAmount() {

        progressBar.setVisibility(View.VISIBLE);
        amountSpentByMember = new HashMap<>();
        amountDueByMember = new HashMap<>();
        expenseMatrix = new HashMap<>();
        friends = new ArrayList<>();

        //update the participant's total amount
        Query query = mDatabaseReference.child(db_groups).orderByChild(db_members + "/" + userName + "/" + db_name).equalTo(userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.exists() && !(userName == null)) {
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
                    mDatabaseReference.child(db_users + "/" + userName + "/" + db_balances).setValue(balance);
                    noExpense_tv.setVisibility(View.GONE);
                    Log.i(TAG, "total calculation");
                } else {

                    noExpense_tv.setVisibility(View.VISIBLE);
                }
                progressBar.setVisibility(View.GONE);
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
        mDatabaseReference.child(db_users + "/" + userName + "/" + db_balances + "/" + db_groups).addChildEventListener(firebaseListener);
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
        updateUsersAmount();
        startFriendsListener();
    }

    private void removeListener() {
        mDatabaseReference.child(db_users + "/" + userName + "/" + db_balances + "/" + db_groups).removeEventListener(firebaseListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        removeListener();
        AppUtils.closeDBReference(mDatabaseReference);
        Log.i(TAG, "listener cleared");
    }

    private void initDBValues(Context context) {
        db_users = context.getString(R.string.db_users);
        db_balances = context.getString(R.string.db_balances);
        db_groups = context.getString(R.string.db_groups);
        db_archivedExpenses = context.getString(R.string.db_archivedExpenses);
        db_expenses = context.getString(R.string.db_expenses);
        db_members = context.getString(R.string.db_members);
        db_nonMembers = context.getString(R.string.db_nonMembers);
        db_owner = context.getString(R.string.db_owner);
        db_photoUrl = context.getString(R.string.db_photoUrl);
        db_amount = context.getString(R.string.db_amount);
        db_status = context.getString(R.string.db_status);
        db_friends = context.getString(R.string.db_friends);
        db_email = context.getString(R.string.db_email);
        db_name = context.getString(R.string.db_name);
        db_imageUrl = context.getString(R.string.db_imageUrl);
        db_totalAmount = context.getString(R.string.db_totalAmount);
        db_dateSpent = context.getString(R.string.db_dateSpent);
        db_category = context.getString(R.string.db_category);
        db_splitDues = context.getString(R.string.db_splitDues);
        db_images = context.getString(R.string.db_images);

    }
}
