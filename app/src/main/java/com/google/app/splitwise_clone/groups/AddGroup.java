package com.google.app.splitwise_clone.groups;

import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.model.User;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.model.SingleBalance;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.app.splitwise_clone.utils.GroupMembersAdapter;
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

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Iterator;
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

    private RecyclerView members_rv;
    private RecyclerView nonmembers_rv;
    private ImageView mPhotoPickerButton;
    private String group_name, user_name;
    private Group mGroup;
    private String photoUrl = "";
    private Map<String, String> all_members = new HashMap<>();
    private Map<String, Boolean> all_friends = new HashMap<>();
    private Map<String, String> group_members = new HashMap<>();
    private Map<String, String> nongroup_members = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_add_group);
        getSupportActionBar().setTitle(getString(R.string.new_group));
        mFirebaseStorage = FirebaseStorage.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mPhotosStorageReference = mFirebaseStorage.getReference().child("images/groups");
        mGroupName = findViewById(R.id.group_name);
        members_rv = (RecyclerView) findViewById(R.id.members_rv);
        nonmembers_rv = findViewById(R.id.nonmembers_rv);
        mPhotoPickerButton = findViewById(R.id.photoPickerButton);
        user_name = FirebaseUtils.getUserName();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        members_rv.setLayoutManager(layoutManager);
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(this);
        nonmembers_rv.setLayoutManager(layoutManager1);

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
            getSupportActionBar().setTitle(group_name);
            if (bundle.containsKey(GroupsList.EDIT_GROUP)) {
                mGroup = bundle.getParcelable(GroupsList.EDIT_GROUP);
            }
        }
        membersViews();
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
                                    photoUrl = mPhotosStorageReference.getPath() + "/" + groupName;
//                                    mPhotoPickerButton.setContentDescription();
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

                        User f = i.getValue(User.class);
                        String name = f.getName();
                        String email = f.getEmail();
                        all_members.put(name, email);

                        if (TextUtils.equals(user_name, name)) {
                            all_friends = f.getFriends();
                        }
                    }

                    //TODO all_members should be the friends of the userName
                    Map<String, String> allMembers = new HashMap<>(all_members);
                    Iterator it = allMembers.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        String name = (String) pair.getKey();
                        if (TextUtils.equals(user_name, name)) continue;
                        if (!all_friends.containsKey(name)) {
                            all_members.remove(name);
                        }
                        it.remove();
                    }

                    //for editing the existing group
                    if (mGroup != null) {
                        mGroupName.setText(mGroup.getName());
                        Map<String, SingleBalance> members = mGroup.getMembers();
                        it = members.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry pair = (Map.Entry) it.next();
                            String name = (String) pair.getKey();
                            String email = (String) pair.getValue();
                            group_members.put(name, email);//TODO update this to have the member's email id
                            it.remove(); // avoids a ConcurrentModificationException
                        }

                    } else {//add the username by default
                        group_members.put(user_name, all_members.get(user_name));

                    }
                    //collect all the non-group members
                    Iterator<String> it1 = all_members.keySet().iterator();
                    while (it1.hasNext()) {

                        String name1 = it1.next();
                        if (!group_members.containsKey(name1))
                            nongroup_members.put(name1, all_members.get(name1));
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
        GroupMembersAdapter mGroupMembersAdapter;
        NonGroupMembersAdapter mNonGroupMembersAdapter;
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

        MenuItem addMenu = menu.findItem(R.id.addGroup);
        MenuItem deleteMenu = menu.findItem(R.id.deleteGroup);
        MenuItem saveMenu = menu.findItem(R.id.saveGroup);

        deleteMenu.setVisible(false);
        saveMenu.setVisible(false);
        if (mGroup == null) {

            addMenu.setVisible(true);
        } else {
            addMenu.setVisible(false);
            if (TextUtils.equals(mGroup.getOwner(), user_name)) {
                deleteMenu.setVisible(true);
                saveMenu.setVisible(true);
            }
        }

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
                        if (dataSnapshot.exists()) {
                            mGroupName.setError(getString(R.string.group_exists));
                            Toast.makeText(AddGroup.this, "Group already exists", Toast.LENGTH_LONG).show();

                        } else {
                            Group grp = new Group(name);
                            String userName = FirebaseUtils.getUserName();

//                          // add all the group members to the group
                            Iterator it = group_members.entrySet().iterator();
                            while (it.hasNext()) {

                                Map.Entry pair = (Map.Entry) it.next();
                                String groupMemberName = (String) pair.getKey();
                                grp.addMember(groupMemberName, new SingleBalance(groupMemberName));

                            }
                            //add the group creator as a member when the group is created
                            grp.setPhotoUrl(photoUrl);
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

            case R.id.saveGroup:

                finish();
                break;

            case R.id.deleteGroup:

                //code to delete the group
                //https://www.tutorialspoint.com/android/android_alert_dialoges.htm
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setMessage(getString(R.string.group_delete_warning));
                alertDialogBuilder.setPositiveButton(getString(R.string.yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                mDatabaseReference.child("groups/" + group_name).removeValue();
                                finish();

                            }
                        });

                alertDialogBuilder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                break;

            case R.id.gotoGroupList:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
