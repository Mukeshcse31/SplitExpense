package com.google.app.splitwise_clone.groups;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.app.splitwise_clone.AddFriend;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.Friend;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
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
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mPhotosStorageReference;
private String photoURL = "";
    private ChildEventListener mChildEventListener;
    private static final int RC_PHOTO_PICKER = 2;
    private static final String TAG = AddGroup.class.getSimpleName();
    private GroupMembersAdapter mGroupMembersAdapter;
    private NonGroupMembersAdapter mNonGroupMembersAdapter;
    private RecyclerView members_rv;
    private RecyclerView nonmembers_rv;
    private ImageButton mPhotoPickerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_add_group);
        getSupportActionBar().setTitle("");
        mFirebaseStorage = FirebaseStorage.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mPhotosStorageReference = mFirebaseStorage.getReference().child("group_photos");
        mGroupName = findViewById(R.id.group_name);
//
//        getSupportActionBar().setTitle("");
//        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        members_rv = (RecyclerView) findViewById(R.id.members_rv);
        nonmembers_rv = findViewById(R.id.nonmembers_rv);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);

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

            // Get a reference to store file at chat_photos/<FILENAME>
            StorageReference photoRef = mPhotosStorageReference.child("group1");

            // Upload file to Firebase Storage
            photoRef.putFile(selectedImageUri)
                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // When the image has successfully uploaded, we get its download URL
                            Uri downloadUrl = mPhotosStorageReference.getDownloadUrl().getResult();
        photoURL = downloadUrl.toString();
                            // Set the download URL to the message box, so that the user can send it to the database
//                            FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, downloadUrl.toString());
//                            mMessagesDatabaseReference.push().setValue(friendlyMessage);
                        }

                    });
        }
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
                        String email = i.getKey();
                        Friend f = i.getValue(Friend.class);
                        String name = f.getName(); //TODO check this
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
                            mGroupName.setError(getString(R.string.group_exists));
                            Toast.makeText(AddGroup.this, "Group already exists", Toast.LENGTH_LONG).show();

                        } else {
                            //add the group creator as a member when the group is created
                            Group grp = new Group(name);
                            SharedPreferences prefs = getSharedPreferences(SPLIT_PREFS, 0);
                            String displayName = prefs.getString(DISPLAY_NAME_KEY, "");
                            if (!TextUtils.isEmpty(displayName)) {

                                grp.addMember(displayName, new SingleBalance());
                            }
                            if(!TextUtils.isEmpty(photoURL)) grp.setPhotoUrl(photoURL);
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


//    private void attachDatabaseReadListener() {
//        if (mChildEventListener == null) {
//            mChildEventListener = new ChildEventListener() {
//                @Override
//                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
//                    mMessageAdapter.add(friendlyMessage);
//                }
//
//                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
//                public void onChildRemoved(DataSnapshot dataSnapshot) {}
//                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
//                public void onCancelled(DatabaseError databaseError) {}
//            };
//            mMessagesDatabaseReference.addChildEventListener(mChildEventListener);
//        }
//    }
//
//    private void detachDatabaseReadListener() {
//        if (mChildEventListener != null) {
//            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
//            mChildEventListener = null;
//        }
//    }

}
