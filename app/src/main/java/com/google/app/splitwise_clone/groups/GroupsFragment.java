package com.google.app.splitwise_clone.groups;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class GroupsFragment extends Fragment implements GroupsAdapter.OnClickListener {

    private DatabaseReference mDatabaseReference;
    List<DataSnapshot> dataSnapshotGroupList = new ArrayList<>();
    ChildEventListener firebaseListener;
    private RecyclerView groups_rv;
    TextView noExpense_tv;
    private String group_name;
    private String userName = "", snackBarMsg;
    onGroupClickListener mGroupListener;
    private GroupsAdapter mGroupsAdapter;
    String db_users, db_balances, db_groups, db_archivedExpenses, db_expenses, db_members, db_nonMembers,
            db_totalAmount, db_dateSpent, db_splitDues, db_images, db_category, db_owner, db_photoUrl, db_amount, db_status, db_friends, db_email, db_name, db_imageUrl;
    private AdView mAdView;

    public GroupsFragment() {
        // Required empty public constructor

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_groups, container, false);

        noExpense_tv = rootView.findViewById(R.id.noExpense_tv);
        groups_rv = rootView.findViewById(R.id.groups_rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        groups_rv.setLayoutManager(layoutManager);
        postponeEnterTransition();

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
            mGroupListener = (onGroupClickListener) context;
            initDBValues(context);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnImageClickListener");
        }
    }

    public interface onGroupClickListener {
        void gotoClickedGroup(int index, String name, View view);

        void gotoEditGroup(int index, String groupName, List<DataSnapshot> data);
    }

    private void showSnackBar() {

        Intent intent = getActivity().getIntent();
        if (intent.hasExtra(AddGroup.GROUP_NAME)) {
            group_name = intent.getStringExtra(AddGroup.GROUP_NAME);
        }

        if (intent.hasExtra(AddGroup.GROUP_ADDED)) {
            snackBarMsg = intent.getStringExtra(AddGroup.GROUP_ADDED);
        }
        if (intent.hasExtra(AddGroup.GROUP_EDITED)) {
            snackBarMsg = intent.getStringExtra(AddGroup.GROUP_EDITED);
        }
        if (intent.hasExtra(AddGroup.GROUP_DELETED)) {
            snackBarMsg = intent.getStringExtra(AddGroup.GROUP_DELETED);
        }
        if (intent.hasExtra(AddGroup.ACTION_CANCEL)) {
            snackBarMsg = intent.getStringExtra(AddGroup.ACTION_CANCEL);
        }

        AppUtils.showSnackBar(getContext(), getActivity().findViewById(android.R.id.content), snackBarMsg);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void gotoSharedGroup(int index, String name, View imageView) {
        mGroupListener.gotoClickedGroup(index, name, imageView);
    }

    @Override
    public void gotoEditGroup(final int index, final String groupName) {

        //only the owner of the group should be able to edit the group

        Query query = mDatabaseReference.child(db_groups + "/" + groupName + "/" + db_owner);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String groupOwner = (String) dataSnapshot.getValue();
                    if (TextUtils.equals(userName, groupOwner)) {
                        mGroupListener.gotoEditGroup(index, groupName, dataSnapshotGroupList);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void populateGroupList() {
        Query query = mDatabaseReference.child(db_groups).orderByChild(db_members + "/" + userName + "/" + db_name).equalTo(userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dataSnapshotGroupList.clear();
                if (dataSnapshot.exists() && !(userName == null)) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        dataSnapshotGroupList.add(i);
                    }
                    mGroupsAdapter = new GroupsAdapter(dataSnapshotGroupList, GroupsFragment.this);
                    groups_rv.setAdapter(mGroupsAdapter);
                    noExpense_tv.setVisibility(View.GONE);
                } else noExpense_tv.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        mDatabaseReference = AppUtils.getDBReference();
        userName = FirebaseUtils.getUserName();
        showSnackBar();
        populateGroupList();
        startFriendsListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        AppUtils.closeDBReference(mDatabaseReference);
        removeListener();
    }

    private void startFriendsListener() {

        firebaseListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                populateGroupList();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                populateGroupList();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                populateGroupList();
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

    private void removeListener() {
        mDatabaseReference.child(db_users + "/" + userName + "/" + db_balances + "/" + db_groups).removeEventListener(firebaseListener);
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
