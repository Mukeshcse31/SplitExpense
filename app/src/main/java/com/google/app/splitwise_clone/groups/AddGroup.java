package com.google.app.splitwise_clone.groups;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.expense.ExpenseList;
import com.google.app.splitwise_clone.model.Friend;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.app.splitwise_clone.utils.GroupMembersAdapter;
import com.google.app.splitwise_clone.utils.GroupsAdapter;
import com.google.app.splitwise_clone.utils.NonGroupMembersAdapter;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AddGroup extends AppCompatActivity implements GroupMembersAdapter.OnClickListener,
        NonGroupMembersAdapter.OnClickListener {

    private AutoCompleteTextView mGroupName;
    private DatabaseReference mDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mPhotosStorageReference;
    private ChildEventListener mChildEventListener;
    private static final int RC_PHOTO_PICKER = 2;
    private static final String TAG = AddGroup.class.getSimpleName();
    private GroupMembersAdapter mGroupMembersAdapter;
    private NonGroupMembersAdapter mNonGroupMembersAdapter;
    private RecyclerView members_rv;
    private RecyclerView nonmembers_rv;
    private ImageView mPhotoPickerButton;
    private String group_name;
    private Group mGroup;
    private Map<String, String> all_members = new HashMap<>();
    private Map<String, String> group_members = new HashMap<>();
    private Map<String, String> nongroup_members = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_add_group);
        getSupportActionBar().setTitle("");
        mFirebaseStorage = FirebaseStorage.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mPhotosStorageReference = mFirebaseStorage.getReference().child("images/groups");
        mGroupName = findViewById(R.id.group_name);
//
//        getSupportActionBar().setTitle("");
//        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        members_rv = (RecyclerView) findViewById(R.id.members_rv);
        nonmembers_rv = findViewById(R.id.nonmembers_rv);
        mPhotoPickerButton = findViewById(R.id.photoPickerButton);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        members_rv.setLayoutManager(layoutManager);
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(this);
        nonmembers_rv.setLayoutManager(layoutManager1);
        membersViews();

        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.containsKey(GroupsList.GROUP_NAME)) {
            group_name = bundle.getString(GroupsList.GROUP_NAME);
            if (bundle.containsKey(GroupsList.EDIT_GROUP)) {
                mGroup = bundle.getParcelable(GroupsList.EDIT_GROUP);
            }
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == RC_SIGN_IN) {
//            if (resultCode == RESULT_OK) {
//                // Sign-in succeeded, set up the UI
//                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
//            } else if (resultCode == RESULT_CANCELED) {
//                // Sign in was canceled by the user, finish the activity
//                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
//                finish();
//            }
//        } else
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            final String groupName = TextUtils.isEmpty(mGroupName.getText().toString()) ? "anonymous" : mGroupName.getText().toString();

            // Get a reference to store file at chat_photos/<FILENAME>
            final StorageReference photoRef = mPhotosStorageReference.child(groupName);

            // Upload file to Firebase Storage
            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            final StorageReference group1 = mPhotosStorageReference.child(groupName);
                            final long ONE_MEGABYTE = 1024 * 1024;
                            group1.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
//                                           https://stackoverflow.com/questions/46619510/how-can-i-download-image-on-firebase-storage
                                    //            https://github.com/bumptech/glide/issues/458
                                    Glide.with(AddGroup.this)
                                            .load(bytes)
                                            .asBitmap()
                                            .into(mPhotoPickerButton);

                                    // Data for "images/island.jpg" is returns, use this as needed
                                    Log.i(TAG, "photo download " + mPhotosStorageReference.getPath());
                                    mPhotoPickerButton.setContentDescription(mPhotosStorageReference.getPath() + "/" + groupName);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {

                                    Log.i(TAG, exception.getMessage());
                                    // Handle any errors
                                }
                            });
                        }
                    });
        }
    }

    private void membersViews() {
        String path1 = getString(R.string.db_users);

        //get all the users
        Query query = mDatabaseReference.child(path1);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        String email = i.getKey();
                        Friend f = i.getValue(Friend.class);
                        String name = f.getName();
                        all_members.put(name, email);
                    }
//--- - - - - - - - - - - - - -TODO if dataSnapshot.exists() check--

                    if (mGroup != null) {
                        Map<String, SingleBalance> members = mGroup.getMembers();
                        Iterator it = members.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            String name = (String) pair.getKey();
                            group_members.put(name, name); //TODO update this to have email id of the user
                            it.remove(); // avoids a ConcurrentModificationException
                        }

                    } else {
                    }
                    Iterator<String> it = all_members.keySet().iterator();
                    while (it.hasNext()) {
                        String userName = it.next();
                        if (group_members.containsKey(userName)) {
//                            group_members.put(userName, all_members.get(userName));
                        } else {
                            nongroup_members.put(userName, all_members.get(userName));
                        }
                    }
                    updateAdapters();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void updateAdapters() {
        mGroupMembersAdapter = new GroupMembersAdapter(group_members, AddGroup.this);
        members_rv.setAdapter(mGroupMembersAdapter);

        mNonGroupMembersAdapter = new NonGroupMembersAdapter(nongroup_members, AddGroup.this);
        nonmembers_rv.setAdapter(mNonGroupMembersAdapter);
    }

    @Override
    public void removeFriendFromGroup(final String name) {

        nongroup_members.put(name, group_members.get(name));
        group_members.remove(name);
        updateAdapters();

    }

    @Override
    public void addFriendToGroup(final String name) {

        group_members.put(name, nongroup_members.get(name));
        nongroup_members.remove(name);
        updateAdapters();
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
                            mGroupName.setError(getString(R.string.group_exists));
                            Toast.makeText(AddGroup.this, "Group already exists", Toast.LENGTH_LONG).show();

                        } else {
                            //add the group creator as a member when the group is created
                            Group grp = new Group(name);
                            String userName = FirebaseUtils.getUserName();

//                            for ()//TODO add all the members'
                            Iterator it = group_members.entrySet().iterator();
                            while (it.hasNext()) {

                                Map.Entry pair = (Map.Entry) it.next();
                                String groupMemberName = (String) pair.getKey();
                                grp.addMember(groupMemberName, new SingleBalance());

                            }
                            grp.addMember(userName, new SingleBalance());

                            grp.setPhotoUrl(mPhotoPickerButton.getContentDescription().toString());
                            grp.setOwner(userName);
                            mDatabaseReference.child("groups/" + name).setValue(grp, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {
                                    final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content),
                                            getString(R.string.group_added), Snackbar.LENGTH_LONG);

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
