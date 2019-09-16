package com.google.app.splitwise_clone.groups;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.app.splitwise_clone.R;
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
    private Uri selectedImageUri;
    private RecyclerView members_rv;
    private RecyclerView nonmembers_rv;
    private ImageView mPhotoPickerButton;
    private String group_name, userName;
    private Group mGroup;
    float friendsCount;
    int friendsCounter;
    private String photoUrl = "";
    Map<String, String> userFriends = new HashMap<>();
    private Map<String, String> all_members = new HashMap<>();
    private Map<String, Boolean> all_friends = new HashMap<>();
    private Map<String, String> group_members = new HashMap<>();
    private Map<String, String> nongroup_members = new HashMap<>();
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_add_group);
        getSupportActionBar().setTitle(getString(R.string.new_group));
        mFirebaseStorage = FirebaseStorage.getInstance();
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mPhotosStorageReference = mFirebaseStorage.getReference().child("images/groups");
        mGroupName = findViewById(R.id.group_name);
        members_rv = findViewById(R.id.members_rv);
        nonmembers_rv = findViewById(R.id.nonmembers_rv);
        mPhotoPickerButton = findViewById(R.id.photoPickerButton);
        userName = FirebaseUtils.getUserName();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        members_rv.setLayoutManager(layoutManager);
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(this);
        nonmembers_rv.setLayoutManager(layoutManager1);


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)
                    this, Manifest.permission.CAMERA)) {


            } else {
                ActivityCompat.requestPermissions( this,
                        new String[]{Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST_CAMERA);
            }

        }
        else
            setphotoClickListener();

//        if (Permissions.checkPermission(this))
//            setphotoClickListener();
//        else
//            requestCameraPermission();

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


    private void requestCameraPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
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
            selectedImageUri = data.getData();
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
                                    //  https://stackoverflow.com/questions/46619510/how-can-i-download-image-on-firebase-storage
                                    //  https://github.com/bumptech/glide/issues/458
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

    private void setphotoClickListener(){
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Check if the group Name is empty
                if (TextUtils.isEmpty(mGroupName.getText().toString())) {
                    mGroupName.setError(getString(R.string.groupname_warning));
                } else {

                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/jpeg");
                    intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                    if (intent.resolveActivity(getPackageManager()) != null)
                        startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
                    setphotoClickListener();
                    // main logic
                } else {
//                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
//                            showMessageOKCancel("You need to allow access permissions",
//                                    new DialogInterface.OnClickListener() {
//                                        @Override
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                            }
//                                        }
//                                    });
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(AddGroup.this)
                .setMessage(message)
                .setIcon(R.drawable.camera)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void membersViews() {
        final String path1 = getString(R.string.db_users);

        //get all the friends of the user
        Query query = mDatabaseReference.child(path1 + "/" + userName + "/friends");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    userFriends = new HashMap<>();
                    userFriends.put(userName, "");
                    friendsCount = dataSnapshot.getChildrenCount() + 1;//include the user
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        final String friend = i.getKey();
                        if( friend != null)
                        userFriends.put(friend, "");
                    }

                    Iterator it = userFriends.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        final String friend = (String) pair.getKey();
                        //Get the friends' email id
                        Query query = mDatabaseReference.child(path1 + "/" + friend + "/email");
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                friendsCounter += 1;
                                if (dataSnapshot.exists()) {
                                    String email = (String) dataSnapshot.getValue();
                                    userFriends.put(friend, email);
                                    getGroupMembers();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void getGroupMembers() {

        if (friendsCount == friendsCounter) {

            //for editing the existing group
            if (mGroup != null) {
                mGroupName.setText(mGroup.getName());
                Map<String, SingleBalance> members = mGroup.getMembers();
                Iterator it = members.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    String name = (String) pair.getKey();
                    SingleBalance sb = (SingleBalance) pair.getValue();
                    String email = sb.getEmail();
                    group_members.put(name, email);
//                            it.remove(); // avoids a ConcurrentModificationException
                }

                //load image
                final StorageReference imageRef = mPhotosStorageReference.child(group_name);
                final long ONE_MEGABYTE = 1024 * 1024;
                imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Glide.with(AddGroup.this)
                                .load(bytes)
                                .asBitmap()
                                .into(mPhotoPickerButton);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {

                        Log.i(TAG, exception.getMessage());
                        // Handle any errors
                    }
                });

            } else {//add the username by default
                group_members.put(userName, userFriends.get(userName));
                nongroup_members = new HashMap<>(userFriends);
                nongroup_members.remove(userName);
            }
            updateAdapters();

        }
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

        String friend = group_members.get(name);
        if(friend != null) {
            nongroup_members.put(name, friend);
            group_members.remove(name);
            updateAdapters();
        }
    }

    @Override
    public void addFriendToGroup(final String name) {

        String friend = nongroup_members.get(name);
        if (friend != null) {
            group_members.put(name, friend);
            nongroup_members.remove(name);
            updateAdapters();
        }
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
            if (TextUtils.equals(mGroup.getOwner(), userName)) {
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

//                          // add all the group members to the group
                            Iterator it = group_members.entrySet().iterator();
                            while (it.hasNext()) {

                                Map.Entry pair = (Map.Entry) it.next();
                                String groupMemberName = (String) pair.getKey();
                                String email = (String) pair.getValue();
                                SingleBalance sb = new SingleBalance(groupMemberName);
                                sb.setEmail(email);
                                grp.addMember(groupMemberName, sb);

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
                Group newGroup = new Group();
                try {
                    newGroup = (Group) mGroup.clone();
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }

                final String newGroupNname = mGroupName.getText().toString();
                newGroup.setName(newGroupNname);

                // add all the new group members to the group
                Iterator it = group_members.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    String groupMemberName = (String) pair.getKey();
                    if (!newGroup.getMembers().containsKey(groupMemberName)) {
                        String email = (String) pair.getValue();
                        SingleBalance sb = new SingleBalance(groupMemberName);
                        sb.setEmail(email);
                        newGroup.addMember(groupMemberName, sb);
                    }
                }

//                if non members are in the group, set them as active No
                it = nongroup_members.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    String groupMemberName = (String) pair.getKey();
                    if (newGroup.getMembers().containsKey(groupMemberName)) {
                        newGroup.getMembers().get(groupMemberName).setActive("No");
                    }
                }

                //update the image if the image name is updated after changing the image
                String prev_imageName = "";
                photoUrl = TextUtils.isEmpty(photoUrl) ? newGroup.getPhotoUrl() : photoUrl; //if the image is not changed and group name changed
                if (!TextUtils.isEmpty(photoUrl)) {
                    String[] imageName = photoUrl.split("/");
                    prev_imageName = imageName[imageName.length - 1];
                    if (!TextUtils.equals(prev_imageName, newGroupNname)) {
                        imageName[imageName.length - 1] = newGroupNname;
//                        final StorageReference photoRef = mPhotosStorageReference.child(String.join("/", imageName));

                        final StorageReference imageRef = mPhotosStorageReference.child(prev_imageName);
                        final long ONE_MEGABYTE = 1024 * 1024;
                        imageRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                final StorageReference photoRef = mPhotosStorageReference.child(newGroupNname);

                                // Upload file to Firebase Storage
                                photoRef.putBytes(bytes)
                                        .addOnSuccessListener(AddGroup.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                Log.i(TAG, "new Image uploaded");
                                            }
                                        });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {

                                Log.i(TAG, exception.getMessage());
                                // Handle any errors
                            }
                        });

                        newGroup.setPhotoUrl(String.join("/", imageName));
                    } else {
                        final StorageReference photoRef = mPhotosStorageReference.child(newGroupNname);
                        // Upload file to Firebase Storage
                        photoRef.putFile(selectedImageUri)
                                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        Log.i(TAG, "new Image uploaded");
                                    }
                                });
                    }
                }

                //Add the clone of the old group with the updated values
                mDatabaseReference.child("groups/" + newGroupNname).setValue(newGroup, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference dataReference) {
                        final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.saved_group), Snackbar.LENGTH_LONG);

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

                //delete the old photo storage
                if (!TextUtils.equals(prev_imageName, newGroupNname)) {
                    mPhotosStorageReference.child(prev_imageName).delete();

                    //Delete the old group
                    mDatabaseReference.child("groups/" + group_name).removeValue();
                }
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
