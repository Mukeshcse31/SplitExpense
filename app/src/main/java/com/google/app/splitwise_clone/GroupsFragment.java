package com.google.app.splitwise_clone;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.app.splitwise_clone.expense.AddExpense;
import com.google.app.splitwise_clone.groups.AddGroup;
import com.google.app.splitwise_clone.utils.AppUtils;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.app.splitwise_clone.utils.GroupsAdapter;
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
    private RecyclerView groups_rv;
    private String group_name;
    private String userName = "", snackBarMsg;
    onGroupClickListener mGroupListener;
    private GroupsAdapter mGroupsAdapter;

    public GroupsFragment() {
        // Required empty public constructor
        mDatabaseReference = AppUtils.getDBReference();
        userName = FirebaseUtils.getUserName();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_groups, container, false);


        groups_rv = rootView.findViewById(R.id.groups_rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        groups_rv.setLayoutManager(layoutManager);

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
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnImageClickListener");
        }
    }

    public interface onGroupClickListener{
        void gotoClickedGroup(int index, String name);
        void gotoEditGroup(int index, String groupName, List<DataSnapshot> data );
    }

private void showSnackBar(){

    Intent intent = getActivity().getIntent();
    if (intent.hasExtra(AddGroup.GROUP_NAME)) {
        group_name = intent.getStringExtra(AddGroup.GROUP_NAME);
    }

    if(intent.hasExtra(AddGroup.GROUP_ADDED)){
        snackBarMsg = intent.getStringExtra(AddGroup.GROUP_ADDED);
    }
    if(intent.hasExtra(AddGroup.GROUP_EDITED)){
        snackBarMsg = intent.getStringExtra(AddGroup.GROUP_EDITED);
    }
    if(intent.hasExtra(AddGroup.GROUP_DELETED)){
        snackBarMsg = intent.getStringExtra(AddGroup.GROUP_DELETED);
    }
    if(intent.hasExtra(AddGroup.ACTION_CANCEL)){
        snackBarMsg = intent.getStringExtra(AddGroup.ACTION_CANCEL);
    }

    if(!TextUtils.isEmpty(snackBarMsg)) {
        final Snackbar snackBar = Snackbar.make(getActivity().findViewById(android.R.id.content),snackBarMsg, Snackbar.LENGTH_LONG);
        snackBar.setAction(getString(R.string.close), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call your action method here
                snackBar.dismiss();
            }
        });
        snackBar.show();
    }
}
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void gotoSharedGroup(int index, String name) {
        mGroupListener.gotoClickedGroup(index, name);
    }

    @Override
    public void gotoEditGroup(final int index, final String groupName) {

        //only the owner of the group should be able to edit the group

        Query query = mDatabaseReference.child("groups/" + groupName + "/owner");

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
        Query query = mDatabaseReference.child("groups").orderByChild("members/" + userName + "/name").equalTo(userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dataSnapshotGroupList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        dataSnapshotGroupList.add(i);
                    }
                    mGroupsAdapter = new GroupsAdapter(dataSnapshotGroupList, GroupsFragment.this);
                    groups_rv.setAdapter(mGroupsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        showSnackBar();
        populateGroupList();
    }
}
