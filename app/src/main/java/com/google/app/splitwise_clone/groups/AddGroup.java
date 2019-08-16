package com.google.app.splitwise_clone.groups;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.app.splitwise_clone.AddFriend;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Friend;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.GroupMembersAdapter;
import com.google.app.splitwise_clone.utils.GroupsAdapter;
import com.google.app.splitwise_clone.utils.NonGroupMembersAdapter;
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
import java.util.Set;

import static com.google.app.splitwise_clone.MainActivity.DISPLAY_NAME_KEY;
import static com.google.app.splitwise_clone.MainActivity.SPLIT_PREFS;

public class AddGroup extends AppCompatActivity implements GroupMembersAdapter.OnClickListener,
        NonGroupMembersAdapter.OnClickListener {

    private AutoCompleteTextView mGroupName;
    private DatabaseReference mDatabaseReference;

    private static final String TAG = AddGroup.class.getSimpleName();
    private GroupMembersAdapter mGroupMembersAdapter;
    private NonGroupMembersAdapter mNonGroupMembersAdapter;
    private RecyclerView members_rv;
    private RecyclerView nonmembers_rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_add_group);
        getSupportActionBar().setTitle("");
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mGroupName = findViewById(R.id.group_name);

        getSupportActionBar().setTitle("");
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        members_rv = (RecyclerView) findViewById(R.id.members_rv);
        nonmembers_rv = findViewById(R.id.nonmembers_rv);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        members_rv.setLayoutManager(layoutManager);
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(this);
        nonmembers_rv.setLayoutManager(layoutManager1);
        membersViews();

    }

    private void membersViews() {
        String path1 = getString(R.string.db_users);
        final String path2 = getString(R.string.db_groups);
        final String currentGroup = "group1";

        Query query = mDatabaseReference.child(path1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, String> all_member = new HashMap<>();
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        String name = i.getKey();
                        Friend f = i.getValue(Friend.class);
                        String email = f.getEmail();
                        all_member.put(name, email);//get all the users
                    }
//--- - - - - - - - - - - - - -TODO if dataSnapshot.exists() check--

                    final Map<String, String> all_members = all_member;
                    Query query1 = mDatabaseReference.child(path2 + "/" + currentGroup + "/members");
                    query1.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                List<String> group_membersList = new ArrayList<>();
                                for (DataSnapshot i : dataSnapshot.getChildren()) {

                                    String name = i.getKey();
                                    group_membersList.add(name);
                                }
                                Map<String, String> group_members = new HashMap<>();
                                Map<String, String> nongroup_members = new HashMap<>();

                                Iterator<String> it = all_members.keySet().iterator();
                                while (it.hasNext()) {
                                    String userName = it.next();
                                    if (group_membersList.contains(userName)) {
                                        group_members.put(userName, all_members.get(userName));
                                    } else {
                                        nongroup_members.put(userName, all_members.get(userName));
                                    }
                                }

                                mGroupMembersAdapter = new GroupMembersAdapter(group_members, AddGroup.this);
                                members_rv.setAdapter(mGroupMembersAdapter);

                                mNonGroupMembersAdapter = new NonGroupMembersAdapter(nongroup_members, AddGroup.this);
                                nonmembers_rv.setAdapter(mNonGroupMembersAdapter);

                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

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
    public void removeFriendFromGroup(final String name) {

        Query query = mDatabaseReference.child("groups/group1/members");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, SingleBalance> groupMembers = new HashMap<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {

                        String name1 = i.getKey();
                        groupMembers.put(name1, i.getValue(SingleBalance.class));

                    }
                    groupMembers.remove(name);
                }
                mDatabaseReference.child("groups/group1/members").setValue(groupMembers, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {
                        Toast.makeText(AddGroup.this, name + " removed", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        membersViews();

    }

    @Override
    public void addFriendToGroup(final String name) {

        Query query = mDatabaseReference.child("groups/group1/members");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, SingleBalance> groupMembers = new HashMap<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {

                        String name1 = i.getKey();
                        groupMembers.put(name1, i.getValue(SingleBalance.class));

                    }
                    groupMembers.put(name, new SingleBalance());

                }
                mDatabaseReference.child("groups/group1/members").setValue(groupMembers, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {
                        Toast.makeText(AddGroup.this, name + " added", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        membersViews();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_group, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addGroup:
                final String name = mGroupName.getText().toString();
                Query query = mDatabaseReference.child("groups").startAt(name).endAt(name);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        int id = 1;
                        if (dataSnapshot.exists()) {
                            mGroupName.setError(getString(R.string.email_exists));
                            Toast.makeText(AddGroup.this, "Group already exists", Toast.LENGTH_LONG).show();

                        } else {
                            //add the group creator as a member when the group is created
                            Group grp = new Group(name);
                            SharedPreferences prefs = getSharedPreferences(SPLIT_PREFS, 0);
                            String displayName = prefs.getString(DISPLAY_NAME_KEY, "");
                            if (!TextUtils.isEmpty(displayName)) {

                                grp.addMember(displayName, new SingleBalance());
                            }

                            mDatabaseReference.child("groups/" + name).setValue(grp, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {
                                    final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content),
                                            getString(R.string.friend_added), Snackbar.LENGTH_LONG);

                                    snackBar.setAction(getString(R.string.close), new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // Call your action method here
                                            snackBar.dismiss();
                                        }
                                    });
                                    snackBar.show();
                                    if (databaseError != null)
                                        Log.i(TAG, databaseError.getDetails());
                                    finish();
                                }
                            });
                        }
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

                break;
            case R.id.gotoGroupList:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
