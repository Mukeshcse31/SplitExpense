package com.google.app.splitwise_clone.groups;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.app.splitwise_clone.FriendsList;
import com.google.app.splitwise_clone.R;
import com.google.app.splitwise_clone.expense.AddExpense;
import com.google.app.splitwise_clone.expense.ExpenseList;
import com.google.app.splitwise_clone.model.Group;
import com.google.app.splitwise_clone.utils.FirebaseUtils;
import com.google.app.splitwise_clone.utils.GroupsAdapter;
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

import java.util.ArrayList;
import java.util.List;

public class GroupsList extends AppCompatActivity implements GroupsAdapter.OnClickListener {

    private DatabaseReference mDatabaseReference;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mPhotosStorageReference;
    List<DataSnapshot> dataSnapshotGroupList = new ArrayList<>();
    ;
    private String TAG = GroupsList.class.getSimpleName();
    private String userName = "";
    public static String GROUP_NAME = "group_name";
    public static String EDIT_GROUP = "edit_group";
    private static final int RC_PHOTO_PICKER = 2;
    private GroupsAdapter mGroupsAdapter;
    private RecyclerView groups_rv;
    private ImageView groupImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);
        userName = FirebaseUtils.getUserName();
        groupImage = findViewById(R.id.groupImage);
        getSupportActionBar().setTitle(getString(R.string.group_list));
        mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        groups_rv = (RecyclerView) findViewById(R.id.groups_rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        groups_rv.setLayoutManager(layoutManager);
        groupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });

    }


    private void populateGroupList() {
        String userName = FirebaseUtils.getUserName();
        Query query = mDatabaseReference.child("groups").orderByChild("members/" + userName + "/name").equalTo(userName);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                dataSnapshotGroupList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot i : dataSnapshot.getChildren()) {
                        dataSnapshotGroupList.add(i);
                    }
                    mGroupsAdapter = new GroupsAdapter(dataSnapshotGroupList, GroupsList.this);
                    groups_rv.setAdapter(mGroupsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.group_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.gotoAddGroup:
                Intent intent = new Intent(GroupsList.this, AddGroup.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void gotoSharedGroup(String name) {

        Intent intent = new Intent(GroupsList.this, ExpenseList.class);
        intent.putExtra(GROUP_NAME, name);
        startActivity(intent);

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
                        Intent intent = new Intent(GroupsList.this, AddGroup.class);
                        Group group = dataSnapshotGroupList.get(index).getValue(Group.class);
                        intent.putExtra(GROUP_NAME, groupName);
                        intent.putExtra(EDIT_GROUP, group);
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

//    //WIP - update image only by the owner of the group
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        //update users' profile picture
//        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
//            Uri selectedImageUri = data.getData();
//
//            // Get a reference to store file at chat_photos/<FILENAME>
//            mPhotosStorageReference = mFirebaseStorage.getReference().child("images/groups");
//            final StorageReference photoRef = mPhotosStorageReference.child(userName);
//
//            // Upload file to Firebase Storage
//            photoRef.putFile(selectedImageUri)
//                    .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//
//                            //update the path of image in the DB users'
//                            mDatabaseReference = FirebaseDatabase.getInstance().getReference();
//                            mDatabaseReference.child("users/" + userName + "/imageUrl").setValue(photoRef.getPath());
//
//                            final StorageReference group1 = mPhotosStorageReference.child("images/users/" + userName);
//                            final long ONE_MEGABYTE = 1024 * 1024;
//                            group1.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
//                                @Override
//                                public void onSuccess(byte[] bytes) {
////                                           https://stackoverflow.com/questions/46619510/how-can-i-download-image-on-firebase-storage
//                                    //            https://github.com/bumptech/glide/issues/458
//                                    Glide.with(GroupsList.this)
//                                            .load(bytes)
//                                            .asBitmap()
//                                            .into(groupImage);
//
////                                    Log.i(TAG, "photo download " + mPhotosStorageReference.getPath());
//                                }
//                            }).addOnFailureListener(new OnFailureListener() {
//                                @Override
//                                public void onFailure(@NonNull Exception exception) {
//
//                                    Log.i(TAG, exception.getMessage());
//                                    // Handle any errors
//                                }
//                            });
//                        }
//                    });
//        }
//    }

    @Override
    public void onResume() {
        super.onResume();
        populateGroupList();
    }
}
